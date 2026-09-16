package application.backend.service;

import application.backend.domain.CourseModule;
import application.backend.domain.Lesson;
import application.backend.domain.Submodule;
import application.backend.domain.Topic;
import application.backend.dto.LessonRequest;
import application.backend.dto.ModuleRequest;
import application.backend.dto.SubmoduleRequest;
import application.backend.dto.TopicRequest;
import application.backend.repository.CourseModuleRepository;
import application.backend.repository.LessonRepository;
import application.backend.repository.SubmoduleRepository;
import application.backend.repository.TopicRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminContentService {
    private final TopicRepository topics;
    private final CourseModuleRepository modules;
    private final SubmoduleRepository submodules;
    private final LessonRepository lessons;

    public AdminContentService(TopicRepository topics, CourseModuleRepository modules,
                               SubmoduleRepository submodules, LessonRepository lessons) {
        this.topics = topics;
        this.modules = modules;
        this.submodules = submodules;
        this.lessons = lessons;
    }

    @Transactional(readOnly = true)
    public List<Topic> allTopics() {
        return topics.findAll(Sort.by("position"));
    }

    @Transactional(readOnly = true)
    public List<CourseModule> modules(Long topicId) {
        if (!topics.existsById(topicId)) throw new NotFoundException("Topic not found: " + topicId);
        return modules.findByTopicIdOrderByPositionAsc(topicId);
    }

    @Transactional(readOnly = true)
    public List<Submodule> submodules(Long moduleId) {
        if (!modules.existsById(moduleId)) throw new NotFoundException("Module not found: " + moduleId);
        return submodules.findByModuleIdOrderByPositionAsc(moduleId);
    }

    @Transactional(readOnly = true)
    public List<Lesson> lessons(Long submoduleId) {
        if (!submodules.existsById(submoduleId)) throw new NotFoundException("Submodule not found: " + submoduleId);
        return lessons.findBySubmoduleIdOrderByPositionAsc(submoduleId);
    }

    @Transactional
    public Topic createTopic(TopicRequest request) {
        topics.findBySlugIgnoreCase(request.slug()).ifPresent(existing -> {
            throw new ConflictException("Topic slug is already in use");
        });
        return topics.saveAndFlush(new Topic(request.slug().trim(), request.title().trim(), request.description(),
                request.position() == null ? 0 : request.position(), request.published()));
    }

    @Transactional
    public Topic updateTopic(Long id, TopicRequest request) {
        Topic topic = topics.findById(id).orElseThrow(() -> new NotFoundException("Topic not found: " + id));
        topics.findBySlugIgnoreCase(request.slug()).filter(other -> !other.getId().equals(id)).ifPresent(existing -> {
            throw new ConflictException("Topic slug is already in use");
        });
        topic.setSlug(request.slug().trim());
        topic.setTitle(request.title().trim());
        topic.setDescription(request.description());
        topic.setPosition(request.position() == null ? topic.getPosition() : request.position());
        topic.setPublished(request.published());
        return topics.saveAndFlush(topic);
    }

    @Transactional
    public void deleteTopic(Long id) {
        if (!topics.existsById(id)) throw new NotFoundException("Topic not found: " + id);
        topics.deleteById(id);
    }

    @Transactional
    public CourseModule createModule(ModuleRequest request) {
        Topic topic = topics.findById(request.topicId()).orElseThrow(() -> new NotFoundException("Topic not found: " + request.topicId()));
        return modules.saveAndFlush(new CourseModule(topic, request.title().trim(), request.description(),
                request.position() == null ? 0 : request.position()));
    }

    @Transactional
    public CourseModule updateModule(Long id, ModuleRequest request) {
        CourseModule module = modules.findById(id).orElseThrow(() -> new NotFoundException("Module not found: " + id));
        Topic topic = topics.findById(request.topicId()).orElseThrow(() -> new NotFoundException("Topic not found: " + request.topicId()));
        module.setTopic(topic);
        module.setTitle(request.title().trim());
        module.setDescription(request.description());
        if (request.position() != null) module.setPosition(request.position());
        return modules.saveAndFlush(module);
    }

    @Transactional
    public void deleteModule(Long id) {
        if (!modules.existsById(id)) throw new NotFoundException("Module not found: " + id);
        modules.deleteById(id);
    }

    @Transactional
    public Submodule createSubmodule(SubmoduleRequest request) {
        CourseModule module = modules.findById(request.moduleId()).orElseThrow(() -> new NotFoundException("Module not found: " + request.moduleId()));
        return submodules.saveAndFlush(new Submodule(module, request.title().trim(), request.position() == null ? 0 : request.position()));
    }

    @Transactional
    public Submodule updateSubmodule(Long id, SubmoduleRequest request) {
        Submodule submodule = submodules.findById(id).orElseThrow(() -> new NotFoundException("Submodule not found: " + id));
        CourseModule module = modules.findById(request.moduleId()).orElseThrow(() -> new NotFoundException("Module not found: " + request.moduleId()));
        submodule.setModule(module);
        submodule.setTitle(request.title().trim());
        if (request.position() != null) submodule.setPosition(request.position());
        return submodules.saveAndFlush(submodule);
    }

    @Transactional
    public void deleteSubmodule(Long id) {
        if (!submodules.existsById(id)) throw new NotFoundException("Submodule not found: " + id);
        submodules.deleteById(id);
    }

    @Transactional
    public Lesson createLesson(LessonRequest request) {
        if (lessons.findAll().stream().anyMatch(existing -> existing.getSlug().equalsIgnoreCase(request.slug().trim()))) {
            throw new ConflictException("Lesson slug is already in use");
        }
        Submodule submodule = submodules.findById(request.submoduleId()).orElseThrow(() -> new NotFoundException("Submodule not found: " + request.submoduleId()));
        return lessons.saveAndFlush(new Lesson(submodule, request.slug().trim(), request.title().trim(), request.summary().trim(),
                request.bodyMarkdown(), request.exampleCode() == null ? "" : request.exampleCode(),
                request.position() == null ? 0 : request.position(), request.published()));
    }

    @Transactional
    public Lesson updateLesson(Long id, LessonRequest request) {
        Lesson lesson = lessons.findById(id).orElseThrow(() -> new NotFoundException("Lesson not found: " + id));
        if (lessons.findAll().stream().anyMatch(existing -> !existing.getId().equals(id)
                && existing.getSlug().equalsIgnoreCase(request.slug().trim()))) {
            throw new ConflictException("Lesson slug is already in use");
        }
        Submodule submodule = submodules.findById(request.submoduleId()).orElseThrow(() -> new NotFoundException("Submodule not found: " + request.submoduleId()));
        lesson.setSubmodule(submodule);
        lesson.setSlug(request.slug().trim());
        lesson.setTitle(request.title().trim());
        lesson.setSummary(request.summary().trim());
        lesson.setBodyMarkdown(request.bodyMarkdown());
        lesson.setExampleCode(request.exampleCode() == null ? "" : request.exampleCode());
        if (request.position() != null) lesson.setPosition(request.position());
        lesson.setPublished(request.published());
        return lessons.saveAndFlush(lesson);
    }

    @Transactional
    public void deleteLesson(Long id) {
        if (!lessons.existsById(id)) throw new NotFoundException("Lesson not found: " + id);
        lessons.deleteById(id);
    }

    public Map<String, Object> idResponse(Long id) {
        return Map.of("id", id);
    }
}
