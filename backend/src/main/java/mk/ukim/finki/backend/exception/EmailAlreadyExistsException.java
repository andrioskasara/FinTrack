package mk.ukim.finki.backend.exception;

/**
 * Exception thrown when trying to register a user with an email that already exists.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
