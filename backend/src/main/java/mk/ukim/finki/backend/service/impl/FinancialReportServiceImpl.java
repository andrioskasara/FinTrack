package mk.ukim.finki.backend.service.impl;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.report.BudgetReportDto;
import mk.ukim.finki.backend.model.dto.report.CategorySummaryDto;
import mk.ukim.finki.backend.model.dto.report.FinancialReportDto;
import mk.ukim.finki.backend.model.dto.report.SavingGoalReportDto;
import mk.ukim.finki.backend.model.entity.Expense;
import mk.ukim.finki.backend.model.entity.Income;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.repository.*;
import mk.ukim.finki.backend.service.FinancialReportService;
import mk.ukim.finki.backend.util.PdfExportUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves the currently authenticated user.
     *
     * @return the User entity
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }

    @Override
    public FinancialReportDto generateDashboard(LocalDate from, LocalDate to) {
        User user = getCurrentUser();

        BigDecimal totalExpense = expenseRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(user.getId())
                .stream()
                .filter(e -> isWithinRange(e.getDate(), from, to))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncome = incomeRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(user.getId())
                .stream()
                .filter(i -> isWithinRange(i.getDate(), from, to))
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySummaryDto> expenseByCategory = expenseRepository.sumByCategory(user, from, to);
        List<CategorySummaryDto> incomeByCategory = incomeRepository.sumByCategory(user, from, to);

        boolean isEmpty = totalIncome.compareTo(BigDecimal.ZERO) == 0
                && totalExpense.compareTo(BigDecimal.ZERO) == 0
                && expenseByCategory.isEmpty()
                && incomeByCategory.isEmpty();

        return FinancialReportDto.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .expenseByCategory(expenseByCategory)
                .incomeByCategory(incomeByCategory)
                .emptyData(isEmpty)
                .build();
    }

    @Override
    public FinancialReportDto generateReport(LocalDate from, LocalDate to) {
        FinancialReportDto report = generateDashboard(from, to);
        User user = getCurrentUser();

        report.setBudgets(mapBudgets(user, from, to));
        report.setSavingGoals(mapSavingGoals(user));

        return report;
    }

    @Override
    public byte[] exportToPdf(LocalDate from, LocalDate to) throws Exception {
        FinancialReportDto report = generateReport(from, to);
        return PdfExportUtil.generateReportPdf(report);
    }

    /**
     * Checks if a date is within the given range (inclusive).
     *
     * @param date the date to check
     * @param from start of the range
     * @param to   end of the range
     * @return true if date is within range, false otherwise
     */
    private boolean isWithinRange(LocalDate date, LocalDate from, LocalDate to) {
        return (date != null) && !date.isBefore(from) && !date.isAfter(to);
    }

    /**
     * Maps the user's budgets to report DTOs with spending and progress information.
     *
     * @param user the user
     * @return list of {@link BudgetReportDto}
     */
    private List<BudgetReportDto> mapBudgets(User user, LocalDate from, LocalDate to) {
        return budgetRepository.findByUserOrderByStartDateDesc(user).stream()
                .map(b -> {
                    BigDecimal spent = budgetRepository.sumSpentByBudget(user, b.getCategory(),
                            from.isAfter(b.getStartDate()) ? from : b.getStartDate(),
                            to.isBefore(b.getEndDate()) ? to : b.getEndDate());

                    float progress = b.getAmount().compareTo(BigDecimal.ZERO) == 0
                            ? 0
                            : spent.divide(b.getAmount(), 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).floatValue();

                    return BudgetReportDto.builder()
                            .budgetName(b.getCategory() != null ? b.getCategory().getName() : "Overall")
                            .amount(b.getAmount())
                            .spent(spent)
                            .progressPercentage(progress)
                            .exceeded(spent.compareTo(b.getAmount()) > 0)
                            .build();
                })
                .toList();
    }

    /**
     * Maps the user's saving goals to report DTOs with progress information.
     *
     * @param user the user
     * @return list of {@link SavingGoalReportDto}
     */
    private List<SavingGoalReportDto> mapSavingGoals(User user) {
        return savingGoalRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(g -> {
                    float progress = g.getTargetAmount().compareTo(BigDecimal.ZERO) == 0
                            ? 0
                            : g.getCurrentAmount().divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).floatValue();

                    return SavingGoalReportDto.builder()
                            .name(g.getName())
                            .targetAmount(g.getTargetAmount())
                            .currentAmount(g.getCurrentAmount())
                            .progressPercentage(progress)
                            .achieved(g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0)
                            .build();
                })
                .toList();
    }
}
