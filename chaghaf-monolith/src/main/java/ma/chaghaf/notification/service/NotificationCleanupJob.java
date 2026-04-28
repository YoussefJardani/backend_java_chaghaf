package ma.chaghaf.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.notification.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Supprime les notifications de plus de 2 jours.
 * Tourne toutes les heures pour rester réactif (notifications expirent vite côté UX).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private static final int RETENTION_DAYS = 2;

    private final NotificationRepository repo;

    @Scheduled(cron = "0 0 * * * *")  // toutes les heures, à la minute 0
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = repo.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Notifications cleanup: {} rows deleted (older than {})", deleted, cutoff);
        }
    }
}
