package todo.exceptions;

public class InvalidCategoryTypeException extends CategoryValidationException {
    public InvalidCategoryTypeException(String message) {
        super(message);
    }
}
