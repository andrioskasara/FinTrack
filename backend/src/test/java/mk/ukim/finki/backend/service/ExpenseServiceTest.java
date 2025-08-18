package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.exception.UnauthorizedTransactionAccessException;
import mk.ukim.finki.backend.mapper.ExpenseMapper;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseDto;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseRequest;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.Expense;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.model.enums.CategoryType;
import mk.ukim.finki.backend.repository.CategoryRepository;
import mk.ukim.finki.backend.repository.ExpenseRepository;
import mk.ukim.finki.backend.repository.UserRepository;
import mk.ukim.finki.backend.service.impl.ExpenseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private User user;
    private UUID userId;
    private UUID expenseId;
    private Expense expense;
    private ExpenseDto expenseDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("user@example.com")
                .build();

        expenseId = UUID.randomUUID();

        mockAuthentication(user.getEmail());

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        Category category = createCustomCategory(UUID.randomUUID());
        expense = createExpense(expenseId, user, category);
        expenseDto = ExpenseDto.builder()
                .id(expenseId)
                .amount(expense.getAmount())
                .build();
    }

    private void mockAuthentication(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(securityContext);
    }

    private Expense createExpense(UUID id, User user, Category category) {
        return Expense.builder()
                .id(id)
                .user(user)
                .category(category)
                .amount(BigDecimal.TEN)
                .date(LocalDate.now())
                .description("Expense description")
                .build();
    }

    private Category createCustomCategory(UUID id) {
        return Category.builder()
                .id(id)
                .user(user)
                .type(CategoryType.EXPENSE)
                .name("Custom Category")
                .predefined(false)
                .build();
    }

    private Category createSystemCategory(UUID id) {
        return Category.builder()
                .id(id)
                .type(CategoryType.EXPENSE)
                .predefined(true)
                .build();
    }

    @Test
    void testGetAllExpenses_returnsMappedDtoList() {
        when(expenseRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense));
        ExpenseDto dto = ExpenseDto.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .build();
        when(expenseMapper.toDto(expense))
                .thenReturn(dto);

        List<ExpenseDto> result = expenseService.getAll();

        assertThat(result)
                .hasSize(1)
                .first()
                .isEqualTo(dto);

        verify(expenseRepository).findAllByUser_IdOrderByDateDescCreatedAtDesc(userId);
    }

    @Test
    void testGetExpenseById_whenExistsAndOwned_returnsDto() {
        when(expenseRepository.findById(expenseId))
                .thenReturn(Optional.of(expense));
        ExpenseDto dto = ExpenseDto.builder()
                .id(expenseId)
                .build();
        when(expenseMapper.toDto(expense))
                .thenReturn(dto);

        ExpenseDto result = expenseService.getById(expenseId);

        assertThat(result.getId()).isEqualTo(expenseId);

        verify(expenseRepository).findById(expenseId);
    }

    @Test
    void testGetExpenseById_whenNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(expenseRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expense not found");
    }

    @Test
    void testGetExpenseById_whenNotOwned_throws() {
        UUID id = UUID.randomUUID();
        Expense otherExpense = createExpense(id, User.builder().id(UUID.randomUUID()).build(),
                createCustomCategory(UUID.randomUUID()));

        when(expenseRepository.findById(id))
                .thenReturn(Optional.of(otherExpense));

        assertThatThrownBy(() -> expenseService.getById(id))
                .isInstanceOf(UnauthorizedTransactionAccessException.class);
    }

    @Test
    void testCreateExpense_whenValidRequest_returnsDto() {
        UUID catId = UUID.randomUUID();
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(BigDecimal.valueOf(30))
                .categoryId(catId)
                .date(LocalDate.now())
                .description("Restaurant")
                .build();

        Category category = createSystemCategory(catId);
        when(categoryRepository.findById(catId))
                .thenReturn(Optional.of(category));

        when(expenseRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ExpenseDto createdDto = ExpenseDto.builder()
                .amount(request.getAmount())
                .build();
        when(expenseMapper.toDto(any()))
                .thenReturn(createdDto);

        ExpenseDto result = expenseService.create(request);

        assertThat(result.getAmount()).isEqualTo(request.getAmount());
        verify(expenseRepository).save(any());
    }

    @Test
    void testCreateExpense_whenCategoryNotFound_throws() {
        UUID catId = UUID.randomUUID();
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(BigDecimal.ONE)
                .categoryId(catId)
                .date(LocalDate.now())
                .build();

        when(categoryRepository.findById(catId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void testCreateExpense_whenCategoryNotOwned_throws() {
        UUID catId = UUID.randomUUID();
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(BigDecimal.ONE)
                .categoryId(catId)
                .date(LocalDate.now())
                .build();

        Category category = createCustomCategory(catId);
        category.setUser(User.builder().id(UUID.randomUUID()).build());
        category.setPredefined(false);

        when(categoryRepository.findById(catId))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() -> expenseService.create(request))
                .isInstanceOf(UnauthorizedTransactionAccessException.class);
    }

    @Test
    void testUpdateExpense_whenValidRequest_returnsDto() {
        UUID catId = UUID.randomUUID();
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(BigDecimal.valueOf(40))
                .categoryId(catId)
                .date(LocalDate.now())
                .description("Updated description")
                .build();

        Category newCategory = createSystemCategory(catId);

        when(expenseRepository.findById(expenseId))
                .thenReturn(Optional.of(expense));
        when(categoryRepository.findById(catId))
                .thenReturn(Optional.of(newCategory));
        when(expenseMapper.toDto(any()))
                .thenReturn(expenseDto);

        ExpenseDto dto = expenseService.update(expenseId, request);

        assertThat(dto.getId()).isEqualTo(expenseId);
        verify(expenseRepository).save(any());
        assertThat(expense.getAmount()).isEqualTo(request.getAmount());
        assertThat(expense.getCategory()).isEqualTo(newCategory);
    }

    @Test
    void testUpdateExpense_whenNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(expenseRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.update(id, ExpenseRequest.builder().build()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expense not found");
    }

    @Test
    void testUpdateExpense_whenNotOwned_throws() {
        Expense unauthorizedExpense = createExpense(
                expenseId, User.builder().id(UUID.randomUUID()).build(), createCustomCategory(UUID.randomUUID()));
        when(expenseRepository.findById(expenseId))
                .thenReturn(Optional.of(unauthorizedExpense));

        assertThatThrownBy(() -> expenseService.update(expenseId, ExpenseRequest.builder().build()))
                .isInstanceOf(UnauthorizedTransactionAccessException.class);
    }

    @Test
    void testDeleteExpense_whenExistsAndOwned_deletesSuccessfully() {
        when(expenseRepository.findById(expenseId))
                .thenReturn(Optional.of(expense));

        expenseService.delete(expenseId);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void testDeleteExpense_whenNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(expenseRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expense not found");
    }

    @Test
    void testDeleteExpense_whenNotOwned_throws() {
        Expense unauthorizedExpense = createExpense(
                expenseId, User.builder().id(UUID.randomUUID()).build(), createCustomCategory(UUID.randomUUID()));
        when(expenseRepository.findById(expenseId))
                .thenReturn(Optional.of(unauthorizedExpense));

        assertThatThrownBy(() -> expenseService.delete(expenseId))
                .isInstanceOf(UnauthorizedTransactionAccessException.class);
    }
}

