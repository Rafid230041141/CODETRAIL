package application.client.view;

import java.awt.Desktop;
import java.net.URI;
import application.client.dsa.judge.DsaProblemArenaWindow;
import application.client.dsa.judge.ProgrammingLanguage;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Dedicated modal window displaying the comprehensive App Development curriculum
 * covering cross-platform frameworks, native Android, native iOS, mobile state
 * management, databases, and store deployment with curated video courses.
 *
 * Designed with full visual and architectural parity to the Web Development curriculum window.
 */
public class AppDevTopicsWindow {

    private static final Map<String, String> VIDEO_URL_OVERRIDES = new ConcurrentHashMap<>();

    public static class AppDevTopic {
        private final String moduleKey;
        private final String phaseTag;
        private final String icon;
        private final String title;
        private final String lessonCount;
        private final String description;
        private final List<String> keyHighlights;
        private final String realWorldValue;
        private final String defaultVideoTitle;
        private final String defaultVideoUrl;

        public AppDevTopic(
                String moduleKey,
                String phaseTag,
                String icon,
                String title,
                String lessonCount,
                String description,
                List<String> keyHighlights,
                String realWorldValue,
                String defaultVideoTitle,
                String defaultVideoUrl
        ) {
            this.moduleKey = moduleKey;
            this.phaseTag = phaseTag;
            this.icon = icon;
            this.title = title;
            this.lessonCount = lessonCount;
            this.description = description;
            this.keyHighlights = keyHighlights;
            this.realWorldValue = realWorldValue;
            this.defaultVideoTitle = defaultVideoTitle;
            this.defaultVideoUrl = defaultVideoUrl;
        }

        public String moduleKey() { return moduleKey; }
        public String phaseTag() { return phaseTag; }
        public String icon() { return icon; }
        public String title() { return title; }
        public String lessonCount() { return lessonCount; }
        public String description() { return description; }
        public List<String> keyHighlights() { return keyHighlights; }
        public String realWorldValue() { return realWorldValue; }
        public String defaultVideoTitle() { return defaultVideoTitle; }

        public String shortDescription() {
            return switch (moduleKey.toLowerCase(Locale.ROOT)) {
                case "flutter" -> "Build native Android and iOS apps from a single codebase with Flutter widgets, reactive layouts, and Dart.";
                case "reactnative" -> "Develop high-performance mobile applications using React, Expo Go tooling, native components, and Flexbox.";
                case "kotlin" -> "Create modern native Android applications with Kotlin, Jetpack Compose declarative UI, ViewModels, and Coroutines.";
                case "swift" -> "Design beautiful Apple apps with Swift, SwiftUI reactive views, SF Symbols, state binding, and Xcode architecture.";
                case "statemgmt" -> "Master scalable application state management using BLoC, Riverpod, Redux, and Clean Architecture patterns.";
                case "mobileapi" -> "Integrate cloud backends with RESTful HTTP clients, Firestore real-time databases, and Firebase Auth.";
                case "sqlite" -> "Persist offline data safely with SQLite databases, Room DAOs, encrypted key-value stores, and Realm.";
                case "publish" -> "Prepare release keystores, provision certificates, automate CI/CD builds, and publish to App Store and Google Play.";
                default -> description;
            };
        }

        public String getVideoUrl() {
            return VIDEO_URL_OVERRIDES.getOrDefault(moduleKey, defaultVideoUrl);
        }

        public void setVideoUrl(String newUrl) {
            if (newUrl != null && !newUrl.isBlank()) {
                VIDEO_URL_OVERRIDES.put(moduleKey, newUrl.trim());
            }
        }
    }

    private static final List<AppDevTopic> TOPICS = List.of(
            new AppDevTopic(
                    "flutter",
                    "PHASE 1 • CROSS-PLATFORM FOUNDATIONS",
                    "📱",
                    "Flutter & Dart Cross-Platform Foundations",
                    "4 Lessons",
                    "Build native, high-performance mobile applications for Android and iOS from a single Dart codebase. Master Stateless and Stateful widgets, the widget tree, Material 3, Cupertino styling, and hot reload workflow.",
                    List.of("Dart 3 Fundamentals", "Stateless & Stateful", "Widget Tree Layouts", "Hot Reload Dev", "Material 3 & Cupertino"),
                    "Google's flagship framework powering cross-platform apps at BMW, Alibaba, and Google Pay.",
                    "Flutter Course for Beginners - 37 Hours",
                    "https://www.youtube.com/watch?v=VPvVD8t02U8"
            ),
            new AppDevTopic(
                    "reactnative",
                    "PHASE 2 • HYBRID MOBILE DEVELOPMENT",
                    "⚛️",
                    "React Native & Expo Mobile Framework",
                    "4 Lessons",
                    "Leverage modern React and JavaScript knowledge to construct native iOS and Android apps. Master Expo Go tooling, native UI primitives, flexbox mobile layouts, touch gestures, and navigation stacks.",
                    List.of("Expo Tooling", "Native Core Components", "Mobile Flexbox", "Touchables & Gestures", "Navigation Stacks"),
                    "Created by Meta; powers mobile apps for Instagram, Discord, Shopify, and Pinterest.",
                    "React Native Tutorial for Beginners",
                    "https://www.youtube.com/watch?v=0-S5a0eXPoc"
            ),
            new AppDevTopic(
                    "kotlin",
                    "PHASE 3 • MODERN ANDROID ENGINEERING",
                    "🤖",
                    "Native Android: Kotlin & Jetpack Compose",
                    "4 Lessons",
                    "The modern industry standard for Android app development. Build declarative user interfaces with Jetpack Compose, asynchronous Kotlin Coroutines, ViewModel architecture, and Material Design 3.",
                    List.of("Kotlin Syntax & Coroutines", "Compose Declarative UI", "ViewModels & StateFlow", "Material Design 3", "Navigation & Intents"),
                    "Recommended by Google; powers the world's most performant native Android mobile experiences.",
                    "Android Development for Beginners - Kotlin Course",
                    "https://www.youtube.com/watch?v=FjrKMcnKahY"
            ),
            new AppDevTopic(
                    "swift",
                    "PHASE 4 • MODERN APPLE PLATFORMS",
                    "🍏",
                    "Native iOS: Swift & SwiftUI Architecture",
                    "4 Lessons",
                    "Build elegant, high-performance applications for iPhone, iPad, and Apple ecosystem. Master modern Swift, SwiftUI declarative views, @State and @Binding reactivity, SF Symbols, and Xcode preview tooling.",
                    List.of("Swift Modern Syntax", "SwiftUI View Hierarchy", "@State & @Binding", "NavigationSplitView", "SwiftData & Previews"),
                    "Apple's official native platform for iOS, iPadOS, watchOS, and macOS engineering.",
                    "iOS & SwiftUI Tutorial for Beginners",
                    "https://www.youtube.com/watch?v=b1oC7sLIgpI"
            ),
            new AppDevTopic(
                    "statemgmt",
                    "PHASE 5 • ARCHITECTURE & STATE PATTERNS",
                    "⚡",
                    "Mobile State Management & Architecture",
                    "3 Lessons",
                    "Architect production-grade mobile applications with predictable state flows. Master the BLoC (Business Logic Component) pattern, Riverpod providers, Redux state pipelines, and Clean Architecture layer separation.",
                    List.of("BLoC Event Streams", "Riverpod Providers", "Clean Architecture", "Dependency Injection", "Reactive Pipelines"),
                    "Essential for building testable, scalable apps that handle complex user interactions and background tasks.",
                    "Flutter Bloc & State Management Masterclass",
                    "https://www.youtube.com/watch?v=oxeYeMHVLII"
            ),
            new AppDevTopic(
                    "mobileapi",
                    "PHASE 6 • BACKEND INTEGRATION & CLOUD",
                    "🔥",
                    "Networking, REST APIs & Firebase Backend",
                    "3 Lessons",
                    "Connect mobile applications to real-world cloud services. Learn HTTP networking with Dio and Retrofit, JSON parsing, Firebase Authentication, Cloud Firestore real-time databases, and Push Notifications (FCM).",
                    List.of("HTTP & Dio / Retrofit", "JSON Deserialization", "Firebase Auth & Firestore", "Push Notifications (FCM)", "Offline Sync"),
                    "Enables user sign-in, remote databases, real-time messaging, and live content syncing.",
                    "Firebase & REST APIs for Mobile Apps",
                    "https://www.youtube.com/watch?v=sfA3NWDBPZ8"
            ),
            new AppDevTopic(
                    "sqlite",
                    "PHASE 7 • OFFLINE PERSISTENCE",
                    "🗄️",
                    "Local Mobile Storage: SQLite, Room & Realm",
                    "3 Lessons",
                    "Deliver flawless offline-first mobile experiences. Learn local SQLite database queries, Android Room ORM, iOS SwiftData/Core Data, encrypted key-value stores, and seamless cloud synchronization.",
                    List.of("SQLite Foundations", "Room DAOs & Entities", "Encrypted SharedPreferences", "Realm / SwiftData", "Data Migrations"),
                    "Guarantees that mobile apps continue to work seamlessly even without network connectivity.",
                    "Room & SQLite Local Database Masterclass",
                    "https://www.youtube.com/watch?v=lwA_Fh_hL7I"
            ),
            new AppDevTopic(
                    "publish",
                    "PHASE 8 • PRODUCTION CI/CD & RELEASE",
                    "📲",
                    "CI/CD, App Store & Google Play Deployment",
                    "3 Lessons",
                    "Prepare, sign, and ship mobile applications to production app stores. Master Android app bundles (AAB), release keystores, iOS distribution certificates, TestFlight beta testing, and Fastlane CI/CD automation.",
                    List.of("Android Keystores & AAB", "iOS Certificates & Provisioning", "TestFlight Beta Testing", "Fastlane CI/CD Automation", "Play Console Submission"),
                    "Navigates review guidelines, deployment pipelines, and global store releases to reach millions of users.",
                    "Deploying to App Store & Google Play Guide",
                    "https://www.youtube.com/watch?v=H7435fS8Zyo"
            )
    );

    public static String extractYouTubeVideoId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        url = url.trim();
        if (url.contains("v=")) {
            int idx = url.indexOf("v=");
            int end = url.indexOf('&', idx);
            return end == -1 ? url.substring(idx + 2) : url.substring(idx + 2, end);
        } else if (url.contains("youtu.be/")) {
            int idx = url.indexOf("youtu.be/");
            int end = url.indexOf('?', idx);
            return end == -1 ? url.substring(idx + 9) : url.substring(idx + 9, end);
        } else if (url.contains("/embed/")) {
            int idx = url.indexOf("/embed/");
            int end = url.indexOf('?', idx);
            return end == -1 ? url.substring(idx + 7) : url.substring(idx + 7, end);
        }
        return null;
    }

    public static String getThumbnailUrl(String videoUrl) {
        String id = extractYouTubeVideoId(videoUrl);
        if (id != null && !id.isBlank()) {
            return "https://img.youtube.com/vi/" + id + "/mqdefault.jpg";
        }
        return null;
    }

    public static AppDevTopic getTopicFor(String text) {
        if (text == null || text.isBlank()) {
            return TOPICS.get(0);
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("flutter") || lower.contains("dart")) return getTopicByModuleKey("flutter");
        if (lower.contains("react") || lower.contains("native") || lower.contains("expo")) return getTopicByModuleKey("reactnative");
        if (lower.contains("kotlin") || lower.contains("compose") || lower.contains("android")) return getTopicByModuleKey("kotlin");
        if (lower.contains("swift") || lower.contains("ios") || lower.contains("apple")) return getTopicByModuleKey("swift");
        if (lower.contains("state") || lower.contains("bloc") || lower.contains("riverpod") || lower.contains("redux")) return getTopicByModuleKey("statemgmt");
        if (lower.contains("api") || lower.contains("firebase") || lower.contains("http") || lower.contains("cloud")) return getTopicByModuleKey("mobileapi");
        if (lower.contains("sqlite") || lower.contains("room") || lower.contains("storage") || lower.contains("realm")) return getTopicByModuleKey("sqlite");
        if (lower.contains("publish") || lower.contains("deploy") || lower.contains("store") || lower.contains("fastlane")) return getTopicByModuleKey("publish");
        return TOPICS.get(0);
    }

    public static AppDevTopic getTopicByModuleKey(String moduleKey) {
        if (moduleKey != null) {
            for (AppDevTopic topic : TOPICS) {
                if (topic.moduleKey().equalsIgnoreCase(moduleKey)) {
                    return topic;
                }
            }
        }
        return TOPICS.get(0);
    }

    public static String getVideoUrlFor(String text) {
        AppDevTopic topic = getTopicFor(text);
        return topic != null ? topic.getVideoUrl() : getTopicVideoUrl("flutter");
    }

    public static String getTopicVideoUrl(String moduleKey) {
        for (AppDevTopic topic : TOPICS) {
            if (topic.moduleKey().equalsIgnoreCase(moduleKey)) {
                return topic.getVideoUrl();
            }
        }
        return "https://www.youtube.com";
    }

    public static void openUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url.trim()));
                return;
            }
        } catch (Throwable ignored) {}
        try {
            Runtime.getRuntime().exec(new String[]{"open", url.trim()});
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void copyToClipboard(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text.trim());
            clipboard.setContent(content);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Stage openWindowInstance = null;

    public static void show(Window owner, boolean isDarkTheme, Consumer<String> onSelectModule) {
        if (openWindowInstance != null && openWindowInstance.isShowing()) {
            openWindowInstance.toFront();
            openWindowInstance.requestFocus();
            return;
        }

        Stage stage = new Stage();
        openWindowInstance = stage;
        // Non-modal independent window (NOT a modal pop-up dialog)
        stage.initModality(Modality.NONE);
        stage.setTitle("App Development Bootcamp — Complete Curriculum & Video Lectures");
        stage.setWidth(1360);
        stage.setHeight(880);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);

        stage.setOnHidden(e -> {
            if (openWindowInstance == stage) {
                openWindowInstance = null;
            }
        });

        BorderPane root = new BorderPane();
        root.getStyleClass().add("webdev-roadmap-window");
        if (isDarkTheme) {
            root.getStyleClass().add("dark-theme");
        }

        // 1. Top Toolbar (Matching Dashboard with top-left logo and top-right back button)
        HBox topToolbar = buildTopToolbar(stage, owner);
        root.setTop(topToolbar);

        // 2. Left Sidebar with To-Do List (App Dev Only)
        VBox sidebar = buildAppDevSidebar(stage, onSelectModule);
        root.setLeft(sidebar);

        // 3. Center Scrollable Curriculum Area
        VBox centerBox = new VBox(0);
        centerBox.getStyleClass().add("page-content");
        centerBox.setStyle("-fx-padding: 0;");

        VBox headerBanner = buildHeaderBanner();

        GridPane grid = new GridPane();
        grid.getStyleClass().add("webdev-roadmap-grid");
        grid.setHgap(18);
        grid.setVgap(20);
        grid.setPadding(new Insets(16, 28, 28, 28));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(100.0 / 3.0);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(100.0 / 3.0);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(100.0 / 3.0);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        for (int i = 0; i < TOPICS.size(); i++) {
            AppDevTopic topic = TOPICS.get(i);
            VBox card = buildTopicCard(topic, stage, onSelectModule);
            int column = i % 3;
            int row = i / 3;
            grid.add(card, column, row);
        }

        centerBox.getChildren().addAll(headerBanner, grid);

        ScrollPane scrollPane = new ScrollPane(centerBox);
        scrollPane.getStyleClass().add("webdev-roadmap-scroll");
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);

        // 4. Footer Bar
        HBox footerBar = buildFooterBar(stage, onSelectModule);
        root.setBottom(footerBar);

        Scene scene = new Scene(root, 1360, 880);
        var cssRes = AppDevTopicsWindow.class.getResource("/resources/css/application.css");
        if (cssRes != null) {
            scene.getStylesheets().add(cssRes.toExternalForm());
        }
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private static HBox buildTopToolbar(Stage stage, Window owner) {
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("app-toolbar");
        topBar.setPadding(new Insets(10, 20, 10, 20));

        StackPane brandMark = new StackPane();
        brandMark.getStyleClass().add("brand-mark");
        brandMark.setMinSize(36, 36);
        brandMark.setPrefSize(36, 36);
        brandMark.setMaxSize(36, 36);

        var logoUrl = AppDevTopicsWindow.class.getResource("/resources/images/logo-icon.png");
        if (logoUrl != null) {
            try {
                ImageView logoView = new ImageView(new Image(logoUrl.toExternalForm()));
                logoView.setFitWidth(28);
                logoView.setFitHeight(28);
                logoView.setPreserveRatio(true);
                logoView.setSmooth(true);
                brandMark.getChildren().add(logoView);
            } catch (Throwable ignored) {}
        }

        HBox brandTitleBox = new HBox(0);
        brandTitleBox.setAlignment(Pos.CENTER_LEFT);

        Label codeLabel = new Label("Code");
        codeLabel.getStyleClass().add("toolbar-title");
        codeLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: 800;");

        Label trailLabel = new Label("Trail");
        trailLabel.getStyleClass().add("brand-title-accent");
        trailLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #0089fc;");

        brandTitleBox.getChildren().addAll(codeLabel, trailLabel);

        Label badge = new Label("APP DEVELOPMENT");
        badge.getStyleClass().add("role-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button();
        backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3px; -fx-background-radius: 999px;");
        var backIconUrl = AppDevTopicsWindow.class.getResource("/resources/images/icon-back-arrow.png");
        if (backIconUrl != null) {
            try {
                ImageView backIconView = new ImageView(new Image(backIconUrl.toExternalForm()));
                backIconView.setFitWidth(30);
                backIconView.setFitHeight(30);
                backIconView.setPreserveRatio(true);
                backIconView.setSmooth(true);
                backBtn.setGraphic(backIconView);
            } catch (Throwable ignored) {}
        }
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: rgba(0, 137, 252, 0.2); -fx-cursor: hand; -fx-padding: 3px; -fx-background-radius: 999px;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3px; -fx-background-radius: 999px;"));
        Tooltip.install(backBtn, new Tooltip("Back to Dashboard"));
        backBtn.setOnAction(e -> {
            stage.close();
            if (owner instanceof Stage ownerStage) {
                ownerStage.toFront();
                ownerStage.requestFocus();
            }
        });

        topBar.getChildren().addAll(brandMark, brandTitleBox, badge, spacer, backBtn);
        return topBar;
    }

    private static VBox buildAppDevSidebar(Stage stage, Consumer<String> onSelectModule) {
        VBox sidebar = new VBox(12);
        sidebar.getStyleClass().add("curriculum-sidebar");
        sidebar.setPrefWidth(290);
        sidebar.setMinWidth(270);
        sidebar.setMaxWidth(310);
        sidebar.setPadding(new Insets(18, 16, 18, 18));

        Label eyebrow = new Label("CURRICULUM");
        eyebrow.getStyleClass().add("section-eyebrow");

        HBox navRow = new HBox(6);
        Button allModulesBtn = new Button("All Modules (8)");
        allModulesBtn.setStyle("-fx-background-color: #0089fc; -fx-text-fill: #ffffff; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8px 12px;");
        allModulesBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(allModulesBtn, Priority.ALWAYS);

        Button statsBtn = new Button("28 Lessons");
        statsBtn.getStyleClass().add("sidebar-action-secondary");
        statsBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statsBtn, Priority.ALWAYS);
        navRow.getChildren().addAll(allModulesBtn, statsBtn);

        // Continue / To Do Box (App Dev Only)
        VBox todoBox = new VBox(8);
        todoBox.getStyleClass().add("continue-sidebar-box");

        HBox todoHeader = new HBox();
        todoHeader.setAlignment(Pos.CENTER_LEFT);

        Label todoTitle = new Label("TO DO LIST");
        todoTitle.getStyleClass().add("continue-sidebar-header");

        Region todoSpacer = new Region();
        HBox.setHgrow(todoSpacer, Priority.ALWAYS);

        Label trackLabel = new Label("App Dev");
        trackLabel.getStyleClass().add("continue-course-title");

        todoHeader.getChildren().addAll(todoTitle, todoSpacer, trackLabel);

        VBox itemsBox = new VBox(4);
        for (int i = 0; i < TOPICS.size(); i++) {
            AppDevTopic topic = TOPICS.get(i);

            Button itemBtn = new Button();
            itemBtn.getStyleClass().add("continue-item-button");
            itemBtn.setMaxWidth(Double.MAX_VALUE);

            Label badge = new Label(i == 0 ? "▶ UP NEXT" : "TO DO");
            badge.getStyleClass().add(i == 0 ? "continue-badge-completed" : "continue-badge-todo");

            Node iconNode = getTopicSmallIconNode(topic);

            Label title = new Label(topic.title());
            title.getStyleClass().add("continue-lesson-title");
            title.setWrapText(false);
            title.setMaxWidth(130);
            HBox.setHgrow(title, Priority.ALWAYS);

            Label count = new Label(topic.lessonCount());
            count.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: 700;");

            HBox row = new HBox(6, badge, iconNode, title, count);
            row.setAlignment(Pos.CENTER_LEFT);
            itemBtn.setGraphic(row);

            itemBtn.setOnAction(e -> {
                stage.close();
                if (onSelectModule != null) {
                    onSelectModule.accept(topic.moduleKey());
                }
            });

            itemsBox.getChildren().add(itemBtn);
        }

        Button quickStartBtn = new Button("Resume: Flutter (Lesson 1) ▶");
        quickStartBtn.getStyleClass().add("start-learning-button");
        quickStartBtn.setMaxWidth(Double.MAX_VALUE);
        quickStartBtn.setOnAction(e -> {
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept("flutter");
            }
        });

        todoBox.getChildren().addAll(todoHeader, itemsBox, quickStartBtn);

        ScrollPane todoScroll = new ScrollPane(todoBox);
        todoScroll.setFitToWidth(true);
        todoScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        todoScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(todoScroll, Priority.ALWAYS);

        Label status = new Label("8 Modules • 28 Lessons • 8 Curated Videos");
        status.getStyleClass().add("sidebar-status");

        sidebar.getChildren().addAll(eyebrow, navRow, todoScroll, status);
        return sidebar;
    }

    private static VBox buildHeaderBanner() {
        VBox banner = new VBox(6);
        banner.getStyleClass().add("webdev-header-banner");
        banner.setPadding(new Insets(18, 28, 12, 28));

        Label title = new Label("App Development Course Curriculum & Video Lectures");
        title.getStyleClass().add("webdev-header-title");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 900;");

        banner.getChildren().add(title);
        return banner;
    }

    private static Node getTopicIconNode(AppDevTopic topic) {
        String imageResource = getTopicImageResource(topic.moduleKey());
        if (imageResource != null) {
            try {
                var res = AppDevTopicsWindow.class.getResource(imageResource);
                if (res != null) {
                    ImageView iv = new ImageView(new Image(res.toExternalForm()));
                    iv.setFitWidth(24);
                    iv.setFitHeight(24);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    return iv;
                }
            } catch (Throwable ignored) {}
        }

        Label emojiLabel = new Label(topic.icon());
        emojiLabel.setStyle("-fx-font-size: 18px;");
        return emojiLabel;
    }

    private static Node getTopicSmallIconNode(AppDevTopic topic) {
        String imageResource = getTopicImageResource(topic.moduleKey());
        if (imageResource != null) {
            try {
                var res = AppDevTopicsWindow.class.getResource(imageResource);
                if (res != null) {
                    ImageView iv = new ImageView(new Image(res.toExternalForm()));
                    iv.setFitWidth(16);
                    iv.setFitHeight(16);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    return iv;
                }
            } catch (Throwable ignored) {}
        }

        Label emojiLabel = new Label(topic.icon());
        emojiLabel.setStyle("-fx-font-size: 13px;");
        return emojiLabel;
    }

    private static String getTopicImageResource(String moduleKey) {
        return switch (moduleKey.toLowerCase(Locale.ROOT)) {
            case "flutter" -> "/resources/images/icon-flutter.png";
            case "reactnative" -> "/resources/images/icon-react-logo.png";
            case "kotlin" -> "/resources/images/icon-kotlin.png";
            case "swift" -> "/resources/images/icon-swift.png";
            case "statemgmt" -> "/resources/images/icon-redux.png";
            case "mobileapi" -> "/resources/images/icon-firebase.png";
            case "sqlite" -> "/resources/images/icon-sqlite.png";
            case "publish" -> "/resources/images/icon-android.png";
            default -> null;
        };
    }

    private static VBox buildTopicCard(AppDevTopic topic, Stage stage, Consumer<String> onSelectModule) {
        VBox card = new VBox();
        card.getStyleClass().add("network-topic-card");
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setVgrow(card, Priority.ALWAYS);

        // 1. TOP VIDEO THUMBNAIL CONTAINER - True 16:9 Aspect Ratio
        StackPane thumbnailPane = new StackPane();
        thumbnailPane.getStyleClass().add("topic-card-image");
        thumbnailPane.setStyle("-fx-background-color: #0b1319;");
        thumbnailPane.minHeightProperty().bind(card.widthProperty().multiply(9.0 / 16.0));
        thumbnailPane.prefHeightProperty().bind(card.widthProperty().multiply(9.0 / 16.0));
        thumbnailPane.maxHeightProperty().bind(card.widthProperty().multiply(9.0 / 16.0));

        ImageView thumbView = new ImageView();
        thumbView.fitWidthProperty().bind(thumbnailPane.widthProperty());
        thumbView.fitHeightProperty().bind(thumbnailPane.heightProperty());
        thumbView.setPreserveRatio(true);
        thumbView.setSmooth(true);
        StackPane.setAlignment(thumbView, Pos.CENTER);

        String thumbUrl = getThumbnailUrl(topic.getVideoUrl());
        if (thumbUrl != null) {
            try {
                Image img = new Image(thumbUrl, true);
                thumbView.setImage(img);
            } catch (Throwable ignored) {}
        }

        // Sleek subtle play badge in center
        StackPane playCircle = new StackPane();
        playCircle.getStyleClass().add("webdev-thumbnail-play-circle");
        Label playGlyph = new Label("▶");
        playGlyph.getStyleClass().add("webdev-thumbnail-play-glyph");
        playCircle.getChildren().add(playGlyph);
        playCircle.setMouseTransparent(true);
        StackPane.setAlignment(playCircle, Pos.CENTER);

        // Lesson count badge top-right
        Label lessonBadge = new Label(topic.lessonCount());
        lessonBadge.getStyleClass().add("webdev-card-badge");
        StackPane.setAlignment(lessonBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(lessonBadge, new Insets(8, 8, 0, 0));

        thumbnailPane.getChildren().addAll(thumbView, playCircle, lessonBadge);

        // 2. CARD BODY: Title with Icon, Small Description, and Start Learning Button
        Label titleLabel = new Label(topic.title());
        titleLabel.getStyleClass().add("topic-card-title");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Node iconNode = getTopicIconNode(topic);
        HBox titleRow = new HBox(8, iconNode, titleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label descLabel = new Label(topic.shortDescription());
        descLabel.getStyleClass().add("topic-card-description");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(56.0);
        VBox.setVgrow(descLabel, Priority.ALWAYS);

        Button startButton = new Button("Start learning");
        startButton.getStyleClass().add("start-learning-button");
        HBox.setHgrow(startButton, Priority.ALWAYS);
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(event -> {
            event.consume();
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept(topic.moduleKey());
            }
        });

        Button exercisesButton = new Button("Exercises");
        exercisesButton.getStyleClass().add("topic-card-exercise-btn");
        exercisesButton.setOnAction(event -> {
            event.consume();
            boolean isDark = stage.getScene() != null && stage.getScene().getRoot().getStyleClass().contains("dark-theme");
            DsaProblemArenaWindow.show(stage, isDark, topic.moduleKey(), null, getLanguageForModule(topic.moduleKey()));
        });

        HBox btnRow = new HBox(6, startButton, exercisesButton);
        btnRow.setAlignment(Pos.CENTER);

        VBox body = new VBox(8.0, titleRow, descLabel, btnRow);
        body.getStyleClass().add("topic-card-body");
        VBox.setVgrow(body, Priority.ALWAYS);

        card.getChildren().addAll(thumbnailPane, body);

        // Smooth 14px rounded corners clipping the top thumbnail and bottom body
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28.0);
        clip.setArcHeight(28.0);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        card.setClip(clip);

        // Clicking anywhere on card triggers start learning
        card.setOnMouseClicked(event -> {
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept(topic.moduleKey());
            }
        });
        card.setCursor(Cursor.HAND);

        // Context menu for link actions (copy/customize video URL)
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("📋 Copy Video URL");
        copyItem.setOnAction(e -> copyToClipboard(topic.getVideoUrl()));
        MenuItem changeItem = new MenuItem("🔗 Change Video Link...");
        changeItem.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(topic.getVideoUrl());
            dialog.initOwner(stage);
            dialog.setTitle("Custom Video URL");
            dialog.setHeaderText("Set custom video link for:\n" + topic.title());
            dialog.setContentText("Video URL:");
            dialog.showAndWait().ifPresent(newUrl -> {
                if (!newUrl.isBlank()) {
                    topic.setVideoUrl(newUrl);
                    String newThumb = getThumbnailUrl(topic.getVideoUrl());
                    if (newThumb != null) {
                        thumbView.setImage(new Image(newThumb, true));
                    }
                }
            });
        });
        contextMenu.getItems().addAll(copyItem, changeItem);
        card.setOnContextMenuRequested(e -> contextMenu.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private static HBox buildFooterBar(Stage stage, Consumer<String> onSelectModule) {
        HBox footer = new HBox(16);
        footer.getStyleClass().add("webdev-footer-bar");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 28, 12, 28));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button startFirstBtn = new Button("Start with Phase 1 (Flutter)");
        startFirstBtn.getStyleClass().add("webdev-footer-start-first-btn");
        startFirstBtn.setOnAction(e -> {
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept("flutter");
            }
        });

        footer.getChildren().addAll(spacer, startFirstBtn);
        return footer;
    }

    public static ProgrammingLanguage getLanguageForModule(String moduleKey) {
        if (moduleKey == null) return ProgrammingLanguage.JAVA;
        return switch (moduleKey.toLowerCase(Locale.ROOT)) {
            case "reactnative", "mobileapi" -> ProgrammingLanguage.JAVASCRIPT;
            case "swift" -> ProgrammingLanguage.CPP;
            default -> ProgrammingLanguage.JAVA;
        };
    }
}
