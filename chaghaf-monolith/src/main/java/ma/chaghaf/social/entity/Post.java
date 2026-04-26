package ma.chaghaf.social.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 100)
    private String authorName;

    @Column(length = 5)
    private String authorAvatar;

    @Column(length = 20)
    private String authorRole;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    private Integer likes = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
