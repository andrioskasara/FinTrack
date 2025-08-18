package mk.ukim.finki.backend.mapper;

import mk.ukim.finki.backend.model.dto.transaction.ExpenseDto;
import mk.ukim.finki.backend.model.entity.Expense;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper for mapping between {@link Expense} entity and {@link ExpenseDto}.
 */
@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    ExpenseDto toDto(Expense expense);

    List<ExpenseDto> toDtoList(List<Expense> expenses);

    @AfterMapping
    default void setCategoryFields(Expense expense, @MappingTarget ExpenseDto dto) {
        if (expense.getCategory() != null) {
            dto.setCategoryId(expense.getCategory().getId());
            dto.setCategoryName(expense.getCategory().getName());
        }
    }
}
