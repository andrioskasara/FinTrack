package mk.ukim.finki.backend.exception;

/**
 * Base class for all category-related business exceptions.
 */
public class CategoryException extends RuntimeException {
    public CategoryException(String message) {
        super(message);
    }
}
