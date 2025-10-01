package mk.ukim.finki.backend.model.dto.saving_goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for saving goals.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingGoalDto {
    private UUID id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private Float progressPercentage;
    private LocalDate deadline;
    private boolean achieved;
}
