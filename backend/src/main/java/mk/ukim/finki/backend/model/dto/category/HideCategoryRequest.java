package mk.ukim.finki.backend.model.dto.category;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for hiding a system (predefined) category.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HideCategoryRequest {
    @NotNull
    private UUID categoryId;
}
