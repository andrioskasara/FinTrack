package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.model.dto.transaction.IncomeDto;
import mk.ukim.finki.backend.model.dto.transaction.IncomeRequest;

/**
 * Service interface specific to Income entities.
 * Extends general CRUD transactional interface.
 */
public interface IncomeService extends TransactionService<IncomeDto, IncomeRequest> {
}
