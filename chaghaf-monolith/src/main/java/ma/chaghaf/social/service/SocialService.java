package ma.chaghaf.social.service;

import lombok.RequiredArgsConstructor;
import ma.chaghaf.social.dto.SocialDtos.*;
import ma.chaghaf.social.entity.Post;
import ma.chaghaf.social.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final PostRepository repo;

    public PostResponse createPost(Long authorId, String authorName, String authorAvatar,
                                    String authorRole, CreatePostRequest req) {
        Post p = Post.builder()
            .authorId(authorId)
            .authorName(authorName)
            .authorAvatar(authorAvatar)
            .authorRole(authorRole)
            .content(req.content())
            .likes(0)
            .build();
        p = repo.save(p);
        return toDto(p);
    }

    public List<PostResponse> listAll() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public Map<String, Object> toggleLike(Long postId) {
        Post p = repo.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post introuvable"));
        int current = p.getLikes() != null ? p.getLikes() : 0;
        p.setLikes(current + 1);
        repo.save(p);
        return Map.of("postId", postId, "likes", p.getLikes());
    }

    private PostResponse toDto(Post p) {
        return new PostResponse(
            p.getId(), p.getAuthorId(), p.getAuthorName(), p.getAuthorAvatar(),
            p.getAuthorRole(), p.getContent(), p.getLikes(), p.getCreatedAt()
        );
    }
}
