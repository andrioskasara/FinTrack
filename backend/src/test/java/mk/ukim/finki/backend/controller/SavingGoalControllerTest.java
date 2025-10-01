package mk.ukim.finki.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.config.SecurityConfig;
import mk.ukim.finki.backend.exception.SavingGoalValidationException;
import mk.ukim.finki.backend.model.dto.saving_goal.*;
import mk.ukim.finki.backend.model.enums.GoalContributionType;
import mk.ukim.finki.backend.security.JwtAuthenticationFilter;
import mk.ukim.finki.backend.service.SavingGoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SavingGoalController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JwtAuthenticationFilter.class)
        })
@AutoConfigureMockMvc(addFilters = false)
public class SavingGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SavingGoalService savingGoalService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID goalId;
    private SavingGoalDto savingGoalDto;
    private CreateSavingGoalRequest createRequest;
    private UpdateSavingGoalRequest updateRequest;
    private GoalContributionRequest contributionRequest;

    @BeforeEach
    void setUp() {
        goalId = UUID.randomUUID();

        savingGoalDto = SavingGoalDto.builder()
                .id(goalId)
                .name("Vacation")
                .targetAmount(BigDecimal.valueOf(1000))
                .currentAmount(BigDecimal.valueOf(200))
                .achieved(false)
                .deadline(LocalDate.now().plusMonths(6))
                .build();

        createRequest = CreateSavingGoalRequest.builder()
                .name("Vacation")
                .targetAmount(BigDecimal.valueOf(1000))
                .deadline(LocalDate.now().plusMonths(6))
                .build();

        updateRequest = UpdateSavingGoalRequest.builder()
                .name("Updated Vacation")
                .targetAmount(BigDecimal.valueOf(1200))
                .deadline(LocalDate.now().plusMonths(8))
                .build();

        contributionRequest = GoalContributionRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .build();
    }

    private String asJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    void getAllSavingGoals_success() throws Exception {
        when(savingGoalService.getAllSavingGoals())
                .thenReturn(List.of(savingGoalDto));

        mockMvc.perform(get("/api/saving-goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(goalId.toString()))
                .andExpect(jsonPath("$[0].name").value("Vacation"));
    }

    @Test
    void getSavingGoalById_success() throws Exception {
        when(savingGoalService.getSavingGoalById(goalId))
                .thenReturn(savingGoalDto);

        mockMvc.perform(get("/api/saving-goals/{id}", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId.toString()))
                .andExpect(jsonPath("$.targetAmount").value(1000));
    }

    @Test
    void getSavingGoalById_notFound() throws Exception {
        when(savingGoalService.getSavingGoalById(goalId))
                .thenThrow(new EntityNotFoundException("Goal not found"));

        mockMvc.perform(get("/api/saving-goals/{id}", goalId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Goal not found"));
    }

    @Test
    void createSavingGoal_success() throws Exception {
        when(savingGoalService.createSavingGoal(any(CreateSavingGoalRequest.class)))
                .thenReturn(savingGoalDto);

        mockMvc.perform(post("/api/saving-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(goalId.toString()))
                .andExpect(jsonPath("$.name").value("Vacation"));
    }

    @Test
    void createSavingGoal_invalid_badRequest() throws Exception {
        CreateSavingGoalRequest invalid = new CreateSavingGoalRequest();
        invalid.setTargetAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/saving-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSavingGoal_success() throws Exception {
        when(savingGoalService.updateSavingGoal(eq(goalId), any(UpdateSavingGoalRequest.class)))
                .thenReturn(savingGoalDto);

        mockMvc.perform(put("/api/saving-goals/{id}", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId.toString()));
    }

    @Test
    void updateSavingGoal_notFound() throws Exception {
        when(savingGoalService.updateSavingGoal(eq(goalId), any(UpdateSavingGoalRequest.class)))
                .thenThrow(new EntityNotFoundException("Goal not found"));

        mockMvc.perform(put("/api/saving-goals/{id}", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Goal not found"));
    }

    @Test
    void deleteSavingGoal_success() throws Exception {
        doNothing().when(savingGoalService).deleteSavingGoal(goalId);

        mockMvc.perform(delete("/api/saving-goals/{id}", goalId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSavingGoal_notFound() throws Exception {
        doThrow(new EntityNotFoundException("Goal not found"))
                .when(savingGoalService).deleteSavingGoal(goalId);

        mockMvc.perform(delete("/api/saving-goals/{id}", goalId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Goal not found"));
    }

    @Test
    void addContribution_success() throws Exception {
        when(savingGoalService.addContribution(eq(goalId), any(GoalContributionRequest.class)))
                .thenReturn(savingGoalDto);

        mockMvc.perform(post("/api/saving-goals/{id}/contribute", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(contributionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId.toString()))
                .andExpect(jsonPath("$.currentAmount").value(200));
    }

    @Test
    void addContribution_invalid_badRequest() throws Exception {
        GoalContributionRequest invalid = new GoalContributionRequest();
        invalid.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/saving-goals/{id}/contribute", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawContribution_success() throws Exception {
        when(savingGoalService.withdrawContribution(eq(goalId), any(GoalContributionRequest.class)))
                .thenReturn(savingGoalDto);

        mockMvc.perform(post("/api/saving-goals/{id}/withdraw", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(contributionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId.toString()));
    }

    @Test
    void withdrawContribution_notEnoughFunds_badRequest() throws Exception {
        when(savingGoalService.withdrawContribution(eq(goalId), any(GoalContributionRequest.class)))
                .thenThrow(new SavingGoalValidationException("Not enough amount to withdraw"));

        mockMvc.perform(post("/api/saving-goals/{id}/withdraw", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(contributionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Not enough amount to withdraw"));
    }

    @Test
    void getContributions_success() throws Exception {
        GoalContributionDto dto = GoalContributionDto.builder()
                .id(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .type(GoalContributionType.DEPOSIT)
                .build();

        when(savingGoalService.getContributions(goalId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/saving-goals/{id}/contributions", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(100))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"));
    }

    @Test
    void getContributions_goalNotFound() throws Exception {
        when(savingGoalService.getContributions(goalId))
                .thenThrow(new EntityNotFoundException("Goal not found"));

        mockMvc.perform(get("/api/saving-goals/{id}/contributions", goalId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Goal not found"));
    }
}
