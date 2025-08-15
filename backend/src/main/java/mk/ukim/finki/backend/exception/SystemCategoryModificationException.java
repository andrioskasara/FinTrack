package mk.ukim.finki.backend.exception;

/**
 * Exception thrown when a system (predefined) category modification is attempted via forbidden operations.
 */
public class SystemCategoryModificationException extends CategoryException {
    public SystemCategoryModificationException() {
        super("System categories cannot be modified");
    }
}
