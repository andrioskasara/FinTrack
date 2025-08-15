package mk.ukim.finki.backend.model.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mk.ukim.finki.backend.model.enums.CategoryType;

import java.util.UUID;

/**
 * Data Transfer Object for transferring category data to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {
    private UUID id;
    private String name;
    private CategoryType type;
    private boolean predefined;
    private String icon;
    private String colorCode;
}
