package application.backend.dto;

import java.util.List;

public record ProgressSummary(long completedLessons, long totalLessons, long quizAttempts,
                              List<TopicProgress> topics) {
}
