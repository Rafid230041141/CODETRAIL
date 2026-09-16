package application.backend.dto;

public record AuthResponse(String token, String role, Long userId, String username, String displayName) {
}
