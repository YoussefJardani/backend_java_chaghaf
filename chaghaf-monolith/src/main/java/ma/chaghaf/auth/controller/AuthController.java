package ma.chaghaf.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.auth.dto.AuthDtos.*;
import ma.chaghaf.auth.entity.User;
import ma.chaghaf.auth.repository.UserRepository;
import ma.chaghaf.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepo;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(authService.me(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth"));
    }

    @PutMapping("/fcm-token")
    public ResponseEntity<Map<String, String>> updateFcmToken(HttpServletRequest req,
                                                                @RequestBody Map<String, String> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");
        User u = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setFcmToken(body.get("fcmToken"));
        userRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "FCM token updated"));
    }

    // ─── New: list all users (admin-style listing) ──────────────────
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listAll() {
        List<UserResponse> users = userRepo.findAll().stream()
            .map(u -> new UserResponse(
                u.getId(), u.getFullName(), u.getEmail(),
                u.getPhone(), u.getRole().name(),
                u.getAvatarLetter(), u.getActive()))
            .toList();
        return ResponseEntity.ok(users);
    }

    // ─── New: change a user's role ──────────────────────────────────
    // POST /api/auth/users/{id}/role  body: {"role":"ADMIN"} or {"role":"USER"}
    @PostMapping("/users/{id}/role")
    public ResponseEntity<Map<String, String>> changeRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String roleStr = body.get("role");
        if (roleStr == null || roleStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "role is required"));
        }
        User.Role role;
        try {
            role = User.Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid role. Use USER or ADMIN."));
        }
        User u = userRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        u.setRole(role);
        userRepo.save(u);
        return ResponseEntity.ok(Map.of(
            "message", "Role updated to " + role.name(),
            "userId", id.toString(),
            "newRole", role.name()
        ));
    }

    // ─── New: promote/demote shortcuts ──────────────────────────────
    @PostMapping("/users/{id}/promote")
    public ResponseEntity<Map<String, String>> promoteToAdmin(@PathVariable Long id) {
        User u = userRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        u.setRole(User.Role.ADMIN);
        userRepo.save(u);
        return ResponseEntity.ok(Map.of(
            "message", "User promoted to ADMIN",
            "userId", id.toString()
        ));
    }

    @PostMapping("/users/{id}/demote")
    public ResponseEntity<Map<String, String>> demoteToUser(@PathVariable Long id) {
        User u = userRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        u.setRole(User.Role.USER);
        userRepo.save(u);
        return ResponseEntity.ok(Map.of(
            "message", "User demoted to USER",
            "userId", id.toString()
        ));
    }
}