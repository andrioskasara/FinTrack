package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.model.dto.report.CategorySummaryDto;
import mk.ukim.finki.backend.model.dto.report.FinancialReportDto;
import mk.ukim.finki.backend.model.dto.report.MonthlyTrendDto;
import mk.ukim.finki.backend.model.dto.report.MonthlyTrendProjection;
import mk.ukim.finki.backend.model.entity.*;
import mk.ukim.finki.backend.repository.*;
import mk.ukim.finki.backend.service.impl.FinancialReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FinancialReportServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private SavingGoalRepository savingGoalRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FinancialReportServiceImpl financialReportService;

    private User user;
    private UUID userId;
    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .build();

        from = LocalDate.of(2025, 1, 1);
        to = LocalDate.of(2025, 12, 31);

        when(userService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void generateDashboard_success() {
        when(expenseRepository.sumAmountByUserAndDateRange(any(User.class), eq(from), eq(to)))
                .thenReturn(new BigDecimal("150.00"));
        when(incomeRepository.sumAmountByUserAndDateRange(any(User.class), eq(from), eq(to)))
                .thenReturn(new BigDecimal("1000.00"));

        when(expenseRepository.sumByCategory(any(User.class), eq(from), eq(to)))
                .thenReturn(List.of(new CategorySummaryDto("Food", new BigDecimal("100.00"))));
        when(incomeRepository.sumByCategory(any(User.class), eq(from), eq(to)))
                .thenReturn(List.of(new CategorySummaryDto("Salary", new BigDecimal("1000.00"))));

        FinancialReportDto result = financialReportService.generateDashboard(from, to);

        assertThat(result.getTotalIncome()).isEqualByComparingTo("1000.00");
        assertThat(result.getTotalExpense()).isEqualByComparingTo("150.00");
        assertThat(result.getBalance()).isEqualByComparingTo("850.00");
        assertThat(result.getExpenseByCategory()).hasSize(1);
        assertThat(result.getIncomeByCategory()).hasSize(1);
    }

    @Test
    void generateDashboard_emptyData() {
        when(expenseRepository.sumAmountByUserAndDateRange(any(User.class), eq(from), eq(to)))
                .thenReturn(null);
        when(incomeRepository.sumAmountByUserAndDateRange(any(User.class), eq(from), eq(to)))
                .thenReturn(null);

        when(expenseRepository.sumByCategory(any(User.class), eq(from), eq(to)))
                .thenReturn(Collections.emptyList());
        when(incomeRepository.sumByCategory(any(User.class), eq(from), eq(to)))
                .thenReturn(Collections.emptyList());
        FinancialReportDto result = financialReportService.generateDashboard(from, to);

        assertThat(result.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getExpenseByCategory()).isEmpty();
        assertThat(result.getIncomeByCategory()).isEmpty();
    }

    @Test
    void generateReport_zeroAmounts_safeDefaults() {
        Category cat = Category.builder().name("Misc").build();
        Budget budget = Budget.builder()
                .amount(BigDecimal.ZERO)
                .category(cat)
                .startDate(from)
                .endDate(to)
                .build();
        when(budgetRepository.findByUserOrderByStartDateDesc(user)).thenReturn(List.of(budget));
        when(budgetRepository.sumSpentByBudget(eq(user), eq(cat), any(), any())).thenReturn(BigDecimal.ZERO);

        SavingGoal goal = SavingGoal.builder()
                .name("Emergency Fund")
                .targetAmount(BigDecimal.ZERO)
                .currentAmount(BigDecimal.ZERO)
                .build();
        when(savingGoalRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(goal));

        FinancialReportDto dto = financialReportService.generateReport(from, to);

        assertThat(dto.getBudgets().get(0).getProgressPercentage()).isEqualTo(0f);
        assertThat(dto.getSavingGoals().get(0).getProgressPercentage()).isEqualTo(0f);
    }

    @Test
    void generateReport_includesBudgetsAndGoals() {
        when(expenseRepository.sumAmountByUserAndDateRange(any(), eq(from), eq(to)))
                .thenReturn(BigDecimal.ZERO);
        when(incomeRepository.sumAmountByUserAndDateRange(any(), eq(from), eq(to)))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumByCategory(any(), eq(from), eq(to)))
                .thenReturn(Collections.emptyList());
        when(incomeRepository.sumByCategory(any(), eq(from), eq(to)))
                .thenReturn(Collections.emptyList());

        Category category = Category.builder().name("Food").build();
        Budget budget = Budget.builder()
                .category(category)
                .amount(new BigDecimal("500"))
                .startDate(from)
                .endDate(to)
                .build();
        when(budgetRepository.findByUserOrderByStartDateDesc(user))
                .thenReturn(List.of(budget));
        when(budgetRepository.sumSpentByBudget(eq(user), eq(category), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        SavingGoal goal = SavingGoal.builder()
                .name("Emergency Fund")
                .targetAmount(new BigDecimal("1000"))
                .currentAmount(new BigDecimal("200"))
                .build();
        when(savingGoalRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(goal));

        FinancialReportDto result = financialReportService.generateReport(from, to);

        assertThat(result.getBudgets()).hasSize(1);
        assertThat(result.getSavingGoals()).hasSize(1);
    }

    @Test
    void getMonthlyTrends_success() {
        YearMonth jan = YearMonth.of(2025, 1);
        YearMonth feb = YearMonth.of(2025, 2);

        MonthlyTrendProjection janExpense = mock(MonthlyTrendProjection.class);
        when(janExpense.getYear()).thenReturn(2025);
        when(janExpense.getMonth()).thenReturn(1);
        when(janExpense.getTotalAmount()).thenReturn(new BigDecimal("200.00"));

        MonthlyTrendProjection febIncome = mock(MonthlyTrendProjection.class);
        when(febIncome.getYear()).thenReturn(2025);
        when(febIncome.getMonth()).thenReturn(2);
        when(febIncome.getTotalAmount()).thenReturn(new BigDecimal("1000.00"));

        when(expenseRepository.findMonthlyExpenseTrends(any(User.class), any(), any()))
                .thenReturn(List.of(janExpense));
        when(incomeRepository.findMonthlyIncomeTrends(any(User.class), any(), any()))
                .thenReturn(List.of(febIncome));

        List<MonthlyTrendDto> result = financialReportService.getMonthlyTrends(from, to);

        assertThat(result).hasSize(2);

        MonthlyTrendDto january = result.stream().filter(r -> r.getPeriod().equals(jan)).findFirst().orElseThrow();
        MonthlyTrendDto february = result.stream().filter(r -> r.getPeriod().equals(feb)).findFirst().orElseThrow();

        assertThat(january.getTotalExpenses()).isEqualByComparingTo("200.00");
        assertThat(january.getTotalIncome()).isEqualByComparingTo("0.00");

        assertThat(february.getTotalIncome()).isEqualByComparingTo("1000.00");
        assertThat(february.getTotalExpenses()).isEqualByComparingTo("0.00");
    }
}
