package ma.chaghaf.subscription.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.subscription.dto.SubscriptionDtos.*;
import ma.chaghaf.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    @GetMapping("/active")
    public ResponseEntity<SubscriptionResponse> active(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        SubscriptionResponse sub = service.getActiveForUser(userId);
        return sub != null ? ResponseEntity.ok(sub) : ResponseEntity.noContent().build();
    }

    @GetMapping("/packs")
    public ResponseEntity<List<PackInfo>> packs() {
        return ResponseEntity.ok(service.listPacks());
    }
}
