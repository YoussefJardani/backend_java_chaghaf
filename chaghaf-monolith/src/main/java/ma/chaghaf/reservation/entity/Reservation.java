package ma.chaghaf.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String salleId;

    @Column(nullable = false, length = 100)
    private String salleName;

    @Column(nullable = false)
    private LocalDate reservationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Duration duration;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Status { PENDING, CONFIRMED, CANCELLED, COMPLETED }
    public enum Duration { ONE_HOUR, TWO_HOURS, HALF_DAY, FULL_DAY }
}
