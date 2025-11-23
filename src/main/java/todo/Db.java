package todo;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;

public class Db {
    private String connectionString;

    private void ensureExists(Connection connection) throws SQLException {
        try (Statement stat = connection.createStatement()) {
            String sql1 = "CREATE TABLE IF NOT EXISTS Categories (name TEXT PRIMARY KEY, description TEXT)";
            stat.execute(sql1);

            String sql2 = "CREATE TABLE IF NOT EXISTS Tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, content TEXT, categoryName TEXT, status TEXT, dueDate TEXT, FOREIGN KEY(categoryName) REFERENCES Categories(name))";
            stat.execute(sql2);
        }
    }
    public Db() throws SQLException {
        Path userHome = Path.of(System.getProperty("user.home"));
        Path dbFile = userHome.resolve("testy-crud.db");
        this.connectionString = "jdbc:sqlite:" + dbFile;
        try (Connection conn = DriverManager.getConnection(connectionString)) {
            ensureExists(conn);
        }
    }

    public Db(String connectionString) throws SQLException {
        this.connectionString = connectionString;
        try (Connection conn = DriverManager.getConnection(connectionString)) {
            ensureExists(conn);
        }
    }

    public ArrayList<Category> getCategories() throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString);
             Statement stat = connection.createStatement();
             ResultSet res = stat.executeQuery("SELECT * FROM Categories")) {

            ArrayList<Category> categories = new ArrayList<>();
            while (res.next()) {
                String name = res.getString("name");
                String description = res.getString("description");
                categories.add(new Category(name, description));
            }
            return categories;
        }
    }

    public Category getCategory(String categoryName) throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            String sql = """
                   SELECT * FROM Categories
                   WHERE name = ?
            """;
            try (PreparedStatement smt = connection.prepareStatement(sql)) {
                smt.setString(1, categoryName);
                try (ResultSet results = smt.executeQuery()) {
                    while (results.next()) {
                        String name = results.getString("name");
                        String description = results.getString("description");
                        return new Category(name, description);
                    }
                    return null;
                }
            }
        }
    }

    public void addCategory(Category category) throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            String sql = """
                   INSERT INTO Categories (name, description) VALUES (?, ?)
            """;
            try (PreparedStatement smt = connection.prepareStatement(sql)) {
                smt.setString(1, category.getName());
                smt.setString(2, category.getDescription());
                smt.execute();
            }
        }
    }

    public void deleteCategory(String categoryName) throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            String sql = """
                   DELETE FROM Categories
                   WHERE name = ?;
            """;
            try (PreparedStatement smt = connection.prepareStatement(sql)) {
                smt.setString(1, categoryName);
                smt.execute();
            }
        }
    }

    public void updateCategory(Category category) throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            String sql = """
                   UPDATE Categories
                   SET description = ?
                   WHERE name = ?;
            """;
            try (PreparedStatement smt = connection.prepareStatement(sql)) {
                smt.setString(1, category.getDescription());
                smt.setString(2, category.getName());
                smt.execute();
            }
        }
    }


    private Task parseTask(ResultSet res) throws SQLException {
        int id = res.getInt("id");
        String name = res.getString("name");
        String content = res.getString("content");
        String categoryName = res.getString("categoryName");
        String due = res.getString("dueDate");
        String _status = res.getString("status");
        TaskStatus taskStatus = null;
        if (_status != null) {
            // try mapping by enum code first
            try {
                taskStatus = TaskStatus.fromCode(_status);
            } catch (Exception ex) {
                try {
                    taskStatus = TaskStatus.valueOf(_status);
                } catch (Exception ex2) {
                    taskStatus = TaskStatus.NOT_STARTED;
                }
            }
        } else {
            taskStatus = TaskStatus.NOT_STARTED;
        }
        return new Task(
                id,
                name,
                content,
                categoryName,
                taskStatus,
                due
        );
    }

    public ArrayList<Task> getTasks() throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString);
             Statement st = connection.createStatement();
             ResultSet res = st.executeQuery("SELECT * FROM Tasks")) {

            ArrayList<Task> tasks = new ArrayList<>();
            while (res.next()) {
                Task t = parseTask(res);
                tasks.add(t);
            }
            return tasks;
        }
    }

    public ArrayList<Task> getTasks(Category category) throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            String sql = """
                    SELECT * FROM Tasks
                    WHERE categoryName = ?;
                    """;

            try (PreparedStatement sttm = connection.prepareStatement(sql)) {
                sttm.setString(1, category.getName());
                try (ResultSet res = sttm.executeQuery()) {

                    ArrayList<Task> tasks = new ArrayList<>();

                    while (res.next()) {
                        Task t = parseTask(res);
                        tasks.add(t);
                    }
                    return tasks;
                }
            }
        }
    }

    public void updateTask(Task task) throws SQLException {
        try (Connection con = DriverManager.getConnection(connectionString)) {
            String sql = """
                    UPDATE Tasks
                    SET name = ?,
                        content = ?,
                        categoryName = ?,
                        status = ?,
                        dueDate = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement sttm = con.prepareStatement(sql)) {
                sttm.setString(1, task.getName());
                sttm.setString(2, task.getContent());
                sttm.setString(3, task.getCategoryName());
                sttm.setString(4, task.getStatus() == null ? null : task.getStatus().getCode());
                sttm.setString(5, task.getDue());
                sttm.setInt(6, task.getId());
                sttm.execute();
            }
        }
    }

    public void deleteTask(Task task) throws SQLException {
        try (Connection con = DriverManager.getConnection(connectionString)) {
            String sql = """
                    DELETE FROM Tasks
                    WHERE id = ?
                    """;
            PreparedStatement sttm = con.prepareStatement(sql);
            sttm.setInt(1, task.getId());
            sttm.execute();
        }
    }

    public int createTask(Task task) throws SQLException {
        try (Connection con = DriverManager.getConnection(connectionString)) {
            String sql = """
                    INSERT INTO Tasks (name, content, categoryName, status, dueDate)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement sttm = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                sttm.setString(1, task.getName());
                sttm.setString(2, task.getContent());
                sttm.setString(3, task.getCategoryName());
                sttm.setString(4, task.getStatus() == null ? null : task.getStatus().getCode());
                sttm.setString(5, task.getDue());
                sttm.executeUpdate();
                try (ResultSet keys = sttm.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        }
        return -1;
    }
}
