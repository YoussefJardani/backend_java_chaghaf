package ma.chaghaf.social.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> sendMessage(HttpServletRequest req,
                                                            @RequestBody Map<String, Object> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");
        return ResponseEntity.ok(Map.of(
            "id", System.currentTimeMillis(),
            "senderId", userId,
            "recipientId", body.getOrDefault("recipientId", 0),
            "content", body.getOrDefault("content", ""),
            "createdAt", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/{otherId}")
    public ResponseEntity<List<Object>> conversation(@PathVariable Long otherId) {
        return ResponseEntity.ok(List.of());
    }
}
