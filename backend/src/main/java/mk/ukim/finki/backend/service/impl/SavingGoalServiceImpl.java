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
import mk.ukim.finki.backend.repository.UserRepository;
import mk.ukim.finki.backend.service.SavingGoalService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final UserRepository userRepository;
    private final SavingGoalMapper savingGoalMapper;
    private final GoalContributionMapper goalContributionMapper;

    /**
     * Retrieves the currently authenticated user.
     *
     * @return the User entity
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }

    /**
     * Validates business rules for updating a saving goal.
     *
     * @param request update request
     * @param current current saving goal
     */
    private void validateUpdateRequest(UpdateSavingGoalRequest request, SavingGoal current) {
        if (request.getTargetAmount().compareTo(current.getCurrentAmount()) < 0) {
            throw new SavingGoalValidationException(TARGET_NOT_LESS_THAN_CURRENT);
        }
    }

    /**
     * Validates business rules for withdrawing.
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
    public List<SavingGoalDto> getAllSavingGoals() {
        User user = getCurrentUser();

        return savingGoalRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(savingGoalMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SavingGoalDto getSavingGoalById(UUID id) {
        User user = getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(EntityNotFoundException::new);

        return savingGoalMapper.toDto(savingGoal);
    }

    @Override
    @Transactional
    public SavingGoalDto createSavingGoal(CreateSavingGoalRequest request) {
        User user = getCurrentUser();

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
        User user = getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(EntityNotFoundException::new);

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
        User user = getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(EntityNotFoundException::new);

        savingGoalRepository.delete(savingGoal);

        log.info("User [{}] deleted saving goal [{}]", user.getEmail(), savingGoal.getId());
    }

    @Override
    @Transactional
    public SavingGoalDto addContribution(UUID id, GoalContributionRequest request) {
        User user = getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(EntityNotFoundException::new);

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
        User user = getCurrentUser();
        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(EntityNotFoundException::new);

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
    public List<GoalContributionDto> getContributions(UUID savingGoalId) {
        User user = getCurrentUser();

        SavingGoal savingGoal = savingGoalRepository.findByIdAndUser(savingGoalId, user)
                .orElseThrow(EntityNotFoundException::new);

        return goalContributionRepository.findBySavingGoalOrderByCreatedAtDesc(savingGoal)
                .stream()
                .map(goalContributionMapper::toDto)
                .collect(Collectors.toList());
    }
}
