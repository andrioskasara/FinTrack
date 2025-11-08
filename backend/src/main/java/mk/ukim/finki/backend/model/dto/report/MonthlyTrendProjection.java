package mk.ukim.finki.backend.model.dto.report;

import java.math.BigDecimal;

/**
 * Projection interface for monthly trend data aggregation.
 */
public interface MonthlyTrendProjection {

    /**
     * Gets the year part of the period.
     *
     * @return the year
     */
    Integer getYear();

    /**
     * Gets the month part of the period.
     *
     * @return the month (1-12)
     */
    Integer getMonth();

    /**
     * Gets the total amount for the period.
     *
     * @return the total amount
     */
    BigDecimal getTotalAmount();
}
