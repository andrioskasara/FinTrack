package mk.ukim.finki.backend.util;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import mk.ukim.finki.backend.model.dto.report.CategorySummaryDto;
import mk.ukim.finki.backend.model.dto.report.FinancialReportDto;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import com.itextpdf.text.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static mk.ukim.finki.backend.util.PdfExportMkStrings.*;

/**
 * Utility class for generating financial reports in PDF format.
 * Encapsulates PDF document building, table and chart generation,
 * and formatting constants for professional appearance.
 */
public final class PdfExportUtil {
    private PdfExportUtil() {
    }

    private static final BaseColor HEADER_BG_COLOR = new BaseColor(30, 144, 255);
    private static final BaseColor TABLE_HEADER_COLOR = new BaseColor(224, 236, 255);
    private static final BaseColor TABLE_ROW_ALT_COLOR = new BaseColor(245, 248, 255);
    private static final BaseColor TEXT_COLOR = BaseColor.BLACK;

    private static final Color[] PIE_CHART_PALETTE = {
            new Color(70, 130, 180),
            new Color(100, 149, 237),
            new Color(176, 224, 230),
            new Color(65, 105, 225),
            new Color(135, 206, 250)
    };

    /**
     * Generates the financial report PDF bytes.
     *
     * @param report financial report data
     * @return PDF file bytes
     * @throws Exception on PDF generation errors
     */
    public static byte[] generateReportPdf(FinancialReportDto report) throws Exception {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = createFont("Helvetica-Bold", 26, TEXT_COLOR);
        Font headerFont = createFont("Helvetica-Bold", 16, HEADER_BG_COLOR);
        Font subHeaderFont = createFont("Helvetica-Bold", 14, HEADER_BG_COLOR);
        Font bodyFont = createFont("Helvetica", 12, TEXT_COLOR);
        Font boldBodyFont = createFont("Helvetica-Bold", 12, TEXT_COLOR);

        // Title
        Paragraph title = new Paragraph(TITLE, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(18f);
        document.add(title);

        // Period
        if (report.getFrom() != null && report.getTo() != null) {
            Paragraph period = new Paragraph(
                    String.format("%s: %s — %s",
                            PERIOD,
                            report.getFrom().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            report.getTo().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))),
                    bodyFont);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(24f);
            document.add(period);
        }

        // Summary Table
        document.add(createSummaryTable(report, headerFont, bodyFont, boldBodyFont));

        if (report.isEmptyData()) {
            Paragraph emptyMsg = new Paragraph(EMPTY_DATA_MSG, bodyFont);
            emptyMsg.setAlignment(Element.ALIGN_CENTER);
            emptyMsg.setSpacingBefore(50f);
            document.add(emptyMsg);
            document.close();
            return baos.toByteArray();
        }

        // Expenses by Category Chart
        if (!report.getExpenseByCategory().isEmpty()) {
            Paragraph heading = new Paragraph(EXPENSES_BY_CATEGORY, subHeaderFont);
            heading.setSpacingBefore(30f);
            heading.setSpacingAfter(8f);
            document.add(heading);
            document.add(generateCategoryPieChart(report.getExpenseByCategory(), EXPENSES_BY_CATEGORY));
        }

        // Income by Category Chart
        if (!report.getIncomeByCategory().isEmpty()) {
            Paragraph heading = new Paragraph(INCOME_BY_CATEGORY, subHeaderFont);
            heading.setSpacingBefore(30f);
            heading.setSpacingAfter(8f);
            document.add(heading);
            document.add(generateCategoryPieChart(report.getIncomeByCategory(), INCOME_BY_CATEGORY));
        }

        // Budgets Table
        addBudgetTable(document, report, bodyFont, boldBodyFont);

        // Saving Goals Table
        addSavingGoalsTable(document, report, bodyFont, boldBodyFont);

        document.close();
        return baos.toByteArray();
    }

    private static PdfPTable createSummaryTable(FinancialReportDto report, Font headerFont, Font bodyFont, Font boldBodyFont) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 2.5f, 2.5f});
        table.setSpacingAfter(36f);

        addHeaderCell(table, TOTAL_INCOME, headerFont);
        addHeaderCell(table, TOTAL_EXPENSE, headerFont);
        addHeaderCell(table, BALANCE, headerFont);

        addValueCell(table, formatCurrency(report.getTotalIncome()), bodyFont);
        addValueCell(table, formatCurrency(report.getTotalExpense()), bodyFont);

        BaseColor balanceColor = BaseColor.BLACK;
        if (report.getBalance().compareTo(BigDecimal.ZERO) > 0) balanceColor = new BaseColor(0, 128, 0);
        if (report.getBalance().compareTo(BigDecimal.ZERO) < 0) balanceColor = BaseColor.RED;

        addValueCell(table, formatCurrency(report.getBalance()), boldBodyFont, balanceColor);

        return table;
    }

    private static void addBudgetTable(Document doc, FinancialReportDto report, Font bodyFont, Font boldBodyFont) throws DocumentException {
        if (report.getBudgets().isEmpty()) return;

        Paragraph heading = new Paragraph(BUDGETS, boldBodyFont);
        heading.setSpacingBefore(36f);
        heading.setSpacingAfter(12f);
        doc.add(heading);

        List<String[]> rows = report.getBudgets().stream()
                .map(b -> new String[]{
                        b.getBudgetName(),
                        formatCurrency(b.getAmount()),
                        formatCurrency(b.getSpent()),
                        String.format("%.2f%%", b.getProgressPercentage())
                }).toList();

        addTable(doc, rows, bodyFont, boldBodyFont);
    }

    private static void addSavingGoalsTable(Document doc, FinancialReportDto report, Font bodyFont, Font boldBodyFont) throws DocumentException {
        if (report.getSavingGoals().isEmpty()) return;

        Paragraph heading = new Paragraph(SAVING_GOALS, boldBodyFont);
        heading.setSpacingBefore(36f);
        heading.setSpacingAfter(12f);
        doc.add(heading);

        List<String[]> rows = report.getSavingGoals().stream()
                .map(g -> new String[]{
                        g.getName(),
                        formatCurrency(g.getTargetAmount()),
                        formatCurrency(g.getCurrentAmount()),
                        String.format("%.2f%%", g.getProgressPercentage())
                }).toList();

        addTable(doc, rows, bodyFont, boldBodyFont);
    }

    private static void addTable(Document doc, List<String[]> rows, Font bodyFont, Font boldHeader) throws DocumentException {
        if (rows.isEmpty()) return;

        int columns = rows.get(0).length;
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingAfter(24f);

        if (columns == 4) {
            table.setWidths(new float[]{3f, 2f, 2f, 2f});
        }

        String[] headers = columns == 4
                ? new String[]{NAME, TARGET_AMOUNT, CURRENT_SPENT, PROGRESS}
                : new String[]{COL_1, COL_2};

        for (String hdr : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase(hdr, boldHeader));
            headerCell.setBackgroundColor(TABLE_HEADER_COLOR);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(8f);
            headerCell.setBorderWidth(0);
            table.addCell(headerCell);
        }

        boolean alternate = false;
        for (String[] row : rows) {
            BaseColor bg = alternate ? BaseColor.WHITE : TABLE_ROW_ALT_COLOR;
            for (String cellText : row) {
                PdfPCell cell = new PdfPCell(new Phrase(cellText, bodyFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(6f);
                cell.setHorizontalAlignment(cellText.matches(".*\\d.*") ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                cell.setBorderWidth(0.5f);
                cell.setBorderColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }
            alternate = !alternate;
        }

        doc.add(table);
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG_COLOR);
        cell.setPadding(10f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setUseAscender(true);
        table.addCell(cell);
    }

    private static void addValueCell(PdfPTable table, String text, Font font) {
        addValueCell(table, text, font, BaseColor.WHITE);
    }

    private static void addValueCell(PdfPTable table, String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setPadding(8f);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    private static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00" + CURRENCY_SYMBOL;
        return amount.setScale(2, RoundingMode.HALF_UP) + CURRENCY_SYMBOL;
    }

    private static Image generateCategoryPieChart(List<CategorySummaryDto> data, String title) throws Exception {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();

        for (CategorySummaryDto c : data) {
            dataset.setValue(c.getCategoryName(), c.getTotalAmount());
        }

        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);

        int i = 0;
        for (Comparable<?> key : dataset.getKeys()) {
            plot.setSectionPaint(key, PIE_CHART_PALETTE[i % PIE_CHART_PALETTE.length]);
            i++;
        }

        BufferedImage buffered = chart.createBufferedImage(540, 320, BufferedImage.TYPE_INT_ARGB, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "png", baos);
        Image img = Image.getInstance(baos.toByteArray());
        img.setAlignment(Element.ALIGN_CENTER);
        img.scaleToFit(540, 320);
        img.setSpacingBefore(12f);
        img.setSpacingAfter(24f);

        return img;
    }

    private static Font createFont(String name, int size, BaseColor color) {
        Font font = FontFactory.getFont(name, size, BaseColor.BLACK);
        font.setColor(color);
        return font;
    }
}