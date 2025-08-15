package mk.ukim.finki.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import mk.ukim.finki.backend.model.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    /**
     * Unique identifier of the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Email of the user, unique and non-null.
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Hashed password of the user, non-null.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Role of the user (USER, ADMIN).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /**
     * Registration timestamp, immutable once set.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
