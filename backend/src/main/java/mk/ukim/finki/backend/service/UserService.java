package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.model.dto.auth.UserRegistrationDto;
import mk.ukim.finki.backend.model.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service interface for user management and authentication.
 */
public interface UserService extends UserDetailsService {

    /**
     * Registers a new user from registration DTO.
     *
     * @param registrationDto user registration details
     * @return persisted User entity
     */
    User registerUser(UserRegistrationDto registrationDto);
}
