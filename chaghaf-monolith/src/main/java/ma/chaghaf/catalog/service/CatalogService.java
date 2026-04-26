package ma.chaghaf.catalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.admin.dto.AdminDtos.CatalogItemDto;
import ma.chaghaf.admin.dto.AdminDtos.SaveCatalogItemRequest;
import ma.chaghaf.catalog.entity.CatalogItem;
import ma.chaghaf.catalog.repository.CatalogItemRepository;
import ma.chaghaf.config.SseEmitterManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogItemRepository repo;
    private final SseEmitterManager sse;

    public List<CatalogItemDto> listAll() {
        return repo.findAllByOrderByTypeAscNameAsc().stream().map(this::toDto).toList();
    }

    public List<CatalogItemDto> listByType(String type) {
        CatalogItem.ItemType t = CatalogItem.ItemType.valueOf(type.toUpperCase());
        return repo.findByTypeOrderByNameAsc(t).stream().map(this::toDto).toList();
    }

    public List<CatalogItemDto> listAvailable() {
        return repo.findByAvailableTrueOrderByTypeAscNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public CatalogItemDto create(SaveCatalogItemRequest req) {
        CatalogItem item = CatalogItem.builder()
            .type(CatalogItem.ItemType.valueOf(req.type().toUpperCase()))
            .name(req.name())
            .description(req.description())
            .price(req.price())
            .emoji(req.emoji())
            .imageBase64(sanitiseBase64(req.imageBase64()))
            .imageMimeType(req.imageMimeType())
            .available(req.available() != null ? req.available() : true)
            .stockQuantity(req.stockQuantity() != null ? req.stockQuantity() : 0)
            .category(req.category())
            .build();
        item = repo.save(item);
        log.info("Catalog item created: {} ({})", item.getName(), item.getType());
        sse.broadcast("catalog-updated", Map.of("action", "created", "item", toDto(item)));
        return toDto(item);
    }

    @Transactional
    public CatalogItemDto update(Long id, SaveCatalogItemRequest req) {
        CatalogItem item = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + id));
        if (req.name() != null) item.setName(req.name());
        if (req.description() != null) item.setDescription(req.description());
        if (req.price() != null) item.setPrice(req.price());
        if (req.emoji() != null) item.setEmoji(req.emoji());
        if (req.available() != null) item.setAvailable(req.available());
        if (req.stockQuantity() != null) item.setStockQuantity(req.stockQuantity());
        if (req.category() != null) item.setCategory(req.category());
        if (req.imageBase64() != null && !req.imageBase64().isBlank()) {
            item.setImageBase64(sanitiseBase64(req.imageBase64()));
            item.setImageMimeType(req.imageMimeType());
        }
        item = repo.save(item);
        sse.broadcast("catalog-updated", Map.of("action", "updated", "item", toDto(item)));
        return toDto(item);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new IllegalArgumentException("Catalog item not found: " + id);
        repo.deleteById(id);
        sse.broadcast("catalog-updated", Map.of("action", "deleted", "id", id));
    }

    @Transactional
    public CatalogItemDto toggleAvailability(Long id) {
        CatalogItem item = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + id));
        item.setAvailable(!Boolean.TRUE.equals(item.getAvailable()));
        item = repo.save(item);
        sse.broadcast("catalog-updated", Map.of("action", "toggled", "item", toDto(item)));
        return toDto(item);
    }

    @Transactional
    public CatalogItemDto updateStock(Long id, int delta) {
        CatalogItem item = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + id));
        int newQty = Math.max(0, item.getStockQuantity() + delta);
        item.setStockQuantity(newQty);
        item.setAvailable(newQty > 0);
        item = repo.save(item);
        sse.broadcast("catalog-updated", Map.of("action", "stock", "item", toDto(item)));
        return toDto(item);
    }

    private String sanitiseBase64(String raw) {
        if (raw == null) return null;
        int comma = raw.indexOf(',');
        return comma >= 0 ? raw.substring(comma + 1) : raw;
    }

    private CatalogItemDto toDto(CatalogItem i) {
        return new CatalogItemDto(
            i.getId(), i.getType().name(), i.getName(), i.getDescription(),
            i.getPrice(), i.getEmoji(),
            i.getImageBase64(), i.getImageMimeType(),
            i.getAvailable(), i.getStockQuantity(), i.getCategory(),
            i.getCreatedAt()
        );
    }
}
