package ma.chaghaf.subscription.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.notification.entity.Notification;
import ma.chaghaf.notification.repository.NotificationRepository;
import ma.chaghaf.subscription.dto.SubscriptionDtos.*;
import ma.chaghaf.subscription.entity.Subscription;
import ma.chaghaf.subscription.entity.SubscriptionChangeRequest;
import ma.chaghaf.subscription.repository.SubscriptionChangeRequestRepository;
import ma.chaghaf.subscription.repository.SubscriptionRepository;
import ma.chaghaf.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;
    private final SubscriptionChangeRequestRepository changeRepo;
    private final SubscriptionRepository subRepo;
    private final NotificationRepository notifRepo;

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

    @GetMapping("/history")
    public ResponseEntity<List<SubscriptionResponse>> history(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(service.getHistory(userId));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(HttpServletRequest req,
                                                           @RequestBody Map<String, Object> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(service.subscribe(userId, body));
    }

    @PostMapping("/renew")
    public ResponseEntity<SubscriptionResponse> renew(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(service.renew(userId));
    }

    @PostMapping("/day-access")
    public ResponseEntity<Map<String, Object>> dayAccess(HttpServletRequest req,
                                                          @RequestBody Map<String, Object> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(service.purchaseDayAccess(userId, body));
    }

    // ───── Demande de changement d'abonnement (validation admin) ─────
    @PostMapping("/change-request")
    @Transactional
    public ResponseEntity<Map<String, Object>> requestChange(HttpServletRequest req,
                                                              @RequestBody Map<String, Object> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        String requestedPack = (String) body.get("requestedPack");
        String requestedDuration = (String) body.get("requestedDuration");
        String reason = (String) body.get("reason");

        if (requestedPack == null || requestedDuration == null) {
            throw new IllegalArgumentException("requestedPack et requestedDuration sont requis");
        }

        String currentPack = subRepo.findByUserIdAndStatus(userId, Subscription.Status.ACTIVE)
            .map(s -> s.getPackType().name())
            .orElse(null);

        SubscriptionChangeRequest cr = SubscriptionChangeRequest.builder()
            .userId(userId)
            .currentPack(currentPack)
            .requestedPack(requestedPack)
            .requestedDuration(requestedDuration)
            .reason(reason)
            .status(SubscriptionChangeRequest.Status.PENDING)
            .build();
        cr = changeRepo.save(cr);

        notifRepo.save(Notification.builder()
            .userId(userId)
            .title("Demande de changement envoyée")
            .body("Votre demande pour le pack " + requestedPack + " est en attente de validation par l'admin.")
            .type("SUBSCRIPTION_REQUEST")
            .read(false)
            .build());

        Map<String, Object> response = new HashMap<>();
        response.put("id", cr.getId());
        response.put("status", cr.getStatus().name());
        response.put("message", "Demande envoyée à l'administrateur. Vous serez notifié de la décision.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/change-requests/me")
    public ResponseEntity<List<SubscriptionChangeRequest>> myChangeRequests(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(changeRepo.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/change-requests/pending")
    public ResponseEntity<List<SubscriptionChangeRequest>> pendingRequests() {
        return ResponseEntity.ok(
            changeRepo.findByStatusOrderByCreatedAtDesc(SubscriptionChangeRequest.Status.PENDING));
    }

    @PostMapping("/change-requests/{id}/approve")
    @Transactional
    public ResponseEntity<Map<String, Object>> approveRequest(HttpServletRequest req,
                                                                @PathVariable Long id,
                                                                @RequestBody(required = false) Map<String, Object> body) {
        Long adminId = (Long) req.getAttribute("X-User-Id");
        SubscriptionChangeRequest cr = changeRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        cr.setStatus(SubscriptionChangeRequest.Status.APPROVED);
        cr.setAdminId(adminId);
        cr.setAdminNote(body != null ? (String) body.get("note") : null);
        cr.setProcessedAt(LocalDateTime.now());
        changeRepo.save(cr);

        subRepo.findByUserIdAndStatus(cr.getUserId(), Subscription.Status.ACTIVE).ifPresent(old -> {
            old.setStatus(Subscription.Status.EXPIRED);
            subRepo.save(old);
        });
        Map<String, Object> subBody = new HashMap<>();
        subBody.put("packType", cr.getRequestedPack());
        subBody.put("duration", cr.getRequestedDuration());
        SubscriptionResponse newSub = service.subscribe(cr.getUserId(), subBody);

        notifRepo.save(Notification.builder()
            .userId(cr.getUserId())
            .title("Changement d'abonnement approuvé")
            .body("Votre nouveau pack " + cr.getRequestedPack() + " est actif !")
            .type("SUBSCRIPTION_REQUEST")
            .read(false)
            .build());

        return ResponseEntity.ok(Map.of(
            "approved", true,
            "newSubscription", newSub
        ));
    }

    @PostMapping("/change-requests/{id}/reject")
    @Transactional
    public ResponseEntity<Map<String, String>> rejectRequest(HttpServletRequest req,
                                                              @PathVariable Long id,
                                                              @RequestBody(required = false) Map<String, Object> body) {
        Long adminId = (Long) req.getAttribute("X-User-Id");
        SubscriptionChangeRequest cr = changeRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        cr.setStatus(SubscriptionChangeRequest.Status.REJECTED);
        cr.setAdminId(adminId);
        cr.setAdminNote(body != null ? (String) body.get("note") : null);
        cr.setProcessedAt(LocalDateTime.now());
        changeRepo.save(cr);

        notifRepo.save(Notification.builder()
            .userId(cr.getUserId())
            .title("Demande refusée")
            .body("Votre demande de changement a été refusée. " +
                  (cr.getAdminNote() != null ? "Motif: " + cr.getAdminNote() : ""))
            .type("SUBSCRIPTION_REQUEST")
            .read(false)
            .build());

        return ResponseEntity.ok(Map.of("rejected", "true"));
    }
}
