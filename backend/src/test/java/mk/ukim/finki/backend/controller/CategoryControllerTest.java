package mk.ukim.finki.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import mk.ukim.finki.backend.config.SecurityConfig;
import mk.ukim.finki.backend.exception.DuplicateCategoryNameException;
import mk.ukim.finki.backend.exception.HiddenCategoryException;
import mk.ukim.finki.backend.exception.UnauthorizedCategoryAccessException;
import mk.ukim.finki.backend.model.dto.category.CategoryDto;
import mk.ukim.finki.backend.model.dto.category.CreateCategoryRequest;
import mk.ukim.finki.backend.model.dto.category.HideCategoryRequest;
import mk.ukim.finki.backend.model.dto.category.UpdateCategoryRequest;
import mk.ukim.finki.backend.model.enums.CategoryType;
import mk.ukim.finki.backend.security.JwtAuthenticationFilter;
import mk.ukim.finki.backend.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JwtAuthenticationFilter.class)
        })
@AutoConfigureMockMvc(addFilters = false)
public class CategoryControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    CategoryService categoryService;
    @Autowired
    ObjectMapper objectMapper;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        categoryDto = new CategoryDto();
        categoryDto.setId(UUID.randomUUID());
        categoryDto.setName("Test Category");
    }

    private String asJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    void getCategories_success() throws Exception {
        when(categoryService.getAllCategories(CategoryType.EXPENSE))
                .thenReturn(List.of(categoryDto));

        mockMvc.perform(get("/api/categories").param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Category"));
    }

    @Test
    void createCategory_success() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Cat");
        request.setType(CategoryType.INCOME);

        when(categoryService.createCategory(any())).thenReturn(categoryDto);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Category"));
    }

    @Test
    void createCategory_duplicateThrows_conflict() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Duplicate");
        request.setType(CategoryType.INCOME);

        when(categoryService.createCategory(any()))
                .thenThrow(new DuplicateCategoryNameException("Category with this name and type already exists."));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Category with this name and type already exists."));
    }

    @Test
    void updateCategory_success() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Update");
        request.setIcon("icon");
        request.setColorCode("#fff");

        when(categoryService.updateCategory(any(), any())).thenReturn(categoryDto);

        mockMvc.perform(put("/api/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Category"));
    }

    @Test
    void updateCategory_notFound_throws404() throws Exception {
        when(categoryService.updateCategory(any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Category not found"));

        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Name");

        mockMvc.perform(put("/api/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(req)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));
    }

    @Test
    void updateCategory_unauthorized_throws403() throws Exception {
        when(categoryService.updateCategory(any(), any()))
                .thenThrow(new UnauthorizedCategoryAccessException());

        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Name");

        mockMvc.perform(put("/api/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(req)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("No permission to access this category"));
    }

    @Test
    void updateCategory_duplicateName_throws409() throws Exception {
        when(categoryService.updateCategory(any(), any()))
                .thenThrow(new DuplicateCategoryNameException("Duplicate category name for this type."));

        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Name");

        mockMvc.perform(put("/api/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(req)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Duplicate category name for this type."));
    }

    @Test
    void deleteCategory_success() throws Exception {
        doNothing().when(categoryService).deleteCategory(any());

        mockMvc.perform(delete("/api/categories/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_notFound() throws Exception {
        Mockito.doThrow(new jakarta.persistence.EntityNotFoundException("Category not found"))
                .when(categoryService).deleteCategory(any());

        mockMvc.perform(delete("/api/categories/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));
    }

    @Test
    void hideCategory_success() throws Exception {
        HideCategoryRequest request = new HideCategoryRequest();
        request.setCategoryId(UUID.randomUUID());

        doNothing().when(categoryService).hideCategory(any());

        mockMvc.perform(post("/api/categories/hide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk());
    }

    @Test
    void hideCategory_notFound() throws Exception {
        Mockito.doThrow(new jakarta.persistence.EntityNotFoundException("Category not found"))
                .when(categoryService).hideCategory(any());

        HideCategoryRequest request = new HideCategoryRequest();
        request.setCategoryId(UUID.randomUUID());

        mockMvc.perform(post("/api/categories/hide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));
    }

    @Test
    void hideCategory_alreadyHidden_badRequest() throws Exception {
        HideCategoryRequest request = new HideCategoryRequest();
        request.setCategoryId(UUID.randomUUID());

        Mockito.doThrow(new HiddenCategoryException("Already hidden"))
                .when(categoryService).hideCategory(any());

        mockMvc.perform(post("/api/categories/hide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Already hidden"));
    }

    @Test
    void unhideCategory_success() throws Exception {
        doNothing().when(categoryService).unhideCategory(any());

        mockMvc.perform(delete("/api/categories/hidden/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void unhideCategory_unauthorized_forbidden() throws Exception {
        Mockito.doThrow(new UnauthorizedCategoryAccessException())
                .when(categoryService).unhideCategory(any());

        mockMvc.perform(delete("/api/categories/hidden/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(content().string("No permission to access this category"));
    }

    @Test
    void unhideCategory_notFound() throws Exception {
        Mockito.doThrow(new jakarta.persistence.EntityNotFoundException("Hidden category not found"))
                .when(categoryService).unhideCategory(any());

        mockMvc.perform(delete("/api/categories/hidden/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Hidden category not found"));
    }
}
