package mk.ukim.finki.backend.controller;

import mk.ukim.finki.backend.config.SecurityConfig;
import mk.ukim.finki.backend.model.dto.report.BudgetReportDto;
import mk.ukim.finki.backend.model.dto.report.CategorySummaryDto;
import mk.ukim.finki.backend.model.dto.report.FinancialReportDto;
import mk.ukim.finki.backend.model.dto.report.SavingGoalReportDto;
import mk.ukim.finki.backend.security.JwtAuthenticationFilter;
import mk.ukim.finki.backend.service.FinancialReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FinancialReportController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JwtAuthenticationFilter.class)
        })
@AutoConfigureMockMvc(addFilters = false)
public class FinancialReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinancialReportService reportService;

    private FinancialReportDto reportDto;
    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        from = LocalDate.of(2025, 1, 1);
        to = LocalDate.of(2025, 1, 31);

        reportDto = FinancialReportDto.builder()
                .totalIncome(BigDecimal.valueOf(1000))
                .totalExpense(BigDecimal.valueOf(400))
                .balance(BigDecimal.valueOf(600))
                .expenseByCategory(List.of(
                        new CategorySummaryDto("Food", BigDecimal.valueOf(200)),
                        new CategorySummaryDto("Transport", BigDecimal.valueOf(200))
                ))
                .incomeByCategory(List.of(
                        new CategorySummaryDto("Salary", BigDecimal.valueOf(1000))
                ))
                .budgets(List.of(
                        BudgetReportDto.builder()
                                .budgetName("Monthly Food Budget")
                                .amount(BigDecimal.valueOf(500))
                                .spent(BigDecimal.valueOf(200))
                                .progressPercentage(40f)
                                .exceeded(false)
                                .build()
                ))
                .savingGoals(List.of(
                        SavingGoalReportDto.builder()
                                .name("Vacation")
                                .targetAmount(BigDecimal.valueOf(2000))
                                .currentAmount(BigDecimal.valueOf(800))
                                .progressPercentage(40f)
                                .achieved(false)
                                .build()
                ))
                .emptyData(false)
                .build();
    }

    @Test
    void getDashboard_success() throws Exception {
        when(reportService.generateDashboard(from, to))
                .thenReturn(reportDto);

        mockMvc.perform(get("/api/dashboard")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000))
                .andExpect(jsonPath("$.totalExpense").value(400))
                .andExpect(jsonPath("$.balance").value(600))
                .andExpect(jsonPath("$.expenseByCategory[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.incomeByCategory[0].categoryName").value("Salary"))
                .andExpect(jsonPath("$.budgets[0].budgetName").value("Monthly Food Budget"))
                .andExpect(jsonPath("$.savingGoals[0].name").value("Vacation"))
                .andExpect(jsonPath("$.emptyData").value(false));
    }

    @Test
    void getDashboard_emptyData() throws Exception {
        FinancialReportDto emptyDto = FinancialReportDto.builder()
                .totalIncome(BigDecimal.ZERO)
                .totalExpense(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .expenseByCategory(List.of())
                .incomeByCategory(List.of())
                .budgets(List.of())
                .savingGoals(List.of())
                .emptyData(true)
                .build();

        when(reportService.generateDashboard(from, to)).thenReturn(emptyDto);

        mockMvc.perform(get("/api/dashboard")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.expenseByCategory").isEmpty())
                .andExpect(jsonPath("$.incomeByCategory").isEmpty())
                .andExpect(jsonPath("$.budgets").isEmpty())
                .andExpect(jsonPath("$.savingGoals").isEmpty())
                .andExpect(jsonPath("$.emptyData").value(true));
    }

    @Test
    void getDashboard_invalidDateParam() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .param("from", "invalid-date")
                        .param("to", "2025-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReport_success() throws Exception {
        when(reportService.generateReport(from, to))
                .thenReturn(reportDto);

        mockMvc.perform(get("/api/reports")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000))
                .andExpect(jsonPath("$.totalExpense").value(400))
                .andExpect(jsonPath("$.balance").value(600));
    }

    @Test
    void getReport_invalidDateParam() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .param("from", "2025-01-01")
                        .param("to", "not-a-date")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportReport_success() throws Exception {
        when(reportService.exportToPdf(from, to))
                .thenReturn("PDF_CONTENT".getBytes());

        mockMvc.perform(get("/api/reports/export")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("PDF_CONTENT".getBytes()));
    }

    @Test
    void exportReport_emptyPdf() throws Exception {
        when(reportService.exportToPdf(from, to))
                .thenReturn("PDF_EMPTY".getBytes());

        mockMvc.perform(get("/api/reports/export")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("PDF_EMPTY".getBytes()));
    }

    @Test
    void exportReport_invalidDateParam() throws Exception {
        mockMvc.perform(get("/api/reports/export")
                        .param("from", "bad-date")
                        .param("to", "also-bad")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isBadRequest());
    }
}

