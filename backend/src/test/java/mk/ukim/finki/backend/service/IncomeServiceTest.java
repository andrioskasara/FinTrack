package mk.ukim.finki.backend.service;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.exception.UnauthorizedTransactionAccessException;
import mk.ukim.finki.backend.mapper.IncomeMapper;
import mk.ukim.finki.backend.model.dto.transaction.IncomeDto;
import mk.ukim.finki.backend.model.dto.transaction.IncomeRequest;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.Income;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.model.enums.CategoryType;
import mk.ukim.finki.backend.repository.CategoryRepository;
import mk.ukim.finki.backend.repository.IncomeRepository;
import mk.ukim.finki.backend.repository.UserRepository;
import mk.ukim.finki.backend.service.impl.IncomeServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private IncomeMapper incomeMapper;

    @InjectMocks
    private IncomeServiceImpl incomeService;

    private User user;
    private UUID userId;
    private UUID incomeId;
    private Income income;
    private IncomeDto incomeDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("user@example.com")
                .build();
        incomeId = UUID.randomUUID();

        mockAuth(user.getEmail());
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        Category category = customCategory(UUID.randomUUID());
        income = createIncome(incomeId, user, category);
        incomeDto = IncomeDto.builder()
                .id(incomeId)
                .amount(income.getAmount())
                .build();
    }

    private void mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);

        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(ctx);
    }

    private Income createIncome(UUID id, User u, Category c) {
        return Income.builder()
                .id(id)
                .user(u)
                .category(c)
                .amount(BigDecimal.TEN)
                .date(LocalDate.now())
                .description("Test income")
                .build();
    }

    private Category customCategory(UUID id) {
        return Category.builder()
                .id(id)
                .user(user)
                .type(CategoryType.INCOME)
                .name("Custom Income Category")
                .predefined(false)
                .build();
    }

    private Category systemCategory(UUID id) {
        return Category.builder()
                .id(id)
                .type(CategoryType.INCOME)
                .name("System Income Category")
                .predefined(true)
                .build();
    }

    @Test
    void getAll_returnsMappedList() {
        when(incomeRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(income));
        IncomeDto dto = IncomeDto.builder()
                .id(income.getId())
                .amount(income.getAmount())
                .build();
        when(incomeMapper.toDto(income))
                .thenReturn(dto);

        List<IncomeDto> result = incomeService.getAll();

        assertThat(result).hasSize(1).first().isEqualTo(dto);
        verify(incomeRepository).findAllByUser_IdOrderByDateDescCreatedAtDesc(userId);
    }

    @Test
    void getById_foundAndOwned_returnsDto() {
        when(incomeRepository.findById(incomeId))
                .thenReturn(Optional.of(income));
        when(incomeMapper.toDto(income))
                .thenReturn(incomeDto);

        IncomeDto result = incomeService.getById(incomeId);

        assertThat(result.getId()).isEqualTo(incomeId);
        verify(incomeRepository).findById(incomeId);
    }

    @Test
    void getById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(incomeRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Income not found");
    }

    @Test
    void getById_notOwned_throws() {
        UUID id = UUID.randomUUID();
        Income other = createIncome(
                id, User.builder().id(UUID.randomUUID()).build(), customCategory(UUID.randomUUID()));
        when(incomeRepository.findById(id))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> incomeService.getById(id))
                .isInstanceOf(UnauthorizedTransactionAccessException.class);
    }

    @Test
    void create_valid_returnsDto() {
        UUID catId = UUID.randomUUID();
        IncomeRequest request = IncomeRequest.builder()
                .amount(BigDecimal.valueOf(30))
                .categoryId(catId)
                .date(LocalDate.now())
                .description("Bonus")
                .build();

        Category category = systemCategory(catId);
        when(categoryRepository.findById(catId))
                .thenReturn(Optional.of(category));
        when(incomeRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(incomeMapper.toDto(any()))
                .thenReturn(IncomeDto.builder().amount(request.getAmount()).build());

        IncomeDto result = incomeService.create(request);

        assertThat(result.getAmount()).isEqualTo(request.getAmount());
        verify(incomeRepository).save(any());
    }

    @Test
    void create_categoryNotFound_throws() {
        UUID catId = UUID.randomUUID();
        IncomeRequest request = IncomeRequest.builder()
                .amount(BigDecimal.ONE)
                .categoryId(catId)
                .date(LocalDate.now())
                .build();

        when(categoryRepository.findById(catId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void create_categoryNotOwned_throws() {
        UUID catId = UUID.randomUUID();
        IncomeRequest request = IncomeRequest.builder()
                .amount(BigDecimal.ONE)
                .categoryId(catId)
                .date(LocalDate.now())
                .build();

        Category foreignCustom = customCategory(catId);
        foreignCustom.setUser(User.builder().id(UUID.randomUUID()).build()); // different owner
        foreignCustom.setPredefined(false);

        when(categoryRepository.findById(catId))
                .thenReturn(Optional.of(foreignCustom));

        assertThatThrownBy(() -> incomeService.create(request))
                .isInstanceOf(UnauthorizedTransactionAccessException.class);
    }

    @Test
    void update_valid_updatesAndReturnsDto() {
        UUID newCat = UUID.randomUUID();
        IncomeRequest request = IncomeRequest.builder()
                .amount(BigDecimal.valueOf(55))
                .categoryId(newCat)
                .date(LocalDate.now())
                .description("Updated desc")
                .build();

        when(incomeRepository.findById(incomeId))
                .thenReturn(Optional.of(income));
        when(categoryRepository.findById(newCat))
                .thenReturn(Optional.of(systemCategory(newCat)));
        when(incomeMapper.toDto(any()))
                .thenReturn(incomeDto);

        IncomeDto dto = incomeService.update(incomeId, request);

        assertThat(dto.getId()).isEqualTo(incomeId);
        verify(incomeRepository).save(any());
        assertThat(income.getAmount()).isEqualTo(request.getAmount());
        assertThat(income.getCategory().getId()).isEqualTo(newCat);
    }

    @Test
    void update_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(incomeRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.update(id, IncomeRequest.builder().build()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Income not found");
    }

    @Test
    void update_notOwned_throws() {
        Income foreign = createIncome(
                incomeId, User.builder().id(UUID.randomUUID()).build(), customCategory(UUID.randomUUID()));
        when(incomeRepository.findById(incomeId))
                .thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> incomeService.update(incomeId, IncomeRequest.builder().build()))
                .isInstanceOf(UnauthorizedTransactionAccessException.class);
    }

    @Test
    void delete_foundAndOwned_deletes() {
        when(incomeRepository.findById(incomeId))
                .thenReturn(Optional.of(income));

        incomeService.delete(incomeId);

        verify(incomeRepository).delete(income);
    }

    @Test
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(incomeRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Income not found");
    }

    @Test
    void delete_notOwned_throws() {
        Income foreign = createIncome(
                incomeId, User.builder().id(UUID.randomUUID()).build(), customCategory(UUID.randomUUID()));
        when(incomeRepository.findById(incomeId))
                .thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> incomeService.delete(incomeId))
                .isInstanceOf(UnauthorizedTransactionAccessException.class);
    }
}
