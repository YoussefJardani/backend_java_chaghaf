package ma.chaghaf.subscription.repository;

import ma.chaghaf.subscription.entity.SubscriptionChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionChangeRequestRepository extends JpaRepository<SubscriptionChangeRequest, Long> {
    List<SubscriptionChangeRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SubscriptionChangeRequest> findByStatusOrderByCreatedAtDesc(SubscriptionChangeRequest.Status status);
}
