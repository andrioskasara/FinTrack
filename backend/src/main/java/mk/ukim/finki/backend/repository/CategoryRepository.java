package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing and querying {@link Category} entities.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findById(UUID id);

    /**
     * Returns a list of custom categories for the user plus all system (predefined) categories,
     * filtered by category type.
     *
     * @param userId user id
     * @param type   category type (EXPENSE/INCOME)
     * @return list of all eligible categories for user and type
     */
    @Query("""
            SELECT c FROM Category c
             WHERE (c.user.id = :userId AND c.type = :type)
                OR (c.predefined = true AND c.type = :type)
            """)
    List<Category> findVisibleByUserIdAndType(@Param("userId") UUID userId, @Param("type") CategoryType type);

    Optional<Category> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<Category> findByUser_IdAndNameIgnoreCaseAndType(UUID userId, String string, CategoryType categoryType);

    boolean existsByUser_IdAndNameIgnoreCaseAndType(UUID userId, String name, CategoryType type);

    List<Category> findByPredefinedTrueAndType(CategoryType type);
}
