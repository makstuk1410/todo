package todo.exceptions;

public class NullCategoryException extends CategoryValidationException {
    public NullCategoryException(String message) {
        super(message);
    }
}
