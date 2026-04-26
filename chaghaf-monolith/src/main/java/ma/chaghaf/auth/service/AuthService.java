package ma.chaghaf.auth.service;

import lombok.RequiredArgsConstructor;
import ma.chaghaf.auth.dto.AuthDtos.*;
import ma.chaghaf.auth.entity.User;
import ma.chaghaf.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        User u = User.builder()
            .fullName(req.fullName())
            .email(req.email())
            .password(passwordEncoder.encode(req.password()))
            .phone(req.phone())
            .role(User.Role.USER)
            .active(true)
            .build();
        u = userRepo.save(u);
        String token = jwtService.generateToken(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token, u.getId(), u.getFullName(), u.getEmail(), u.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepo.findByEmail(req.email())
            .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect"));
        if (!passwordEncoder.matches(req.password(), u.getPassword())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }
        if (!Boolean.TRUE.equals(u.getActive())) {
            throw new IllegalArgumentException("Compte désactivé");
        }
        String token = jwtService.generateToken(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token, u.getId(), u.getFullName(), u.getEmail(), u.getRole().name());
    }

    public UserResponse me(Long userId) {
        User u = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(),
            u.getPhone(), u.getRole().name(), u.getAvatarLetter(), u.getActive());
    }
}
