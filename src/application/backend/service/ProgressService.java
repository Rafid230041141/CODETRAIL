package application.backend.service;

import application.backend.domain.Lesson;
import application.backend.domain.LessonProgress;
import application.backend.domain.Topic;
import application.backend.domain.UserAccount;
import application.backend.dto.ProgressSummary;
import application.backend.dto.TopicProgress;
import application.backend.repository.LessonProgressRepository;
import application.backend.repository.LessonRepository;
import application.backend.repository.QuizAttemptRepository;
import application.backend.repository.TopicRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressService {
    private final LessonProgressRepository progress;
    private final LessonRepository lessons;
    private final QuizAttemptRepository attempts;
    private final TopicRepository topics;
    private final TopicService topicService;

    public ProgressService(LessonProgressRepository progress, LessonRepository lessons,
                           QuizAttemptRepository attempts, TopicRepository topics, TopicService topicService) {
        this.progress = progress;
        this.lessons = lessons;
        this.attempts = attempts;
        this.topics = topics;
        this.topicService = topicService;
    }

    @Transactional
    public ProgressSummary markLesson(UserAccount user, Long lessonId, boolean completed) {
        Lesson lesson = topicService.findPublishedLesson(lessonId);
        LessonProgress value = progress.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseGet(() -> new LessonProgress(user, lesson));
        value.setCompleted(completed);
        value.touch();
        progress.save(value);
        return summary(user);
    }

    @Transactional(readOnly = true)
    public ProgressSummary summary(UserAccount user) {
        List<TopicProgress> topicProgress = new ArrayList<>();
        long total = 0;
        long completed = 0;
        List<Topic> publishedTopics = topics.findByPublishedTrueAndSlugInOrderByPositionAsc(
                TopicService.PUBLISHED_TOPIC_SLUGS.stream().sorted().toList());
        for (Topic topic : publishedTopics) {
            long topicTotal = lessons.countBySubmoduleModuleTopicIdAndPublishedTrue(topic.getId());
            long topicCompleted = progress.countByUserIdAndLessonSubmoduleModuleTopicIdAndCompletedTrue(user.getId(), topic.getId());
            topicProgress.add(new TopicProgress(topic.getId(), topic.getTitle(), topicCompleted, topicTotal));
            total += topicTotal;
            completed += topicCompleted;
        }
        return new ProgressSummary(completed, total, attempts.countByUserId(user.getId()), topicProgress);
    }
}
