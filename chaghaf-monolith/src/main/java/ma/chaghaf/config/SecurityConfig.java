package ma.chaghaf.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    /**
     * Liste des origines autorisées par CORS, séparées par virgules.
     * Exemples:
     *   prod   : "https://chaghaf-mobile.com,exp://192.168.1.10:19000"
     *   dev    : "*" (tout autorisé — le défaut)
     */
    @Value("${cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (origins.size() == 1 && origins.get(0).equals("*")) {
            cors.setAllowedOriginPatterns(List.of("*"));
        } else {
            cors.setAllowedOriginPatterns(origins);
        }
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        cors.setExposedHeaders(List.of("Authorization"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cors);
        return src;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> {})
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .authorizeHttpRequests(auth -> auth
                // ── Endpoints publics ────────────────────────────
                .requestMatchers(
                    "/", "/error",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/health",
                    "/api/catalog",
                    "/api/catalog/**",
                    "/api/boissons",
                    "/api/boissons/cafe-guide",
                    "/api/snacks/catalog",
                    "/api/reservations/salles",
                    "/api/subscriptions/packs",
                    "/actuator/**"
                ).permitAll()

                // ── Endpoints ADMIN uniquement ──────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/subscriptions/change-requests/pending").hasRole("ADMIN")
                .requestMatchers("/api/subscriptions/change-requests/*/approve").hasRole("ADMIN")
                .requestMatchers("/api/subscriptions/change-requests/*/reject").hasRole("ADMIN")

                // ── Tout le reste : authentifié ─────────────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
