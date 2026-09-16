package application.backend.repository;

import application.backend.domain.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByPublishedTrueAndSlugInOrderByPositionAsc(List<String> slugs);
    List<Topic> findByPublishedTrueOrderByPositionAsc();
    Optional<Topic> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
}
