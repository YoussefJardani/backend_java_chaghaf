package ma.chaghaf.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.auth.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            // SSE / EventSource fallback: ?token=
            String paramToken = request.getParameter("token");
            if (paramToken != null && !paramToken.isBlank()) token = paramToken;
        }

        if (token != null) {
            try {
                Claims claims = jwtService.parse(token);
                Long userId = claims.get("userId", Long.class);
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);

                request.setAttribute("X-User-Id", userId);
                request.setAttribute("X-User-Email", email);
                request.setAttribute("X-User-Role", role);

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // Invalid token → just don't set auth; permitAll routes still work
            }
        }
        chain.doFilter(request, response);
    }
}
