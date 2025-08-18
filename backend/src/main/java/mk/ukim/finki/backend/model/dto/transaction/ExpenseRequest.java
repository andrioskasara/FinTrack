package mk.ukim.finki.backend.model.dto.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Expense request model used for create/update requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {
    @NotNull
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull
    private UUID categoryId;

    @NotNull
    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate date;

    private String description;
}
