package application.backend.web;

import application.backend.dto.EnrollmentResponse;
import application.backend.service.CurrentUserService;
import application.backend.service.EnrollmentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollments;
    private final CurrentUserService currentUser;

    public EnrollmentController(
            EnrollmentService enrollments,
            CurrentUserService currentUser) {
        this.enrollments = enrollments;
        this.currentUser = currentUser;
    }

    @PostMapping("/{topicId}")
    public EnrollmentResponse enroll(
            @PathVariable("topicId") Long topicId,
            Authentication authentication) {
        return enrollments.enroll(
                currentUser.require(authentication),
                topicId
        );
    }
}