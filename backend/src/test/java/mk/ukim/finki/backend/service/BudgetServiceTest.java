package mk.ukim.finki.backend.service;

import jakarta.persistence.EntityNotFoundException;
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
import mk.ukim.finki.backend.service.impl.BudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserService userService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private User user;
    private Budget budget;
    private BudgetDto budgetDto;
    private UUID budgetId;
    private Category category;

    @BeforeEach
    void setUp() {
        budgetId = UUID.randomUUID();
        user = User.builder().id(UUID.randomUUID()).email("test@test.com").build();
        category = Category.builder().id(UUID.randomUUID()).name("Custom Category").build();

        budget = Budget.builder()
                .id(budgetId)
                .user(user)
                .category(category)
                .amount(BigDecimal.valueOf(100))
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .archived(false)
                .build();

        budgetDto = BudgetDto.builder()
                .id(budgetId)
                .amount(BigDecimal.valueOf(100))
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .build();

        when(userService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getAllBudgets_success() {
        when(budgetRepository.findByUserOrderByStartDateDesc(user))
                .thenReturn(List.of(budget));
        when(budgetRepository.sumSpentByBudget(user, category, budget.getStartDate(), budget.getEndDate()))
                .thenReturn(BigDecimal.valueOf(50));
        when(budgetMapper.toDto(any())).thenReturn(budgetDto);

        List<BudgetDto> result = budgetService.getAllBudgets();

        assertThat(result).containsExactly(budgetDto);
        verify(budgetRepository, atLeastOnce()).findByUserOrderByStartDateDesc(user);
        verify(budgetRepository).sumSpentByBudget(user, category, budget.getStartDate(), budget.getEndDate());
    }

    @Test
    void getBudgetById_found_returnDto() {
        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.of(budget));
        when(budgetMapper.toDto(any()))
                .thenReturn(budgetDto);

        BudgetDto result = budgetService.getBudgetById(budgetId);

        assertThat(result).isEqualTo(budgetDto);
    }

    @Test
    void getBudgetById_notFound_throws() {
        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.getBudgetById(budgetId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Budget not found");
    }

    @Test
    void createBudget_success() {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(category.getId())
                .amount(BigDecimal.valueOf(200))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .isRollover(false)
                .build();

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));
        when(budgetRepository.findOverlappingBudgets(user, category, request.getStartDate(), request.getEndDate()))
                .thenReturn(Collections.emptyList());
        when(budgetMapper.toDto(any()))
                .thenReturn(budgetDto);

        BudgetDto result = budgetService.createBudget(request);

        assertThat(result).isEqualTo(budgetDto);
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void createBudget_invalidDates_throws() {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(category.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().minusDays(1))
                .amount(BigDecimal.valueOf(10))
                .isRollover(false)
                .build();

        assertThatThrownBy(() -> budgetService.createBudget(request))
                .isInstanceOf(BudgetValidationException.class)
                .hasMessage("End date cannot be before start date");
    }

    @Test
    void createBudget_overlapping_throws() {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(category.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .amount(BigDecimal.valueOf(10))
                .isRollover(false)
                .build();

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));
        when(budgetRepository.findOverlappingBudgets(user, category, request.getStartDate(), request.getEndDate()))
                .thenReturn(List.of(budget));

        assertThatThrownBy(() -> budgetService.createBudget(request))
                .isInstanceOf(BudgetValidationException.class)
                .hasMessage("Overlapping budget exists for this category and period");
    }

    @Test
    void createBudget_categoryNotFound_throws() {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(UUID.randomUUID())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .amount(BigDecimal.valueOf(50))
                .build();

        when(categoryRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.createBudget(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void updateBudget_success() {
        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .categoryId(category.getId())
                .amount(BigDecimal.valueOf(300))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .archived(false)
                .isRollover(false)
                .build();

        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));
        when(budgetRepository.findOverlappingBudgets(user, category, request.getStartDate(), request.getEndDate()))
                .thenReturn(Collections.emptyList());
        when(budgetMapper.toDto(any()))
                .thenReturn(budgetDto);

        BudgetDto result = budgetService.updateBudget(budgetId, request);

        assertThat(result).isEqualTo(budgetDto);
        verify(budgetRepository).save(budget);
    }

    @Test
    void updateBudget_budgetNotFound_throws() {
        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .categoryId(category.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .amount(BigDecimal.valueOf(10))
                .build();

        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudget(budgetId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Budget not found");
    }

    @Test
    void updateBudget_categoryNotFound_throws() {
        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .categoryId(UUID.randomUUID())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .amount(BigDecimal.valueOf(10))
                .build();

        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudget(budgetId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void updateBudget_overlapping_throws() {
        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .categoryId(category.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .amount(BigDecimal.valueOf(10))
                .build();

        Budget overlappingBudget = Budget.builder().id(UUID.randomUUID()).archived(false).build();

        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.of(budget));
        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));
        when(budgetRepository.findOverlappingBudgets(user, category, request.getStartDate(), request.getEndDate()))
                .thenReturn(List.of(overlappingBudget));

        assertThatThrownBy(() -> budgetService.updateBudget(budgetId, request))
                .isInstanceOf(BudgetValidationException.class)
                .hasMessage("Overlapping budget exists for this category and period");
    }

    @Test
    void deleteBudget_success() {
        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.of(budget));

        budgetService.deleteBudget(budgetId);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void deleteBudget_notFound_throws() {
        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deleteBudget(budgetId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Budget not found");
    }

    @Test
    void calculateProgress_zeroSpent_returnsZero() {
        when(budgetRepository.findByUserOrderByStartDateDesc(user))
                .thenReturn(List.of(budget));
        when(budgetRepository.sumSpentByBudget(user, category, budget.getStartDate(), budget.getEndDate()))
                .thenReturn(BigDecimal.ZERO);

        when(budgetMapper.toDto(any())).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            return BudgetDto.builder()
                    .id(b.getId())
                    .amount(b.getAmount())
                    .startDate(b.getStartDate())
                    .endDate(b.getEndDate())
                    .progressPercentage(b.getProgressPercentage())
                    .build();
        });

        List<BudgetDto> result = budgetService.getAllBudgets();

        assertThat(result.get(0).getProgressPercentage()).isEqualTo(0);
    }

    @Test
    void calculateProgress_spentGreaterThanAmount_returns100() {
        when(budgetRepository.findByUserOrderByStartDateDesc(user))
                .thenReturn(List.of(budget));
        when(budgetRepository.sumSpentByBudget(user, category, budget.getStartDate(), budget.getEndDate()))
                .thenReturn(BigDecimal.valueOf(200));

        when(budgetMapper.toDto(any())).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            return BudgetDto.builder()
                    .id(b.getId())
                    .amount(b.getAmount())
                    .startDate(b.getStartDate())
                    .endDate(b.getEndDate())
                    .progressPercentage(b.getProgressPercentage())
                    .build();
        });

        List<BudgetDto> result = budgetService.getAllBudgets();

        assertThat(result.get(0).getProgressPercentage()).isEqualTo(100);
    }

    @Test
    void rolloverBudget_success() {
        when(budgetRepository.findByIdAndUser(budgetId, user))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(budgetMapper.toDto(any()))
                .thenReturn(budgetDto);

        BudgetDto result = budgetService.rolloverBudget(budgetId);

        assertThat(result).isEqualTo(budgetDto);
        verify(budgetRepository).save(any(Budget.class));
    }
}
