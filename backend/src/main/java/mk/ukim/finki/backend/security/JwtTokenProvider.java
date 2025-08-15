package mk.ukim.finki.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Provider for creating, parsing and validating JWT tokens.
 */
@Component
@Slf4j
public class JwtTokenProvider {
    private final Key key;
    private final long jwtExpirationInMs;

    /**
     * Constructs token provider with secret and expiration time.
     */
    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.jwtExpirationInMs = expirationMs;
    }

    /**
     * Generates a signed JWT token containing the subject (email).
     *
     * @param email user email to include in token subject
     * @return signed JWT token string
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email (subject) from a JWT token.
     *
     * @param token JWT token string
     * @return email subject from token
     */
    public String getEmailFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Validates a JWT token’s signature and expiration.
     *
     * @param authToken JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (JwtException ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Returns token expiration duration in milliseconds.
     *
     * @return expiration time in ms
     */
    public long getJwtExpirationInMs() {
        return jwtExpirationInMs;
    }
}
