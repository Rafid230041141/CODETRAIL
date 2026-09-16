package application.backend.dto;

public record LessonSummary(Long id, String slug, String title, String summary, int position, boolean completed) {
}
