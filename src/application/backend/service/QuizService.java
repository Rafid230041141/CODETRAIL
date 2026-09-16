package application.backend.service;

import application.backend.domain.Lesson;
import application.backend.domain.QuizAttempt;
import application.backend.domain.QuizQuestion;
import application.backend.domain.UserAccount;
import application.backend.dto.QuizAttemptRequest;
import application.backend.dto.QuizAttemptResult;
import application.backend.dto.QuizQuestionView;
import application.backend.repository.QuizAttemptRepository;
import application.backend.repository.QuizQuestionRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {
    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final TopicService topicService;

    public QuizService(QuizQuestionRepository questions, QuizAttemptRepository attempts, TopicService topicService) {
        this.questions = questions;
        this.attempts = attempts;
        this.topicService = topicService;
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionView> questions(Long lessonId) {
        topicService.findPublishedLesson(lessonId);
        return questions.findByLessonIdOrderByQuestionOrderAsc(lessonId).stream()
                .map(q -> new QuizQuestionView(q.getId(), q.getPrompt(),
                        List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD())))
                .toList();
    }

    @Transactional
    public QuizAttemptResult attempt(UserAccount user, Long lessonId, QuizAttemptRequest request) {
        Lesson lesson = topicService.findPublishedLesson(lessonId);
        List<QuizQuestion> quizQuestions = questions.findByLessonIdOrderByQuestionOrderAsc(lessonId);
        List<Integer> answers = request.answers() == null ? List.of() : request.answers();
        int score = 0;
        List<String> feedback = new ArrayList<>();
        for (int i = 0; i < quizQuestions.size(); i++) {
            QuizQuestion question = quizQuestions.get(i);
            Integer answer = i < answers.size() ? answers.get(i) : null;
            boolean correct = answer != null && answer == question.getCorrectIndex();
            if (correct) {
                score++;
                feedback.add("Question " + (i + 1) + ": correct");
            } else {
                feedback.add("Question " + (i + 1) + ": " + question.getExplanation());
            }
        }
        attempts.save(new QuizAttempt(user, lesson, score, quizQuestions.size()));
        return new QuizAttemptResult(score, quizQuestions.size(), feedback);
    }
}
