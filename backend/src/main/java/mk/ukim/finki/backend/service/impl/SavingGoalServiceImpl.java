package mk.ukim.finki.backend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.backend.exception.SavingGoalValidationException;
import mk.ukim.finki.backend.mapper.GoalContributionMapper;
import mk.ukim.finki.backend.mapper.SavingGoalMapper;
import mk.ukim.finki.backend.model.dto.saving_goal.*;
import mk.ukim.finki.backend.model.entity.GoalContribution;
import mk.ukim.finki.backend.model.entity.SavingGoal;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.model.enums.GoalContributionType;
import mk.ukim.finki.backend.repository.GoalContributionRepository;
import mk.ukim.finki.backend.repository.SavingGoalRepository;
import mk.ukim.finki.backend.service.SavingGoalService;
import mk.ukim.finki.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static mk.ukim.finki.backend.util.SavingGoalServiceMessages.*;

/**
 * Implementation of {@link SavingGoalService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavingGoalServiceImpl implements SavingGoalService {

    private final SavingGoalRepository savingGoalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final UserService userService;
    private final SavingGoalMapper savingGoalMapper;
    private final GoalContributionMapper goalContributionMapper;

    /**
     * Validates business rules for updating a saving goal.
     * Ensures the new target amount is not less than the already saved amount.
     *
     * @param request update request containing new target amount
     * @param current current saving goal entity
     * @throws SavingGoalValidationException if target amount is less than current amount
     */
    private void validateUpdateRequest(UpdateSavingGoalRequest request, SavingGoal current) {
        if (request.getTargetAmount().compareTo(current.getCurrentAmount()) < 0) {
            throw new SavingGoalValidationException(TARGET_NOT_LESS_THAN_CURRENT);
        }
    }

    /**
     * Validates that a withdrawal amount does not exceed the current saved amount.
     *
     * @param savingGoal the saving goal to withdraw from
     * @param amount     the amount to withdraw
     * @throws SavingGoalValidationException if the current amount is less than the requested withdrawal
     */
    private void validateWithdrawal(SavingGoal savingGoal, BigDecimal amount) {
        if (savingGoal.getCurrentAmount().compareTo(amount) < 0) {
            throw new SavingGoalValidationException(WITHDRAW_NOT_ENOUGH);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingGoalDto> getAllSavingGoals() {
        User user = userService.getCurrentUser();

        return savingGoalRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(savingGoalMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGoalDto getSavingGoalById(UUID id) {
        User user = userService.getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException(SAVING_GOAL_NOT_FOUND));  // ✅ Use constant

        return savingGoalMapper.toDto(savingGoal);
    }

    @Override
    @Transactional
    public SavingGoalDto createSavingGoal(CreateSavingGoalRequest request) {
        User user = userService.getCurrentUser();

        SavingGoal savingGoal = SavingGoal.builder()
                .user(user)
                .name(request.getName().trim())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .deadline(request.getDeadline())
                .achieved(false)
                .build();

        savingGoalRepository.save(savingGoal);

        log.info("User [{}] created saving goal [{}] target={} deadline={}",
                user.getEmail(), savingGoal.getName(), savingGoal.getTargetAmount(), savingGoal.getDeadline());

        return savingGoalMapper.toDto(savingGoal);
    }

    @Override
    @Transactional
    public SavingGoalDto updateSavingGoal(UUID id, UpdateSavingGoalRequest request) {
        User user = userService.getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException(SAVING_GOAL_NOT_FOUND));  // ✅ Use constant

        validateUpdateRequest(request, savingGoal);

        savingGoal.setName(request.getName().trim());
        savingGoal.setTargetAmount(request.getTargetAmount());
        savingGoal.setDeadline(request.getDeadline());
        savingGoal.setAchieved(savingGoal.getCurrentAmount().compareTo(savingGoal.getTargetAmount()) >= 0);

        savingGoalRepository.save(savingGoal);

        log.info("User [{}] updated saving goal [{}]", user.getEmail(), savingGoal.getId());

        return savingGoalMapper.toDto(savingGoal);
    }

    @Override
    @Transactional
    public void deleteSavingGoal(UUID id) {
        User user = userService.getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException(SAVING_GOAL_NOT_FOUND));  // ✅ Use constant

        savingGoalRepository.delete(savingGoal);

        log.info("User [{}] deleted saving goal [{}]", user.getEmail(), savingGoal.getId());
    }

    @Override
    @Transactional
    public SavingGoalDto addContribution(UUID id, GoalContributionRequest request) {
        User user = userService.getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException(SAVING_GOAL_NOT_FOUND));  // ✅ Use constant

        GoalContribution contribution = GoalContribution.builder()
                .savingGoal(savingGoal)
                .amount(request.getAmount())
                .type(GoalContributionType.DEPOSIT)
                .build();
        goalContributionRepository.save(contribution);

        savingGoal.setCurrentAmount(savingGoal.getCurrentAmount().add(request.getAmount()));
        savingGoal.updateAchievedStatus();
        savingGoalRepository.save(savingGoal);

        log.info("User [{}] contributed {} to saving goal [{}] (current={})",
                user.getEmail(), request.getAmount(), savingGoal.getId(), savingGoal.getCurrentAmount());

        return savingGoalMapper.toDto(savingGoal);
    }

    @Override
    @Transactional
    public SavingGoalDto withdrawContribution(UUID id, GoalContributionRequest request) {
        User user = userService.getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException(SAVING_GOAL_NOT_FOUND));  // ✅ Use constant

        validateWithdrawal(savingGoal, request.getAmount());

        GoalContribution contribution = GoalContribution.builder()
                .savingGoal(savingGoal)
                .amount(request.getAmount())
                .type(GoalContributionType.WITHDRAWAL)
                .build();
        goalContributionRepository.save(contribution);

        savingGoal.setCurrentAmount(savingGoal.getCurrentAmount().subtract(request.getAmount()));
        savingGoal.updateAchievedStatus();
        savingGoalRepository.save(savingGoal);

        log.info("User [{}] withdrew {} from saving goal [{}] (current={})",
                user.getEmail(), request.getAmount(), savingGoal.getId(), savingGoal.getCurrentAmount());

        return savingGoalMapper.toDto(savingGoal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalContributionDto> getContributions(UUID savingGoalId) {
        User user = userService.getCurrentUser();

        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(savingGoalId, user)
                .orElseThrow(() -> new EntityNotFoundException(SAVING_GOAL_NOT_FOUND));  // ✅ Use constant

        return goalContributionRepository.findBySavingGoalOrderByCreatedAtDesc(savingGoal)
                .stream()
                .map(goalContributionMapper::toDto)
                .collect(Collectors.toList());
    }
}
