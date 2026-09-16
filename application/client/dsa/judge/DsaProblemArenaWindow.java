package application.client.dsa.judge;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class DsaProblemArenaWindow {

    private static Stage arenaStage;

    // Cache user code per problem ID + language ID
    private static final Map<String, String> CODE_CACHE = new HashMap<>();

    private final Stage stage;
    private final boolean isDark;

    private ComboBox<String> topicComboBox;
    private HBox problemTabsBox;
    private DsaProblem currentProblem;
    private ProgrammingLanguage currentLanguage = ProgrammingLanguage.CPP;

    private Label arenaTag;
    private VBox metaBox;
    private String currentTopicKey = "arrays";

    // Problem pane controls
    private Label problemTitleLabel;
    private Label difficultyBadge;
    private Label timeLimitLabel;
    private Label memoryLimitLabel;
    private VBox problemContentBox;

    // Editor controls
    private ComboBox<ProgrammingLanguage> languageComboBox;
    private CodeArea codeEditorArea;
    private TextArea lineNumberArea;
    private CodeCompletionPopup completionPopup;
    private int editorFontSize = 14;
    private Label editorFileTabLabel;
    private Label editorBreadcrumbFileLabel;
    private Label editorStatusLabel;
    private Label editorLangBadge;

    // Judge Console & Verdict controls
    private CheckBox customInputCheckBox;
    private TextArea customInputArea;
    private VBox customInputContainer;
    private Button runBtn;
    private Button submitBtn;
    private ProgressBar judgingProgressBar;
    private HBox verdictBanner;
    private Label verdictIconLabel;
    private Label verdictTitleLabel;
    private Label verdictStatsLabel;
    private VBox testResultsContainer;
    private ScrollPane testResultsScroll;
    private TextArea errorLogArea;

    public static final List<String> CANONICAL_TOPIC_KEYS = List.of(
            "arrays",
            "linked lists",
            "stacks",
            "queues",
            "hash maps",
            "heaps",
            "trees",
            "dsu",
            "trie",
            "sorting algorithms",
            "searching",
            "graphs",
            "range queries",
            "algorithmic paradigms",
            "string algorithms",
            "mathematics",
            "html5",
            "css3",
            "javascript",
            "react",
            "node",
            "database",
            "auth",
            "deploy",
            "flutter",
            "reactnative",
            "kotlin",
            "swift",
            "statemgmt",
            "mobileapi",
            "sqlite",
            "publish",
            "ml_foundations",
            "math_ai",
            "scikit",
            "deep_learning",
            "vision",
            "nlp",
            "genai",
            "mlops",
            "numpy",
            "pandas",
            "eda",
            "statistics",
            "feature_eng",
            "bigdata",
            "sql_analytics",
            "bi_dashboards",
            "math_games",
            "pygame",
            "unity_basics",
            "unity_3d",
            "unreal",
            "game_physics",
            "audio_vfx",
            "game_publish"
    );

    public static ProgrammingLanguage getDefaultLanguageForTopic(String topicKey) {
        if (topicKey == null) return ProgrammingLanguage.CPP;
        String key = topicKey.toLowerCase(Locale.ROOT).trim();
        String canonical = DsaProblemRepository.getCanonicalTopicKey(key);
        if (canonical != null) {
            key = canonical;
        }

        return switch (key) {
            // Web Development (JavaScript)
            case "html5", "css3", "javascript", "react", "node", "database", "auth", "deploy" -> ProgrammingLanguage.JAVASCRIPT;

            // AI / ML (Python)
            case "ml_foundations", "math_ai", "scikit", "deep_learning", "vision", "nlp", "genai", "mlops" -> ProgrammingLanguage.PYTHON;

            // Data Science (Python)
            case "numpy", "pandas", "eda", "statistics", "feature_eng", "bigdata", "sql_analytics", "bi_dashboards" -> ProgrammingLanguage.PYTHON;

            // App Development
            case "reactnative", "mobileapi" -> ProgrammingLanguage.JAVASCRIPT;
            case "swift" -> ProgrammingLanguage.CPP;
            case "flutter", "kotlin", "statemgmt", "sqlite", "publish" -> ProgrammingLanguage.JAVA;

            // Game Development
            case "pygame" -> ProgrammingLanguage.PYTHON;
            case "unity_basics", "unity_3d" -> ProgrammingLanguage.CSHARP;
            case "math_games", "unreal", "game_physics", "audio_vfx", "game_publish" -> ProgrammingLanguage.CPP;

            // Languages
            case "python" -> ProgrammingLanguage.PYTHON;
            case "java" -> ProgrammingLanguage.JAVA;
            case "cpp" -> ProgrammingLanguage.CPP;
            case "c" -> ProgrammingLanguage.C;
            case "csharp" -> ProgrammingLanguage.CSHARP;

            // DSA and default
            default -> ProgrammingLanguage.CPP;
        };
    }

    public static boolean isAlgorithmicTopic(String topicKey) {
        if (topicKey == null) return true;
        String key = topicKey.toLowerCase(Locale.ROOT).trim();
        String canonical = DsaProblemRepository.getCanonicalTopicKey(key);
        if (canonical != null) {
            key = canonical;
        }

        return switch (key) {
            case "html5", "css3", "javascript", "react", "node", "database", "auth", "deploy",
                 "flutter", "reactnative", "kotlin", "swift", "statemgmt", "mobileapi", "sqlite", "publish",
                 "ml_foundations", "math_ai", "scikit", "deep_learning", "vision", "nlp", "genai", "mlops",
                 "numpy", "pandas", "eda", "statistics", "feature_eng", "bigdata", "sql_analytics", "bi_dashboards",
                 "math_games", "pygame", "unity_basics", "unity_3d", "unreal", "game_physics", "audio_vfx", "game_publish" -> false;
            default -> true;
        };
    }

    public static String getPracticalStarterTemplate(String topicKey, ProgrammingLanguage lang) {
        if (topicKey == null || lang == null) return lang != null ? lang.starterTemplate() : "";
        String k = topicKey.toLowerCase(Locale.ROOT).trim();
        String canonical = DsaProblemRepository.getCanonicalTopicKey(k);
        if (canonical == null) canonical = k;

        // Web Development
        if (canonical.equals("html5")) {
            return """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Web Development Exercise</title>
                        <style>
                            body { font-family: system-ui, -apple-system, sans-serif; margin: 24px; line-height: 1.6; }
                            header, nav, main, section, article, footer { margin-bottom: 16px; padding: 12px; }
                        </style>
                    </head>
                    <body>
                        <!-- Build your semantic HTML5 solution here -->
                        <header>
                            <h1>Project Portfolio</h1>
                        </header>
                        <main>
                            <section>
                                <p>Welcome to the project. Complete the exercise specifications.</p>
                            </section>
                        </main>
                    </body>
                    </html>
                    """;
        }
        if (canonical.equals("css3")) {
            return """
                    /* Modern CSS3 Exercise */
                    :root {
                        --primary-color: #0284c7;
                        --bg-color: #f8fafc;
                        --text-color: #0f172a;
                    }

                    * {
                        box-sizing: border-box;
                        margin: 0;
                        padding: 0;
                    }

                    /* Write your styles, flexbox, or grid rules below */
                    """;
        }
        if (canonical.equals("javascript") || canonical.equals("react") || canonical.equals("node") ||
            canonical.equals("database") || canonical.equals("auth") || canonical.equals("deploy")) {
            if (canonical.equals("react")) {
                return """
                        import React, { useState, useEffect, useReducer } from 'react';

                        // React Component Solution
                        export default function App() {
                            const [state, setState] = useState(null);

                            return (
                                <div className="app-container">
                                    <h2>React Exercise Component</h2>
                                    {/* Implement your component logic here */}
                                </div>
                            );
                        }
                        """;
            }
            if (canonical.equals("node") || canonical.equals("auth")) {
                return """
                        const express = require('express');
                        const app = express();

                        app.use(express.json());

                        // Write your routes and middleware below
                        app.get('/api/health', (req, res) => {
                            res.json({ status: 'ok', timestamp: new Date().toISOString() });
                        });

                        // Start server or export app
                        module.exports = app;
                        """;
            }
            return """
                    // JavaScript Implementation
                    function solution() {
                        // Write your solution here
                        return true;
                    }

                    console.log('Solution output:', solution());
                    """;
        }

        // App Development
        if (canonical.equals("flutter") || canonical.equals("statemgmt") || canonical.equals("mobileapi") || canonical.equals("sqlite")) {
            return """
                    import 'package:flutter/material.dart';

                    void main() {
                        runApp(const MyApp());
                    }

                    class MyApp extends StatelessWidget {
                        const MyApp({super.key});

                        @override
                        Widget build(BuildContext context) {
                            return MaterialApp(
                                home: Scaffold(
                                    appBar: AppBar(title: const Text('Mobile Exercise')),
                                    body: const Center(
                                        child: Text('Build your widget here'),
                                    ),
                                ),
                            );
                        }
                    }
                    """;
        }
        if (canonical.equals("reactnative")) {
            return """
                    import React from 'react';
                    import { StyleSheet, Text, View, TouchableOpacity } from 'react-native';

                    export default function App() {
                        return (
                            <View style={styles.container}>
                                <Text style={styles.title}>React Native Component</Text>
                            </View>
                        );
                    }

                    const styles = StyleSheet.create({
                        container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
                        title: { fontSize: 20, fontWeight: 'bold' },
                    });
                    """;
        }
        if (canonical.equals("kotlin")) {
            return """
                    // Kotlin Android & Logic Solution
                    data class User(val id: String, val name: String)

                    fun main() {
                        // Implement your logic here
                        println("Kotlin exercise running...")
                    }
                    """;
        }
        if (canonical.equals("swift")) {
            return """
                    import Foundation

                    // Swift Solution
                    struct Item: Identifiable {
                        let id = UUID()
                        var name: String
                    }

                    func runSolution() {
                        // Implement your logic here
                        print("Swift exercise executing...")
                    }

                    runSolution()
                    """;
        }

        // AI/ML & Data Science
        if (canonical.equals("numpy") || canonical.equals("pandas") || canonical.equals("eda") ||
            canonical.equals("statistics") || canonical.equals("feature_eng") || canonical.equals("bigdata") ||
            canonical.equals("ml_foundations") || canonical.equals("math_ai") || canonical.equals("scikit") ||
            canonical.equals("deep_learning") || canonical.equals("vision") || canonical.equals("nlp") ||
            canonical.equals("genai") || canonical.equals("mlops")) {
            return """
                    import numpy as np
                    import pandas as pd

                    def solve():
                        # Write your data science / ML pipeline code here
                        print("Pipeline executed successfully.")

                    if __name__ == '__main__':
                        solve()
                    """;
        }
        if (canonical.equals("sql_analytics")) {
            return """
                    -- SQL Analytics Query
                    SELECT
                        category,
                        COUNT(*) AS total_count,
                        AVG(amount) AS average_amount
                    FROM transactions
                    GROUP BY category
                    ORDER BY total_count DESC;
                    """;
        }

        // Game Dev
        if (canonical.equals("math_games") || canonical.equals("pygame")) {
            return """
                    import pygame
                    import sys

                    # Pygame / Game Logic Starter
                    def main():
                        pygame.init()
                        screen = pygame.display.set_mode((800, 600))
                        pygame.display.set_caption("Game Exercise")
                        clock = pygame.time.Clock()

                        running = True
                        while running:
                            for event in pygame.event.get():
                                if event.type == pygame.QUIT:
                                    running = False
                            screen.fill((30, 30, 30))
                            pygame.display.flip()
                            clock.tick(60)

                        pygame.quit()
                        sys.exit()

                    if __name__ == '__main__':
                        main()
                    """;
        }
        if (canonical.equals("unity_basics") || canonical.equals("unity_3d") || canonical.equals("game_physics")) {
            return """
                    using System;
                    using System.Collections.Generic;

                    public class GameExerciseSolution {
                        public static void Main(string[] args) {
                            // Game mechanics and simulation
                            Console.WriteLine("Game simulation ready.");
                        }
                    }
                    """;
        }

        return lang.starterTemplate();
    }

    public static void show(String initialTopicKey, String initialProblemId, ProgrammingLanguage initialLanguage) {
        show(null, false, initialTopicKey, initialProblemId, initialLanguage);
    }

    public static void show(javafx.stage.Window owner, boolean isDark, String initialTopicKey, String initialProblemId) {
        show(owner, isDark, initialTopicKey, initialProblemId, null);
    }

    public static void show(javafx.stage.Window owner, boolean isDark, String initialTopicKey, String initialProblemId, ProgrammingLanguage initialLanguage) {
        if (arenaStage != null) {
            arenaStage.toFront();
            arenaStage.requestFocus();
            return;
        }

        Stage stage = new Stage();
        arenaStage = stage;
        stage.initOwner(owner);
        stage.setTitle("CodeTrail DSA Arena & Online Judge");

        DsaProblemArenaWindow arena = new DsaProblemArenaWindow(stage, isDark, initialTopicKey, initialProblemId, initialLanguage);
        arena.initUi();

        stage.setOnHidden(e -> arenaStage = null);
        stage.show();
    }

    private final String initialTopicKey;
    private final String initialProblemId;
    private final ProgrammingLanguage initialLanguage;
    private boolean initializing = true;
    private boolean initialLoad = true;

    private DsaProblemArenaWindow(Stage stage, boolean isDark, String initialTopicKey, String initialProblemId, ProgrammingLanguage initialLanguage) {
        this.stage = stage;
        this.isDark = isDark;
        this.initialTopicKey = initialTopicKey;
        this.initialProblemId = initialProblemId;
        this.initialLanguage = initialLanguage;

        if (initialLanguage != null) {
            this.currentLanguage = initialLanguage;
        } else if (initialTopicKey != null) {
            this.currentLanguage = getDefaultLanguageForTopic(initialTopicKey);
        } else {
            this.currentLanguage = ProgrammingLanguage.CPP;
        }
    }

    private void initUi() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("arena-root");
        if (isDark) {
            root.getStyleClass().add("dark-theme");
        } else {
            root.getStyleClass().add("light-theme");
        }

        // Apply application CSS
        try {
            var cssRes = getClass().getResource("/resources/css/application.css");
            if (cssRes != null) {
                root.getStylesheets().add(cssRes.toExternalForm());
            }
        } catch (Throwable ignored) {}

        // Top Navigation & Toolbar
        VBox topHeader = buildTopHeader();
        root.setTop(topHeader);

        // Center SplitPane: Problem Statement (Left) vs Code Editor & Judge Console (Right)
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.48);

        Node leftProblemPane = buildLeftProblemPane();
        Node rightEditorPane = buildRightEditorPane();

        splitPane.getItems().addAll(leftProblemPane, rightEditorPane);
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1260, 840);
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(680);

        // Window icon
        try {
            var iconRes = getClass().getResource("/resources/images/logo-icon.png");
            if (iconRes != null) {
                stage.getIcons().add(new Image(iconRes.toExternalForm()));
            }
        } catch (Throwable ignored) {}

        if (languageComboBox != null) {
            languageComboBox.getSelectionModel().select(currentLanguage);
        }

        // Load initial topic and problems
        int targetTopicIdx = -1;
        if (initialTopicKey != null) {
            String canonical = DsaProblemRepository.getCanonicalTopicKey(initialTopicKey);
            if (canonical != null) {
                targetTopicIdx = CANONICAL_TOPIC_KEYS.indexOf(canonical);
            }
        } else {
            targetTopicIdx = 0;
        }

        if (targetTopicIdx >= 0 && targetTopicIdx < CANONICAL_TOPIC_KEYS.size()) {
            if (topicComboBox != null && targetTopicIdx < topicComboBox.getItems().size()) {
                topicComboBox.getSelectionModel().select(targetTopicIdx);
            }
            selectTopicByIndex(targetTopicIdx);
        } else {
            // Unmapped topic! Do NOT silently fall through to Arrays. Show honest empty state.
            showNoExercisesState(initialTopicKey);
        }
        initializing = false;
        initialLoad = false;
    }

    private VBox buildTopHeader() {
        VBox container = new VBox(6);
        container.getStyleClass().add("arena-header-container");
        container.setStyle("-fx-background-color: " + (isDark ? "#12191d" : "#ffffff") + "; -fx-border-color: " + (isDark ? "#243238" : "#e2e8f0") + "; -fx-border-width: 0 0 1 0;");

        // Upper Toolbar
        HBox upperToolbar = new HBox(16);
        upperToolbar.setAlignment(Pos.CENTER_LEFT);
        upperToolbar.setPadding(new Insets(12, 24, 6, 24));

        // Brand Logo & Title
        ImageView logoView = new ImageView();
        try {
            var res = getClass().getResource("/resources/images/logo-icon.png");
            if (res != null) {
                logoView.setImage(new Image(res.toExternalForm()));
                logoView.setFitWidth(26);
                logoView.setFitHeight(26);
                logoView.setPreserveRatio(true);
                logoView.setSmooth(true);
            }
        } catch (Throwable ignored) {}

        Label brandCode = new Label("Code");
        brandCode.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#ffffff" : "#0f172a") + ";");

        Label brandTrail = new Label("Trail");
        brandTrail.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #0089fc;");

        HBox brandBox = new HBox(3, logoView, brandCode, brandTrail);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        arenaTag = new Label("DSA COMPETITIVE ARENA");
        arenaTag.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: " + (isDark ? "#163640" : "#e0f2fe") + "; -fx-text-fill: #0089fc; -fx-padding: 4 10; -fx-background-radius: 6;");

        // Topic Selector Dropdown
        Label topicLabel = new Label("Topic:");
        topicLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        topicComboBox = new ComboBox<>();
        topicComboBox.getItems().addAll(
                "1. Arrays (5 Problems)",
                "2. Linked Lists (5 Problems)",
                "3. Stacks (5 Problems)",
                "4. Queues & Deques (5 Problems)",
                "5. Hash Maps & Sets (5 Problems)",
                "6. Heaps & Priority Queues (5 Problems)",
                "7. Trees & BST (5 Problems)",
                "8. Disjoint Set Union (5 Problems)",
                "9. Trie / Prefix Trees (5 Problems)",
                "10. Sorting Algorithms (5 Problems)",
                "11. Searching & Bounds (5 Problems)",
                "12. Graphs & Shortest Paths (5 Problems)",
                "13. Range Queries (Segment/BIT) (5 Problems)",
                "14. Algorithmic Paradigms (DP/Greedy) (5 Problems)",
                "15. String Algorithms (KMP/Z) (5 Problems)",
                "16. Mathematics & Number Theory (5 Problems)",
                "17. HTML5 & Semantic Web (5 Exercises)",
                "18. Modern CSS3 & Flexbox Grid (5 Exercises)",
                "19. JavaScript ES6+ & DOM (5 Exercises)",
                "20. React.js Framework (5 Exercises)",
                "21. Node.js & Express Backend (5 Exercises)",
                "22. Databases: PostgreSQL & MongoDB (5 Exercises)",
                "23. Authentication & Web Security (5 Exercises)",
                "24. DevOps & Cloud Deployment (5 Exercises)",
                "25. Flutter & Dart Mobile (5 Exercises)",
                "26. React Native & Expo (5 Exercises)",
                "27. Kotlin & Jetpack Compose (5 Exercises)",
                "28. Swift & SwiftUI iOS (5 Exercises)",
                "29. Mobile State Management (5 Exercises)",
                "30. Mobile Networking & Firebase (5 Exercises)",
                "31. Local SQLite & Room Storage (5 Exercises)",
                "32. App Store & Play Publishing (5 Exercises)",
                "33. ML Foundations & Supervised Learning (5 Exercises)",
                "34. Math & Linear Algebra for AI (5 Exercises)",
                "35. Scikit-learn & Feature Engineering (5 Exercises)",
                "36. Deep Learning & PyTorch (5 Exercises)",
                "37. Computer Vision & CNNs (5 Exercises)",
                "38. NLP & Transformers (5 Exercises)",
                "39. Generative AI & LLMs (5 Exercises)",
                "40. MLOps & Model Deployment (5 Exercises)",
                "41. NumPy & Numerical Computing (5 Exercises)",
                "42. Pandas Data Wrangling (5 Exercises)",
                "43. EDA & Data Visualization (5 Exercises)",
                "44. Statistics & Hypothesis Testing (5 Exercises)",
                "45. Feature Engineering & PCA (5 Exercises)",
                "46. Big Data with Apache Spark (5 Exercises)",
                "47. Advanced SQL Analytics (5 Exercises)",
                "48. BI & Streamlit Dashboards (5 Exercises)",
                "49. Game Math & Vectors (5 Exercises)",
                "50. 2D Game Dev with Pygame (5 Exercises)",
                "51. Unity Engine Foundations (5 Exercises)",
                "52. Unity 3D Lighting & Level Design (5 Exercises)",
                "53. Unreal Engine 5 & Blueprints (5 Exercises)",
                "54. Game Physics & Collisions (5 Exercises)",
                "55. Game Audio, Shaders & VFX (5 Exercises)",
                "56. Game Publishing & Optimization (5 Exercises)"
        );
        topicComboBox.getSelectionModel().select(0);
        topicComboBox.setStyle(
                "-fx-background-color: " + (isDark ? "#1a242c" : "#f8fafc") + ";" +
                "-fx-border-color: " + (isDark ? "#283740" : "#cbd5e1") + ";" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                "-fx-font-size: 12.5px; -fx-font-weight: 700;" +
                "-fx-pref-width: 280px; -fx-cursor: hand;"
        );
        topicComboBox.setOnAction(e -> {
            if (initializing) return;
            int idx = topicComboBox.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                selectTopicByIndex(idx);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Circular Back button matching DsaTopicsWindow
        Button backBtn = new Button();
        backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3px; -fx-background-radius: 999px;");
        var arrowRes = getClass().getResource("/resources/images/icon-back-arrow.png");
        if (arrowRes != null) {
            try {
                ImageView arrowIv = new ImageView(new Image(arrowRes.toExternalForm()));
                arrowIv.setFitWidth(28);
                arrowIv.setFitHeight(28);
                arrowIv.setPreserveRatio(true);
                arrowIv.setSmooth(true);
                backBtn.setGraphic(arrowIv);
            } catch (Throwable ignored) {}
        } else {
            backBtn.setText("←");
            backBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: 900;");
        }
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: rgba(0, 137, 252, 0.15); -fx-cursor: hand; -fx-padding: 3px; -fx-background-radius: 999px;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3px; -fx-background-radius: 999px;"));
        Tooltip.install(backBtn, new Tooltip("Return to Topics"));
        backBtn.setOnAction(e -> stage.close());

        upperToolbar.getChildren().addAll(brandBox, arenaTag, topicLabel, topicComboBox, spacer, backBtn);

        // Problem Tabs Row (5 Problems for current topic)
        problemTabsBox = new HBox(8);
        problemTabsBox.setAlignment(Pos.CENTER_LEFT);
        problemTabsBox.setPadding(new Insets(2, 4, 4, 4));

        ScrollPane tabsScroll = new ScrollPane(problemTabsBox);
        tabsScroll.setFitToHeight(true);
        tabsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tabsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tabsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0 20 8 20;");

        container.getChildren().addAll(upperToolbar, tabsScroll);
        return container;
    }

    private Node buildLeftProblemPane() {
        VBox container = new VBox(14);
        container.setPadding(new Insets(18, 24, 18, 24));
        container.setStyle("-fx-background-color: " + (isDark ? "#0f1418" : "#ffffff") + ";");

        // Problem Title & Difficulty Badge
        problemTitleLabel = new Label("Problem Title");
        problemTitleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + (isDark ? "#f8fafc" : "#0f172a") + ";");
        problemTitleLabel.setWrapText(true);

        difficultyBadge = new Label("Easy");
        difficultyBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 20;");

        HBox titleRow = new HBox(12, problemTitleLabel, difficultyBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Codeforces-Style Problem Metadata (simple text, no box)
        metaBox = new VBox(3);
        metaBox.setAlignment(Pos.CENTER);
        metaBox.setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");

        timeLimitLabel = new Label("time limit per test: 1.0 seconds");
        timeLimitLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        memoryLimitLabel = new Label("memory limit per test: 256 megabytes");
        memoryLimitLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        Label inputTypeLabel = new Label("input: standard input");
        inputTypeLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        Label outputTypeLabel = new Label("output: standard output");
        outputTypeLabel.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        metaBox.getChildren().addAll(timeLimitLabel, memoryLimitLabel, inputTypeLabel, outputTypeLabel);

        // Scrollable Problem Content (Statement, Input, Output, Constraints, Sample Tests)
        problemContentBox = new VBox(14);

        ScrollPane scrollPane = new ScrollPane(problemContentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        container.getChildren().addAll(titleRow, metaBox, scrollPane);
        return container;
    }

    private Node buildRightEditorPane() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(14, 20, 14, 20));
        container.setStyle("-fx-background-color: " + (isDark ? "#0c1014" : "#f8fafc") + ";");

        // Integrated IDE Compiler Box
        VBox compilerFrame = new VBox();
        compilerFrame.setStyle(
                "-fx-background-color: " + (isDark ? "#10161b" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#243238" : "#cbd5e1") + ";" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;"
        );
        VBox.setVgrow(compilerFrame, Priority.ALWAYS);

        // 1. Editor Header Toolbar (Active Tab, Language, Reset, Font size)
        HBox editorToolbar = new HBox(10);
        editorToolbar.setAlignment(Pos.CENTER_LEFT);
        editorToolbar.setPadding(new Insets(8, 14, 8, 14));
        editorToolbar.setStyle(
                "-fx-background-color: " + (isDark ? "#141c22" : "#f1f5f9") + ";" +
                "-fx-border-color: " + (isDark ? "#243238" : "#e2e8f0") + "; -fx-border-width: 0 0 1 0;" +
                "-fx-background-radius: 8px 8px 0 0;"
        );

        editorFileTabLabel = new Label("</> " + currentLanguage.defaultFileName());
        editorFileTabLabel.setStyle(
                "-fx-font-family: 'JetBrains Mono', Menlo, monospace; -fx-font-weight: 800; -fx-font-size: 12px;" +
                "-fx-text-fill: #0089fc; -fx-background-color: " + (isDark ? "#10161b" : "#ffffff") + ";" +
                "-fx-padding: 5px 12px; -fx-background-radius: 5px; -fx-border-color: " + (isDark ? "#283740" : "#cbd5e1") + "; -fx-border-radius: 5px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 1. VS Code Tab Bar (Top)
        HBox editorTabBar = new HBox(0);
        editorTabBar.setAlignment(Pos.CENTER_LEFT);
        editorTabBar.setStyle(
                "-fx-background-color: #151923;" +
                "-fx-border-color: #232a3b; -fx-border-width: 0 0 1px 0;" +
                "-fx-background-radius: 8px 8px 0 0;"
        );

        // Active File Tab (Night Owl / Programiz dark style)
        HBox activeTab = new HBox(8);
        activeTab.setAlignment(Pos.CENTER_LEFT);
        activeTab.setPadding(new Insets(7, 14, 7, 14));
        activeTab.setStyle(
                "-fx-background-color: #1c2130;" +
                "-fx-border-color: #82aaff #232a3b transparent #232a3b;" +
                "-fx-border-width: 2px 1px 0 1px;"
        );

        editorFileTabLabel = new Label("</> " + currentLanguage.defaultFileName());
        editorFileTabLabel.setStyle("-fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #d5deeb;");

        Label tabCloseIcon = new Label("×");
        tabCloseIcon.setStyle("-fx-font-size: 13px; -fx-text-fill: #707e94; -fx-cursor: hand;");

        activeTab.getChildren().addAll(editorFileTabLabel, tabCloseIcon);

        Region tabSpacer = new Region();
        HBox.setHgrow(tabSpacer, Priority.ALWAYS);

        // Toolbar actions inside Tab Bar (VS Code action bar)
        HBox tabActions = new HBox(6);
        tabActions.setAlignment(Pos.CENTER_RIGHT);
        tabActions.setPadding(new Insets(0, 10, 0, 0));

        Label langLabel = new Label("Lang:");
        langLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #969696;");

        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll(ProgrammingLanguage.values());
        languageComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProgrammingLanguage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        });
        languageComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ProgrammingLanguage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        });
        languageComboBox.getSelectionModel().select(currentLanguage);
        languageComboBox.setStyle(
                "-fx-background-color: #232a3b;" +
                "-fx-border-color: #30394f;" +
                "-fx-border-radius: 4px; -fx-background-radius: 4px;" +
                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;"
        );
        languageComboBox.setOnAction(e -> {
            ProgrammingLanguage selected = languageComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && selected != currentLanguage) {
                saveCurrentCode();
                currentLanguage = selected;
                editorFileTabLabel.setText("</> " + currentLanguage.defaultFileName());
                if (editorBreadcrumbFileLabel != null) {
                    editorBreadcrumbFileLabel.setText(currentLanguage.defaultFileName());
                }
                if (editorLangBadge != null) {
                    editorLangBadge.setText("{ } " + currentLanguage.displayName());
                }
                if (completionPopup != null) {
                    completionPopup.setLanguage(currentLanguage);
                }
                loadCodeForCurrentProblemAndLanguage();
            }
        });

        Button resetBtn = new Button("Reset Template");
        resetBtn.setStyle(
                "-fx-background-color: #232a3b;" +
                "-fx-border-color: #30394f;" +
                "-fx-border-radius: 4px; -fx-background-radius: 4px;" +
                "-fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #d5deeb;" +
                "-fx-cursor: hand; -fx-padding: 3px 8px;"
        );
        resetBtn.setOnMouseEntered(e -> resetBtn.setStyle("-fx-background-color: #2b354a; -fx-border-color: #82aaff; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-padding: 3px 8px;"));
        resetBtn.setOnMouseExited(e -> resetBtn.setStyle("-fx-background-color: #232a3b; -fx-border-color: #30394f; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #d5deeb; -fx-cursor: hand; -fx-padding: 3px 8px;"));
        resetBtn.setOnAction(e -> {
            if (currentLanguage != null) {
                if (isAlgorithmicTopic(currentTopicKey)) {
                    codeEditorArea.replaceText(currentLanguage.starterTemplate());
                } else {
                    codeEditorArea.replaceText(getPracticalStarterTemplate(currentTopicKey, currentLanguage));
                }
            }
        });

        Button zoomOutBtn = new Button("A-");
        zoomOutBtn.setStyle("-fx-background-color: #232a3b; -fx-border-color: #30394f; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 3px 7px; -fx-text-fill: #d5deeb;");
        zoomOutBtn.setOnAction(e -> {
            if (editorFontSize > 11) {
                editorFontSize--;
                updateEditorFont();
            }
        });

        Button zoomInBtn = new Button("A+");
        zoomInBtn.setStyle("-fx-background-color: #232a3b; -fx-border-color: #30394f; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 3px 7px; -fx-text-fill: #d5deeb;");
        zoomInBtn.setOnAction(e -> {
            if (editorFontSize < 24) {
                editorFontSize++;
                updateEditorFont();
            }
        });

        tabActions.getChildren().addAll(langLabel, languageComboBox, resetBtn, zoomOutBtn, zoomInBtn);
        editorTabBar.getChildren().addAll(activeTab, tabSpacer, tabActions);

        // 2. Breadcrumbs Bar
        HBox breadcrumbsBar = new HBox(6);
        breadcrumbsBar.setAlignment(Pos.CENTER_LEFT);
        breadcrumbsBar.setPadding(new Insets(3, 14, 3, 14));
        breadcrumbsBar.setStyle(
                "-fx-background-color: #181c28;" +
                "-fx-border-color: #212738; -fx-border-width: 0 0 1px 0;"
        );

        Label bcFolder = new Label("📁 src");
        bcFolder.setStyle("-fx-font-size: 11px; -fx-text-fill: #707e94; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;");
        Label bcSep1 = new Label("›");
        bcSep1.setStyle("-fx-font-size: 11px; -fx-text-fill: #484f5d;");

        editorBreadcrumbFileLabel = new Label(currentLanguage.defaultFileName());
        editorBreadcrumbFileLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #d5deeb; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; -fx-font-weight: 600;");
        Label bcSep2 = new Label("›");
        bcSep2.setStyle("-fx-font-size: 11px; -fx-text-fill: #484f5d;");

        Label bcSymbol = new Label("{ } main()");
        bcSymbol.setStyle("-fx-font-size: 11px; -fx-text-fill: #82aaff; -fx-font-family: 'JetBrains Mono', Menlo, monospace;");

        breadcrumbsBar.getChildren().addAll(bcFolder, bcSep1, editorBreadcrumbFileLabel, bcSep2, bcSymbol);

        // 3. Code Editor & Built-in Line Numbers (Night Owl / Programiz Dark theme)
        HBox editorContainer = new HBox(0);
        editorContainer.setStyle("-fx-background-color: #1c2130;");
        VBox.setVgrow(editorContainer, Priority.ALWAYS);

        codeEditorArea = new CodeArea();
        codeEditorArea.getStyleClass().add("code-editor-area");
        codeEditorArea.setParagraphGraphicFactory(LineNumberFactory.get(codeEditorArea));
        HBox.setHgrow(codeEditorArea, Priority.ALWAYS);
        VBox.setVgrow(codeEditorArea, Priority.ALWAYS);

        updateEditorFont();

        codeEditorArea.textProperty().addListener((obs, o, n) -> {
            if (n != null) {
                codeEditorArea.setStyleSpans(0, CodeSyntaxHighlighter.computeHighlighting(n, currentLanguage));
            }
        });

        // Install Autocomplete / Auto-fill popup engine
        completionPopup = new CodeCompletionPopup(codeEditorArea, true);
        completionPopup.setLanguage(currentLanguage);

        editorContainer.getChildren().add(codeEditorArea);

        // 4. Status Bar (Footer - Slate Navy #151923)
        HBox editorStatusBar = new HBox(16);
        editorStatusBar.setAlignment(Pos.CENTER_LEFT);
        editorStatusBar.setPadding(new Insets(3, 14, 3, 14));
        editorStatusBar.setStyle(
                "-fx-background-color: #151923;" +
                "-fx-border-color: #232a3b; -fx-border-width: 1px 0 0 0;" +
                "-fx-background-radius: 0 0 8px 8px;"
        );

        editorStatusLabel = new Label("Ln 1, Col 1    1 line    Spaces: 4    UTF-8    LF");
        editorStatusLabel.setStyle("-fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #707e94;");

        Runnable updateStatus = () -> {
            int caret = codeEditorArea.getCaretPosition();
            String text = codeEditorArea.getText();
            int line = 1;
            int col = 1;
            for (int i = 0; i < Math.min(caret, text != null ? text.length() : 0); i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    col = 1;
                } else {
                    col++;
                }
            }
            int totalLines = (text == null || text.isEmpty()) ? 1 : text.split("\n", -1).length;
            editorStatusLabel.setText(String.format("Ln %d, Col %d    %d lines    Spaces: 4    UTF-8    LF", line, col, totalLines));
        };
        codeEditorArea.caretPositionProperty().addListener((obs, o, n) -> updateStatus.run());
        codeEditorArea.textProperty().addListener((obs, o, n) -> updateStatus.run());

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        editorLangBadge = new Label("{ } " + currentLanguage.displayName());
        editorLangBadge.setStyle("-fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #ffffff;");

        editorStatusBar.getChildren().addAll(editorStatusLabel, statusSpacer, editorLangBadge);

        compilerFrame.setStyle(
                "-fx-background-color: #1c2130;" +
                "-fx-border-color: #283144;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;"
        );
        compilerFrame.setEffect(new DropShadow(12, 0, 4, Color.rgb(0, 0, 0, 0.45)));
        compilerFrame.getChildren().addAll(editorTabBar, breadcrumbsBar, editorContainer, editorStatusBar);

        // 4. Custom Input Section (Collapsible)
        customInputCheckBox = new CheckBox("Test with Custom Input");
        customInputCheckBox.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + "; -fx-cursor: hand;");

        customInputArea = new TextArea();
        customInputArea.setPromptText("Enter standard input (stdin) here...");
        customInputArea.setPrefRowCount(3);
        customInputArea.setStyle("-fx-font-family: 'JetBrains Mono', Menlo, Consolas, monospace; -fx-font-size: 12px; -fx-border-color: " + (isDark ? "#243238" : "#cbd5e1") + "; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        customInputContainer = new VBox(6, customInputArea);
        customInputContainer.setVisible(false);
        customInputContainer.setManaged(false);

        customInputCheckBox.setOnAction(e -> {
            boolean active = customInputCheckBox.isSelected();
            customInputContainer.setVisible(active);
            customInputContainer.setManaged(active);
        });

        // 5. Action Buttons (Play / Run Code, Submit Solution)
        runBtn = new Button("▶ Run");
        runBtn.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800; -fx-font-size: 13px;" +
                "-fx-background-radius: 8px; -fx-border-radius: 8px;" +
                "-fx-padding: 9px 22px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.35), 6, 0, 0, 2);"
        );
        runBtn.setOnMouseEntered(e -> runBtn.setStyle(
                "-fx-background-color: #059669; -fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800; -fx-font-size: 13px;" +
                "-fx-background-radius: 8px; -fx-border-radius: 8px;" +
                "-fx-padding: 9px 22px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.45), 8, 0, 0, 2);"
        ));
        runBtn.setOnMouseExited(e -> runBtn.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800; -fx-font-size: 13px;" +
                "-fx-background-radius: 8px; -fx-border-radius: 8px;" +
                "-fx-padding: 9px 22px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.35), 6, 0, 0, 2);"
        ));
        Tooltip.install(runBtn, new Tooltip("Run code and view output (Sample Test 1 or Custom Input)"));
        runBtn.setOnAction(e -> handleRunCode());

        submitBtn = new Button("Submit Solution");
        submitBtn.setStyle(
                "-fx-background-color: #0089fc; -fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800; -fx-font-size: 13px;" +
                "-fx-background-radius: 8px; -fx-border-radius: 8px;" +
                "-fx-padding: 9px 24px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 137, 252, 0.35), 8, 0, 0, 2);"
        );
        submitBtn.setOnMouseEntered(e -> submitBtn.setStyle(
                "-fx-background-color: #0077db; -fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800; -fx-font-size: 13px;" +
                "-fx-background-radius: 8px; -fx-border-radius: 8px;" +
                "-fx-padding: 9px 24px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 137, 252, 0.45), 10, 0, 0, 2);"
        ));
        submitBtn.setOnMouseExited(e -> submitBtn.setStyle(
                "-fx-background-color: #0089fc; -fx-text-fill: #ffffff;" +
                "-fx-font-weight: 800; -fx-font-size: 13px;" +
                "-fx-background-radius: 8px; -fx-border-radius: 8px;" +
                "-fx-padding: 9px 24px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 137, 252, 0.35), 8, 0, 0, 2);"
        ));
        Tooltip.install(submitBtn, new Tooltip("Submit code to judge against all test cases"));
        submitBtn.setOnAction(e -> handleSubmitSolution());

        HBox actionRow = new HBox(12, customInputCheckBox, new Region(), runBtn, submitBtn);
        HBox.setHgrow(actionRow.getChildren().get(1), Priority.ALWAYS);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        // 6. Judging Progress (sleek 3px line) & Verdict Banner
        judgingProgressBar = new ProgressBar();
        judgingProgressBar.setMaxWidth(Double.MAX_VALUE);
        judgingProgressBar.setStyle("-fx-accent: #0089fc; -fx-pref-height: 3px; -fx-min-height: 3px; -fx-max-height: 3px;");
        judgingProgressBar.setVisible(false);
        judgingProgressBar.setManaged(false);

        verdictBanner = new HBox(12);
        verdictBanner.setAlignment(Pos.CENTER_LEFT);
        verdictBanner.setPadding(new Insets(12, 16, 12, 16));
        verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#161e24" : "#f8fafc") + "; -fx-border-color: " + (isDark ? "#243238" : "#e2e8f0") + "; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        verdictIconLabel = new Label();
        verdictIconLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900;");

        verdictTitleLabel = new Label("Ready to judge");
        verdictTitleLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + ";");

        verdictStatsLabel = new Label("Select language and press Submit Solution or Run on Sample Tests");
        verdictStatsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#64748b") + ";");

        verdictBanner.getChildren().addAll(verdictIconLabel, new VBox(2, verdictTitleLabel, verdictStatsLabel));

        // 7. Test Case Execution Results List & Compiler Error Log
        testResultsContainer = new VBox(6);
        testResultsScroll = new ScrollPane(testResultsContainer);
        testResultsScroll.setFitToWidth(true);
        testResultsScroll.setPrefHeight(160);
        testResultsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: " + (isDark ? "#28343b" : "#e2e8f0") + "; -fx-border-radius: 8px;");
        testResultsScroll.setVisible(false);
        testResultsScroll.setManaged(false);

        errorLogArea = new TextArea();
        errorLogArea.setEditable(false);
        errorLogArea.setPrefHeight(140);
        errorLogArea.setStyle("-fx-font-family: 'JetBrains Mono', Menlo, Consolas, monospace; -fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-border-color: #ef4444; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        errorLogArea.setVisible(false);
        errorLogArea.setManaged(false);

        container.getChildren().addAll(
                compilerFrame,
                customInputContainer,
                actionRow,
                judgingProgressBar,
                verdictBanner,
                testResultsScroll,
                errorLogArea
        );
        return container;
    }

    private void updateEditorFont() {
        if (codeEditorArea != null) {
            codeEditorArea.setStyle(
                    "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', Menlo, Monaco, Consolas, monospace;" +
                    "-fx-font-size: " + editorFontSize + "px;" +
                    "-fx-background-color: #1c2130;"
            );
        }
    }

    private void updateLineNumbers() {
        if (codeEditorArea != null && currentLanguage != null) {
            String text = codeEditorArea.getText();
            if (text != null) {
                codeEditorArea.setStyleSpans(0, CodeSyntaxHighlighter.computeHighlighting(text, currentLanguage));
            }
        }
    }

    private void selectTopicByIndex(int topicIdx) {
        if (topicIdx < 0 || topicIdx >= CANONICAL_TOPIC_KEYS.size()) return;

        String topicKey = CANONICAL_TOPIC_KEYS.get(topicIdx);
        this.currentTopicKey = topicKey;

        if (!initialLoad || initialLanguage == null) {
            ProgrammingLanguage defaultLang = getDefaultLanguageForTopic(topicKey);
            if (defaultLang != null && defaultLang != currentLanguage) {
                currentLanguage = defaultLang;
                if (languageComboBox != null) {
                    languageComboBox.getSelectionModel().select(currentLanguage);
                }
            }
        }

        List<DsaProblem> problems = DsaProblemRepository.getProblemsForTopic(topicKey);
        if (problems.isEmpty()) {
            showNoExercisesState(topicKey);
            return;
        }

        DsaProblem toSelect = problems.get(0);
        if (initialProblemId != null) {
            for (DsaProblem p : problems) {
                if (p.id().equalsIgnoreCase(initialProblemId)) {
                    toSelect = p;
                    break;
                }
            }
        }

        // Build problem tabs
        problemTabsBox.getChildren().clear();
        int probNum = 1;
        for (DsaProblem prob : problems) {
            final DsaProblem p = prob;
            Button tabBtn = new Button(probNum + ". " + p.difficulty().label() + " (" + p.id() + ")");
            tabBtn.setStyle(getTabButtonStyle(p == toSelect));
            tabBtn.setOnAction(e -> {
                saveCurrentCode();
                selectProblem(p);
                for (var node : problemTabsBox.getChildren()) {
                    if (node instanceof Button btn) {
                        btn.setStyle(getTabButtonStyle(btn == tabBtn));
                    }
                }
            });
            problemTabsBox.getChildren().add(tabBtn);
            probNum++;
        }

        selectProblem(toSelect);
    }

    private void showNoExercisesState(String topicName) {
        currentProblem = null;
        if (problemTabsBox != null) {
            problemTabsBox.getChildren().clear();
            Label emptyTab = new Label("No Exercises");
            emptyTab.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-padding: 6px 14px;");
            problemTabsBox.getChildren().add(emptyTab);
        }
        if (problemTitleLabel != null) {
            problemTitleLabel.setText("No Practice Exercises Available");
        }
        if (difficultyBadge != null) {
            difficultyBadge.setText("Notice");
            difficultyBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-background-color: " + (isDark ? "#1e293b" : "#f1f5f9") + "; -fx-padding: 4 10; -fx-background-radius: 20;");
        }
        if (timeLimitLabel != null) timeLimitLabel.setText("");
        if (memoryLimitLabel != null) memoryLimitLabel.setText("");

        if (problemContentBox != null) {
            problemContentBox.getChildren().clear();

            Label header = new Label("TOPIC NOT FOUND OR UNDER CONSTRUCTION");
            header.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #f59e0b; -fx-padding: 8 0 4 0;");

            Label msg = new Label(
                    "No coding exercises were found for \"" + (topicName != null ? topicName : "Unknown Topic") + "\".\n\n" +
                    "• Please select an available topic from the dropdown menu above.\n" +
                    "• You can also use the integrated code editor and compiler on the right to test your own code freely."
            );
            msg.setWrapText(true);
            msg.setStyle("-fx-font-size: 13.5px; -fx-line-spacing: 4; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#475569") + ";");

            problemContentBox.getChildren().addAll(header, msg);
        }

        if (codeEditorArea != null && currentLanguage != null) {
            codeEditorArea.replaceText(currentLanguage.starterTemplate());
        }
        clearJudgeResults();
    }

    private String getTabButtonStyle(boolean selected) {
        String base = "-fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 6px 14px; ";
        if (selected) {
            return base + "-fx-background-color: #0089fc; -fx-text-fill: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0, 137, 252, 0.35), 8, 0, 0, 2);";
        } else {
            return base + "-fx-background-color: " + (isDark ? "#1b252c" : "#f1f5f9") + "; -fx-border-color: " + (isDark ? "#283740" : "#e2e8f0") + "; -fx-border-radius: 8px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";";
        }
    }

    private void selectProblem(DsaProblem problem) {
        this.currentProblem = problem;

        boolean isAlgo = isAlgorithmicTopic(currentTopicKey);

        // Update Title & Badge
        problemTitleLabel.setText(problem.title());
        
        String diffBg = switch (problem.difficulty()) {
            case EASY -> isDark ? "#064e3b" : "#dcfce7";
            case MEDIUM -> isDark ? "#451a03" : "#fef3c7";
            case HARD -> isDark ? "#450a0a" : "#fee2e2";
        };
        String diffFg = switch (problem.difficulty()) {
            case EASY -> isDark ? "#6ee7b7" : "#15803d";
            case MEDIUM -> isDark ? "#fcd34d" : "#b45309";
            case HARD -> isDark ? "#fca5a5" : "#b91c1c";
        };
        difficultyBadge.setText(problem.difficulty().label() + " (" + problem.difficulty().ratingRange() + ")");
        difficultyBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + diffFg + "; -fx-background-color: " + diffBg + "; -fx-padding: 4 10; -fx-background-radius: 20;");

        // Update Meta Box - only visible for algorithmic / DS / Language problems where time complexity matters
        if (metaBox != null) {
            metaBox.setVisible(isAlgo);
            metaBox.setManaged(isAlgo);
            if (isAlgo) {
                timeLimitLabel.setText("time limit per test: " + problem.getTimeLimitFormatted());
                memoryLimitLabel.setText("memory limit per test: " + problem.getMemoryLimitFormatted());
            }
        }

        if (arenaTag != null) {
            if (isAlgo) {
                arenaTag.setText("DSA COMPETITIVE ARENA");
                arenaTag.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: " + (isDark ? "#163640" : "#e0f2fe") + "; -fx-text-fill: #0089fc; -fx-padding: 4 10; -fx-background-radius: 6;");
            } else {
                arenaTag.setText("EXERCISE COMPILER");
                arenaTag.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: " + (isDark ? "#1e293b" : "#f1f5f9") + "; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0284c7") + "; -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-color: " + (isDark ? "#334155" : "#cbd5e1") + "; -fx-border-radius: 6;");
            }
        }

        if (stage != null) {
            stage.setTitle(isAlgo ? "CodeTrail DSA Arena & Online Judge" : "CodeTrail Exercise Compiler & Playground");
        }

        if (customInputCheckBox != null) {
            customInputCheckBox.setVisible(isAlgo);
            customInputCheckBox.setManaged(isAlgo);
        }

        if (submitBtn != null) {
            submitBtn.setText(isAlgo ? "Submit Solution" : "Verify Exercise");
        }

        // Update Problem Content
        problemContentBox.getChildren().clear();

        if (isAlgo) {
            // 1. Problem Statement
            Label stmtHeader = new Label("PROBLEM STATEMENT");
            stmtHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 4 0 0 0;");

            Label stmtText = new Label(problem.statement());
            stmtText.setStyle("-fx-font-size: 13.5px; -fx-line-spacing: 3; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + ";");
            stmtText.setWrapText(true);

            problemContentBox.getChildren().addAll(stmtHeader, stmtText);

            // 2. Input Format
            Label inHeader = new Label("INPUT FORMAT");
            inHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 4 0 0 0;");

            Label inText = new Label(problem.inputFormat());
            inText.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
            inText.setWrapText(true);

            problemContentBox.getChildren().addAll(inHeader, inText);

            // 3. Output Format
            Label outHeader = new Label("OUTPUT FORMAT");
            outHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 4 0 0 0;");

            Label outText = new Label(problem.outputFormat());
            outText.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
            outText.setWrapText(true);

            problemContentBox.getChildren().addAll(outHeader, outText);

            // 4. Constraints
            Label constHeader = new Label("CONSTRAINTS");
            constHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 4 0 0 0;");

            Label constText = new Label(problem.constraints());
            constText.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0284c7") + "; -fx-background-color: " + (isDark ? "#161e24" : "#f1f5f9") + "; -fx-border-color: " + (isDark ? "#243238" : "#e2e8f0") + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8 12;");
            constText.setWrapText(true);

            problemContentBox.getChildren().addAll(constHeader, constText);

            // 5. Sample Tests
            Label sampleHeader = new Label("EXAMPLES / SAMPLE TESTS");
            sampleHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 6 0 0 0;");
            problemContentBox.getChildren().add(sampleHeader);

            int sampleNum = 1;
            for (TestCase sample : problem.getSampleTests()) {
                VBox sampleBox = buildSampleBox(sampleNum++, sample);
                problemContentBox.getChildren().add(sampleBox);
            }
        } else {
            // Practical statement problem format - no competitive metadata
            Label stmtHeader = new Label("PROBLEM STATEMENT");
            stmtHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 4 0 0 0;");

            Label stmtText = new Label(problem.statement());
            stmtText.setStyle("-fx-font-size: 13.5px; -fx-line-spacing: 4; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + ";");
            stmtText.setWrapText(true);

            problemContentBox.getChildren().addAll(stmtHeader, stmtText);

            if (problem.inputFormat() != null && !problem.inputFormat().isBlank()) {
                Label reqHeader = new Label("REQUIREMENTS & SPECIFICATIONS");
                reqHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 6 0 0 0;");

                Label reqText = new Label(problem.inputFormat());
                reqText.setStyle("-fx-font-size: 13px; -fx-line-spacing: 3; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
                reqText.setWrapText(true);

                problemContentBox.getChildren().addAll(reqHeader, reqText);
            }

            if (problem.outputFormat() != null && !problem.outputFormat().isBlank()) {
                Label delivHeader = new Label("EXPECTED DELIVERABLES");
                delivHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 6 0 0 0;");

                Label delivText = new Label(problem.outputFormat());
                delivText.setStyle("-fx-font-size: 13px; -fx-line-spacing: 3; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
                delivText.setWrapText(true);

                problemContentBox.getChildren().addAll(delivHeader, delivText);
            }

            if (problem.constraints() != null && !problem.constraints().isBlank()) {
                Label constHeader = new Label("CONSTRAINTS & TECHNICAL SPEC");
                constHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 6 0 0 0;");

                Label constText = new Label(problem.constraints());
                constText.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0284c7") + "; -fx-background-color: " + (isDark ? "#161e24" : "#f1f5f9") + "; -fx-border-color: " + (isDark ? "#243238" : "#e2e8f0") + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8 12;");
                constText.setWrapText(true);

                problemContentBox.getChildren().addAll(constHeader, constText);
            }

            List<TestCase> sampleTests = problem.getSampleTests();
            if (!sampleTests.isEmpty()) {
                Label cpHeader = new Label("PRACTICE CHECKPOINTS");
                cpHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 6 0 0 0;");
                problemContentBox.getChildren().add(cpHeader);

                int cpNum = 1;
                for (TestCase sample : sampleTests) {
                    VBox cpCard = new VBox(6);
                    cpCard.setStyle("-fx-background-color: " + (isDark ? "#141b20" : "#f8fafc") + "; -fx-border-color: " + (isDark ? "#28343b" : "#e2e8f0") + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 14;");

                    Label cpTitle = new Label("Checkpoint " + (cpNum++) + ": " + sample.input());
                    cpTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + ";");

                    Label cpExpected = new Label("Expected: " + sample.expectedOutput());
                    cpExpected.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
                    cpExpected.setWrapText(true);

                    cpCard.getChildren().addAll(cpTitle, cpExpected);
                    problemContentBox.getChildren().add(cpCard);
                }
            }
        }

        // Note
        if (problem.note() != null && !problem.note().isBlank()) {
            Label noteHeader = new Label(isAlgo ? "NOTE" : "NOTE & LEARNING TIPS");
            noteHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0089fc; -fx-padding: 4 0 0 0;");

            Label noteText = new Label(problem.note());
            noteText.setStyle("-fx-font-size: 13px; -fx-font-style: italic; -fx-text-fill: " + (isDark ? "#94a3b8" : "#64748b") + ";");
            noteText.setWrapText(true);

            problemContentBox.getChildren().addAll(noteHeader, noteText);
        }

        // Load cached or default starter code
        loadCodeForCurrentProblemAndLanguage();
        clearJudgeResults();
    }

    private VBox buildSampleBox(int num, TestCase sample) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: " + (isDark ? "#141b20" : "#f8fafc") + "; -fx-border-color: " + (isDark ? "#28343b" : "#e2e8f0") + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12;");

        // Input sub-box
        VBox inBox = new VBox();
        inBox.setStyle("-fx-border-color: " + (isDark ? "#283740" : "#e2e8f0") + "; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        HBox inHead = new HBox(8);
        inHead.setAlignment(Pos.CENTER_LEFT);
        inHead.setPadding(new Insets(6, 12, 6, 12));
        inHead.setStyle("-fx-background-color: " + (isDark ? "#1a242c" : "#eef2f6") + "; -fx-background-radius: 6px 6px 0 0;");

        Label inLabel = new Label("Sample Input " + num);
        inLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 11.5px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + ";");

        Button copyInBtn = new Button("Copy");
        copyInBtn.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2px 8px; -fx-background-color: " + (isDark ? "#283740" : "#ffffff") + "; -fx-border-color: " + (isDark ? "#384954" : "#cbd5e1") + "; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#475569") + "; -fx-cursor: hand;");
        copyInBtn.setOnAction(e -> copyToClipboard(sample.input()));

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        inHead.getChildren().addAll(inLabel, sp1, copyInBtn);

        Label inVal = new Label(sample.input());
        inVal.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12.5px; -fx-padding: 8px 12px; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + "; -fx-background-color: " + (isDark ? "#12181d" : "#ffffff") + "; -fx-background-radius: 0 0 6px 6px;");
        inVal.setMaxWidth(Double.MAX_VALUE);

        inBox.getChildren().addAll(inHead, inVal);

        // Output sub-box
        VBox outBox = new VBox();
        outBox.setStyle("-fx-border-color: " + (isDark ? "#283740" : "#e2e8f0") + "; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        HBox outHead = new HBox(8);
        outHead.setAlignment(Pos.CENTER_LEFT);
        outHead.setPadding(new Insets(6, 12, 6, 12));
        outHead.setStyle("-fx-background-color: " + (isDark ? "#1a242c" : "#eef2f6") + "; -fx-background-radius: 6px 6px 0 0;");

        Label outLabel = new Label("Sample Output " + num);
        outLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 11.5px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + ";");

        Button copyOutBtn = new Button("Copy");
        copyOutBtn.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2px 8px; -fx-background-color: " + (isDark ? "#283740" : "#ffffff") + "; -fx-border-color: " + (isDark ? "#384954" : "#cbd5e1") + "; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#475569") + "; -fx-cursor: hand;");
        copyOutBtn.setOnAction(e -> copyToClipboard(sample.expectedOutput()));

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        outHead.getChildren().addAll(outLabel, sp2, copyOutBtn);

        Label outVal = new Label(sample.expectedOutput());
        outVal.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12.5px; -fx-padding: 8px 12px; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + "; -fx-background-color: " + (isDark ? "#12181d" : "#ffffff") + "; -fx-background-radius: 0 0 6px 6px;");
        outVal.setMaxWidth(Double.MAX_VALUE);

        outBox.getChildren().addAll(outHead, outVal);

        box.getChildren().addAll(inBox, outBox);

        if (sample.explanation() != null && !sample.explanation().isBlank()) {
            Label exp = new Label("Explanation: " + sample.explanation());
            exp.setStyle("-fx-font-size: 11.5px; -fx-font-style: italic; -fx-text-fill: " + (isDark ? "#94a3b8" : "#64748b") + ";");
            exp.setWrapText(true);
            box.getChildren().add(exp);
        }

        return box;
    }

    private void saveCurrentCode() {
        if (currentProblem != null && currentLanguage != null && codeEditorArea != null) {
            String key = currentProblem.id() + "_" + currentLanguage.id();
            CODE_CACHE.put(key, codeEditorArea.getText());
        }
    }

    private void loadCodeForCurrentProblemAndLanguage() {
        if (currentProblem == null || currentLanguage == null) return;
        String key = currentProblem.id() + "_" + currentLanguage.id();
        String saved = CODE_CACHE.get(key);
        if (saved != null) {
            codeEditorArea.replaceText(saved);
        } else {
            if (isAlgorithmicTopic(currentTopicKey)) {
                codeEditorArea.replaceText(currentLanguage.starterTemplate());
            } else {
                codeEditorArea.replaceText(getPracticalStarterTemplate(currentTopicKey, currentLanguage));
            }
        }
        if (editorFileTabLabel != null) {
            editorFileTabLabel.setText("</> " + currentLanguage.defaultFileName());
        }
        if (editorBreadcrumbFileLabel != null) {
            editorBreadcrumbFileLabel.setText(currentLanguage.defaultFileName());
        }
        if (editorLangBadge != null) {
            editorLangBadge.setText("{ } " + currentLanguage.displayName());
        }
        updateLineNumbers();
    }

    private void clearJudgeResults() {
        boolean isAlgo = isAlgorithmicTopic(currentTopicKey);
        if (isAlgo) {
            verdictTitleLabel.setText("Ready to run or judge");
            verdictStatsLabel.setText("Select language, click ▶ Run to test output or Submit Solution to judge");
        } else {
            verdictTitleLabel.setText("Exercise Compiler Ready");
            verdictStatsLabel.setText("Click ▶ Run to execute implementation or Verify Exercise to validate checkpoints");
        }
        verdictIconLabel.setText("");
        judgingProgressBar.setVisible(false);
        judgingProgressBar.setManaged(false);
        testResultsScroll.setVisible(false);
        testResultsScroll.setManaged(false);
        errorLogArea.setVisible(false);
        errorLogArea.setManaged(false);
        verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#161e24" : "#f8fafc") + "; -fx-border-color: " + (isDark ? "#243238" : "#e2e8f0") + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");
    }

    private void handleRunCode() {
        if (currentProblem == null) {
            saveCurrentCode();
            String code = codeEditorArea.getText();
            String testInput = (customInputArea != null && customInputCheckBox.isSelected()) ? customInputArea.getText() : "";
            setRunningState(true, "Running code on custom input...");
            OnlineJudgeService.runCustomTestAsync(
                    currentLanguage,
                    code,
                    testInput,
                    "",
                    2000,
                    result -> {
                        setRunningState(false, "");
                        displayRunResult(result, true);
                    }
            );
            return;
        }
        saveCurrentCode();
        String code = codeEditorArea.getText();

        boolean isCustom = customInputCheckBox.isSelected();
        String testInput = isCustom ? customInputArea.getText() : (currentProblem.getSampleTests().isEmpty() ? "" : currentProblem.getSampleTests().get(0).input());
        String expectedOut = isCustom ? "" : (currentProblem.getSampleTests().isEmpty() ? "" : currentProblem.getSampleTests().get(0).expectedOutput());

        setRunningState(true, isCustom ? "Running code on custom input..." : "Running code on Sample Test 1...");

        OnlineJudgeService.runCustomTestAsync(
                currentLanguage,
                code,
                testInput,
                expectedOut,
                currentProblem.timeLimitMs(),
                result -> {
                    setRunningState(false, "");
                    displayRunResult(result, isCustom);
                }
        );
    }

    private void setRunningState(boolean running, String message) {
        submitBtn.setDisable(running);
        runBtn.setDisable(running);
        judgingProgressBar.setVisible(running);
        judgingProgressBar.setManaged(running);

        if (running) {
            verdictTitleLabel.setText("Running Code...");
            verdictTitleLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #10b981;");
            verdictStatsLabel.setText(message);
            verdictIconLabel.setText("");
            testResultsScroll.setVisible(false);
            testResultsScroll.setManaged(false);
            errorLogArea.setVisible(false);
            errorLogArea.setManaged(false);
            verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#102a20" : "#ecfdf5") + "; -fx-border-color: #10b981; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");
        }
    }

    private void displayRunResult(TestCaseResult result, boolean isCustom) {
        verdictIconLabel.setText("");
        judgingProgressBar.setVisible(false);
        judgingProgressBar.setManaged(false);

        if (result.verdict() == SubmissionVerdict.COMPILATION_ERROR) {
            verdictTitleLabel.setText("Compilation Error");
            verdictTitleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #ef4444;");
            verdictStatsLabel.setText("Build failed. See compiler output below.");
            errorLogArea.setText(result.errorOutput());
            errorLogArea.setVisible(true);
            errorLogArea.setManaged(true);
            testResultsScroll.setVisible(false);
            testResultsScroll.setManaged(false);
            verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#3f1414" : "#fef2f2") + "; -fx-border-color: #ef4444; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");
            return;
        }

        if (result.verdict() == SubmissionVerdict.TIME_LIMIT_EXCEEDED) {
            verdictTitleLabel.setText("Time Limit Exceeded");
            verdictTitleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #f97316;");
            verdictStatsLabel.setText("Execution timed out (limit: " + currentProblem.getTimeLimitFormatted() + ")");
            errorLogArea.setText("Execution was terminated after exceeding the time limit of " + currentProblem.getTimeLimitFormatted() + ".");
            errorLogArea.setVisible(true);
            errorLogArea.setManaged(true);
            testResultsScroll.setVisible(false);
            testResultsScroll.setManaged(false);
            verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#3e210c" : "#fff7ed") + "; -fx-border-color: #f97316; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");
            return;
        }

        if (result.verdict() == SubmissionVerdict.RUNTIME_ERROR) {
            verdictTitleLabel.setText("Runtime Error");
            verdictTitleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #ef4444;");
            verdictStatsLabel.setText("Program crashed or exited with non-zero status • Time: " + result.timeMs() + " ms");
            verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#3f1414" : "#fef2f2") + "; -fx-border-color: #ef4444; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");
        } else {
            // Normal execution finished - DO NOT display "Accepted" message!
            verdictTitleLabel.setText("Program Output");
            verdictTitleLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + ";");
            verdictStatsLabel.setText("Execution time: " + result.timeMs() + " ms" + (isCustom ? " • Custom Input" : " • Sample Test 1"));
            verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#161e24" : "#f8fafc") + "; -fx-border-color: " + (isDark ? "#283740" : "#cbd5e1") + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");
        }

        testResultsContainer.getChildren().clear();

        // 1. Input Box (if provided)
        if (result.input() != null && !result.input().isBlank()) {
            VBox inBox = new VBox(4);
            Label inTitle = new Label("Input (stdin):");
            inTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 11.5px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
            TextArea inArea = new TextArea(result.input());
            inArea.setEditable(false);
            inArea.setPrefRowCount(Math.min(3, Math.max(1, result.input().split("\n").length)));
            inArea.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px; -fx-border-color: " + (isDark ? "#28343b" : "#e2e8f0") + "; -fx-border-radius: 4px;");
            inBox.getChildren().addAll(inTitle, inArea);
            testResultsContainer.getChildren().add(inBox);
        }

        // 2. Output Box (stdout)
        VBox outBox = new VBox(4);
        Label outTitle = new Label("Output (stdout):");
        outTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 11.5px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
        String stdout = result.actualOutput().isEmpty() ? "(No output)" : result.actualOutput();
        TextArea outArea = new TextArea(stdout);
        outArea.setEditable(false);
        outArea.setPrefRowCount(Math.min(6, Math.max(2, stdout.split("\n").length)));
        outArea.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px; -fx-border-color: " + (isDark ? "#28343b" : "#e2e8f0") + "; -fx-border-radius: 4px;");
        outBox.getChildren().addAll(outTitle, outArea);
        testResultsContainer.getChildren().add(outBox);

        // 3. Expected Sample Output (when not custom input)
        if (!isCustom && result.expectedOutput() != null && !result.expectedOutput().isBlank()) {
            VBox expBox = new VBox(4);
            Label expTitle = new Label("Expected Sample Output:");
            expTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 11.5px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
            TextArea expArea = new TextArea(result.expectedOutput());
            expArea.setEditable(false);
            expArea.setPrefRowCount(Math.min(4, Math.max(1, result.expectedOutput().split("\n").length)));
            expArea.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 12px; -fx-border-color: " + (isDark ? "#28343b" : "#e2e8f0") + "; -fx-border-radius: 4px;");
            expBox.getChildren().addAll(expTitle, expArea);
            testResultsContainer.getChildren().add(expBox);
        }

        // 4. Stderr (if any)
        if (!result.errorOutput().isBlank()) {
            VBox errBox = new VBox(4);
            Label errTitle = new Label("Standard Error (stderr):");
            errTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 11.5px; -fx-text-fill: #ef4444;");
            TextArea errArea = new TextArea(result.errorOutput());
            errArea.setEditable(false);
            errArea.setPrefRowCount(3);
            errArea.setStyle("-fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-border-color: #ef4444; -fx-border-radius: 4px;");
            errBox.getChildren().addAll(errTitle, errArea);
            testResultsContainer.getChildren().add(errBox);
        }

        testResultsScroll.setVisible(true);
        testResultsScroll.setManaged(true);
    }

    private void handleSubmitSolution() {
        if (currentProblem == null) {
            verdictTitleLabel.setText("No Problem Selected");
            verdictTitleLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #f59e0b;");
            verdictStatsLabel.setText("Please select a problem or topic before submitting a solution.");
            return;
        }
        saveCurrentCode();
        String code = codeEditorArea.getText();

        setJudgingState(true, "Compiling solution in " + currentLanguage.displayName() + "...");

        OnlineJudgeService.submitSolutionAsync(
                currentProblem,
                currentLanguage,
                code,
                this::displayJudgeResult,
                progressMessage -> verdictStatsLabel.setText(progressMessage)
        );
    }

    private void setJudgingState(boolean judging, String message) {
        submitBtn.setDisable(judging);
        runBtn.setDisable(judging);
        judgingProgressBar.setVisible(judging);
        judgingProgressBar.setManaged(judging);

        if (judging) {
            verdictTitleLabel.setText("Judging Solution...");
            verdictTitleLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #0089fc;");
            verdictStatsLabel.setText(message);
            verdictIconLabel.setText("");
            testResultsScroll.setVisible(false);
            testResultsScroll.setManaged(false);
            errorLogArea.setVisible(false);
            errorLogArea.setManaged(false);
            verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#162838" : "#edf7ff") + "; -fx-border-color: #0089fc; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");
        } else {
            judgingProgressBar.setVisible(false);
            judgingProgressBar.setManaged(false);
        }
    }

    private void displayJudgeResult(JudgeResult result) {
        setJudgingState(false, "");
        SubmissionVerdict verdict = result.overallVerdict();

        verdictTitleLabel.setText(verdict.label() + (result.failedTestIndex() > 0 ? " on test " + result.failedTestIndex() : ""));
        verdictTitleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: " + verdict.color() + ";");
        verdictIconLabel.setText(switch (verdict) {
            case ACCEPTED -> "✔";
            case WRONG_ANSWER -> "✖";
            case TIME_LIMIT_EXCEEDED -> "⧗";
            case COMPILATION_ERROR -> "!";
            case RUNTIME_ERROR -> "✕";
            default -> "";
        });

        if (verdict == SubmissionVerdict.COMPILATION_ERROR) {
            verdictStatsLabel.setText("Compilation failed. See compiler output below.");
            errorLogArea.setText(result.compilationError());
            errorLogArea.setVisible(true);
            errorLogArea.setManaged(true);
            testResultsScroll.setVisible(false);
            testResultsScroll.setManaged(false);
            verdictBanner.setStyle("-fx-background-color: " + (isDark ? "#3b1742" : "#faf5ff") + "; -fx-border-color: #8b5cf6; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");
            return;
        }

        verdictStatsLabel.setText(String.format("Passed %d of %d tests • Time: %d ms", result.passedTests(), result.totalTests(), result.maxTimeMs()));

        String bannerBg = switch (verdict) {
            case ACCEPTED -> isDark ? "#0c3b28" : "#ecfdf5";
            case WRONG_ANSWER -> isDark ? "#3f1414" : "#fef2f2";
            case TIME_LIMIT_EXCEEDED -> isDark ? "#3e210c" : "#fff7ed";
            default -> isDark ? "#161e24" : "#f8fafc";
        };
        verdictBanner.setStyle("-fx-background-color: " + bannerBg + "; -fx-border-color: " + verdict.color() + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12 16;");

        // Populate test case list
        testResultsContainer.getChildren().clear();
        for (TestCaseResult tcr : result.testResults()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(7, 12, 7, 12));
            row.setStyle("-fx-background-color: " + (isDark ? "#161e24" : "#ffffff") + "; -fx-border-color: " + (isDark ? "#243238" : "#e2e8f0") + "; -fx-border-radius: 6; -fx-background-radius: 6;");

            Label statusIcon = new Label(tcr.isPassed() ? "✔" : "✘");
            statusIcon.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + (tcr.isPassed() ? "#10b981" : "#ef4444") + ";");

            Label testName = new Label("Test " + tcr.testIndex() + (tcr.isSample() ? " (Sample)" : ""));
            testName.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + ";");

            Label verdictLbl = new Label(tcr.verdict().shortCode());
            verdictLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: " + tcr.verdict().color() + ";");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label timeLbl = new Label(tcr.timeMs() + " ms");
            timeLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#64748b") + ";");

            row.getChildren().addAll(statusIcon, testName, verdictLbl, sp, timeLbl);
            testResultsContainer.getChildren().add(row);
        }

        testResultsScroll.setVisible(true);
        testResultsScroll.setManaged(true);
    }

    private void copyToClipboard(String text) {
        if (text == null) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
}
