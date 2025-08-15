package mk.ukim.finki.backend.model.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mk.ukim.finki.backend.model.enums.CategoryType;

/**
 * Request DTO for creating new custom categories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryRequest {
    @NotBlank
    private String name;

    @NotNull
    private CategoryType type;

    private String icon;

    private String colorCode;
}
