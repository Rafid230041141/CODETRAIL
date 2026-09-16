package application.backend.dto;

import jakarta.validation.constraints.NotNull;

public record LessonProgressRequest(@NotNull Boolean completed) {
}
