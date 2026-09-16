package application.client.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import application.client.dto.ApiModels.AdminUserView;
import application.client.dto.ApiModels.AuthResponse;
import application.client.dto.ApiModels.LessonDetails;
import application.client.dto.ApiModels.LessonSummary;
import application.client.dto.ApiModels.ModuleView;
import application.client.dto.ApiModels.ProgressSummary;
import application.client.dto.ApiModels.SubmoduleView;
import application.client.dto.ApiModels.TopicProgress;
import application.client.dto.ApiModels.TopicSummary;
import application.client.dto.ApiModels.TopicTree;
import application.client.service.ApiClient;
import application.client.util.MarkdownRenderer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Separator;
import java.util.Locale;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import application.client.view.WebDevTopicsWindow;
import application.client.view.AppDevTopicsWindow;
import application.client.view.DsaTopicsWindow;
import application.client.view.LanguagesTopicsWindow;
import application.client.view.AiMlTopicsWindow;
import application.client.view.DataScienceTopicsWindow;
import application.client.view.GameDevTopicsWindow;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class AppController {
    @FXML
    private StackPane appRoot;
    @FXML
    private StackPane loginView;
    @FXML
    private StackPane studentView;
    @FXML
    private StackPane adminView;
    @FXML
    private Label authTitleLabel;
    @FXML
    private Label authSubtitleLabel;
    @FXML
    private TextField displayNameField;
    @FXML
    private StackPane usernameFieldShell;
    @FXML
    private TextField usernameField;
    @FXML
    private StackPane passwordFieldShell;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordTextField;
    @FXML
    private ToggleButton passwordVisibilityToggle;
    @FXML
    private javafx.scene.image.ImageView passwordVisibilityIcon;
    @FXML
    private ComboBox<String> roleSelector;
    @FXML
    private Label authMessageLabel;
    @FXML
    private Button authPrimaryButton;
    @FXML
    private Button authModeButton;
    @FXML
    private javafx.scene.layout.HBox forgotPasswordRow;
    @FXML
    private Button forgotPasswordButton;
    @FXML
    private Button googleAuthButton;
    @FXML
    private Label googleAuthLabel;
    @FXML
    private ToggleButton loginThemeToggle;
    @FXML
    private ToggleButton studentThemeToggle;
    @FXML
    private ToggleButton adminThemeToggle;
    @FXML
    private javafx.scene.image.ImageView loginThemeIcon;
    @FXML
    private javafx.scene.image.ImageView studentThemeIcon;
    @FXML
    private javafx.scene.image.ImageView adminThemeIcon;
    @FXML
    private Label brandTitleLabel;
    @FXML
    private Label brandTitleAccentLabel;
    @FXML
    private Label brandSubtitleLabel;
    @FXML
    private Label studentNameLabel;
    @FXML
    private Button studentProfileButton;
    @FXML
    private javafx.scene.image.ImageView studentProfileIcon;
    @FXML
    private javafx.scene.image.ImageView profileAvatarIcon;
    @FXML
    private TreeView<NavigationItem> curriculumTree;
    @FXML
    private VBox continueSidebarSection;
    @FXML
    private Label continueTrackLabel;
    @FXML
    private VBox continueItemsBox;
    @FXML
    private StackPane studentContent;
    @FXML
    private ScrollPane dashboardPane;
    @FXML
    private ScrollPane lessonPane;
    @FXML
    private ScrollPane profilePane;
    @FXML
    private Pane topicDrawerScrim;
    @FXML
    private VBox topicDrawerPane;
    @FXML
    private javafx.scene.image.ImageView topicDrawerImage;
    @FXML
    private Label topicDrawerTitle;
    @FXML
    private Label topicDrawerLessonsBadge;
    @FXML
    private Label topicDrawerModulesBadge;
    @FXML
    private Label topicDrawerWhatTaught;
    @FXML
    private Label topicDrawerRealWorld;
    @FXML
    private Button topicDrawerStartButton;
    @FXML
    private Label profileAvatarInitial;
    @FXML
    private Label profileDisplayNameLabel;
    @FXML
    private Label profileUsernameLabel;
    @FXML
    private Label profileCompletedLessonsLabel;
    @FXML
    private Label profileProgressPercentLabel;
    @FXML
    private ProgressBar profileProgressBar;
    @FXML
    private Label profileQuizAttemptsLabel;
    @FXML
    private Label profileCoursesCountLabel;
    @FXML
    private Label profileDetailDisplayName;
    @FXML
    private Label profileDetailUsername;
    @FXML
    private Label profileDetailRole;
    @FXML
    private javafx.scene.layout.FlowPane profileTopicProgressBox;
    @FXML
    private javafx.scene.layout.FlowPane topicCards;
    @FXML
    private Label dashboardProgressLabel;
    @FXML
    private StackPane overallProgressChart;
    @FXML
    private javafx.scene.layout.FlowPane topicProgressBox;
    @FXML
    private Label lessonPathLabel;
    @FXML
    private Label lessonTitleLabel;
    @FXML
    private Label lessonSummaryLabel;
    @FXML
    private VBox markdownContent;
    @FXML
    private TextArea lessonExampleArea;
    @FXML
    private CheckBox lessonCompletedCheckBox;
    @FXML
    private Button quizButton;
    @FXML
    private Button lessonWatchVideoBtn;
    @FXML
    private Button lessonCopyVideoBtn;
    private String currentLessonVideoUrl;

    @FXML
    private VBox lessonVideoContainer;
    @FXML
    private Label lessonVideoTitleLabel;
    @FXML
    private Button lessonVideoPlayBtn;
    @FXML
    private Button lessonVideoStopBtn;
    @FXML
    private Button lessonVideoRewindBtn;
    @FXML
    private Button lessonVideoForwardBtn;
    @FXML
    private Button lessonVideoCopyLinkBtn;
    @FXML
    private StackPane embeddedWebPlayerPane;
    @FXML
    private javafx.scene.image.ImageView lessonVideoPoster;
    @FXML
    private Region lessonPosterOverlay;
    @FXML
    private VBox lessonPlayPromptBox;
    @FXML
    private StackPane lessonPlayCircleBtn;

    private WebView lessonWebView;
    @FXML
    private VBox simulationSection;
    @FXML
    private SimulationController simulationContentController;
    @FXML
    private Label studentStatusLabel;
    @FXML
    private Label adminNameLabel;
    @FXML
    private Label adminStatusLabel;
    @FXML
    private TableView<AdminUserView> usersTable;
    @FXML
    private TableColumn<AdminUserView, String> usernameColumn;
    @FXML
    private TableColumn<AdminUserView, String> displayNameColumn;
    @FXML
    private TableColumn<AdminUserView, String> roleColumn;
    @FXML
    private TableColumn<AdminUserView, String> progressColumn;
    @FXML
    private TableColumn<AdminUserView, String> quizAttemptsColumn;

    private final ApiClient apiClient;
    private final ApplicationContext applicationContext;
    private boolean registrationMode;
    private boolean updatingCompletion;
    private LessonDetails currentLesson;
    private List<TopicSummary> topics = List.of();
    private TopicSummary selectedDrawerTopic;
    private List<TopicTree> currentTopicTrees = List.of();

    public AppController(ApiClient apiClient, ApplicationContext applicationContext) {
        this.apiClient = apiClient;
        this.applicationContext = applicationContext;
    }

    @FXML
    private void initialize() {
        roleSelector.setItems(FXCollections.observableArrayList("STUDENT", "ADMIN"));
        roleSelector.getSelectionModel().select("STUDENT");
        curriculumTree.setShowRoot(false);
        curriculumTree.setCellFactory(tree -> new TreeCell<>() {
            @Override
            protected void updateItem(NavigationItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        curriculumTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldItem, selected) -> openNavigationItem(selected));
        lessonCompletedCheckBox.selectedProperty().addListener(
                (observable, oldValue, completed) -> saveCompletion(completed));

        usernameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().username()));
        displayNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().displayName()));
        roleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().role()));
        progressColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().completedLessons() + " / " + cell.getValue().totalLessons()));
        quizAttemptsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                Integer.toString(cell.getValue().quizAttempts())));
        // "Code" and "Trail" are two separate labels so "Trail" can be
        // tinted with the brand blue.
        brandTitleLabel.setText("Code");
        brandTitleAccentLabel.setText("Trail");
        brandSubtitleLabel.setText("LEARNING PLATFORM");
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        linkFieldShellFocus(usernameFieldShell, usernameField);
        linkFieldShellFocus(passwordFieldShell, passwordField, passwordTextField);
        javafx.scene.control.Tooltip initialTooltip = new javafx.scene.control.Tooltip("Switch to dark mode");
        for (ToggleButton toggle : List.of(loginThemeToggle, studentThemeToggle, adminThemeToggle)) {
            javafx.scene.control.Tooltip.install(toggle, initialTooltip);
        }
        topicCards.widthProperty().addListener((obs, oldVal, newVal) -> updateTopicCardWidths());
        topicCards.prefWrapLengthProperty().bind(topicCards.widthProperty());
        if (profileTopicProgressBox != null) {
            profileTopicProgressBox.widthProperty().addListener((obs, oldVal, newVal) -> updateProfileCardWidths());
            profileTopicProgressBox.prefWrapLengthProperty().bind(profileTopicProgressBox.widthProperty());
        }
        showAuthMode(false);
        showOnly(loginView);
    }

    private static final String MOON_DARK = "/resources/images/icon-moon-dark.png";
    private static final String SUN_LIGHT = "/resources/images/icon-sun-light.png";

    @FXML
    private void submitAuth() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            showAuthError("Enter a username and password.");
            return;
        }
        if (registrationMode && displayNameField.getText().isBlank()) {
            showAuthError("Enter your display name.");
            return;
        }

        setAuthBusy(true);
        CompletableFuture<AuthResponse> request = registrationMode
                ? apiClient.register(username, displayNameField.getText().trim(), password, roleSelector.getValue())
                : apiClient.login(username, password);
        request.whenComplete((response, error) -> Platform.runLater(() -> {
            setAuthBusy(false);
            if (error != null) {
                showAuthError(readableError(error));
                return;
            }
            apiClient.useSession(response);
            usernameField.clear();
            passwordField.clear();
            displayNameField.clear();
            if ("ADMIN".equalsIgnoreCase(response.role())) {
                openAdmin(response);
            } else {
                openStudent(response);
            }
        }));
    }

    @FXML
    private void toggleAuthMode() {
        showAuthMode(!registrationMode);
    }

    private AuthResponse currentSession;

    @FXML
    private void logout() {
        stopLessonVideo();
        simulationContentController.stop();
        apiClient.clearSession();
        closeTopicDrawer();
        currentLesson = null;
        currentSession = null;
        authMessageLabel.setText("");
        setVisibleManaged(profilePane, false);
        showOnly(loginView);
    }

    @FXML
    private void showDashboard() {
        stopLessonVideo();
        closeTopicDrawer();
        setVisibleManaged(dashboardPane, true);
        setVisibleManaged(lessonPane, false);
        setVisibleManaged(profilePane, false);
        refreshProgress();
    }

    @FXML
    private void showProfile() {
        stopLessonVideo();
        closeTopicDrawer();
        setVisibleManaged(dashboardPane, false);
        setVisibleManaged(lessonPane, false);
        setVisibleManaged(profilePane, true);
        if (currentSession != null) {
            updateProfileView(currentSession);
        }
        refreshProgress();
    }

    private void updateProfileView(AuthResponse session) {
        if (session == null) {
            return;
        }
        if (profileDisplayNameLabel != null) {
            profileDisplayNameLabel.setText(session.displayName());
        }
        if (profileUsernameLabel != null) {
            profileUsernameLabel.setText("@" + session.username());
        }
        if (profileDetailDisplayName != null) {
            profileDetailDisplayName.setText(session.displayName());
        }
        if (profileDetailUsername != null) {
            profileDetailUsername.setText(session.username());
        }
        if (profileDetailRole != null) {
            profileDetailRole.setText(session.role());
        }
        if (profileAvatarInitial != null) {
            String initial = (session.displayName() != null && !session.displayName().isBlank())
                    ? session.displayName().substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                    : "S";
            profileAvatarInitial.setText(initial);
        }
    }

    @FXML
    private void refreshAdminUsers() {
        setAdminStatus("Refreshing users...");
        apiClient.adminUsers().whenComplete((users, error) -> Platform.runLater(() -> {
            if (error != null) {
                setAdminStatus(readableError(error));
                return;
            }
            usersTable.setItems(FXCollections.observableArrayList(users));
            setAdminStatus(users.size() + " registered users");
        }));
    }

    @FXML
    private void openQuiz() {
        if (currentLesson == null || currentLesson.quizQuestionCount() == 0) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/QuizView.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            QuizController controller = loader.getController();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(appRoot.getScene().getWindow());
            dialog.setTitle(currentLesson.title() + " Quiz");
            Scene scene = new Scene(root, 620, 520);
            scene.getStylesheets().add(getClass().getResource("/resources/css/application.css").toExternalForm());
            if (appRoot.getStyleClass().contains("dark-theme")) {
                root.getStyleClass().add("dark-theme");
            }
            dialog.setScene(scene);
            controller.start(currentLesson.id(), currentLesson.title(), dialog, this::refreshProgress);
            dialog.showAndWait();
        } catch (IOException exception) {
            studentStatusLabel.setText("Quiz could not open: " + exception.getMessage());
        }
    }

    @FXML
    private void toggleTheme() {
        boolean dark = !appRoot.getStyleClass().contains("dark-theme");
        if (dark) {
            appRoot.getStyleClass().add("dark-theme");
        } else {
            appRoot.getStyleClass().remove("dark-theme");
        }
        for (ToggleButton toggle : List.of(loginThemeToggle, studentThemeToggle, adminThemeToggle)) {
            toggle.setSelected(dark);
        }
        String iconPath = dark ? SUN_LIGHT : MOON_DARK;
        javafx.scene.image.Image icon = new javafx.scene.image.Image(
                getClass().getResource(iconPath).toExternalForm());
        for (javafx.scene.image.ImageView view : List.of(loginThemeIcon, studentThemeIcon, adminThemeIcon)) {
            view.setImage(icon);
        }
        updateAccountIcon(dark);
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                dark ? "Switch to light mode" : "Switch to dark mode");
        for (ToggleButton toggle : List.of(loginThemeToggle, studentThemeToggle, adminThemeToggle)) {
            javafx.scene.control.Tooltip.install(toggle, tooltip);
        }
    }

    private void updateAccountIcon(boolean dark) {
        String accountIconPath = dark ? "/resources/images/icon-user-account-dark.png" : "/resources/images/icon-user-account.png";
        javafx.scene.image.Image accountIcon = new javafx.scene.image.Image(
                getClass().getResource(accountIconPath).toExternalForm());
        if (studentProfileIcon != null) {
            studentProfileIcon.setImage(accountIcon);
        }
        if (profileAvatarIcon != null) {
            profileAvatarIcon.setImage(accountIcon);
        }
    }

    public void shutdown() {
        simulationContentController.stop();
    }

    private void openStudent(AuthResponse session) {
        this.currentSession = session;
        studentNameLabel.setText(session.displayName());
        updateAccountIcon(appRoot.getStyleClass().contains("dark-theme"));
        updateProfileView(session);
        showOnly(studentView);
        setStudentStatus("Loading curriculum...");
        apiClient.topics()
                .thenCompose(loadedTopics -> {
                    topics = List.copyOf(loadedTopics);
                    List<CompletableFuture<TopicTree>> requests = loadedTopics.stream()
                            .map(topic -> apiClient.topicTree(topic.id()))
                            .toList();
                    return CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new))
                            .thenApply(ignored -> requests.stream().map(CompletableFuture::join).toList());
                })
                .whenComplete((trees, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        setStudentStatus(readableError(error));
                        return;
                    }
                    closeTopicDrawer();
                    currentTopicTrees = trees;
                    buildCurriculumTree(trees);
                    buildTopicCards();
                    updateContinueLearningWidget();
                    showDashboard();
                    setStudentStatus("Curriculum tracks are ready.");
                }));
    }

    private void openAdmin(AuthResponse session) {
        adminNameLabel.setText(session.displayName());
        showOnly(adminView);
        refreshAdminUsers();
    }

    private record LessonRef(TopicTree topic, ModuleView module, SubmoduleView submodule, LessonSummary lesson) {}

    private void buildCurriculumTree(List<TopicTree> trees) {
        currentTopicTrees = trees;
        TreeItem<NavigationItem> root = new TreeItem<>(new NavigationItem("Curriculum", null, null));

        List<LessonRef> allLessons = new ArrayList<>();
        for (TopicTree topic : trees) {
            for (ModuleView module : topic.modules()) {
                for (SubmoduleView submodule : module.submodules()) {
                    for (LessonSummary lesson : submodule.lessons()) {
                        allLessons.add(new LessonRef(topic, module, submodule, lesson));
                    }
                }
            }
        }

        int lastCompletedIndex = -1;
        for (int i = 0; i < allLessons.size(); i++) {
            if (allLessons.get(i).lesson().completed()) {
                lastCompletedIndex = i;
            }
        }
        int nextTodoIndex = lastCompletedIndex >= 0
                ? (lastCompletedIndex + 1 < allLessons.size() ? lastCompletedIndex + 1 : -1)
                : (allLessons.isEmpty() ? -1 : 0);

        Long nextTodoLessonId = nextTodoIndex >= 0 ? allLessons.get(nextTodoIndex).lesson().id() : null;
        TreeItem<NavigationItem> todoTreeItem = null;

        for (TopicTree topic : trees) {
            TreeItem<NavigationItem> topicItem = new TreeItem<>(
                    new NavigationItem(topic.title(), topic.id(), null));
            for (ModuleView module : topic.modules()) {
                TreeItem<NavigationItem> moduleItem = new TreeItem<>(
                        new NavigationItem(module.title(), topic.id(), null));
                for (SubmoduleView submodule : module.submodules()) {
                    TreeItem<NavigationItem> submoduleItem = new TreeItem<>(
                            new NavigationItem(submodule.title(), topic.id(), null));
                    for (LessonSummary lesson : submodule.lessons()) {
                        String label;
                        if (lesson.completed()) {
                            label = "✓  " + lesson.title();
                        } else if (lesson.id().equals(nextTodoLessonId)) {
                            label = "▶  " + lesson.title() + " (To Do)";
                        } else {
                            label = "○  " + lesson.title();
                        }
                        TreeItem<NavigationItem> lessonItem = new TreeItem<>(new NavigationItem(
                                label, topic.id(), lesson.id()));
                        if (lesson.id().equals(nextTodoLessonId)) {
                            todoTreeItem = lessonItem;
                        }
                        submoduleItem.getChildren().add(lessonItem);
                    }
                    moduleItem.getChildren().add(submoduleItem);
                }
                topicItem.getChildren().add(moduleItem);
            }
            root.getChildren().add(topicItem);
        }
        curriculumTree.setRoot(root);

        if (todoTreeItem != null) {
            expandParents(todoTreeItem);
        } else if (!root.getChildren().isEmpty()) {
            root.getChildren().get(0).setExpanded(true);
        }
    }

    private static final double CARD_IMAGE_HEIGHT = 158.0;
    private static final double CARD_RADIUS = 14.0;
    private static final double CARD_ASPECT_RATIO = 340.0 / 600.0;

    private void buildTopicCards() {
        topicCards.getChildren().clear();
        for (TopicSummary topic : topics) {
            topicCards.getChildren().add(buildTopicCard(topic));
        }
        updateTopicCardWidths();
        Platform.runLater(this::updateTopicCardWidths);
    }

    private void updateTopicCardWidths() {
        if (topicCards == null || topicCards.getChildren().isEmpty()) {
            return;
        }
        double width = topicCards.getWidth();
        if (width <= 100.0) {
            if (topicCards.getScene() != null && topicCards.getScene().getWidth() > 0) {
                width = Math.max(300.0, topicCards.getScene().getWidth() - 406.0);
            } else {
                width = 860.0;
            }
        }
        double gap = topicCards.getHgap();
        int columns;
        if (width >= 1150.0) {
            columns = 4;
        } else if (width >= 680.0) {
            columns = 3;
        } else if (width >= 420.0) {
            columns = 2;
        } else {
            columns = 1;
        }
        double cardWidth = Math.floor((width - (columns - 1) * gap - 2.0) / columns);
        if (cardWidth < 180.0) {
            cardWidth = 180.0;
        }
        double imageHeight = Math.round(cardWidth * CARD_ASPECT_RATIO);
        for (javafx.scene.Node node : topicCards.getChildren()) {
            if (node instanceof VBox card) {
                card.setMinWidth(cardWidth);
                card.setPrefWidth(cardWidth);
                card.setMaxWidth(cardWidth);
                if (!card.getChildren().isEmpty() && card.getChildren().get(0) instanceof javafx.scene.image.ImageView img) {
                    img.setFitHeight(imageHeight);
                }
            }
        }
    }

    private void updateProfileCardWidths() {
        if (profileTopicProgressBox == null || profileTopicProgressBox.getChildren().isEmpty()) {
            return;
        }
        double width = profileTopicProgressBox.getWidth();
        if (width <= 100.0) {
            if (profileTopicProgressBox.getScene() != null && profileTopicProgressBox.getScene().getWidth() > 0) {
                width = Math.max(300.0, profileTopicProgressBox.getScene().getWidth() - 406.0);
            } else {
                width = 860.0;
            }
        }
        double gap = profileTopicProgressBox.getHgap();
        int columns;
        if (width >= 1150.0) {
            columns = 4;
        } else if (width >= 680.0) {
            columns = 3;
        } else if (width >= 420.0) {
            columns = 2;
        } else {
            columns = 1;
        }
        double cardWidth = Math.floor((width - (columns - 1) * gap - 2.0) / columns);
        if (cardWidth < 180.0) {
            cardWidth = 180.0;
        }
        for (javafx.scene.Node node : profileTopicProgressBox.getChildren()) {
            if (node instanceof VBox card) {
                card.setMinWidth(cardWidth);
                card.setPrefWidth(cardWidth);
                card.setMaxWidth(cardWidth);
            }
        }
    }

    private VBox buildTopicCard(TopicSummary topic) {
        VBox card = new VBox();
        card.getStyleClass().add("network-topic-card");

        javafx.scene.image.ImageView thumbnail = new javafx.scene.image.ImageView(topicThumbnail(topic));
        thumbnail.setFitHeight(CARD_IMAGE_HEIGHT);
        thumbnail.setPreserveRatio(false);
        thumbnail.getStyleClass().add("topic-card-image");
        thumbnail.fitWidthProperty().bind(card.widthProperty());

        Label title = styledLabel(topic.title(), "topic-card-title");
        Label description = styledLabel(topic.description(), "topic-card-description");
        description.setMaxHeight(56.0);

        Button startButton = new Button("Start learning");
        startButton.getStyleClass().add("start-learning-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(event -> {
            event.consume();
            openTopicDrawer(topic);
        });

        VBox body = new VBox(8.0, title, description, startButton);
        body.getStyleClass().add("topic-card-body");
        VBox.setVgrow(description, javafx.scene.layout.Priority.ALWAYS);

        card.getChildren().addAll(thumbnail, body);

        // Round only the card's corners; the image clips to match the top corners.
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(CARD_RADIUS * 2);
        clip.setArcHeight(CARD_RADIUS * 2);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        card.setClip(clip);

        card.setOnMouseClicked(event -> openTopicDrawer(topic));
        card.setCursor(javafx.scene.Cursor.HAND);
        return card;
    }

    private javafx.scene.image.Image topicThumbnail(TopicSummary topic) {
        String slug = topic.slug() == null ? "" : topic.slug().toLowerCase(java.util.Locale.ROOT);
        String fileName;
        if (slug.contains("dsa") || slug.contains("data-structure")) {
            fileName = "topic-dsa.jpg";
        } else if (slug.contains("web")) {
            fileName = "topic-web-dev.png";
        } else if (slug.contains("app")) {
            fileName = "topic-app-dev.png";
        } else if (slug.contains("ai") || slug.contains("ml")) {
            fileName = "topic-ai-ml.jpg";
        } else if (slug.contains("data-science") || slug.contains("science")) {
            fileName = "topic-data-science.png";
        } else if (slug.contains("game")) {
            fileName = "topic-game-dev.png";
        } else {
            fileName = "topic-languages.jpg";
        }
        return new javafx.scene.image.Image(
                getClass().getResource("/resources/images/" + fileName).toExternalForm());
    }

    @FXML
    private void openTopicDrawer(TopicSummary topic) {
        if (topic == null) {
            return;
        }
        selectedDrawerTopic = topic;
        topicDrawerImage.setImage(topicThumbnail(topic));
        topicDrawerTitle.setText(topic.title());

        int lessonCount = 0;
        int moduleCount = 0;
        TopicTree matchedTree = currentTopicTrees.stream()
                .filter(t -> t.id().equals(topic.id()))
                .findFirst()
                .orElse(null);
        if (matchedTree != null) {
            moduleCount = matchedTree.modules().size();
            for (ModuleView mv : matchedTree.modules()) {
                for (SubmoduleView smv : mv.submodules()) {
                    lessonCount += smv.lessons().size();
                }
            }
        }
        topicDrawerLessonsBadge.setText(lessonCount + (lessonCount == 1 ? " Lesson" : " Lessons"));
        topicDrawerModulesBadge.setText(moduleCount + (moduleCount == 1 ? " Module" : " Modules"));

        topicDrawerWhatTaught.setText(getTopicWhatTaught(topic));
        topicDrawerRealWorld.setText(getTopicRealWorld(topic));

        setVisibleManaged(topicDrawerScrim, true);
        setVisibleManaged(topicDrawerPane, true);
        topicDrawerPane.toFront();
    }

    @FXML
    private void closeTopicDrawer() {
        if (topicDrawerScrim != null) {
            setVisibleManaged(topicDrawerScrim, false);
        }
        if (topicDrawerPane != null) {
            setVisibleManaged(topicDrawerPane, false);
        }
    }

    @FXML
    private void handleTopicDrawerStart() {
        if (selectedDrawerTopic == null) {
            return;
        }
        String slug = selectedDrawerTopic.slug() == null ? "" : selectedDrawerTopic.slug().toLowerCase(Locale.ROOT);
        String titleStr = selectedDrawerTopic.title() == null ? "" : selectedDrawerTopic.title().toLowerCase(Locale.ROOT);
        long topicId = selectedDrawerTopic.id();
        closeTopicDrawer();
        if (slug.contains("web") || titleStr.contains("web")) {
            openWebDevRoadmapWindow();
        } else if (slug.contains("app") || titleStr.contains("app")) {
            openAppDevRoadmapWindow();
        } else if (slug.contains("dsa") || titleStr.contains("dsa") || slug.contains("data-structure") || titleStr.contains("data structure")) {
            openDsaRoadmapWindow();
        } else if (slug.contains("language") || titleStr.contains("language")) {
            openLanguagesRoadmapWindow();
        } else if (slug.contains("ai") || slug.contains("ml") || titleStr.contains("ai") || titleStr.contains("machine learning")) {
            openAiMlRoadmapWindow();
        } else if (slug.contains("data-science") || slug.contains("science") || titleStr.contains("data science")) {
            openDataScienceRoadmapWindow();
        } else if (slug.contains("game") || titleStr.contains("game")) {
            openGameDevRoadmapWindow();
        } else {
            openFirstLesson(topicId);
        }
    }

    private void openWebDevRoadmapWindow() {
        try {
            boolean isDark = appRoot != null && appRoot.getStyleClass().contains("dark-theme");
            javafx.stage.Window owner = (appRoot != null && appRoot.getScene() != null)
                    ? appRoot.getScene().getWindow() : null;
            WebDevTopicsWindow.show(owner, isDark, this::openWebDevModule);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStudentStatus("Could not open Web Development topics: " + ex.getMessage());
        }
    }

    public void openWebDevModule(String moduleKeyword) {
        openCurriculumTopicModule("web", moduleKeyword);
    }

    private void openAppDevRoadmapWindow() {
        try {
            boolean isDark = appRoot != null && appRoot.getStyleClass().contains("dark-theme");
            javafx.stage.Window owner = (appRoot != null && appRoot.getScene() != null)
                    ? appRoot.getScene().getWindow() : null;
            AppDevTopicsWindow.show(owner, isDark, this::openAppDevModule);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStudentStatus("Could not open App Development topics: " + ex.getMessage());
        }
    }

    public void openAppDevModule(String moduleKeyword) {
        openCurriculumTopicModule("app", moduleKeyword);
    }

    private void openDsaRoadmapWindow() {
        try {
            boolean isDark = appRoot != null && appRoot.getStyleClass().contains("dark-theme");
            javafx.stage.Window owner = (appRoot != null && appRoot.getScene() != null)
                    ? appRoot.getScene().getWindow() : null;
            DsaTopicsWindow.show(owner, isDark, this::openDsaModule);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStudentStatus("Could not open DSA topics: " + ex.getMessage());
        }
    }

    public void openDsaModule(String moduleKeyword) {
        openCurriculumTopicModule("dsa", moduleKeyword);
    }

    private void openLanguagesRoadmapWindow() {
        try {
            boolean isDark = appRoot != null && appRoot.getStyleClass().contains("dark-theme");
            javafx.stage.Window owner = (appRoot != null && appRoot.getScene() != null)
                    ? appRoot.getScene().getWindow() : null;
            LanguagesTopicsWindow.show(owner, isDark, this::openLanguagesModule);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStudentStatus("Could not open Languages topics: " + ex.getMessage());
        }
    }

    public void openLanguagesModule(String moduleKeyword) {
        openCurriculumTopicModule("language", moduleKeyword);
    }

    private void openAiMlRoadmapWindow() {
        try {
            boolean isDark = appRoot != null && appRoot.getStyleClass().contains("dark-theme");
            javafx.stage.Window owner = (appRoot != null && appRoot.getScene() != null)
                    ? appRoot.getScene().getWindow() : null;
            AiMlTopicsWindow.show(owner, isDark, this::openAiMlModule);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStudentStatus("Could not open AI/ML topics: " + ex.getMessage());
        }
    }

    public void openAiMlModule(String moduleKeyword) {
        openCurriculumTopicModule("ai", moduleKeyword);
    }

    private void openDataScienceRoadmapWindow() {
        try {
            boolean isDark = appRoot != null && appRoot.getStyleClass().contains("dark-theme");
            javafx.stage.Window owner = (appRoot != null && appRoot.getScene() != null)
                    ? appRoot.getScene().getWindow() : null;
            DataScienceTopicsWindow.show(owner, isDark, this::openDataScienceModule);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStudentStatus("Could not open Data Science topics: " + ex.getMessage());
        }
    }

    public void openDataScienceModule(String moduleKeyword) {
        openCurriculumTopicModule("science", moduleKeyword);
    }

    private void openGameDevRoadmapWindow() {
        try {
            boolean isDark = appRoot != null && appRoot.getStyleClass().contains("dark-theme");
            javafx.stage.Window owner = (appRoot != null && appRoot.getScene() != null)
                    ? appRoot.getScene().getWindow() : null;
            GameDevTopicsWindow.show(owner, isDark, this::openGameDevModule);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStudentStatus("Could not open Game Development topics: " + ex.getMessage());
        }
    }

    public void openGameDevModule(String moduleKeyword) {
        openCurriculumTopicModule("game", moduleKeyword);
    }

    public void openCurriculumTopicModule(String topicKeyword, String moduleKeyword) {
        if (appRoot != null && appRoot.getScene() != null && appRoot.getScene().getWindow() instanceof Stage mainStage) {
            mainStage.toFront();
            mainStage.requestFocus();
        }
        if (curriculumTree == null || curriculumTree.getRoot() == null) {
            return;
        }
        closeTopicDrawer();

        TreeItem<NavigationItem> targetTopicItem = null;
        for (TreeItem<NavigationItem> item : curriculumTree.getRoot().getChildren()) {
            String label = item.getValue().label();
            if (label != null && label.toLowerCase(Locale.ROOT).contains(topicKeyword.toLowerCase(Locale.ROOT))) {
                targetTopicItem = item;
                break;
            }
        }
        if (targetTopicItem == null && !curriculumTree.getRoot().getChildren().isEmpty()) {
            targetTopicItem = curriculumTree.getRoot().getChildren().get(0);
        }
        if (targetTopicItem == null) {
            return;
        }
        targetTopicItem.setExpanded(true);

        TreeItem<NavigationItem> targetModuleItem = null;
        if (moduleKeyword != null && !moduleKeyword.isBlank()) {
            String kw = moduleKeyword.toLowerCase(Locale.ROOT);
            for (TreeItem<NavigationItem> modItem : targetTopicItem.getChildren()) {
                String modLabel = modItem.getValue().label().toLowerCase(Locale.ROOT);
                if (modLabel.contains(kw) || kw.contains(modLabel)) {
                    targetModuleItem = modItem;
                    break;
                }
            }
        }
        if (targetModuleItem == null && !targetTopicItem.getChildren().isEmpty()) {
            targetModuleItem = targetTopicItem.getChildren().get(0);
        }

        if (targetModuleItem != null) {
            targetModuleItem.setExpanded(true);
            TreeItem<NavigationItem> targetLesson = firstIncompleteLesson(targetModuleItem);
            if (targetLesson == null) {
                targetLesson = firstLesson(targetModuleItem);
            }
            if (targetLesson != null) {
                expandParents(targetLesson);
                curriculumTree.getSelectionModel().clearSelection();
                curriculumTree.getSelectionModel().select(targetLesson);
                openNavigationItem(targetLesson);
            }
        }
    }

    private String getTopicWhatTaught(TopicSummary topic) {
        String slug = topic.slug() == null ? "" : topic.slug().toLowerCase(Locale.ROOT);
        if (slug.contains("dsa") || slug.contains("data-structure")) {
            return "• Essential linear and non-linear data structures: Arrays, Linked Lists, Stacks, Queues, Hash Maps, Heaps, BSTs, and Segment Trees.\n"
                    + "• Graph theory algorithms: BFS, DFS, Dijkstra, Bellman-Ford, Kruskal, and Topological Sort.\n"
                    + "• Core algorithmic paradigms: Dynamic Programming, Divide and Conquer, Greedy approaches, and Backtracking.";
        } else if (slug.contains("web")) {
            return "• Complete 8-phase full-stack curriculum: HTML5 semantic markup, CSS3 responsive layouts (Flexbox & Grid), and modern JavaScript ES6+ & DOM.\n"
                    + "• Frontend SPA architecture with React.js, functional components, and hooks.\n"
                    + "• Backend API services with Node.js/Express, PostgreSQL & MongoDB databases, JWT authentication, and production cloud deployment.";
        } else if (slug.contains("app")) {
            return "• Comprehensive 8-phase mobile curriculum: Flutter & Dart cross-platform development, and React Native with Expo tooling.\n"
                    + "• Native Android engineering with Kotlin & Jetpack Compose, and native iOS with Swift & SwiftUI.\n"
                    + "• Production architecture: BLoC state management, REST & Firebase cloud backends, SQLite/Room persistence, and App Store / Google Play CI/CD deployment.";
        } else if (slug.contains("ai") || slug.contains("ml")) {
            return "• Supervised and unsupervised machine learning algorithms, cost functions, gradient descent, and model evaluation.\n"
                    + "• Deep learning architectures, neural networks, and modern AI models.\n"
                    + "• Practical model development, training, and fine-tuning with Python frameworks: Scikit-learn, TensorFlow/Keras, and PyTorch.";
        } else if (slug.contains("data-science") || slug.contains("science")) {
            return "• High-performance numerical computing with NumPy multidimensional arrays.\n"
                    + "• Tabular data wrangling, cleaning, transformation, aggregation, and time-series analysis with Pandas.\n"
                    + "• Publication-grade visual storytelling and statistical charting using Matplotlib and Seaborn.";
        } else if (slug.contains("game")) {
            return "• Game loop lifecycle, delta time, collision detection, and 2D/3D physics mathematics.\n"
                    + "• Industry-standard game engine workflows, component architecture, and C# scripting in Unity.\n"
                    + "• 2D arcade physics, sprite animation, and game mechanics using Python and Pygame.";
        } else {
            return "• Core language syntax, control structures, and memory paradigms across Python, Java, JavaScript, C#, C++, and C.\n"
                    + "• Object-Oriented Programming (OOP), functional programming, streams, lambdas, and type safety.\n"
                    + "• Tooling, package managers, virtual environments, build systems (Maven & Gradle), and automated testing frameworks (pytest, JUnit, Jest).";
        }
    }

    private String getTopicRealWorld(TopicSummary topic) {
        String slug = topic.slug() == null ? "" : topic.slug().toLowerCase(Locale.ROOT);
        if (slug.contains("dsa") || slug.contains("data-structure")) {
            return "Efficient data structures and algorithmic thinking are the bedrock of high-scale software engineering. They enable systems to process millions of transactions per second with optimal time and memory complexity, and form the core benchmark in technical interviews at top technology companies.";
        } else if (slug.contains("web")) {
            return "Full-stack web developers build and maintain the digital platforms that drive global business, SaaS applications, and modern cloud services. Mastering frontend architecture, backend REST APIs, databases, authentication, and DevOps equips you for high-impact production engineering roles.";
        } else if (slug.contains("app")) {
            return "From mission-critical medical workstations and financial trading dashboards to consumer mobile devices, native and cross-platform apps deliver ultra-fast, offline-capable client experiences with deep operating system integration.";
        } else if (slug.contains("ai") || slug.contains("ml")) {
            return "Artificial intelligence is transforming every modern industry—from autonomous vehicles and medical diagnostics to intelligent search, fraud prevention, and generative assistants. Building ML models enables you to turn raw data into automated, intelligent decisions.";
        } else if (slug.contains("data-science") || slug.contains("science")) {
            return "Data is the world's most valuable strategic asset. Data science enables companies to discover critical market trends, build predictive models, optimize supply chains, and make data-driven decisions that generate measurable business value.";
        } else if (slug.contains("game")) {
            return "Game development pushes the boundaries of real-time computer science, graphics rendering, and low-latency interactive simulation. The core skills you build—spatial mathematics, performance profiling, and state management—are in high demand across gaming, simulation, robotics, and AR/VR.";
        } else {
            return "Mastering foundational programming languages gives you the versatility to adapt to any engineering stack. Whether writing high-performance C++ game engines, scalable enterprise microservices in Java, or rapid automation scripts in Python, polyglot fluency is the hallmark of versatile senior software engineers.";
        }
    }

    private void updateContinueLearningWidget() {
        if (continueSidebarSection == null || continueItemsBox == null) {
            return;
        }
        if (currentTopicTrees == null || currentTopicTrees.isEmpty()) {
            setVisibleManaged(continueSidebarSection, false);
            return;
        }

        List<LessonRef> allLessons = new ArrayList<>();
        for (TopicTree topic : currentTopicTrees) {
            for (ModuleView module : topic.modules()) {
                for (SubmoduleView submodule : module.submodules()) {
                    for (LessonSummary lesson : submodule.lessons()) {
                        allLessons.add(new LessonRef(topic, module, submodule, lesson));
                    }
                }
            }
        }

        if (allLessons.isEmpty()) {
            setVisibleManaged(continueSidebarSection, false);
            return;
        }

        int lastCompletedIndex = -1;
        for (int i = 0; i < allLessons.size(); i++) {
            if (allLessons.get(i).lesson().completed()) {
                lastCompletedIndex = i;
            }
        }

        LessonRef finishedRef = lastCompletedIndex >= 0 ? allLessons.get(lastCompletedIndex) : null;
        LessonRef todoRef = null;
        if (lastCompletedIndex >= 0) {
            if (lastCompletedIndex + 1 < allLessons.size()) {
                todoRef = allLessons.get(lastCompletedIndex + 1);
            }
        } else {
            todoRef = allLessons.get(0);
        }

        setVisibleManaged(continueSidebarSection, true);
        continueItemsBox.getChildren().clear();

        if (todoRef != null) {
            continueTrackLabel.setText(todoRef.module().title());
        } else if (finishedRef != null) {
            continueTrackLabel.setText(finishedRef.module().title());
        } else {
            continueTrackLabel.setText("All Tracks");
        }

        if (finishedRef != null) {
            Button finishedBtn = new Button();
            finishedBtn.getStyleClass().add("continue-item-button");
            finishedBtn.setMaxWidth(Double.MAX_VALUE);

            Label badge = new Label("✓ Finished");
            badge.getStyleClass().add("continue-badge-completed");

            Label title = new Label(finishedRef.lesson().title());
            title.getStyleClass().add("continue-lesson-title");
            title.setWrapText(true);

            HBox row = new HBox(8.0, badge, title);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
            finishedBtn.setGraphic(row);

            final LessonRef targetFinished = finishedRef;
            finishedBtn.setOnAction(e -> openLessonById(targetFinished.topic().id(), targetFinished.lesson().id()));
            continueItemsBox.getChildren().add(finishedBtn);
        }

        if (todoRef != null) {
            Button todoBtn = new Button();
            todoBtn.getStyleClass().add("continue-item-button");
            todoBtn.setMaxWidth(Double.MAX_VALUE);

            Label badge = new Label("▶ To Do");
            badge.getStyleClass().add("continue-badge-todo");

            Label title = new Label(todoRef.lesson().title());
            title.getStyleClass().add("continue-lesson-title");
            title.setWrapText(true);

            HBox row = new HBox(8.0, badge, title);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
            todoBtn.setGraphic(row);

            final LessonRef targetTodo = todoRef;
            todoBtn.setOnAction(e -> openLessonById(targetTodo.topic().id(), targetTodo.lesson().id()));
            continueItemsBox.getChildren().add(todoBtn);

            Button resumeBtn = new Button("Resume Learning ▶");
            resumeBtn.getStyleClass().add("continue-quick-action");
            resumeBtn.setMaxWidth(Double.MAX_VALUE);
            resumeBtn.setOnAction(e -> openLessonById(targetTodo.topic().id(), targetTodo.lesson().id()));
            continueItemsBox.getChildren().add(resumeBtn);
        } else {
            Label allDoneLabel = new Label("🎉 All curriculum lessons completed!");
            allDoneLabel.getStyleClass().add("continue-lesson-title");
            continueItemsBox.getChildren().add(allDoneLabel);
        }
    }

    private void openLessonById(Long topicId, Long lessonId) {
        if (curriculumTree.getRoot() == null || lessonId == null) {
            return;
        }
        TreeItem<NavigationItem> target = findLessonItem(curriculumTree.getRoot(), lessonId);
        if (target != null) {
            expandParents(target);
            curriculumTree.getSelectionModel().clearSelection();
            curriculumTree.getSelectionModel().select(target);
            openNavigationItem(target);
        } else if (topicId != null) {
            openFirstLesson(topicId);
        }
    }

    private TreeItem<NavigationItem> findLessonItem(TreeItem<NavigationItem> node, Long lessonId) {
        if (node == null) {
            return null;
        }
        if (node.getValue() != null && lessonId.equals(node.getValue().lessonId())) {
            return node;
        }
        for (TreeItem<NavigationItem> child : node.getChildren()) {
            TreeItem<NavigationItem> found = findLessonItem(child, lessonId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void expandParents(TreeItem<?> item) {
        TreeItem<?> parent = item.getParent();
        while (parent != null) {
            parent.setExpanded(true);
            parent = parent.getParent();
        }
    }

    private void openFirstLesson(long topicId) {
        TreeItem<NavigationItem> topicItem = curriculumTree.getRoot().getChildren().stream()
                .filter(item -> Long.valueOf(topicId).equals(item.getValue().topicId()))
                .findFirst()
                .orElse(null);
        TreeItem<NavigationItem> lesson = firstIncompleteLesson(topicItem);
        if (lesson == null) {
            lesson = firstLesson(topicItem);
        }
        if (lesson != null) {
            expandParents(lesson);
            curriculumTree.getSelectionModel().clearSelection();
            curriculumTree.getSelectionModel().select(lesson);
            openNavigationItem(lesson);
        }
    }

    private TreeItem<NavigationItem> firstIncompleteLesson(TreeItem<NavigationItem> item) {
        if (item == null) {
            return null;
        }
        if (item.getValue().lessonId() != null) {
            String label = item.getValue().label();
            if (label != null && !label.startsWith("✓  ")) {
                return item;
            }
            return null;
        }
        for (TreeItem<NavigationItem> child : item.getChildren()) {
            TreeItem<NavigationItem> result = firstIncompleteLesson(child);
            if (result != null) {
                item.setExpanded(true);
                return result;
            }
        }
        return null;
    }

    private TreeItem<NavigationItem> firstLesson(TreeItem<NavigationItem> item) {
        if (item == null) {
            return null;
        }
        if (item.getValue().lessonId() != null) {
            return item;
        }
        for (TreeItem<NavigationItem> child : item.getChildren()) {
            TreeItem<NavigationItem> result = firstLesson(child);
            if (result != null) {
                item.setExpanded(true);
                return result;
            }
        }
        return null;
    }

    private void openNavigationItem(TreeItem<NavigationItem> selected) {
        if (selected == null || selected.getValue().lessonId() == null) {
            return;
        }
        NavigationItem item = selected.getValue();
        setStudentStatus("Loading lesson...");
        apiClient.enroll(item.topicId())
                .thenCompose(ignored -> apiClient.lesson(item.lessonId()))
                .whenComplete((lesson, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        setStudentStatus(readableError(error));
                        return;
                    }
                    displayLesson(lesson, selected);
                    setStudentStatus("Lesson loaded from the Spring Boot server.");
                }));
    }

    private void displayLesson(LessonDetails lesson, TreeItem<NavigationItem> selected) {
        stopLessonVideo();
        closeTopicDrawer();
        currentLesson = lesson;
        lessonPathLabel.setText(pathFor(selected));
        lessonTitleLabel.setText(lesson.title());
        lessonSummaryLabel.setText(lesson.summary());
        MarkdownRenderer.render(lesson.bodyMarkdown(), markdownContent);
        lessonExampleArea.setText(lesson.exampleCode() == null ? "" : lesson.exampleCode());
        lessonExampleArea.setManaged(!lessonExampleArea.getText().isBlank());
        lessonExampleArea.setVisible(!lessonExampleArea.getText().isBlank());
        updatingCompletion = true;
        lessonCompletedCheckBox.setSelected(lesson.completed());
        updatingCompletion = false;
        quizButton.setDisable(lesson.quizQuestionCount() == 0);
        quizButton.setText(lesson.quizQuestionCount() == 0 ? "Quiz unavailable" : "Take quiz");

        String path = pathFor(selected);
        String searchContext = path + " " + lesson.title();
        String lowerPath = path.toLowerCase(Locale.ROOT);
        String lowerTitle = lesson.title().toLowerCase(Locale.ROOT);
        Long selTopicId = (selected != null && selected.getValue() != null) ? selected.getValue().topicId() : null;

        boolean isWeb = lowerPath.contains("web") || lowerTitle.contains("web") || isWebTopicId(selTopicId);
        boolean isApp = lowerPath.contains("app") || lowerTitle.contains("app") || isAppTopicId(selTopicId);
        boolean isDsa = lowerPath.contains("dsa") || lowerPath.contains("data structure") || lowerTitle.contains("dsa") || isDsaTopicId(selTopicId);
        boolean isLang = lowerPath.contains("language") || isLanguagesTopicId(selTopicId);
        boolean isAi = lowerPath.contains("ai") || lowerPath.contains("machine learning") || isAiMlTopicId(selTopicId);
        boolean isDs = lowerPath.contains("science") || lowerPath.contains("data science") || isDataScienceTopicId(selTopicId);
        boolean isGame = lowerPath.contains("game") || isGameDevTopicId(selTopicId);

        String videoTitle = "";
        if (isWeb) {
            currentLessonVideoUrl = WebDevTopicsWindow.getVideoUrlFor(searchContext);
            var t = WebDevTopicsWindow.getTopicFor(searchContext);
            videoTitle = t.title() + " — " + t.defaultVideoTitle();
        } else if (isApp) {
            currentLessonVideoUrl = AppDevTopicsWindow.getVideoUrlFor(searchContext);
            var t = AppDevTopicsWindow.getTopicFor(searchContext);
            videoTitle = t.title() + " — " + t.defaultVideoTitle();
        } else if (isDsa) {
            currentLessonVideoUrl = DsaTopicsWindow.getVideoUrlFor(searchContext);
            var t = DsaTopicsWindow.getTopicFor(searchContext);
            videoTitle = t.title() + " — " + t.defaultVideoTitle();
        } else if (isLang) {
            currentLessonVideoUrl = LanguagesTopicsWindow.getVideoUrlFor(searchContext);
            var t = LanguagesTopicsWindow.getTopicFor(searchContext);
            videoTitle = t.title() + " — " + t.defaultVideoTitle();
        } else if (isAi) {
            currentLessonVideoUrl = AiMlTopicsWindow.getVideoUrlFor(searchContext);
            var t = AiMlTopicsWindow.getTopicFor(searchContext);
            videoTitle = t.title() + " — " + t.defaultVideoTitle();
        } else if (isDs) {
            currentLessonVideoUrl = DataScienceTopicsWindow.getVideoUrlFor(searchContext);
            var t = DataScienceTopicsWindow.getTopicFor(searchContext);
            videoTitle = t.title() + " — " + t.defaultVideoTitle();
        } else if (isGame) {
            currentLessonVideoUrl = GameDevTopicsWindow.getVideoUrlFor(searchContext);
            var t = GameDevTopicsWindow.getTopicFor(searchContext);
            videoTitle = t.title() + " — " + t.defaultVideoTitle();
        } else {
            currentLessonVideoUrl = null;
        }

        boolean hasVideo = currentLessonVideoUrl != null && !currentLessonVideoUrl.isBlank();
        if (lessonWatchVideoBtn != null) {
            setVisibleManaged(lessonWatchVideoBtn, false);
        }
        if (lessonCopyVideoBtn != null) {
            setVisibleManaged(lessonCopyVideoBtn, false);
        }

        if (lessonVideoContainer != null) {
            setVisibleManaged(lessonVideoContainer, hasVideo);
            if (hasVideo) {
                if (lessonVideoTitleLabel != null) {
                    lessonVideoTitleLabel.setText(videoTitle);
                }
                String thumbUrl = WebDevTopicsWindow.getThumbnailUrl(currentLessonVideoUrl);
                if (thumbUrl != null && lessonVideoPoster != null) {
                    try {
                        lessonVideoPoster.setImage(new Image(thumbUrl, true));
                        lessonVideoPoster.setFitWidth(720);
                        lessonVideoPoster.setFitHeight(360);
                    } catch (Throwable ignored) {}
                }
                if (embeddedWebPlayerPane != null) {
                    embeddedWebPlayerPane.setCursor(Cursor.HAND);
                }
            }
        }

        boolean hasSimulation = lesson.simulation() != null;
        setVisibleManaged(simulationSection, hasSimulation);
        if (hasSimulation) {
            simulationContentController.load(lesson.simulation());
        } else {
            simulationContentController.stop();
        }
        setVisibleManaged(dashboardPane, false);
        setVisibleManaged(lessonPane, true);
        setVisibleManaged(profilePane, false);
    }

    private boolean isWebTopicId(Long topicId) {
        if (topicId == null || topics == null) {
            return false;
        }
        return topics.stream()
                .anyMatch(t -> topicId.equals(t.id())
                        && ((t.slug() != null && t.slug().toLowerCase(Locale.ROOT).contains("web"))
                            || (t.title() != null && t.title().toLowerCase(Locale.ROOT).contains("web"))));
    }

    private boolean isAppTopicId(Long topicId) {
        if (topicId == null || topics == null) {
            return false;
        }
        return topics.stream()
                .anyMatch(t -> topicId.equals(t.id())
                        && ((t.slug() != null && t.slug().toLowerCase(Locale.ROOT).contains("app"))
                            || (t.title() != null && t.title().toLowerCase(Locale.ROOT).contains("app"))));
    }

    private boolean isDsaTopicId(Long topicId) {
        if (topicId == null || topics == null) {
            return false;
        }
        return topics.stream()
                .anyMatch(t -> topicId.equals(t.id())
                        && ((t.slug() != null && (t.slug().toLowerCase(Locale.ROOT).contains("dsa") || t.slug().toLowerCase(Locale.ROOT).contains("data-structure")))
                            || (t.title() != null && (t.title().toLowerCase(Locale.ROOT).contains("dsa") || t.title().toLowerCase(Locale.ROOT).contains("data structure")))));
    }

    private boolean isLanguagesTopicId(Long topicId) {
        if (topicId == null || topics == null) {
            return false;
        }
        return topics.stream()
                .anyMatch(t -> topicId.equals(t.id())
                        && ((t.slug() != null && t.slug().toLowerCase(Locale.ROOT).contains("language"))
                            || (t.title() != null && t.title().toLowerCase(Locale.ROOT).contains("language"))));
    }

    private boolean isAiMlTopicId(Long topicId) {
        if (topicId == null || topics == null) {
            return false;
        }
        return topics.stream()
                .anyMatch(t -> topicId.equals(t.id())
                        && ((t.slug() != null && (t.slug().toLowerCase(Locale.ROOT).contains("ai") || t.slug().toLowerCase(Locale.ROOT).contains("ml")))
                            || (t.title() != null && (t.title().toLowerCase(Locale.ROOT).contains("ai") || t.title().toLowerCase(Locale.ROOT).contains("machine learning")))));
    }

    private boolean isDataScienceTopicId(Long topicId) {
        if (topicId == null || topics == null) {
            return false;
        }
        return topics.stream()
                .anyMatch(t -> topicId.equals(t.id())
                        && ((t.slug() != null && (t.slug().toLowerCase(Locale.ROOT).contains("science") || t.slug().toLowerCase(Locale.ROOT).contains("data-science")))
                            || (t.title() != null && (t.title().toLowerCase(Locale.ROOT).contains("science") || t.title().toLowerCase(Locale.ROOT).contains("data science")))));
    }

    private boolean isGameDevTopicId(Long topicId) {
        if (topicId == null || topics == null) {
            return false;
        }
        return topics.stream()
                .anyMatch(t -> topicId.equals(t.id())
                        && ((t.slug() != null && t.slug().toLowerCase(Locale.ROOT).contains("game"))
                            || (t.title() != null && t.title().toLowerCase(Locale.ROOT).contains("game"))));
    }

    @FXML
    private void startLessonVideo() {
        if (currentLessonVideoUrl == null || currentLessonVideoUrl.isBlank()) {
            return;
        }
        String videoId = WebDevTopicsWindow.extractYouTubeVideoId(currentLessonVideoUrl);
        if (videoId == null || videoId.isBlank()) {
            setStudentStatus("No YouTube video ID found for this lesson.");
            return;
        }
        if (embeddedWebPlayerPane == null) {
            return;
        }

        try {
            if (lessonWebView == null) {
                lessonWebView = new WebView();
                lessonWebView.setMaxWidth(720);
                lessonWebView.setPrefWidth(640);
                lessonWebView.setPrefHeight(360);
                lessonWebView.setMaxHeight(380);
                // Desktop Mac Safari User Agent provides broadband buffering and desktop mouse event handling
                lessonWebView.getEngine().setUserAgent(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Safari/605.1.15"
                );
                lessonWebView.getEngine().setCreatePopupHandler(param -> null);
                lessonWebView.getEngine().locationProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && !newVal.equals("about:blank")
                            && !newVal.contains("/player")
                            && !newVal.contains("/embed/")) {
                        Platform.runLater(() -> WebDevTopicsWindow.openUrl(newVal));
                    }
                });
            }

            // Load via our embedded Spring Boot server (which provides a valid HTTP origin and referer)
            // or fallback directly to youtube-nocookie.com.
            String playerUrl;
            if (apiClient != null && apiClient.getBaseUri() != null) {
                playerUrl = apiClient.getBaseUri().resolve("player?v=" + videoId).toString();
            } else {
                playerUrl = "https://www.youtube-nocookie.com/embed/" + videoId
                        + "?autoplay=1&playsinline=1&rel=0&enablejsapi=1";
            }
            lessonWebView.getEngine().load(playerUrl);

            embeddedWebPlayerPane.getChildren().setAll(lessonWebView);
            embeddedWebPlayerPane.setOnMouseClicked(null);
            embeddedWebPlayerPane.setCursor(Cursor.DEFAULT);

            if (lessonVideoPlayBtn != null) setVisibleManaged(lessonVideoPlayBtn, false);
            if (lessonVideoStopBtn != null) setVisibleManaged(lessonVideoStopBtn, true);
            if (lessonVideoRewindBtn != null) setVisibleManaged(lessonVideoRewindBtn, true);
            if (lessonVideoForwardBtn != null) setVisibleManaged(lessonVideoForwardBtn, true);
            setStudentStatus("Playing embedded video: " + currentLessonVideoUrl);
        } catch (Throwable t) {
            t.printStackTrace();
            setStudentStatus("Could not load embedded player: " + t.getMessage());
        }
    }

    @FXML
    private void stopLessonVideo() {
        if (lessonWebView != null) {
            try {
                lessonWebView.getEngine().load("about:blank");
            } catch (Throwable ignored) {}
        }
        if (embeddedWebPlayerPane != null) {
            embeddedWebPlayerPane.getChildren().clear();
            if (lessonVideoPoster != null) embeddedWebPlayerPane.getChildren().add(lessonVideoPoster);
            if (lessonPosterOverlay != null) embeddedWebPlayerPane.getChildren().add(lessonPosterOverlay);
            if (lessonPlayPromptBox != null) embeddedWebPlayerPane.getChildren().add(lessonPlayPromptBox);
            embeddedWebPlayerPane.setOnMouseClicked(e -> startLessonVideo());
            embeddedWebPlayerPane.setCursor(Cursor.HAND);
        }
        if (lessonVideoPlayBtn != null) setVisibleManaged(lessonVideoPlayBtn, true);
        if (lessonVideoStopBtn != null) setVisibleManaged(lessonVideoStopBtn, false);
        if (lessonVideoRewindBtn != null) setVisibleManaged(lessonVideoRewindBtn, false);
        if (lessonVideoForwardBtn != null) setVisibleManaged(lessonVideoForwardBtn, false);
    }

    @FXML
    private void seekLessonVideoBackward() {
        seekLessonVideo(-10);
    }

    @FXML
    private void seekLessonVideoForward() {
        seekLessonVideo(10);
    }

    private void seekLessonVideo(int seconds) {
        if (lessonWebView != null) {
            try {
                lessonWebView.getEngine().executeScript("if (window.seekBy) window.seekBy(" + seconds + ");");
                setStudentStatus((seconds > 0 ? "Skipped forward +" : "Rewound ") + Math.abs(seconds) + "s");
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @FXML
    private void openCurrentLessonVideo() {
        if (currentLessonVideoUrl != null && !currentLessonVideoUrl.isBlank()) {
            WebDevTopicsWindow.openUrl(currentLessonVideoUrl);
            setStudentStatus("Opening video lecture: " + currentLessonVideoUrl);
        }
    }

    @FXML
    private void copyCurrentLessonVideo() {
        if (currentLessonVideoUrl != null && !currentLessonVideoUrl.isBlank()) {
            WebDevTopicsWindow.copyToClipboard(currentLessonVideoUrl);
            setStudentStatus("Video lecture URL copied to clipboard!");
            if (lessonCopyVideoBtn != null) {
                lessonCopyVideoBtn.setText("✓ Copied!");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> lessonCopyVideoBtn.setText("📋 Copy Video Link"));
                pause.play();
            }
            if (lessonVideoCopyLinkBtn != null) {
                lessonVideoCopyLinkBtn.setText("✓ Copied!");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> lessonVideoCopyLinkBtn.setText("📋 Copy Link"));
                pause.play();
            }
        }
    }

    private void saveCompletion(boolean completed) {
        if (updatingCompletion || currentLesson == null) {
            return;
        }
        lessonCompletedCheckBox.setDisable(true);
        apiClient.updateCompletion(currentLesson.id(), completed).whenComplete((ignored, error) -> Platform.runLater(() -> {
            lessonCompletedCheckBox.setDisable(false);
            if (error != null) {
                updatingCompletion = true;
                lessonCompletedCheckBox.setSelected(!completed);
                updatingCompletion = false;
                setStudentStatus(readableError(error));
                return;
            }
            currentLesson = new LessonDetails(
                    currentLesson.id(), currentLesson.topicId(), currentLesson.title(), currentLesson.summary(),
                    currentLesson.bodyMarkdown(), currentLesson.exampleCode(), completed,
                    currentLesson.quizQuestionCount(), currentLesson.simulation());
            refreshProgress();
            refreshTrees();
            setStudentStatus(completed ? "Lesson marked complete." : "Lesson returned to in progress.");
        }));
    }

    private void refreshProgress() {
        apiClient.progress().whenComplete((progress, error) -> Platform.runLater(() -> {
            if (error != null) {
                setStudentStatus(readableError(error));
                return;
            }
            renderProgress(progress);
        }));
    }

    private void renderProgress(ProgressSummary progress) {
        double ratio = progress.totalLessons() == 0
                ? 0.0 : (double) progress.completedLessons() / progress.totalLessons();
        overallProgressChart.getChildren().setAll(buildProgressDonut(ratio, 118.0));
        dashboardProgressLabel.setText(progress.completedLessons() + " of " + progress.totalLessons()
                + " lessons complete\n" + progress.quizAttempts() + " quiz attempts so far");
        topicProgressBox.getChildren().clear();
        for (TopicProgress topic : progress.topics()) {
            double topicRatio = topic.totalLessons() == 0
                    ? 0.0 : (double) topic.completedLessons() / topic.totalLessons();
            StackPane donut = buildProgressDonut(topicRatio, 84.0);
            Label title = styledLabel(topic.title(), "topic-progress-title");
            title.setWrapText(true);
            title.setMaxWidth(120.0);
            title.setAlignment(javafx.geometry.Pos.CENTER);
            Label amount = styledLabel(topic.completedLessons() + " / " + topic.totalLessons(), "topic-progress-amount");
            VBox tile = new VBox(8.0, donut, title, amount);
            tile.setAlignment(javafx.geometry.Pos.CENTER);
            tile.getStyleClass().add("topic-progress-tile");
            topicProgressBox.getChildren().add(tile);
        }

        if (profileCompletedLessonsLabel != null) {
            profileCompletedLessonsLabel.setText(progress.completedLessons() + " / " + progress.totalLessons());
            double clamped = Math.max(0.0, Math.min(1.0, ratio));
            profileProgressPercentLabel.setText(Math.round(clamped * 100) + "%");
            if (profileProgressBar != null) {
                profileProgressBar.setProgress(clamped);
            }
            if (profileQuizAttemptsLabel != null) {
                profileQuizAttemptsLabel.setText(String.valueOf(progress.quizAttempts()));
            }
            if (profileCoursesCountLabel != null) {
                profileCoursesCountLabel.setText(String.valueOf(progress.topics().size()));
            }

            if (profileTopicProgressBox != null) {
                profileTopicProgressBox.getChildren().clear();
                for (TopicProgress topic : progress.topics()) {
                    double topicRatio = topic.totalLessons() == 0
                            ? 0.0 : (double) topic.completedLessons() / topic.totalLessons();
                    VBox card = new VBox(8.0);
                    card.getStyleClass().add("profile-course-item");

                    Label title = styledLabel(topic.title(), "profile-course-title");
                    title.setWrapText(true);
                    Label count = styledLabel(topic.completedLessons() + " of " + topic.totalLessons()
                            + " completed (" + Math.round(topicRatio * 100) + "%)", "profile-course-subtitle");
                    count.setWrapText(true);
                    ProgressBar bar = new ProgressBar(topicRatio);
                    bar.setMaxWidth(Double.MAX_VALUE);
                    bar.getStyleClass().add("profile-course-progress");

                    card.getChildren().addAll(title, count, bar);
                    profileTopicProgressBox.getChildren().add(card);
                }
                updateProfileCardWidths();
                Platform.runLater(this::updateProfileCardWidths);
            }
        }
    }

    /**
     * Builds a small donut chart with the completion percentage centered inside it.
     */
    private StackPane buildProgressDonut(double ratio, double size) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        javafx.scene.chart.PieChart chart = new javafx.scene.chart.PieChart();
        javafx.scene.chart.PieChart.Data completedSlice =
                new javafx.scene.chart.PieChart.Data("Completed", Math.max(clamped, 0.0001));
        javafx.scene.chart.PieChart.Data remainingSlice =
                new javafx.scene.chart.PieChart.Data("Remaining", Math.max(1.0 - clamped, 0.0001));
        chart.getData().addAll(completedSlice, remainingSlice);
        chart.setLabelsVisible(false);
        chart.setLegendVisible(false);
        chart.setStartAngle(90);
        chart.setAnimated(false);
        chart.setMinSize(size, size);
        chart.setPrefSize(size, size);
        chart.setMaxSize(size, size);
        chart.getStyleClass().add("progress-donut");
        if (completedSlice.getNode() != null) {
            completedSlice.getNode().getStyleClass().add("progress-donut-completed");
            completedSlice.getNode().setStyle("-fx-pie-color: #10b981;");
        }
        if (remainingSlice.getNode() != null) {
            remainingSlice.getNode().getStyleClass().add("progress-donut-remaining");
        }

        javafx.scene.shape.Circle hole = new javafx.scene.shape.Circle(size * 0.32);
        hole.getStyleClass().add("progress-donut-hole");

        Label percentLabel = new Label(Math.round(clamped * 100) + "%");
        percentLabel.getStyleClass().add("progress-donut-label");

        StackPane stack = new StackPane(chart, hole, percentLabel);
        stack.setMinSize(size, size);
        stack.setPrefSize(size, size);
        stack.setMaxSize(size, size);
        return stack;
    }

    private void refreshTrees() {
        if (topics.isEmpty()) {
            return;
        }
        List<CompletableFuture<TopicTree>> requests = topics.stream()
                .map(topic -> apiClient.topicTree(topic.id()))
                .toList();
        CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> requests.stream().map(CompletableFuture::join).toList())
                .thenAccept(trees -> Platform.runLater(() -> {
                    currentTopicTrees = trees;
                    buildCurriculumTree(trees);
                    updateContinueLearningWidget();
                }));
    }

    private void showAuthMode(boolean register) {
        registrationMode = register;
        setVisibleManaged(displayNameField, register);
        setVisibleManaged(roleSelector, register);
        setVisibleManaged(forgotPasswordRow, !register);
        authTitleLabel.setText(register ? "Create your account" : "Welcome back");
        authSubtitleLabel.setText(register
                ? "Register a student account through CodeTrail."
                : "Sign in to continue learning.");
        authPrimaryButton.setText(register ? "Create account" : "Sign in");
        authModeButton.setText(register ? "Already registered? Sign in" : "New here? Create account");
        if (googleAuthLabel != null) {
            googleAuthLabel.setText(register ? "Sign up with Google" : "Continue with Google");
        }
        authMessageLabel.setText("");
    }

    private void setAuthBusy(boolean busy) {
        authPrimaryButton.setDisable(busy);
        authModeButton.setDisable(busy);
        if (googleAuthButton != null) {
            googleAuthButton.setDisable(busy);
        }
        if (forgotPasswordButton != null) {
            forgotPasswordButton.setDisable(busy);
        }
        authPrimaryButton.setText(busy ? "Please wait..." : registrationMode ? "Create account" : "Sign in");
    }

    @FXML
    private void handleGoogleAuth() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Sign in with Google");
        dialog.setHeaderText(null);
        if (appRoot.getScene() != null && appRoot.getScene().getWindow() != null) {
            dialog.initOwner(appRoot.getScene().getWindow());
        }

        VBox content = new VBox(14.0);
        content.setPrefWidth(380.0);
        content.setStyle("-fx-padding: 22px 24px; -fx-background-color: #ffffff;");

        HBox headerBox = new HBox(12.0);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.image.ImageView googleIcon = new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(getClass().getResource("/resources/images/icon-google.png").toExternalForm()));
        googleIcon.setFitWidth(28.0);
        googleIcon.setFitHeight(28.0);
        googleIcon.setPreserveRatio(true);

        VBox titleBox = new VBox(2.0);
        Label gTitle = new Label("Sign in with Google");
        gTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #202124;");
        Label gSub = new Label("to continue to CodeTrail");
        gSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f6368;");
        titleBox.getChildren().addAll(gTitle, gSub);
        headerBox.getChildren().addAll(googleIcon, titleBox);

        Separator sep = new Separator();

        Label emailPrompt = new Label("Enter your Gmail address:");
        emailPrompt.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #3c4043;");
        TextField emailField = new TextField("student@gmail.com");
        emailField.setPromptText("username@gmail.com");
        emailField.setStyle("-fx-padding: 9px 12px; -fx-border-color: #dadce0; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        Label namePrompt = new Label("Your Full Name:");
        namePrompt.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #3c4043;");
        TextField nameField = new TextField("Student");
        nameField.setPromptText("Display Name");
        nameField.setStyle("-fx-padding: 9px 12px; -fx-border-color: #dadce0; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        Label hint = new Label("Standard Google authorization will automatically provision or sign into your CodeTrail student account.");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #70757a;");

        content.getChildren().addAll(headerBox, sep, emailPrompt, emailField, namePrompt, nameField, hint);
        dialog.getDialogPane().setContent(content);

        ButtonType nextButtonType = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(nextButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == nextButtonType) {
                return new Pair<>(emailField.getText().trim(), nameField.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pair -> {
            String email = pair.getKey();
            String name = pair.getValue();
            if (email.isBlank() || !email.contains("@")) {
                showAuthError("Please enter a valid Gmail address.");
                return;
            }
            setAuthBusy(true);
            apiClient.googleLogin(email, name).whenComplete((session, error) -> Platform.runLater(() -> {
                setAuthBusy(false);
                if (error != null) {
                    showAuthError(readableError(error));
                    return;
                }
                apiClient.useSession(session);
                openStudent(session);
            }));
        });
    }

    @FXML
    private void showForgotPasswordDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText(null);
        if (appRoot.getScene() != null && appRoot.getScene().getWindow() != null) {
            dialog.initOwner(appRoot.getScene().getWindow());
        }

        VBox content = new VBox(14.0);
        content.setPrefWidth(400.0);
        content.setStyle("-fx-padding: 22px 24px; -fx-background-color: #ffffff;");

        Label title = new Label("Forgot your password?");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #14242e;");

        Label subtitle = new Label("Enter your registered Gmail address or username. A 6-digit verification code will be dispatched to reset your credentials.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #60717b;");

        TextField identifierField = new TextField();
        identifierField.setPromptText("Gmail address or username");
        identifierField.setStyle("-fx-padding: 9px 12px; -fx-border-color: #d5dde1; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        if (usernameField != null && !usernameField.getText().isBlank()) {
            identifierField.setText(usernameField.getText().trim());
        }

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button sendCodeButton = new Button("Send verification code");
        sendCodeButton.setMaxWidth(Double.MAX_VALUE);
        sendCodeButton.setStyle("-fx-background-color: #0089fc; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 9px; -fx-background-radius: 6px; -fx-cursor: hand;");

        VBox step2Box = new VBox(10.0);
        step2Box.setVisible(false);
        step2Box.setManaged(false);

        Separator sep = new Separator();
        TextField codeField = new TextField();
        codeField.setPromptText("Enter 6-digit verification code");
        codeField.setStyle("-fx-padding: 9px 12px; -fx-border-color: #d5dde1; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");
        newPasswordField.setStyle("-fx-padding: 9px 12px; -fx-border-color: #d5dde1; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        confirmPasswordField.setStyle("-fx-padding: 9px 12px; -fx-border-color: #d5dde1; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        Button submitResetButton = new Button("Reset Password");
        submitResetButton.setMaxWidth(Double.MAX_VALUE);
        submitResetButton.setStyle("-fx-background-color: #107c41; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 9px; -fx-background-radius: 6px; -fx-cursor: hand;");

        step2Box.getChildren().addAll(sep, new Label("Enter the code from your email:"), codeField, newPasswordField, confirmPasswordField, submitResetButton);

        sendCodeButton.setOnAction(e -> {
            String identifier = identifierField.getText().trim();
            if (identifier.isBlank()) {
                statusLabel.setStyle("-fx-text-fill: #bd2c2c; -fx-font-size: 12px;");
                statusLabel.setText("Please enter your username or email address.");
                return;
            }
            sendCodeButton.setDisable(true);
            statusLabel.setStyle("-fx-text-fill: #60717b; -fx-font-size: 12px;");
            statusLabel.setText("Sending verification code to " + identifier + "...");

            apiClient.forgotPassword(identifier).whenComplete((res, err) -> Platform.runLater(() -> {
                sendCodeButton.setDisable(false);
                if (err != null) {
                    statusLabel.setStyle("-fx-text-fill: #bd2c2c; -fx-font-size: 12px;");
                    statusLabel.setText(readableError(err));
                    return;
                }
                statusLabel.setStyle("-fx-text-fill: #107c41; -fx-font-size: 12px; -fx-font-weight: 600;");
                statusLabel.setText(res.message() + (res.code() != null ? " [Code: " + res.code() + "]" : ""));
                sendCodeButton.setText("Resend code");
                step2Box.setVisible(true);
                step2Box.setManaged(true);
            }));
        });

        submitResetButton.setOnAction(e -> {
            String identifier = identifierField.getText().trim();
            String code = codeField.getText().trim();
            String newPw = newPasswordField.getText();
            String confirmPw = confirmPasswordField.getText();

            if (code.isBlank() || newPw.isBlank()) {
                statusLabel.setStyle("-fx-text-fill: #bd2c2c; -fx-font-size: 12px;");
                statusLabel.setText("Please fill in the code and your new password.");
                return;
            }
            if (!newPw.equals(confirmPw)) {
                statusLabel.setStyle("-fx-text-fill: #bd2c2c; -fx-font-size: 12px;");
                statusLabel.setText("Passwords do not match.");
                return;
            }

            submitResetButton.setDisable(true);
            apiClient.resetPassword(identifier, code, newPw).whenComplete((res, err) -> Platform.runLater(() -> {
                submitResetButton.setDisable(false);
                if (err != null) {
                    statusLabel.setStyle("-fx-text-fill: #bd2c2c; -fx-font-size: 12px;");
                    statusLabel.setText(readableError(err));
                    return;
                }
                dialog.close();
                authMessageLabel.setStyle("-fx-text-fill: #107c41; -fx-font-weight: 600;");
                authMessageLabel.setText("Password successfully reset! Please sign in with your new password.");
                passwordField.clear();
            }));
        });

        content.getChildren().addAll(title, subtitle, identifierField, sendCodeButton, statusLabel, step2Box);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Highlights a field's "shell" container (border/background) whenever
     * any of its underlying inputs has focus - JavaFX has no built-in
     * :focus-within pseudo-class, so it's reproduced here with a style class.
     */
    private void linkFieldShellFocus(StackPane shell, TextField... inputs) {
        for (TextField input : inputs) {
            input.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                boolean anyFocused = false;
                for (TextField candidate : inputs) {
                    anyFocused |= candidate.isFocused();
                }
                if (anyFocused && !shell.getStyleClass().contains("field-shell-focused")) {
                    shell.getStyleClass().add("field-shell-focused");
                } else if (!anyFocused) {
                    shell.getStyleClass().remove("field-shell-focused");
                }
            });
        }
    }

    private static final String EYE_OPEN = "/resources/images/icon-eye.png";
    private static final String EYE_OFF = "/resources/images/icon-eye-off.png";

    @FXML
    private void togglePasswordVisibility() {
        boolean reveal = passwordVisibilityToggle.isSelected();
        setVisibleManaged(passwordTextField, reveal);
        setVisibleManaged(passwordField, !reveal);
        passwordVisibilityIcon.setImage(new javafx.scene.image.Image(
                getClass().getResourceAsStream(reveal ? EYE_OFF : EYE_OPEN)));
        javafx.scene.control.TextField focused = reveal ? passwordTextField : passwordField;
        focused.requestFocus();
        focused.positionCaret(focused.getText() == null ? 0 : focused.getText().length());
    }

    private void showAuthError(String message) {
        authMessageLabel.setText(message);
    }

    private void showOnly(Node visibleNode) {
        for (Node node : List.of(loginView, studentView, adminView)) {
            setVisibleManaged(node, node == visibleNode);
        }
    }

    private void setVisibleManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setStudentStatus(String message) {
        studentStatusLabel.setText(message);
    }

    private void setAdminStatus(String message) {
        adminStatusLabel.setText(message);
    }

    private Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private String cleanNavLabel(String label) {
        if (label == null) {
            return "";
        }
        String clean = label;
        if (clean.startsWith("✓  ") || clean.startsWith("▶  ") || clean.startsWith("○  ")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith(" (To Do)")) {
            clean = clean.substring(0, clean.length() - 8);
        }
        return clean.trim();
    }

    private String pathFor(TreeItem<NavigationItem> item) {
        List<String> parts = new ArrayList<>();
        TreeItem<NavigationItem> current = item;
        while (current != null && current.getParent() != null) {
            parts.addFirst(cleanNavLabel(current.getValue().label()));
            current = current.getParent();
        }
        return String.join("  /  ", parts);
    }

    private String readableError(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current.getCause() != null) && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "The request could not be completed." : current.getMessage();
    }

    private record NavigationItem(String label, Long topicId, Long lessonId) {
    }
}
