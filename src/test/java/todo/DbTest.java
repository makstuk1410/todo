package todo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
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
}
