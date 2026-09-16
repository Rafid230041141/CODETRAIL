package application.backend.dto;

import java.util.List;

public record QuizAttemptResult(int score, int total, List<String> feedback) {
}
