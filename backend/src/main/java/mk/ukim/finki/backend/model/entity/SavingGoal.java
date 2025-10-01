package mk.ukim.finki.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing a user saving goal.
 * A saving goal has a target amount and accumulates contributions (currentAmount).
 */
@Entity
@Table(name = "saving_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingGoal {

    /**
     * Unique identifier for the saving goal.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Owner of the saving goal. Cannot be null.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Human-readable name of the saving goal.
     * For example: "Vacation Fund" or "New Laptop".
     * Cannot be null or blank.
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Total target amount the user aims to save.
     * Must be greater than zero.
     */
    @Column(name = "target_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetAmount;

    /**
     * Current accumulated amount towards the goal.
     * Starts at zero and increases with contributions.
     */
    @Column(name = "current_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    /**
     * Optional deadline for achieving the saving goal.
     * Can be null if no deadline is set.
     */
    @Column
    private LocalDate deadline;

    /**
     * Whether the saving goal is achieved.
     */
    @Column(nullable = false)
    private boolean achieved = false;

    /**
     * Timestamp when the saving goal was created.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (currentAmount == null) currentAmount = BigDecimal.ZERO;
    }

    /**
     * Updates the achieved status based on current and target amounts.
     */
    public void updateAchievedStatus() {
        this.achieved = this.currentAmount != null
                && this.targetAmount != null
                && this.currentAmount.compareTo(this.targetAmount) >= 0;
    }
}
