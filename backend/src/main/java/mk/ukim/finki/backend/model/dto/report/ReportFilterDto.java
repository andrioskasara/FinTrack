package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterDto {
    private LocalDate from;
    private LocalDate to;
    private List<String> categories;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String transactionType; // "EXPENSE", "INCOME", or "ALL"
    private String groupBy; // "DAY", "WEEK", "MONTH", "YEAR"
    private String sortBy; // "AMOUNT", "DATE", "CATEGORY"
    private boolean includeBudgets;
    private boolean includeSavingGoals;
}