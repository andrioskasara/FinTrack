package mk.ukim.finki.backend.service;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.exception.SavingGoalValidationException;
import mk.ukim.finki.backend.model.dto.saving_goal.*;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing saving goals.
 */
public interface SavingGoalService {

    /**
     * Returns all saving goals for the current authenticated user.
     *
     * @return list of saving goal DTOs ordered by creation date desc
     */
    List<SavingGoalDto> getAllSavingGoals();

    /**
     * Returns a saving goal owned by the current user.
     *
     * @param id goal id
     * @return saving goal DTO
     * @throws EntityNotFoundException if not owned/found
     */
    SavingGoalDto getSavingGoalById(UUID id);

    /**
     * Creates a new saving goal for the current user.
     *
     * @param request creation request
     * @return created saving goal DTO
     * @throws SavingGoalValidationException on invalid input
     */
    SavingGoalDto createSavingGoal(CreateSavingGoalRequest request);

    /**
     * Updates existing saving goal owned by the current user.
     *
     * @param id      goal id
     * @param request update request
     * @return updated saving goal DTO
     * @throws SavingGoalValidationException on invalid input
     */
    SavingGoalDto updateSavingGoal(UUID id, UpdateSavingGoalRequest request);

    /**
     * Deletes a saving goal owned by the current user.
     *
     * @param id goal id
     */
    void deleteSavingGoal(UUID id);

    /**
     * Adds a contribution (deposit) to a saving goal and stores a contribution record.
     *
     * @param id      goal id
     * @param request contribution request
     * @return updated saving goal DTO
     */
    SavingGoalDto addContribution(UUID id, GoalContributionRequest request);

    /**
     * Withdraws an amount from a saving goal and stores a contribution record of type WITHDRAWAL.
     *
     * @param id      goal id
     * @param request contribution request
     * @return updated saving goal DTO
     */
    SavingGoalDto withdrawContribution(UUID id, GoalContributionRequest request);

    /**
     * Returns contribution history for a saving goal owned by the current user ordered by newest first.
     *
     * @param savingGoalId goal id
     * @return list of contribution DTOs
     */
    List<GoalContributionDto> getContributions(UUID savingGoalId);
}
