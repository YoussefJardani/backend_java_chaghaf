package ma.chaghaf.catalog.repository;

import ma.chaghaf.catalog.entity.SnackOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnackOrderRepository extends JpaRepository<SnackOrder, Long> {

    List<SnackOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SnackOrder> findAllByOrderByCreatedAtDesc();

    List<SnackOrder> findByStatusOrderByCreatedAtDesc(String status);
}
