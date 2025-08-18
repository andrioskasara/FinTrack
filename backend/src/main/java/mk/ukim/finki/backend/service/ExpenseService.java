package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.model.dto.transaction.ExpenseDto;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseRequest;

/**
 * Service interface specific to Expense entities.
 * Extends general CRUD transactional interface.
 */
public interface ExpenseService extends TransactionService<ExpenseDto, ExpenseRequest> {
}
