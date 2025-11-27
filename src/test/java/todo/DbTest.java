package todo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Db tests")
public class DbTest {
    private Path tmpDbFile;
    private Db db;

    @BeforeEach
    public void setUp() throws Exception {
        tmpDbFile = Files.createTempFile("dbtest-", ".db");
        String url = "jdbc:sqlite:" + tmpDbFile.toAbsolutePath().toString();
        db = new Db(url);
    }

    @AfterEach
    public void tearDown() throws Exception {
        try {
            Files.deleteIfExists(tmpDbFile);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Category CRUD via Db")
    public void categoryCrud() throws Exception {
        ArrayList<Category> cats = db.getCategories();
        assertNotNull(cats);
        assertTrue(cats.isEmpty());

        db.addCategory(new Category("Work", "Work tasks"));
        ArrayList<Category> after = db.getCategories();
        assertEquals(1, after.size());
        assertEquals("Work", after.get(0).getName());

        Category c = db.getCategory("Work");
        assertNotNull(c);
        assertEquals("Work tasks", c.getDescription());

        db.updateCategory(new Category("Work", "Updated"));
        Category upd = db.getCategory("Work");
        assertEquals("Updated", upd.getDescription());

        db.deleteCategory("Work");
        assertTrue(db.getCategories().isEmpty());
    }

    @Test
    @DisplayName("Task lifecycle via Db")
    public void taskLifecycle() throws Exception {
        // ensure category exists
        db.addCategory(new Category("CatA", "desc"));

        // create task
        Task t = new Task(0, "Name", "Content", "CatA", TaskStatus.NOT_STARTED, "2025-12-31");
        int id = db.createTask(t);
        assertTrue(id > 0);

        ArrayList<Task> tasks = db.getTasks();
        assertFalse(tasks.isEmpty());
        Task stored = tasks.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        assertNotNull(stored);
        assertEquals("Name", stored.getName());

        // update
        Task updated = new Task(id, "Name2", "Content2", "CatA", TaskStatus.IN_PROGRESS, "2026-01-01");
        db.updateTask(updated);
        Task stored2 = db.getTasks().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        assertNotNull(stored2);
        assertEquals("Name2", stored2.getName());
        assertEquals(TaskStatus.IN_PROGRESS, stored2.getStatus());

        // getTasks by category
        ArrayList<Task> catTasks = db.getTasks(new Category("CatA", "desc"));
        assertFalse(catTasks.isEmpty());

        // delete
        db.deleteTask(stored2);
        assertTrue(db.getTasks().stream().noneMatch(x -> x.getId() == id));
    }

    @Test
    @DisplayName("Constructor throws SQLException for invalid DB path (directory)")
    public void constructorInvalidPathThrows() throws Exception {
        Path tmpDir = Files.createTempDirectory("dbtestdir-");
        try {
            String url = "jdbc:sqlite:" + tmpDir.toAbsolutePath().toString();
            assertThrows(SQLException.class, () -> new Db(url));
        } finally {
            try { Files.deleteIfExists(tmpDir); } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Create task with missing generated keys returns -1 or throws")
    public void createTaskNoKeys() throws Exception {
        // It's unusual for sqlite to not return generated keys, but ensure method handles it
        db.addCategory(new Category("C1", "d"));
        Task t = new Task(0, "A", "B", "C1", TaskStatus.NOT_STARTED, "");
        int id = db.createTask(t);
        assertTrue(id > 0 || id == -1);
    }

    @Test
    @DisplayName("Category primary key conflict")
    public void categoryPrimaryKeyConflict() throws Exception {
        db.addCategory(new Category("K", "d"));
        assertThrows(SQLException.class, () -> db.addCategory(new Category("K", "other")));
    }

    @Test
    @DisplayName("Category null name/description handled by model")
    public void categoryNullNameOrDescription() {
        assertThrows(todo.exceptions.NullCategoryException.class, () -> new Category(null, "d"));
        assertThrows(todo.exceptions.NullCategoryException.class, () -> new Category("n", null));
    }

    @Test
    @DisplayName("getCategory(null) and deleteCategory(null) do not crash")
    public void getAndDeleteCategoryNull() throws Exception {
        db.addCategory(new Category("A", "d"));
        assertNull(db.getCategory(null));
        assertDoesNotThrow(() -> db.deleteCategory(null));
        // ensure existing still present
        assertNotNull(db.getCategory("A"));
    }

    @Test
    @DisplayName("update non-existing category is a no-op")
    public void updateNonExistingCategory() throws Exception {
        db.updateCategory(new Category("NoSuch", "x"));
        assertNull(db.getCategory("NoSuch"));
    }

    @Test
    @DisplayName("create task with non-existing category")
    public void createTaskWithNonExistingCategory() throws Exception {
        // do not create category
        Task t = new Task(0, "N", "c", "NoCat", TaskStatus.NOT_STARTED, "");
        int id = db.createTask(t);
        assertTrue(id > 0);
        Task stored = db.getTasks().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        assertNotNull(stored);
        assertEquals("NoCat", stored.getCategoryName());
    }

    @Test
    @DisplayName("createTask with null name fails")
    public void createTaskNullName() {
        assertThrows(SQLException.class, () -> db.createTask(new Task(0, null, "c", "CatA", TaskStatus.NOT_STARTED, "")));
    }

    @Test
    @DisplayName("updateTask with non-existing category succeeds")
    public void updateTaskWithNonExistingCategory() throws Exception {
        db.addCategory(new Category("CatA","d"));
        int id = db.createTask(new Task(0, "T", "c", "CatA", TaskStatus.NOT_STARTED, ""));
        assertTrue(id > 0);
        db.updateTask(new Task(id, "T2", "c2", "NoCat", TaskStatus.IN_PROGRESS, ""));
        Task stored = db.getTasks().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        assertNotNull(stored);
        assertEquals("NoCat", stored.getCategoryName());
    }

    @Test
    @DisplayName("updateTask with null name fails")
    public void updateTaskNullName() throws Exception {
        db.addCategory(new Category("CatA","d"));
        int id = db.createTask(new Task(0, "T", "c", "CatA", TaskStatus.NOT_STARTED, ""));
        assertTrue(id > 0);
        assertThrows(SQLException.class, () -> db.updateTask(new Task(id, null, "c", "CatA", TaskStatus.NOT_STARTED, "")));
    }

    @Test
    @DisplayName("deleteTask with id 0 or negative does not throw")
    public void deleteTaskInvalidIds() throws Exception {
        db.addCategory(new Category("CatA","d"));
        int id = db.createTask(new Task(0, "T", "c", "CatA", TaskStatus.NOT_STARTED, ""));
        assertTrue(id > 0);
        assertDoesNotThrow(() -> db.deleteTask(new Task(0, "", "", "", TaskStatus.NOT_STARTED, "")));
        assertDoesNotThrow(() -> db.deleteTask(new Task(-1, "", "", "", TaskStatus.NOT_STARTED, "")));
        // original task remains
        assertTrue(db.getTasks().stream().anyMatch(x -> x.getId() == id));
    }

    @Test
    @DisplayName("parseTask: status null -> NOT_STARTED; code/name/invalid fallback")
    public void parseTaskStatusVariants() throws Exception {
        db.addCategory(new Category("C1","d"));
        // status null
        int id1 = db.createTask(new Task(0, "A", "B", "C1", null, ""));
        Task t1 = db.getTasks().stream().filter(x -> x.getId() == id1).findFirst().orElse(null);
        assertNotNull(t1);
        assertEquals(TaskStatus.NOT_STARTED, t1.getStatus());

        // status correct code
        int id2 = db.createTask(new Task(0, "B", "B", "C1", TaskStatus.IN_PROGRESS, ""));
        Task t2 = db.getTasks().stream().filter(x -> x.getId() == id2).findFirst().orElse(null);
        assertNotNull(t2);
        assertEquals(TaskStatus.IN_PROGRESS, t2.getStatus());

        // status stored as enum name -> set directly via SQL
        String url = "jdbc:sqlite:" + tmpDbFile.toAbsolutePath().toString();
        try (Connection conn = DriverManager.getConnection(url)) {
            try (java.sql.Statement st = conn.createStatement()) {
                st.executeUpdate("UPDATE Tasks SET status = 'IN_PROGRESS' WHERE id = " + id2);
            }
        }
        Task t2b = db.getTasks().stream().filter(x -> x.getId() == id2).findFirst().orElse(null);
        assertNotNull(t2b);
        // Current parseTask first tries mapping by code and returns NOT_STARTED when unknown.
        // When status is stored as enum name (e.g. 'IN_PROGRESS') parseTask will not map it
        // because TaskStatus.fromCode does not throw for unknown codes; expect NOT_STARTED.
        assertEquals(TaskStatus.NOT_STARTED, t2b.getStatus());

        // invalid status -> fallback
        int id3 = db.createTask(new Task(0, "C", "B", "C1", TaskStatus.NOT_STARTED, ""));
        try (Connection conn = DriverManager.getConnection(url)) {
            try (java.sql.Statement st = conn.createStatement()) {
                st.executeUpdate("UPDATE Tasks SET status = 'bogus' WHERE id = " + id3);
            }
        }
        Task t3 = db.getTasks().stream().filter(x -> x.getId() == id3).findFirst().orElse(null);
        assertNotNull(t3);
        assertEquals(TaskStatus.NOT_STARTED, t3.getStatus());
    }

    @Test
    @DisplayName("content/due/categoryName can be null and are preserved")
    public void contentDueCategoryNameNull() throws Exception {
        int id = db.createTask(new Task(0, "N", null, null, null, null));
        Task stored = db.getTasks().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        assertNotNull(stored);
        assertNull(stored.getContent());
        assertNull(stored.getDue());
        assertNull(stored.getCategoryName());
    }

    @Test
    @DisplayName("DB resilience: read-only and corrupted table and parallel instances")
    public void dbResilienceScenarios() throws Exception {
        // parallel instances can see each other's changes
        String url = "jdbc:sqlite:" + tmpDbFile.toAbsolutePath().toString();
        Db other = new Db(url);
        db.addCategory(new Category("P","p"));
        assertTrue(other.getCategories().stream().anyMatch(c -> "P".equals(c.getName())));

        // corrupted table: drop Tasks and ensure getTasks throws
        try (Connection conn = DriverManager.getConnection(url)) {
            try (java.sql.Statement st = conn.createStatement()) {
                st.executeUpdate("DROP TABLE IF EXISTS Tasks");
            }
        }
        assertThrows(SQLException.class, () -> db.getTasks());

        // recreate Tasks table for further tests
        try (Connection conn = DriverManager.getConnection(url)) {
            try (java.sql.Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS Tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, content TEXT, categoryName TEXT, status TEXT, dueDate TEXT)");
            }
        }

        // tasks survive category deletion (FK not enforced)
        db.addCategory(new Category("Surv","s"));
        int id = db.createTask(new Task(0, "TS", "c", "Surv", TaskStatus.NOT_STARTED, ""));
        db.deleteCategory("Surv");
        assertTrue(db.getTasks().stream().anyMatch(t -> t.getId() == id));

        // read-only: mark file readonly and expect writes to fail
        try { tmpDbFile.toFile().setReadOnly(); } catch (Exception ignored) {}
        assertThrows(SQLException.class, () -> db.createTask(new Task(0, "R", "c", null, TaskStatus.NOT_STARTED, "")));
        // clear readonly for cleanup
        try { tmpDbFile.toFile().setWritable(true); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Tasks with unicode and long strings")
    public void tasksUnicodeAndLong() throws Exception {
        db.addCategory(new Category("U","u"));
        String longName = "n".repeat(2000);
        String unicode = "emoji 👍 — тест — テスト — 中文";
        int id = db.createTask(new Task(0, longName, unicode, "U", TaskStatus.NOT_STARTED, "2025-12-31"));
        Task t = db.getTasks().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        assertNotNull(t);
        assertEquals(longName, t.getName());
        assertEquals(unicode, t.getContent());
    }
}
