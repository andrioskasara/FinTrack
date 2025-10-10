package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.dto.report.CategorySummaryDto;
import mk.ukim.finki.backend.model.entity.Income;
import mk.ukim.finki.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

}
