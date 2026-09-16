package application.backend.dto;

import java.time.Instant;

public record EnrollmentResponse(Long id, Long topicId, Instant enrolledAt) {
}
