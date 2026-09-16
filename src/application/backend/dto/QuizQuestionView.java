package application.backend.dto;

import java.util.List;

public record QuizQuestionView(Long id, String prompt, List<String> options) {
}
