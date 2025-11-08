package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Budget performance overview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPerformanceDto {
    private int totalBudgets;
    private int onTrack; // progress < 90%
    private int atRisk; // progress between 90-100%
    private int exceeded; // progress > 100%
    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private BigDecimal overallUtilization; // percentage
    private List<BudgetReportDto> budgets;
}