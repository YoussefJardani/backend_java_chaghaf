package ma.chaghaf.catalog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.admin.dto.AdminDtos.CatalogItemDto;
import ma.chaghaf.admin.dto.AdminDtos.SaveCatalogItemRequest;
import ma.chaghaf.catalog.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService service;

    @GetMapping
    public ResponseEntity<List<CatalogItemDto>> listAvailable() {
        return ResponseEntity.ok(service.listAvailable());
    }

    @GetMapping("/all")
    public ResponseEntity<List<CatalogItemDto>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<CatalogItemDto>> listByType(@PathVariable String type) {
        return ResponseEntity.ok(service.listByType(type));
    }

    @PostMapping
    public ResponseEntity<CatalogItemDto> create(@Valid @RequestBody SaveCatalogItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CatalogItemDto> update(@PathVariable Long id,
                                                  @RequestBody SaveCatalogItemRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Item supprimé"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CatalogItemDto> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleAvailability(id));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<CatalogItemDto> adjustStock(@PathVariable Long id,
                                                       @RequestBody Map<String, Integer> body) {
        int delta = body.getOrDefault("delta", 0);
        return ResponseEntity.ok(service.updateStock(id, delta));
    }
}
