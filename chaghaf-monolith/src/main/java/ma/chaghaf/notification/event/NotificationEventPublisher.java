package ma.chaghaf.notification.event;

import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.notification.entity.Notification;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publie les notifications sur RabbitMQ. Activé seulement si
 * `chaghaf.rabbitmq.enabled=true` (sinon NotificationService continue
 * de fonctionner en mode synchrone DB-only).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "chaghaf.rabbitmq", name = "enabled", havingValue = "true")
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public NotificationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(Notification n) {
        NotificationEvent event = new NotificationEvent(
            n.getId(),
            n.getUserId(),
            n.getTitle(),
            n.getBody(),
            n.getType(),
            n.getLink(),
            n.getCreatedAt()
        );
        rabbitTemplate.convertAndSend(
            RabbitConfig.NOTIFICATION_EXCHANGE,
            RabbitConfig.NOTIFICATION_ROUTING_KEY,
            event
        );
        log.debug("Notification event published: id={} user={}", n.getId(), n.getUserId());
    }
}
