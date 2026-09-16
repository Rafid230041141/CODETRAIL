package application.backend.repository;

import application.backend.domain.Lesson;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findBySubmoduleIdOrderByPositionAsc(Long submoduleId);
    List<Lesson> findBySubmoduleModuleTopicSlugIgnoreCase(String topicSlug);
    long countBySubmoduleModuleTopicId(Long topicId);
    long countBySubmoduleModuleTopicIdAndPublishedTrue(Long topicId);
}
