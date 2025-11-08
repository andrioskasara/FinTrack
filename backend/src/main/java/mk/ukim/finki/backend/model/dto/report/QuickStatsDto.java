package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Quick stats for dashboard cards
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickStatsDto {
    private BigDecimal currentBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal netCashFlow;
    private BigDecimal savingsRate;
    private int activeBudgets;
    private int activeSavingGoals;
    private BigDecimal totalSavingsProgress;
}