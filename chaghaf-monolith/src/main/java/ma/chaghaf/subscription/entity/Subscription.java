package ma.chaghaf.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PackType packType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Duration duration;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public long getDaysLeft() {
        LocalDate today = LocalDate.now();
        if (today.isAfter(endDate)) return 0;
        return ChronoUnit.DAYS.between(today, endDate);
    }

    public enum PackType { BASIC, PREMIUM, VIP, STUDENT }
    public enum Duration { WEEK, MONTH, QUARTER, YEAR }
    public enum Status { ACTIVE, EXPIRED, CANCELLED }
}
