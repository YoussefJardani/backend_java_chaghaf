package ma.chaghaf.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les tentatives de connexion par email.
 * - 5 échecs en 15 min → blocage 15 min
 * - Success → reset
 * - Mémoire seule (suffisant pour 1 instance), pas de Redis
 */
@Slf4j
@Service
public class LoginRateLimiter {

    private static final int  MAX_ATTEMPTS = 5;
    private static final Duration WINDOW   = Duration.ofMinutes(15);

    private static class Attempt {
        int count;
        Instant firstAttempt;
        Instant blockedUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * Lance une exception si la clé (email) est actuellement bloquée.
     */
    public void check(String key) {
        if (key == null || key.isBlank()) return;
        Attempt a = attempts.get(key.toLowerCase());
        if (a == null) return;

        Instant now = Instant.now();
        if (a.blockedUntil != null && now.isBefore(a.blockedUntil)) {
            long secondsLeft = Duration.between(now, a.blockedUntil).getSeconds();
            throw new IllegalArgumentException(
                "Trop de tentatives. Réessayez dans " + (secondsLeft / 60 + 1) + " minute(s).");
        }
    }

    /**
     * À appeler quand le login échoue (mauvais mot de passe, etc.).
     */
    public void recordFailure(String key) {
        if (key == null || key.isBlank()) return;
        String k = key.toLowerCase();
        Instant now = Instant.now();

        attempts.compute(k, (kk, a) -> {
            if (a == null || a.firstAttempt == null
                || Duration.between(a.firstAttempt, now).compareTo(WINDOW) > 0) {
                Attempt fresh = new Attempt();
                fresh.count = 1;
                fresh.firstAttempt = now;
                return fresh;
            }
            a.count++;
            if (a.count >= MAX_ATTEMPTS) {
                a.blockedUntil = now.plus(WINDOW);
                log.warn("Login locked for {} until {} ({} failures)", k, a.blockedUntil, a.count);
            }
            return a;
        });
    }

    /**
     * À appeler quand un login réussit → reset du compteur.
     */
    public void recordSuccess(String key) {
        if (key == null || key.isBlank()) return;
        attempts.remove(key.toLowerCase());
    }
}
