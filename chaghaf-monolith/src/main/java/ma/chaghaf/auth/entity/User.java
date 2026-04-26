package ma.chaghaf.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private Boolean active = true;

    @Column(length = 500)
    private String fcmToken;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getAvatarLetter() {
        if (fullName == null || fullName.isBlank()) return "?";
        return String.valueOf(fullName.trim().charAt(0)).toUpperCase();
    }

    public enum Role { USER, ADMIN }
}
