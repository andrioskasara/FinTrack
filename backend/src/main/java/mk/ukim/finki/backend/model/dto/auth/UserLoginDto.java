package mk.ukim.finki.backend.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO representing user login credentials.
 */
@Data
public class UserLoginDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
