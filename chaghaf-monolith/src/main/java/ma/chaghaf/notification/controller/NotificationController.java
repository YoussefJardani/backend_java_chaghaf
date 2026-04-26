package ma.chaghaf.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.notification.dto.NotificationDtos.NotificationResponse;
import ma.chaghaf.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
