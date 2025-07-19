package mk.ukim.finki.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {
    private JwtTokenProvider jwtTokenProvider;

    private final String secret = "MySuperSecretKeyForJwtThatIsLongEnough123!";
    private final long expirationMs = 86400000;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    void testGenerateAndValidateToken() {
        String email = "test@example.com";

        String token = jwtTokenProvider.generateToken(email);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(email, jwtTokenProvider.getEmailFromJWT(token));
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.token.string";

        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }
}
