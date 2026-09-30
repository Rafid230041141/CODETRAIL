package application.client.learning;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GamePreviewBlock provides an interactive 2D physics and game engine preview canvas featuring:
 * - 60 FPS real-time AnimationTimer game loop with delta-time (dt) integration
 * - Interactive player entity with velocity, gravity, jumping, and friction physics
 * - Dynamic collectible targets and AABB collision detection
 * - Real-time telemetry HUD (FPS, dt in ms, coordinates, score, velocity)
 * - Physics parameter sliders (Gravity, Movement Speed)
 * - On-screen tactile controls and keyboard navigation (A/D or Left/Right arrows, Space to jump)
 */
public class GamePreviewBlock implements LearningBlock {
    private final String id;
    private final String title;
    private final String challengeNote;

    public GamePreviewBlock(String id, String title, String challengeNote) {
        this.id = id;
        this.title = title;
        this.challengeNote = (challengeNote != null && !challengeNote.isBlank())
                ? challengeNote
                : "Challenge: Use arrow keys or on-screen buttons to collect the floating energy orbs. Notice how delta-time (dt) ensures fluid movement even if frame rates fluctuate!";
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public CourseCategory category() {
        return CourseCategory.GAME_DEV;
    }

    private static class Coin {
        double x, y, radius;
        boolean collected;

        Coin(double x, double y) {
            this.x = x;
            this.y = y;
            this.radius = 10;
            this.collected = false;
        }
    }

    @Override
    public Node render(boolean isDark) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
                "-fx-background-color: " + (isDark ? "#121a24" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#233345" : "#e2e8f0") + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.image.ImageView gameIconView = application.client.util.LucideIcons.icon("gamepad-2", 18, isDark);
        HBox gameIconBadge = new HBox(gameIconView);
        gameIconBadge.setAlignment(Pos.CENTER);
        gameIconBadge.setPadding(new Insets(3, 4, 3, 4));
        gameIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#500724" : "#fce7f3") + ";" +
                "-fx-border-color: " + (isDark ? "#db2777" : "#ec4899") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("2D GAME ENGINE & PHYSICS CANVAS: " + title.toUpperCase());
        heading.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #ec4899; -fx-letter-spacing: 1.1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label envBadge = new Label("60 FPS Physics Simulation");
        envBadge.setStyle(
                "-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #ec4899; " +
                "-fx-background-color: " + (isDark ? "#500724" : "#fce7f3") + "; " +
                "-fx-padding: 3 8; -fx-background-radius: 6;"
        );

        header.getChildren().addAll(gameIconBadge, heading, spacer, envBadge);

        // Canvas container
        double canvasW = 560;
        double canvasH = 220;
        Canvas canvas = new Canvas(canvasW, canvasH);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Game State Variables
        final double[] playerX = {60.0};
        final double[] playerY = {canvasH - 50.0};
        final double[] playerVx = {0.0};
        final double[] playerVy = {0.0};
        final double playerSize = 24.0;
        final boolean[] onGround = {true};
        final int[] score = {0};
        final boolean[] isRunning = {true};

        // Physics parameters (mutable via sliders)
        final double[] gravityVal = {580.0}; // px/s^2
        final double[] moveSpeedVal = {220.0}; // px/s

        // Coins / Targets
        List<Coin> coins = new ArrayList<>();
        Random rand = new Random(42);
        Runnable spawnCoins = () -> {
            coins.clear();
            for (int i = 0; i < 5; i++) {
                coins.add(new Coin(140 + i * 85 + rand.nextInt(20), 60 + rand.nextInt(70)));
            }
        };
        spawnCoins.run();

        // Telemetry HUD Labels
        Label fpsLabel = new Label("FPS: 60");
        fpsLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: 700;");

        Label dtLabel = new Label("dt: 16.6ms");
        dtLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: 700;");

        Label scoreLabel = new Label("Score: 0 / 5");
        scoreLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11.5px; -fx-text-fill: #f59e0b; -fx-font-weight: 800;");

        Label posLabel = new Label("Pos: (60, 170)");
        posLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        HBox hud = new HBox(16);
        hud.setAlignment(Pos.CENTER_LEFT);
        hud.setPadding(new Insets(4, 10, 4, 10));
        hud.setStyle(
                "-fx-background-color: " + (isDark ? "#080c11" : "#f1f5f9") + ";" +
                "-fx-border-color: " + (isDark ? "#1e293b" : "#cbd5e1") + ";" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );
        hud.getChildren().addAll(fpsLabel, dtLabel, scoreLabel, posLabel);

        // Movement keys state
        final boolean[] keyLeft = {false};
        final boolean[] keyRight = {false};

        // Render Frame Method
        java.util.function.Consumer<Double> updatePhysicsAndRender = (dt) -> {
            if (!isRunning[0]) return;

            // Horizontal Movement
            double targetVx = 0;
            if (keyLeft[0]) targetVx -= moveSpeedVal[0];
            if (keyRight[0]) targetVx += moveSpeedVal[0];
            playerVx[0] = targetVx;

            // Apply gravity
            playerVy[0] += gravityVal[0] * dt;

            // Integrate position: x += vx * dt, y += vy * dt
            playerX[0] += playerVx[0] * dt;
            playerY[0] += playerVy[0] * dt;

            // Ground collision
            double groundY = canvasH - 35.0 - playerSize;
            if (playerY[0] >= groundY) {
                playerY[0] = groundY;
                playerVy[0] = 0;
                onGround[0] = true;
            } else {
                onGround[0] = false;
            }

            // Wall bounds
            if (playerX[0] < 10) playerX[0] = 10;
            if (playerX[0] > canvasW - playerSize - 10) playerX[0] = canvasW - playerSize - 10;

            // Coin collision check (AABB / Circle overlap)
            for (Coin c : coins) {
                if (!c.collected) {
                    double cx = playerX[0] + playerSize / 2.0;
                    double cy = playerY[0] + playerSize / 2.0;
                    double distSq = (cx - c.x) * (cx - c.x) + (cy - c.y) * (cy - c.y);
                    if (distSq <= (c.radius + playerSize / 2.0) * (c.radius + playerSize / 2.0)) {
                        c.collected = true;
                        score[0]++;
                        scoreLabel.setText("Score: " + score[0] + " / 5");
                    }
                }
            }

            // --- DRAW CANVAS ---
            // Clear background
            gc.setFill(isDark ? Color.web("#090e17") : Color.web("#f8fafc"));
            gc.fillRect(0, 0, canvasW, canvasH);

            // Draw grid lines
            gc.setStroke(isDark ? Color.web("#142033") : Color.web("#e2e8f0"));
            gc.setLineWidth(1.0);
            for (int x = 0; x < canvasW; x += 40) {
                gc.strokeLine(x, 0, x, canvasH);
            }
            for (int y = 0; y < canvasH; y += 40) {
                gc.strokeLine(0, y, canvasW, y);
            }

            // Draw Ground Platform
            gc.setFill(isDark ? Color.web("#1e293b") : Color.web("#cbd5e1"));
            gc.fillRect(0, canvasH - 35, canvasW, 35);

            gc.setStroke(isDark ? Color.web("#334155") : Color.web("#94a3b8"));
            gc.setLineWidth(2.0);
            gc.strokeLine(0, canvasH - 35, canvasW, canvasH - 35);

            // Draw Platform Label
            gc.setFill(isDark ? Color.web("#64748b") : Color.web("#475569"));
            gc.fillText("PLATFORM SURFACE: Y = " + (int)(canvasH - 35), 14, canvasH - 12);

            // Draw Coins (Targets)
            for (Coin c : coins) {
                if (!c.collected) {
                    gc.setFill(Color.web("#f59e0b"));
                    gc.fillOval(c.x - c.radius, c.y - c.radius, c.radius * 2, c.radius * 2);

                    gc.setStroke(Color.web("#fef08a"));
                    gc.setLineWidth(1.5);
                    gc.strokeOval(c.x - c.radius, c.y - c.radius, c.radius * 2, c.radius * 2);

                    gc.setFill(Color.web("#78350f"));
                    gc.fillOval(c.x - 3, c.y - 3, 6, 6);
                }
            }

            // Draw Player
            // Glow drop
            gc.setFill(Color.web("#0284c7", 0.25));
            gc.fillRoundRect(playerX[0] - 3, playerY[0] - 3, playerSize + 6, playerSize + 6, 8, 8);

            // Body
            gc.setFill(Color.web("#38bdf8"));
            gc.fillRoundRect(playerX[0], playerY[0], playerSize, playerSize, 6, 6);

            // Eye / Direction indicator
            gc.setFill(Color.web("#0f172a"));
            double eyeOffset = (playerVx[0] >= 0) ? 14 : 4;
            gc.fillOval(playerX[0] + eyeOffset, playerY[0] + 6, 5, 5);

            // Telemetry update
            posLabel.setText(String.format("Pos: (%.0f, %.0f) | Vy: %.0f", playerX[0], playerY[0], playerVy[0]));
        };

        // Animation Timer Loop
        final long[] lastNanoTime = {System.nanoTime()};
        final int[] frameCount = {0};
        final long[] lastFpsUpdate = {System.nanoTime()};

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - lastNanoTime[0]) / 1_000_000_000.0;
                lastNanoTime[0] = now;

                if (dt > 0.1) dt = 0.1; // clamp delta-time spike

                frameCount[0]++;
                if (now - lastFpsUpdate[0] >= 500_000_000L) {
                    int fps = (int) (frameCount[0] * 1_000_000_000L / (now - lastFpsUpdate[0]));
                    fpsLabel.setText("FPS: " + fps);
                    dtLabel.setText(String.format("dt: %.1fms", dt * 1000.0));
                    frameCount[0] = 0;
                    lastFpsUpdate[0] = now;
                }

                updatePhysicsAndRender.accept(dt);
            }
        };
        timer.start();

        // Control Panel
        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button leftBtn = new Button("Move Left");
        leftBtn.setStyle("-fx-background-color: " + (isDark ? "#1e293b" : "#e2e8f0") + "; -fx-text-fill: " + (isDark ? "#f8fafc" : "#0f172a") + "; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 5 12;");
        leftBtn.setOnMousePressed(e -> keyLeft[0] = true);
        leftBtn.setOnMouseReleased(e -> keyLeft[0] = false);

        Button jumpBtn = new Button("Jump (Space)");
        jumpBtn.setStyle("-fx-background-color: #ec4899; -fx-text-fill: #ffffff; -fx-font-weight: 800; -fx-cursor: hand; -fx-padding: 5 14;");
        jumpBtn.setOnAction(e -> {
            if (onGround[0]) {
                playerVy[0] = -340.0; // Jump impulse
                onGround[0] = false;
            }
        });

        Button rightBtn = new Button("Move Right");
        rightBtn.setStyle("-fx-background-color: " + (isDark ? "#1e293b" : "#e2e8f0") + "; -fx-text-fill: " + (isDark ? "#f8fafc" : "#0f172a") + "; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 5 12;");
        rightBtn.setOnMousePressed(e -> keyRight[0] = true);
        rightBtn.setOnMouseReleased(e -> keyRight[0] = false);

        Button resetBtn = new Button("Reset Engine");
        resetBtn.setStyle("-fx-background-color: transparent; -fx-border-color: " + (isDark ? "#334155" : "#cbd5e1") + "; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + "; -fx-cursor: hand; -fx-padding: 5 10;");
        resetBtn.setOnAction(e -> {
            playerX[0] = 60.0;
            playerY[0] = canvasH - 35.0 - playerSize;
            playerVx[0] = 0;
            playerVy[0] = 0;
            score[0] = 0;
            scoreLabel.setText("Score: 0 / 5");
            spawnCoins.run();
        });

        Region ctrlSpacer = new Region();
        HBox.setHgrow(ctrlSpacer, Priority.ALWAYS);

        // Physics Sliders
        Label gravityLabel = new Label("Gravity:");
        gravityLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
        Slider gravitySlider = new Slider(200, 1000, 580);
        gravitySlider.setPrefWidth(90);
        gravitySlider.valueProperty().addListener((obs, oldVal, newVal) -> gravityVal[0] = newVal.doubleValue());

        Label speedLabel = new Label("Speed:");
        speedLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
        Slider speedSlider = new Slider(100, 400, 220);
        speedSlider.setPrefWidth(90);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> moveSpeedVal[0] = newVal.doubleValue());

        controls.getChildren().addAll(leftBtn, jumpBtn, rightBtn, resetBtn, ctrlSpacer, gravityLabel, gravitySlider, speedLabel, speedSlider);

        // Keyboard navigation setup
        card.setFocusTraversable(true);
        card.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) keyLeft[0] = true;
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) keyRight[0] = true;
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP || e.getCode() == KeyCode.W) {
                if (onGround[0]) {
                    playerVy[0] = -340.0;
                    onGround[0] = false;
                }
            }
        });
        card.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) keyLeft[0] = false;
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) keyRight[0] = false;
        });

        // Note
        Label noteLabel = new Label(challengeNote);
        noteLabel.setWrapText(true);
        noteLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: " + (isDark ? "#64748b" : "#64748b") + ";");

        // Canvas container box with border
        VBox canvasBox = new VBox(canvas);
        canvasBox.setAlignment(Pos.CENTER);
        canvasBox.setStyle(
                "-fx-background-color: " + (isDark ? "#090e17" : "#f8fafc") + ";" +
                "-fx-border-color: " + (isDark ? "#1e293b" : "#cbd5e1") + ";" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-overflow: hidden;"
        );

        card.getChildren().addAll(header, hud, canvasBox, controls, noteLabel);
        return card;
    }
}
