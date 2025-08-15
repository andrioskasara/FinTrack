package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Finds a user by email.
     *
     * @param email user's email
     * @return Optional user
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with given email.
     *
     * @param email user's email
     * @return true if exists
     */
    boolean existsByEmail(String email);
}
