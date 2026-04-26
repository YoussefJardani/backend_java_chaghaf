package ma.chaghaf.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationDtos {

    public record SalleInfo(
        String id, String name, String emoji,
        String capacity, BigDecimal pricePerHour
    ) {}

    public record ReservationResponse(
        Long id, Long userId, String salleId, String salleName,
        LocalDate reservationDate, String duration,
        BigDecimal price, String status
    ) {}
}
