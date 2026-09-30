package application.client.learning;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CourseLearningOrchestrator constructs bespoke learning block sequences tailored
 * to each course category and topic, while strictly preserving standard flows for Languages and DSA.
 */
public class CourseLearningOrchestrator {

    /**
     * Builds the tailored learning blocks for a curriculum lesson.
     * Returns an empty list for Languages and DSA to guarantee zero regressions.
     */
    public static List<LearningBlock> buildLessonBlocks(CourseCategory category, String topicKey, String lessonTitle, String path) {
        if (category == null || category.isLanguagesOrDsa()) {
            return List.of(); // Zero changes to Languages and DSA
        }

        List<LearningBlock> blocks = new ArrayList<>();
        String k = topicKey != null ? topicKey.trim().toLowerCase(Locale.ROOT) : "";

        switch (category) {
            case AI_ML -> buildAiMlBlocks(blocks, k, lessonTitle);
            case DATA_SCIENCE -> buildDataScienceBlocks(blocks, k, lessonTitle);
            case WEB_DEV -> buildWebDevBlocks(blocks, k, lessonTitle);
            case APP_DEV -> buildAppDevBlocks(blocks, k, lessonTitle);
            case GAME_DEV -> buildGameDevBlocks(blocks, k, lessonTitle);
            default -> {}
        }

        return blocks;
    }

    // =========================================================================
    // AI / MACHINE LEARNING FLOW
    // Objective -> Concept -> Formula -> Interactive Experiment -> Understanding Check -> Model Evaluation
    // =========================================================================
    private static void buildAiMlBlocks(List<LearningBlock> blocks, String topicKey, String title) {
        boolean isDeepLearning = topicKey.contains("deep") || topicKey.contains("neural") || topicKey.contains("nlp") || topicKey.contains("vision") || topicKey.contains("genai");

        // 1. Learning Objective
        blocks.add(new ObjectiveBlock(
                "ai-obj-1",
                "Supervised Learning & Optimization",
                CourseCategory.AI_ML,
                "Master the mathematical intuition, loss optimization, and evaluation metrics behind predictive machine learning models.",
                List.of(
                        "Understand how model parameters (weights & bias) map inputs to target predictions",
                        "Formulate Mean Squared Error (MSE) loss and minimize prediction residuals",
                        "Interact with live parameters to observe empirical loss minimization",
                        "Evaluate model generalization using R² variance and residual diagnostics"
                ),
                "Applied ML Foundation"
        ));

        // 2. Mathematical Intuition & Formula
        if (isDeepLearning) {
            blocks.add(new FormulaBlock(
                    "ai-math-dl",
                    "Artificial Neuron & Sigmoid Activation",
                    "z = \\sum_{i=1}^{n} (w_i x_i) + b \\implies a = \\sigma(z) = \\frac{1}{1 + e^{-z}}",
                    List.of(
                            new FormulaBlock.VariableInfo("wᵢ", "Synaptic Weight", "Amplifies or suppresses the feature's influence"),
                            new FormulaBlock.VariableInfo("xᵢ", "Input Feature", "Normalized numeric input signal"),
                            new FormulaBlock.VariableInfo("b", "Neuron Bias", "Shifts activation threshold independent of inputs"),
                            new FormulaBlock.VariableInfo("σ(z)", "Sigmoid Activation", "Squeezes linear pre-activation into a [0, 1] probability")
                    ),
                    "Non-linear activation functions allow multi-layer neural networks to approximate arbitrary non-linear functions (Universal Approximation Theorem)."
            ));

            blocks.add(new InteractiveExperimentBlock(
                    "ai-exp-dl",
                    "Sigmoid Activation Curve",
                    InteractiveExperimentBlock.ExperimentType.SIGMOID_ACTIVATION
            ));
        } else {
            blocks.add(new FormulaBlock(
                    "ai-math-lr",
                    "Linear Hypothesis & Mean Squared Error Loss",
                    "\\hat{y} = wx + b   |   \\text{MSE} = \\frac{1}{n}\\sum_{i=1}^{n}(y_i - \\hat{y}_i)^2",
                    List.of(
                            new FormulaBlock.VariableInfo("ŷ", "Model Prediction", "Calculated response of the linear hypothesis function"),
                            new FormulaBlock.VariableInfo("w", "Slope / Weight", "Controls rate of change of y with respect to feature x"),
                            new FormulaBlock.VariableInfo("b", "Intercept / Bias", "Expected value of y when feature x is exactly zero"),
                            new FormulaBlock.VariableInfo("MSE", "Mean Squared Error", "Penalizes distance between predictions and actual labels quadratically")
                    ),
                    "Squaring residuals (yᵢ - ŷᵢ)² penalizes large outliers severely and guarantees a convex loss bowl with a unique global minimum."
            ));

            blocks.add(new InteractiveExperimentBlock(
                    "ai-exp-lr",
                    "Linear Regression Loss Minimization",
                    InteractiveExperimentBlock.ExperimentType.LINEAR_REGRESSION
            ));
        }

        // 3. Check Your Understanding
        blocks.add(new UnderstandingCheckBlock(
                "ai-quiz-1",
                "Loss Functions & Optimization",
                CourseCategory.AI_ML,
                "Why do regression algorithms minimize squared errors (MSE) instead of raw sum of errors (Σ (y - ŷ))?",
                List.of(
                        new UnderstandingCheckBlock.Option("A", "Because raw errors can be positive and negative, canceling out to zero despite terrible predictions.", true,
                                "Exactly! An error of +10 and an error of -10 would sum to 0 if not squared or made absolute, falsely implying perfection!"),
                        new UnderstandingCheckBlock.Option("B", "Because squaring numbers is the only way to run calculations on GPUs.", false,
                                "GPUs perform additions and multiplications equally well; squaring is chosen for mathematical convex optimization properties."),
                        new UnderstandingCheckBlock.Option("C", "Because MSE completely ignores all outliers in training data.", false,
                                "In fact, MSE does the opposite: squaring magnifies large outlier errors, making the model very sensitive to them."),
                        new UnderstandingCheckBlock.Option("D", "Because linear regression cannot calculate square roots.", false,
                                "Square roots are calculated easily (RMSE), but MSE simplifies the derivative during gradient descent: d/dw (error)² = 2 * error.")
                )
        ));

        // 4. Model Evaluation Benchmarks
        blocks.add(new ModelEvaluationBlock(
                "ai-eval-1",
                "Regression Quality Benchmarks",
                List.of(
                        new ModelEvaluationBlock.MetricItem("MSE Loss", "0.142", "Target < 0.250", true),
                        new ModelEvaluationBlock.MetricItem("R² Variance", "0.94", "Target > 0.90", true),
                        new ModelEvaluationBlock.MetricItem("RMSE", "0.377", "Target < 0.500", true),
                        new ModelEvaluationBlock.MetricItem("MAE", "0.312", "Target < 0.400", true)
                ),
                "Model exhibits high explained variance (R² = 0.94) and balanced residuals with no sign of severe underfitting or overfitting."
        ));
    }

    // =========================================================================
    // DATA SCIENCE FLOW
    // Question -> Dataset Preview -> Exploration -> Cleaning -> Interpretation
    // =========================================================================
    private static void buildDataScienceBlocks(List<LearningBlock> blocks, String topicKey, String title) {
        // 1. Question & Goal
        blocks.add(new ObjectiveBlock(
                "ds-obj-1",
                "Data Exploration & Wrangling",
                CourseCategory.DATA_SCIENCE,
                "Explore demographic distributions, detect anomalies and missing values, and prepare clean tabular features for statistical analytics.",
                List.of(
                        "Inspect dataset schema, row samples, and columnar memory footprint",
                        "Quantify missing values across categorical and continuous variables",
                        "Perform median imputation and encode categorical text columns",
                        "Extract business insights and validate data integrity before modeling"
                ),
                "Practical Data Wrangling"
        ));

        // 2. Interactive Dataset Preview Table
        blocks.add(new DatasetViewerBlock(
                "ds-table-titanic",
                "Titanic Demographics Preview",
                "Titanic Passenger Survival Dataset",
                891,
                List.of("PassengerId", "Survived", "Pclass", "Name", "Sex", "Age", "Fare", "Embarked"),
                List.of(
                        List.of("1", "0", "3", "Braund, Mr. Owen Harris", "male", "22.0", "7.25", "S"),
                        List.of("2", "1", "1", "Cumings, Mrs. John Bradley", "female", "38.0", "71.28", "C"),
                        List.of("3", "1", "3", "Heikkinen, Miss. Laina", "female", "26.0", "7.92", "S"),
                        List.of("4", "1", "1", "Futrelle, Mrs. Jacques Heath", "female", "35.0", "53.10", "S"),
                        List.of("5", "0", "3", "Allen, Mr. William Henry", "male", "35.0", "8.05", "S"),
                        List.of("6", "0", "3", "Moran, Mr. James", "male", "NaN", "8.45", "Q"),
                        List.of("7", "0", "1", "McCarthy, Mr. Timothy J", "male", "54.0", "51.86", "S"),
                        List.of("8", "0", "3", "Palsson, Master. Gosta Leonard", "male", "2.0", "21.07", "S")
                ),
                List.of(
                        new DatasetViewerBlock.ColumnSchema("PassengerId", "int64", 0, "Unique record identifier"),
                        new DatasetViewerBlock.ColumnSchema("Survived", "int64", 0, "Binary target: 1 = survived, 0 = deceased"),
                        new DatasetViewerBlock.ColumnSchema("Pclass", "int64", 0, "Ticket class: 1st, 2nd, 3rd"),
                        new DatasetViewerBlock.ColumnSchema("Name", "object", 0, "Passenger full name & honorific title"),
                        new DatasetViewerBlock.ColumnSchema("Sex", "object", 0, "Gender: male / female (needs binary mapping)"),
                        new DatasetViewerBlock.ColumnSchema("Age", "float64", 177, "Passenger age in years (19.8% nulls)"),
                        new DatasetViewerBlock.ColumnSchema("Fare", "float64", 0, "Ticket price paid in British pounds"),
                        new DatasetViewerBlock.ColumnSchema("Embarked", "object", 2, "Port of embarkation (C, Q, S)")
                ),
                "1. Impute missing Age values with the median of each Pclass group to preserve age distribution.\n"
                        + "2. Convert Sex to binary integers (female=1, male=0).\n"
                        + "3. One-hot encode the Embarked categorical feature into dummy indicator variables."
        ));

        // 3. Notebook-Style Pandas & Chart Execution Cell
        blocks.add(new DataScienceNotebookBlock(
                "ds-notebook-1",
                "Pandas Groupby & Department Aggregation",
                "df.groupby(\"Department\")[\"Salary\"].mean()"
        ));

        // 4. Understanding & Interpretation Check
        blocks.add(new UnderstandingCheckBlock(
                "ds-quiz-1",
                "Data Exploration Check",
                CourseCategory.DATA_SCIENCE,
                "When imputing missing values in a skewed feature (like Age or Income), why is the median generally preferred over the mean?",
                List.of(
                        new UnderstandingCheckBlock.Option("A", "The median is resistant to extreme outliers and does not distort the central tendency.", true,
                                "Correct! The median represents the 50th percentile and is robust against skewed distributions and extreme outliers."),
                        new UnderstandingCheckBlock.Option("B", "The mean always rounds down to zero for floating point numbers.", false,
                                "False: the mean preserves exact decimal precision."),
                        new UnderstandingCheckBlock.Option("C", "Pandas does not support computing the mean on columns with missing values.", false,
                                "Pandas handles missing values with df['col'].mean(skipna=True) seamlessly."),
                        new UnderstandingCheckBlock.Option("D", "Using the median eliminates the need to normalize or standardize features later.", false,
                                "Imputation method does not replace feature scaling.")
                )
        ));
    }

    // =========================================================================
    // WEB DEVELOPMENT FLOW
    // Concept & UI Specs -> Code Editor -> Live Web Preview -> Debug Challenge
    // =========================================================================
    private static void buildWebDevBlocks(List<LearningBlock> blocks, String topicKey, String title) {
        // 1. Concept & UI Specs
        blocks.add(new ObjectiveBlock(
                "web-obj-1",
                "UI Architecture & Responsive Design",
                CourseCategory.WEB_DEV,
                "Construct modern semantic web layouts, responsive CSS grid/flex structures, and interactive client-side logic.",
                List.of(
                        "Structure content using semantic HTML5 elements (<header>, <nav>, <main>, <article>)",
                        "Implement responsive layouts using CSS Flexbox and CSS Grid auto-fit properties",
                        "Inspect live layout changes instantly in the embedded Web Preview renderer",
                        "Resolve common UI alignment, overflow, and mobile viewport styling bugs"
                ),
                "Modern Frontend"
        ));

        // 2. Live Web Preview Block
        String sampleWebHtml = "<!DOCTYPE html><html><head><meta charset='utf-8'><style>"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }"
                + ".navbar { display: flex; justify-content: space-between; align-items: center; background: #1e293b; padding: 12px 20px; border-radius: 8px; border: 1px solid #334155; }"
                + ".logo { font-weight: 800; color: #38bdf8; font-size: 16px; }"
                + ".nav-links { display: flex; gap: 16px; }"
                + ".nav-links a { color: #94a3b8; text-decoration: none; font-size: 13px; font-weight: 600; }"
                + ".nav-links a:hover { color: #38bdf8; }"
                + ".hero { margin-top: 20px; padding: 24px; background: linear-gradient(135deg, #1e293b, #0f172a); border: 1px solid #334155; border-radius: 10px; text-align: center; }"
                + ".hero h1 { margin: 0 0 10px 0; font-size: 22px; color: #f1f5f9; }"
                + ".hero p { color: #94a3b8; font-size: 13px; max-width: 480px; margin: 0 auto 16px auto; }"
                + ".cta-btn { background: #0284c7; color: #ffffff; border: none; padding: 9px 18px; border-radius: 6px; font-weight: 700; cursor: pointer; transition: all 0.2s; }"
                + ".cta-btn:hover { background: #0369a1; transform: translateY(-1px); }"
                + "</style></head><body>"
                + "<div class='navbar'>"
                + "<div class='logo'>CodeTrail Web</div>"
                + "<div class='nav-links'><a href='#'>Courses</a><a href='#'>Sandbox</a><a href='#'>Docs</a></div>"
                + "</div>"
                + "<div class='hero'>"
                + "<h1>Responsive UI Component</h1>"
                + "<p>Render HTML5, CSS Flexbox & JavaScript in real time with embedded WebKit engine.</p>"
                + "<button class='cta-btn' onclick='alert(\"Action triggered!\")'>Interactive Demo</button>"
                + "</div></body></html>";

        blocks.add(new LiveWebPreviewBlock(
                "web-preview-1",
                "Live DOM & CSS Layout Engine",
                sampleWebHtml,
                "Challenge: On screens smaller than 600px, the nav-links should wrap or collapse into a vertical column with gap: 8px. Use @media (max-width: 600px) in your CSS!"
        ));
    }

    // =========================================================================
    // APP DEVELOPMENT FLOW
    // Concept & Architecture -> Mobile Phone Frame Preview -> State Debugger
    // =========================================================================
    private static void buildAppDevBlocks(List<LearningBlock> blocks, String topicKey, String title) {
        String framework = "Flutter";
        if (topicKey.contains("react")) framework = "React Native";
        else if (topicKey.contains("kotlin")) framework = "Jetpack Compose";
        else if (topicKey.contains("swift")) framework = "SwiftUI";

        // 1. Concept & Architecture
        blocks.add(new ObjectiveBlock(
                "app-obj-1",
                "Mobile Architecture & State Flow",
                CourseCategory.APP_DEV,
                "Design reactive mobile component hierarchies, handle local state transitions, and ensure smooth multi-platform rendering.",
                List.of(
                        "Compose declarative UI widgets (Scaffold, Column, ListView, AppBars)",
                        "Manage unidirectional state flow and atomic widget rebuilds",
                        "Preview mobile layout inside a simulated smartphone viewport",
                        "Inspect lifecycle states, button callbacks, and reactive streams"
                ),
                framework + " Mobile Native"
        ));

        // 2. Mobile Phone Frame Preview
        blocks.add(new MobilePreviewBlock(
                "app-preview-1",
                framework + " Component Sandbox",
                framework,
                "CodeTrail Mobile",
                "State Rebuild Note: When increment (+) is pressed, only the CounterCard widget subtree triggers build(). The surrounding Scaffold and BottomNavigationBar stay cached without wasteful redraws."
        ));
    }

    // =========================================================================
    // GAME DEV FLOW
    // =========================================================================
    private static void buildGameDevBlocks(List<LearningBlock> blocks, String topicKey, String title) {
        blocks.add(new ObjectiveBlock(
                "game-obj-1",
                "Game Physics & 2D Vector Mechanics",
                CourseCategory.GAME_DEV,
                "Implement delta-time movement, collision bounding boxes, and frame-rate independent game loops.",
                List.of(
                        "Calculate position updates using velocity vectors and delta-time (dt)",
                        "Implement Axis-Aligned Bounding Box (AABB) collision checks",
                        "Maintain a fixed 60 FPS update loop with interpolated rendering"
                ),
                "Game Engine Architecture"
        ));

        // 2. 2D Game Engine & Physics Canvas Preview
        blocks.add(new GamePreviewBlock(
                "game-preview-1",
                "Kinematic Delta-Time Physics & Collision",
                "Challenge: Control the player with arrow keys / on-screen buttons to collect stars. Use sliders to tune gravity and velocity!"
        ));

        blocks.add(new UnderstandingCheckBlock(
                "game-quiz-1",
                "Game Loop Mechanics",
                CourseCategory.GAME_DEV,
                "Why must position updates in a game loop multiply velocity by deltaTime (position += velocity * dt)?",
                List.of(
                        new UnderstandingCheckBlock.Option("A", "To ensure game objects move at the same real-world speed regardless of frame rate fluctuations.", true,
                                "Exactly! If a game lags to 30 FPS or runs at 144 FPS, multiplying by dt keeps movement consistent across all displays!"),
                        new UnderstandingCheckBlock.Option("B", "Because delta-time converts pixel coordinates to GPS coordinates.", false,
                                "Delta-time measures elapsed seconds between consecutive frames."),
                        new UnderstandingCheckBlock.Option("C", "Because GPUs will throw a division by zero error without it.", false,
                                "Delta-time is a CPU physics loop multiplier.")
                )
        ));
    }
}
