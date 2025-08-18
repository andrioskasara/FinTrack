package mk.ukim.finki.backend.mapper;

import mk.ukim.finki.backend.model.dto.transaction.IncomeDto;
import mk.ukim.finki.backend.model.entity.Income;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper for mapping between {@link Income} entity and {@link IncomeDto}.
 */
@Mapper(componentModel = "spring")
public interface IncomeMapper {
    IncomeDto toDto(Income income);

    List<IncomeDto> toDtoList(List<Income> incomes);

    @AfterMapping
    default void setCategoryFields(Income income, @MappingTarget IncomeDto dto) {
        if (income.getCategory() != null) {
            dto.setCategoryId(income.getCategory().getId());
            dto.setCategoryName(income.getCategory().getName());
        }
    }
}
