package ma.chaghaf.catalog.controller;

import lombok.RequiredArgsConstructor;
import ma.chaghaf.admin.dto.AdminDtos.CatalogItemDto;
import ma.chaghaf.catalog.entity.CatalogItem;
import ma.chaghaf.catalog.repository.CatalogItemRepository;
import ma.chaghaf.catalog.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boissons")
@RequiredArgsConstructor
public class BoissonController {

    private final CatalogService catalogService;
    private final CatalogItemRepository catalogItemRepo;

    @GetMapping
    public ResponseEntity<List<CatalogItemDto>> listBoissons() {
        return ResponseEntity.ok(catalogService.listByType("BOISSON"));
    }

    @PostMapping("/consume")
    @Transactional
    public ResponseEntity<Map<String, Object>> consume(@RequestBody Map<String, Object> body) {
        String boissonType = body.get("boissonType") != null ? body.get("boissonType").toString() : null;
        if (boissonType != null) {
            catalogItemRepo.findAll().stream()
                .filter(i -> i.getType() == CatalogItem.ItemType.BOISSON
                    && i.getName().equalsIgnoreCase(boissonType)
                    && Boolean.TRUE.equals(i.getAvailable())
                    && i.getStockQuantity() != null && i.getStockQuantity() > 0)
                .findFirst()
                .ifPresent(item -> {
                    item.setStockQuantity(Math.max(0, item.getStockQuantity() - 1));
                    if (item.getStockQuantity() == 0) item.setAvailable(false);
                    catalogItemRepo.save(item);
                });
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Boisson consommée",
            "boissonType", boissonType != null ? boissonType : "",
            "consumedAt", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Object>> history() {
        return ResponseEntity.ok(List.of());
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
