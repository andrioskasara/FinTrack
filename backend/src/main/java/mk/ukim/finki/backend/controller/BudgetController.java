package mk.ukim.finki.backend.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.exception.BudgetValidationException;
import mk.ukim.finki.backend.model.dto.budget.BudgetDto;
import mk.ukim.finki.backend.model.dto.budget.CreateBudgetRequest;
import mk.ukim.finki.backend.model.dto.budget.UpdateBudgetRequest;
import mk.ukim.finki.backend.service.BudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing budgets via an authenticated API.
 * <p>
 * Supports standard CRUD operations and budget-specific actions.
 * All endpoints operate on the budgets of the currently authenticated user.
 */
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Retrieves all budgets of the current authenticated user.
     *
     * @return list of Budget DTOs
     */
    @GetMapping
    public List<BudgetDto> getAll() {
        return budgetService.getAllBudgets();
    }

    /**
     * Retrieves a specific budget by ID if owned by the current user.
     *
     * @param id UUID of the budget
     * @return HTTP 200 with Budget DTO if found
     * @throws EntityNotFoundException if the budget does not exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto> getById(@PathVariable UUID id) {
        BudgetDto dto = budgetService.getBudgetById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Creates a new budget for the current user.
     *
     * @param request validated budget creation data
     * @return HTTP 201 Created with the created Budget DTO
     * @throws BudgetValidationException                   if the request data is invalid (e.g., overlapping budgets or invalid dates)
     * @throws jakarta.persistence.EntityNotFoundException if the category does not exist
     */
    @PostMapping
    public ResponseEntity<BudgetDto> create(@Valid @RequestBody CreateBudgetRequest request) {
        BudgetDto dto = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Updates an existing budget owned by the current user.
     *
     * @param id      UUID of the budget to update
     * @param request validated update data
     * @return HTTP 200 OK with the updated Budget DTO
     * @throws BudgetValidationException if the update violates business rules (e.g., overlapping budgets)
     * @throws EntityNotFoundException   if the budget or category does not exist
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    /**
     * Deletes a budget owned by the current user.
     *
     * @param id UUID of the budget to delete
     * @return HTTP 204 No Content if deletion was successful
     * @throws EntityNotFoundException if the budget does not exist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Archives all expired budgets of the current user.
     * <p>
     * Expired budgets are those with an end date before the current date.
     *
     * @return HTTP 204 No Content when archiving is complete
     */
    @PostMapping("/archive")
    public ResponseEntity<Void> archiveExpired() {
        budgetService.archiveExpiredBudgets();
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all expired (archived) budgets for the authenticated user.
     * <p>
     * Expired budgets are read-only and include spent amounts and progress percentages.
     *
     * @return list of expired BudgetDto
     */
    @GetMapping("/expired")
    public List<BudgetDto> getExpiredBudgets() {
        return budgetService.getExpiredBudgets();
    }

    /**
     * Rolls over an existing budget into a new budget period.
     * The new budget starts the day after the old budget's end date.
     *
     * @param id UUID of the budget to rollover
     * @return HTTP 201 Created with the new Budget DTO
     */
    @PostMapping("/{id}/rollover")
    public ResponseEntity<BudgetDto> rollover(@PathVariable UUID id) {
        BudgetDto dto = budgetService.rolloverBudget(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
