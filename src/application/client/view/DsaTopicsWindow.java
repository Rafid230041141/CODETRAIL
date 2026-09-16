package application.client.view;

import java.awt.Desktop;
import java.net.URI;
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
 * Dedicated window displaying the Data Structures & Competitive Programming curriculum
 * with embedded curated video courses and dashboard-grade visual styling.
 */
public class DsaTopicsWindow {

    private static final Map<String, String> VIDEO_URL_OVERRIDES = new ConcurrentHashMap<>();

    public static class DsaTopic {
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

        public DsaTopic(
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
            return description;
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

    private static final List<DsaTopic> TOPICS = List.of(
            new DsaTopic(
                    "data structures",
                    "PHASE 1 • FUNDAMENTAL STRUCTURES",
                    "📦",
                    "Linear & Non-Linear Data Structures",
                    "15 Lessons",
                    "Master foundational memory structures: Contiguous arrays, linked chains, LIFO stacks, FIFO queues, heaps, hash maps, BSTs, and Disjoint Set Union.",
                    List.of("Array & Linked List", "Stack & Queue", "Hash Map & Heap", "BST & AVL Trees", "Trie & DSU"),
                    "Essential core of efficient systems, indexing engines, and technical interview problem solving.",
                    "Data Structures & Algorithms Course - FreeCodeCamp",
                    "https://www.youtube.com/watch?v=RBSGKlAvoiM"
            ),
            new DsaTopic(
                    "sorting algorithms",
                    "PHASE 2 • COMPARISON & DISTRIBUTION",
                    "🔄",
                    "Sorting Algorithms & Complexity Analysis",
                    "7 Lessons",
                    "Implement comparison and non-comparison sorting: Merge Sort, Quick Sort, Heap Sort, Counting Sort, Radix Sort, and Bucket Sort with detailed time-space trade-offs.",
                    List.of("Merge Sort O(NlogN)", "Quick Sort & Pivots", "Heap Sort in-place", "Counting & Radix Sort", "Stability & Invariants"),
                    "Orders collections for rapid logarithmic searches, database query optimization, and ranking pipelines.",
                    "Sorting Algorithms Full Masterclass",
                    "https://www.youtube.com/watch?v=PkJIc588flQ"
            ),
            new DsaTopic(
                    "searching",
                    "PHASE 3 • LOGARITHMIC SEARCH TECHNIQUES",
                    "🔍",
                    "Linear, Binary Search & Search on Answer",
                    "4 Lessons",
                    "Master halving search spaces: Linear scan, standard binary search on sorted sequences, lower/upper bounds, and monotonic binary search on answer spaces.",
                    List.of("Binary Search O(logN)", "Lower & Upper Bound", "Search on Answer", "Monotonic Feasibility", "Rotated Array Search"),
                    "Enables sub-linear query lookups, numerical root approximation, and optimization problem solving.",
                    "Binary Search Algorithms & Applications",
                    "https://www.youtube.com/watch?v=fDKIpRe8GW4"
            ),
            new DsaTopic(
                    "graphs",
                    "PHASE 4 • GRAPH TRAVERSAL & NETWORKS",
                    "🕸️",
                    "Graph Theory, Shortest Path & Connectivity",
                    "12 Lessons",
                    "Navigate complex networks: BFS, DFS, Dijkstra, Bellman-Ford, Floyd-Warshall, Kruskal & Prim MSTs, Topological Sort, and Tarjan Strongly Connected Components.",
                    List.of("BFS & DFS Traversal", "Dijkstra Shortest Path", "Bellman-Ford & Cycles", "Kruskal / Prim MST", "Tarjan SCC & Bridges"),
                    "Powers social graphs, road routing navigation (GPS), dependency compilation, and network topology.",
                    "Graph Algorithms for Technical Interviews",
                    "https://www.youtube.com/watch?v=tWVWeAqZ0WU"
            ),
            new DsaTopic(
                    "range queries",
                    "PHASE 5 • ADVANCED RANGE STRUCTURES",
                    "📊",
                    "Range Queries: Segment Trees & Fenwick Trees",
                    "7 Lessons",
                    "Answer dynamic range queries in O(logN): Prefix sums, Segment Trees with lazy propagation, Fenwick Binary Indexed Trees (BIT), Sparse Tables, and Sqrt Decomposition.",
                    List.of("Prefix Sum Arrays", "Segment Tree (Point/Range)", "Lazy Propagation", "Fenwick Tree (BIT)", "Sparse Table RMQ"),
                    "Crucial for high-frequency trading engines, GIS boundary lookups, and competitive programming contests.",
                    "Segment Trees & Range Queries Masterclass",
                    "https://www.youtube.com/watch?v=ZBHKZF5w4dU"
            ),
            new DsaTopic(
                    "algorithmic paradigms",
                    "PHASE 6 • CORE ALGORITHMIC PARADIGMS",
                    "🧠",
                    "Dynamic Programming, Greedy & Backtracking",
                    "5 Lessons",
                    "Solve complex combinatorial challenges: Divide and conquer, greedy choice properties, 1D/2D Dynamic Programming with memoization and tabulation, recursion, and backtracking.",
                    List.of("Divide & Conquer", "Greedy Approximations", "1D & 2D Dynamic Prog", "State Transitions", "Backtracking & Pruning"),
                    "Solves real-world optimization problems: knapsack allocation, sequencing, scheduling, and resource planning.",
                    "Dynamic Programming Masterclass",
                    "https://www.youtube.com/watch?v=oBt53YbR9Kk"
            ),
            new DsaTopic(
                    "string algorithms",
                    "PHASE 7 • PATTERN MATCHING & AUTOMATA",
                    "🔤",
                    "String Algorithms, KMP & Suffix Automata",
                    "4 Lessons",
                    "Process textual sequences at scale: Knuth-Morris-Pratt (KMP) failure function, Z-algorithm, Trie pattern matching, Suffix Arrays, and polynomial rolling string hashing.",
                    List.of("KMP Failure Function", "Z-Algorithm Matching", "Trie Multi-Pattern", "Rolling Hash", "Suffix Automata"),
                    "Drives full-text search engines, bioinformatics DNA matching, compiler tokenizers, and plagiarism checkers.",
                    "String Algorithms - KMP & Pattern Matching",
                    "https://www.youtube.com/watch?v=V5-7GzOfADQ"
            ),
            new DsaTopic(
                    "mathematics",
                    "PHASE 8 • DISCRETE MATH & NUMBER THEORY",
                    "📐",
                    "Discrete Mathematics & Number Theory for CP",
                    "5 Lessons",
                    "Harness mathematical guarantees: Prime sieves (Eratosthenes), Modular arithmetic, Euclidean GCD, Combinatorics (nCr with modular inverse), and 2D/3D computational geometry.",
                    List.of("Prime Sieve & Factors", "Modular Arithmetic & Inv", "Euclid & Extended GCD", "Combinatorics & nCr", "Computational Geometry"),
                    "Forms the cryptographic backbone of secure communication, game geometry, and algorithmic computation.",
                    "Number Theory & Mathematics for Competitive Programming",
                    "https://www.youtube.com/watch?v=1xNbjMdbjug"
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

    public static DsaTopic getTopicFor(String text) {
        if (text == null || text.isBlank()) {
            return TOPICS.get(0);
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("sort")) return getTopicByModuleKey("sorting algorithms");
        if (lower.contains("search")) return getTopicByModuleKey("searching");
        if (lower.contains("graph") || lower.contains("tree") || lower.contains("bfs") || lower.contains("dfs")) return getTopicByModuleKey("graphs");
        if (lower.contains("range") || lower.contains("segment") || lower.contains("fenwick")) return getTopicByModuleKey("range queries");
        if (lower.contains("paradigm") || lower.contains("dynamic") || lower.contains("dp") || lower.contains("greedy")) return getTopicByModuleKey("algorithmic paradigms");
        if (lower.contains("string") || lower.contains("kmp") || lower.contains("trie")) return getTopicByModuleKey("string algorithms");
        if (lower.contains("math") || lower.contains("number") || lower.contains("geometry")) return getTopicByModuleKey("mathematics");
        if (lower.contains("struct") || lower.contains("array") || lower.contains("stack") || lower.contains("queue") || lower.contains("heap")) return getTopicByModuleKey("data structures");
        return TOPICS.get(0);
    }

    public static DsaTopic getTopicByModuleKey(String moduleKey) {
        if (moduleKey != null) {
            for (DsaTopic topic : TOPICS) {
                if (topic.moduleKey().equalsIgnoreCase(moduleKey)) {
                    return topic;
                }
            }
        }
        return TOPICS.get(0);
    }

    public static String getVideoUrlFor(String text) {
        DsaTopic topic = getTopicFor(text);
        return topic != null ? topic.getVideoUrl() : getTopicVideoUrl(TOPICS.get(0).moduleKey());
    }

    public static String getTopicVideoUrl(String moduleKey) {
        for (DsaTopic topic : TOPICS) {
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
        stage.initModality(Modality.NONE);
        stage.setTitle("DSA & Competitive Programming — Complete Curriculum & Video Lectures");
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

        // 2. Left Sidebar with To-Do List
        VBox sidebar = buildSidebar(stage, onSelectModule);
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
            DsaTopic topic = TOPICS.get(i);
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
        var cssRes = DsaTopicsWindow.class.getResource("/resources/css/application.css");
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

        var logoUrl = DsaTopicsWindow.class.getResource("/resources/images/logo-icon.png");
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

        Label badge = new Label("DSA / COMPETITIVE PROGRAMMING");
        badge.getStyleClass().add("role-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button();
        backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3px; -fx-background-radius: 999px;");
        var backIconUrl = DsaTopicsWindow.class.getResource("/resources/images/icon-back-arrow.png");
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

    private static VBox buildSidebar(Stage stage, Consumer<String> onSelectModule) {
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

        Button statsBtn = new Button("59 Lessons");
        statsBtn.getStyleClass().add("sidebar-action-secondary");
        statsBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statsBtn, Priority.ALWAYS);
        navRow.getChildren().addAll(allModulesBtn, statsBtn);

        // Continue / To Do Box
        VBox todoBox = new VBox(8);
        todoBox.getStyleClass().add("continue-sidebar-box");

        HBox todoHeader = new HBox();
        todoHeader.setAlignment(Pos.CENTER_LEFT);

        Label todoTitle = new Label("TO DO LIST");
        todoTitle.getStyleClass().add("continue-sidebar-header");

        Region todoSpacer = new Region();
        HBox.setHgrow(todoSpacer, Priority.ALWAYS);

        Label trackLabel = new Label("DSA / CP");
        trackLabel.getStyleClass().add("continue-course-title");

        todoHeader.getChildren().addAll(todoTitle, todoSpacer, trackLabel);

        VBox itemsBox = new VBox(4);
        for (int i = 0; i < TOPICS.size(); i++) {
            DsaTopic topic = TOPICS.get(i);

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

        Button quickStartBtn = new Button("Resume: Data Structures (Lesson 1) ▶");
        quickStartBtn.getStyleClass().add("start-learning-button");
        quickStartBtn.setMaxWidth(Double.MAX_VALUE);
        quickStartBtn.setOnAction(e -> {
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept(TOPICS.get(0).moduleKey());
            }
        });

        todoBox.getChildren().addAll(todoHeader, itemsBox, quickStartBtn);

        ScrollPane todoScroll = new ScrollPane(todoBox);
        todoScroll.setFitToWidth(true);
        todoScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        todoScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(todoScroll, Priority.ALWAYS);

        Label status = new Label("8 Modules • 59 Lessons • 8 Curated Videos");
        status.getStyleClass().add("sidebar-status");

        sidebar.getChildren().addAll(eyebrow, navRow, todoScroll, status);
        return sidebar;
    }

    private static VBox buildHeaderBanner() {
        VBox banner = new VBox(6);
        banner.getStyleClass().add("webdev-header-banner");
        banner.setPadding(new Insets(18, 28, 12, 28));

        Label title = new Label("Data Structures & Algorithms Curriculum & Video Lectures");
        title.getStyleClass().add("webdev-header-title");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 900;");

        banner.getChildren().add(title);
        return banner;
    }

    private static Node getTopicIconNode(DsaTopic topic) {
        String imageResource = getTopicImageResource(topic.moduleKey());
        if (imageResource != null) {
            try {
                var res = DsaTopicsWindow.class.getResource(imageResource);
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

    private static Node getTopicSmallIconNode(DsaTopic topic) {
        String imageResource = getTopicImageResource(topic.moduleKey());
        if (imageResource != null) {
            try {
                var res = DsaTopicsWindow.class.getResource(imageResource);
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
            case "data structures" -> "/resources/images/icon-dsa-structures.png";
            case "sorting algorithms" -> "/resources/images/icon-dsa-sorting.png";
            case "searching" -> "/resources/images/icon-dsa-searching.png";
            case "graphs" -> "/resources/images/icon-dsa-graphs.png";
            case "range queries" -> "/resources/images/icon-dsa-range.png";
            case "algorithmic paradigms" -> "/resources/images/icon-dsa-dp.png";
            case "string algorithms" -> "/resources/images/icon-dsa-strings.png";
            case "mathematics" -> "/resources/images/icon-dsa-math.png";
            default -> null;
        };
    }

    private static VBox buildTopicCard(DsaTopic topic, Stage stage, Consumer<String> onSelectModule) {
        VBox card = new VBox();
        card.getStyleClass().add("network-topic-card");
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setVgrow(card, Priority.ALWAYS);

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

        StackPane playCircle = new StackPane();
        playCircle.getStyleClass().add("webdev-thumbnail-play-circle");
        Label playGlyph = new Label("▶");
        playGlyph.getStyleClass().add("webdev-thumbnail-play-glyph");
        playCircle.getChildren().add(playGlyph);
        playCircle.setMouseTransparent(true);
        StackPane.setAlignment(playCircle, Pos.CENTER);

        Label lessonBadge = new Label(topic.lessonCount());
        lessonBadge.getStyleClass().add("webdev-card-badge");
        StackPane.setAlignment(lessonBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(lessonBadge, new Insets(8, 8, 0, 0));

        thumbnailPane.getChildren().addAll(thumbView, playCircle, lessonBadge);

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
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(event -> {
            event.consume();
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept(topic.moduleKey());
            }
        });

        VBox body = new VBox(8.0, titleRow, descLabel, startButton);
        body.getStyleClass().add("topic-card-body");
        VBox.setVgrow(body, Priority.ALWAYS);

        card.getChildren().addAll(thumbnailPane, body);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(28.0);
        clip.setArcHeight(28.0);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        card.setClip(clip);

        card.setOnMouseClicked(event -> {
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept(topic.moduleKey());
            }
        });
        card.setCursor(Cursor.HAND);

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

        Button startFirstBtn = new Button("Start with Phase 1 (Data Structures)");
        startFirstBtn.getStyleClass().add("webdev-footer-start-first-btn");
        startFirstBtn.setOnAction(e -> {
            stage.close();
            if (onSelectModule != null) {
                onSelectModule.accept(TOPICS.get(0).moduleKey());
            }
        });

        footer.getChildren().addAll(spacer, startFirstBtn);
        return footer;
    }
}
