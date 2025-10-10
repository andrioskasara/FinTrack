package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a saving goal report for the user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingGoalReportDto {

    /**
     * Name of the saving goal.
     */
    private String name;

    /**
     * Target amount to save.
     */
    private BigDecimal targetAmount;

    /**
     * Current amount saved.
     */
    private BigDecimal currentAmount;

    /**
     * Progress percentage.
     */
    private Float progressPercentage;

    /**
     * Whether the goal has been achieved.
     */
    private boolean achieved;
}
