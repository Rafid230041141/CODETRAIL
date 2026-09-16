package application.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuizAttemptRequest(@NotNull @Valid List<Integer> answers) {
}
