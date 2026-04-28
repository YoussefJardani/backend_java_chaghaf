package ma.chaghaf.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.notification.dto.NotificationDtos.*;
import ma.chaghaf.notification.entity.Notification;
import ma.chaghaf.notification.event.NotificationEventPublisher;
import ma.chaghaf.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;

    @Autowired(required = false)
    private NotificationEventPublisher eventPublisher;

    public Notification send(SendNotificationRequest req) {
        Notification n = Notification.builder()
            .userId(req.targetUserId())
            .title(req.title())
            .body(req.body())
            .type(req.type() != null ? req.type() : "SYSTEM")
            .link(req.link())
            .read(false)
            .build();
        n = repo.save(n);
        log.info("Notification sent to user {}: {}", req.targetUserId(), req.title());

        if (eventPublisher != null) {
            try {
                eventPublisher.publish(n);
            } catch (Exception e) {
                log.warn("RabbitMQ publish failed (notification still saved): {}", e.getMessage());
            }
        }
        return n;
    }

    public List<NotificationResponse> findByUser(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(n -> new NotificationResponse(
                n.getId(), n.getTitle(), n.getBody(), n.getType(),
                n.getLink(), n.getRead(), n.getCreatedAt()))
            .toList();
    }

    public long countUnread(Long userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        repo.markAllReadForUser(userId);
    }
}
