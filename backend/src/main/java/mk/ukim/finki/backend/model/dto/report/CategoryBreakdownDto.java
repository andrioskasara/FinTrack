package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mk.ukim.finki.backend.model.enums.CategoryType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed category breakdown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBreakdownDto {
    private CategoryType type;
    private BigDecimal totalAmount;
    private List<CategorySummaryDto> categories;
    private CategorySummaryDto topCategory;
    private int totalCategories;
}