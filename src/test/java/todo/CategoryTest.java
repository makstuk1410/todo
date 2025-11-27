package todo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Category tests")
public class CategoryTest {

    @Test
    @DisplayName("Constructor and getters with normal values")
    public void constructorAndGetters_normal() {
        Category c = new Category("Work", "Tasks for work");
        assertEquals("Work", c.getName());
        assertEquals("Tasks for work", c.getDescription());
    }

    @Test
    @DisplayName("Constructor accepts null name")
    public void constructor_nullName() {
        assertThrows(todo.exceptions.NullCategoryException.class, () -> new Category(null, "some description"));
    }

    @Test
    @DisplayName("Constructor accepts null description")
    public void constructor_nullDescription() {
        assertThrows(todo.exceptions.NullCategoryException.class, () -> new Category("Home", null));
    }

    @Test
    @DisplayName("Constructor accepts empty strings")
    public void constructor_emptyStrings() {
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("", ""));
    }

    @Test
    @DisplayName("Constructor handles unicode and long strings")
    public void constructor_unicodeAndLong() {
        String longName = "n".repeat(1000);
        String unicode = "opisęł — тест — テスト";
        Category c = new Category(longName, unicode);
        assertEquals(longName, c.getName());
        assertEquals(unicode, c.getDescription());
    }

    @Test
    @DisplayName("Factory rejects non-string types")
    public void factoryRejectsNonString() {
        assertThrows(todo.exceptions.InvalidCategoryTypeException.class, () -> Category.of(123, "desc"));
        assertThrows(todo.exceptions.InvalidCategoryTypeException.class, () -> Category.of("name", 456));
    }

    @Test
    @DisplayName("Factory rejects when both arguments are non-String")
    public void factoryBothNonString() {
        assertThrows(todo.exceptions.InvalidCategoryTypeException.class, () -> Category.of(1, 2));
    }

    @Test
    @DisplayName("Factory rejects null arguments (name, description, or both)")
    public void factoryNullArguments() {
        assertThrows(todo.exceptions.InvalidCategoryTypeException.class, () -> Category.of(null, "desc"));
        assertThrows(todo.exceptions.InvalidCategoryTypeException.class, () -> Category.of("name", null));
        assertThrows(todo.exceptions.InvalidCategoryTypeException.class, () -> Category.of(null, null));
    }

    @Test
    @DisplayName("Factory accepts two Strings")
    public void factoryValidPath() {
        assertDoesNotThrow(() -> {
            Category c = Category.of("Personal", "My tasks");
            assertEquals("Personal", c.getName());
            assertEquals("My tasks", c.getDescription());
        });
    }

    @Test
    @DisplayName("Whitespace and tricky string variants")
    public void whitespaceAndTrickyVariants() {
        // Strings composed only of whitespace should be rejected
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category(" ", "desc"));
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("\t", "desc"));
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("\n", "desc"));
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("\r\n", "desc"));

        // Unicode spaces that are considered whitespace should be rejected
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("\u2003", "desc")); // em space

        // Names with surrounding whitespace but containing visible chars should be accepted and preserved
        assertDoesNotThrow(() -> {
            Category c = new Category("  Name  ", "Desc");
            assertEquals("  Name  ", c.getName());
        });

        // Names containing zero-width or non-breaking spaces are not considered blank by isBlank()
        assertDoesNotThrow(() -> new Category("\u200Bname", "desc")); // zero-width space + name
        assertDoesNotThrow(() -> new Category("name\u00A0", "desc")); // non-breaking space

        // Strings containing control characters along with other chars are accepted
        assertDoesNotThrow(() -> new Category("name\nmore", "desc"));
    }

    @Test
    @DisplayName("Constructor: one empty/blank and the other valid (4 variants)")
    public void constructorOneBlankOtherValid() {
        // name empty, description valid
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("", "Desc"));

        // name blank, description valid
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("  ", "Desc"));

        // name valid, description empty
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("Name", ""));

        // name valid, description blank
        assertThrows(todo.exceptions.EmptyCategoryException.class, () -> new Category("Name", "\t"));
    }
}
