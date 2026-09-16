package application.backend.repository;

import application.backend.domain.QuizQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByLessonIdOrderByQuestionOrderAsc(Long lessonId);
    long countByLessonId(Long lessonId);
}
