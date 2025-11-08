package mk.ukim.finki.backend.model.dto.auth;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO representing an authentication response containing JWT token and expiry.
 */
@Data
@AllArgsConstructor
public class AuthResponseDto {

    /**
     * JWT token issued to the user.
     */
    private String token;

    /**
     * Expiration time of the token in milliseconds.
     */
    private long expiresIn;

    /**
     * Email of the authenticated user.
     */
    private String email;

    /**
     * Role of the authenticated user.
     */
    private String role;
}
