package mk.ukim.finki.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import mk.ukim.finki.backend.model.enums.GoalContributionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a single contribution (deposit or withdrawal) towards a saving goal.
 * Stores a history record for audits, reporting and progress history.
 */
@Entity
@Table(name = "goal_contributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalContribution {

    /**
     * Unique identifier for the contribution record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Owning saving goal this contribution belongs to. Cannot be null.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saving_goal_id")
    private SavingGoal savingGoal;

    /**
     * Amount of the contribution (always positive).
     * For withdrawals the type indicates it's a withdrawal.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Whether this record is a deposit or withdrawal.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalContributionType type;

    /**
     * Timestamp when the contribution was created.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
    }
}
