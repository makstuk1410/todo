package todo;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

public class Db {
    private String connectionString;

    private void ensureExists(Connection connection) throws SQLException {

        Statement stat = connection.createStatement();
        String sql1 = """
               CREATE TABLE IF NOT EXISTS Categories (
               name TEXT PRIMARY KEY,
               description TEXT
               )
               """;
        stat.execute(sql1);

        String sql2 = """
               CREATE TABLE IF NOT EXISTS Tasks (
               id NUMBER PRIMARY KEY,
               name TEXT NOT NULL,
               content TEXT,
               categoryName TEXT,
               status TEXT,
               dueDate TEXT,
               FOREIGN KEY(categoryName) REFERENCES Categories(name)
               )
               """;

        stat.execute(sql2);
    }


    public Db() throws SQLException {
        Path userHome = Path.of(System.getProperty("user.home"));
        Path dbFile =  userHome.resolve("testy-crud.db");
        this.connectionString = "jdbc:sqlite:" + dbFile;
        ensureExists(DriverManager.getConnection(connectionString));
    }

    public Db(String connectionString) throws SQLException {
        this.connectionString = connectionString;
        ensureExists(DriverManager.getConnection(connectionString));
    }

    public ArrayList<Category> getCategories() throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            Statement stat = connection.createStatement();
            String sql1 = """
                   SELECT * FROM Categories
            """;
            ResultSet res = stat.executeQuery(sql1);
            ArrayList<Category> categories = new ArrayList<>();
            while (res.next()) {
                String name = res.getString("name");
                String description = res.getString("description");
                categories.add(new Category(name, description));
            }
        }
        return new ArrayList<>();
    }

    public Category getCategory(String categoryName) throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            Statement stat = connection.createStatement();
            String sql = """
                   SELECT * FROM Categories
                   WHERE name = ?
            """;

            PreparedStatement smt = connection.prepareStatement(sql);
            smt.setString(1, categoryName);
            ResultSet results = smt.executeQuery();

            while (results.next()) {
                String name = results.getString("name");
                String description = results.getString("description");

                return new Category(name, description);
            }



            return null;
        }
    }

    public void addCategory(Category category) throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            Statement stat = connection.createStatement();
            String sql = """
                   INSERT INTO Categories (name, description) VALUES (?, ?)
            """;

            PreparedStatement smt = connection.prepareStatement(sql);
            smt.setString(1, category.getName());
            smt.setString(2, category.getDescription());

            smt.execute();
        }
    }

    public void deleteCategory(String categoryName) throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            Statement stat = connection.createStatement();
            String sql = """
                   DELETE FROM Categories
                   WHERE name = ?;
            """;
            PreparedStatement smt = connection.prepareStatement(sql);
            smt.setString(1, categoryName);
            smt.execute();
        }
    }

    public void updateCategory(Category category) throws SQLException {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            Statement stat = connection.createStatement();
            String sql = """
                   UPDATE Categories
                   SET description = ?
                   WHERE name = ?;
            """;
            PreparedStatement smt = connection.prepareStatement(sql);
            smt.setString(1, category.getDescription());
            smt.setString(2, category.getName());

            smt.execute();
        }
    }


    private Task parseTask(ResultSet res) throws SQLException {
        int id = res.getInt("id");
        String name = res.getString("name");
        String content = res.getString("content");
        String categoryName = res.getString("categoryName");
        String due = res.getString("dueDate");
        String _status = res.getString("status");

        TaskStatus taskStatus = TaskStatus.valueOf(_status);

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
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            String sql = """
                    SELECT * FROM Tasks
                    """;

            ResultSet res = connection.createStatement().executeQuery(sql);

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
                    WHERE category = ?;
                    """;

            PreparedStatement sttm = connection.prepareStatement(sql);
            sttm.setString(1, category.getName());

            ResultSet res = sttm.executeQuery(sql);

            ArrayList<Task> tasks = new ArrayList<>();

            while (res.next()) {
                Task t = parseTask(res);
                tasks.add(t);
            }
            return tasks;
        }
    }

    public void updateTask(Task task) throws SQLException {
        try (Connection con = DriverManager.getConnection(connectionString)) {
            String sql = """
                    UPDATE Tasks
                    SET name = ?,
                        content = ?,
                        categoryName = ?,
                        dueDate = ?
                    WHERE id = ?
                    """;
            PreparedStatement sttm = con.prepareStatement(sql);
            sttm.setString(1, task.getName());
            sttm.setString(2, task.getContent());
            sttm.setString(3, task.getCategoryName());
            sttm.setString(4, task.getDue());
            sttm.setInt(5, task.getId());
            sttm.execute();
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

    public void createTask(Task task) throws SQLException {
        try (Connection con = DriverManager.getConnection(connectionString)) {
            String sql = """
                    INSERT INTO Tasks (id, name, content, categoryName, dueDate)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            PreparedStatement sttm = con.prepareStatement(sql);
            sttm.setInt(1, task.getId());
            sttm.setString(2, task.getName());
            sttm.setString(3, task.getContent());
            sttm.setString(4, task.getCategoryName());
            sttm.setString(5, task.getDue());
            sttm.execute();
        }
    }
}
