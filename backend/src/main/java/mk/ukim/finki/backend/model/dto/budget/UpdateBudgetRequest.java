package mk.ukim.finki.backend.model.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for updating an existing budget.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBudgetRequest {
    private UUID categoryId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Budget amount must be greater than 0")
    private BigDecimal amount;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private boolean isRollover = false;

    private boolean archived = false;
}
