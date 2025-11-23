package todo;
import java.nio.file.Path;
import java.sql.*;

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
               dueDate TEXT,
               FOREIGN KEY(categoryName) REFERENCES Categories(name)
               )
               """;

        stat.execute(sql2);
    }


    public Db() {
        Path userHome = Path.of(System.getProperty("user.home"));
        Path dbFile =  userHome.resolve("testy-crud.db");
        this.connectionString = "jdbc:sqlite:" + dbFile;
        try {
            ensureExists(DriverManager.getConnection(connectionString));
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Db(String connectionString) {
        this.connectionString = connectionString;
        try {
            ensureExists(DriverManager.getConnection(connectionString));
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void getCategories() {
        try(Connection connection = DriverManager.getConnection(connectionString)) {
            Statement stat = connection.createStatement();
            String sql1 = """
                   SELECT * FROM Categories
            """;
            ResultSet res = stat.executeQuery(sql1);

            while (res.next()) {
                String name = res.getString("name");
                String description = res.getString("description");
                System.out.println(name + " " + description);

            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
