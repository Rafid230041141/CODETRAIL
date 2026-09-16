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
import java.time.Instant;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
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
    private int score;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false, updatable = false)
    private Instant completedAt;

    protected QuizAttempt() {
    }

    public QuizAttempt(UserAccount user, Lesson lesson, int score, int total) {
        this.user = user;
        this.lesson = lesson;
        this.score = score;
        this.total = total;
    }

    @PrePersist
    void onCreate() {
        if (completedAt == null) {
            completedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public Lesson getLesson() { return lesson; }
    public int getScore() { return score; }
    public int getTotal() { return total; }
    public Instant getCompletedAt() { return completedAt; }
}
