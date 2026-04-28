package ma.chaghaf.notification.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Payload publié sur RabbitMQ après création d'une notification.
 * Le listener peut le consommer pour push FCM, email, etc.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationEvent(
    Long id,
    Long userId,
    String title,
    String body,
    String type,
    String link,
    LocalDateTime createdAt
) implements Serializable {}
