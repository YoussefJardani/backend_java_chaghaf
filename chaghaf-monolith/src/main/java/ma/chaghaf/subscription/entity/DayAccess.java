package ma.chaghaf.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "day_accesses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DayAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false, unique = true, length = 100)
    private String qrToken;

    @Column(nullable = false)
    private LocalDate accessDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccessType accessType = AccessType.DAY_PASS;

    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum AccessType { DAY_PASS, SUBSCRIPTION, GUEST, UNKNOWN }
}
