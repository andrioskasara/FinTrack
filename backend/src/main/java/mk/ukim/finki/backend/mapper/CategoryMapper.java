package mk.ukim.finki.backend.mapper;

import mk.ukim.finki.backend.model.dto.category.CategoryDto;
import mk.ukim.finki.backend.model.entity.Category;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for mapping between {@link Category} entity and {@link CategoryDto}.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toDto(Category category);

    List<CategoryDto> toDtoList(List<Category> categories);
}
