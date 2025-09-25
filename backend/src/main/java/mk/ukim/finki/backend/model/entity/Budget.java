package mk.ukim.finki.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing a budget.
 * <p>
 * A budget can be assigned to a specific category or be general.
 */
@Entity
@Table(name = "budgets",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "category_id", "start_date", "end_date"},
                name = "uq_budgets_user_category_period"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    /**
     * Unique identifier for the budget.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Owner of the budget. Cannot be null.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Category this budget applies to. Null = overall/general budget.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Amount allocated for this budget.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Start date of the budget period.
     */
    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * End date of the budget period.
     */
    @Column(nullable = false)
    private LocalDate endDate;

    /**
     * Whether unspent budget rolls over to next period.
     */
    @Column(nullable = false)
    private boolean isRollover = false;

    /**
     * Whether the budget is archived (expired or manually archived).
     */
    @Column(nullable = false)
    private boolean archived = false;

    /**
     * Progress of budget usage in percentage (0-100).
     * Progress is calculated dynamically.
     */
    @Transient
    private Float progressPercentage;

    /**
     * Timestamp when the budget was created.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
