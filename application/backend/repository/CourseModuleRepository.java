package application.backend.repository;

import application.backend.domain.CourseModule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {
    List<CourseModule> findByTopicIdOrderByPositionAsc(Long topicId);
}
