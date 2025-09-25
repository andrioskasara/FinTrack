package mk.ukim.finki.backend.mapper;

import mk.ukim.finki.backend.model.dto.budget.BudgetDto;
import mk.ukim.finki.backend.model.entity.Budget;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for mapping between {@link Budget} entity and {@link BudgetDto}.
 */
@Mapper(componentModel = "spring")
public interface BudgetMapper {

    BudgetDto toDto(Budget budget);

    @AfterMapping
    default void setCategoryFields(Budget budget, @MappingTarget BudgetDto dto) {
        if (budget.getCategory() != null) {
            dto.setCategoryId(budget.getCategory().getId());
            dto.setCategoryName(budget.getCategory().getName());
        } else {
            dto.setCategoryId(null);
            dto.setCategoryName("Overall");
        }

        dto.setProgressPercentage(budget.getProgressPercentage() != null ? budget.getProgressPercentage() : 0f);
    }
}
