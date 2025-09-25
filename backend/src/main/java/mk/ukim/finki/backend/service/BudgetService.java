package mk.ukim.finki.backend.service;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.exception.BudgetValidationException;
import mk.ukim.finki.backend.model.dto.budget.BudgetDto;
import mk.ukim.finki.backend.model.dto.budget.CreateBudgetRequest;
import mk.ukim.finki.backend.model.dto.budget.UpdateBudgetRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing user budgets.
 */
public interface BudgetService {

    /**
     * Retrieves all budgets for the current user.
     *
     * @return list of budgets
     */
    List<BudgetDto> getAllBudgets();

    /**
     * Retrieves a single budget by ID for the current user
     *
     * @param id identifier of the budget
     * @return DTO representation of the budget
     * @throws EntityNotFoundException if budget does not exist
     */
    BudgetDto getBudgetById(UUID id);

    /**
     * Creates a new budget.
     *
     * @param request creation data
     * @return created budget DTO
     * @throws BudgetValidationException if overlapping budget exists
     */
    BudgetDto createBudget(CreateBudgetRequest request);

    /**
     * Updates an existing budget.
     *
     * @param id      budget id
     * @param request update data
     * @return updated budget DTO
     * @throws BudgetValidationException if overlapping budget exists
     */
    BudgetDto updateBudget(UUID id, UpdateBudgetRequest request);

    /**
     * Deletes a budget.
     *
     * @param id budget id
     */
    void deleteBudget(UUID id);

    /**
     * Archives all expired budgets for the current user.
     */
    void archiveExpiredBudgets();

    /**
     * Retrieves all expired (archived) budgets for the authenticated user.
     * <p>
     * Expired budgets are read-only and include their spent amounts and progress percentages.
     *
     * @return list of expired BudgetDto
     */
    List<BudgetDto> getExpiredBudgets();

    /**
     * Rolls over an existing budget into a new budget period.
     * <p>
     * The new budget will start immediately after the original budget's end date
     * and preserve the amount and category. Overlapping budgets are prevented.
     *
     * @param budgetId UUID of the budget to rollover
     * @return {@link BudgetDto} representing the newly created rollover budget
     * @throws EntityNotFoundException   if the original budget does not exist or does not belong to the current user
     * @throws BudgetValidationException if the rollover would overlap with an existing budget
     */
    BudgetDto rolloverBudget(UUID budgetId);
}
