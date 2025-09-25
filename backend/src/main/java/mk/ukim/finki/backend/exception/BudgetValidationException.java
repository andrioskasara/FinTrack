package mk.ukim.finki.backend.exception;

public class BudgetValidationException extends RuntimeException {
    public BudgetValidationException(String message) {
        super(message);
    }
}
