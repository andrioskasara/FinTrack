package mk.ukim.finki.backend.service;

import mk.ukim.finki.backend.model.dto.auth.UserRegistrationDto;
import mk.ukim.finki.backend.model.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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

    /**
     * Gets the currently authenticated user.
     *
     * @return current User entity
     * @throws UsernameNotFoundException if not authenticated
     */
    User getCurrentUser();

    /**
     * Finds a user by email.
     *
     * @param email user's email
     * @return User entity
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if user not found
     */
    User findByEmail(String email);
}
