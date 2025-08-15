package mk.ukim.finki.backend.exception;

/**
 * Exception thrown for hidden category business rule violations (e.g. hiding a custom category).
 */
public class HiddenCategoryException extends CategoryException {
    public HiddenCategoryException(String message) {
        super(message);
    }
}
