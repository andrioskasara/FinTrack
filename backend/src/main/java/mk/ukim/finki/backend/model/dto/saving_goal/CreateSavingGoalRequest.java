package mk.ukim.finki.backend.model.dto.saving_goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request for creating a saving goal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSavingGoalRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be greater than 0")
    private BigDecimal targetAmount;

    @FutureOrPresent(message = "Deadline cannot be in the past")
    private LocalDate deadline;
}
