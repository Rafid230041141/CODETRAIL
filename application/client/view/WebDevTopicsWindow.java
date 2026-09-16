package application.client.view;

import java.awt.Desktop;
import java.net.URI;
import application.client.dsa.judge.DsaProblemArenaWindow;
import application.client.dsa.judge.ProgrammingLanguage;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.FlowPane;
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
import javafx.util.Duration;

/**
 * Dedicated modal window displaying the full-stack Web Development curriculum
 * inspired by industry-standard Udemy courses with embedded video courses.
 *
 * Each topic card features curated video courses, direct "Watch Video" browser launch,
 * one-click link copying, customizable video links, and platform navigation.
 */
public class WebDevTopicsWindow {

    private static final Map<String, String> VIDEO_URL_OVERRIDES = new ConcurrentHashMap<>();

    public static class WebDevTopic {
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

        public WebDevTopic(
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
                case "html5" -> "Master semantic markup, modern forms, SEO principles, and accessibility to structure clean web applications.";
                case "css3" -> "Build responsive, pixel-perfect user interfaces with Flexbox, CSS Grid, media queries, and animations.";
                case "javascript" -> "Learn modern ES6+ syntax, asynchronous programming, DOM manipulation, and dynamic event handling.";
                case "react" -> "Construct modular single-page web applications with reusable functional components and React hooks.";
                case "node" -> "Engineer scalable server-side REST APIs, routing architectures, and middleware with Node.js and Express.";
                case "database" -> "Persist application data at scale using relational PostgreSQL tables and schema-free MongoDB collections.";
                case "auth" -> "Protect apps with robust password hashing, stateless JWT authentication, and modern web security best practices.";
                case "deploy" -> "Ship full-stack web apps into production with Docker containerization, CI/CD pipelines, and cloud hosting.";
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

    private static final List<WebDevTopic> TOPICS = List.of(
            new WebDevTopic(
                    "html5",
                    "PHASE 1 • FRONTEND FOUNDATION",
                    "🌐",
                    "HTML5 & Semantic Web Structure",
                    "3 Lessons",
                    "Master the architectural skeleton of modern web applications. Learn semantic markup, SEO best practices, document outlines, forms with client-side validation, accessibility (a11y), and media streaming.",
                    List.of("Semantic Elements", "Forms & Validation", "SEO & a11y", "Audio & Video", "DOM Structure"),
                    "Accessible markup powering every modern website & enterprise web application.",
                    "HTML & CSS Full Course - Beginner to Pro",
                    "https://www.youtube.com/watch?v=kUMe1FH4CHE"
            ),
            new WebDevTopic(
                    "css3",
                    "PHASE 2 • MODERN STYLING & DESIGN",
                    "🎨",
                    "Modern CSS3, Flexbox & CSS Grid",
                    "3 Lessons",
                    "Build responsive, pixel-perfect user interfaces. Master the CSS box model, 1D Flexbox for component navigation, 2D CSS Grid for complex layouts, media queries for mobile-first design, and animations.",
                    List.of("Box Model", "Flexbox 1D Layouts", "CSS Grid 2D", "Responsive Breakpoints", "CSS Variables"),
                    "Creates polished, fluid UI layouts adapted to every screen size from mobile to 4K.",
                    "CSS Flexbox & CSS Grid Masterclass",
                    "https://www.youtube.com/watch?v=3elGSZSWTbM"
            ),
            new WebDevTopic(
                    "javascript",
                    "PHASE 3 • CLIENT-SIDE PROGRAMMING",
                    "⚡",
                    "JavaScript ES6+ & DOM Manipulation",
                    "4 Lessons",
                    "The core programming language of the web. Learn modern syntax, ES6+ features, arrow functions, DOM manipulation, asynchronous fetch APIs, promises, and browser event cycles.",
                    List.of("let/const & Scope", "Arrow Functions", "DOM Event Handling", "Async / Await", "Fetch API & JSON"),
                    "Drives interactive experiences, dynamic DOM updates, and client-side application state.",
                    "JavaScript Full Course - Beginner to Master",
                    "https://www.youtube.com/watch?v=EerdGm-ehJQ"
            ),
            new WebDevTopic(
                    "react",
                    "PHASE 4 • MODERN FRONTEND FRAMEWORKS",
                    "⚛️",
                    "React.js Modern Frontend Framework",
                    "4 Lessons",
                    "The industry-standard frontend library created by Meta. Build modular single-page apps (SPAs) with JSX, functional components, hooks (useState, useEffect), and reactive component state.",
                    List.of("JSX Architecture", "Functional Components", "useState & useEffect", "Component State", "SPA Routing"),
                    "Used by Meta, Netflix, Airbnb, and Stripe for enterprise reactive user interfaces.",
                    "React.js Full Course - Hooks, State & Components",
                    "https://www.youtube.com/watch?v=bMknfKXIFA8"
            ),
            new WebDevTopic(
                    "node",
                    "PHASE 5 • SERVER-SIDE ARCHITECTURE",
                    "🟢",
                    "Backend Engineering: Node.js & Express",
                    "4 Lessons",
                    "Build high-throughput backends using JavaScript on the server. Master Express routing, middleware pipelines, RESTful API design, JSON serialization, and error management.",
                    List.of("Node.js Runtime", "Express Routing", "Custom Middleware", "RESTful Architecture", "Error Handling"),
                    "Powers backend microservices, REST APIs, and high-concurrency cloud services.",
                    "Node.js & Express.js REST API Masterclass",
                    "https://www.youtube.com/watch?v=Oe421EPjeBE"
            ),
            new WebDevTopic(
                    "database",
                    "PHASE 6 • DATA PERSISTENCE & STORAGE",
                    "🗄️",
                    "Databases: PostgreSQL & MongoDB",
                    "3 Lessons",
                    "Store and manage enterprise data at scale. Learn relational schemas, foreign keys, and SQL queries with PostgreSQL alongside schema-less JSON document storage with MongoDB.",
                    List.of("PostgreSQL (SQL)", "Relational Schemas", "MongoDB (NoSQL)", "BSON Documents", "ACID & Indexing"),
                    "Provides durable persistence for user data, transaction ledgers, and catalog content.",
                    "PostgreSQL & MongoDB Database Tutorial",
                    "https://www.youtube.com/watch?v=qw--VYLpxG4"
            ),
            new WebDevTopic(
                    "auth",
                    "PHASE 7 • SECURITY & IDENTITY",
                    "🔒",
                    "Authentication, JWT & Web Security",
                    "3 Lessons",
                    "Secure production applications against vulnerabilities. Implement salted bcrypt password hashing, stateless JSON Web Tokens (JWT), CORS headers, and OWASP top 10 defenses.",
                    List.of("bcrypt Password Hashing", "JWT Bearer Tokens", "CORS Configuration", "OWASP Defenses", "Role Authorization"),
                    "Guarantees data privacy, user authentication, and secure client-server transactions.",
                    "User Authentication & JWT Security Masterclass",
                    "https://www.youtube.com/watch?v=mbsmsi7l3r4"
            ),
            new WebDevTopic(
                    "deploy",
                    "PHASE 8 • PRODUCTION DEVOPS & CLOUD",
                    "🚀",
                    "Full-Stack DevOps & Cloud Deployment",
                    "4 Lessons",
                    "Ship complete applications into production. Master environment configuration (.env), Docker containerization, CI/CD automated deployment pipelines, cloud hosting, and SSL.",
                    List.of("Environment Variables", "Docker Containers", "CI/CD Pipelines", "Cloud Platforms", "SSL / HTTPS"),
                    "Enables zero-downtime releases, scalable hosting, and automated delivery pipelines.",
                    "Docker & Cloud Deployment Full Course",
                    "https://www.youtube.com/watch?v=3c-iBn73dDE"
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

    public static WebDevTopic getTopicFor(String text) {
        if (text == null || text.isBlank()) {
            return TOPICS.get(0);
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("html")) return getTopicByModuleKey("html5");
        if (lower.contains("css")) return getTopicByModuleKey("css3");
        if (lower.contains("javascript") || lower.contains("js") || lower.contains("dom")) return getTopicByModuleKey("javascript");
        if (lower.contains("react")) return getTopicByModuleKey("react");
        if (lower.contains("node") || lower.contains("express")) return getTopicByModuleKey("node");
        if (lower.contains("database") || lower.contains("sql") || lower.contains("postgres") || lower.contains("mongo")) return getTopicByModuleKey("database");
        if (lower.contains("auth") || lower.contains("jwt") || lower.contains("security")) return getTopicByModuleKey("auth");
        if (lower.contains("deploy") || lower.contains("docker") || lower.contains("devops") || lower.contains("cloud")) return getTopicByModuleKey("deploy");
        return TOPICS.get(0);
    }

    public static WebDevTopic getTopicByModuleKey(String moduleKey) {
        if (moduleKey != null) {
            for (WebDevTopic topic : TOPICS) {
                if (topic.moduleKey().equalsIgnoreCase(moduleKey)) {
                    return topic;
                }
            }
        }
        return TOPICS.get(0);
    }

    public static String getVideoUrlFor(String text) {
        WebDevTopic topic = getTopicFor(text);
        return topic != null ? topic.getVideoUrl() : getTopicVideoUrl("html5");
    }

    public static String getTopicVideoUrl(String moduleKey) {
        for (WebDevTopic topic : TOPICS) {
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
        stage.setTitle("Web Development Bootcamp — Complete Curriculum & Video Lectures");
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

        // 2. Left Sidebar with To-Do List (Only related to Web Dev)
        VBox sidebar = buildWebDevSidebar(stage, onSelectModule);
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
            WebDevTopic topic = TOPICS.get(i);
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
        var cssRes = WebDevTopicsWindow.class.getResource("/resources/css/application.css");
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

        var logoUrl = WebDevTopicsWindow.class.getResource("/resources/images/logo-icon.png");
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

        Label badge = new Label("WEB DEVELOPMENT");
        badge.getStyleClass().add("role-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button();
        backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3px; -fx-background-radius: 999px;");
        var backIconUrl = WebDevTopicsWindow.class.getResource("/resources/images/icon-back-arrow.png");
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

    private static VBox buildWebDevSidebar(Stage stage, Consumer<String> onSelectModule) {
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

        // Continue / To Do Box (Web Dev Only)
        VBox todoBox = new VBox(8);
        todoBox.getStyleClass().add("continue-sidebar-box");

        HBox todoHeader = new HBox();
        todoHeader.setAlignment(Pos.CENTER_LEFT);

        Label todoTitle = new Label("TO DO LIST");
        todoTitle.getStyleClass().add("continue-sidebar-header");

        Region todoSpacer = new Region();
        HBox.setHgrow(todoSpacer, Priority.ALWAYS);

        Label trackLabel = new Label("Web Dev");
        trackLabel.getStyleClass().add("continue-course-title");

        todoHeader.getChildren().addAll(todoTitle, todoSpacer, trackLabel);

        VBox itemsBox = new VBox(4);
        for (int i = 0; i < TOPICS.size(); i++) {
            WebDevTopic topic = TOPICS.get(i);

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

        Button quickStartBtn = new Button("Resume: HTML5 (Lesson 1) ▶");
        quickStartBtn.getStyleClass().add("start-learning-button");
        quickStartBtn.setMaxWidth(Double.MAX_VALUE);
        quickStartBtn.setOnAction(e -> {
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept("html5");
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

        Label title = new Label("Web Development Course Curriculum & Video Lectures");
        title.getStyleClass().add("webdev-header-title");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 900;");

        banner.getChildren().add(title);
        return banner;
    }

    private static Node getTopicIconNode(WebDevTopic topic) {
        String imageResource = switch (topic.moduleKey().toLowerCase(Locale.ROOT)) {
            case "html5" -> "/resources/images/icon-html5-shield.png";
            case "css3" -> "/resources/images/icon-css3-shield.png";
            case "javascript" -> "/resources/images/icon-js-logo.png";
            case "react" -> "/resources/images/icon-react-logo.png";
            case "node" -> "/resources/images/icon-node-backend.png";
            case "database" -> "/resources/images/icon-db-logo.png";
            case "auth" -> "/resources/images/icon-auth-security.png";
            case "deploy" -> "/resources/images/icon-deploy-devops.png";
            default -> null;
        };

        if (imageResource != null) {
            try {
                var res = WebDevTopicsWindow.class.getResource(imageResource);
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

    private static Node getTopicSmallIconNode(WebDevTopic topic) {
        String imageResource = switch (topic.moduleKey().toLowerCase(Locale.ROOT)) {
            case "html5" -> "/resources/images/icon-html5-shield.png";
            case "css3" -> "/resources/images/icon-css3-shield.png";
            case "javascript" -> "/resources/images/icon-js-logo.png";
            case "react" -> "/resources/images/icon-react-logo.png";
            case "node" -> "/resources/images/icon-node-backend.png";
            case "database" -> "/resources/images/icon-db-logo.png";
            case "auth" -> "/resources/images/icon-auth-security.png";
            case "deploy" -> "/resources/images/icon-deploy-devops.png";
            default -> null;
        };

        if (imageResource != null) {
            try {
                var res = WebDevTopicsWindow.class.getResource(imageResource);
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

    private static VBox buildTopicCard(WebDevTopic topic, Stage stage, Consumer<String> onSelectModule) {
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
            DsaProblemArenaWindow.show(stage, isDark, topic.moduleKey(), null, ProgrammingLanguage.JAVASCRIPT);
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

        Button startFirstBtn = new Button("Start with Phase 1 (HTML5)");
        startFirstBtn.getStyleClass().add("webdev-footer-start-first-btn");
        startFirstBtn.setOnAction(e -> {
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept("html5");
            }
        });

        footer.getChildren().addAll(spacer, startFirstBtn);
        return footer;
    }
}
