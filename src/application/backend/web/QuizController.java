package application.backend.web;

import application.backend.dto.QuizAttemptRequest;
import application.backend.dto.QuizAttemptResult;
import application.backend.dto.QuizQuestionView;
import application.backend.service.CurrentUserService;
import application.backend.service.QuizService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("backendQuizController")
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizService quizzes;
    private final CurrentUserService currentUser;

    public QuizController(QuizService quizzes, CurrentUserService currentUser) {
        this.quizzes = quizzes;
        this.currentUser = currentUser;
    }

    @GetMapping("/{lessonId}")
    public List<QuizQuestionView> questions(@PathVariable Long lessonId) {
        return quizzes.questions(lessonId);
    }

    @PostMapping("/{lessonId}/attempts")
    public QuizAttemptResult attempt(@PathVariable Long lessonId, @Valid @RequestBody QuizAttemptRequest request,
                                     Authentication authentication) {
        return quizzes.attempt(currentUser.require(authentication), lessonId, request);
    }
}
