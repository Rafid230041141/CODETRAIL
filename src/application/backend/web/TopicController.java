package application.backend.web;

import application.backend.dto.LessonDetails;
import application.backend.dto.TopicSummary;
import application.backend.dto.TopicTree;
import application.backend.service.CurrentUserService;
import application.backend.service.TopicService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TopicController {
    private final TopicService topics;
    private final CurrentUserService currentUser;

    public TopicController(TopicService topics, CurrentUserService currentUser) {
        this.topics = topics;
        this.currentUser = currentUser;
    }

    @GetMapping("/topics")
    public List<TopicSummary> topics() {
        return topics.publishedTopics();
    }

    @GetMapping("/topics/{topicId}/tree")
    public TopicTree tree(@PathVariable("topicId") Long topicId, Authentication authentication) {
        return topics.tree(topicId, currentUser.require(authentication).getId());
    }

    @GetMapping("/lessons/{lessonId}")
    public LessonDetails lesson(@PathVariable("lessonId") Long lessonId, Authentication authentication) {
        return topics.lesson(lessonId, currentUser.require(authentication).getId());
    }
}
