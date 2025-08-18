package mk.ukim.finki.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseDto;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseRequest;
import mk.ukim.finki.backend.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Expense entities via an authenticated API.
 * Supports standard CRUD operations for the logged-in user's expenses.
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    /**
     * Retrieves all expenses for the current authenticated user, sorted descending by date and creation.
     *
     * @return list of Expense DTOs.
     */
    @GetMapping
    public List<ExpenseDto> getAllExpenses() {
        return expenseService.getAll();
    }

    /**
     * Retrieves a specific expense by ID if owned by current user.
     *
     * @param id UUID of the expense
     * @return HTTP 200 with Expense DTO if found and authorized
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDto> getExpense(@PathVariable UUID id) {
        ExpenseDto dto = expenseService.getById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Creates a new expense for the current user.
     *
     * @param request validated Expense creation data
     * @return created Expense DTO
     */
    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(@Valid @RequestBody ExpenseRequest request) {
        ExpenseDto dto = expenseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Updates an existing expense owned by the current user.
     *
     * @param id UUID of the expense to update
     * @param request validated update data
     * @return updated Expense DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDto> updateExpense(@PathVariable UUID id,
                                                    @Valid @RequestBody ExpenseRequest request) {
        ExpenseDto dto = expenseService.update(id, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * Deletes an expense owned by current user.
     *
     * @param id UUID of the expense to delete
     * @return HTTP 204 No Content upon successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
