package mk.ukim.finki.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.auth.AuthResponseDto;
import mk.ukim.finki.backend.model.dto.auth.UserInfoDto;
import mk.ukim.finki.backend.model.dto.auth.UserLoginDto;
import mk.ukim.finki.backend.model.dto.auth.UserRegistrationDto;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.security.JwtTokenProvider;
import mk.ukim.finki.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user authentication.
 * <p>
 * Provides endpoints for user registration and login.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registers a new user.
     *
     * @param dto the user registration data transfer object
     * @return 200 OK with confirmation message
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDto dto) {
        User createdUser = userService.registerUser(dto);
        return ResponseEntity.ok("User registered with email: " + createdUser.getEmail());
    }

    /**
     * Authenticates a user and issues a JWT token.
     *
     * @param dto the login credentials
     * @return 200 OK with AuthResponseDto containing token and expiry on success;
     * 401 Unauthorized with message on failure
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginDto dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );
            String token = jwtTokenProvider.generateToken(dto.getEmail());
            User user = userService.findByEmail(dto.getEmail());

            return ResponseEntity.ok(new AuthResponseDto(
                    token,
                    jwtTokenProvider.getJwtExpirationInMs(),
                    user.getEmail(),
                    user.getRole().name()
            ));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    /**
     * Returns the currently authenticated user's information.
     *
     * @return 200 OK with user email and role
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(new UserInfoDto(user.getEmail(), user.getRole().name()));
    }
}
