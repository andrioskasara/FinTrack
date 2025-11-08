package mk.ukim.finki.backend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.backend.mapper.ExpenseMapper;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseDto;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseRequest;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.Expense;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.repository.CategoryRepository;
import mk.ukim.finki.backend.repository.ExpenseRepository;
import mk.ukim.finki.backend.service.ExpenseService;
import mk.ukim.finki.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static mk.ukim.finki.backend.util.TransactionServiceMessages.EXPENSE_NOT_FOUND;

/**
 * Expense service implementation extending base financial transaction logic.
 */
@Slf4j
@Service
public class ExpenseServiceImpl extends AbstractTransactionService<Expense> implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                              CategoryRepository categoryRepository,
                              ExpenseMapper expenseMapper,
                              UserService userService) {
        super(categoryRepository, userService);
        this.expenseRepository = expenseRepository;
        this.expenseMapper = expenseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseDto> getAll() {
        User user = userService.getCurrentUser();
        List<Expense> expenses = expenseRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(user.getId());

        return expenses.stream()
                .map(expenseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseDto getById(UUID id) {
        User user = userService.getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(EXPENSE_NOT_FOUND));
        validateOwnership(expense, user);
        return expenseMapper.toDto(expense);
    }

    @Override
    @Transactional
    public ExpenseDto create(ExpenseRequest request) {
        User user = userService.getCurrentUser();
        Category category = findCategoryOrThrow(request.getCategoryId());
        validateCategoryOwnership(category, user);

        Expense expense = Expense.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .build();

        expenseRepository.save(expense);

        log.info("User [{}] created an expense: amount={}, category={}, date={}",
                user.getEmail(), expense.getAmount(), category.getName(), expense.getDate());

        return expenseMapper.toDto(expense);
    }

    @Override
    @Transactional
    public ExpenseDto update(UUID id, ExpenseRequest request) {
        User user = userService.getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(EXPENSE_NOT_FOUND));
        validateOwnership(expense, user);

        Category category = findCategoryOrThrow(request.getCategoryId());
        validateCategoryOwnership(category, user);

        expense.setAmount(request.getAmount());
        expense.setCategory(category);
        expense.setDate(request.getDate());
        expense.setDescription(request.getDescription());

        expenseRepository.save(expense);

        log.info("User [{}] updated expense ID [{}]: amount={}, category={}, date={}",
                user.getEmail(), id, expense.getAmount(), category.getName(), expense.getDate());

        return expenseMapper.toDto(expense);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User user = userService.getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(EXPENSE_NOT_FOUND));
        validateOwnership(expense, user);

        expenseRepository.delete(expense);

        log.info("User [{}] deleted expense ID [{}], amount={}, category={}, date={}",
                user.getEmail(), id, expense.getAmount(), expense.getCategory().getName(), expense.getDate());
    }
}
