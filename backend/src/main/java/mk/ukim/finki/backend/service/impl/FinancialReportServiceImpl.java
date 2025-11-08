package mk.ukim.finki.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.backend.model.dto.report.*;
import mk.ukim.finki.backend.model.entity.Budget;
import mk.ukim.finki.backend.model.entity.SavingGoal;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.model.enums.CategoryType;
import mk.ukim.finki.backend.repository.*;
import mk.ukim.finki.backend.service.FinancialReportService;
import mk.ukim.finki.backend.service.UserService;
import mk.ukim.finki.backend.util.PdfExportUtil;
import mk.ukim.finki.backend.util.ReportConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for financial reports and analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialReportServiceImpl implements FinancialReportService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final UserService userService;

    @Override
    public FinancialReportDto generateDashboard(LocalDate from, LocalDate to) {
        log.info("Generating dashboard for period: {} to {}", from, to);
        validateDateRange(from, to);

        User user = userService.getCurrentUser();

        BigDecimal totalExpense = Optional.ofNullable(
                        expenseRepository.sumAmountByUserAndDateRange(user, from, to))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalIncome = Optional.ofNullable(
                        incomeRepository.sumAmountByUserAndDateRange(user, from, to))
                .orElse(BigDecimal.ZERO);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<CategorySummaryDto> expenseByCategory =
                Optional.ofNullable(expenseRepository.sumByCategory(user, from, to))
                        .orElse(Collections.emptyList());
        List<CategorySummaryDto> incomeByCategory =
                Optional.ofNullable(incomeRepository.sumByCategory(user, from, to))
                        .orElse(Collections.emptyList());
        boolean isEmpty = isDataEmpty(totalIncome, totalExpense, expenseByCategory, incomeByCategory);

        return FinancialReportDto.builder()
                .from(from)
                .to(to)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .expenseByCategory(expenseByCategory)
                .incomeByCategory(incomeByCategory)
                .emptyData(isEmpty)
                .build();
    }

    @Override
    public FinancialReportDto generateReport(LocalDate from, LocalDate to) {
        log.info("Generating detailed report for period: {} to {}", from, to);
        FinancialReportDto report = generateDashboard(from, to);

        User user = userService.getCurrentUser();
        report.setBudgets(mapBudgets(user, from, to));
        report.setSavingGoals(mapSavingGoals(user));

        return report;
    }

    @Override
    public byte[] exportToPdf(LocalDate from, LocalDate to) {
        log.info("Exporting PDF report for period: {} to {}", from, to);
        try {
            FinancialReportDto report = generateReport(from, to);
            return PdfExportUtil.generateReportPdf(report);
        } catch (Exception e) {
            log.error("PDF generation failed for period {} to {}", from, to, e);
            throw new RuntimeException(ReportConstants.ERROR_PDF_GENERATION_FAILED, e);
        }
    }

    @Override
    public List<MonthlyTrendDto> getMonthlyTrends(LocalDate from, LocalDate to) {
        log.info("Generating monthly trends for period: {} to {}", from, to);
        validateDateRange(from, to);

        User user = userService.getCurrentUser();

        List<MonthlyTrendProjection> expenseProjections = expenseRepository.findMonthlyExpenseTrends(user, from, to);
        List<MonthlyTrendProjection> incomeProjections = incomeRepository.findMonthlyIncomeTrends(user, from, to);

        Map<YearMonth, BigDecimal> expenseTrends = expenseProjections.stream()
                .collect(Collectors.toMap(
                        p -> YearMonth.of(p.getYear(), p.getMonth()),
                        MonthlyTrendProjection::getTotalAmount
                ));

        Map<YearMonth, BigDecimal> incomeTrends = incomeProjections.stream()
                .collect(Collectors.toMap(
                        p -> YearMonth.of(p.getYear(), p.getMonth()),
                        MonthlyTrendProjection::getTotalAmount
                ));

        Set<YearMonth> allPeriods = new TreeSet<>();
        allPeriods.addAll(expenseTrends.keySet());
        allPeriods.addAll(incomeTrends.keySet());

        return allPeriods.stream()
                .sorted()
                .map(period -> {
                    BigDecimal income = incomeTrends.getOrDefault(period, BigDecimal.ZERO);
                    BigDecimal expenses = expenseTrends.getOrDefault(period, BigDecimal.ZERO);
                    BigDecimal savings = income.subtract(expenses);
                    BigDecimal savingsRate = calculateSavingsRate(income, savings);

                    return MonthlyTrendDto.builder()
                            .period(period)
                            .totalIncome(income)
                            .totalExpenses(expenses)
                            .savings(savings)
                            .savingsRate(savingsRate)
                            .build();
                })
                .toList();
    }

    @Override
    public CategoryBreakdownDto getCategoryBreakdown(LocalDate from, LocalDate to, String type) {
        log.info("Generating category breakdown for type: {}, period: {} to {}", type, from, to);
        validateDateRange(from, to);

        User user = userService.getCurrentUser();

        // Determine which repository to use based on type
        List<CategorySummaryDto> categories = ReportConstants.TRANSACTION_TYPE_INCOME.equalsIgnoreCase(type)
                ? incomeRepository.sumByCategory(user, from, to)
                : expenseRepository.sumByCategory(user, from, to);

        CategoryType categoryType = ReportConstants.TRANSACTION_TYPE_INCOME.equalsIgnoreCase(type)
                ? CategoryType.INCOME
                : CategoryType.EXPENSE;

        CategorySummaryDto topCategory = findTopCategory(categories);
        BigDecimal totalAmount = calculateTotalAmount(categories);

        return CategoryBreakdownDto.builder()
                .type(categoryType)
                .totalAmount(totalAmount)
                .categories(categories)
                .topCategory(topCategory)
                .totalCategories(categories.size())
                .build();
    }

    @Override
    public BudgetPerformanceDto getBudgetPerformance(LocalDate from, LocalDate to) {
        log.info("Generating budget performance report for period: {} to {}", from, to);
        User user = userService.getCurrentUser();

        List<BudgetReportDto> budgets = mapBudgets(user, from, to);

        BudgetStats stats = calculateBudgetStats(budgets);

        return BudgetPerformanceDto.builder()
                .totalBudgets(budgets.size())
                .onTrack(stats.onTrack())
                .atRisk(stats.atRisk())
                .exceeded(stats.exceeded())
                .totalBudgeted(stats.totalBudgeted())
                .totalSpent(stats.totalSpent())
                .overallUtilization(stats.overallUtilization())
                .budgets(budgets)
                .build();
    }

    @Override
    public QuickStatsDto getQuickStats(LocalDate from, LocalDate to) {
        log.info("Generating quick stats for period: {} to {}", from, to);
        validateDateRange(from, to);

        User user = userService.getCurrentUser();

        BigDecimal monthlyIncome = incomeRepository.sumAmountByUserAndDateRange(user, from, to);
        BigDecimal monthlyExpenses = expenseRepository.sumAmountByUserAndDateRange(user, from, to);
        BigDecimal netCashFlow = monthlyIncome.subtract(monthlyExpenses);
        BigDecimal savingsRate = calculateSavingsRate(monthlyIncome, netCashFlow);

        List<Budget> activeBudgets = budgetRepository.findActiveBudgets(user, LocalDate.now());
        List<SavingGoal> activeGoals = getActiveSavingGoals(user);

        BigDecimal totalSavingsProgress = calculateAverageSavingsProgress(activeGoals);

        return QuickStatsDto.builder()
                .currentBalance(netCashFlow)
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .netCashFlow(netCashFlow)
                .savingsRate(savingsRate)
                .activeBudgets(activeBudgets.size())
                .activeSavingGoals(activeGoals.size())
                .totalSavingsProgress(totalSavingsProgress)
                .build();
    }

    @Override
    public byte[] exportToCsv(LocalDate from, LocalDate to) {
        log.info("Exporting CSV report for period: {} to {}", from, to);
        FinancialReportDto report = generateReport(from, to);

        StringBuilder csv = new StringBuilder();
        csv.append(ReportConstants.CSV_HEADER);

        report.getExpenseByCategory().forEach(expense ->
                csv.append(String.format(ReportConstants.CSV_ROW_FORMAT,
                        expense.getCategoryName(),
                        "Expense",
                        expense.getTotalAmount()))
        );

        report.getIncomeByCategory().forEach(income ->
                csv.append(String.format(ReportConstants.CSV_ROW_FORMAT,
                        income.getCategoryName(),
                        "Income",
                        income.getTotalAmount()))
        );

        return csv.toString().getBytes();
    }

    @Override
    public FinancialReportDto generateReportWithFilters(ReportFilterDto filters) {
        log.info("Generating report with filters: {}", filters);
        return generateReport(filters.getFrom(), filters.getTo());
    }

    /**
     * Validates the date range parameters.
     *
     * @param from start date
     * @param to   end date
     * @throws IllegalArgumentException if date range is invalid
     */
    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(ReportConstants.ERROR_DATE_RANGE_NULL);
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(ReportConstants.ERROR_START_AFTER_END);
        }
        if (from.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(ReportConstants.ERROR_FUTURE_START_DATE);
        }
    }

    /**
     * Checks if financial data is empty.
     *
     * @param totalIncome       total income amount
     * @param totalExpense      total expense amount
     * @param expenseByCategory expense categories
     * @param incomeByCategory  income categories
     * @return true if no meaningful data exists
     */
    private boolean isDataEmpty(BigDecimal totalIncome, BigDecimal totalExpense,
                                List<CategorySummaryDto> expenseByCategory,
                                List<CategorySummaryDto> incomeByCategory) {
        return totalIncome.compareTo(BigDecimal.ZERO) == 0
                && totalExpense.compareTo(BigDecimal.ZERO) == 0
                && expenseByCategory.isEmpty()
                && incomeByCategory.isEmpty();
    }

    /**
     * Maps user budgets to report DTOs with progress information.
     *
     * @param user the user
     * @param from report start date
     * @param to   report end date
     * @return list of budget report DTOs
     */
    private List<BudgetReportDto> mapBudgets(User user, LocalDate from, LocalDate to) {
        return budgetRepository.findByUserOrderByStartDateDesc(user).stream()
                .filter(budget -> isBudgetRelevantForReport(budget, from, to))
                .map(budget -> {
                    BigDecimal spent = calculateBudgetSpending(user, budget, from, to);
                    float progress = calculateProgressPercentage(spent, budget.getAmount());

                    return BudgetReportDto.builder()
                            .budgetName(getBudgetName(budget))
                            .amount(budget.getAmount())
                            .spent(spent)
                            .progressPercentage(progress)
                            .exceeded(progress > 100)
                            .build();
                })
                .toList();
    }

    /**
     * Maps user saving goals to report DTOs with progress information.
     *
     * @param user the user
     * @return list of saving goal report DTOs
     */
    private List<SavingGoalReportDto> mapSavingGoals(User user) {
        return savingGoalRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(goal -> {
                    float progress = calculateProgressPercentage(goal.getCurrentAmount(), goal.getTargetAmount());

                    return SavingGoalReportDto.builder()
                            .name(goal.getName())
                            .targetAmount(goal.getTargetAmount())
                            .currentAmount(goal.getCurrentAmount())
                            .progressPercentage(progress)
                            .achieved(goal.isAchieved())
                            .build();
                })
                .toList();
    }

    /**
     * Checks if a budget is relevant for the report period.
     *
     * @param budget     the budget
     * @param reportFrom report start date
     * @param reportTo   report end date
     * @return true if budget overlaps with report period
     */
    private boolean isBudgetRelevantForReport(Budget budget, LocalDate reportFrom, LocalDate reportTo) {
        return !budget.getEndDate().isBefore(reportFrom) &&
                !budget.getStartDate().isAfter(reportTo);
    }

    /**
     * Calculates spending for a budget within the relevant period.
     *
     * @param user       the user
     * @param budget     the budget
     * @param reportFrom report start date
     * @param reportTo   report end date
     * @return total spent amount
     */
    private BigDecimal calculateBudgetSpending(User user, Budget budget, LocalDate reportFrom, LocalDate reportTo) {
        LocalDate effectiveFrom = reportFrom.isAfter(budget.getStartDate()) ? reportFrom : budget.getStartDate();
        LocalDate effectiveTo = reportTo.isBefore(budget.getEndDate()) ? reportTo : budget.getEndDate();

        return budgetRepository.sumSpentByBudget(user, budget.getCategory(), effectiveFrom, effectiveTo);
    }

    /**
     * Calculates progress percentage.
     *
     * @param current current amount
     * @param target  target amount
     * @return progress percentage (0-100)
     */
    private float calculateProgressPercentage(BigDecimal current, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return current.divide(target, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .floatValue();
    }

    /**
     * Gets display name for budget.
     *
     * @param budget the budget
     * @return budget display name
     */
    private String getBudgetName(Budget budget) {
        return budget.getCategory() != null ? budget.getCategory().getName() : "Overall Budget";
    }

    /**
     * Calculates savings rate.
     *
     * @param income  total income
     * @param savings total savings
     * @return savings rate percentage
     */
    private BigDecimal calculateSavingsRate(BigDecimal income, BigDecimal savings) {
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            return savings.divide(income, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Finds the top category by amount.
     *
     * @param categories list of categories
     * @return top category or null if empty
     */
    private CategorySummaryDto findTopCategory(List<CategorySummaryDto> categories) {
        return categories.stream()
                .max(Comparator.comparing(CategorySummaryDto::getTotalAmount))
                .orElse(null);
    }

    /**
     * Calculates total amount from categories.
     *
     * @param categories list of categories
     * @return total amount
     */
    private BigDecimal calculateTotalAmount(List<CategorySummaryDto> categories) {
        return categories.stream()
                .map(CategorySummaryDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gets active (non-achieved) saving goals.
     *
     * @param user the user
     * @return list of active saving goals
     */
    private List<SavingGoal> getActiveSavingGoals(User user) {
        return savingGoalRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(goal -> !goal.isAchieved())
                .toList();
    }

    /**
     * Calculates average savings progress.
     *
     * @param goals list of saving goals
     * @return average progress percentage
     */
    private BigDecimal calculateAverageSavingsProgress(List<SavingGoal> goals) {
        if (goals.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalProgress = goals.stream()
                .map(goal -> goal.getCurrentAmount().divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalProgress.divide(BigDecimal.valueOf(goals.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates budget performance statistics.
     *
     * @param budgets list of budget reports
     * @return budget statistics
     */
    private BudgetStats calculateBudgetStats(List<BudgetReportDto> budgets) {
        int onTrack = (int) budgets.stream()
                .filter(b -> b.getProgressPercentage() < 90)
                .count();
        int atRisk = (int) budgets.stream()
                .filter(b -> b.getProgressPercentage() >= 90 && b.getProgressPercentage() <= 100)
                .count();
        int exceeded = (int) budgets.stream()
                .filter(b -> b.getProgressPercentage() > 100)
                .count();

        BigDecimal totalBudgeted = budgets.stream()
                .map(BudgetReportDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(BudgetReportDto::getSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal overallUtilization = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalBudgeted, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new BudgetStats(onTrack, atRisk, exceeded, totalBudgeted, totalSpent, overallUtilization);
    }

    /**
     * Record for budget statistics.
     */
    private record BudgetStats(int onTrack, int atRisk, int exceeded,
                               BigDecimal totalBudgeted, BigDecimal totalSpent,
                               BigDecimal overallUtilization) {
    }
}