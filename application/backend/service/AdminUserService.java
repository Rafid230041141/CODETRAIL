package application.backend.service;

import application.backend.domain.UserAccount;
import application.backend.dto.AdminUserView;
import application.backend.repository.LessonProgressRepository;
import application.backend.repository.LessonRepository;
import application.backend.repository.QuizAttemptRepository;
import application.backend.repository.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final UserAccountRepository users;
    private final LessonProgressRepository progress;
    private final LessonRepository lessons;
    private final QuizAttemptRepository attempts;

    public AdminUserService(UserAccountRepository users, LessonProgressRepository progress, LessonRepository lessons,
                            QuizAttemptRepository attempts) {
        this.users = users;
        this.progress = progress;
        this.lessons = lessons;
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> users() {
        long totalLessons = lessons.count();
        return users.findAllByOrderByUsernameAsc().stream().map(user -> view(user, totalLessons)).toList();
    }

    private AdminUserView view(UserAccount user, long totalLessons) {
        return new AdminUserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                progress.countByUserIdAndCompletedTrue(user.getId()), totalLessons, attempts.countByUserId(user.getId()));
    }
}
