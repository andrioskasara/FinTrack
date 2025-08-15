package mk.ukim.finki.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.category.CategoryDto;
import mk.ukim.finki.backend.model.dto.category.CreateCategoryRequest;
import mk.ukim.finki.backend.model.dto.category.HideCategoryRequest;
import mk.ukim.finki.backend.model.dto.category.UpdateCategoryRequest;
import mk.ukim.finki.backend.model.enums.CategoryType;
import mk.ukim.finki.backend.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the category management API.
 * <p>
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Retrieves all categories of a given type visible to the authenticated user.
     *
     * @param type EXPENSE or INCOME
     * @return list of category DTOs
     */
    @GetMapping
    public List<CategoryDto> getCategories(@RequestParam CategoryType type) {
        return categoryService.getAllCategories(type);
    }

    /**
     * Creates a new custom category.
     *
     * @param request creation data
     * @return created category DTO
     */
    @PostMapping
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    /**
     * Updates an existing custom category.
     *
     * @param id      category id
     * @param request update data
     * @return updated category DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Deletes a custom category.
     *
     * @param id category id
     * @return empty response (204 NO CONTENT)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Hides a system category for the current user.
     *
     * @param request hide request
     * @return success response
     */
    @PostMapping("/hide")
    public ResponseEntity<Void> hide(@RequestBody HideCategoryRequest request) {
        categoryService.hideCategory(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Unhides a system category previously hidden by the user.
     *
     * @param hiddenCategoryId ID of HiddenCategory mapping
     * @return empty response (204 NO CONTENT)
     */
    @DeleteMapping("/hidden/{hiddenCategoryId}")
    public ResponseEntity<Void> unhide(@PathVariable UUID hiddenCategoryId) {
        categoryService.unhideCategory(hiddenCategoryId);
        return ResponseEntity.noContent().build();
    }
}
