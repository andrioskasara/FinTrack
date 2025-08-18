package mk.ukim.finki.backend.model.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing an Income entity in responses.
 * Contains monetary amount, category details, date, description, and timestamps.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeDto {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate date;
    private String description;
    private Instant createdAt;
}
