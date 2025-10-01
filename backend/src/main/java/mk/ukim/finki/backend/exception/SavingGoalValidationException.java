package mk.ukim.finki.backend.exception;

/**
 * Thrown when a saving goal operation violates business rules.
 */
public class SavingGoalValidationException extends RuntimeException {
    public SavingGoalValidationException(String message) {
        super(message);
    }
}