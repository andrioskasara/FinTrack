package mk.ukim.finki.backend.exception;

/**
 * Exception thrown when a user tries to access or modify a category they do not own or that is not allowed.
 */
public class UnauthorizedCategoryAccessException extends CategoryException {
    public UnauthorizedCategoryAccessException() {
        super("No permission to access this category");
    }
}
