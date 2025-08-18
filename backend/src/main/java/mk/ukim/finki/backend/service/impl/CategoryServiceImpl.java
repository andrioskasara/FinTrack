package mk.ukim.finki.backend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import mk.ukim.finki.backend.model.enums.CategoryType;
import mk.ukim.finki.backend.repository.CategoryRepository;
import mk.ukim.finki.backend.repository.HiddenCategoryRepository;
import mk.ukim.finki.backend.repository.UserRepository;
import mk.ukim.finki.backend.repository.ExpenseRepository;
import mk.ukim.finki.backend.repository.IncomeRepository;
import mk.ukim.finki.backend.service.CategoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static mk.ukim.finki.backend.util.CategoryServiceMessages.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final HiddenCategoryRepository hiddenCategoryRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryMapper categoryMapper;

    private static final UUID FALLBACK_EXPENSE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FALLBACK_INCOME_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CATEGORY_NOT_FOUND));
    }

    private void assertOwnership(Category category, User user) {
        if (category.isPredefined() || category.getUser() == null || !user.getId().equals(category.getUser().getId())) {
            throw new UnauthorizedCategoryAccessException();
        }
    }

    private UUID getFallbackIdForType(CategoryType type) {
        return (type == CategoryType.EXPENSE) ? FALLBACK_EXPENSE_ID : FALLBACK_INCOME_ID;
    }

    @Override
    public List<CategoryDto> getAllCategories(CategoryType type) {
        User user = getCurrentUser();

        Set<UUID> hiddenCategoriesIds = hiddenCategoryRepository.findByUser_Id(user.getId())
                .stream()
                .map(h -> h.getCategory().getId())
                .collect(Collectors.toSet());

        return categoryRepository.findVisibleByUserIdAndType(user.getId(), type)
                .stream()
                .filter(category -> !category.getId().equals(FALLBACK_EXPENSE_ID) && !category.getId().equals(FALLBACK_INCOME_ID))
                .filter(category -> !category.isPredefined() || !hiddenCategoriesIds.contains(category.getId()))
                .map(categoryMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public CategoryDto createCategory(CreateCategoryRequest request) {
        User user = getCurrentUser();
        String name = request.getName().trim();

        if (categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(user.getId(), name, request.getType()))
            throw new DuplicateCategoryNameException(DUPLICATE_CATEGORY_CREATE);

        Category category = Category.builder()
                .user(user)
                .name(name)
                .type(request.getType())
                .predefined(false)
                .icon(request.getIcon())
                .colorCode(request.getColorCode())
                .build();

        categoryRepository.save(category);

        log.info("User [{}] created custom category '{}', type={}", user.getEmail(), name, request.getType());

        return categoryMapper.toDto(category);
    }

    @Transactional
    @Override
    public CategoryDto updateCategory(UUID id, UpdateCategoryRequest request) {
        User user = getCurrentUser();
        String name = request.getName().trim();
        Category category = findCategoryOrThrow(id);

        if (category.isPredefined() || !user.equals(category.getUser()))
            throw new UnauthorizedCategoryAccessException();

        if (categoryRepository.existsByUser_IdAndNameIgnoreCaseAndType(user.getId(), name, category.getType())
                && !category.getName().equalsIgnoreCase(name))
            throw new DuplicateCategoryNameException(DUPLICATE_CATEGORY_UPDATE);

        category.setName(name);
        category.setIcon(request.getIcon());
        category.setColorCode(request.getColorCode());

        categoryRepository.save(category);

        log.info("User [{}] updated category ID [{}]: name '{}'", user.getEmail(), category.getId(), name);

        return categoryMapper.toDto(category);
    }

    @Transactional
    @Override
    public void deleteCategory(UUID id) {
        User user = getCurrentUser();
        Category category = findCategoryOrThrow(id);
        assertOwnership(category, user);

        UUID fallbackId = getFallbackIdForType(category.getType());

        Category fallback = categoryRepository.findById(fallbackId)
                .orElseThrow(() -> new EntityNotFoundException(FALLBACK_NOT_FOUND));

        if (category.getType() == CategoryType.EXPENSE) {
            expenseRepository.findAllByCategory_Id(category.getId())
                    .forEach(expense -> {
                        expense.setCategory(fallback);
                        expenseRepository.save(expense);
                    });
        } else if (category.getType() == CategoryType.INCOME) {
            incomeRepository.findAllByCategory_Id(category.getId())
                    .forEach(income -> {
                        income.setCategory(fallback);
                        incomeRepository.save(income);
                    });
        }

        categoryRepository.delete(category);

        log.info("User [{}] deleted custom category '{}', ID={}, type={}. Transactions reassigned to '{}', fallback ID={}",
                user.getEmail(), category.getName(), category.getId(), category.getType(),
                fallback.getName(), fallback.getId());
    }

    @Transactional
    @Override
    public void hideCategory(HideCategoryRequest request) {
        User user = getCurrentUser();
        Category category = findCategoryOrThrow(request.getCategoryId());

        if (!category.isPredefined())
            throw new HiddenCategoryException(HIDE_NON_SYSTEM);

        if (hiddenCategoryRepository.existsByUser_IdAndCategory_Id(user.getId(), category.getId()))
            throw new HiddenCategoryException(ALREADY_HIDDEN);

        HiddenCategory hiddenCategory = HiddenCategory.builder()
                .user(user)
                .category(category)
                .build();

        hiddenCategoryRepository.save(hiddenCategory);

        log.info("User [{}] hid system category '{}', ID={}", user.getEmail(), category.getName(), category.getId());
    }

    @Transactional
    @Override
    public void unhideCategory(UUID hiddenCategoryId) {
        User user = getCurrentUser();

        HiddenCategory hiddenCategory = hiddenCategoryRepository.findById(hiddenCategoryId)
                .orElseThrow(() -> new EntityNotFoundException(HIDDEN_CATEGORY_NOT_FOUND));

        if (!hiddenCategory.getUser().equals(user))
            throw new UnauthorizedCategoryAccessException();

        hiddenCategoryRepository.delete(hiddenCategory);

        log.info("User [{}] unhid category '{}', ID={}", user.getEmail(), hiddenCategory.getCategory().getName(), hiddenCategory.getCategory().getId());
    }
}
