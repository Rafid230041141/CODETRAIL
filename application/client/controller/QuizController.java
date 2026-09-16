package application.client.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import application.backend.repository.UserAccountRepository;
import application.backend.service.QuizService;
import application.client.dto.ApiModels.QuizAttemptResult;
import application.client.dto.ApiModels.QuizQuestionView;
import application.client.service.ApiClient;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class QuizController {
    private static final int QUIZ_DURATION_SECONDS = 300; // 5 minutes

    @FXML
    private Label quizLessonLabel;
    @FXML
    private Label quizTimerLabel;
    @FXML
    private Label quizProgressLabel;
    @FXML
    private Label questionLabel;
    @FXML
    private RadioButton optionA;
    @FXML
    private RadioButton optionB;
    @FXML
    private RadioButton optionC;
    @FXML
    private RadioButton optionD;
    @FXML
    private ToggleGroup answerGroup;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button actionButton;
    @FXML
    private VBox questionPane;
    @FXML
    private VBox resultPane;
    @FXML
    private Label resultStatusLabel;
    @FXML
    private Label resultScoreLabel;
    @FXML
    private VBox resultFeedbackBox;
    @FXML
    private Button retryButton;
    @FXML
    private Button closeButton;

    private final ApiClient apiClient;

    @Autowired(required = false)
    private QuizService fallbackQuizService;

    @Autowired(required = false)
    private UserAccountRepository fallbackUserRepo;

    private List<QuizQuestionView> questions = List.of();
    private final List<Integer> answers = new ArrayList<>();
    private long lessonId;
    private int questionIndex;
    private Stage stage;
    private Runnable onCompleted;

    private Timeline timer;
    private int remainingSeconds = QUIZ_DURATION_SECONDS;

    public QuizController(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void start(long lessonId, String lessonTitle, Stage stage, Runnable onCompleted) {
        this.lessonId = lessonId;
        this.stage = stage;
        this.onCompleted = onCompleted;
        if (stage != null) {
            stage.setOnCloseRequest(e -> stopTimer());
        }
        quizLessonLabel.setText(lessonTitle);
        questionPane.setVisible(false);
        questionPane.setManaged(false);
        resultPane.setVisible(false);
        resultPane.setManaged(false);

        loadQuestions();
    }

    private void loadQuestions() {
        actionButton.setDisable(true);
        actionButton.setText("Loading...");
        feedbackLabel.setText("Loading questions...");

        apiClient.quiz(lessonId).whenComplete((loaded, error) -> Platform.runLater(() -> {
            if (error == null && loaded != null && !loaded.isEmpty()) {
                questions = List.copyOf(loaded);
                actionButton.setDisable(false);
                actionButton.setText("Next question");
                feedbackLabel.setText("");
                initQuizState();
                return;
            }

            // Fallback to in-process QuizService if HTTP call failed or returned empty
            if (fallbackQuizService != null) {
                try {
                    var backendQuestions = fallbackQuizService.questions(lessonId);
                    if (backendQuestions != null && !backendQuestions.isEmpty()) {
                        questions = backendQuestions.stream()
                                .map(q -> new QuizQuestionView(q.id(), q.prompt(), q.options()))
                                .toList();
                        actionButton.setDisable(false);
                        actionButton.setText("Next question");
                        feedbackLabel.setText("");
                        initQuizState();
                        return;
                    }
                } catch (Exception ex) {
                    // In-process fallback error
                }
            }

            // If both failed, show error and let the user retry
            String msg = (error != null)
                    ? (error.getCause() == null ? error.getMessage() : error.getCause().getMessage())
                    : "No quiz questions found.";
            feedbackLabel.setText(msg);
            actionButton.setDisable(false);
            actionButton.setText("🔄 Retry");
        }));
    }

    private void initQuizState() {
        answers.clear();
        questionIndex = 0;
        questionPane.setVisible(true);
        questionPane.setManaged(true);
        resultPane.setVisible(false);
        resultPane.setManaged(false);
        startTimer();
        showQuestion();
    }

    private void startTimer() {
        stopTimer();
        remainingSeconds = QUIZ_DURATION_SECONDS;
        updateTimerDisplay();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateTimerDisplay();
            if (remainingSeconds <= 0) {
                stopTimer();
                handleTimeExpired();
            }
        }));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void updateTimerDisplay() {
        int minutes = Math.max(0, remainingSeconds / 60);
        int seconds = Math.max(0, remainingSeconds % 60);
        if (quizTimerLabel != null) {
            quizTimerLabel.setText(String.format("⏱ %02d:%02d", minutes, seconds));
            if (remainingSeconds <= 60) {
                quizTimerLabel.getStyleClass().remove("quiz-timer-normal");
            } else {
                if (!quizTimerLabel.getStyleClass().contains("quiz-timer-normal")) {
                    quizTimerLabel.getStyleClass().add("quiz-timer-normal");
                }
            }
        }
    }

    private void handleTimeExpired() {
        Toggle selected = answerGroup.getSelectedToggle();
        if (selected != null) {
            answers.add((Integer) selected.getUserData());
        }
        while (answers.size() < questions.size()) {
            answers.add(-1);
        }
        feedbackLabel.setText("Time's up! Submitting answers...");
        submit();
    }

    @FXML
    private void advance() {
        if (questions.isEmpty()) {
            loadQuestions();
            return;
        }
        Toggle selected = answerGroup.getSelectedToggle();
        if (selected == null) {
            feedbackLabel.setText("Choose one answer before continuing.");
            return;
        }
        answers.add((Integer) selected.getUserData());
        if (questionIndex < questions.size() - 1) {
            questionIndex++;
            showQuestion();
            return;
        }
        stopTimer();
        submit();
    }

    @FXML
    private void retryQuiz() {
        initQuizState();
    }

    @FXML
    private void close() {
        stopTimer();
        if (stage != null) {
            stage.close();
        }
    }

    private void showQuestion() {
        if (questions.isEmpty()) {
            feedbackLabel.setText("No quiz questions are available.");
            actionButton.setDisable(false);
            actionButton.setText("🔄 Retry");
            return;
        }
        questionPane.setVisible(true);
        questionPane.setManaged(true);
        answerGroup.selectToggle(null);
        feedbackLabel.setText("");
        QuizQuestionView question = questions.get(questionIndex);
        quizProgressLabel.setText("QUESTION " + (questionIndex + 1) + " OF " + questions.size());
        questionLabel.setText(question.prompt());
        List<RadioButton> buttons = options();
        List<String> opts = question.options() != null ? question.options() : List.of();
        for (int index = 0; index < buttons.size(); index++) {
            RadioButton btn = buttons.get(index);
            if (index < opts.size()) {
                btn.setText(opts.get(index));
                btn.setUserData(index);
                btn.setVisible(true);
                btn.setManaged(true);
            } else {
                btn.setText("");
                btn.setVisible(false);
                btn.setManaged(false);
            }
        }
        actionButton.setText(questionIndex == questions.size() - 1 ? "Submit quiz" : "Next question");
        actionButton.setDisable(false);
    }

    private void submit() {
        actionButton.setDisable(true);
        feedbackLabel.setText("Grading quiz...");
        apiClient.submitQuiz(lessonId, answers).whenComplete((result, error) -> Platform.runLater(() -> {
            if (error == null && result != null) {
                displayResult(result);
                return;
            }

            // Attempt in-process fallback
            if (fallbackQuizService != null && fallbackUserRepo != null && apiClient.getCurrentSession() != null) {
                try {
                    String username = apiClient.getCurrentSession().username();
                    var userOpt = fallbackUserRepo.findByUsernameIgnoreCase(username);
                    if (userOpt.isPresent()) {
                        var backendResult = fallbackQuizService.attempt(userOpt.get(), lessonId,
                                new application.backend.dto.QuizAttemptRequest(answers));
                        displayResult(new QuizAttemptResult(
                                backendResult.score(), backendResult.total(), backendResult.feedback()));
                        return;
                    }
                } catch (Exception ex) {
                    // Fallback failed
                }
            }

            actionButton.setDisable(false);
            actionButton.setText("Submit quiz");
            feedbackLabel.setText(error != null
                    ? (error.getCause() == null ? error.getMessage() : error.getCause().getMessage())
                    : "Grading failed");
        }));
    }

    private void displayResult(QuizAttemptResult result) {
        stopTimer();
        questionPane.setVisible(false);
        questionPane.setManaged(false);
        resultPane.setVisible(true);
        resultPane.setManaged(true);
        resultScoreLabel.setText(result.score() + " / " + result.total());
        boolean passed = result.score() >= 4;
        if (passed) {
            resultStatusLabel.setText("🎉 PASSED! (Score: " + result.score() + "/" + result.total() + " ≥ 4) — Lesson marked finished!");
            resultStatusLabel.getStyleClass().setAll("quiz-result-passed");
            if (retryButton != null) {
                retryButton.setVisible(false);
                retryButton.setManaged(false);
            }
        } else {
            resultStatusLabel.setText("❌ NOT PASSED (Score: " + result.score() + "/" + result.total() + " < 4) — Need at least 4/8 to complete.");
            resultStatusLabel.getStyleClass().setAll("quiz-result-failed");
            if (retryButton != null) {
                retryButton.setVisible(true);
                retryButton.setManaged(true);
            }
        }
        resultFeedbackBox.getChildren().clear();
        for (String feedback : result.feedback()) {
            Label label = new Label(feedback);
            label.setWrapText(true);
            label.getStyleClass().add("quiz-feedback-item");
            resultFeedbackBox.getChildren().add(label);
        }
        if (passed && onCompleted != null) {
            onCompleted.run();
        }
    }

    private List<RadioButton> options() {
        return List.of(optionA, optionB, optionC, optionD);
    }
}
