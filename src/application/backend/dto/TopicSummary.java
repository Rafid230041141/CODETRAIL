package application.backend.dto;

public record TopicSummary(Long id, String slug, String title, String description, int position) {
}
