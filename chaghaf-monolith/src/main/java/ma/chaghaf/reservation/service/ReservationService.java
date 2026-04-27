package ma.chaghaf.reservation.service;

import lombok.RequiredArgsConstructor;
import ma.chaghaf.notification.entity.Notification;
import ma.chaghaf.notification.repository.NotificationRepository;
import ma.chaghaf.reservation.dto.ReservationDtos.*;
import ma.chaghaf.reservation.entity.Reservation;
import ma.chaghaf.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository repo;
    private final NotificationRepository notifRepo;

    public List<ReservationResponse> findByUser(Long userId) {
        return repo.findByUserId(userId).stream().map(this::toDto).toList();
    }

    public List<SalleInfo> listSalles() {
        return List.of(
            new SalleInfo("s1", "Salle de Réunion", "🏛️", "business-outline",
                "1–8 personnes", new BigDecimal("100.00"),
                new BigDecimal("400.00"), new BigDecimal("700.00"),
                List.of("Wifi", "Écran TV", "Tableau blanc", "Climatisation")),
            new SalleInfo("s2", "Salle Photo", "📸", "camera-outline",
                "1–3 personnes", new BigDecimal("80.00"),
                new BigDecimal("320.00"), new BigDecimal("560.00"),
                List.of("Studio photo", "Éclairage pro", "Fond blanc")),
            new SalleInfo("s3", "Studio Podcast", "🎙️", "mic-outline",
                "1–4 personnes", new BigDecimal("120.00"),
                new BigDecimal("480.00"), new BigDecimal("840.00"),
                List.of("Microphones", "Insonorisation", "Mixeur audio"))
        );
    }

    @Transactional
    public ReservationResponse create(Long userId, Map<String, Object> body) {
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        String salleId = (String) body.get("salleId");
        String dateStr = (String) body.get("reservationDate");
        String durationStr = (String) body.get("duration");

        if (salleId == null || dateStr == null || durationStr == null) {
            throw new IllegalArgumentException("salleId, reservationDate et duration sont requis");
        }

        SalleInfo salle = listSalles().stream()
            .filter(s -> s.id().equalsIgnoreCase(salleId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Salle introuvable: " + salleId));

        Reservation.Duration duration;
        try {
            duration = Reservation.Duration.valueOf(durationStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Durée invalide: " + durationStr);
        }

        BigDecimal price = switch (duration) {
            case ONE_HOUR  -> salle.pricePerHour();
            case TWO_HOURS -> salle.pricePerHour().multiply(BigDecimal.valueOf(2));
            case HALF_DAY  -> salle.halfDayPrice();
            case FULL_DAY  -> salle.fullDayPrice();
        };

        Reservation r = Reservation.builder()
            .userId(userId)
            .salleId(salle.id())
            .salleName(salle.name())
            .reservationDate(LocalDate.parse(dateStr))
            .duration(duration)
            .price(price)
            .status(Reservation.Status.CONFIRMED)
            .build();
        r = repo.save(r);

        notifRepo.save(Notification.builder()
            .userId(userId)
            .title("Réservation confirmée")
            .body(salle.name() + " · " + dateStr + " · " + price + " dh")
            .type("RESERVATION")
            .read(false)
            .build());

        return toDto(r);
    }

    @Transactional
    public void cancel(Long userId, Long id) {
        Reservation r = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Réservation introuvable"));
        if (!r.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Accès non autorisé");
        }
        r.setStatus(Reservation.Status.CANCELLED);
        repo.save(r);

        notifRepo.save(Notification.builder()
            .userId(userId)
            .title("Réservation annulée")
            .body(r.getSalleName() + " · " + r.getReservationDate())
            .type("RESERVATION")
            .read(false)
            .build());
    }

    private ReservationResponse toDto(Reservation r) {
        return new ReservationResponse(
            r.getId(), r.getUserId(), r.getSalleId(), r.getSalleName(),
            r.getReservationDate(), r.getDuration().name(),
            r.getPrice(), r.getStatus().name()
        );
    }
}
