package mk.ukim.finki.backend.controller;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.report.FinancialReportDto;
import mk.ukim.finki.backend.service.FinancialReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for financial reports and analytics.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FinancialReportController {

    private final FinancialReportService reportService;

    /**
     * Returns a summary dashboard with key financial metrics.
     *
     * @param from start date
     * @param to   end date
     * @return financial report DTO
     */
    @GetMapping("/dashboard")
    public ResponseEntity<FinancialReportDto> getDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportService.generateDashboard(from, to));
    }

    /**
     * Returns a financial report with full analytics.
     *
     * @param from start date of the reporting period
     * @param to   end date of the reporting period
     * @return detailed financial report
     */
    @GetMapping("/reports")
    public ResponseEntity<FinancialReportDto> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportService.generateReport(from, to));
    }

    /**
     * Exports the financial report to a PDF file.
     *
     * @param from start date of the reporting period
     * @param to   end date of the reporting period
     * @return PDF file as a byte array
     */
    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) throws Exception {
        byte[] pdf = reportService.exportToPdf(from, to);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                .body(pdf);
    }
}
