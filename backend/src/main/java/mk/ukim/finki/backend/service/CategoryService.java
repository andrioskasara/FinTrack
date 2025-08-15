package mk.ukim.finki.backend.service;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.exception.DuplicateCategoryNameException;
import mk.ukim.finki.backend.exception.HiddenCategoryException;
import mk.ukim.finki.backend.exception.UnauthorizedCategoryAccessException;
import mk.ukim.finki.backend.model.dto.category.CategoryDto;
import mk.ukim.finki.backend.model.dto.category.CreateCategoryRequest;
import mk.ukim.finki.backend.model.dto.category.HideCategoryRequest;
import mk.ukim.finki.backend.model.dto.category.UpdateCategoryRequest;
import mk.ukim.finki.backend.model.enums.CategoryType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing categories, including business rules and access control.
 * <p>
 * Business rules:
 * <ul>
 *   <li>Users can create, edit, delete their own custom categories only.</li>
 *   <li>System (predefined) categories cannot be edited or deleted, only hidden/unhidden per user.</li>
 *   <li>Hiding is only allowed for system categories.</li>
 *   <li>Custom category names must be unique per user and type.</li>
 *   <li>On deletion of a custom category, all related expenses/incomes must be reassigned to a system fallback.</li>
 * </ul>
 */
public interface CategoryService {

    /**
     * Retrieves all categories visible to the current user filtered by type.
     * Categories are visible if they are custom categories belonging to the user,
     * or system categories not hidden by the user.
     *
     * @param type category type (EXPENSE/INCOME)
     * @return list of visible categories
     */
    List<CategoryDto> getAllCategories(CategoryType type);

    /**
     * Creates a new custom category for the current user.
     *
     * @param request creation data for the category
     * @return created category DTO
     * @throws DuplicateCategoryNameException if the name/type is not unique for this user
     */
    CategoryDto createCategory(CreateCategoryRequest request);

    /**
     * Updates an existing custom category owned by the current user.
     *
     * @param id      category id
     * @param request update data
     * @return updated category DTO
     * @throws UnauthorizedCategoryAccessException if not owner or system category
     * @throws DuplicateCategoryNameException      if rename would cause a duplicate
     */
    CategoryDto updateCategory(UUID id, UpdateCategoryRequest request);

    /**
     * Deletes a custom category owned by the current user.
     * Related expense/income transactions must be reassigned to fallback categories.
     *
     * @param id category id to delete
     * @throws UnauthorizedCategoryAccessException if not owner or system category
     * @throws EntityNotFoundException             if fallback category is missing
     */
    void deleteCategory(UUID id);

    /**
     * Hides a system (predefined) category for the current user.
     *
     * @param request hide request
     * @throws HiddenCategoryException if already hidden or non-system category
     */
    void hideCategory(HideCategoryRequest request);

    /**
     * Unhides a system category that was previously hidden by the current user.
     *
     * @param hiddenCategoryId id of the hidden relationship to remove
     * @throws UnauthorizedCategoryAccessException if not owner
     */
    void unhideCategory(UUID hiddenCategoryId);
}
