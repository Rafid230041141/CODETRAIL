package application.backend.repository;

import application.backend.domain.Enrollment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByUserIdAndTopicId(Long userId, Long topicId);
}
