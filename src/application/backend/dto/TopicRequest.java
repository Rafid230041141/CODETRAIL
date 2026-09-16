package application.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TopicRequest(@NotBlank String slug, @NotBlank String title, String description,
                           Integer position, @NotNull Boolean published) {
}
