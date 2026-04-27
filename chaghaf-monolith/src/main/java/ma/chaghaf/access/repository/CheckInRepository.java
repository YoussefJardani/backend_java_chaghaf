package ma.chaghaf.access.repository;

import ma.chaghaf.access.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    List<CheckIn> findByActiveTrueOrderByCheckedInAtDesc();
    long countByActiveTrue();
    Optional<CheckIn> findFirstByUserIdAndActiveTrueOrderByCheckedInAtDesc(Long userId);
}
