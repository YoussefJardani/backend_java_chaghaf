package ma.chaghaf.catalog.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.admin.dto.AdminDtos.CatalogItemDto;
import ma.chaghaf.catalog.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/snacks")
@RequiredArgsConstructor
public class SnackController {

    private final CatalogService catalogService;

    private static final AtomicLong ORDER_COUNTER = new AtomicLong(1);

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

        return ResponseEntity.ok(Map.of(
            "id", ORDER_COUNTER.getAndIncrement(),
            "userId", userId != null ? userId : 0,
            "items", items,
            "note", body.getOrDefault("note", ""),
            "totalPrice", total,
            "status", "PENDING",
            "createdAt", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Object>> myOrders() {
        return ResponseEntity.ok(List.of());
    }
}
