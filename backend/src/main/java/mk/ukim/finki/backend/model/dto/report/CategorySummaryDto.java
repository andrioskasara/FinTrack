package mk.ukim.finki.backend.model.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents aggregated data for a single category in a report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySummaryDto {
    /**
     * Name of the category.
     */
    private String categoryName;

    /**
     * Total amount spent or received in this category.
     */
    private BigDecimal totalAmount;
}
