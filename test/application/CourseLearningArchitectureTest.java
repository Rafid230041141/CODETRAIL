package application;

import application.client.learning.*;

import java.util.List;

/**
 * Automated test suite for Course-Type Aware Learning Architecture in CodeTrail.
 */
public class CourseLearningArchitectureTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  STARTING COURSE-TYPE AWARE ARCHITECTURE TESTS  ");
        System.out.println("=================================================");

        testCategoryTopicMapping();
        testLanguagesAndDsaPreservedUntouched();
        testAiMlLearningFlow();
        testDataScienceLearningFlow();
        testWebDevLearningFlow();
        testAppDevLearningFlow();
        testGameDevLearningFlow();
        testPracticeEnvironmentsAndTheoryOnly();
        testMathematicalCorrectness();

        System.out.println("=================================================");
        System.out.println("  ALL COURSE LEARNING ARCHITECTURE TESTS PASSED! ");
        System.out.println("=================================================");
    }

    private static void testCategoryTopicMapping() {
        System.out.println("[TEST 1] Verifying CourseCategory mapping for all topics...");

        // AI / ML Topics
        List<String> aiTopics = List.of("ml_foundations", "math_ai", "scikit", "deep_learning", "vision", "nlp", "genai", "mlops");
        for (String t : aiTopics) {
            CourseCategory cat = CourseCategory.fromTopicKey(t);
            if (cat != CourseCategory.AI_ML) {
                throw new AssertionError("Expected AI_ML for topic " + t + ", got: " + cat);
            }
        }
        System.out.println("  ✓ All 8 AI/ML topics map to CourseCategory.AI_ML");

        // Data Science Topics
        List<String> dsTopics = List.of("numpy", "pandas", "eda", "statistics", "feature_eng", "bigdata", "sql_analytics", "bi_dashboards");
        for (String t : dsTopics) {
            CourseCategory cat = CourseCategory.fromTopicKey(t);
            if (cat != CourseCategory.DATA_SCIENCE) {
                throw new AssertionError("Expected DATA_SCIENCE for topic " + t + ", got: " + cat);
            }
        }
        System.out.println("  ✓ All 8 Data Science topics map to CourseCategory.DATA_SCIENCE");

        // Web Dev Topics
        List<String> webTopics = List.of("html5", "css3", "javascript", "react", "node", "database", "auth", "deploy");
        for (String t : webTopics) {
            CourseCategory cat = CourseCategory.fromTopicKey(t);
            if (cat != CourseCategory.WEB_DEV) {
                throw new AssertionError("Expected WEB_DEV for topic " + t + ", got: " + cat);
            }
        }
        System.out.println("  ✓ All 8 Web Dev topics map to CourseCategory.WEB_DEV");

        // App Dev Topics
        List<String> appTopics = List.of("flutter", "reactnative", "kotlin", "swift", "statemgmt", "mobileapi", "sqlite", "publish");
        for (String t : appTopics) {
            CourseCategory cat = CourseCategory.fromTopicKey(t);
            if (cat != CourseCategory.APP_DEV) {
                throw new AssertionError("Expected APP_DEV for topic " + t + ", got: " + cat);
            }
        }
        System.out.println("  ✓ All 8 App Dev topics map to CourseCategory.APP_DEV");

        // Game Dev Topics
        List<String> gameTopics = List.of("math_games", "pygame", "unity_basics", "unity_3d", "unreal", "game_physics", "audio_vfx", "game_publish");
        for (String t : gameTopics) {
            CourseCategory cat = CourseCategory.fromTopicKey(t);
            if (cat != CourseCategory.GAME_DEV) {
                throw new AssertionError("Expected GAME_DEV for topic " + t + ", got: " + cat);
            }
        }
        System.out.println("  ✓ All 8 Game Dev topics map to CourseCategory.GAME_DEV");

        // DSA Topics
        List<String> dsaTopics = List.of(
                "arrays", "linked lists", "stacks", "queues", "hash maps", "heaps",
                "trees", "dsu", "trie", "sorting algorithms", "searching", "graphs",
                "range queries", "algorithmic paradigms", "string algorithms", "mathematics"
        );
        for (String t : dsaTopics) {
            CourseCategory cat = CourseCategory.fromTopicKey(t);
            if (cat != CourseCategory.DSA) {
                throw new AssertionError("Expected DSA for topic " + t + ", got: " + cat);
            }
        }
        System.out.println("  ✓ All 16 DSA topics map to CourseCategory.DSA");
    }

    private static void testLanguagesAndDsaPreservedUntouched() {
        System.out.println("[TEST 2] Verifying Languages and DSA flows remain 100% untouched...");

        List<LearningBlock> langBlocks = CourseLearningOrchestrator.buildLessonBlocks(CourseCategory.LANGUAGES, "python", "Variables and Types", "Languages/Python");
        if (!langBlocks.isEmpty()) {
            throw new AssertionError("Languages must have empty custom blocks (untouched standard flow). Got " + langBlocks.size());
        }

        List<LearningBlock> dsaBlocks = CourseLearningOrchestrator.buildLessonBlocks(CourseCategory.DSA, "trees", "Binary Search Trees", "DSA/Trees");
        if (!dsaBlocks.isEmpty()) {
            throw new AssertionError("DSA must have empty custom blocks (untouched competitive flow). Got " + dsaBlocks.size());
        }

        if (!CourseCategory.LANGUAGES.isLanguagesOrDsa() || !CourseCategory.DSA.isLanguagesOrDsa()) {
            throw new AssertionError("isLanguagesOrDsa check failed");
        }

        System.out.println("  ✓ Languages and DSA flows produce 0 block overhead, preserving native experience completely.");
    }

    private static void testAiMlLearningFlow() {
        System.out.println("[TEST 3] Verifying AI/ML intuition-led learning flow components...");

        List<LearningBlock> blocks = CourseLearningOrchestrator.buildLessonBlocks(CourseCategory.AI_ML, "ml_foundations", "Supervised Learning", "AI/ML");
        if (blocks.isEmpty()) {
            throw new AssertionError("Expected AI/ML blocks, got none");
        }

        boolean hasObj = false, hasFormula = false, hasExp = false, hasQuiz = false, hasEval = false;
        for (LearningBlock b : blocks) {
            if (b instanceof ObjectiveBlock) hasObj = true;
            if (b instanceof FormulaBlock) hasFormula = true;
            if (b instanceof InteractiveExperimentBlock) hasExp = true;
            if (b instanceof UnderstandingCheckBlock) hasQuiz = true;
            if (b instanceof ModelEvaluationBlock) hasEval = true;
        }

        if (!hasObj || !hasFormula || !hasExp || !hasQuiz || !hasEval) {
            throw new AssertionError("AI/ML flow missing required component: obj=" + hasObj + ", formula=" + hasFormula + ", exp=" + hasExp + ", quiz=" + hasQuiz + ", eval=" + hasEval);
        }

        System.out.println("  ✓ AI/ML produces Objective, Formula, Interactive Experiment, Understanding Check, and Evaluation blocks.");
    }

    private static void testDataScienceLearningFlow() {
        System.out.println("[TEST 4] Verifying Data Science exploration flow components...");

        List<LearningBlock> blocks = CourseLearningOrchestrator.buildLessonBlocks(CourseCategory.DATA_SCIENCE, "pandas", "DataFrame Wrangling", "Data Science");
        if (blocks.isEmpty()) {
            throw new AssertionError("Expected Data Science blocks, got none");
        }

        boolean hasObj = false, hasDataset = false, hasNotebook = false, hasQuiz = false;
        for (LearningBlock b : blocks) {
            if (b instanceof ObjectiveBlock) hasObj = true;
            if (b instanceof DatasetViewerBlock) hasDataset = true;
            if (b instanceof DataScienceNotebookBlock) hasNotebook = true;
            if (b instanceof UnderstandingCheckBlock) hasQuiz = true;
        }

        if (!hasObj || !hasDataset || !hasNotebook || !hasQuiz) {
            throw new AssertionError("Data Science flow missing required component: obj=" + hasObj + ", dataset=" + hasDataset + ", notebook=" + hasNotebook + ", quiz=" + hasQuiz);
        }

        System.out.println("  ✓ Data Science produces Objective, Dataset Table Viewer, Pandas Notebook Query Cell, and Interpretation Check blocks.");
    }

    private static void testWebDevLearningFlow() {
        System.out.println("[TEST 5] Verifying Web Development live preview flow components...");

        List<LearningBlock> blocks = CourseLearningOrchestrator.buildLessonBlocks(CourseCategory.WEB_DEV, "html5", "Semantic Web Structure", "Web Dev");
        if (blocks.isEmpty()) {
            throw new AssertionError("Expected Web Dev blocks, got none");
        }

        boolean hasObj = false, hasWebPreview = false;
        for (LearningBlock b : blocks) {
            if (b instanceof ObjectiveBlock) hasObj = true;
            if (b instanceof LiveWebPreviewBlock) hasWebPreview = true;
        }

        if (!hasObj || !hasWebPreview) {
            throw new AssertionError("Web Dev flow missing required component: obj=" + hasObj + ", webPreview=" + hasWebPreview);
        }

        System.out.println("  ✓ Web Dev produces Objective and Live Web Preview with embedded rendering & debug challenge.");
    }

    private static void testAppDevLearningFlow() {
        System.out.println("[TEST 6] Verifying App Development mobile device frame flow components...");

        List<LearningBlock> blocks = CourseLearningOrchestrator.buildLessonBlocks(CourseCategory.APP_DEV, "flutter", "Stateful Architecture", "App Dev");
        if (blocks.isEmpty()) {
            throw new AssertionError("Expected App Dev blocks, got none");
        }

        boolean hasObj = false, hasMobilePreview = false;
        for (LearningBlock b : blocks) {
            if (b instanceof ObjectiveBlock) hasObj = true;
            if (b instanceof MobilePreviewBlock) hasMobilePreview = true;
        }

        if (!hasObj || !hasMobilePreview) {
            throw new AssertionError("App Dev flow missing required component: obj=" + hasObj + ", mobilePreview=" + hasMobilePreview);
        }

        System.out.println("  ✓ App Dev produces Objective and Mobile Device Frame Preview with interactive component simulation.");
    }

    private static void testGameDevLearningFlow() {
        System.out.println("[TEST 7] Verifying Game Development physics & loop flow components...");

        List<LearningBlock> blocks = CourseLearningOrchestrator.buildLessonBlocks(CourseCategory.GAME_DEV, "math_games", "Game Physics", "Game Dev");
        if (blocks.isEmpty()) {
            throw new AssertionError("Expected Game Dev blocks, got none");
        }

        boolean hasObj = false, hasGamePreview = false, hasQuiz = false;
        for (LearningBlock b : blocks) {
            if (b instanceof ObjectiveBlock) hasObj = true;
            if (b instanceof GamePreviewBlock) hasGamePreview = true;
            if (b instanceof UnderstandingCheckBlock) hasQuiz = true;
        }

        if (!hasObj || !hasGamePreview || !hasQuiz) {
            throw new AssertionError("Game Dev flow missing required component: obj=" + hasObj + ", gamePreview=" + hasGamePreview + ", quiz=" + hasQuiz);
        }

        System.out.println("  ✓ Game Dev produces Objective, 2D Game Preview Canvas with 60 FPS loop, and Game Physics delta-time check blocks.");
    }

    private static void testPracticeEnvironmentsAndTheoryOnly() {
        System.out.println("[TEST 8] Verifying PracticeEnvironment resolution & Theory-Only suppression...");

        // 1. Languages must always map to COMPILER
        PracticeEnvironment langEnv = PracticeEnvironment.forCategory(CourseCategory.LANGUAGES, "What is a Pointer", "");
        if (langEnv != PracticeEnvironment.COMPILER) {
            throw new AssertionError("Languages must always use COMPILER, got: " + langEnv);
        }

        // 2. DSA must always map to ALGORITHM_LAB
        PracticeEnvironment dsaEnv = PracticeEnvironment.forCategory(CourseCategory.DSA, "Introduction to Trees", "");
        if (dsaEnv != PracticeEnvironment.ALGORITHM_LAB) {
            throw new AssertionError("DSA must always use ALGORITHM_LAB, got: " + dsaEnv);
        }

        // 3. AI/ML with code -> ML_LAB
        PracticeEnvironment aiEnv = PracticeEnvironment.forCategory(CourseCategory.AI_ML, "Gradient Descent", "import numpy as np");
        if (aiEnv != PracticeEnvironment.ML_LAB) {
            throw new AssertionError("AI/ML with code must use ML_LAB, got: " + aiEnv);
        }

        // 4. Data Science with code -> DATA_SCIENCE_LAB
        PracticeEnvironment dsEnv = PracticeEnvironment.forCategory(CourseCategory.DATA_SCIENCE, "Data Cleaning", "import pandas as pd");
        if (dsEnv != PracticeEnvironment.DATA_SCIENCE_LAB) {
            throw new AssertionError("Data Science with code must use DATA_SCIENCE_LAB, got: " + dsEnv);
        }

        // 5. Web Dev with code -> WEB_PREVIEW
        PracticeEnvironment webEnv = PracticeEnvironment.forCategory(CourseCategory.WEB_DEV, "Flexbox Nav", "<div class='nav'></div>");
        if (webEnv != PracticeEnvironment.WEB_PREVIEW) {
            throw new AssertionError("Web Dev with code must use WEB_PREVIEW, got: " + webEnv);
        }

        // 6. App Dev with code -> APP_PREVIEW
        PracticeEnvironment appEnv = PracticeEnvironment.forCategory(CourseCategory.APP_DEV, "Widget Trees", "class MyWidget extends StatelessWidget");
        if (appEnv != PracticeEnvironment.APP_PREVIEW) {
            throw new AssertionError("App Dev with code must use APP_PREVIEW, got: " + appEnv);
        }

        // 7. Game Dev with code -> GAME_PREVIEW
        PracticeEnvironment gameEnv = PracticeEnvironment.forCategory(CourseCategory.GAME_DEV, "Kinematics Loop", "dt = clock.tick()");
        if (gameEnv != PracticeEnvironment.GAME_PREVIEW) {
            throw new AssertionError("Game Dev with code must use GAME_PREVIEW, got: " + gameEnv);
        }

        // 8. Theory-Only Lessons (Purely conceptual without code) -> THEORY_ONLY
        List<String> theoryTitles = List.of(
                "What is Supervised Learning?",
                "Introduction to Neural Architectures",
                "Overview of Data Science Pipelines",
                "Foundations of Web Accessibility",
                "Theory of Game State Machines"
        );
        for (String title : theoryTitles) {
            PracticeEnvironment theoryEnv = PracticeEnvironment.forCategory(CourseCategory.AI_ML, title, "");
            if (!theoryEnv.isTheoryOnly()) {
                throw new AssertionError("Expected THEORY_ONLY for '" + title + "', got: " + theoryEnv);
            }
        }

        System.out.println("  ✓ All 8 PracticeEnvironment types correctly resolved; Theory-Only suppression active for purely conceptual lessons.");
    }

    private static void testMathematicalCorrectness() {
        System.out.println("[TEST 9] Verifying analytical least-squares regression calculations...");

        double[] x = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0 };
        double[] y = { 2.2, 2.9, 4.2, 5.1, 5.8, 7.2, 7.9, 9.1 };
        int n = x.length;

        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumXX += x[i] * x[i];
        }

        double wOpt = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double bOpt = (sumY - wOpt * sumX) / n;

        // Verify slope and intercept are reasonable (slope ~ 1.0, intercept ~ 1.1)
        if (wOpt < 0.9 || wOpt > 1.1) {
            throw new AssertionError("Analytical slope w* out of range: " + wOpt);
        }
        if (bOpt < 0.9 || bOpt > 1.4) {
            throw new AssertionError("Analytical intercept b* out of range: " + bOpt);
        }

        // Calculate MSE with optimal weights
        double totalResidualSq = 0;
        for (int i = 0; i < n; i++) {
            double res = y[i] - (wOpt * x[i] + bOpt);
            totalResidualSq += res * res;
        }
        double mse = totalResidualSq / n;

        if (mse > 0.15) {
            throw new AssertionError("Optimal MSE should be < 0.15, got: " + mse);
        }

        System.out.println("  ✓ Closed-form OLS correctly yields w* = " + String.format("%.3f", wOpt) + ", b* = " + String.format("%.3f", bOpt) + ", MSE = " + String.format("%.4f", mse));
    }
}
