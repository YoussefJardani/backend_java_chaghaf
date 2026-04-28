package ma.chaghaf.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.notification.dto.NotificationDtos.NotificationResponse;
import ma.chaghaf.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> myNotifications(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(service.findByUser(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(Map.of("count", service.countUnread(userId)));
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markAllRead(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        service.markAllRead(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
