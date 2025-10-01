package mk.ukim.finki.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.saving_goal.*;
import mk.ukim.finki.backend.service.SavingGoalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing saving goals.
 * <p>
 * All endpoints operate on saving goals of the currently authenticated user.
 */
@RestController
@RequestMapping("/api/saving-goals")
@RequiredArgsConstructor
public class SavingGoalController {

    private final SavingGoalService savingGoalService;

    /**
     * Retrieves all saving goals for the authenticated user.
     *
     * @return list of saving goal DTOs
     */
    @GetMapping
    public List<SavingGoalDto> getAll() {
        return savingGoalService.getAllSavingGoals();
    }

    /**
     * Retrieves a specific saving goal by id.
     *
     * @param id saving goal id
     * @return saving goal DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<SavingGoalDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(savingGoalService.getSavingGoalById(id));
    }

    /**
     * Creates a new saving goal.
     *
     * @param request DTO containing goal creation data
     * @return created saving goal DTO
     */
    @PostMapping
    public ResponseEntity<SavingGoalDto> create(@Valid @RequestBody CreateSavingGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savingGoalService.createSavingGoal(request));
    }

    /**
     * Updates an existing saving goal.
     *
     * @param id      saving goal id
     * @param request DTO containing goal update data
     * @return updated saving goal DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<SavingGoalDto> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateSavingGoalRequest request) {
        return ResponseEntity.ok(savingGoalService.updateSavingGoal(id, request));
    }

    /**
     * Deletes a saving goal.
     *
     * @param id saving goal id
     * @return empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        savingGoalService.deleteSavingGoal(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a contribution to a saving goal.
     *
     * @param id      saving goal id
     * @param request DTO containing contribution amount
     * @return updated saving goal DTO
     */
    @PostMapping("/{id}/contribute")
    public ResponseEntity<SavingGoalDto> contribute(@PathVariable UUID id,
                                                    @Valid @RequestBody GoalContributionRequest request) {
        return ResponseEntity.ok(savingGoalService.addContribution(id, request));
    }

    /**
     * Withdraws an amount from a saving goal.
     *
     * @param id      saving goal id
     * @param request DTO containing withdrawal amount
     * @return updated saving goal DTO
     */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<SavingGoalDto> withdraw(@PathVariable UUID id,
                                                  @Valid @RequestBody GoalContributionRequest request) {
        return ResponseEntity.ok(savingGoalService.withdrawContribution(id, request));
    }

    /**
     * Returns contribution history for a saving goal.
     *
     * @param id saving goal id
     * @return list of contribution DTOs
     */
    @GetMapping("/{id}/contributions")
    public List<GoalContributionDto> getContributions(@PathVariable UUID id) {
        return savingGoalService.getContributions(id);
    }
}
