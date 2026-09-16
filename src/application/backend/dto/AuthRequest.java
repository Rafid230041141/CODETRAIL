package application.backend.dto;

import application.backend.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(max = 160) String displayName,
        @NotBlank @Size(min = 8, max = 128) String password,
        Role role) {
}
