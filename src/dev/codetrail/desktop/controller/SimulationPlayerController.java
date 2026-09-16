package dev.codetrail.desktop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.SimulationPalette;
import dev.codetrail.desktop.simulation.SimulationCanvasRenderer;
import dev.codetrail.desktop.simulation.SimulationCursor;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationEngineRegistry;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SimulationTrace;
import dev.codetrail.desktop.simulation.SimulationTypeResolver;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.TraceCaption;
import dev.codetrail.desktop.simulation.TeachingSequence;
import dev.codetrail.desktop.simulation.SimulationTeachingGuide;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.input.SimulationInputAdapter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance JavaFX simulation player supporting 55 VisuAlgo-grade
 * algorithms across Array, Linked List, Tree, Graph, Table, and Stack renderers.
 */
public final class SimulationPlayerController {
    private static final Map<String, Map<String, String>> INPUT_DRAFTS = new LinkedHashMap<>();

    private final SimulationEngineRegistry engines = SimulationEngineRegistry.defaultRegistry();
    private SimulationCanvasRenderer renderer = new SimulationCanvasRenderer();
    private final AtomicInteger generationNumber = new AtomicInteger();
    private Timeline playback;
    private Transition stepMovement;
    private SimulationEngine engine;
    private SimulationMetadata metadata;
    private SimulationTrace trace;
    private SimulationCursor cursor;
    private TeachingSequence teachingSequence;
    private JsonNode originalExampleInput;
    private JsonNode currentInput;
    private boolean teachingMode = true;
    private boolean projectorMode;
    private String currentView = "visualization";
    private boolean focusDiagram;
    private double splitPosition = 0.65;
    private boolean adjustingSplit;
    private String simulationType;
    private SimulationInputAdapter inputAdapter;
    private final Map<String, SimulationInputFieldController> inputFields = new LinkedHashMap<>();
    private Boolean darkModeOverride;
    private Runnable onBackAction;

    @FXML private BorderPane simulationRoot;
    @FXML private Button backButton;
    @FXML private Label simulationTitleLabel;
    @FXML private Label simulationMetaLabel;
    @FXML private Label goalLabel;
    @FXML private MenuButton examplesButton;
    @FXML private MenuItem alternateExampleItem;
    @FXML private ChoiceBox<String> playbackModeChoice;
    @FXML private StackPane canvasContainer;
    @FXML private Canvas simulationCanvas;
    @FXML private Button previousButton;
    @FXML private Button playPauseButton;
    @FXML private Button nextButton;
    @FXML private Button restartButton;
    @FXML private Slider speedSlider;
    @FXML private Label speedValueLabel;
    @FXML private Label stepLabel;
    @FXML private Label narrationLabel;
    @FXML private Label fullNarrationLabel;
    @FXML private Label recordedValuesLabel;
    @FXML private VBox pseudocodeBox;
    @FXML private ScrollPane pseudocodeScrollPane;
    @FXML private FlowPane legendBox;
    @FXML private SplitPane simulationSplit;
    @FXML private ToggleButton focusButton;
    @FXML private TitledPane stepDetailsPane;
    @FXML private VBox algorithmPanel;
    @FXML private VBox inputPanel;
    @FXML private VBox inputFieldsBox;
    @FXML private VBox playbackDock;
    @FXML private Label inputErrorLabel;
    @FXML private Button inputButton;
    @FXML private Button visualizationButton;
    @FXML private Button algorithmButton;
    @FXML private Button runInputButton;
    @FXML private Label statusLabel;

    public SimulationPlayerController() {
    }

    @FXML
    private void initialize() {
        applyProjectorMode();
        if (narrationLabel != null) {
            narrationLabel.setTooltip(new Tooltip());
            narrationLabel.setAccessibleText("Current action. Open Details for recorded state.");
            narrationLabel.widthProperty().addListener((observable, oldValue, newValue) -> updateCaption());
            narrationLabel.heightProperty().addListener((observable, oldValue, newValue) -> updateCaption());
            narrationLabel.fontProperty().addListener((observable, oldValue, newValue) -> updateCaption());
        }
        if (playbackModeChoice != null) {
            playbackModeChoice.getItems().setAll("Teaching steps", "All steps");
            playbackModeChoice.setValue("Teaching steps");
            playbackModeChoice.setTooltip(new Tooltip("Teaching steps focus on decisions and changes. All steps includes setup and implementation detail."));
            playbackModeChoice.valueProperty().addListener((observable, previous, value) -> {
                stopPlayback();
                teachingMode = !"All steps".equals(value);
                if (cursor != null) renderCurrent();
            });
        }
        if (simulationSplit != null && !simulationSplit.getDividers().isEmpty()) {
            simulationSplit.getDividers().getFirst().positionProperty().addListener((observable, previous, value) -> {
                if (!adjustingSplit && "visualization".equals(currentView) && !focusDiagram && !projectorMode) {
                    splitPosition = value.doubleValue();
                }
            });
        }
        playback = new Timeline(new KeyFrame(Duration.millis(850.0), event -> advance()));
        playback.setCycleCount(Timeline.INDEFINITE);
        if (speedSlider != null) {
            speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> updatePlaybackRate());
            updatePlaybackRate();
        }
        if (canvasContainer != null) {
            canvasContainer.widthProperty().addListener((observable, oldValue, newValue) -> resizeCanvas());
            canvasContainer.heightProperty().addListener((observable, oldValue, newValue) -> resizeCanvas());
            canvasContainer.sceneProperty().addListener((observable, oldScene, newScene) -> {
                if (newScene == null) {
                    stopPlayback();
                    saveDraft();
                    generationNumber.incrementAndGet();
                } else {
                    applyCanvasTheme();
                    applyProjectorMode();
                    resizeCanvas();
                }
            });
        }
        if (pseudocodeScrollPane != null) {
            pseudocodeScrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
                if (cursor != null) {
                    ensureActiveLineVisible(cursor.current().highlightedPseudocodeLine());
                }
            });
        }
        if (pseudocodeBox != null) {
            pseudocodeBox.heightProperty().addListener((observable, oldValue, newValue) -> {
                if (cursor != null) {
                    ensureActiveLineVisible(cursor.current().highlightedPseudocodeLine());
                }
            });
        }
        showView("visualization");
        resizeCanvas();
        disablePlayback(true);
    }

    public void load(String rawType, String configJson) {
        this.simulationType = SimulationTypeResolver.resolve(rawType, configJson);
        try {
            engine = engines.require(simulationType);
            metadata = engine.metadata();
        } catch (IllegalArgumentException ex) {
            showStatus("This simulation engine is not registered: " + rawType);
            return;
        }

        if (simulationTitleLabel != null) {
            simulationTitleLabel.setText(metadata.title());
        }
        if (simulationMetaLabel != null) {
            simulationMetaLabel.setText(metadata.timeComplexity() + " time · " + metadata.spaceComplexity() + " space");
        }
        if (metadata != null && metadata.rendererFamily() == RendererFamily.GRAPH) {
            this.splitPosition = 0.72;
            if (canvasContainer != null) {
                canvasContainer.setMinHeight(480);
                canvasContainer.setPrefHeight(580);
            }
            if (simulationRoot != null) {
                simulationRoot.setMinHeight(860);
                simulationRoot.setPrefHeight(940);
            }
            Platform.runLater(() -> {
                if (simulationRoot != null && simulationRoot.getScene() != null
                        && simulationRoot.getScene().getWindow() instanceof Stage stage) {
                    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                    stage.setX(bounds.getMinX());
                    stage.setY(bounds.getMinY());
                    stage.setWidth(bounds.getWidth());
                    stage.setHeight(bounds.getHeight());
                    stage.setMaximized(true);
                }
            });
        }
        renderPseudocode();
        renderLegend();

        originalExampleInput = SimulationTypeResolver.normalizeInput(engine, configJson);
        if (alternateExampleItem != null) {
            alternateExampleItem.setText(SimulationTeachingGuide.alternateLabel(metadata.type()));
            alternateExampleItem.setDisable(SimulationTeachingGuide.alternateInput(metadata.type()) == null);
        }
        if (examplesButton != null) {
            examplesButton.setDisable(false);
        }
        try {
            configureInputForm(originalExampleInput);
        } catch (Exception ex) {
            // continue without custom input form
        }
        generateTrace(originalExampleInput, "Example");
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkModeOverride = darkMode;
        applyCanvasTheme();
    }

    public boolean isDarkMode() {
        if (darkModeOverride != null) {
            return darkModeOverride;
        }
        if (simulationRoot != null && simulationRoot.getScene() != null) {
            Parent root = simulationRoot.getScene().getRoot();
            if (root != null) {
                return root.getStyleClass().contains("dark-theme");
            }
        }
        return false;
    }

    public void setPresentationMode(boolean presentationMode) {
        this.projectorMode = presentationMode;
        applyProjectorMode();
    }

    @FXML
    private void onBack() {
        stopPlayback();
        saveDraft();
        if (onBackAction != null) {
            onBackAction.run();
            return;
        }
        if (simulationRoot != null && simulationRoot.getScene() != null
                && simulationRoot.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

    @FXML private void onShowVisualization() { showView("visualization"); }
    @FXML private void onShowAlgorithm() { showView("algorithm"); }
    @FXML private void onToggleFocus() {
        focusDiagram = focusButton != null && focusButton.isSelected();
        showView("visualization");
    }
    @FXML private void onCaptionDetails() {
        if (algorithmPanel != null && !algorithmPanel.isVisible()) showView("algorithm");
        if (stepDetailsPane != null) stepDetailsPane.setExpanded(true);
    }
    @FXML private void onCaptionKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            onCaptionDetails();
            event.consume();
        }
    }
    @FXML private void onShowInput() { stopPlayback(); showView("input"); }
    @FXML private void onRunOriginalExample() {
        if (originalExampleInput == null) return;
        saveDraft();
        generateTrace(originalExampleInput, "Example");
    }
    @FXML private void onRunAlternateExample() {
        if (metadata == null) return;
        saveDraft();
        JsonNode alternate = SimulationTeachingGuide.alternateInput(metadata.type());
        if (alternate != null && !alternate.isNull()) generateTrace(alternate, "Alternate example");
    }
    @FXML private void onResetExample() {
        if (inputAdapter == null) return;
        inputAdapter.example().forEach((key, value) -> {
            SimulationInputFieldController field = inputFields.get(key);
            if (field != null) field.setValue(value);
        });
        if (inputErrorLabel != null) {
            inputErrorLabel.setText("");
            inputErrorLabel.setVisible(false);
            inputErrorLabel.setManaged(false);
        }
        saveDraft();
    }

    @FXML
    private void onPrevious() {
        if (cursor == null) return;
        stopPlayback();
        showStep(previousIndex());
    }

    @FXML
    private void onPlayPause() {
        if (cursor == null) {
            showStatus("The simulation is still loading.");
            return;
        }
        if (playback.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            stopPlayback();
            return;
        }
        if (atLastStep()) {
            showStatus("The trace is at its final step. Restart to play it again.");
            return;
        }
        updatePlaybackRate();
        playback.play();
        if (playPauseButton != null) playPauseButton.setText("Pause");
        showStatus("Playing the generated trace.");
    }

    @FXML
    private void onNext() {
        if (cursor == null) return;
        showStep(nextIndex());
        if (atLastStep()) {
            stopPlayback(false);
        }
    }

    @FXML
    private void onRestart() {
        if (trace == null) return;
        stopPlayback();
        cursor = trace.cursor();
        moveToIndex(firstIndex());
        renderCurrent();
        showStatus("Trace restarted.");
    }

    @FXML
    private void onRunCustomInput() {
        if (engine == null || metadata == null) {
            showStatus("The simulation is still loading.");
            return;
        }
        final JsonNode input;
        try {
            saveDraft();
            input = inputAdapter.parse(fieldValues());
        } catch (IllegalArgumentException ex) {
            showInputError(ex.getMessage());
            return;
        }
        if (inputErrorLabel != null) {
            inputErrorLabel.setVisible(false);
            inputErrorLabel.setManaged(false);
        }
        generateTrace(input, "Custom input");
    }

    public void stopPlayback() { stopPlayback(true); }

    public void stopPlayback(boolean settleMovement) {
        if (settleMovement) {
            boolean settling = stepMovement != null;
            stopMovement();
            if (settling && cursor != null) renderCurrent();
        }
        if (playback != null) {
            playback.stop();
        }
        if (playPauseButton != null) {
            playPauseButton.setText("Play");
        }
    }

    private void generateTrace(JsonNode input, String source) {
        if (engine == null) return;
        stopPlayback();
        disablePlayback(true);
        if (runInputButton != null) {
            runInputButton.setDisable(true);
            runInputButton.setText("Preparing…");
        }
        if (examplesButton != null) examplesButton.setDisable(true);
        showStatus("Preparing the simulation…");
        int request = generationNumber.incrementAndGet();
        JsonNode requestedInput = input.deepCopy();
        CompletableFuture.supplyAsync(() -> engine.generateTrace(requestedInput))
                .whenComplete((generated, failure) -> UiSupport.fx(() -> {
                    if (request != generationNumber.get()) return;
                    if (runInputButton != null) {
                        runInputButton.setDisable(false);
                        runInputButton.setText("Run simulation");
                    }
                    if (examplesButton != null) examplesButton.setDisable(false);
                    if (failure != null) {
                        String message = inputAdapter == null ? "Check your input and try again." : inputAdapter.friendlyError(UiSupport.errorMessage(failure));
                        if (inputPanel != null && inputPanel.isVisible()) showInputError(message);
                        else showStatus(message);
                        disablePlayback(cursor == null);
                        return;
                    }
                    if (generated == null || generated.isEmpty()) {
                        showStatus("The simulation engine returned no steps.");
                        return;
                    }
                    trace = generated;
                    currentInput = requestedInput.deepCopy();
                    teachingSequence = TeachingSequence.of(generated);
                    if (goalLabel != null) {
                        goalLabel.setText(SimulationTeachingGuide.goal(metadata.type(), currentInput));
                    }
                    renderer = new SimulationCanvasRenderer(generated);
                    renderer.setDarkMode(isDarkMode());
                    renderer.setProjectorMode(projectorMode);
                    cursor = generated.cursor();
                    moveToIndex(firstIndex());
                    disablePlayback(false);
                    renderCurrent();
                    showView("visualization");
                    showStatus(source + " loaded. Use the controls to inspect each step.");
                }));
    }

    private void renderCurrent() {
        stopMovement();
        if (cursor == null || metadata == null || simulationCanvas == null) return;
        SimulationStep step = cursor.current();
        renderer.setDarkMode(isDarkMode());
        renderer.setProjectorMode(projectorMode);
        renderer.render(simulationCanvas.getGraphicsContext2D(), step,
                Math.max(1.0, simulationCanvas.getWidth()), Math.max(1.0, simulationCanvas.getHeight()));

        if (stepLabel != null) {
            if (teachingMode && teachingSequence != null) {
                int ordinal = teachingSequence.indices().indexOf(cursor.index()) + 1;
                stepLabel.setText(ordinal > 0 ? "Step " + ordinal + " of " + teachingSequence.size()
                        : "Trace step " + (cursor.index() + 1));
            } else {
                stepLabel.setText("Step " + (cursor.index() + 1) + " of " + cursor.size());
            }
        }
        updateCaption();
        if (pseudocodeBox != null) {
            for (int index = 0; index < pseudocodeBox.getChildren().size(); index++) {
                Node node = pseudocodeBox.getChildren().get(index);
                if (index + 1 == step.highlightedPseudocodeLine()) {
                    node.getStyleClass().setAll("simulation-code-line", "simulation-code-line-active");
                } else {
                    node.getStyleClass().setAll("simulation-code-line");
                }
            }
        }
        ensureActiveLineVisible(step.highlightedPseudocodeLine());
        if (previousButton != null) previousButton.setDisable(atFirstStep());
        if (nextButton != null) nextButton.setDisable(atLastStep());
    }

    private void updateCaption() {
        if (cursor == null || trace == null || metadata == null || narrationLabel == null) return;
        TraceCaption caption = TraceCaption.at(trace, cursor.index());
        if (fullNarrationLabel != null) fullNarrationLabel.setText(caption.activeLine());
        if (recordedValuesLabel != null) recordedValuesLabel.setText(caption.recordedValues());
        if (narrationLabel.getTooltip() != null) {
            narrationLabel.getTooltip().setText(caption.narration() + "\nOpen Details for recorded state.");
        }
        narrationLabel.setText(caption.narration());
    }

    private int firstIndex() {
        return teachingMode && teachingSequence != null ? teachingSequence.firstIndex() : 0;
    }

    private int lastIndex() {
        return teachingMode && teachingSequence != null ? teachingSequence.lastIndex() : (trace != null ? trace.size() - 1 : 0);
    }

    private int nextIndex() {
        return teachingMode && teachingSequence != null ? teachingSequence.nextIndex(cursor.index())
                : Math.min(trace != null ? trace.size() - 1 : 0, cursor.index() + 1);
    }

    private int previousIndex() {
        return teachingMode && teachingSequence != null ? teachingSequence.previousIndex(cursor.index())
                : Math.max(0, cursor.index() - 1);
    }

    private boolean atFirstStep() { return cursor == null || cursor.index() <= firstIndex(); }
    private boolean atLastStep() { return cursor == null || cursor.index() >= lastIndex(); }

    private void moveToIndex(int targetIndex) {
        if (cursor == null || trace == null) return;
        int target = Math.max(0, Math.min(trace.size() - 1, targetIndex));
        while (cursor.index() < target) cursor = cursor.next();
        while (cursor.index() > target) cursor = cursor.previous();
    }

    private void showStep(int targetIndex) {
        if (cursor == null || simulationCanvas == null) return;
        int prevIdx = cursor.index();
        SimulationStep previous = cursor.current();
        moveToIndex(targetIndex);
        renderCurrent();
        if (cursor.index() <= prevIdx || simulationCanvas.getScene() == null) return;
        SimulationStep current = cursor.current();
        Transition movement = new Transition() {
            { setCycleDuration(Duration.millis(240)); }
            @Override protected void interpolate(double progress) {
                renderer.renderTransition(simulationCanvas.getGraphicsContext2D(), previous, current,
                        Math.max(1.0, simulationCanvas.getWidth()),
                        Math.max(1.0, simulationCanvas.getHeight()), progress);
            }
        };
        stepMovement = movement;
        movement.setOnFinished(event -> {
            if (stepMovement == movement) {
                stepMovement = null;
                renderCurrent();
            }
        });
        movement.playFromStart();
    }

    private void stopMovement() {
        if (stepMovement == null) return;
        stepMovement.stop();
        stepMovement = null;
    }

    private void advance() {
        if (cursor == null || atLastStep()) {
            stopPlayback();
            return;
        }
        showStep(nextIndex());
        if (atLastStep()) {
            stopPlayback(false);
        }
    }

    private void renderPseudocode() {
        if (pseudocodeBox == null || metadata == null) return;
        pseudocodeBox.getChildren().clear();
        pseudocodeBox.setFillWidth(true);
        pseudocodeBox.setMaxWidth(Double.MAX_VALUE);
        for (int index = 0; index < metadata.pseudocode().size(); index++) {
            String line = metadata.pseudocode().get(index);
            HBox row = new HBox(10.0);
            row.setMinWidth(0.0);
            row.setMaxWidth(Double.MAX_VALUE);
            row.getStyleClass().add("simulation-code-line");

            Label lineNumber = new Label(Integer.toString(index + 1));
            lineNumber.setMinWidth(26.0);
            lineNumber.setPrefWidth(26.0);
            lineNumber.setMaxWidth(26.0);
            lineNumber.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
            lineNumber.getStyleClass().add("simulation-code-line-number");

            Label codeLine = new Label(line.stripLeading());
            codeLine.setAccessibleText(line);
            String indentation = line.substring(0, line.length() - line.stripLeading().length());
            Runnable alignIndentation = () -> {
                Text measure = new Text(indentation);
                measure.setFont(codeLine.getFont());
                HBox.setMargin(codeLine, new Insets(0, 0, 0, measure.getLayoutBounds().getWidth() * 0.35));
            };
            codeLine.fontProperty().addListener((observable, oldFont, newFont) -> alignIndentation.run());
            alignIndentation.run();
            codeLine.setWrapText(true);
            codeLine.setMinWidth(0.0);
            codeLine.setMaxWidth(Double.MAX_VALUE);
            codeLine.getStyleClass().add("simulation-code-line-text");
            HBox.setHgrow(codeLine, Priority.ALWAYS);
            row.getChildren().addAll(lineNumber, codeLine);
            pseudocodeBox.getChildren().add(row);
        }
    }

    private void renderLegend() {
        if (legendBox == null) return;
        legendBox.getChildren().clear();
        for (SnapshotStatus status : SnapshotStatus.values()) {
            HBox row = new HBox(8.0);
            Region swatch = new Region();
            swatch.setMinSize(12.0, 12.0);
            swatch.setPrefSize(12.0, 12.0);
            swatch.setMaxSize(12.0, 12.0);
            swatch.setBackground(new Background(new BackgroundFill(
                    SimulationPalette.statusColor(status, isDarkMode()),
                    new CornerRadii(3), Insets.EMPTY)));
            swatch.getStyleClass().add("simulation-legend-swatch");
            Label name = new Label(statusName(status));
            name.getStyleClass().add("simulation-legend-item");
            row.getChildren().addAll(swatch, name);
            legendBox.getChildren().add(row);
        }
    }

    private void resizeCanvas() {
        if (canvasContainer == null || simulationCanvas == null) return;
        double width = canvasContainer.getWidth();
        double height = canvasContainer.getHeight();
        Insets insets = canvasContainer.getInsets();
        double availableWidth = width - (insets != null ? insets.getLeft() + insets.getRight() : 0);
        double availableHeight = height - (insets != null ? insets.getTop() + insets.getBottom() : 0);
        if (availableWidth > 0.0) {
            simulationCanvas.setWidth(Math.max(1.0, availableWidth));
        }
        if (availableHeight > 0.0) {
            simulationCanvas.setHeight(Math.max(1.0, availableHeight));
        }
        if (cursor != null) {
            renderCurrent();
        }
    }

    private void ensureActiveLineVisible(int highlightedLine) {
        if (pseudocodeScrollPane == null || pseudocodeBox == null
                || highlightedLine <= 0 || highlightedLine > pseudocodeBox.getChildren().size()) {
            return;
        }
        Node activeLine = pseudocodeBox.getChildren().get(highlightedLine - 1);
        Platform.runLater(() -> {
            if (activeLine.getScene() == null || activeLine.getScene().getWindow() == null
                    || !activeLine.getScene().getWindow().isShowing()) {
                return;
            }
            pseudocodeBox.applyCss();
            pseudocodeBox.layout();
            Bounds viewport = pseudocodeScrollPane.getViewportBounds();
            Bounds content = pseudocodeBox.getLayoutBounds();
            Bounds line = activeLine.getBoundsInParent();
            double viewportHeight = viewport.getHeight();
            double contentHeight = content.getHeight();
            double scrollableHeight = contentHeight - viewportHeight;
            if (!(viewportHeight > 0.0) || !(scrollableHeight > 0.0)) {
                return;
            }
            double currentTop = pseudocodeScrollPane.getVvalue() * scrollableHeight;
            double targetTop = currentTop;
            if (line.getHeight() > viewportHeight || line.getMinY() < currentTop) {
                targetTop = Math.max(0.0, line.getMinY() - 8.0);
            } else if (line.getMaxY() > currentTop + viewportHeight) {
                targetTop = Math.min(scrollableHeight, line.getMaxY() - viewportHeight + 8.0);
            }
            pseudocodeScrollPane.setVvalue(targetTop / scrollableHeight);
        });
    }

    private void applyCanvasTheme() {
        renderer.setDarkMode(isDarkMode());
        if (legendBox != null) renderLegend();
        if (cursor != null) renderCurrent();
    }

    private void applyProjectorMode() {
        if (simulationRoot != null) {
            simulationRoot.getStyleClass().remove("simulation-projector");
            if (projectorMode) simulationRoot.getStyleClass().add("simulation-projector");
        }
        if (playbackModeChoice != null) {
            double modeWidth = projectorMode ? 220 : 165;
            playbackModeChoice.setMinWidth(modeWidth);
            playbackModeChoice.setPrefWidth(modeWidth);
            playbackModeChoice.setMaxWidth(modeWidth);
        }
        if (simulationSplit != null && focusButton != null) applyWorkspaceLayout();
        renderer.setProjectorMode(projectorMode);
        if (cursor != null) {
            renderCurrent();
            ensureActiveLineVisible(cursor.current().highlightedPseudocodeLine());
        }
    }

    private void configureInputForm(JsonNode example) throws IOException {
        inputAdapter = new SimulationInputAdapter(metadata);
        Map<String, String> values = inputAdapter.display(example);
        if (simulationType != null) {
            values = INPUT_DRAFTS.getOrDefault(simulationType, values);
        }
        inputFields.clear();
        if (inputFieldsBox == null) return;
        inputFieldsBox.getChildren().clear();
        for (SimulationInputAdapter.Field field : inputAdapter.fields()) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dev/codetrail/desktop/fxml/SimulationInputField.fxml"));
            Node row = loader.load();
            SimulationInputFieldController controller = loader.getController();
            controller.configure(field, values.getOrDefault(field.key(), inputAdapter.example().get(field.key())));
            inputFields.put(field.key(), controller);
            inputFieldsBox.getChildren().add(row);
        }
        if (inputButton != null) inputButton.setDisable(false);
    }

    private Map<String, String> fieldValues() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        inputFields.forEach((key, controller) -> values.put(key, controller.value()));
        return values;
    }

    private void saveDraft() {
        if (simulationType != null && inputAdapter != null) {
            INPUT_DRAFTS.put(simulationType, fieldValues());
        }
    }

    private void showView(String view) {
        currentView = view;
        applyWorkspaceLayout();
    }

    private void applyWorkspaceLayout() {
        if (simulationSplit == null) return;
        boolean input = "input".equals(currentView);
        boolean codeOnly = "algorithm".equals(currentView);
        boolean diagramOnly = !codeOnly && (projectorMode || focusDiagram);
        adjustingSplit = true;
        simulationSplit.setVisible(!input);
        simulationSplit.setManaged(!input);
        if (inputPanel != null) {
            inputPanel.setVisible(input);
            inputPanel.setManaged(input);
        }
        if (playbackDock != null) {
            playbackDock.setVisible(!input);
            playbackDock.setManaged(!input);
        }

        if (canvasContainer != null) {
            canvasContainer.setVisible(!codeOnly);
            canvasContainer.setMinHeight(codeOnly ? 0 : diagramOnly ? 0 : 220);
            canvasContainer.setMaxHeight(codeOnly ? 0 : Double.MAX_VALUE);
            canvasContainer.setMinWidth(0);
            canvasContainer.setMaxWidth(Double.MAX_VALUE);
        }
        if (algorithmPanel != null) {
            algorithmPanel.setVisible(!diagramOnly);
            algorithmPanel.setMinHeight(diagramOnly ? 0 : codeOnly ? 0 : 150);
            algorithmPanel.setMaxHeight(diagramOnly ? 0 : Double.MAX_VALUE);
            algorithmPanel.setMinWidth(0);
            algorithmPanel.setMaxWidth(Double.MAX_VALUE);
        }
        simulationSplit.getStyleClass().remove("simulation-single-pane");
        if (codeOnly || diagramOnly) simulationSplit.getStyleClass().add("simulation-single-pane");
        simulationSplit.setDividerPositions(codeOnly ? 0 : diagramOnly ? 1 : splitPosition);
        if (focusButton != null) {
            focusButton.setSelected(diagramOnly);
            focusButton.setDisable(input || projectorMode);
        }
        if (visualizationButton != null) {
            visualizationButton.setDisable(!input && !codeOnly);
            visualizationButton.setVisible(input || codeOnly);
            visualizationButton.setManaged(input || codeOnly);
        }
        if (algorithmButton != null) algorithmButton.setDisable(codeOnly);
        if (inputButton != null) inputButton.setDisable(input || inputAdapter == null);
        Platform.runLater(() -> {
            if (simulationSplit.getScene() == null || simulationSplit.getScene().getWindow() == null
                    || !simulationSplit.getScene().getWindow().isShowing()) {
                adjustingSplit = false;
                return;
            }
            simulationSplit.applyCss();
            simulationSplit.layout();
            adjustingSplit = false;
            if (!input && !codeOnly) resizeCanvas();
            if (!input && !diagramOnly && cursor != null)
                ensureActiveLineVisible(cursor.current().highlightedPseudocodeLine());
            updateCaption();
        });
    }

    private void showInputError(String message) {
        if (inputErrorLabel == null) return;
        inputErrorLabel.setText(message);
        inputErrorLabel.setVisible(true);
        inputErrorLabel.setManaged(true);
    }

    private void showStatus(String message) {
        if (statusLabel == null) return;
        statusLabel.setText(message);
        boolean show = !(message.endsWith("loaded. Use the controls to inspect each step.")
                || message.equals("Trace restarted.") || message.equals("Playing the generated trace."));
        statusLabel.setVisible(show);
        statusLabel.setManaged(show);
    }

    private void disablePlayback(boolean disabled) {
        if (previousButton != null) previousButton.setDisable(disabled || atFirstStep());
        if (nextButton != null) nextButton.setDisable(disabled || atLastStep());
        if (playPauseButton != null) playPauseButton.setDisable(disabled);
        if (restartButton != null) restartButton.setDisable(disabled);
        if (playbackModeChoice != null) playbackModeChoice.setDisable(disabled);
    }

    private void updatePlaybackRate() {
        if (playback == null || speedSlider == null) return;
        double rate = speedSlider.getValue();
        playback.setRate(rate);
        if (speedValueLabel != null) {
            speedValueLabel.setText(String.format(Locale.ROOT, "%.2f×", rate));
        }
    }

    private String statusName(SnapshotStatus status) {
        return switch (status) {
            case DEFAULT -> "Structure";
            case ACTIVE -> "Current";
            case DONE -> "Result";
            case REJECTED -> "Rejected";
        };
    }
}
