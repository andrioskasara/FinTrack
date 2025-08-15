package mk.ukim.finki.backend.repository;

import mk.ukim.finki.backend.model.entity.HiddenCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing {@link HiddenCategory} entities (hidden system categories).
 */
@Repository
public interface HiddenCategoryRepository extends JpaRepository<HiddenCategory, UUID> {
    List<HiddenCategory> findByUser_Id(UUID userId);

    Optional<HiddenCategory> findByUser_IdAndCategory_Id(UUID userId, UUID categoryId);

    boolean existsByUser_IdAndCategory_Id(UUID userId, UUID categoryId);
}
