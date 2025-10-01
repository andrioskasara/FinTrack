package mk.ukim.finki.backend.model.dto.saving_goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mk.ukim.finki.backend.model.enums.GoalContributionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for saving goals contributions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalContributionDto {
    private UUID id;
    private BigDecimal amount;
    private GoalContributionType type;
    private Instant createdAt;
}
