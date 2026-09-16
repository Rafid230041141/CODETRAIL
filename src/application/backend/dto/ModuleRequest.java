package application.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModuleRequest(@NotNull Long topicId, @NotBlank String title, String description, Integer position) {
}
