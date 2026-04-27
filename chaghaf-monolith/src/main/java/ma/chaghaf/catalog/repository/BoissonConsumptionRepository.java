package ma.chaghaf.catalog.repository;

import ma.chaghaf.catalog.entity.BoissonConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BoissonConsumptionRepository extends JpaRepository<BoissonConsumption, Long> {
    List<BoissonConsumption> findByUserIdAndConsumedDay(Long userId, LocalDate consumedDay);
    long countByUserIdAndConsumedDay(Long userId, LocalDate consumedDay);
}
