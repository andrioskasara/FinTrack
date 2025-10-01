package mk.ukim.finki.backend.mapper;

import mk.ukim.finki.backend.model.dto.saving_goal.SavingGoalDto;
import mk.ukim.finki.backend.model.entity.SavingGoal;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for mapping between {@link SavingGoal} entity and {@link SavingGoalDto}.
 */
@Mapper(componentModel = "spring")
public interface SavingGoalMapper {

    SavingGoalDto toDto(SavingGoal goal);

    @AfterMapping
    default void setComputedFields(SavingGoal goal, @MappingTarget SavingGoalDto dto) {
        dto.setCurrentAmount(goal.getCurrentAmount());
        dto.setProgressPercentage(calculateProgress(goal));
        dto.setAchieved(goal.isAchieved());
    }

    default float calculateProgress(SavingGoal goal) {
        if (goal.getTargetAmount() == null || goal.getTargetAmount().signum() <= 0)
            return 0f;
        if (goal.getCurrentAmount() == null) return 0f;
        var progress = goal.getCurrentAmount().divide(goal.getTargetAmount(), 2, java.math.RoundingMode.HALF_UP)
                .multiply(java.math.BigDecimal.valueOf(100));
        return Math.min(progress.floatValue(), 100f);
    }
}
