package mk.ukim.finki.backend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.backend.exception.BudgetValidationException;
import mk.ukim.finki.backend.mapper.BudgetMapper;
import mk.ukim.finki.backend.model.dto.budget.BudgetDto;
import mk.ukim.finki.backend.model.dto.budget.CreateBudgetRequest;
import mk.ukim.finki.backend.model.dto.budget.UpdateBudgetRequest;
import mk.ukim.finki.backend.model.entity.Budget;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.repository.BudgetRepository;
import mk.ukim.finki.backend.repository.CategoryRepository;
import mk.ukim.finki.backend.service.BudgetService;
import mk.ukim.finki.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static mk.ukim.finki.backend.util.BudgetServiceMessages.*;

/**
 * Service implementation for {@link BudgetService}.
 * Handles CRUD operations, progress calculation, and archiving of expired budgets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;

    /**
     * Retrieves a category by ID or throws an exception if not found.
     *
     * @param id category ID
     * @return the Category entity
     */
    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CATEGORY_NOT_FOUND));
    }

    /**
     * Validates that startDate is before or equal to endDate.
     *
     * @param startDate start date
     * @param endDate   end date
     */
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BudgetValidationException(BUDGET_END_BEFORE_START);
        }
    }

    /**
     * Checks for overlapping budgets for a given user and category.
     *
     * @param user      budget owner
     * @param category  category
     * @param start     start date
     * @param end       end date
     * @param excludeId budget ID to exclude (for update)
     */
    private void checkOverlappingBudgets(User user, Category category, LocalDate start, LocalDate end, UUID excludeId) {
        List<Budget> overlapping = budgetRepository.findOverlappingBudgets(user, category, start, end)
                .stream()
                .filter(b -> !b.isArchived())
                .filter(b -> !b.getId().equals(excludeId))
                .toList();

        if (!overlapping.isEmpty())
            throw new BudgetValidationException(BUDGET_OVERLAP);
    }

    /**
     * Calculates the progress percentage of a budget based on actual spending.
     *
     * @param budget Budget entity
     * @return budget with updated progressPercentage
     */
    private Budget calculateProgress(Budget budget) {
        BigDecimal spent = Optional.ofNullable(
                budgetRepository.sumSpentByBudget(
                        budget.getUser(),
                        budget.getCategory(),
                        budget.getStartDate(),
                        budget.getEndDate())
        ).orElse(BigDecimal.ZERO);

        BigDecimal progress = budget.getAmount().compareTo(BigDecimal.ZERO) > 0
                ? spent.divide(budget.getAmount(), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        budget.setProgressPercentage(Math.min(progress.floatValue(), 100f));
        return budget;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetDto> getAllBudgets() {
        User user = userService.getCurrentUser();
        archiveExpiredBudgets();

        return budgetRepository.findByUserOrderByStartDateDesc(user)
                .stream()
                .map(this::calculateProgress)
                .map(budgetMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetDto getBudgetById(UUID id) {
        User user = userService.getCurrentUser();

        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException(BUDGET_NOT_FOUND));

        return budgetMapper.toDto(calculateProgress(budget));
    }

    @Override
    @Transactional
    public BudgetDto createBudget(CreateBudgetRequest request) {
        User user = userService.getCurrentUser();

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        validateDates(startDate, endDate);

        Category category = findCategoryOrThrow(request.getCategoryId());
        checkOverlappingBudgets(user, category, startDate, endDate, null);

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .startDate(startDate)
                .endDate(endDate)
                .archived(false)
                .isRollover(request.isRollover())
                .build();

        budgetRepository.save(budget);

        log.info("User [{}] created budget for category [{}]: {} - {} amount={}",
                user.getEmail(), category.getName(), startDate, endDate, request.getAmount());

        return budgetMapper.toDto(calculateProgress(budget));
    }

    @Override
    @Transactional
    public BudgetDto updateBudget(UUID id, UpdateBudgetRequest request) {
        User user = userService.getCurrentUser();

        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException(BUDGET_NOT_FOUND));

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        validateDates(startDate, endDate);

        Category category = findCategoryOrThrow(request.getCategoryId());
        checkOverlappingBudgets(user, category, startDate, endDate, id);

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setStartDate(startDate);
        budget.setEndDate(endDate);
        budget.setRollover(request.isRollover());
        budget.setArchived(request.isArchived());

        budgetRepository.save(budget);

        log.info("User [{}] updated budget ID [{}] for category [{}]: {} - {} amount={}",
                user.getEmail(), budget.getId(), category.getName(), startDate, endDate, request.getAmount());

        return budgetMapper.toDto(calculateProgress(budget));
    }

    @Override
    @Transactional
    public void deleteBudget(UUID id) {
        User user = userService.getCurrentUser();

        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException(BUDGET_NOT_FOUND));

        budgetRepository.delete(budget);

        log.info("User [{}] deleted budget ID [{}] for category [{}]",
                user.getEmail(), budget.getId(), budget.getCategory().getName());
    }

    @Override
    @Transactional
    public void archiveExpiredBudgets() {
        User user = userService.getCurrentUser();

        List<Budget> expired = budgetRepository.findByUserOrderByStartDateDesc(user)
                .stream()
                .filter(b -> !b.isArchived() && b.getEndDate().isBefore(LocalDate.now()))
                .toList();

        if (!expired.isEmpty()) {
            expired.forEach(b -> b.setArchived(true));
            budgetRepository.saveAll(expired);

            expired.forEach(b ->
                    log.info("Archived expired budget ID [{}] for category [{}]",
                            b.getId(), b.getCategory().getName())
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetDto> getExpiredBudgets() {
        User user = userService.getCurrentUser();
        List<Budget> expiredBudgets = budgetRepository.findByUserAndArchivedTrueOrderByEndDateDesc(user);

        return expiredBudgets.stream()
                .map(this::calculateProgress)
                .map(budgetMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BudgetDto rolloverBudget(UUID budgetId) {
        User user = userService.getCurrentUser();
        Budget oldBudget = budgetRepository.findByIdAndUser(budgetId, user)
                .orElseThrow(() -> new EntityNotFoundException(BUDGET_NOT_FOUND));

        if (oldBudget.getEndDate().isAfter(LocalDate.now())) {
            throw new BudgetValidationException(BUDGET_ROLLOVER_ACTIVE);
        }

        var period = calculateRolloverPeriod(oldBudget);

        checkOverlappingBudgets(user, oldBudget.getCategory(), period.start(), period.end(), null);

        Budget newBudget = Budget.builder()
                .user(user)
                .category(oldBudget.getCategory())
                .amount(oldBudget.getAmount())
                .startDate(period.start())
                .endDate(period.end())
                .archived(false)
                .isRollover(true)
                .build();

        budgetRepository.save(newBudget);
        return budgetMapper.toDto(newBudget);
    }

    private record RolloverPeriod(LocalDate start, LocalDate end) {
    }

    private RolloverPeriod calculateRolloverPeriod(Budget oldBudget) {
        boolean isFullMonth = oldBudget.getStartDate().getDayOfMonth() == 1 &&
                oldBudget.getEndDate().getDayOfMonth() == oldBudget.getEndDate().lengthOfMonth();

        if (isFullMonth) {
            LocalDate start = oldBudget.getStartDate().plusMonths(1).withDayOfMonth(1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return new RolloverPeriod(start, end);
        } else {
            long days = ChronoUnit.DAYS.between(oldBudget.getStartDate(), oldBudget.getEndDate()) + 1;
            LocalDate start = oldBudget.getEndDate().plusDays(1);
            LocalDate end = start.plusDays(days - 1);
            return new RolloverPeriod(start, end);
        }
    }
}
