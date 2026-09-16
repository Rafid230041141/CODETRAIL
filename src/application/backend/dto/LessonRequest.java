package application.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LessonRequest(@NotNull Long submoduleId, @NotBlank String slug, @NotBlank String title,
                            @NotBlank String summary, @NotBlank String bodyMarkdown, String exampleCode,
                            Integer position, @NotNull Boolean published) {
}
