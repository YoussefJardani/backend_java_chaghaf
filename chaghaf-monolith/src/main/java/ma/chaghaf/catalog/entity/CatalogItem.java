package ma.chaghaf.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String emoji;

    @Column(columnDefinition = "TEXT")
    private String imageBase64;

    @Column(length = 50)
    private String imageMimeType;

    @Builder.Default
    private Boolean available = true;

    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(length = 60)
    private String category;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ItemType { BOISSON, SNACK }
}
