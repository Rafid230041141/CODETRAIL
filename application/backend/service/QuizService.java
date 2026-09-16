package application.backend.service;

import application.backend.domain.Lesson;
import application.backend.domain.QuizAttempt;
import application.backend.domain.QuizQuestion;
import application.backend.domain.UserAccount;
import application.backend.dto.QuizAttemptRequest;
import application.backend.dto.QuizAttemptResult;
import application.backend.dto.QuizQuestionView;
import application.backend.repository.LessonRepository;
import application.backend.repository.QuizAttemptRepository;
import application.backend.repository.QuizQuestionRepository;
import application.backend.seed.QuizBankSeeder;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {
    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final LessonRepository lessons;
    private final TopicService topicService;
    private final ProgressService progressService;

    public QuizService(QuizQuestionRepository questions, QuizAttemptRepository attempts,
                       LessonRepository lessons, TopicService topicService, ProgressService progressService) {
        this.questions = questions;
        this.attempts = attempts;
        this.lessons = lessons;
        this.topicService = topicService;
        this.progressService = progressService;
    }

    @Transactional
    public List<QuizQuestionView> questions(Long lessonId) {
        Lesson lesson = lessons.findById(lessonId).orElse(null);
        if (lesson == null) {
            lesson = topicService.findPublishedLesson(lessonId);
        }
        List<QuizQuestion> quizQuestions = questions.findByLessonIdOrderByQuestionOrderAsc(lessonId);
        if (quizQuestions.isEmpty()) {
            ensureQuestionsForLesson(lesson);
            quizQuestions = questions.findByLessonIdOrderByQuestionOrderAsc(lessonId);
        }
        return quizQuestions.stream().map(q -> {
            List<String> options = new ArrayList<>();
            if (q.getOptionA() != null && !q.getOptionA().isBlank()) options.add(q.getOptionA());
            if (q.getOptionB() != null && !q.getOptionB().isBlank()) options.add(q.getOptionB());
            if (q.getOptionC() != null && !q.getOptionC().isBlank()) options.add(q.getOptionC());
            if (q.getOptionD() != null && !q.getOptionD().isBlank()) options.add(q.getOptionD());
            String prompt = (q.getPrompt() != null && !q.getPrompt().isBlank()) ? q.getPrompt() : "Question";
            return new QuizQuestionView(q.getId(), prompt, options);
        }).toList();
    }

    @Transactional
    public QuizAttemptResult attempt(UserAccount user, Long lessonId, QuizAttemptRequest request) {
        Lesson lesson = lessons.findById(lessonId).orElse(null);
        if (lesson == null) {
            lesson = topicService.findPublishedLesson(lessonId);
        }
        List<QuizQuestion> quizQuestions = questions.findByLessonIdOrderByQuestionOrderAsc(lessonId);
        if (quizQuestions.isEmpty()) {
            ensureQuestionsForLesson(lesson);
            quizQuestions = questions.findByLessonIdOrderByQuestionOrderAsc(lessonId);
        }
        List<Integer> answers = (request == null || request.answers() == null) ? List.of() : request.answers();
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
                String exp = (question.getExplanation() != null && !question.getExplanation().isBlank())
                        ? question.getExplanation() : "Incorrect answer";
                feedback.add("Question " + (i + 1) + ": " + exp);
            }
        }
        attempts.save(new QuizAttempt(user, lesson, score, quizQuestions.size()));
        if (score >= 4) {
            progressService.markLesson(user, lessonId, true);
        }
        return new QuizAttemptResult(score, quizQuestions.size(), feedback);
    }

    private void ensureQuestionsForLesson(Lesson lesson) {
        if (lesson == null) return;
        String topicTitle = "";
        String moduleTitle = "";
        String submoduleTitle = "";
        if (lesson.getSubmodule() != null) {
            submoduleTitle = lesson.getSubmodule().getTitle();
            if (lesson.getSubmodule().getModule() != null) {
                moduleTitle = lesson.getSubmodule().getModule().getTitle();
                if (lesson.getSubmodule().getModule().getTopic() != null) {
                    topicTitle = lesson.getSubmodule().getModule().getTopic().getTitle();
                }
            }
        }
        List<QuizBankSeeder.QuestionSpec> specs = QuizBankSeeder.getQuestionsForLesson(
                topicTitle, moduleTitle, submoduleTitle, lesson.getTitle());
        int order = 1;
        for (QuizBankSeeder.QuestionSpec spec : specs) {
            questions.save(new QuizQuestion(lesson, order++, spec.prompt(),
                    spec.a(), spec.b(), spec.c(), spec.d(), spec.correct(), spec.explanation()));
        }
    }
}
