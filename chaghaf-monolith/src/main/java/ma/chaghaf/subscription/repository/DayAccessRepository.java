package ma.chaghaf.subscription.repository;

import ma.chaghaf.subscription.entity.DayAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DayAccessRepository extends JpaRepository<DayAccess, Long> {
    Optional<DayAccess> findByQrToken(String qrToken);
    List<DayAccess> findByUserId(Long userId);
    List<DayAccess> findByUserIdAndAccessDate(Long userId, LocalDate date);
}
