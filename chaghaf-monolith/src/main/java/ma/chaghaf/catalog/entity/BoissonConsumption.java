package ma.chaghaf.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "boisson_consumptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoissonConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate consumedDay;  // jour "logique" (reset à 7h)

    @Column(nullable = false, length = 100)
    private String boissonName;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    @Builder.Default
    private Boolean wasFree = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
