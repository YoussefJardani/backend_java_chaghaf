package ma.chaghaf.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Refuse de démarrer si JWT_SECRET n'est pas fourni via env var.
     * Empêche d'utiliser un secret par défaut qui serait connu de tous.
     */
    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isBlank()) {
            log.error("FATAL: JWT_SECRET environment variable is missing.");
            throw new IllegalStateException(
                "JWT_SECRET must be defined as an environment variable. " +
                "Set it on Azure Container Apps via: " +
                "az containerapp update --name app-chaghaf --resource-group rg-chaghaf " +
                "--set-env-vars JWT_SECRET=<une-clé-de-256-bits-min>");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            log.error("FATAL: JWT_SECRET is too short ({}B). Must be at least 32 bytes (256 bits).",
                secret.getBytes(StandardCharsets.UTF_8).length);
            throw new IllegalStateException("JWT_SECRET too short — need at least 32 bytes (256 bits)");
        }
        log.info("JWT_SECRET validated ({}B)", secret.getBytes(StandardCharsets.UTF_8).length);
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);
        return Jwts.builder()
            .claims(claims)
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key())
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
