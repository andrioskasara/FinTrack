package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

}
