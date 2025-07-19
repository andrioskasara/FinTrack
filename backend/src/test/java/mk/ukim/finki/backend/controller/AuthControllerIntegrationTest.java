package mk.ukim.finki.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mk.ukim.finki.backend.model.dto.UserLoginDto;
import mk.ukim.finki.backend.model.dto.UserRegistrationDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final String email = "testuser@example.com";
    private final String password = "Password1!";

    @BeforeAll
    void setupUser() throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail(email);
        registrationDto.setPassword(password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isOk());
    }

    @Test
    void testRegisterNewUser() throws Exception {
        String newEmail = "newuser@example.com";
        String newPassword = "NewPass1!";

        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail(newEmail);
        registrationDto.setPassword(newPassword);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User registered with email")));
    }

    @Test
    void testLoginSuccess() throws Exception {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail(email);
        loginDto.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(86400000));
    }

    @Test
    void testLoginFailure() throws Exception {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail(email);
        loginDto.setPassword("WrongPassword1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }
}
