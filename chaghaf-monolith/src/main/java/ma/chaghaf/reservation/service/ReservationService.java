package ma.chaghaf.reservation.service;

import lombok.RequiredArgsConstructor;
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

    public List<ReservationResponse> findByUser(Long userId) {
        return repo.findByUserId(userId).stream().map(this::toDto).toList();
    }

    public List<SalleInfo> listSalles() {
        return List.of(
            new SalleInfo("s1", "Salle de Réunion", "🏛️", "1–8 personnes", new BigDecimal("100.00")),
            new SalleInfo("s2", "Salle Photo", "📸", "1–3 personnes", new BigDecimal("80.00")),
            new SalleInfo("s3", "Studio Podcast", "🎙️", "1–4 personnes", new BigDecimal("120.00"))
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
            case HALF_DAY  -> salle.pricePerHour().multiply(BigDecimal.valueOf(4));
            case FULL_DAY  -> salle.pricePerHour().multiply(BigDecimal.valueOf(8));
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
        return toDto(repo.save(r));
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
    }

    private ReservationResponse toDto(Reservation r) {
        return new ReservationResponse(
            r.getId(), r.getUserId(), r.getSalleId(), r.getSalleName(),
            r.getReservationDate(), r.getDuration().name(),
            r.getPrice(), r.getStatus().name()
        );
    }
}
