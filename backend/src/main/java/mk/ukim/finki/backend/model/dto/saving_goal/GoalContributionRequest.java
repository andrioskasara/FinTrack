package mk.ukim.finki.backend.model.dto.saving_goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request object for adding or withdrawing a contribution
 * to/from a saving goal.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalContributionRequest {

    /**
     * Amount to contribute or withdraw.
     * Must be greater than zero.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
}