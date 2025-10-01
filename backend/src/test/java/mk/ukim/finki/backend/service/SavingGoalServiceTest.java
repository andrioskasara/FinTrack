package mk.ukim.finki.backend.service;

import jakarta.persistence.EntityNotFoundException;
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
import mk.ukim.finki.backend.service.impl.SavingGoalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SavingGoalServiceTest {

    @Mock
    private SavingGoalRepository savingGoalRepository;

    @Mock
    private GoalContributionRepository goalContributionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SavingGoalMapper savingGoalMapper;

    @Mock
    private GoalContributionMapper goalContributionMapper;

    @InjectMocks
    private SavingGoalServiceImpl savingGoalService;

    private User user;
    private SavingGoal savingGoal;
    private UUID goalId;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .build();

        goalId = UUID.randomUUID();
        savingGoal = SavingGoal.builder()
                .id(goalId)
                .user(user)
                .name("Vacation")
                .targetAmount(BigDecimal.valueOf(1000))
                .currentAmount(BigDecimal.ZERO)
                .achieved(false)
                .build();

        mockAuthentication(user.getEmail());

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));
    }

    private void mockAuthentication(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAllSavingGoals_returnsList() {
        when(savingGoalRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(savingGoal));
        when(savingGoalMapper.toDto(savingGoal))
                .thenReturn(new SavingGoalDto());

        List<SavingGoalDto> result = savingGoalService.getAllSavingGoals();

        assertEquals(1, result.size());
        verify(savingGoalRepository).findByUserOrderByCreatedAtDesc(user);
    }

    @Test
    void getSavingGoalById_success() {
        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.of(savingGoal));
        when(savingGoalMapper.toDto(savingGoal))
                .thenReturn(new SavingGoalDto());

        SavingGoalDto dto = savingGoalService.getSavingGoalById(goalId);

        assertNotNull(dto);
    }

    @Test
    void getSavingGoalById_notFound_shouldThrow() {
        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> savingGoalService.getSavingGoalById(goalId));
    }

    @Test
    void createSavingGoal_success() {
        CreateSavingGoalRequest req = new CreateSavingGoalRequest("Car", BigDecimal.valueOf(2000), null);
        when(savingGoalMapper.toDto(any()))
                .thenReturn(new SavingGoalDto());

        SavingGoalDto dto = savingGoalService.createSavingGoal(req);

        assertNotNull(dto);
        verify(savingGoalRepository).save(any(SavingGoal.class));
    }

    @Test
    void updateSavingGoal_success() {
        UpdateSavingGoalRequest req = new UpdateSavingGoalRequest("New", BigDecimal.valueOf(1000), null);

        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.of(savingGoal));
        when(savingGoalMapper.toDto(savingGoal))
                .thenReturn(new SavingGoalDto());

        SavingGoalDto dto = savingGoalService.updateSavingGoal(goalId, req);

        assertNotNull(dto);
        verify(savingGoalRepository).save(savingGoal);
    }

    @Test
    void updateSavingGoal_notFound_shouldThrow() {
        UpdateSavingGoalRequest req = new UpdateSavingGoalRequest("X", BigDecimal.TEN, null);

        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> savingGoalService.updateSavingGoal(goalId, req));
    }

    @Test
    void updateSavingGoal_targetLessThanCurrent_shouldThrow() {
        savingGoal.setCurrentAmount(BigDecimal.TEN);
        UpdateSavingGoalRequest req = new UpdateSavingGoalRequest("X", BigDecimal.ONE, null);

        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.of(savingGoal));

        assertThrows(SavingGoalValidationException.class,
                () -> savingGoalService.updateSavingGoal(goalId, req));
    }

    @Test
    void deleteSavingGoal_success() {
        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.of(savingGoal));

        savingGoalService.deleteSavingGoal(goalId);

        verify(savingGoalRepository).delete(savingGoal);
    }

    @Test
    void deleteSavingGoal_notFound_shouldThrow() {
        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> savingGoalService.deleteSavingGoal(goalId));
    }

    @Test
    void addContribution_success_reachesTarget_setsAchievedTrue() {
        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.of(savingGoal));
        when(savingGoalMapper.toDto(any()))
                .thenReturn(new SavingGoalDto());

        GoalContributionRequest req = new GoalContributionRequest(BigDecimal.valueOf(1000));
        SavingGoalDto dto = savingGoalService.addContribution(goalId, req);

        assertNotNull(dto);
        assertTrue(savingGoal.isAchieved());
    }

    @Test
    void addContribution_notFound_shouldThrow() {
        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.empty());

        GoalContributionRequest req = new GoalContributionRequest(BigDecimal.ONE);

        assertThrows(EntityNotFoundException.class,
                () -> savingGoalService.addContribution(goalId, req));
    }

    @Test
    void withdrawContribution_success() {
        savingGoal.setCurrentAmount(BigDecimal.TEN);

        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.of(savingGoal));
        when(savingGoalMapper.toDto(any()))
                .thenReturn(new SavingGoalDto());

        GoalContributionRequest req = new GoalContributionRequest(BigDecimal.ONE);
        SavingGoalDto dto = savingGoalService.withdrawContribution(goalId, req);

        assertNotNull(dto);
        assertEquals(BigDecimal.valueOf(9), savingGoal.getCurrentAmount());
    }

    @Test
    void withdrawContribution_notEnough_shouldThrow() {
        savingGoal.setCurrentAmount(BigDecimal.ONE);

        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.of(savingGoal));

        GoalContributionRequest req = new GoalContributionRequest(BigDecimal.TEN);

        assertThrows(SavingGoalValidationException.class,
                () -> savingGoalService.withdrawContribution(goalId, req));
    }

    @Test
    void withdrawContribution_notFound_shouldThrow() {
        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.empty());

        GoalContributionRequest req = new GoalContributionRequest(BigDecimal.ONE);

        assertThrows(EntityNotFoundException.class,
                () -> savingGoalService.withdrawContribution(goalId, req));
    }

    @Test
    void getContributions_success() {
        GoalContribution contrib = GoalContribution.builder()
                .savingGoal(savingGoal)
                .amount(BigDecimal.ONE)
                .type(GoalContributionType.DEPOSIT)
                .build();

        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.of(savingGoal));
        when(goalContributionRepository.findBySavingGoalOrderByCreatedAtDesc(savingGoal))
                .thenReturn(List.of(contrib));
        when(goalContributionMapper.toDto(any()))
                .thenReturn(new GoalContributionDto());

        List<GoalContributionDto> result = savingGoalService.getContributions(goalId);

        assertEquals(1, result.size());
    }

    @Test
    void getContributions_notFound_shouldThrow() {
        when(savingGoalRepository.findByIdAndUser(goalId, user))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> savingGoalService.getContributions(goalId));
    }
}
