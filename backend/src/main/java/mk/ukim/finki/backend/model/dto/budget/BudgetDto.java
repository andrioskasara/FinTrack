package mk.ukim.finki.backend.model.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for transferring budget data to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetDto {
    private UUID id;

    private UUID categoryId;

    private String categoryName;

    private BigDecimal amount;

    private LocalDate startDate;

    private LocalDate endDate;

    private Float progressPercentage;

    private boolean isRollover;

    private boolean archived;
}
