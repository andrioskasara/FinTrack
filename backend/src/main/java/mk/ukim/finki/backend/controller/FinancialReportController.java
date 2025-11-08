package mk.ukim.finki.backend.controller;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.report.*;
import mk.ukim.finki.backend.service.FinancialReportService;
import mk.ukim.finki.backend.util.ReportConstants;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for financial reports and analytics.
 * Provides endpoints for dashboard, reports, trends, and exports.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FinancialReportController {

    private final FinancialReportService reportService;

    /**
     * Returns a summary dashboard with key financial metrics.
     *
     * @param from start date of the period (inclusive)
     * @param to   end date of the period (inclusive)
     * @return financial report DTO with dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<FinancialReportDto> getDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        FinancialReportDto dashboard = reportService.generateDashboard(from, to);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Returns a financial report with full analytics.
     *
     * @param from start date of the reporting period (inclusive)
     * @param to   end date of the reporting period (inclusive)
     * @return detailed financial report with budgets and saving goals
     */
    @GetMapping("/reports")
    public ResponseEntity<FinancialReportDto> getFullReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        FinancialReportDto report = reportService.generateReport(from, to);
        return ResponseEntity.ok(report);
    }

    /**
     * Returns quick statistics for dashboard cards.
     * Defaults to current month if no dates provided.
     *
     * @param from start date (optional)
     * @param to   end date (optional)
     * @return quick statistics DTO
     */
    @GetMapping("/quick-stats")
    public ResponseEntity<QuickStatsDto> getQuickStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null || to == null) {
            LocalDate now = LocalDate.now();
            from = now.withDayOfMonth(1);
            to = now.withDayOfMonth(now.lengthOfMonth());
        }

        QuickStatsDto stats = reportService.getQuickStats(from, to);
        return ResponseEntity.ok(stats);
    }

    /**
     * Returns monthly trend data for line charts.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return list of monthly trend DTOs
     */
    @GetMapping("/trends/monthly")
    public ResponseEntity<List<MonthlyTrendDto>> getMonthlyTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<MonthlyTrendDto> trends = reportService.getMonthlyTrends(from, to);
        return ResponseEntity.ok(trends);
    }

    /**
     * Returns category breakdown with analytics.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @param type category type (EXPENSE or INCOME), defaults to EXPENSE
     * @return category breakdown DTO
     */
    @GetMapping("/categories/breakdown")
    public ResponseEntity<CategoryBreakdownDto> getCategoryBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "EXPENSE") String type) {

        CategoryBreakdownDto breakdown = reportService.getCategoryBreakdown(from, to, type);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Returns budget performance overview.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return budget performance DTO
     */
    @GetMapping("/budgets/performance")
    public ResponseEntity<BudgetPerformanceDto> getBudgetPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        BudgetPerformanceDto performance = reportService.getBudgetPerformance(from, to);
        return ResponseEntity.ok(performance);
    }

    /**
     * Exports financial report to PDF format.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return PDF file as byte array with proper headers
     */
    @GetMapping(value = "/reports/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportToPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        try {
            byte[] pdfBytes = reportService.exportToPdf(from, to);
            String filename = String.format(ReportConstants.PDF_FILENAME_FORMAT, from, to);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF", e);
        }
    }

    /**
     * Exports financial data to CSV format.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return CSV file as byte array with proper headers
     */
    @GetMapping(value = "/reports/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportToCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] csvBytes = reportService.exportToCsv(from, to);
        String filename = String.format(ReportConstants.CSV_FILENAME_FORMAT, from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvBytes);
    }
}