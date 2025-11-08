package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.dto.report.CategorySummaryDto;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.Expense;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.model.dto.report.MonthlyTrendProjection;
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
 * Repository for Expense entity.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /**
     * Find all expenses by user id ordered descending by date and createdAt.
     *
     * @param userId id of user
     * @return list of expenses
     */
    List<Expense> findAllByUser_IdOrderByDateDescCreatedAtDesc(UUID userId);

    /**
     * Check if an expense exists by id and user id.
     * Used for authorization.
     *
     * @param expenseId id of expense
     * @param userId    id of user
     * @return true if exists, false otherwise
     */
    boolean existsByIdAndUser_Id(UUID expenseId, UUID userId);

    /**
     * Find all expenses assigned to the given category.
     *
     * @param categoryId id of category
     * @return list of expenses in the category
     */
    List<Expense> findAllByCategory_Id(UUID categoryId);

    /**
     * Aggregates total expense amounts by category for a given user and date range.
     * This is used to build reports and category-based charts.
     *
     * @param user the user
     * @param from start date of the report period (inclusive)
     * @param to   end date of the report period (inclusive)
     * @return list of category summaries sorted by total amount descending
     */
    @Query("""
            SELECT new mk.ukim.finki.backend.model.dto.report.CategorySummaryDto(c.name, SUM(e.amount))
            FROM Expense e
            JOIN e.category c
            WHERE e.user = :user
            AND e.date BETWEEN :from AND :to
            GROUP BY c.name
            ORDER BY SUM(e.amount) DESC
            """)
    List<CategorySummaryDto> sumByCategory(@Param("user") User user,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    /**
     * Bulk reassigns all expenses from one category to another.
     * <p>
     * This is used when a custom category is deleted to reassign all associated
     * transactions to a fallback category (typically "Uncategorized").
     * <p>
     *
     * @param oldCategoryId the ID of the category being deleted
     * @param newCategory   the fallback category to reassign expenses to
     */
    @Modifying
    @Query("UPDATE Expense e SET e.category = :newCategory WHERE e.category.id = :oldCategoryId")
    void reassignCategory(@Param("oldCategoryId") UUID oldCategoryId,
                          @Param("newCategory") Category newCategory);

    /**
     * Calculates total expense amount for a user within a date range.
     *
     * @param user the user
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return total expense amount, zero if no expenses found
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) " +
            "FROM Expense e " +
            "WHERE e.user = :user " +
            "AND e.date BETWEEN :from AND :to")
    BigDecimal sumAmountByUserAndDateRange(@Param("user") User user,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    /**
     * Finds monthly expense trends for a user within a date range.
     *
     * @param user the user
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return list of monthly trend projections
     */
    @Query("""
            SELECT YEAR(e.date) AS year,
                   MONTH(e.date) AS month,
                   SUM(e.amount) AS totalAmount
            FROM Expense e
            WHERE e.user = :user AND e.date BETWEEN :from AND :to
            GROUP BY YEAR(e.date), MONTH(e.date)
            ORDER BY YEAR(e.date), MONTH(e.date)
            """)
    List<MonthlyTrendProjection> findMonthlyExpenseTrends(@Param("user") User user,
                                                          @Param("from") LocalDate from,
                                                          @Param("to") LocalDate to);
}
