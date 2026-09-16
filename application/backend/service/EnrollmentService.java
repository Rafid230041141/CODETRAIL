package application.backend.service;

import application.backend.domain.Enrollment;
import application.backend.domain.Topic;
import application.backend.domain.UserAccount;
import application.backend.dto.EnrollmentResponse;
import application.backend.repository.EnrollmentRepository;
import application.backend.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final TopicRepository topics;

    public EnrollmentService(EnrollmentRepository enrollments, TopicRepository topics) {
        this.enrollments = enrollments;
        this.topics = topics;
    }

    @Transactional
    public EnrollmentResponse enroll(UserAccount user, Long topicId) {
        Topic topic = topics.findById(topicId).filter(Topic::isPublished)
                .orElseThrow(() -> new NotFoundException("Published topic not found: " + topicId));
        if (!TopicService.PUBLISHED_TOPIC_SLUGS.contains(topic.getSlug())) {
            throw new NotFoundException("Topic not found: " + topicId);
        }
        Enrollment enrollment = enrollments.findByUserIdAndTopicId(user.getId(), topicId)
                .orElseGet(() -> enrollments.save(new Enrollment(user, topic)));
        return new EnrollmentResponse(enrollment.getId(), topic.getId(), enrollment.getEnrolledAt());
    }
}
