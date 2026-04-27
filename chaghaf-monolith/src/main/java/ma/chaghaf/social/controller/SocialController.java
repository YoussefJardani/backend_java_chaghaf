package ma.chaghaf.social.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.auth.entity.User;
import ma.chaghaf.auth.repository.UserRepository;
import ma.chaghaf.social.dto.SocialDtos.*;
import ma.chaghaf.social.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService service;
    private final UserRepository userRepo;

    @GetMapping
    public ResponseEntity<List<PostResponse>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(HttpServletRequest req,
                                                @Valid @RequestBody CreatePostRequest body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");
        User u = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(service.createPost(
            userId, u.getFullName(), u.getAvatarLetter(), u.getRole().name(), body));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleLike(id));
    }
}
