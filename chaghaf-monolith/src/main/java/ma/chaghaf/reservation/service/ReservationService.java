package ma.chaghaf.reservation.service;

import lombok.RequiredArgsConstructor;
import ma.chaghaf.reservation.dto.ReservationDtos.*;
import ma.chaghaf.reservation.entity.Reservation;
import ma.chaghaf.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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

    private ReservationResponse toDto(Reservation r) {
        return new ReservationResponse(
            r.getId(), r.getUserId(), r.getSalleId(), r.getSalleName(),
            r.getReservationDate(), r.getDuration().name(),
            r.getPrice(), r.getStatus().name()
        );
    }
}
