package ma.chaghaf.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.admin.dto.AdminDtos.*;
import ma.chaghaf.catalog.entity.SnackOrder;
import ma.chaghaf.catalog.repository.SnackOrderRepository;
import ma.chaghaf.config.SseEmitterManager;
import ma.chaghaf.notification.dto.NotificationDtos.SendNotificationRequest;
import ma.chaghaf.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService service;
    private final SseEmitterManager sse;
    private final SnackOrderRepository snackOrderRepo;
    private final NotificationService notifications;
    private final ObjectMapper json = new ObjectMapper();

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream() { return sse.subscribe(); }

    @GetMapping("/sse-status")
    public ResponseEntity<Map<String, Integer>> sseStatus() {
        return ResponseEntity.ok(Map.of("connected", sse.getConnectedCount()));
    }

    @GetMapping("/occupation")
    public ResponseEntity<OccupationStats> occupation() {
        return ResponseEntity.ok(service.getOccupationStats());
    }

    @GetMapping("/live-stats")
    public ResponseEntity<LiveStats> liveStats() {
        return ResponseEntity.ok(service.getLiveStats());
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<Map<String, Object>>> reservations(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String salleId) {
        return ResponseEntity.ok(service.getReservations(date, status, salleId));
    }

    @PostMapping("/qr/validate")
    public ResponseEntity<QrValidationResult> validateQr(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        QrValidationResult result = service.validateQr(token);
        sse.broadcast("qr-scan", result);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/message/send")
    public ResponseEntity<Map<String, String>> sendMessage(
            HttpServletRequest request,
            @Valid @RequestBody SendMessageRequest req) {
        Long adminId = (Long) request.getAttribute("X-User-Id");
        service.sendDirectMessage(adminId, req);
        return ResponseEntity.ok(Map.of("message",
            "Message envoyé à l'utilisateur " + req.getTargetUserId()));
    }

    @PostMapping("/social/post")
    public ResponseEntity<?> createPost(
            HttpServletRequest request,
            @Valid @RequestBody CreateAdminPostRequest req) {
        Long adminId = (Long) request.getAttribute("X-User-Id");
        return ResponseEntity.ok(service.createAdminPost(adminId, req));
    }

    @GetMapping("/clients")
    public ResponseEntity<List<Map<String, Object>>> clients() {
        return ResponseEntity.ok(service.getAllClients());
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<ClientDetail> clientDetail(@PathVariable Long id) {
        return ResponseEntity.ok(service.getClientDetail(id));
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<Map<String, String>> updateClient(
            @PathVariable Long id, @RequestBody ClientUpdateRequest req) {
        service.updateClient(id, req);
        return ResponseEntity.ok(Map.of("message", "Client mis à jour"));
    }

    @PostMapping("/clients/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        String pwd = body.get("newPassword");
        if (pwd == null || pwd.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe trop court"));
        }
        service.updateClient(id, new ClientUpdateRequest(null, null, null, pwd));
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé"));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcast(
            HttpServletRequest request, @RequestBody SendMessageRequest req) {
        Long adminId = (Long) request.getAttribute("X-User-Id");
        service.getAllClients().forEach(c -> {
            try {
                Long uid = ((Number) c.get("id")).longValue();
                if (!uid.equals(adminId)) {
                    service.sendDirectMessage(adminId, new SendMessageRequest(
                        uid, req.getTitle(), req.getBody(), req.getType()));
                }
            } catch (Exception ignored) {}
        });
        return ResponseEntity.ok(Map.of("message", "Broadcast envoyé"));
    }

    // ── Snack orders (vue ERP) ────────────────────────────────────
    @GetMapping("/snack-orders")
    public ResponseEntity<List<Map<String, Object>>> snackOrders(
            @RequestParam(required = false) String status) {
        List<SnackOrder> orders = (status == null || status.isBlank())
            ? snackOrderRepo.findAllByOrderByCreatedAtDesc()
            : snackOrderRepo.findByStatusOrderByCreatedAtDesc(status);
        List<Map<String, Object>> out = orders.stream().map(this::snackOrderToDto).toList();
        return ResponseEntity.ok(out);
    }

    @PatchMapping("/snack-orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateSnackOrderStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return snackOrderRepo.findById(id)
            .map(o -> {
                String newStatus = body.getOrDefault("status", "PENDING");
                o.setStatus(newStatus);
                o.setUpdatedAt(LocalDateTime.now());
                snackOrderRepo.save(o);
                try {
                    notifications.send(new SendNotificationRequest(
                        o.getUserId(),
                        "Mise à jour commande snacks",
                        "Votre commande #" + o.getId() + " est maintenant : " + newStatus,
                        "SNACK_ORDER",
                        "/snacks/orders/" + o.getId()));
                } catch (Exception ignored) {}
                sse.broadcast("snack-order-updated", Map.of("id", o.getId(), "status", newStatus));
                return ResponseEntity.ok(snackOrderToDto(o));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> snackOrderToDto(SnackOrder o) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", o.getId());
        m.put("userId", o.getUserId());
        try {
            m.put("items", json.readValue(
                o.getItemsJson() == null ? "[]" : o.getItemsJson(),
                new TypeReference<List<Map<String, Object>>>(){}));
        } catch (Exception e) {
            m.put("items", List.of());
        }
        m.put("note", o.getNote() == null ? "" : o.getNote());
        m.put("totalPrice", o.getTotalPrice());
        m.put("status", o.getStatus());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        m.put("updatedAt", o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null);
        return m;
    }
}
