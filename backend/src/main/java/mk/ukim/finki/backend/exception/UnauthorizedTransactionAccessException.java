package mk.ukim.finki.backend.exception;

/**
 * Exception for unauthorized access attempts.
 */
public class UnauthorizedTransactionAccessException extends RuntimeException {

    public UnauthorizedTransactionAccessException() {
        super("No permission to access this transaction");
    }

    public UnauthorizedTransactionAccessException(String message) {
        super(message);
    }
}
