package mk.ukim.finki.backend.exception;

/**
 * Thrown when a user tries to access/modify a saving goal they don't own.
 */
public class UnauthorizedSavingGoalAccessException extends SecurityException {
    public UnauthorizedSavingGoalAccessException() {
        super("No permission to access this saving goal");
    }
}
