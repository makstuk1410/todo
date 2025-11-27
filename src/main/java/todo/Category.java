/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package todo;

import todo.exceptions.CategoryValidationException;
import todo.exceptions.EmptyCategoryException;
import todo.exceptions.InvalidCategoryTypeException;
import todo.exceptions.NullCategoryException;

public class Category {

    private String name;
    private String description;

    public Category(String name, String description) {
        // Validate nulls
        if (name == null) throw new NullCategoryException("Category name must not be null");
        if (description == null) throw new NullCategoryException("Category description must not be null");

        // Validate emptiness
        if (name.isBlank()) throw new EmptyCategoryException("Category name must not be empty or blank");
        if (description.isBlank()) throw new EmptyCategoryException("Category description must not be empty or blank");

        this.name = name;
        this.description = description;
    }

    /**
     * Factory that accepts generic objects and enforces they are Strings.
     * Throws {@link InvalidCategoryTypeException} when supplied types are not String.
     */
    public static Category of(Object name, Object description) {
        if (!(name instanceof String)) throw new InvalidCategoryTypeException("Category name must be a String");
        if (!(description instanceof String)) throw new InvalidCategoryTypeException("Category description must be a String");
        return new Category((String) name, (String) description);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
