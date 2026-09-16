package application.backend.repository;

import application.backend.domain.Simulation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {
    Optional<Simulation> findByLessonId(Long lessonId);
}
