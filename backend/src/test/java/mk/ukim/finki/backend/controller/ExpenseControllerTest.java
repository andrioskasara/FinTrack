package mk.ukim.finki.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.config.SecurityConfig;
import mk.ukim.finki.backend.exception.UnauthorizedTransactionAccessException;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseDto;
import mk.ukim.finki.backend.model.dto.transaction.ExpenseRequest;
import mk.ukim.finki.backend.security.JwtAuthenticationFilter;
import mk.ukim.finki.backend.service.ExpenseService;
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

@WebMvcTest(controllers = ExpenseController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JwtAuthenticationFilter.class)
        })
@AutoConfigureMockMvc(addFilters = false)
public class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID expenseId;
    private ExpenseDto expenseDto;
    private ExpenseRequest expenseRequest;

    @BeforeEach
    void setUp() {
        expenseId = UUID.randomUUID();

        expenseDto = ExpenseDto.builder()
                .id(expenseId)
                .amount(BigDecimal.TEN)
                .date(LocalDate.now())
                .description("Test expense")
                .categoryId(UUID.randomUUID())
                .categoryName("Food")
                .build();

        expenseRequest = ExpenseRequest.builder()
                .amount(BigDecimal.TEN)
                .date(LocalDate.now())
                .description("Test expense")
                .categoryId(expenseDto.getCategoryId())
                .build();
    }

    @Test
    void getAllExpenses_ReturnsOkAndExpensesList() throws Exception {
        when(expenseService.getAll())
                .thenReturn(List.of(expenseDto));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(expenseId.toString()))
                .andExpect(jsonPath("$[0].categoryName").value("Food"));
    }

    @Test
    void getExpenseById_ValidId_ReturnsOkAndExpense() throws Exception {
        when(expenseService.getById(expenseId))
                .thenReturn(expenseDto);

        mockMvc.perform(get("/api/expenses/{id}", expenseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    void getExpenseById_NotFound_ReturnsNotFound() throws Exception {
        when(expenseService.getById(expenseId))
                .thenThrow(new EntityNotFoundException("Expense not found"));

        mockMvc.perform(get("/api/expenses/{id}", expenseId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createExpense_ValidRequest_ReturnsCreatedAndExpense() throws Exception {
        when(expenseService.create(any(ExpenseRequest.class)))
                .thenReturn(expenseDto);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(10))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    void createExpense_InvalidRequest_ReturnsBadRequest() throws Exception {
        ExpenseRequest invalidRequest = ExpenseRequest.builder().build();

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateExpense_ValidIdAndRequest_ReturnsOkAndExpense() throws Exception {
        when(expenseService.update(any(UUID.class), any(ExpenseRequest.class)))
                .thenReturn(expenseDto);

        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    void updateExpense_NotFound_ReturnsNotFound() throws Exception {
        when(expenseService.update(any(UUID.class), any(ExpenseRequest.class)))
                .thenThrow(new EntityNotFoundException("Expense not found"));

        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExpense_ValidId_ReturnsNoContent() throws Exception {
        doNothing().when(expenseService).delete(expenseId);

        mockMvc.perform(delete("/api/expenses/{id}", expenseId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteExpense_NotFound_ReturnsNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Expense not found")).when(expenseService).delete(expenseId);

        mockMvc.perform(delete("/api/expenses/{id}", expenseId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExpense_NotAuthorized_ReturnsForbidden() throws Exception {
        doThrow(new UnauthorizedTransactionAccessException("Not authorized")).when(expenseService).delete(expenseId);

        mockMvc.perform(delete("/api/expenses/{id}", expenseId))
                .andExpect(status().isForbidden());
    }
}
