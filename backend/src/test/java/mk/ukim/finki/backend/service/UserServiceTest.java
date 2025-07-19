package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.exception.EmailAlreadyExistsException;
import mk.ukim.finki.backend.model.dto.UserRegistrationDto;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.model.enums.UserRole;
import mk.ukim.finki.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    public UserServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUserSuccess() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("test@example.com");
        dto.setPassword("Password1!");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .email(dto.getEmail())
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(dto);

        assertEquals(dto.getEmail(), result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(UserRole.USER, result.getRole());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUserEmailAlreadyExists() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("existing@example.com");
        dto.setPassword("Password1!");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any(User.class));
    }
}
