package todo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskDao tests")
public class TaskDaoTest {
    private Path tmpDbFile;
    private Db db;
    private TaskDao dao;

    @BeforeEach
    public void setUp() throws Exception {
        tmpDbFile = Files.createTempFile("taskdao-", ".db");
        String url = "jdbc:sqlite:" + tmpDbFile.toAbsolutePath().toString();
        db = new Db(url);
        dao = new TaskDao(db);
        // ensure a category exists for FK
        db.addCategory(new Category("CatA", "desc"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        try { Files.deleteIfExists(tmpDbFile); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Create and list tasks")
    public void createAndList() {
        List<Task> before = dao.getAllTasks();
        assertNotNull(before);

        int id = dao.createTask(new Task(0, "T1", "C1", "CatA", TaskStatus.NOT_STARTED, "2025-12-01"));
        assertTrue(id > 0);

        List<Task> after = dao.getAllTasks();
        assertTrue(after.stream().anyMatch(t -> t.getId() == id && "T1".equals(t.getName())));
    }

    @Test
    @DisplayName("Update task")
    public void updateTask() {
        int id = dao.createTask(new Task(0, "T1", "C1", "CatA", TaskStatus.NOT_STARTED, ""));
        Task updated = new Task(id, "T1x", "C1x", "CatA", TaskStatus.IN_PROGRESS, "2026-01-01");
        dao.updateTask(updated);
        List<Task> tasks = dao.getAllTasks();
        assertTrue(tasks.stream().anyMatch(t -> t.getId() == id && "T1x".equals(t.getName()) && t.getStatus() == TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("Delete task")
    public void deleteTask() {
        int id = dao.createTask(new Task(0, "T1", "C1", "CatA", TaskStatus.NOT_STARTED, ""));
        dao.deleteTask(id);
        assertTrue(dao.getAllTasks().stream().noneMatch(t -> t.getId() == id));
    }

    @Test
    @DisplayName("DB write failures wrapped in RuntimeException")
    public void writeFailuresWrapped() throws Exception {
        class FailingDb extends Db {
            public FailingDb() throws SQLException { super(); }
            @Override
            public int createTask(Task task) throws SQLException { throw new SQLException("insert fail"); }
            @Override
            public void updateTask(Task task) throws SQLException { throw new SQLException("update fail"); }
            @Override
            public void deleteTask(Task task) throws SQLException { throw new SQLException("delete fail"); }
        }

        Db fdb = new FailingDb();
        TaskDao fdao = new TaskDao(fdb);
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> fdao.createTask(new Task(0, "a","b","CatA", TaskStatus.NOT_STARTED, "")));
        assertTrue(ex1.getCause() instanceof SQLException);
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> fdao.updateTask(new Task(1, "a","b","CatA", TaskStatus.NOT_STARTED, "")));
        assertTrue(ex2.getCause() instanceof SQLException);
        RuntimeException ex3 = assertThrows(RuntimeException.class, () -> fdao.deleteTask(1));
        assertTrue(ex3.getCause() instanceof SQLException);
    }

    @Test
    @DisplayName("DB read failure is wrapped")
    public void readFailureWrapped() throws Exception {
        class FailingDb extends Db {
            public FailingDb() throws SQLException { super(); }
            @Override
            public java.util.ArrayList<Task> getTasks() throws SQLException { throw new SQLException("read fail"); }
        }
        Db fdb = new FailingDb();
        TaskDao fdao = new TaskDao(fdb);
        RuntimeException rex = assertThrows(RuntimeException.class, () -> fdao.getAllTasks());
        assertTrue(rex.getCause() instanceof SQLException);
    }

    @Test
    @DisplayName("TaskDao() when Db() constructor throws SQLException")
    public void defaultConstructorDbThrows() throws Exception {
        String originalHome = System.getProperty("user.home");
        Path badHome = Files.createTempFile("badhome-", ".tmp");
        try {
            System.setProperty("user.home", badHome.toAbsolutePath().toString());
            RuntimeException ex = assertThrows(RuntimeException.class, () -> new TaskDao());
            assertNotNull(ex.getCause());
            assertTrue(ex.getCause() instanceof SQLException);
        } finally {
            System.setProperty("user.home", originalHome);
            try { Files.deleteIfExists(badHome); } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("deleteTask with id 0 and negative id do not crash and do not remove valid tasks")
    public void deleteInvalidIds() {
        int id = dao.createTask(new Task(0, "T1", "C1", "CatA", TaskStatus.NOT_STARTED, ""));
        assertTrue(id > 0);

        assertDoesNotThrow(() -> dao.deleteTask(0));
        assertTrue(dao.getAllTasks().stream().anyMatch(t -> t.getId() == id));

        assertDoesNotThrow(() -> dao.deleteTask(-5));
        assertTrue(dao.getAllTasks().stream().anyMatch(t -> t.getId() == id));
    }

    @Test
    @DisplayName("createTask with null or incomplete data")
    public void createTaskNullsAndEmpty() {
        // null Task should throw NPE
        assertThrows(NullPointerException.class, () -> dao.createTask(null));

        // null name should result in DB constraint error wrapped in RuntimeException
        Task bad = new Task(0, null, "c", "CatA", TaskStatus.NOT_STARTED, "");
        RuntimeException rex = assertThrows(RuntimeException.class, () -> dao.createTask(bad));
        assertTrue(rex.getCause() instanceof SQLException);

        // empty name (non-null) should insert (DB accepts empty string)
        int id2 = dao.createTask(new Task(0, "", "c", "CatA", TaskStatus.NOT_STARTED, ""));
        assertTrue(id2 > 0);
    }

    @Test
    @DisplayName("getAllTasks returns empty list for new DB")
    public void getAllTasksEmptyDb() throws Exception {
        Path tmp = Files.createTempFile("emptydb-", ".db");
        String url = "jdbc:sqlite:" + tmp.toAbsolutePath().toString();
        Db fresh = new Db(url);
        TaskDao tdao = new TaskDao(fresh);
        List<Task> tasks = tdao.getAllTasks();
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
        try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("TaskDao(Db) when db is null should fail on use")
    public void taskDaoNullDb() {
        TaskDao td = new TaskDao(null);
        assertThrows(NullPointerException.class, () -> td.getAllTasks());
    }

    @Test
    @DisplayName("deleteTask passes correct Task object to Db")
    public void deletePassesCorrectObject() throws Exception {
        Path tmp = Files.createTempFile("taskdao-capture-", ".db");
        String url = "jdbc:sqlite:" + tmp.toAbsolutePath().toString();
        Db real = new Db(url);
        // ensure category
        real.addCategory(new Category("Cap", "d"));

        final Task[] captured = new Task[1];
        class CapturingDb extends Db {
            public CapturingDb() throws SQLException { super(url); }
            @Override
            public void deleteTask(Task task) throws SQLException { captured[0] = task; super.deleteTask(task); }
        }

        Db cdb = new CapturingDb();
        TaskDao tdao = new TaskDao(cdb);
        int id = tdao.createTask(new Task(0, "Name", "Content", "Cap", TaskStatus.NOT_STARTED, ""));
        assertTrue(id > 0);
        tdao.deleteTask(id);
        assertNotNull(captured[0]);
        assertEquals(id, captured[0].getId());
        try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
    }
}
