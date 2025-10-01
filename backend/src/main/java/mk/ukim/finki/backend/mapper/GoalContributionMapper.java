package mk.ukim.finki.backend.mapper;

import mk.ukim.finki.backend.model.dto.saving_goal.GoalContributionDto;
import mk.ukim.finki.backend.model.entity.GoalContribution;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for mapping between {@link GoalContribution} entity and {@link GoalContributionDto}.
 */
@Mapper(componentModel = "spring")
public interface GoalContributionMapper {
    GoalContributionDto toDto(GoalContribution contribution);
}
