package ma.chaghaf.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.notification.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Supprime automatiquement les notifications de plus de 30 jours.
 * Empêche l'accumulation infinie de la table.
 * S'exécute tous les jours à 3h du matin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationRepository repo;

    @Scheduled(cron = "0 0 3 * * *")  // tous les jours à 03:00
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = repo.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Notifications cleanup: {} rows deleted (older than {})", deleted, cutoff);
        } else {
            log.debug("Notifications cleanup: nothing to delete");
        }
    }
}
