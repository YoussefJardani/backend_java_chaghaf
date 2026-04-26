package ma.chaghaf.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.admin.dto.AdminDtos.*;
import ma.chaghaf.config.SseEmitterManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService service;
    private final SseEmitterManager sse;

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
}
