package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a user's budget report with progress and status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetReportDto {

    /**
     * Budget name (category or overall).
     */
    private String budgetName;

    /**
     * Total allocated budget amount.
     */
    private BigDecimal amount;

    /**
     * Total amount spent within this budget period.
     */
    private BigDecimal spent;

    /**
     * Progress percentage.
     */
    private Float progressPercentage;

    /**
     * Whether the user has exceeded the budget.
     */
    private boolean exceeded;
}

