package todo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryDao tests")
public class CategoryDaoTest {
    private Path tmpDbFile;
    private Db db;
    private CategoryDao dao;

    @BeforeEach
    public void setUp() throws Exception {
        tmpDbFile = Files.createTempFile("test-db-", ".db");
        String url = "jdbc:sqlite:" + tmpDbFile.toAbsolutePath().toString();
        db = new Db(url);
        dao = new CategoryDao(db);
    }

    @AfterEach
    public void tearDown() throws Exception {
        try {
            Files.deleteIfExists(tmpDbFile);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Create and list categories")
    public void createAndList() {
        List<Category> before = dao.getAllCategories();
        assertNotNull(before);
        assertTrue(before.isEmpty());

        dao.createCategory(new Category("Work", "Work tasks"));
        List<Category> after = dao.getAllCategories();
        assertEquals(1, after.size());
        assertEquals("Work", after.get(0).getName());
        assertEquals("Work tasks", after.get(0).getDescription());
    }

    @Test
    @DisplayName("Update category description")
    public void updateCategory() {
        dao.createCategory(new Category("Home", "initial"));
        List<Category> cur = dao.getAllCategories();
        assertEquals(1, cur.size());

        dao.updateCategory(new Category("Home", "updated"));
        List<Category> updated = dao.getAllCategories();
        assertEquals(1, updated.size());
        assertEquals("updated", updated.get(0).getDescription());
    }

    @Test
    @DisplayName("Delete category")
    public void deleteCategory() {
        dao.createCategory(new Category("Tmp", "d"));
        assertEquals(1, dao.getAllCategories().size());
        dao.deleteCategory("Tmp");
        assertTrue(dao.getAllCategories().isEmpty());
    }

    @Test
    @DisplayName("Creating duplicate category throws")
    public void duplicateThrows() {
        dao.createCategory(new Category("Dup", "one"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.createCategory(new Category("Dup", "two")));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof java.sql.SQLException);
    }

    @Test
    @DisplayName("DB read failure is wrapped in RuntimeException")
    public void dbReadFailureWrapped() throws Exception {
        // make a Db whose getCategories throws SQLException
        class FailingDb extends Db {
            public FailingDb() throws java.sql.SQLException { super(); }
            @Override
            public java.util.ArrayList<Category> getCategories() throws java.sql.SQLException {
                throw new java.sql.SQLException("Simulated read failure");
            }
        }

        Db failing = new FailingDb();
        CategoryDao failingDao = new CategoryDao(failing);
        RuntimeException rex = assertThrows(RuntimeException.class, () -> failingDao.getAllCategories());
        assertNotNull(rex.getCause());
        assertTrue(rex.getCause() instanceof java.sql.SQLException);
        assertTrue(rex.getCause().getMessage().contains("Simulated read failure"));
    }

    @Test
    @DisplayName("DB write failure (insert/update/delete) is wrapped in RuntimeException")
    public void dbWriteFailuresWrapped() throws Exception {
        class FailingDb extends Db {
            public FailingDb() throws java.sql.SQLException { super(); }
            @Override
            public void addCategory(Category c) throws java.sql.SQLException { throw new java.sql.SQLException("Simulated insert failure"); }
            @Override
            public void updateCategory(Category c) throws java.sql.SQLException { throw new java.sql.SQLException("Simulated update failure"); }
            @Override
            public void deleteCategory(String name) throws java.sql.SQLException { throw new java.sql.SQLException("Simulated delete failure"); }
        }

        Db failing = new FailingDb();
        CategoryDao failingDao = new CategoryDao(failing);

        RuntimeException rex1 = assertThrows(RuntimeException.class, () -> failingDao.createCategory(new Category("X","x")));
        assertTrue(rex1.getCause() instanceof java.sql.SQLException);

        RuntimeException rex2 = assertThrows(RuntimeException.class, () -> failingDao.updateCategory(new Category("X","y")));
        assertTrue(rex2.getCause() instanceof java.sql.SQLException);

        RuntimeException rex3 = assertThrows(RuntimeException.class, () -> failingDao.deleteCategory("X"));
        assertTrue(rex3.getCause() instanceof java.sql.SQLException);
    }

    @Test
    @DisplayName("DB locked error propagates message")
    public void dbLockedPropagation() throws Exception {
        class LockedDb extends Db {
            public LockedDb() throws java.sql.SQLException { super(); }
            @Override
            public void addCategory(Category c) throws java.sql.SQLException { throw new java.sql.SQLException("database is locked"); }
        }
        Db locked = new LockedDb();
        CategoryDao d = new CategoryDao(locked);
        RuntimeException rex = assertThrows(RuntimeException.class, () -> d.createCategory(new Category("L","l")));
        assertNotNull(rex.getCause());
        assertTrue(rex.getCause().getMessage().toLowerCase().contains("locked"));
    }

    @Test
    @DisplayName("CategoryDao() when Db() constructor throws SQLException")
    public void defaultConstructorDbThrows() throws Exception {
        String originalHome = System.getProperty("user.home");
        Path badHome = Files.createTempFile("badhome-cat-", ".tmp");
        try {
            System.setProperty("user.home", badHome.toAbsolutePath().toString());
            RuntimeException ex = assertThrows(RuntimeException.class, () -> new CategoryDao());
            assertNotNull(ex.getCause());
            assertTrue(ex.getCause() instanceof java.sql.SQLException);
        } finally {
            System.setProperty("user.home", originalHome);
            try { Files.deleteIfExists(badHome); } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("CategoryDao(Db) with null db: first use throws NPE")
    public void categoryDaoNullDbFirstUse() {
        CategoryDao cd = new CategoryDao(null);
        assertThrows(NullPointerException.class, () -> cd.getAllCategories());
    }

    @Test
    @DisplayName("createCategory with null argument")
    public void createCategoryNull() {
        assertThrows(NullPointerException.class, () -> dao.createCategory(null));
    }

    @Test
    @DisplayName("createCategory with incomplete data (null or empty name)")
    public void createCategoryIncomplete() {
        // null name -> Category constructor throws NullCategoryException
        assertThrows(todo.exceptions.NullCategoryException.class, () -> dao.createCategory(new Category(null, "d")));

        // empty name -> EmptyCategoryException
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> dao.createCategory(new Category("", "d")));
    }

    @Test
    @DisplayName("deleteCategory null/empty/unknown")
    public void deleteCategoryEdgeCases() {
        dao.createCategory(new Category("Exist", "x"));
        assertEquals(1, dao.getAllCategories().size());

        // delete null should not remove existing
        assertDoesNotThrow(() -> dao.deleteCategory(null));
        assertEquals(1, dao.getAllCategories().size());

        // delete empty string should not remove existing
        assertDoesNotThrow(() -> dao.deleteCategory(""));
        assertEquals(1, dao.getAllCategories().size());

        // delete unknown name should not remove existing
        assertDoesNotThrow(() -> dao.deleteCategory("unknown"));
        assertEquals(1, dao.getAllCategories().size());
    }

    @Test
    @DisplayName("updateCategory null or invalid data")
    public void updateCategoryNullAndInvalid() {
        // null category to update -> NPE
        assertThrows(NullPointerException.class, () -> dao.updateCategory(null));

        // invalid categories should be rejected at construction
        assertThrows(todo.exceptions.NullCategoryException.class, () -> dao.updateCategory(new Category(null, "d")));
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> dao.updateCategory(new Category("", "d")));
    }
}
