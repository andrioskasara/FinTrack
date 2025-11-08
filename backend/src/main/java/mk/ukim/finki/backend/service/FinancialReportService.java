package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.model.dto.report.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Service responsible for generating financial reports and dashboards.
 */
public interface FinancialReportService {

    /**
     * Generates a light version of the financial report (dashboard) for the given date range.
     * <p>
     * The dashboard includes:
     * - Total income
     * - Total expenses
     * - Balance
     * - Income and expense breakdown by category
     * </p>
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return a {@link FinancialReportDto} with aggregated dashboard data
     */
    FinancialReportDto generateDashboard(LocalDate from, LocalDate to);

    /**
     * Generates a detailed financial report for the given date range.
     * <p>
     * This includes all dashboard data plus:
     * - Budget performance (spent amount, progress, exceeded status)
     * - Saving goal progress (target vs current)
     * </p>
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return a {@link FinancialReportDto} with detailed report data
     */
    FinancialReportDto generateReport(LocalDate from, LocalDate to);

    /**
     * Returns monthly trend data for line charts.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return list of monthly trend DTOs
     */
    List<MonthlyTrendDto> getMonthlyTrends(LocalDate from, LocalDate to);

    /**
     * Returns category breakdown with analytics.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @param type category type (EXPENSE or INCOME)
     * @return category breakdown DTO
     */
    CategoryBreakdownDto getCategoryBreakdown(LocalDate from, LocalDate to, String type);

    /**
     * Returns budget performance overview.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return budget performance DTO
     */
    BudgetPerformanceDto getBudgetPerformance(LocalDate from, LocalDate to);

    /**
     * Returns quick statistics for dashboard cards.
     *
     * @param from start date
     * @param to   end date
     * @return quick statistics DTO
     */
    QuickStatsDto getQuickStats(LocalDate from, LocalDate to);


    /**
     * Exports a given {@link FinancialReportDto} into a PDF document.
     * <p>
     * The PDF includes:
     * - Totals (income, expenses, balance)
     * - Pie charts for income and expense categories
     * - Budget summary table
     * - Saving goal summary table
     * </p>
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return a byte array containing the generated PDF content
     * @throws Exception if the PDF generation fails
     */
    byte[] exportToPdf(LocalDate from, LocalDate to) throws Exception;

    /**
     * Exports a given {@link FinancialReportDto} into a CSV document.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return a byte array containing the generated CSV content
     */
    byte[] exportToCsv(LocalDate from, LocalDate to);

    FinancialReportDto generateReportWithFilters(ReportFilterDto filters);
}
