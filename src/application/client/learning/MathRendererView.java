package application.client.learning;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * MathRendererView renders LaTeX mathematical formulas using local KaTeX via JavaFX WebView.
 * Provides seamless theme adaptation (dark/light), transparent background, and graceful
 * Unicode fallback for headless test environments.
 */
public class MathRendererView extends StackPane {

    private final String formulaText;
    private boolean isDark;
    private WebView webView;
    private Label fallbackLabel;

    public MathRendererView(String formulaText, boolean isDark) {
        this.formulaText = formulaText != null ? formulaText : "";
        this.isDark = isDark;
        setAlignment(Pos.CENTER);
        setMinHeight(50);
        setPrefHeight(60);
        setMaxHeight(78);

        initView();
    }

    private void initView() {
        try {
            webView = new WebView();
            webView.setContextMenuEnabled(false);
            webView.setPageFill(Color.TRANSPARENT);
            webView.setMinHeight(48);
            webView.setPrefHeight(58);
            webView.setMaxHeight(76);

            String htmlUrl = resolveRendererHtmlUrl();
            if (htmlUrl != null) {
                String colorHex = isDark ? "#38bdf8" : "#0284c7";
                String jsonPayload = "{\"formula\":" + escapeJson(formulaText) + ",\"color\":" + escapeJson(colorHex) + "}";
                String encodedPayload = URLEncoder.encode(jsonPayload, StandardCharsets.UTF_8).replace("+", "%20");
                String targetUrl = htmlUrl + "#" + encodedPayload;

                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        renderViaScript(formulaText, isDark);
                    }
                });

                webView.getEngine().load(targetUrl);
                getChildren().add(webView);
                return;
            }
        } catch (Throwable t) {
            // WebKit native library unavailable (e.g. headless CI or unsupported platform)
        }

        // Fallback to high-quality formatted Unicode math label
        setupFallback();
    }

    private void setupFallback() {
        getChildren().clear();
        String unicodeMath = toUnicodeFallback(formulaText);
        fallbackLabel = new Label(unicodeMath);
        fallbackLabel.setAlignment(Pos.CENTER);
        fallbackLabel.setStyle(
                "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'DejaVu Sans Mono', monospace;" +
                "-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0284c7") + ";"
        );
        getChildren().add(fallbackLabel);
    }

    private void renderViaScript(String formula, boolean dark) {
        if (webView == null) return;
        Platform.runLater(() -> {
            try {
                String colorHex = dark ? "#38bdf8" : "#0284c7";
                String script = String.format(
                        "if (typeof window.renderMath === 'function') { window.renderMath('%s', '%s'); }",
                        escapeJs(formula), escapeJs(colorHex)
                );
                webView.getEngine().executeScript(script);
            } catch (Throwable ignored) {
            }
        });
    }

    public void setTheme(boolean dark) {
        this.isDark = dark;
        if (webView != null) {
            renderViaScript(formulaText, dark);
        } else if (fallbackLabel != null) {
            fallbackLabel.setStyle(
                    "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'DejaVu Sans Mono', monospace;" +
                    "-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + (dark ? "#38bdf8" : "#0284c7") + ";"
            );
        }
    }

    /**
     * Resolves the KaTeX math renderer HTML URL. If running in a JAR, extracts the katex bundle
     * to a local user cache directory so WebKit can load fonts and stylesheets via file:// protocol.
     */
    private static String resolveRendererHtmlUrl() {
        try {
            URL res = MathRendererView.class.getResource("/resources/katex/math_renderer.html");
            if (res == null) {
                return null;
            }

            if ("file".equalsIgnoreCase(res.getProtocol())) {
                return res.toExternalForm();
            }

            // In JAR environment, extract to ~/.codetrail/katex_cache once
            String userHome = System.getProperty("user.home", ".");
            Path cacheDir = Paths.get(userHome, ".codetrail", "katex_cache");
            Path targetHtml = cacheDir.resolve("math_renderer.html");

            if (!Files.exists(targetHtml)) {
                Files.createDirectories(cacheDir);
                Path fontsDir = cacheDir.resolve("fonts");
                Files.createDirectories(fontsDir);

                copyResourceToFile("/resources/katex/math_renderer.html", targetHtml);
                copyResourceToFile("/resources/katex/katex.min.js", cacheDir.resolve("katex.min.js"));
                copyResourceToFile("/resources/katex/katex.min.css", cacheDir.resolve("katex.min.css"));
            }

            return targetHtml.toUri().toURL().toExternalForm();
        } catch (Throwable e) {
            return null;
        }
    }

    private static void copyResourceToFile(String resourcePath, Path targetPath) {
        try (InputStream in = MathRendererView.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Transforms standard LaTeX formula constructs into clean Unicode math notation for fallback.
     */
    public static String toUnicodeFallback(String latex) {
        if (latex == null || latex.isBlank()) return "";

        String s = latex;
        // Hats and accents
        s = s.replace("\\hat{y}_i", "ŷᵢ");
        s = s.replace("\\hat{y}", "ŷ");
        s = s.replace("y_hat_i", "ŷᵢ");
        s = s.replace("y_hat", "ŷ");

        // Common text labels & functions
        s = s.replace("\\text{MSE}", "MSE");
        s = s.replace("\\sigma(z)", "σ(z)");
        s = s.replace("\\sigma", "σ");

        // Subscripts
        s = s.replace("y_i", "yᵢ");
        s = s.replace("w_i", "wᵢ");
        s = s.replace("x_i", "xᵢ");

        // Operators & symbols
        s = s.replace("\\sum_{i=1}^{n}", "Σ");
        s = s.replace("\\sum_{i=1}^n", "Σ");
        s = s.replace("\\frac{1}{n}", "(1/n)");
        s = s.replace("\\frac{1}{1 + e^{-z}}", "1 / (1 + e⁻ᶻ)");
        s = s.replace("\\implies", " ==> ");
        s = s.replace("\\Longrightarrow", " ==> ");
        s = s.replace("\\qquad", "   ");
        s = s.replace("\\quad", " ");
        s = s.replace("\\Big|", "|");
        s = s.replace("^2", "²");
        s = s.replace("e^{-z}", "e⁻ᶻ");

        // Clean redundant braces or backslashes
        s = s.replace("\\", "");
        s = s.replaceAll("[{}]", "");
        return s.trim();
    }

    private static String escapeJson(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
