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
        User u = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(service.createPost(
            userId, u.getFullName(), u.getAvatarLetter(), u.getRole().name(), body));
    }
}
