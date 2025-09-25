package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.entity.Budget;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing and querying {@link Budget} entities.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /**
     * Finds all budgets for a given user, optionally filtered by category.
     *
     * @param user     owner of budgets
     * @param category optional category filter (nullable for overall budgets)
     * @return list of budgets
     */
    List<Budget> findByUserAndCategory(User user, Category category);

    /**
     * Finds all budgets for a user sorted by start date descending.
     *
     * @param user owner
     * @return list of budgets
     */
    List<Budget> findByUserOrderByStartDateDesc(User user);

    /**
     * Finds a budget by id and owner.
     *
     * @param id   budget id
     * @param user owner
     * @return optional budget
     */
    Optional<Budget> findByIdAndUser(UUID id, User user);

    /**
     * Finds all budgets for a given user that are archived (expired), sorted by end date descending.
     * <p>
     * These budgets are considered "expired" and are typically read-only for analytics or rollover purposes.
     *
     * @param user owner of the budgets
     * @return list of expired budgets
     */
    List<Budget> findByUserAndArchivedTrueOrderByEndDateDesc(User user);

    /**
     * Finds all active budgets for a user within a date range.
     * Only non-archived budgets are returned.
     *
     * @param user owner of budgets
     * @param date any date within budget start and end dates
     * @return list of active budgets
     */
    @Query("""
            SELECT b from Budget b
            WHERE b.user = :user
            AND b.archived = false
            AND :date BETWEEN b.startDate AND b.endDate
            """)
    List<Budget> findActiveBudgets(@Param("user") User user,
                                   @Param("date") LocalDate date);

    /**
     * Checks for overlapping budgets for a user and optional category.
     * Overlap means: existing.start <= new.end AND existing.end >= new.start
     *
     * @param user      budget owner
     * @param category  optional category (null = overall)
     * @param startDate new budget start
     * @param endDate   new budget end
     * @return list of overlapping budgets
     */
    @Query("""
            SELECT b from Budget b
            WHERE b.user = :user
            AND (:category IS NULL OR b.category = :category)
            AND b.archived = false
            AND b.startDate <= :endDate
            AND b.endDate >= :startDate
            """)
    List<Budget> findOverlappingBudgets(@Param("user") User user,
                                        @Param("category") Category category,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    /**
     * Calculates the total amount spent by a user within a given date range.
     * If a category is provided, only expenses in that category are included.
     * If the category is null, calculate overall spending for the user in that period.
     *
     * @param user      owner of the expenses
     * @param category  optional category (null = overall)
     * @param startDate start date of the period
     * @param endDate   end date of the period
     * @return the total amount spent by the user in the given period
     */
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
            WHERE e.user = :user
              AND (:category IS NULL OR e.category = :category)
              AND e.date >= :startDate
              AND e.date <= :endDate
            """)
    BigDecimal sumSpentByBudget(@Param("user") User user,
                                @Param("category") Category category,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);
}
