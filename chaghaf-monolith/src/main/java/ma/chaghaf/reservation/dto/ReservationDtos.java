package ma.chaghaf.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationDtos {

    public record SalleInfo(
        String id, String name, String emoji, String icon,
        String capacity, BigDecimal pricePerHour,
        BigDecimal halfDayPrice, BigDecimal fullDayPrice,
        List<String> features
    ) {}

    public record ReservationResponse(
        Long id, Long userId, String salleId, String salleName,
        LocalDate reservationDate, String duration,
        BigDecimal price, String status
    ) {}
}
