package ma.chaghaf.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.config.SseEmitterManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements de notification depuis RabbitMQ.
 * Aujourd'hui : rebroadcast SSE vers les ERP connectés.
 * Demain : push FCM, email, webhooks externes, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chaghaf.rabbitmq", name = "enabled", havingValue = "true")
public class NotificationEventListener {

    private final SseEmitterManager sse;

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void handle(NotificationEvent event) {
        log.info("Notification event received from RabbitMQ: id={} type={}", event.id(), event.type());
        try {
            sse.broadcast("new-notification", event);
        } catch (Exception e) {
            log.warn("SSE rebroadcast failed: {}", e.getMessage());
        }
    }
}
