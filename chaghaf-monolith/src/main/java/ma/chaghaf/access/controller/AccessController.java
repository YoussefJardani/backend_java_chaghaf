package ma.chaghaf.access.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.access.entity.CheckIn;
import ma.chaghaf.access.repository.CheckInRepository;
import ma.chaghaf.auth.entity.User;
import ma.chaghaf.auth.repository.UserRepository;
import ma.chaghaf.notification.entity.Notification;
import ma.chaghaf.notification.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class AccessController {

    private final CheckInRepository checkInRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notifRepo;

    private static final int MAX_CAPACITY = 30;
    private static final int NOTIFY_THRESHOLD = 3;  // dès la 3ème personne, on envoie une notif aux nouveaux
    private static final String QR_TOKEN = "CHAGHAF-LOCAL-CHECKIN";

    @GetMapping("/qr-token")
    public ResponseEntity<Map<String, String>> qrToken() {
        return ResponseEntity.ok(Map.of(
            "token", QR_TOKEN,
            "label", "Chaghaf · Check-in"
        ));
    }

    @GetMapping("/occupancy")
    public ResponseEntity<Map<String, Object>> occupancy() {
        long current = checkInRepo.countByActiveTrue();
        int percent = MAX_CAPACITY > 0 ? (int) Math.round((current * 100.0) / MAX_CAPACITY) : 0;
        Map<String, Object> r = new HashMap<>();
        r.put("currentCount", current);
        r.put("maxCapacity", MAX_CAPACITY);
        r.put("percent", percent);
        r.put("status", percent < 50 ? "Calme" : percent < 80 ? "Modéré" : "Plein");
        r.put("activeUsers", checkInRepo.findByActiveTrueOrderByCheckedInAtDesc().stream()
            .map(c -> Map.of(
                "userId", c.getUserId(),
                "userName", c.getUserName(),
                "since", c.getCheckedInAt().toString()))
            .toList());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/check-in")
    @Transactional
    public ResponseEntity<Map<String, Object>> checkIn(HttpServletRequest req,
                                                        @RequestBody Map<String, String> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        String token = body.get("qrToken");
        if (token == null || !token.equals(QR_TOKEN)) {
            throw new IllegalArgumentException("QR Code invalide");
        }

        // Empêcher double check-in
        if (checkInRepo.findFirstByUserIdAndActiveTrueOrderByCheckedInAtDesc(userId).isPresent()) {
            throw new IllegalArgumentException("Vous êtes déjà à l'intérieur de l'espace");
        }

        User u = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        long currentBefore = checkInRepo.countByActiveTrue();
        if (currentBefore >= MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacité maximale atteinte (" + MAX_CAPACITY + ")");
        }

        CheckIn ci = CheckIn.builder()
            .userId(userId)
            .userName(u.getFullName())
            .active(true)
            .build();
        ci = checkInRepo.save(ci);

        long currentAfter = currentBefore + 1;

        // Notification de bienvenue si seuil atteint
        if (currentAfter >= NOTIFY_THRESHOLD) {
            notifRepo.save(Notification.builder()
                .userId(userId)
                .title("Bienvenue à Chaghaf")
                .body("Il y a " + currentAfter + " personnes dans l'espace. Combien de temps comptez-vous rester ?")
                .type("CHECK_IN")
                .read(false)
                .build());
        }

        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("checkInId", ci.getId());
        r.put("userName", u.getFullName());
        r.put("checkedInAt", ci.getCheckedInAt().toString());
        r.put("currentOccupancy", currentAfter);
        r.put("maxCapacity", MAX_CAPACITY);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/check-out")
    @Transactional
    public ResponseEntity<Map<String, Object>> checkOut(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        CheckIn ci = checkInRepo.findFirstByUserIdAndActiveTrueOrderByCheckedInAtDesc(userId)
            .orElseThrow(() -> new IllegalArgumentException("Aucun check-in actif"));

        ci.setActive(false);
        ci.setCheckedOutAt(LocalDateTime.now());
        checkInRepo.save(ci);

        long current = checkInRepo.countByActiveTrue();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "checkedOutAt", ci.getCheckedOutAt().toString(),
            "currentOccupancy", current,
            "maxCapacity", MAX_CAPACITY
        ));
    }

    @GetMapping("/my-status")
    public ResponseEntity<Map<String, Object>> myStatus(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        return checkInRepo.findFirstByUserIdAndActiveTrueOrderByCheckedInAtDesc(userId)
            .map(ci -> {
                Map<String, Object> r = new HashMap<>();
                r.put("inside", true);
                r.put("checkedInAt", ci.getCheckedInAt().toString());
                r.put("durationMinutes", java.time.Duration.between(ci.getCheckedInAt(), LocalDateTime.now()).toMinutes());
                return ResponseEntity.ok(r);
            })
            .orElseGet(() -> ResponseEntity.ok(Map.of("inside", false)));
    }
}
