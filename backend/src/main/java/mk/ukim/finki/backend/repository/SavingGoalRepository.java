package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.entity.SavingGoal;
import mk.ukim.finki.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing saving goals.
 */
public interface SavingGoalRepository extends JpaRepository<SavingGoal, UUID> {

    /**
     * Finds all saving goals for a user, ordered by creation timestamp descending.
     *
     * @param user the owner of the saving goals
     * @return list of saving goals
     */
    List<SavingGoal> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Finds a saving goal by its ID and user.
     *
     * @param id   the goal identifier
     * @param user the owner of the goal
     * @return optional containing the goal if found, empty otherwise
     */
    Optional<SavingGoal> findByIdAndUser(UUID id, User user);
}
