package mk.ukim.finki.backend.util;

public final class CategoryServiceMessages {
    private CategoryServiceMessages() {
    }

    public static final String CATEGORY_NOT_FOUND = "Category not found";
    public static final String FALLBACK_NOT_FOUND = "Fallback category not found";
    public static final String DUPLICATE_CATEGORY_CREATE = "Category with this name and type already exists.";
    public static final String DUPLICATE_CATEGORY_UPDATE = "Duplicate category name for this type.";
    public static final String HIDE_NON_SYSTEM = "You can only hide system categories";
    public static final String ALREADY_HIDDEN = "Already hidden";
    public static final String HIDDEN_CATEGORY_NOT_FOUND = "Hidden category not found";
}
