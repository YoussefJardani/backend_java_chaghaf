package ma.chaghaf.catalog.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.admin.dto.AdminDtos.CatalogItemDto;
import ma.chaghaf.catalog.entity.BoissonConsumption;
import ma.chaghaf.catalog.entity.CatalogItem;
import ma.chaghaf.catalog.repository.BoissonConsumptionRepository;
import ma.chaghaf.catalog.repository.CatalogItemRepository;
import ma.chaghaf.catalog.service.CatalogService;
import ma.chaghaf.notification.entity.Notification;
import ma.chaghaf.notification.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boissons")
@RequiredArgsConstructor
public class BoissonController {

    private final CatalogService catalogService;
    private final CatalogItemRepository catalogItemRepo;
    private final BoissonConsumptionRepository consumptionRepo;
    private final NotificationRepository notifRepo;

    @GetMapping
    public ResponseEntity<List<CatalogItemDto>> listBoissons() {
        return ResponseEntity.ok(catalogService.listByType("BOISSON"));
    }

    /**
     * Le "jour logique" se renouvelle à 7h du matin.
     * Avant 7h → on considère qu'on est encore dans le jour précédent.
     */
    private LocalDate currentLogicalDay() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() < 7) return now.toLocalDate().minusDays(1);
        return now.toLocalDate();
    }

    @GetMapping("/today-status")
    public ResponseEntity<Map<String, Object>> todayStatus(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        LocalDate day = currentLogicalDay();
        long count = consumptionRepo.countByUserIdAndConsumedDay(userId, day);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "logicalDay", day.toString(),
            "consumedToday", count,
            "freeAvailable", count == 0,
            "resetTime", "07:00",
            "message", count == 0
                ? "Votre première boisson de la journée est gratuite !"
                : "Votre boisson gratuite a déjà été utilisée. Les suivantes sont payantes."
        ));
    }

    @PostMapping("/consume")
    @Transactional
    public ResponseEntity<Map<String, Object>> consume(HttpServletRequest req,
                                                        @RequestBody Map<String, Object> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        String boissonType = body.get("boissonType") != null ? body.get("boissonType").toString() : null;
        if (boissonType == null) {
            throw new IllegalArgumentException("boissonType est requis");
        }

        // Trouver la boisson dans le catalogue
        CatalogItem item = catalogItemRepo.findAll().stream()
            .filter(i -> i.getType() == CatalogItem.ItemType.BOISSON
                && i.getName().equalsIgnoreCase(boissonType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Boisson introuvable: " + boissonType));

        if (!Boolean.TRUE.equals(item.getAvailable())
            || item.getStockQuantity() == null || item.getStockQuantity() <= 0) {
            throw new IllegalArgumentException("Boisson en rupture de stock");
        }

        // Vérifier si l'utilisateur a déjà consommé aujourd'hui
        LocalDate day = currentLogicalDay();
        long alreadyConsumed = consumptionRepo.countByUserIdAndConsumedDay(userId, day);
        boolean isFree = alreadyConsumed == 0;
        BigDecimal price = isFree ? BigDecimal.ZERO : (item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);

        // Décrémenter le stock
        item.setStockQuantity(Math.max(0, item.getStockQuantity() - 1));
        if (item.getStockQuantity() == 0) item.setAvailable(false);
        catalogItemRepo.save(item);

        // Enregistrer la consommation
        BoissonConsumption consumption = BoissonConsumption.builder()
            .userId(userId)
            .consumedDay(day)
            .boissonName(item.getName())
            .price(price)
            .wasFree(isFree)
            .build();
        consumptionRepo.save(consumption);

        // Notification
        notifRepo.save(Notification.builder()
            .userId(userId)
            .title(isFree ? "Boisson gratuite servie" : "Boisson facturée")
            .body(item.getName() + (isFree ? " · Offerte (1ère du jour)" : " · " + price + " dh"))
            .type("BOISSON")
            .read(false)
            .build());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("boissonType", item.getName());
        response.put("free", isFree);
        response.put("price", price);
        response.put("message", isFree
            ? "Première boisson de la journée — offerte !"
            : "Boisson facturée: " + price + " dh");
        response.put("consumedAt", LocalDateTime.now().toString());
        response.put("nextDrinkPrice", item.getPrice());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> history(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        LocalDate day = currentLogicalDay();
        List<Map<String, Object>> history = consumptionRepo.findByUserIdAndConsumedDay(userId, day).stream()
            .map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("boissonName", c.getBoissonName());
                m.put("price", c.getPrice());
                m.put("wasFree", c.getWasFree());
                m.put("consumedAt", c.getCreatedAt().toString());
                return m;
            })
            .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/cafe-guide")
    public ResponseEntity<Map<String, Object>> cafeGuide() {
        return ResponseEntity.ok(Map.of(
            "title", "Guide Café",
            "steps", List.of(
                Map.of("step", 1, "instruction", "Insérez votre capsule dans la machine"),
                Map.of("step", 2, "instruction", "Choisissez la taille de votre tasse"),
                Map.of("step", 3, "instruction", "Appuyez sur le bouton correspondant"),
                Map.of("step", 4, "instruction", "Patientez 30 secondes")
            ),
            "tips", List.of(
                "Nettoyez la machine après usage",
                "Jetez les capsules usagées dans la poubelle prévue"
            )
        ));
    }
}
