package application.backend.repository;

import application.backend.domain.LessonProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    List<LessonProgress> findByUserId(Long userId);
    long countByUserIdAndCompletedTrue(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndLessonSubmoduleModuleTopicIdAndCompletedTrue(Long userId, Long topicId);
}
