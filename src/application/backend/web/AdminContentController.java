package application.backend.web;

import application.backend.domain.CourseModule;
import application.backend.domain.Lesson;
import application.backend.domain.Submodule;
import application.backend.domain.Topic;
import application.backend.dto.LessonRequest;
import application.backend.dto.AdminContentViews.LessonItem;
import application.backend.dto.AdminContentViews.ModuleItem;
import application.backend.dto.AdminContentViews.SubmoduleItem;
import application.backend.dto.AdminContentViews.TopicItem;
import application.backend.dto.ModuleRequest;
import application.backend.dto.SubmoduleRequest;
import application.backend.dto.TopicRequest;
import application.backend.dto.TopicSummary;
import application.backend.service.AdminContentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {
    private final AdminContentService content;

    public AdminContentController(AdminContentService content) {
        this.content = content;
    }

    @GetMapping("/topics")
    public List<TopicItem> topics() {
        return content.allTopics().stream()
                .map(topic -> new TopicItem(topic.getId(), topic.getSlug(), topic.getTitle(), topic.getDescription(),
                        topic.getPosition(), topic.isPublished()))
                .toList();
    }

    @GetMapping("/modules")
    public List<ModuleItem> modules(@RequestParam Long topicId) {
        return content.modules(topicId).stream()
                .map(module -> new ModuleItem(module.getId(), module.getTopic().getId(), module.getTitle(),
                        module.getDescription(), module.getPosition()))
                .toList();
    }

    @GetMapping("/submodules")
    public List<SubmoduleItem> submodules(@RequestParam Long moduleId) {
        return content.submodules(moduleId).stream()
                .map(submodule -> new SubmoduleItem(submodule.getId(), submodule.getModule().getId(),
                        submodule.getTitle(), submodule.getPosition()))
                .toList();
    }

    @GetMapping("/lessons")
    public List<LessonItem> lessons(@RequestParam Long submoduleId) {
        return content.lessons(submoduleId).stream()
                .map(lesson -> new LessonItem(lesson.getId(), lesson.getSubmodule().getId(), lesson.getSlug(),
                        lesson.getTitle(), lesson.getSummary(), lesson.getPosition(), lesson.isPublished()))
                .toList();
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicSummary> createTopic(@Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicSummary(content.createTopic(request)));
    }

    @PutMapping("/topics/{id}")
    public TopicSummary updateTopic(@PathVariable Long id, @Valid @RequestBody TopicRequest request) {
        return topicSummary(content.updateTopic(id, request));
    }

    @DeleteMapping("/topics/{id}")
    public Map<String, Object> deleteTopic(@PathVariable Long id) {
        content.deleteTopic(id);
        return Map.of("deleted", true);
    }

    @PostMapping("/modules")
    public ResponseEntity<Map<String, Object>> createModule(@Valid @RequestBody ModuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(content.idResponse(content.createModule(request).getId()));
    }

    @PutMapping("/modules/{id}")
    public Map<String, Object> updateModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        return content.idResponse(content.updateModule(id, request).getId());
    }

    @DeleteMapping("/modules/{id}")
    public Map<String, Object> deleteModule(@PathVariable Long id) {
        content.deleteModule(id);
        return Map.of("deleted", true);
    }

    @PostMapping("/submodules")
    public ResponseEntity<Map<String, Object>> createSubmodule(@Valid @RequestBody SubmoduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(content.idResponse(content.createSubmodule(request).getId()));
    }

    @PutMapping("/submodules/{id}")
    public Map<String, Object> updateSubmodule(@PathVariable Long id, @Valid @RequestBody SubmoduleRequest request) {
        return content.idResponse(content.updateSubmodule(id, request).getId());
    }

    @DeleteMapping("/submodules/{id}")
    public Map<String, Object> deleteSubmodule(@PathVariable Long id) {
        content.deleteSubmodule(id);
        return Map.of("deleted", true);
    }

    @PostMapping("/lessons")
    public ResponseEntity<Map<String, Object>> createLesson(@Valid @RequestBody LessonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(content.idResponse(content.createLesson(request).getId()));
    }

    @PutMapping("/lessons/{id}")
    public Map<String, Object> updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return content.idResponse(content.updateLesson(id, request).getId());
    }

    @DeleteMapping("/lessons/{id}")
    public Map<String, Object> deleteLesson(@PathVariable Long id) {
        content.deleteLesson(id);
        return Map.of("deleted", true);
    }

    private TopicSummary topicSummary(Topic topic) {
        return new TopicSummary(topic.getId(), topic.getSlug(), topic.getTitle(), topic.getDescription(), topic.getPosition());
    }
}
