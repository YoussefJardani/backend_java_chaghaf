package ma.chaghaf.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SubscriptionDtos {

    public record SubscriptionResponse(
        Long id, Long userId, String packType, String duration,
        LocalDate startDate, LocalDate endDate, BigDecimal price,
        String status, long daysLeft
    ) {}

    public record PackInfo(
        String type, String label, String description,
        BigDecimal pricePerMonth, String[] features
    ) {}
}
