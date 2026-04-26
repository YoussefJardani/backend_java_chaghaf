package ma.chaghaf.catalog.repository;

import ma.chaghaf.catalog.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findByTypeOrderByNameAsc(CatalogItem.ItemType type);
    List<CatalogItem> findByAvailableTrueOrderByTypeAscNameAsc();
    List<CatalogItem> findAllByOrderByTypeAscNameAsc();
}
