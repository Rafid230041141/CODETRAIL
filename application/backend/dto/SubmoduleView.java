package application.backend.dto;

import java.util.List;

public record SubmoduleView(Long id, String title, int position, List<LessonSummary> lessons) {
}
