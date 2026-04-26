package ma.chaghaf.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class NotificationDtos {

    public record SendNotificationRequest(
        @NotNull Long targetUserId,
        @NotBlank String title,
        @NotBlank String body,
        String type,
        String link
    ) {}

    public record NotificationResponse(
        Long id, String title, String body, String type, String link,
        Boolean read, LocalDateTime createdAt
    ) {}
}
