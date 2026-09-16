package application.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmoduleRequest(@NotNull Long moduleId, @NotBlank String title, Integer position) {
}
