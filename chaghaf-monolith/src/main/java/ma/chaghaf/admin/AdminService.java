package ma.chaghaf.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.admin.dto.AdminDtos.*;
import ma.chaghaf.auth.entity.User;
import ma.chaghaf.auth.repository.UserRepository;
import ma.chaghaf.config.SseEmitterManager;
import ma.chaghaf.notification.dto.NotificationDtos.SendNotificationRequest;
import ma.chaghaf.notification.service.NotificationService;
import ma.chaghaf.reservation.entity.Reservation;
import ma.chaghaf.reservation.repository.ReservationRepository;
import ma.chaghaf.social.dto.SocialDtos.CreatePostRequest;
import ma.chaghaf.social.service.SocialService;
import ma.chaghaf.subscription.entity.DayAccess;
import ma.chaghaf.subscription.entity.Subscription;
import ma.chaghaf.subscription.repository.DayAccessRepository;
import ma.chaghaf.subscription.repository.SubscriptionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepo;
    private final ReservationRepository reservationRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final DayAccessRepository dayAccessRepo;
    private final NotificationService notifService;
    private final SocialService socialService;
    private final PasswordEncoder passwordEncoder;
    private final SseEmitterManager sse;

    private static final int TOTAL_CAPACITY = 30;

    public OccupationStats getOccupationStats() {
        LocalDate today = LocalDate.now();
        long todayReservations = reservationRepo.findAll().stream()
            .filter(r -> today.equals(r.getReservationDate())
                && r.getStatus() != Reservation.Status.CANCELLED)
            .count();
        long todayDayAccess = dayAccessRepo.findAll().stream()
            .filter(d -> today.equals(d.getAccessDate()))
            .count();
        long activeSubs = subscriptionRepo.findAll().stream()
            .filter(s -> s.getStatus() == Subscription.Status.ACTIVE)
            .count();

        List<String> salleIds = List.of("s1", "s2", "s3");
        List<String> salleNames = List.of("Salle de Réunion", "Salle Photo", "Studio Podcast");
        List<String> salleEmojis = List.of("🏛️", "📸", "🎙️");
        List<String> salleCaps = List.of("1–8 personnes", "1–3 personnes", "1–4 personnes");

        List<RoomOccupation> rooms = new ArrayList<>();
        for (int i = 0; i < salleIds.size(); i++) {
            final String sid = salleIds.get(i);
            long cnt = reservationRepo.findAll().stream()
                .filter(r -> sid.equals(r.getSalleId())
                    && today.equals(r.getReservationDate())
                    && r.getStatus() != Reservation.Status.CANCELLED)
                .count();
            rooms.add(new RoomOccupation(sid, salleNames.get(i), salleEmojis.get(i),
                salleCaps.get(i), cnt, cnt > 0));
        }

        int occupied = (int) Math.min(todayReservations + todayDayAccess, TOTAL_CAPACITY);
        double pct = TOTAL_CAPACITY > 0 ? Math.round(occupied * 1000.0 / TOTAL_CAPACITY) / 10.0 : 0.0;

        return new OccupationStats(TOTAL_CAPACITY, occupied, pct,
            todayReservations, todayDayAccess, activeSubs, rooms, LocalDateTime.now());
    }

    public List<Map<String, Object>> getReservations(String date, String status, String salleId) {
        return reservationRepo.findAll().stream()
            .filter(r -> {
                if (date != null && !date.isBlank() && !date.equals(r.getReservationDate().toString())) return false;
                if (status != null && !status.isBlank() && !status.equalsIgnoreCase(r.getStatus().name())) return false;
                if (salleId != null && !salleId.isBlank() && !salleId.equalsIgnoreCase(r.getSalleId())) return false;
                return true;
            })
            .sorted(Comparator.comparing(Reservation::getReservationDate).reversed())
            .map(r -> {
                String userName = userRepo.findById(r.getUserId())
                    .map(User::getFullName).orElse("User #" + r.getUserId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("userId", r.getUserId());
                m.put("userName", userName);
                m.put("salleId", r.getSalleId());
                m.put("salleName", r.getSalleName());
                m.put("reservationDate", r.getReservationDate().toString());
                m.put("duration", r.getDuration().name());
                m.put("price", r.getPrice());
                m.put("status", r.getStatus().name());
                m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
                return m;
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public QrValidationResult validateQr(String token) {
        if (token == null || token.isBlank()) return fail(token, "Token vide");
        Optional<DayAccess> optAccess = dayAccessRepo.findByQrToken(token);
        if (optAccess.isEmpty()) return resolveUserFromToken(token);

        DayAccess access = optAccess.get();
        boolean used = Boolean.TRUE.equals(access.getUsed());
        User user = null;
        String name = "Visiteur";
        String email = "";
        String avatar = "?";

        if (access.getUserId() != null) {
            Optional<User> ou = userRepo.findById(access.getUserId());
            if (ou.isPresent()) {
                user = ou.get();
                name = user.getFullName();
                email = user.getEmail();
                avatar = user.getAvatarLetter();
            }
        }

        if (!used) {
            access.setUsed(true);
            dayAccessRepo.save(access);
        }

        return new QrValidationResult(true, token,
            user != null ? user.getId() : null,
            name, email, avatar,
            access.getAccessType().name(), access.getAccessDate(), used,
            used ? "⚠️ Déjà utilisé — " + name : "✅ Accès validé pour " + name,
            LocalDateTime.now());
    }

    private QrValidationResult resolveUserFromToken(String token) {
        try {
            String[] parts = token.split("-");
            for (String p : parts) {
                try {
                    long uid = Long.parseLong(p);
                    Optional<User> ou = userRepo.findById(uid);
                    if (ou.isPresent()) {
                        User u = ou.get();
                        return new QrValidationResult(true, token, u.getId(),
                            u.getFullName(), u.getEmail(), u.getAvatarLetter(),
                            "UNKNOWN", LocalDate.now(), false,
                            "✅ Membre identifié : " + u.getFullName(),
                            LocalDateTime.now());
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception ignored) {}
        return fail(token, "Token non reconnu");
    }

    private QrValidationResult fail(String token, String msg) {
        return new QrValidationResult(false, token, null, null, null, null,
            null, LocalDate.now(), false, "❌ " + msg, LocalDateTime.now());
    }

    @Transactional
    public void sendDirectMessage(Long adminId, SendMessageRequest req) {
        notifService.send(new SendNotificationRequest(
            req.getTargetUserId(), req.getTitle(), req.getBody(), req.getType(), null));
        sse.broadcast("new-notification", Map.of(
            "targetUserId", req.getTargetUserId(),
            "title", req.getTitle(),
            "body", req.getBody(),
            "sentAt", LocalDateTime.now().toString()));
        log.info("Admin {} sent direct message to user {}", adminId, req.getTargetUserId());
    }

    @Transactional
    public Object createAdminPost(Long adminId, CreateAdminPostRequest req) {
        User admin = userRepo.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        var post = socialService.createPost(adminId, admin.getFullName(),
            admin.getAvatarLetter(), "ADMIN", new CreatePostRequest(req.getContent()));

        if (req.isNotifyAll()) {
            List<Long> targets = userRepo.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()) && !u.getId().equals(adminId))
                .map(User::getId).toList();
            String shortContent = req.getContent().length() > 80
                ? req.getContent().substring(0, 80) + "…" : req.getContent();
            targets.forEach(uid -> {
                try {
                    notifService.send(new SendNotificationRequest(uid,
                        "📢 " + admin.getFullName(), shortContent, "SYSTEM", null));
                } catch (Exception e) {
                    log.warn("Failed to notify user {}: {}", uid, e.getMessage());
                }
            });
            sse.broadcast("admin-post", Map.of(
                "adminName", admin.getFullName(),
                "content", req.getContent(),
                "notifiedCount", targets.size(),
                "postedAt", LocalDateTime.now().toString()));
        }
        return post;
    }

    public List<Map<String, Object>> getAllClients() {
        return userRepo.findAll().stream()
            .sorted(Comparator.comparing(User::getId).reversed())
            .map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", u.getId());
                m.put("fullName", u.getFullName());
                m.put("email", u.getEmail());
                m.put("phone", u.getPhone() != null ? u.getPhone() : "");
                m.put("role", u.getRole().name());
                m.put("active", u.getActive());
                m.put("avatar", u.getAvatarLetter());
                m.put("hasFcmToken", u.getFcmToken() != null);
                return m;
            })
            .collect(Collectors.toList());
    }

    public ClientDetail getClientDetail(Long id) {
        User u = userRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        Optional<Subscription> activeSub = subscriptionRepo.findByUserIdAndStatus(id, Subscription.Status.ACTIVE);
        Object subObj = activeSub.<Object>map(s -> Map.of(
            "packType", s.getPackType().name(),
            "duration", s.getDuration().name(),
            "startDate", s.getStartDate().toString(),
            "endDate", s.getEndDate().toString(),
            "daysLeft", s.getDaysLeft())).orElse(null);
        String since = u.getCreatedAt() != null
            ? u.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy")) : "N/A";
        return new ClientDetail(u.getId(), u.getFullName(), u.getEmail(),
            u.getPhone() != null ? u.getPhone() : "",
            u.getRole().name(), u.getAvatarLetter(),
            u.getActive(), since, u.getFcmToken() != null, subObj);
    }

    @Transactional
    public void updateClient(Long id, ClientUpdateRequest req) {
        User u = userRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        if (req.fullName() != null && !req.fullName().isBlank()) u.setFullName(req.fullName());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.active() != null) u.setActive(req.active());
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.newPassword()));
        }
        userRepo.save(u);
        log.info("Admin updated client {}", id);
    }

    public LiveStats getLiveStats() {
        LocalDate today = LocalDate.now();
        return new LiveStats(
            userRepo.count(),
            userRepo.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getActive())).count(),
            subscriptionRepo.findAll().stream()
                .filter(s -> s.getStatus() == Subscription.Status.ACTIVE).count(),
            reservationRepo.findAll().stream()
                .filter(r -> today.equals(r.getReservationDate())).count(),
            dayAccessRepo.findAll().stream()
                .filter(d -> today.equals(d.getAccessDate())).count(),
            0L, 0L, sse.getConnectedCount(), LocalDateTime.now());
    }
}
