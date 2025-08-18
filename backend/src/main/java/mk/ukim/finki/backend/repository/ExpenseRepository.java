package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
