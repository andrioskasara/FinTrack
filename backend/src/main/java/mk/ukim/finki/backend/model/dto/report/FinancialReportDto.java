package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents the full financial report/dashboard for a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialReportDto {

    /**
     * Start of the report period.
     */
    private LocalDate from;

    /**
     * End of the report period.
     */
    private LocalDate to;

    /**
     * Total income within the selected period.
     */
    private BigDecimal totalIncome;

    /**
     * Total expenses within the selected period.
     */
    private BigDecimal totalExpense;

    /**
     * Current balance (income - expenses).
     */
    private BigDecimal balance;

    /**
     * List of category-wise expense summaries.
     */
    private List<CategorySummaryDto> expenseByCategory;

    /**
     * List of category-wise income summaries.
     */
    private List<CategorySummaryDto> incomeByCategory;

    /**
     * List of budget reports for the user.
     */
    private List<BudgetReportDto> budgets;

    /**
     * List of saving goal reports for the user.
     */
    private List<SavingGoalReportDto> savingGoals;

    /**
     * Indicates whether the user has any meaningful data to display on the dashboard.
     * If true → onboarding view should be displayed instead of an empty dashboard.
     */
    private boolean emptyData;
}
