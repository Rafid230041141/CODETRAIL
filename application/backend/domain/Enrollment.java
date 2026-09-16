package application.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(name = "uk_enrollment_user_topic", columnNames = {"user_id", "topic_id"}))
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false, updatable = false)
    private Instant enrolledAt;

    protected Enrollment() {
    }

    public Enrollment(UserAccount user, Topic topic) {
        this.user = user;
        this.topic = topic;
    }

    @PrePersist
    void onCreate() {
        if (enrolledAt == null) {
            enrolledAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public Topic getTopic() { return topic; }
    public Instant getEnrolledAt() { return enrolledAt; }
}
