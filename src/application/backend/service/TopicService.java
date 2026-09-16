package application.backend.service;

import application.backend.domain.CourseModule;
import application.backend.domain.Lesson;
import application.backend.domain.Submodule;
import application.backend.domain.Topic;
import application.backend.dto.LessonDetails;
import application.backend.dto.LessonSummary;
import application.backend.dto.ModuleView;
import application.backend.dto.SimulationView;
import application.backend.dto.SubmoduleView;
import application.backend.dto.TopicSummary;
import application.backend.dto.TopicTree;
import application.backend.repository.LessonProgressRepository;
import application.backend.repository.LessonRepository;
import application.backend.repository.QuizQuestionRepository;
import application.backend.repository.TopicRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicService {
    public static final Set<String> PUBLISHED_TOPIC_SLUGS = Set.of(
            "languages",
            "dsa-competitive-programming",
            "web-development",
            "app-development",
            "ai-ml",
            "data-science",
            "game-development");

    private final TopicRepository topics;
    private final LessonRepository lessons;
    private final LessonProgressRepository progress;
    private final QuizQuestionRepository questions;

    public TopicService(TopicRepository topics, LessonRepository lessons, LessonProgressRepository progress,
                        QuizQuestionRepository questions) {
        this.topics = topics;
        this.lessons = lessons;
        this.progress = progress;
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public List<TopicSummary> publishedTopics() {
        return topics.findByPublishedTrueAndSlugInOrderByPositionAsc(PUBLISHED_TOPIC_SLUGS.stream().sorted().toList())
                .stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public TopicTree tree(Long topicId, Long userId) {
        Topic topic = topics.findById(topicId)
                .filter(Topic::isPublished)
                .orElseThrow(() -> new NotFoundException("Published topic not found: " + topicId));
        if (!PUBLISHED_TOPIC_SLUGS.contains(topic.getSlug())) {
            throw new NotFoundException("Topic not found: " + topicId);
        }
        Map<Long, Boolean> completed = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p.isCompleted(), (a, b) -> b));
        List<ModuleView> modules = topic.getModules().stream()
                .sorted(Comparator.comparingInt(CourseModule::getPosition))
                .map(module -> new ModuleView(module.getId(), module.getTitle(), module.getDescription(), module.getPosition(),
                        module.getSubmodules().stream().sorted(Comparator.comparingInt(Submodule::getPosition))
                                .map(submodule -> new SubmoduleView(submodule.getId(), submodule.getTitle(), submodule.getPosition(),
                                        submodule.getLessons().stream().filter(Lesson::isPublished)
                                                .sorted(Comparator.comparingInt(Lesson::getPosition))
                                                .map(lesson -> new LessonSummary(lesson.getId(), lesson.getSlug(), lesson.getTitle(),
                                                        lesson.getSummary(), lesson.getPosition(), completed.getOrDefault(lesson.getId(), false)))
                                                .toList()))
                                .toList()))
                .toList();
        return new TopicTree(topic.getId(), topic.getSlug(), topic.getTitle(), modules);
    }

    @Transactional(readOnly = true)
    public LessonDetails lesson(Long lessonId, Long userId) {
        Lesson lesson = findPublishedLesson(lessonId);
        Long topicId = lesson.getSubmodule().getModule().getTopic().getId();
        SimulationView simulation = lesson.getSimulation() == null ? null
                : new SimulationView(lesson.getSimulation().getId(), lesson.getSimulation().getType(), lesson.getSimulation().getConfigJson());
        boolean completed = progress.findByUserIdAndLessonId(userId, lessonId).map(p -> p.isCompleted()).orElse(false);
        return new LessonDetails(lesson.getId(), topicId, lesson.getTitle(), lesson.getSummary(), lesson.getBodyMarkdown(),
                lesson.getExampleCode(), completed, (int) questions.countByLessonId(lessonId), simulation);
    }

    public Lesson findPublishedLesson(Long lessonId) {
        Lesson lesson = lessons.findById(lessonId)
                .filter(Lesson::isPublished)
                .orElseThrow(() -> new NotFoundException("Published lesson not found: " + lessonId));
        if (!PUBLISHED_TOPIC_SLUGS.contains(lesson.getSubmodule().getModule().getTopic().getSlug())
                || !lesson.getSubmodule().getModule().getTopic().isPublished()) {
            throw new NotFoundException("Lesson not found: " + lessonId);
        }
        return lesson;
    }

    private TopicSummary summary(Topic topic) {
        return new TopicSummary(topic.getId(), topic.getSlug(), topic.getTitle(), topic.getDescription(), topic.getPosition());
    }
}
