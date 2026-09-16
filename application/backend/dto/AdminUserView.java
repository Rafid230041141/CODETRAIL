package application.backend.dto;

import application.backend.domain.Role;

public record AdminUserView(Long id, String username, String displayName, Role role,
                            long completedLessons, long totalLessons, long quizAttempts) {
}
