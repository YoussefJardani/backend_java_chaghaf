package ma.chaghaf.social.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class SocialDtos {

    public record CreatePostRequest(@NotBlank String content) {}

    public record PostResponse(
        Long id, Long authorId, String authorName, String authorAvatar,
        String authorRole, String content, Integer likes, LocalDateTime createdAt
    ) {}
}
