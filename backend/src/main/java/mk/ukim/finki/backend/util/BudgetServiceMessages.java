package mk.ukim.finki.backend.util;

public final class BudgetServiceMessages {

    private BudgetServiceMessages() {
    }

    public static final String CATEGORY_NOT_FOUND = "Category not found";
    public static final String BUDGET_NOT_FOUND = "Budget not found";
    public static final String BUDGET_END_BEFORE_START = "End date cannot be before start date";
    public static final String BUDGET_OVERLAP = "Overlapping budget exists for this category and period";
    public static final String BUDGET_ROLLOVER_ACTIVE = "Cannot rollover an active budget";
}
