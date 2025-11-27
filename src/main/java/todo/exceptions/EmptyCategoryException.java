package todo.exceptions;

public class EmptyCategoryException extends CategoryValidationException {
    public EmptyCategoryException(String message) {
        super(message);
    }
}
