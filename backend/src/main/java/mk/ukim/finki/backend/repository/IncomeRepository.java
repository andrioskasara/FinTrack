package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.dto.report.CategorySummaryDto;
import mk.ukim.finki.backend.model.dto.report.MonthlyTrendProjection;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.Income;
import mk.ukim.finki.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Income entity.
 */
@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID> {

    /**
     * Find all incomes by user id ordered descending by date and createdAt.
     *
     * @param userId id of user
     * @return list of incomes
     */
    List<Income> findAllByUser_IdOrderByDateDescCreatedAtDesc(UUID userId);

    /**
     * Check if an income exists by id and user id.
     * Used for authorization.
     *
     * @param incomeId id of income
     * @param userId   id of user
     * @return true if exists, false otherwise
     */
    boolean existsByIdAndUser_Id(UUID incomeId, UUID userId);

    /**
     * Find all incomes assigned to the given category.
     *
     * @param categoryId id of category
     * @return list of incomes in the category
     */
    List<Income> findAllByCategory_Id(UUID categoryId);

    /**
     * Aggregates total income amounts by category for a given user and date range.
     * This is used to build reports and category-based charts.
     *
     * @param user the user
     * @param from start date of the report period (inclusive)
     * @param to   end date of the report period (inclusive)
     * @return list of category summaries sorted by total amount descending
     */
    @Query("""
            SELECT new mk.ukim.finki.backend.model.dto.report.CategorySummaryDto(c.name, SUM(i.amount))
            FROM Income i
            JOIN i.category c
            WHERE i.user = :user
            AND i.date BETWEEN :from AND :to
            GROUP BY c.name
            ORDER BY SUM(i.amount) DESC
            """)
    List<CategorySummaryDto> sumByCategory(@Param("user") User user,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    /**
     * Bulk reassigns all incomes from one category to another.
     * <p>
     * This is used when a custom category is deleted to reassign all associated
     * transactions to a fallback category (typically "Uncategorized").
     * <p>
     *
     * @param oldCategoryId the ID of the category being deleted
     * @param newCategory   the fallback category to reassign incomes to
     */
    @Modifying
    @Query("UPDATE Income i SET i.category = :newCategory WHERE i.category.id = :oldCategoryId")
    void reassignCategory(@Param("oldCategoryId") UUID oldCategoryId,
                          @Param("newCategory") Category newCategory);

    /**
     * Calculates total income amount for a user within a date range.
     *
     * @param user the user
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return total income amount, zero if no incomes found
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.user = :user AND i.date BETWEEN :from AND :to")
    BigDecimal sumAmountByUserAndDateRange(@Param("user") User user,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    /**
     * Finds monthly income trends for a user within a date range.
     *
     * @param user the user
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return list of monthly trend projections
     */
    @Query("""
            SELECT 
                YEAR(i.date) AS year,
                MONTH(i.date) AS month,
                SUM(i.amount) AS totalAmount
            FROM Income i
            WHERE i.user = :user AND i.date BETWEEN :from AND :to
            GROUP BY YEAR(i.date), MONTH(i.date)
            ORDER BY YEAR(i.date), MONTH(i.date)
            """)
    List<MonthlyTrendProjection> findMonthlyIncomeTrends(@Param("user") User user,
                                                         @Param("from") LocalDate from,
                                                         @Param("to") LocalDate to);
}
