package ma.chaghaf.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_change_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 30)
    private String currentPack;

    @Column(nullable = false, length = 30)
    private String requestedPack;

    @Column(nullable = false, length = 30)
    private String requestedDuration;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String adminNote;

    private Long adminId;

    private LocalDateTime processedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Status { PENDING, APPROVED, REJECTED }
}
