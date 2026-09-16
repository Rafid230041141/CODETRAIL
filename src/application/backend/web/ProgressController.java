package application.backend.web;

import application.backend.dto.LessonProgressRequest;
import application.backend.dto.ProgressSummary;
import application.backend.service.CurrentUserService;
import application.backend.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {
    private final ProgressService progress;
    private final CurrentUserService currentUser;

    public ProgressController(ProgressService progress, CurrentUserService currentUser) {
        this.progress = progress;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public ProgressSummary me(Authentication authentication) {
        return progress.summary(currentUser.require(authentication));
    }

    @PutMapping("/lessons/{lessonId}")
    public ProgressSummary lesson(@PathVariable Long lessonId, @Valid @RequestBody LessonProgressRequest request,
                                  Authentication authentication) {
        return progress.markLesson(currentUser.require(authentication), lessonId, request.completed());
    }
}
