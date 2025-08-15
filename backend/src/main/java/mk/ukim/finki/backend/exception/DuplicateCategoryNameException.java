package mk.ukim.finki.backend.exception;

/**
 * Exception thrown on duplicate category name per user/type.
 */
public class DuplicateCategoryNameException extends CategoryException {
    public DuplicateCategoryNameException(String message) {
        super(message);
    }
}
