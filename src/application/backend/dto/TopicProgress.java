package application.backend.dto;

public record TopicProgress(Long topicId, String title, long completedLessons, long totalLessons) {
}
