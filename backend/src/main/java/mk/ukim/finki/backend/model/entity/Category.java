package mk.ukim.finki.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import mk.ukim.finki.backend.model.enums.CategoryType;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a category.
 * <p>
 * A category can be:
 * <ul>
 *   <li><b>Predefined (system)</b>: Available to all users, not editable/deletable, can be hidden per user.
 *   <li><b>Custom</b>: Belongs to a single user; fully editable and deletable by the owner.</li>
 * </ul>
 * <p>
 * Uniqueness is enforced on (user, name, type).
 */
@Entity
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "name", "type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    /**
     * Unique identifier for category.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Owner of the category. Null for system (predefined) categories.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Name of the category (must be unique per user and type).
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Type of the category (EXPENSE or INCOME).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    /**
     * Whether the category is a system (predefined) category.
     */
    @Column(nullable = false)
    private boolean predefined = false;

    /**
     * Icon representing this category in UI.
     */
    @Column(length = 255)
    private String icon;

    /**
     * Visual color code for the category (e.g., "#FFAABB").
     */
    @Column(length = 7)
    private String colorCode;

    /**
     * Timestamp when the category was created.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}

