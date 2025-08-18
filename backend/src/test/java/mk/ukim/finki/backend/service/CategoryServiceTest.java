package mk.ukim.finki.backend.service;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.exception.DuplicateCategoryNameException;
import mk.ukim.finki.backend.exception.HiddenCategoryException;
import mk.ukim.finki.backend.exception.UnauthorizedCategoryAccessException;
import mk.ukim.finki.backend.mapper.CategoryMapper;
import mk.ukim.finki.backend.model.dto.category.CategoryDto;
import mk.ukim.finki.backend.model.dto.category.CreateCategoryRequest;
import mk.ukim.finki.backend.model.dto.category.HideCategoryRequest;
import mk.ukim.finki.backend.model.dto.category.UpdateCategoryRequest;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.HiddenCategory;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.model.entity.Expense;
import mk.ukim.finki.backend.model.enums.CategoryType;
import mk.ukim.finki.backend.repository.CategoryRepository;
import mk.ukim.finki.backend.repository.HiddenCategoryRepository;
import mk.ukim.finki.backend.repository.UserRepository;
import mk.ukim.finki.backend.repository.ExpenseRepository;
import mk.ukim.finki.backend.repository.IncomeRepository;
import mk.ukim.finki.backend.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    HiddenCategoryRepository hiddenCategoryRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ExpenseRepository expenseRepository;
    @Mock
    IncomeRepository incomeRepository;
    @Mock
    CategoryMapper categoryMapper;
    @InjectMocks
    CategoryServiceImpl categoryService;
    private User user;
    private UUID userId;
    private CategoryDto dto;
    private static final UUID FALLBACK_EXPENSE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FALLBACK_INCOME_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@test.com").build();
        dto = new CategoryDto();
        mockAuth(user.getEmail());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    }

    private void mockAuth(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(context);
    }

    private Category customCategory(UUID id) {
        return Category.builder()
                .id(id)
                .user(user)
                .type(CategoryType.EXPENSE)
                .predefined(false)
                .name("Test Category")
                .build();
    }

    private Category systemCategory(UUID id) {
        return Category.builder().id(id)
                .type(CategoryType.EXPENSE)
                .predefined(true)
                .build();
    }

    private Category systemCategory(UUID id, CategoryType type) {
        return Category.builder()
                .id(id)
                .type(type)
                .predefined(true)
                .build();
    }

    @Test
    void getAllCategories_filtersFallbacksAndHiddenSystemCategories() {
        Category system = systemCategory(UUID.randomUUID());
        Category fallback = systemCategory(FALLBACK_EXPENSE_ID);
        Category custom = customCategory(UUID.randomUUID());

        when(categoryRepository.findVisibleByUserIdAndType(userId, CategoryType.EXPENSE))
                .thenReturn(List.of(system, fallback, custom));

        HiddenCategory hiddenCategory = HiddenCategory.builder()
                .category(system)
                .user(user)
                .build();

        when(hiddenCategoryRepository.findByUser_Id(userId))
                .thenReturn(List.of(hiddenCategory));

        when(categoryMapper.toDto(any(Category.class)))
                .thenReturn(dto);

        List<CategoryDto> result = categoryService.getAllCategories(CategoryType.EXPENSE);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void getAllCategories_incomeType_filtersFallbackAndHidden() {
        Category fallbackIncome = systemCategory(FALLBACK_INCOME_ID, CategoryType.INCOME);
        Category systemIncome = systemCategory(UUID.randomUUID(), CategoryType.INCOME);
        Category customIncome = Category.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(CategoryType.INCOME)
                .predefined(false)
                .name("Custom Income")
                .build();

        when(categoryRepository.findVisibleByUserIdAndType(userId, CategoryType.INCOME))
                .thenReturn(List.of(fallbackIncome, systemIncome, customIncome));

        HiddenCategory hidden = HiddenCategory.builder()
                .category(systemIncome)
                .user(user)
                .build();

        when(hiddenCategoryRepository.findByUser_Id(userId))
                .thenReturn(List.of(hidden));

        when(categoryMapper.toDto(any(Category.class))).thenReturn(dto);

        List<CategoryDto> result = categoryService.getAllCategories(CategoryType.INCOME);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void createCategory_success() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName(" New Category   ");
        request.setType(CategoryType.EXPENSE);

        when(categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(userId, "New Category", CategoryType.EXPENSE))
                .thenReturn(false);

        when(categoryMapper.toDto(any()))
                .thenReturn(dto);

        CategoryDto result = categoryService.createCategory(request);

        assertThat(result).isEqualTo(dto);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_duplicateThrows() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Test");
        request.setType(CategoryType.EXPENSE);
        when(categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(userId, "Test", CategoryType.EXPENSE))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    void updateCategory_success() {
        UUID id = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName(" Updated ");
        request.setIcon("icon");
        request.setColorCode("#fff");
        Category category = customCategory(id);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(userId, "Updated", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryMapper.toDto(any()))
                .thenReturn(dto);

        CategoryDto result = categoryService.updateCategory(id, request);

        assertThat(result).isEqualTo(dto);
        assertThat(category.getName()).isEqualTo("Updated");
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_missingThrows() {
        when(categoryRepository.findById(any()))
                .thenReturn(Optional.empty());
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Test Name")
                .icon("icon")
                .colorCode("#fff")
                .build();
        assertThatThrownBy(() -> categoryService.updateCategory(UUID.randomUUID(), request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateCategory_predefinedOrOtherUserThrows() {
        UUID id = UUID.randomUUID();
        Category predefinedCategory = systemCategory(id);
        Category otherUserCategory = Category.builder()
                .id(id)
                .predefined(false)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Test Name")
                .icon("icon")
                .colorCode("#fff")
                .build();

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(predefinedCategory));
        assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                .isInstanceOf(UnauthorizedCategoryAccessException.class);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(otherUserCategory));
        assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                .isInstanceOf(UnauthorizedCategoryAccessException.class);
    }

    @Test
    void updateCategory_duplicateNameThrows() {
        UUID id = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("New");
        Category category = customCategory(id);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(userId, "New", CategoryType.EXPENSE))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    void deleteCategory_success() {
        UUID id = UUID.randomUUID();
        Category category = customCategory(id);
        Category fallback = systemCategory(FALLBACK_EXPENSE_ID);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));

        when(categoryRepository.findById(FALLBACK_EXPENSE_ID))
                .thenReturn(Optional.of(fallback));

        categoryService.deleteCategory(id);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_success_reassignsTransactions() {
        UUID categoryId = UUID.randomUUID();
        Category category = customCategory(categoryId);

        Category fallback = systemCategory(FALLBACK_EXPENSE_ID);

        Expense expense1 = Expense.builder()
                .id(UUID.randomUUID())
                .category(category)
                .build();
        Expense expense2 = Expense.builder()
                .id(UUID.randomUUID())
                .category(category)
                .build();

        when(categoryRepository.findById(categoryId))

                .thenReturn(Optional.of(category));
        when(categoryRepository.findById(FALLBACK_EXPENSE_ID))
                .thenReturn(Optional.of(fallback));
        when(expenseRepository.findAllByCategory_Id(categoryId))
                .thenReturn(List.of(expense1, expense2));

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository).delete(category);

        assertThat(expense1.getCategory().getId()).isEqualTo(fallback.getId());
        assertThat(expense2.getCategory().getId()).isEqualTo(fallback.getId());
    }

    @Test
    void deleteCategory_notFoundThrows() {
        when(categoryRepository.findById(any()))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.deleteCategory(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteCategory_predefinedOrOtherUserThrows() {
        UUID id = UUID.randomUUID();
        Category predefinedCategory = systemCategory(id);
        Category nullUserCategory = Category.builder()
                .id(id)
                .predefined(false)
                .user(null)
                .build();
        Category otherUserCategory = Category.builder()
                .id(id)
                .predefined(false)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(predefinedCategory));
        assertThatThrownBy(() -> categoryService.deleteCategory(id))
                .isInstanceOf(UnauthorizedCategoryAccessException.class);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(nullUserCategory));
        assertThatThrownBy(() -> categoryService.deleteCategory(id))
                .isInstanceOf(UnauthorizedCategoryAccessException.class);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(otherUserCategory));
        assertThatThrownBy(() -> categoryService.deleteCategory(id))
                .isInstanceOf(UnauthorizedCategoryAccessException.class);
    }

    @Test
    void deleteCategory_missingFallbackThrows() {
        UUID id = UUID.randomUUID();
        Category category = customCategory(id);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));
        when(categoryRepository.findById(FALLBACK_EXPENSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteCategory_incomeType_usesFallbackIncomeId() {
        UUID id = UUID.randomUUID();
        Category category = Category.builder()
                .id(id)
                .user(user)
                .type(CategoryType.INCOME)
                .predefined(false)
                .build();

        Category fallbackIncome = systemCategory(FALLBACK_INCOME_ID, CategoryType.INCOME);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(FALLBACK_INCOME_ID))
                .thenReturn(Optional.of(fallbackIncome));

        categoryService.deleteCategory(id);

        verify(categoryRepository).delete(category);
    }

    @Test
    void hideCategory_success() {
        UUID id = UUID.randomUUID();
        Category category = systemCategory(id);
        HideCategoryRequest request = new HideCategoryRequest();
        request.setCategoryId(id);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));
        when(hiddenCategoryRepository.existsByUser_IdAndCategory_Id(userId, id))
                .thenReturn(false);

        categoryService.hideCategory(request);

        verify(hiddenCategoryRepository).save(any(HiddenCategory.class));
    }

    @Test
    void hideCategory_nonPredefinedThrows() {
        UUID id = UUID.randomUUID();
        Category category = customCategory(id);
        HideCategoryRequest request = new HideCategoryRequest();
        request.setCategoryId(id);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.hideCategory(request))
                .isInstanceOf(HiddenCategoryException.class);
    }

    @Test
    void hideCategory_alreadyHiddenThrows() {
        UUID id = UUID.randomUUID();
        Category category = systemCategory(id);
        HideCategoryRequest request = new HideCategoryRequest();
        request.setCategoryId(id);

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));
        when(hiddenCategoryRepository.existsByUser_IdAndCategory_Id(userId, id))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.hideCategory(request))
                .isInstanceOf(HiddenCategoryException.class);
    }

    @Test
    void hideCategory_notFoundThrows() {
        HideCategoryRequest request = new HideCategoryRequest();
        request.setCategoryId(UUID.randomUUID());

        when(categoryRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.hideCategory(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void unhideCategory_success() {
        UUID id = UUID.randomUUID();
        HiddenCategory hiddenCategory = HiddenCategory.builder()
                .id(id)
                .user(user)
                .category(Category.builder().name("Name").id(UUID.randomUUID()).build())
                .build();

        when(hiddenCategoryRepository.findById(id))
                .thenReturn(Optional.of(hiddenCategory));

        categoryService.unhideCategory(id);

        verify(hiddenCategoryRepository).delete(hiddenCategory);
    }

    @Test
    void unhideCategory_notFoundThrows() {
        when(hiddenCategoryRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.unhideCategory(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void unhideCategory_otherUserThrows() {
        UUID id = UUID.randomUUID();
        HiddenCategory hiddenCategory = HiddenCategory.builder()
                .id(id)
                .user(User.builder().id(UUID.randomUUID()).build())
                .category(Category.builder().id(UUID.randomUUID()).build())
                .build();

        when(hiddenCategoryRepository.findById(id))
                .thenReturn(Optional.of(hiddenCategory));

        assertThatThrownBy(() -> categoryService.unhideCategory(id))
                .isInstanceOf(UnauthorizedCategoryAccessException.class);
    }
}
