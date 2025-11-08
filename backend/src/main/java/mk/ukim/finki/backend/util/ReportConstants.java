package mk.ukim.finki.backend.util;


/**
 * Constants for report and analytics feature.
 */
public final class ReportConstants {
    private ReportConstants() {
    }

    // Date range presets
    public static final String DATE_RANGE_LAST_30_DAYS = "LAST_30_DAYS";
    public static final String DATE_RANGE_THIS_MONTH = "THIS_MONTH";
    public static final String DATE_RANGE_LAST_MONTH = "LAST_MONTH";
    public static final String DATE_RANGE_THIS_YEAR = "THIS_YEAR";
    public static final String DATE_RANGE_CUSTOM = "CUSTOM";

    // Transaction types
    public static final String TRANSACTION_TYPE_EXPENSE = "EXPENSE";
    public static final String TRANSACTION_TYPE_INCOME = "INCOME";
    public static final String TRANSACTION_TYPE_ALL = "ALL";

    // Group by periods
    public static final String GROUP_BY_DAY = "DAY";
    public static final String GROUP_BY_WEEK = "WEEK";
    public static final String GROUP_BY_MONTH = "MONTH";
    public static final String GROUP_BY_YEAR = "YEAR";

    // Sort options
    public static final String SORT_BY_AMOUNT = "AMOUNT";
    public static final String SORT_BY_DATE = "DATE";
    public static final String SORT_BY_CATEGORY = "CATEGORY";

    // Error messages
    public static final String ERROR_DATE_RANGE_NULL = "Date range cannot be null";
    public static final String ERROR_START_AFTER_END = "Start date cannot be after end date";
    public static final String ERROR_FUTURE_START_DATE = "Start date cannot be in the future";
    public static final String ERROR_PDF_GENERATION_FAILED = "Failed to generate PDF report";

    // File names
    public static final String PDF_FILENAME_FORMAT = "financial-report-%s-to-%s.pdf";
    public static final String CSV_FILENAME_FORMAT = "financial-data-%s-to-%s.csv";

    // CSV headers
    public static final String CSV_HEADER = "Category,Type,Amount\n";
    public static final String CSV_ROW_FORMAT = "\"%s\",\"%s\",%.2f\n";
}