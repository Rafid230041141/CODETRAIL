package application.client.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import application.client.dto.ApiModels.QuizQuestionView;
import application.client.service.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class QuizController {
    @FXML
    private Label quizLessonLabel;
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
    private Label resultScoreLabel;
    @FXML
    private VBox resultFeedbackBox;

    private final ApiClient apiClient;
    private List<QuizQuestionView> questions = List.of();
    private final List<Integer> answers = new ArrayList<>();
    private long lessonId;
    private int questionIndex;
    private Stage stage;
    private Runnable onCompleted;

    public QuizController(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void start(long lessonId, String lessonTitle, Stage stage, Runnable onCompleted) {
        this.lessonId = lessonId;
        this.stage = stage;
        this.onCompleted = onCompleted;
        quizLessonLabel.setText(lessonTitle);
        actionButton.setDisable(true);
        feedbackLabel.setText("Loading questions...");
        apiClient.quiz(lessonId).whenComplete((loaded, error) -> Platform.runLater(() -> {
            if (error != null) {
                feedbackLabel.setText(error.getCause() == null ? error.getMessage() : error.getCause().getMessage());
                return;
            }
            questions = List.copyOf(loaded);
            actionButton.setDisable(false);
            showQuestion();
        }));
    }

    @FXML
    private void advance() {
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
        submit();
    }

    @FXML
    private void close() {
        if (stage != null) {
            stage.close();
        }
    }

    private void showQuestion() {
        if (questions.isEmpty()) {
            feedbackLabel.setText("No quiz questions are available.");
            actionButton.setDisable(true);
            return;
        }
        answerGroup.selectToggle(null);
        feedbackLabel.setText("");
        QuizQuestionView question = questions.get(questionIndex);
        quizProgressLabel.setText("QUESTION " + (questionIndex + 1) + " OF " + questions.size());
        questionLabel.setText(question.prompt());
        List<RadioButton> buttons = options();
        for (int index = 0; index < buttons.size(); index++) {
            buttons.get(index).setText(question.options().get(index));
            buttons.get(index).setUserData(index);
        }
        actionButton.setText(questionIndex == questions.size() - 1 ? "Submit quiz" : "Next question");
    }

    private void submit() {
        actionButton.setDisable(true);
        feedbackLabel.setText("Saving result...");
        apiClient.submitQuiz(lessonId, answers).whenComplete((result, error) -> Platform.runLater(() -> {
            if (error != null) {
                actionButton.setDisable(false);
                feedbackLabel.setText(error.getCause() == null ? error.getMessage() : error.getCause().getMessage());
                return;
            }
            questionPane.setVisible(false);
            questionPane.setManaged(false);
            resultPane.setVisible(true);
            resultPane.setManaged(true);
            resultScoreLabel.setText(result.score() + " / " + result.total());
            resultFeedbackBox.getChildren().clear();
            for (String feedback : result.feedback()) {
                Label label = new Label(feedback);
                label.setWrapText(true);
                label.getStyleClass().add("quiz-feedback-item");
                resultFeedbackBox.getChildren().add(label);
            }
            if (onCompleted != null) {
                onCompleted.run();
            }
        }));
    }

    private List<RadioButton> options() {
        return List.of(optionA, optionB, optionC, optionD);
    }
}
