package mk.ukim.finki.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.transaction.IncomeDto;
import mk.ukim.finki.backend.model.dto.transaction.IncomeRequest;
import mk.ukim.finki.backend.service.IncomeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Income entities via an authenticated API.
 * Supports standard CRUD operations for the logged-in user's incomes.
 */
@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeController {
    private final IncomeService incomeService;

    /**
     * Retrieves all incomes for the current authenticated user, sorted descending by date and creation.
     *
     * @return list of Income DTOs.
     */
    @GetMapping
    public List<IncomeDto> getAllIncomes() {
        return incomeService.getAll();
    }

    /**
     * Retrieves a specific income by ID if owned by current user.
     *
     * @param id UUID of the income
     * @return HTTP 200 with Income DTO if found and authorized
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncomeDto> getIncome(@PathVariable UUID id) {
        IncomeDto dto = incomeService.getById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Creates a new income for the current user.
     *
     * @param request validated Income creation data
     * @return created Income DTO
     */
    @PostMapping
    public ResponseEntity<IncomeDto> createIncome(@Valid @RequestBody IncomeRequest request) {
        IncomeDto dto = incomeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Updates an existing income owned by the current user.
     *
     * @param id      UUID of the income to update
     * @param request validated update data
     * @return updated Income DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<IncomeDto> updateIncome(@PathVariable UUID id,
                                                  @Valid @RequestBody IncomeRequest request) {
        IncomeDto dto = incomeService.update(id, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * Deletes an income owned by current user.
     *
     * @param id UUID of the income to delete
     * @return HTTP 204 No Content upon successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable UUID id) {
        incomeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
