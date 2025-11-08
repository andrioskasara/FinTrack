package mk.ukim.finki.backend.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfoDto {
    private String email;
    private String role;
}
