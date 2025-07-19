package mk.ukim.finki.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.model.dto.AuthResponseDto;
import mk.ukim.finki.backend.model.dto.UserLoginDto;
import mk.ukim.finki.backend.model.dto.UserRegistrationDto;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.security.JwtTokenProvider;
import mk.ukim.finki.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDto dto) {
        User createdUser = userService.registerUser(dto);
        return ResponseEntity.ok("User registered with email: " + createdUser.getEmail());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginDto dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );
            String token = jwtTokenProvider.generateToken(dto.getEmail());

            return ResponseEntity.ok(new AuthResponseDto(token, jwtTokenProvider.getJwtExpirationInMs()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }
    }
}
