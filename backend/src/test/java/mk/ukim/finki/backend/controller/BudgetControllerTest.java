package mk.ukim.finki.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.config.SecurityConfig;
import mk.ukim.finki.backend.model.dto.budget.BudgetDto;
import mk.ukim.finki.backend.model.dto.budget.CreateBudgetRequest;
import mk.ukim.finki.backend.model.dto.budget.UpdateBudgetRequest;
import mk.ukim.finki.backend.security.JwtAuthenticationFilter;
import mk.ukim.finki.backend.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BudgetController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JwtAuthenticationFilter.class)
        })
@AutoConfigureMockMvc(addFilters = false)
public class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID budgetId;
    private BudgetDto budgetDto;
    private CreateBudgetRequest createRequest;
    private UpdateBudgetRequest updateRequest;

    @BeforeEach
    void setUp() {
        budgetId = UUID.randomUUID();

        budgetDto = BudgetDto.builder()
                .id(budgetId)
                .categoryId(UUID.randomUUID())
                .categoryName("Food")
                .amount(BigDecimal.valueOf(100))
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(5))
                .progressPercentage(50f)
                .isRollover(false)
                .archived(false)
                .build();

        createRequest = CreateBudgetRequest.builder()
                .categoryId(budgetDto.getCategoryId())
                .amount(BigDecimal.valueOf(100))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .isRollover(false)
                .build();

        updateRequest = UpdateBudgetRequest.builder()
                .categoryId(budgetDto.getCategoryId())
                .amount(BigDecimal.valueOf(150))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .isRollover(true)
                .archived(false)
                .build();
    }

    private String asJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    void getAllBudgets_success() throws Exception {
        when(budgetService.getAllBudgets())
                .thenReturn(List.of(budgetDto));

        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(budgetId.toString()))
                .andExpect(jsonPath("$[0].categoryName").value("Food"));
    }

    @Test
    void getBudgetById_success() throws Exception {
        when(budgetService.getBudgetById(budgetId))
                .thenReturn(budgetDto);

        mockMvc.perform(get("/api/budgets/{id}", budgetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId.toString()))
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void getBudgetById_notFound() throws Exception {
        when(budgetService.getBudgetById(budgetId))
                .thenThrow(new EntityNotFoundException("Budget not found"));

        mockMvc.perform(get("/api/budgets/{id}", budgetId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Budget not found"));
    }

    @Test
    void createBudget_success() throws Exception {
        when(budgetService.createBudget(any(CreateBudgetRequest.class)))
                .thenReturn(budgetDto);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    void createBudget_invalid_throwsBadRequest() throws Exception {
        CreateBudgetRequest invalidRequest = new CreateBudgetRequest();
        invalidRequest.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBudget_success() throws Exception {
        when(budgetService.updateBudget(eq(budgetId), any(UpdateBudgetRequest.class)))
                .thenReturn(budgetDto);

        mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId.toString()));
    }

    @Test
    void updateBudget_notFound() throws Exception {
        when(budgetService.updateBudget(eq(budgetId), any(UpdateBudgetRequest.class)))
                .thenThrow(new EntityNotFoundException("Budget not found"));

        mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Budget not found"));
    }

    @Test
    void deleteBudget_success() throws Exception {
        doNothing().when(budgetService).deleteBudget(budgetId);

        mockMvc.perform(delete("/api/budgets/{id}", budgetId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBudget_notFound() throws Exception {
        doThrow(new EntityNotFoundException("Budget not found")).when(budgetService).deleteBudget(budgetId);

        mockMvc.perform(delete("/api/budgets/{id}", budgetId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Budget not found"));
    }

    @Test
    void archiveExpiredBudgets_success() throws Exception {
        doNothing().when(budgetService).archiveExpiredBudgets();

        mockMvc.perform(post("/api/budgets/archive"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getExpiredBudgets_success() throws Exception {
        when(budgetService.getExpiredBudgets())
                .thenReturn(List.of(budgetDto));

        mockMvc.perform(get("/api/budgets/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(budgetId.toString()));
    }

    @Test
    void rolloverBudget_success() throws Exception {
        when(budgetService.rolloverBudget(budgetId))
                .thenReturn(budgetDto);

        mockMvc.perform(post("/api/budgets/{id}/rollover", budgetId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(budgetId.toString()));
    }
}
