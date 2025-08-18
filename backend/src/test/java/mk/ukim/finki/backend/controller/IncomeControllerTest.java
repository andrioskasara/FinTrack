package mk.ukim.finki.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.config.SecurityConfig;
import mk.ukim.finki.backend.security.JwtAuthenticationFilter;
import mk.ukim.finki.backend.model.dto.transaction.IncomeDto;
import mk.ukim.finki.backend.model.dto.transaction.IncomeRequest;
import mk.ukim.finki.backend.service.IncomeService;
import mk.ukim.finki.backend.exception.UnauthorizedTransactionAccessException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IncomeController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JwtAuthenticationFilter.class)
        })
@AutoConfigureMockMvc(addFilters = false)
public class IncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncomeService incomeService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID incomeId;
    private IncomeDto incomeDto;
    private IncomeRequest incomeRequest;

    @BeforeEach
    void setUp() {
        incomeId = UUID.randomUUID();

        incomeDto = IncomeDto.builder()
                .id(incomeId)
                .amount(BigDecimal.TEN)
                .date(LocalDate.now())
                .description("Test income")
                .categoryId(UUID.randomUUID())
                .categoryName("Salary")
                .build();

        incomeRequest = IncomeRequest.builder()
                .amount(BigDecimal.TEN)
                .date(LocalDate.now())
                .description("Test income")
                .categoryId(incomeDto.getCategoryId())
                .build();
    }

    @Test
    void getAllIncomes_ReturnsOkAndList() throws Exception {
        when(incomeService.getAll())
                .thenReturn(List.of(incomeDto));

        mockMvc.perform(get("/api/incomes"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(incomeId.toString()))
                .andExpect(jsonPath("$[0].categoryName").value("Salary"));
    }

    @Test
    void getIncomeById_Found_ReturnsOk() throws Exception {
        when(incomeService.getById(incomeId))
                .thenReturn(incomeDto);

        mockMvc.perform(get("/api/incomes/{id}", incomeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incomeId.toString()))
                .andExpect(jsonPath("$.categoryName").value("Salary"));
    }

    @Test
    void getIncomeById_NotFound_Returns404() throws Exception {
        when(incomeService.getById(incomeId))
                .thenThrow(new EntityNotFoundException("Income not found"));

        mockMvc.perform(get("/api/incomes/{id}", incomeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIncomeById_Forbidden_Returns403() throws Exception {
        when(incomeService.getById(incomeId))
                .thenThrow(new UnauthorizedTransactionAccessException("No permission"));

        mockMvc.perform(get("/api/incomes/{id}", incomeId))
                .andExpect(status().isForbidden());
    }

    @Test
    void createIncome_Valid_Returns201() throws Exception {
        when(incomeService.create(any(IncomeRequest.class)))
                .thenReturn(incomeDto);

        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(10))
                .andExpect(jsonPath("$.categoryName").value("Salary"));
    }

    @Test
    void createIncome_Invalid_Returns400() throws Exception {
        IncomeRequest invalid = IncomeRequest.builder().build();

        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateIncome_Valid_Returns200() throws Exception {
        when(incomeService.update(any(UUID.class), any(IncomeRequest.class)))
                .thenReturn(incomeDto);

        mockMvc.perform(put("/api/incomes/{id}", incomeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incomeId.toString()));
    }

    @Test
    void updateIncome_NotFound_Returns404() throws Exception {
        when(incomeService.update(any(UUID.class), any(IncomeRequest.class)))
                .thenThrow(new EntityNotFoundException("Income not found"));

        mockMvc.perform(put("/api/incomes/{id}", incomeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIncome_Valid_Returns204() throws Exception {
        doNothing().when(incomeService).delete(incomeId);

        mockMvc.perform(delete("/api/incomes/{id}", incomeId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteIncome_NotFound_Returns404() throws Exception {
        doThrow(new EntityNotFoundException("Income not found")).when(incomeService).delete(incomeId);

        mockMvc.perform(delete("/api/incomes/{id}", incomeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIncome_Forbidden_Returns403() throws Exception {
        doThrow(new UnauthorizedTransactionAccessException("No permission")).when(incomeService).delete(incomeId);

        mockMvc.perform(delete("/api/incomes/{id}", incomeId))
                .andExpect(status().isForbidden());
    }
}
