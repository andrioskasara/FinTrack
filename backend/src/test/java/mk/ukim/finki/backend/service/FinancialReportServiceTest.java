package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.model.dto.report.CategorySummaryDto;
import mk.ukim.finki.backend.model.dto.report.FinancialReportDto;
import mk.ukim.finki.backend.model.entity.*;
import mk.ukim.finki.backend.repository.*;
import mk.ukim.finki.backend.service.impl.FinancialReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    private UserRepository userRepository;

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

        mockAuthentication(user.getEmail());

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));
    }

    private void mockAuthentication(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void generateDashboard_success() {
        Expense expense = Expense.builder()
                .amount(new BigDecimal("50"))
                .date(LocalDate.of(2025, 6, 15))
                .build();
        Income income = Income.builder()
                .amount(new BigDecimal("100"))
                .date(LocalDate.of(2025, 6, 20))
                .build();

        when(expenseRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense));
        when(incomeRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(income));
        when(expenseRepository.sumByCategory(any(), any(), any())).thenReturn(
                List.of(new CategorySummaryDto("Food", new BigDecimal("50"))));
        when(incomeRepository.sumByCategory(any(), any(), any())).thenReturn(
                List.of(new CategorySummaryDto("Salary", new BigDecimal("100"))));

        FinancialReportDto dto = financialReportService.generateDashboard(from, to);

        assertThat(dto.getTotalExpense()).isEqualTo(new BigDecimal("50"));
        assertThat(dto.getTotalIncome()).isEqualTo(new BigDecimal("100"));
        assertThat(dto.getBalance()).isEqualTo(new BigDecimal("50"));
        assertThat(dto.isEmptyData()).isFalse();
    }

    @Test
    void generateDashboard_emptyData() {
        when(expenseRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(incomeRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(expenseRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());
        when(incomeRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());

        FinancialReportDto dto = financialReportService.generateDashboard(from, to);

        assertThat(dto.getTotalExpense()).isEqualTo(BigDecimal.ZERO);
        assertThat(dto.getTotalIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(dto.getExpenseByCategory()).isEmpty();
        assertThat(dto.getIncomeByCategory()).isEmpty();
        assertThat(dto.isEmptyData()).isTrue();
    }

    @Test
    void generateReport_success() {
        when(expenseRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(incomeRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(expenseRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());
        when(incomeRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());

        Category category = Category.builder().name("Food").build();
        Budget budget = Budget.builder()
                .amount(new BigDecimal("200"))
                .category(category)
                .startDate(from)
                .endDate(to)
                .build();

        when(budgetRepository.findByUserOrderByStartDateDesc(user))
                .thenReturn(List.of(budget));
        when(budgetRepository.sumSpentByBudget(eq(user), eq(category), any(), any()))
                .thenReturn(new BigDecimal("50"));

        SavingGoal goal = SavingGoal.builder()
                .name("Vacation")
                .targetAmount(new BigDecimal("1000"))
                .currentAmount(new BigDecimal("200"))
                .build();
        when(savingGoalRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(goal));

        FinancialReportDto dto = financialReportService.generateReport(from, to);

        assertThat(dto.getBudgets()).hasSize(1);
        assertThat(dto.getBudgets().get(0).getProgressPercentage()).isEqualTo(25.0f);
        assertThat(dto.getSavingGoals()).hasSize(1);
        assertThat(dto.getSavingGoals().get(0).getProgressPercentage()).isEqualTo(20.0f);
    }

    @Test
    void generateReport_zeroBudgetAmount() {
        Category category = Category.builder().name("Misc").build();
        Budget budget = Budget.builder()
                .amount(BigDecimal.ZERO)
                .category(category)
                .startDate(from)
                .endDate(to)
                .build();
        when(budgetRepository.findByUserOrderByStartDateDesc(user))
                .thenReturn(List.of(budget));
        when(budgetRepository.sumSpentByBudget(eq(user), eq(category), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(savingGoalRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of());

        FinancialReportDto dto = financialReportService.generateReport(from, to);
        assertThat(dto.getBudgets().get(0).getProgressPercentage()).isEqualTo(0f);
    }

    @Test
    void generateReport_zeroSavingGoalTarget() {
        SavingGoal goal = SavingGoal.builder()
                .name("Emergency")
                .targetAmount(BigDecimal.ZERO)
                .currentAmount(BigDecimal.ZERO)
                .build();
        when(savingGoalRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(goal));
        when(budgetRepository.findByUserOrderByStartDateDesc(user))
                .thenReturn(List.of());

        FinancialReportDto dto = financialReportService.generateReport(from, to);
        assertThat(dto.getSavingGoals().get(0).getProgressPercentage()).isEqualTo(0f);
    }

    @Test
    void exportToPdf_success() throws Exception {
        byte[] pdfBytes = financialReportService.exportToPdf(from, to);
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    void exportToPdf_emptyData() throws Exception {
        when(expenseRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(incomeRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(expenseRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());
        when(incomeRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());
        when(budgetRepository.findByUserOrderByStartDateDesc(any())).thenReturn(List.of());
        when(savingGoalRepository.findByUserOrderByCreatedAtDesc(any())).thenReturn(List.of());

        byte[] pdfBytes = financialReportService.exportToPdf(from, to);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    void exportToPdf_noCharts() throws Exception {
        Category category = Category.builder().name("Food").build();
        Budget budget = Budget.builder()
                .amount(new BigDecimal("200"))
                .category(category)
                .startDate(from)
                .endDate(to)
                .build();
        when(budgetRepository.findByUserOrderByStartDateDesc(user))
                .thenReturn(List.of(budget));
        when(budgetRepository.sumSpentByBudget(eq(user), eq(category), any(), any()))
                .thenReturn(new BigDecimal("50"));

        SavingGoal goal = SavingGoal.builder()
                .name("Trip")
                .targetAmount(new BigDecimal("1000"))
                .currentAmount(new BigDecimal("100"))
                .build();
        when(savingGoalRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(goal));

        when(expenseRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(incomeRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId)).thenReturn(List.of());
        when(expenseRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());
        when(incomeRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());

        byte[] pdfBytes = financialReportService.exportToPdf(from, to);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }
}
