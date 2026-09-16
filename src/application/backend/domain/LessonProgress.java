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
@Table(name = "lesson_progress", uniqueConstraints = @UniqueConstraint(name = "uk_progress_user_lesson", columnNames = {"user_id", "lesson_id"}))
public class LessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private Instant lastViewedAt;

    private Instant completedAt;

    protected LessonProgress() {
    }

    public LessonProgress(UserAccount user, Lesson lesson) {
        this.user = user;
        this.lesson = lesson;
    }

    @PrePersist
    void onCreate() {
        if (lastViewedAt == null) {
            lastViewedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public Lesson getLesson() { return lesson; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.completedAt = completed ? (completedAt == null ? Instant.now() : completedAt) : null;
    }
    public Instant getLastViewedAt() { return lastViewedAt; }
    public void touch() { this.lastViewedAt = Instant.now(); }
    public Instant getCompletedAt() { return completedAt; }
}
