package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;


/**
 * DTO representing monthly financial trends for charts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDto {

    /**
     * The period represented as YearMonth.
     */
    private YearMonth period;

    /**
     * Total income for the period.
     */
    private BigDecimal totalIncome;

    /**
     * Total expenses for the period.
     */
    private BigDecimal totalExpenses;

    /**
     * Savings for the period (income - expenses).
     */
    private BigDecimal savings;

    /**
     * Savings rate as percentage.
     */
    private BigDecimal savingsRate;
}
