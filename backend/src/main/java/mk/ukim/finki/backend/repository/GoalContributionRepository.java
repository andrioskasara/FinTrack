package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.entity.GoalContribution;
import mk.ukim.finki.backend.model.entity.SavingGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for reading and writing {@link GoalContribution} entities.
 */
public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

    /**
     * Returns all contributions for the given saving goal ordered by creation time descending (newest first).
     *
     * @param savingGoal saving goal entity
     * @return list of contribution records
     */
    List<GoalContribution> findBySavingGoalOrderByCreatedAtDesc(SavingGoal savingGoal);
}
