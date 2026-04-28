package ma.chaghaf.catalog.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.admin.dto.AdminDtos.CatalogItemDto;
import ma.chaghaf.catalog.entity.SnackOrder;
import ma.chaghaf.catalog.repository.SnackOrderRepository;
import ma.chaghaf.catalog.service.CatalogService;
import ma.chaghaf.notification.dto.NotificationDtos.SendNotificationRequest;
import ma.chaghaf.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/snacks")
@RequiredArgsConstructor
public class SnackController {

    private final CatalogService catalogService;
    private final SnackOrderRepository orderRepo;
    private final NotificationService notifications;
    private final ObjectMapper json = new ObjectMapper();

    @GetMapping("/catalog")
    public ResponseEntity<List<CatalogItemDto>> catalog() {
        return ResponseEntity.ok(catalogService.listByType("SNACK"));
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(HttpServletRequest req,
                                                            @RequestBody Map<String, Object> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = body.get("items") instanceof List
            ? (List<Map<String, Object>>) body.get("items")
            : List.of();

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            Object price = item.get("price");
            Object qty = item.get("quantity");
            if (price != null && qty != null) {
                try {
                    BigDecimal p = new BigDecimal(price.toString());
                    int q = Integer.parseInt(qty.toString());
                    total = total.add(p.multiply(BigDecimal.valueOf(q)));
                } catch (Exception ignored) {}
            }
        }

        String itemsJson;
        try {
            itemsJson = json.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            itemsJson = "[]";
        }

        String note = body.getOrDefault("note", "") == null ? "" : body.get("note").toString();

        SnackOrder order = SnackOrder.builder()
            .userId(userId != null ? userId : 0L)
            .itemsJson(itemsJson)
            .note(note)
            .totalPrice(total)
            .status("PENDING")
            .build();
        order = orderRepo.save(order);

        try {
            notifications.send(new SendNotificationRequest(
                order.getUserId(),
                "Commande snacks reçue",
                "Votre commande #" + order.getId() + " (" + total + " dh) a été enregistrée.",
                "SNACK_ORDER",
                "/snacks/orders/" + order.getId()
            ));
        } catch (Exception e) {
            log.warn("Notif snack creation failed: {}", e.getMessage());
        }

        return ResponseEntity.ok(toDto(order, items));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> myOrders(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        if (userId == null) return ResponseEntity.ok(List.of());
        List<Map<String, Object>> out = orderRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(o -> toDto(o, parseItems(o.getItemsJson())))
            .toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable Long id) {
        return orderRepo.findById(id)
            .map(o -> ResponseEntity.ok(toDto(o, parseItems(o.getItemsJson()))))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body) {
        String newStatus = body.getOrDefault("status", "PENDING");
        return orderRepo.findById(id)
            .map(o -> {
                o.setStatus(newStatus);
                o.setUpdatedAt(LocalDateTime.now());
                orderRepo.save(o);

                try {
                    notifications.send(new SendNotificationRequest(
                        o.getUserId(),
                        "Mise à jour commande snacks",
                        "Votre commande #" + o.getId() + " est maintenant : " + newStatus,
                        "SNACK_ORDER",
                        "/snacks/orders/" + o.getId()
                    ));
                } catch (Exception ignored) {}

                return ResponseEntity.ok(toDto(o, parseItems(o.getItemsJson())));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private List<Map<String, Object>> parseItems(String s) {
        if (s == null || s.isBlank()) return List.of();
        try {
            return json.readValue(s, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>(){});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> toDto(SnackOrder o, List<Map<String, Object>> items) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", o.getId());
        m.put("userId", o.getUserId());
        m.put("items", items);
        m.put("note", o.getNote() == null ? "" : o.getNote());
        m.put("totalPrice", o.getTotalPrice());
        m.put("status", o.getStatus());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        m.put("updatedAt", o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null);
        return m;
    }
}
