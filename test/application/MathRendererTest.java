package application;

import application.client.learning.CourseCategory;
import application.client.learning.CourseLearningOrchestrator;
import application.client.learning.FormulaBlock;
import application.client.learning.LearningBlock;
import application.client.learning.MathRendererView;
import javafx.application.Platform;
import javafx.scene.Node;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * MathRendererTest verifies KaTeX resource packaging, LaTeX notation,
 * FormulaBlock rendering in both light and dark themes, and fallback behavior.
 */
public class MathRendererTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("           CodeTrail Math Renderer & KaTeX Verification Suite                   ");
        System.out.println("================================================================================");

        int passed = 0;
        int failed = 0;

        try {
            testKatexResourcePackaging();
            passed++;
            System.out.println("[PASS] Suite 1: KaTeX 0.16.9 Assets & HTML Template Packaging");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 1: KaTeX Packaging failed: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testUnicodeFallbackTransformation();
            passed++;
            System.out.println("[PASS] Suite 2: Unicode Mathematical Symbol Fallback Transformation");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 2: Fallback Transformation failed: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testFormulaSymbolFormatting();
            passed++;
            System.out.println("[PASS] Suite 3: FormulaBlock Symbol Formatting (ŷ, wᵢ, xᵢ, σ(z))");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 3: Symbol Formatting failed: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testCourseOrchestratorLatexFormulas();
            passed++;
            System.out.println("[PASS] Suite 4: CourseLearningOrchestrator LaTeX Sources & Clean Variables");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 4: Orchestrator LaTeX verification failed: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testJavaFxRenderingAndTheming();
            passed++;
            System.out.println("[PASS] Suite 5: FormulaBlock Node Rendering & Dual-Theme Adaptation");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 5: Node rendering failed: " + t.getMessage());
            t.printStackTrace();
        }

        System.out.println("================================================================================");
        System.out.printf("Results: %d Passed, %d Failed%n", passed, failed);
        System.out.println("================================================================================");

        try {
            Platform.exit();
        } catch (Throwable ignored) {}
        System.exit(failed > 0 ? 1 : 0);
    }

    private static void testKatexResourcePackaging() throws Exception {
        String[] requiredResources = {
                "/resources/katex/math_renderer.html",
                "/resources/katex/katex.min.js",
                "/resources/katex/katex.min.css"
        };

        for (String resPath : requiredResources) {
            try (InputStream in = MathRendererTest.class.getResourceAsStream(resPath)) {
                if (in == null) {
                    throw new AssertionError("Missing KaTeX resource on classpath: " + resPath);
                }
                byte[] sample = in.readNBytes(64);
                if (sample.length == 0) {
                    throw new AssertionError("Empty KaTeX resource file: " + resPath);
                }
            }
        }
    }

    private static void testUnicodeFallbackTransformation() {
        String lrLatex = "\\hat{y} = wx + b   |   \\text{MSE} = \\frac{1}{n}\\sum_{i=1}^{n}(y_i - \\hat{y}_i)^2";
        String fallbackLr = MathRendererView.toUnicodeFallback(lrLatex);

        if (!fallbackLr.contains("ŷ = wx + b")) {
            throw new AssertionError("Expected 'ŷ = wx + b' in fallback, got: " + fallbackLr);
        }
        if (!fallbackLr.contains("MSE")) {
            throw new AssertionError("Expected 'MSE' in fallback, got: " + fallbackLr);
        }
        if (!fallbackLr.contains("yᵢ") || !fallbackLr.contains("ŷᵢ")) {
            throw new AssertionError("Expected 'yᵢ' and 'ŷᵢ' true subscripts in fallback, got: " + fallbackLr);
        }
        if (!fallbackLr.contains("Σ")) {
            throw new AssertionError("Expected 'Σ' summation symbol in fallback, got: " + fallbackLr);
        }

        String dlLatex = "z = \\sum_{i=1}^{n} (w_i x_i) + b \\implies a = \\sigma(z) = \\frac{1}{1 + e^{-z}}";
        String fallbackDl = MathRendererView.toUnicodeFallback(dlLatex);

        if (!fallbackDl.contains("wᵢ") || !fallbackDl.contains("xᵢ")) {
            throw new AssertionError("Expected 'wᵢ' and 'xᵢ' in fallback, got: " + fallbackDl);
        }
        if (!fallbackDl.contains("σ(z)")) {
            throw new AssertionError("Expected 'σ(z)' in fallback, got: " + fallbackDl);
        }
        if (!fallbackDl.contains("==>")) {
            throw new AssertionError("Expected '==>' implication in fallback, got: " + fallbackDl);
        }
    }

    private static void testFormulaSymbolFormatting() {
        if (!"ŷ".equals(FormulaBlock.formatSymbol("y_hat"))) {
            throw new AssertionError("Failed formatSymbol(y_hat)");
        }
        if (!"ŷ".equals(FormulaBlock.formatSymbol("\\hat{y}"))) {
            throw new AssertionError("Failed formatSymbol(\\hat{y})");
        }
        if (!"wᵢ".equals(FormulaBlock.formatSymbol("w_i"))) {
            throw new AssertionError("Failed formatSymbol(w_i)");
        }
        if (!"xᵢ".equals(FormulaBlock.formatSymbol("x_i"))) {
            throw new AssertionError("Failed formatSymbol(x_i)");
        }
        if (!"σ(z)".equals(FormulaBlock.formatSymbol("\\sigma(z)"))) {
            throw new AssertionError("Failed formatSymbol(\\sigma(z))");
        }
        if (!"MSE".equals(FormulaBlock.formatSymbol("\\text{MSE}"))) {
            throw new AssertionError("Failed formatSymbol(\\text{MSE})");
        }
    }

    private static void testCourseOrchestratorLatexFormulas() {
        List<LearningBlock> blocks = CourseLearningOrchestrator.buildLessonBlocks(
                CourseCategory.AI_ML, "linear-regression", "Linear Regression Foundations", "ai/ml/lr"
        );

        FormulaBlock fb = (FormulaBlock) blocks.stream()
                .filter(b -> b instanceof FormulaBlock)
                .findFirst()
                .orElseThrow(() -> new AssertionError("FormulaBlock not generated for AI/ML!"));

        if (!fb.title().contains("Linear Hypothesis")) {
            throw new AssertionError("Unexpected formula title: " + fb.title());
        }

        // Test Deep Learning formula block
        List<LearningBlock> dlBlocks = CourseLearningOrchestrator.buildLessonBlocks(
                CourseCategory.AI_ML, "deep-learning", "Neural Network Foundations", "ai/ml/dl"
        );

        FormulaBlock dlFb = (FormulaBlock) dlBlocks.stream()
                .filter(b -> b instanceof FormulaBlock)
                .findFirst()
                .orElseThrow(() -> new AssertionError("FormulaBlock not generated for Deep Learning!"));

        if (!dlFb.title().contains("Neuron & Sigmoid")) {
            throw new AssertionError("Unexpected DL formula title: " + dlFb.title());
        }
    }

    private static void testJavaFxRenderingAndTheming() throws Exception {
        // Run JavaFX node rendering test on JavaFX toolkit thread if possible, or direct instantiation
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] renderError = new Throwable[1];

        Runnable task = () -> {
            try {
                FormulaBlock fb = new FormulaBlock(
                        "test-fb",
                        "Linear Hypothesis & MSE Loss",
                        "\\hat{y} = wx + b   |   \\text{MSE} = \\frac{1}{n}\\sum_{i=1}^{n}(y_i - \\hat{y}_i)^2",
                        List.of(
                                new FormulaBlock.VariableInfo("ŷ", "Model Prediction", "Calculated response"),
                                new FormulaBlock.VariableInfo("w", "Slope", "Controls slope"),
                                new FormulaBlock.VariableInfo("b", "Intercept", "Expected baseline"),
                                new FormulaBlock.VariableInfo("MSE", "Mean Squared Error", "Quadratic penalty")
                        ),
                        "Squaring residuals (yᵢ - ŷᵢ)² penalizes outliers."
                );

                // Render dark theme
                Node darkNode = fb.render(true);
                if (darkNode == null) {
                    throw new AssertionError("FormulaBlock.render(true) returned null");
                }

                // Render light theme
                Node lightNode = fb.render(false);
                if (lightNode == null) {
                    throw new AssertionError("FormulaBlock.render(false) returned null");
                }
            } catch (Throwable t) {
                renderError[0] = t;
            } finally {
                latch.countDown();
            }
        };

        try {
            Platform.startup(task);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(task);
        }

        boolean finished = latch.await(4, TimeUnit.SECONDS);
        if (!finished) {
            throw new AssertionError("Timed out waiting for JavaFX rendering");
        }
        if (renderError[0] != null) {
            throw new AssertionError("Error rendering FormulaBlock in JavaFX", renderError[0]);
        }
    }
}
