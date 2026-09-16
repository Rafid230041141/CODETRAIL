package application.backend.repository;

import application.backend.domain.Submodule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmoduleRepository extends JpaRepository<Submodule, Long> {
    List<Submodule> findByModuleIdOrderByPositionAsc(Long moduleId);
}
