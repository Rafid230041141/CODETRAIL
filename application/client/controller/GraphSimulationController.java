package application.client.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import application.algorithm.GraphAlgorithms;
import application.algorithm.GraphAlgorithms.Algorithm;
import application.algorithm.GraphAlgorithms.Edge;
import application.algorithm.GraphAlgorithms.Graph;
import application.algorithm.GraphAlgorithms.Step;
import application.algorithm.GraphAlgorithms.Vertex;
import application.client.dto.ApiModels.SimulationView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;
import javafx.util.StringConverter;

/** Interactive, editable visualizer for the weighted graph algorithms. */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class GraphSimulationController {
    private static final double GRAPH_MARGIN = 54.0;
    private static final double VERTEX_SIZE = 48.0;
    private static final int MAX_VERTICES = 12;

    @FXML
    private ComboBox<Algorithm> algorithmSelector;
    @FXML
    private ComboBox<String> startVertexSelector;
    @FXML
    private Label startVertexLabel;
    @FXML
    private Slider speedSlider;
    @FXML
    private Label speedLabel;
    @FXML
    private Button playPauseButton;
    @FXML
    private Button previousStepButton;
    @FXML
    private Button nextStepButton;
    @FXML
    private Pane graphPane;
    @FXML
    private ToggleButton moveModeButton;
    @FXML
    private ToggleButton addVertexModeButton;
    @FXML
    private ToggleButton addEdgeModeButton;
    @FXML
    private ToggleButton deleteModeButton;
    @FXML
    private Spinner<Integer> edgeWeightSpinner;
    @FXML
    private CheckBox directedEdgeCheckBox;
    @FXML
    private Label editorStatusLabel;
    @FXML
    private Label algorithmTitleLabel;
    @FXML
    private Label algorithmDescriptionLabel;
    @FXML
    private Label complexityLabel;
    @FXML
    private Label stepCounterLabel;
    @FXML
    private Label stepMessageLabel;
    @FXML
    private Label totalWeightLabel;
    @FXML
    private VBox graphPseudocodeBox;
    @FXML
    private ListView<String> valuesList;

    private final ObjectMapper objectMapper;
    private final ToggleGroup editModeGroup = new ToggleGroup();
    private Graph graph = GraphAlgorithms.sampleGraph();
    private List<Step> steps = List.of();
    private Timeline playback;
    private EditMode editMode = EditMode.MOVE;
    private String pendingEdgeStart;
    private String dragVertexId;
    private boolean vertexDragged;
    private boolean updatingSelectors;
    private int stepIndex;

    public GraphSimulationController() {
        this(new ObjectMapper());
    }

    @Autowired
    public GraphSimulationController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @FXML
    private void initialize() {
        algorithmSelector.setItems(FXCollections.observableArrayList(Algorithm.values()));
        algorithmSelector.setConverter(new AlgorithmNameConverter());
        algorithmSelector.setCellFactory(list -> new AlgorithmCell());

        configureEditMode(moveModeButton, EditMode.MOVE);
        configureEditMode(addVertexModeButton, EditMode.ADD_VERTEX);
        configureEditMode(addEdgeModeButton, EditMode.ADD_EDGE);
        configureEditMode(deleteModeButton, EditMode.DELETE);
        editModeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                moveModeButton.setSelected(true);
                return;
            }
            editMode = (EditMode) newValue.getUserData();
            pendingEdgeStart = null;
            pausePlayback();
            editorStatusLabel.setText(editMode.label + " mode");
            renderStep();
        });
        moveModeButton.setSelected(true);
        edgeWeightSpinner.setEditable(false);
        edgeWeightSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-20, 99, 4));
        graphPane.setOnMouseClicked(this::handleCanvasClick);

        refreshStartVertexSelector();
        algorithmSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingSelectors) {
                configureAlgorithmControls(newValue);
                prepareVisualization();
            }
        });
        startVertexSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingSelectors) {
                prepareVisualization();
            }
        });
        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            speedLabel.setText(String.format(Locale.ROOT, "%.1fx", newValue.doubleValue()));
            if (playback != null && playback.getStatus() == Timeline.Status.RUNNING) {
                startPlayback();
            }
        });
        graphPane.widthProperty().addListener((observable, oldValue, newValue) -> renderStep());
        graphPane.heightProperty().addListener((observable, oldValue, newValue) -> renderStep());

        algorithmSelector.getSelectionModel().select(Algorithm.DIJKSTRA);
        configureAlgorithmControls(Algorithm.DIJKSTRA);
        prepareVisualization();
        Platform.runLater(this::renderStep);
    }

    /** Load one server-provided graph simulation and honor its configured start vertex. */
    public void load(SimulationView simulation) {
        stop();
        Algorithm algorithm = algorithmFor(simulation == null ? null : simulation.type());
        graph = GraphAlgorithms.sampleGraph(algorithm);
        pendingEdgeStart = null;
        dragVertexId = null;
        vertexDragged = false;

        String requestedStart = configuredStart(simulation == null ? null : simulation.configJson());
        updatingSelectors = true;
        algorithmSelector.getSelectionModel().select(algorithm);
        refreshStartVertexSelector();
        if (requestedStart != null) {
            startVertexSelector.getSelectionModel().select(requestedStart);
        } else {
            startVertexSelector.getSelectionModel().selectFirst();
        }
        updatingSelectors = false;

        configureAlgorithmControls(algorithm);
        prepareVisualization();
        editorStatusLabel.setText(algorithmName(algorithm) + " preset loaded; edit it on the canvas");
    }

    /** Stop animation when the lesson or application leaves the simulation. */
    public void stop() {
        pausePlayback();
    }

    public void activate() {
        Platform.runLater(this::renderStep);
    }

    public void deactivate() {
        stop();
    }

    public void selectAlgorithm(Algorithm algorithm) {
        if (algorithm == null) {
            return;
        }
        if (algorithmSelector.getValue() == algorithm) {
            prepareVisualization();
        } else {
            algorithmSelector.getSelectionModel().select(algorithm);
        }
    }

    @FXML
    private void togglePlayback() {
        if (playback != null && playback.getStatus() == Timeline.Status.RUNNING) {
            pausePlayback();
            return;
        }
        if (stepIndex >= steps.size() - 1) {
            stepIndex = 0;
            renderStep();
        }
        startPlayback();
    }

    @FXML
    private void showPreviousStep() {
        pausePlayback();
        if (stepIndex > 0) {
            stepIndex--;
            renderStep();
        }
    }

    @FXML
    private void showNextStep() {
        pausePlayback();
        advanceStep();
    }

    @FXML
    private void resetVisualization() {
        prepareVisualization();
    }

    @FXML
    private void restoreSampleGraph() {
        Algorithm algorithm = algorithmSelector.getValue();
        if (algorithm == null) {
            algorithm = Algorithm.DIJKSTRA;
        }
        replaceGraph(GraphAlgorithms.sampleGraph(algorithm), algorithmName(algorithm) + " preset restored");
    }

    @FXML
    private void startBlankGraph() {
        replaceGraph(new Graph(List.of(new Vertex("A", 0.50, 0.50)), List.of()),
                "Blank graph ready; add vertices on the canvas");
    }

    private void configureEditMode(ToggleButton button, EditMode mode) {
        button.setToggleGroup(editModeGroup);
        button.setUserData(mode);
    }

    private void prepareVisualization() {
        Algorithm algorithm = algorithmSelector.getValue();
        String startVertex = startVertexSelector.getValue();
        if (algorithm == null || startVertex == null || graph.vertices().isEmpty()) {
            return;
        }
        pausePlayback();
        startVertexSelector.setDisable(algorithm == Algorithm.KRUSKAL
                || algorithm == Algorithm.FLOYD_WARSHALL);
        steps = GraphAlgorithms.run(graph, algorithm, startVertex);
        stepIndex = 0;
        updateAlgorithmCopy(algorithm);
        renderStep();
    }

    private void configureAlgorithmControls(Algorithm algorithm) {
        if (algorithm == null) {
            return;
        }
        boolean usesSource = algorithm != Algorithm.KRUSKAL && algorithm != Algorithm.FLOYD_WARSHALL;
        startVertexSelector.setDisable(!usesSource);
        startVertexLabel.setText(usesSource ? "START VERTEX"
                : algorithm == Algorithm.FLOYD_WARSHALL ? "ALL VERTICES" : "NOT REQUIRED");
        directedEdgeCheckBox.setSelected(algorithm == Algorithm.BELLMAN_FORD
                || algorithm == Algorithm.FLOYD_WARSHALL);

        int currentWeight = edgeWeightSpinner.getValue() == null ? 4 : edgeWeightSpinner.getValue();
        int minimumWeight = algorithm == Algorithm.DIJKSTRA ? 0 : -20;
        int boundedWeight = Math.max(minimumWeight, Math.min(99, currentWeight));
        edgeWeightSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                minimumWeight, 99, boundedWeight));
    }

    private void updateAlgorithmCopy(Algorithm algorithm) {
        switch (algorithm) {
            case DIJKSTRA -> {
                algorithmTitleLabel.setText("Dijkstra's shortest path");
                algorithmDescriptionLabel.setText(
                        "Finds the lowest-cost distance from one start vertex by repeatedly relaxing edges.");
                complexityLabel.setText("O((V + E) log V) with a priority queue");
            }
            case PRIM -> {
                algorithmTitleLabel.setText("Prim's minimum spanning tree");
                algorithmDescriptionLabel.setText(
                        "Grows one connected tree by choosing the lightest edge that reaches a new vertex.");
                complexityLabel.setText("O(E log V) with a priority queue");
            }
            case KRUSKAL -> {
                algorithmTitleLabel.setText("Kruskal's minimum spanning tree");
                algorithmDescriptionLabel.setText(
                        "Takes edges from lightest to heaviest and rejects any edge that creates a cycle.");
                complexityLabel.setText("O(E log E)");
            }
            case BELLMAN_FORD -> {
                algorithmTitleLabel.setText("Bellman-Ford shortest paths");
                algorithmDescriptionLabel.setText(
                        "Relaxes every directed edge in passes and detects reachable negative cycles.");
                complexityLabel.setText("O(VE)");
            }
            case FLOYD_WARSHALL -> {
                algorithmTitleLabel.setText("Floyd-Warshall all-pairs paths");
                algorithmDescriptionLabel.setText(
                        "Uses each vertex as an intermediate to improve every source-destination distance.");
                complexityLabel.setText("O(V^3)");
            }
        }
    }

    private void startPlayback() {
        pausePlayback();
        if (steps.size() < 2) {
            return;
        }
        double intervalMillis = 1150.0 / speedSlider.getValue();
        playback = new Timeline(new KeyFrame(Duration.millis(intervalMillis), event -> advanceStep()));
        playback.setCycleCount(Timeline.INDEFINITE);
        playback.play();
        playPauseButton.setText("Pause");
    }

    private void pausePlayback() {
        if (playback != null) {
            playback.stop();
        }
        if (playPauseButton != null) {
            playPauseButton.setText("Play");
        }
    }

    private void advanceStep() {
        if (stepIndex < steps.size() - 1) {
            stepIndex++;
            renderStep();
        }
        if (stepIndex >= steps.size() - 1) {
            pausePlayback();
        }
    }

    private void renderStep() {
        if (steps.isEmpty() || graphPane == null) {
            return;
        }
        Step step = steps.get(stepIndex);
        stepCounterLabel.setText("STEP " + (stepIndex + 1) + " OF " + steps.size());
        stepMessageLabel.setText(step.message());
        previousStepButton.setDisable(stepIndex == 0);
        nextStepButton.setDisable(stepIndex == steps.size() - 1);
        totalWeightLabel.setText(stateSummary(algorithmSelector.getValue(), step));
        updatePseudocode(step.pseudocodeLine());
        updateValues(step);
        drawGraph(step);
    }

    private void updatePseudocode(int activeLine) {
        graphPseudocodeBox.getChildren().clear();
        List<String> lines = pseudocodeFor(algorithmSelector.getValue());
        for (int index = 0; index < lines.size(); index++) {
            Label line = new Label((index + 1) + "  " + lines.get(index));
            line.setMaxWidth(Double.MAX_VALUE);
            line.setWrapText(true);
            line.getStyleClass().add("pseudocode-line");
            if (index == activeLine) {
                line.getStyleClass().add("pseudocode-line-active");
            }
            graphPseudocodeBox.getChildren().add(line);
        }
    }

    private List<String> pseudocodeFor(Algorithm algorithm) {
        if (algorithm == null) {
            return List.of();
        }
        return switch (algorithm) {
            case DIJKSTRA -> List.of(
                    "dist[start] = 0; all others = INF",
                    "while an unvisited vertex is reachable",
                    "u = unvisited vertex with smallest dist",
                    "for each unvisited neighbor v of u",
                    "if dist[u] + weight(u, v) < dist[v]",
                    "dist[v] = dist[u] + weight(u, v)");
            case PRIM -> List.of(
                    "key[start] = 0; all others = INF",
                    "while the tree has fewer than V vertices",
                    "u = outside vertex with smallest key",
                    "add u and its parent edge to the tree",
                    "for each edge (u, v) leaving the tree",
                    "if weight(u, v) < key[v]: update key");
            case KRUSKAL -> List.of(
                    "sort edges by increasing weight",
                    "make a separate set for each vertex",
                    "for each edge (u, v) in order",
                    "if find(u) != find(v)",
                    "add edge; union(u, v)",
                    "else reject edge");
            case BELLMAN_FORD -> List.of(
                    "dist[start] = 0; all others = INF",
                    "repeat V - 1 passes",
                    "inspect every directed edge (u, v)",
                    "if dist[u] + weight(u, v) < dist[v]: relax",
                    "stop early when a pass makes no changes",
                    "scan once more to detect a negative cycle");
            case FLOYD_WARSHALL -> List.of(
                    "dist = direct-edge weights; dist[v][v] = 0",
                    "for each intermediate vertex k",
                    "for every source i and destination j",
                    "dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])",
                    "continue until every k is used",
                    "if dist[v][v] < 0: report a negative cycle",
                    "read the completed all-pairs matrix");
        };
    }

    private void updateValues(Step step) {
        List<String> rows = new ArrayList<>();
        Algorithm algorithm = algorithmSelector.getValue();
        if (algorithm == Algorithm.DIJKSTRA || algorithm == Algorithm.BELLMAN_FORD) {
            graph.vertices().stream()
                    .map(Vertex::id)
                    .sorted()
                    .forEach(vertexId -> {
                        int value = step.values().getOrDefault(vertexId, Integer.MAX_VALUE);
                        rows.add(vertexId + "     " + (value == Integer.MAX_VALUE ? "INF" : value));
                    });
        } else if (algorithm == Algorithm.FLOYD_WARSHALL) {
            List<String> vertexIds = graph.vertices().stream().map(Vertex::id).sorted().toList();
            rows.add("      " + String.join("     ", vertexIds));
            for (String from : vertexIds) {
                StringBuilder row = new StringBuilder(from).append("   ");
                for (String to : vertexIds) {
                    int value = step.values().getOrDefault(from + "->" + to, Integer.MAX_VALUE);
                    row.append(value == Integer.MAX_VALUE ? "INF" : value).append("     ");
                }
                rows.add(row.toString().stripTrailing());
            }
        } else if (step.selectedEdgeIds().isEmpty()) {
            rows.add("No edges selected yet");
        } else {
            Map<String, Edge> edgeById = edgeById();
            step.selectedEdgeIds().stream()
                    .map(edgeById::get)
                    .filter(edge -> edge != null)
                    .sorted(Comparator.comparingInt(Edge::weight).thenComparing(Edge::id))
                    .forEach(edge -> rows.add(edge.from() + " - " + edge.to() + "     weight " + edge.weight()));
        }
        valuesList.setItems(FXCollections.observableArrayList(rows));
    }

    private void drawGraph(Step step) {
        double width = Math.max(graphPane.getWidth(), 420.0);
        double height = Math.max(graphPane.getHeight(), 390.0);
        double usableWidth = width - GRAPH_MARGIN * 2;
        double usableHeight = height - GRAPH_MARGIN * 2;
        Map<String, Vertex> vertexById = new HashMap<>();
        graph.vertices().forEach(vertex -> vertexById.put(vertex.id(), vertex));
        graphPane.getChildren().clear();

        for (Edge edge : graph.edges()) {
            Vertex from = vertexById.get(edge.from());
            Vertex to = vertexById.get(edge.to());
            if (from == null || to == null) {
                continue;
            }
            double startX = GRAPH_MARGIN + clamp(from.x()) * usableWidth;
            double startY = GRAPH_MARGIN + clamp(from.y()) * usableHeight;
            double endX = GRAPH_MARGIN + clamp(to.x()) * usableWidth;
            double endY = GRAPH_MARGIN + clamp(to.y()) * usableHeight;

            Line line = new Line(startX, startY, endX, endY);
            line.getStyleClass().add("graph-edge");
            applyEdgeState(line.getStyleClass(), edge.id(), step);
            line.setOnMouseClicked(event -> handleEdgeClick(edge.id(), event));
            graphPane.getChildren().add(line);

            if (edge.directed()) {
                Polygon arrow = directedArrow(startX, startY, endX, endY);
                arrow.getStyleClass().add("graph-edge-arrow");
                applyEdgeState(arrow.getStyleClass(), edge.id(), step);
                arrow.setOnMouseClicked(event -> handleEdgeClick(edge.id(), event));
                graphPane.getChildren().add(arrow);
            }

            Label weight = new Label(Integer.toString(edge.weight()));
            weight.getStyleClass().add("graph-weight");
            applyEdgeState(weight.getStyleClass(), edge.id(), step);
            weight.setOnMouseClicked(event -> handleEdgeClick(edge.id(), event));
            weight.relocate((startX + endX) / 2.0 - 13.0, (startY + endY) / 2.0 - 13.0);
            graphPane.getChildren().add(weight);
        }

        Set<String> activeVertices = activeVertices(step.activeEdgeId());
        for (Vertex vertex : graph.vertices()) {
            StackPane vertexCircle = new StackPane(new Label(vertex.id()));
            vertexCircle.setPrefSize(VERTEX_SIZE, VERTEX_SIZE);
            vertexCircle.setMinSize(VERTEX_SIZE, VERTEX_SIZE);
            vertexCircle.setMaxSize(VERTEX_SIZE, VERTEX_SIZE);
            vertexCircle.getStyleClass().add("graph-vertex");
            if (step.visitedVertices().contains(vertex.id())) {
                vertexCircle.getStyleClass().add(algorithmSelector.getValue() == Algorithm.FLOYD_WARSHALL
                        ? "graph-vertex-active" : "graph-vertex-visited");
            }
            if (activeVertices.contains(vertex.id())) {
                vertexCircle.getStyleClass().add("graph-vertex-active");
            }
            if (vertex.id().equals(pendingEdgeStart)) {
                vertexCircle.getStyleClass().add("graph-vertex-editor-selected");
            }

            VBox vertexGroup = new VBox(3.0, vertexCircle);
            vertexGroup.setAlignment(Pos.CENTER);
            if (algorithmSelector.getValue() == Algorithm.DIJKSTRA
                    || algorithmSelector.getValue() == Algorithm.BELLMAN_FORD) {
                int distance = step.values().getOrDefault(vertex.id(), Integer.MAX_VALUE);
                Label distanceLabel = new Label(distance == Integer.MAX_VALUE ? "INF" : "d=" + distance);
                distanceLabel.getStyleClass().add("graph-distance");
                vertexGroup.getChildren().add(distanceLabel);
            }
            configureVertexInteraction(vertexGroup, vertex.id());
            double x = GRAPH_MARGIN + clamp(vertex.x()) * usableWidth;
            double y = GRAPH_MARGIN + clamp(vertex.y()) * usableHeight;
            vertexGroup.relocate(x - VERTEX_SIZE / 2.0, y - VERTEX_SIZE / 2.0);
            graphPane.getChildren().add(vertexGroup);
        }
    }

    private String stateSummary(Algorithm algorithm, Step step) {
        if (algorithm == Algorithm.PRIM || algorithm == Algorithm.KRUSKAL) {
            return "Selected weight: " + step.totalWeight();
        }
        if (algorithm == Algorithm.FLOYD_WARSHALL) {
            return step.message().toLowerCase(Locale.ROOT).contains("negative cycle")
                    ? "Negative cycle" : "All-pairs distances";
        }
        return "Source: " + startVertexSelector.getValue();
    }

    private Polygon directedArrow(double startX, double startY, double endX, double endY) {
        double length = Math.hypot(endX - startX, endY - startY);
        if (length < 1.0) {
            return new Polygon();
        }
        double unitX = (endX - startX) / length;
        double unitY = (endY - startY) / length;
        double tipX = endX - unitX * (VERTEX_SIZE / 2.0 + 4.0);
        double tipY = endY - unitY * (VERTEX_SIZE / 2.0 + 4.0);
        double baseX = tipX - unitX * 13.0;
        double baseY = tipY - unitY * 13.0;
        double perpendicularX = -unitY * 6.0;
        double perpendicularY = unitX * 6.0;
        return new Polygon(
                tipX, tipY,
                baseX + perpendicularX, baseY + perpendicularY,
                baseX - perpendicularX, baseY - perpendicularY);
    }

    private void configureVertexInteraction(VBox vertexGroup, String vertexId) {
        vertexGroup.setOnMouseClicked(event -> handleVertexClick(vertexId, event));
        vertexGroup.setOnMousePressed(event -> {
            if (editMode == EditMode.MOVE) {
                dragVertexId = vertexId;
                vertexDragged = false;
                pausePlayback();
                event.consume();
            }
        });
        vertexGroup.setOnMouseDragged(event -> {
            if (editMode == EditMode.MOVE && vertexId.equals(dragVertexId)) {
                Point2D point = graphPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                vertexGroup.relocate(clampToCanvasX(point.getX()) - VERTEX_SIZE / 2.0,
                        clampToCanvasY(point.getY()) - VERTEX_SIZE / 2.0);
                vertexDragged = true;
                event.consume();
            }
        });
        vertexGroup.setOnMouseReleased(event -> {
            if (editMode == EditMode.MOVE && vertexId.equals(dragVertexId)) {
                if (vertexDragged) {
                    Point2D point = graphPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                    moveVertex(vertexId, point.getX(), point.getY());
                }
                dragVertexId = null;
                vertexDragged = false;
                event.consume();
            }
        });
    }

    private void handleCanvasClick(MouseEvent event) {
        if (editMode != EditMode.ADD_VERTEX || event.getTarget() != graphPane) {
            return;
        }
        if (graph.vertices().size() >= MAX_VERTICES) {
            editorStatusLabel.setText("Maximum of " + MAX_VERTICES + " vertices reached");
            return;
        }
        double x = normalizedX(event.getX());
        double y = normalizedY(event.getY());
        boolean occupied = graph.vertices().stream()
                .anyMatch(vertex -> Math.hypot(vertex.x() - x, vertex.y() - y) < 0.11);
        if (occupied) {
            editorStatusLabel.setText("That position is occupied");
            return;
        }
        String vertexId = nextVertexId();
        if (vertexId == null) {
            editorStatusLabel.setText("No vertex labels available");
            return;
        }
        List<Vertex> vertices = new ArrayList<>(graph.vertices());
        vertices.add(new Vertex(vertexId, x, y));
        replaceGraph(new Graph(vertices, graph.edges()), "Vertex " + vertexId + " added");
    }

    private void handleVertexClick(String vertexId, MouseEvent event) {
        switch (editMode) {
            case ADD_EDGE -> selectEdgeEndpoint(vertexId);
            case DELETE -> deleteVertex(vertexId);
            case MOVE, ADD_VERTEX -> {
                return;
            }
        }
        event.consume();
    }

    private void selectEdgeEndpoint(String vertexId) {
        pausePlayback();
        if (pendingEdgeStart == null) {
            pendingEdgeStart = vertexId;
            editorStatusLabel.setText("Vertex " + vertexId + " selected");
            renderStep();
            return;
        }
        if (pendingEdgeStart.equals(vertexId)) {
            pendingEdgeStart = null;
            editorStatusLabel.setText("Edge selection cleared");
            renderStep();
            return;
        }
        String from = pendingEdgeStart;
        String to = vertexId;
        pendingEdgeStart = null;
        boolean directed = directedEdgeCheckBox.isSelected();
        boolean duplicate = graph.edges().stream().anyMatch(edge -> overlaps(edge, from, to, directed));
        if (duplicate) {
            editorStatusLabel.setText("That edge already exists");
            renderStep();
            return;
        }
        List<Edge> edges = new ArrayList<>(graph.edges());
        int weight = edgeWeightSpinner.getValue() == null ? 1 : edgeWeightSpinner.getValue();
        String edgeId = edgeId(from, to, directed);
        edges.add(new Edge(edgeId, from, to, weight, directed));
        replaceGraph(new Graph(graph.vertices(), edges), "Edge " + edgeId + " added");
    }

    private void handleEdgeClick(String edgeId, MouseEvent event) {
        if (editMode != EditMode.DELETE) {
            return;
        }
        List<Edge> edges = graph.edges().stream()
                .filter(edge -> !edge.id().equals(edgeId))
                .toList();
        replaceGraph(new Graph(graph.vertices(), edges), "Edge " + edgeId + " deleted");
        event.consume();
    }

    private void deleteVertex(String vertexId) {
        if (graph.vertices().size() == 1) {
            editorStatusLabel.setText("The graph needs at least one vertex");
            return;
        }
        List<Vertex> vertices = graph.vertices().stream()
                .filter(vertex -> !vertex.id().equals(vertexId))
                .toList();
        List<Edge> edges = graph.edges().stream()
                .filter(edge -> !edge.from().equals(vertexId) && !edge.to().equals(vertexId))
                .toList();
        replaceGraph(new Graph(vertices, edges), "Vertex " + vertexId + " deleted");
    }

    private void moveVertex(String vertexId, double canvasX, double canvasY) {
        double x = normalizedX(canvasX);
        double y = normalizedY(canvasY);
        List<Vertex> vertices = graph.vertices().stream()
                .map(vertex -> vertex.id().equals(vertexId) ? new Vertex(vertex.id(), x, y) : vertex)
                .toList();
        replaceGraph(new Graph(vertices, graph.edges()), "Vertex " + vertexId + " moved");
    }

    private void replaceGraph(Graph replacement, String status) {
        pausePlayback();
        graph = replacement;
        pendingEdgeStart = null;
        refreshStartVertexSelector();
        prepareVisualization();
        editorStatusLabel.setText(status);
    }

    private void refreshStartVertexSelector() {
        String selected = startVertexSelector.getValue();
        List<String> vertexIds = graph.vertices().stream()
                .map(Vertex::id)
                .sorted()
                .toList();
        boolean wasUpdating = updatingSelectors;
        updatingSelectors = true;
        startVertexSelector.setItems(FXCollections.observableArrayList(vertexIds));
        if (selected != null && vertexIds.contains(selected)) {
            startVertexSelector.getSelectionModel().select(selected);
        } else {
            startVertexSelector.getSelectionModel().selectFirst();
        }
        updatingSelectors = wasUpdating;
    }

    private void applyEdgeState(List<String> styleClasses, String edgeId, Step step) {
        if (step.rejectedEdgeIds().contains(edgeId)) {
            styleClasses.add("graph-edge-rejected");
        }
        if (step.selectedEdgeIds().contains(edgeId)) {
            styleClasses.add("graph-edge-selected");
        }
        if (edgeId.equals(step.activeEdgeId())) {
            styleClasses.add("graph-edge-active");
        }
        if (editMode == EditMode.DELETE) {
            styleClasses.add("graph-delete-target");
        }
    }

    private Set<String> activeVertices(String activeEdgeId) {
        if (activeEdgeId == null || activeEdgeId.isBlank()) {
            return Set.of();
        }
        return graph.edges().stream()
                .filter(edge -> edge.id().equals(activeEdgeId))
                .findFirst()
                .map(edge -> Set.of(edge.from(), edge.to()))
                .orElse(Set.of());
    }

    private Map<String, Edge> edgeById() {
        Map<String, Edge> result = new HashMap<>();
        graph.edges().forEach(edge -> result.put(edge.id(), edge));
        return result;
    }

    private boolean connects(Edge edge, String left, String right) {
        return edge.from().equals(left) && edge.to().equals(right)
                || edge.from().equals(right) && edge.to().equals(left);
    }

    private boolean overlaps(Edge edge, String from, String to, boolean directed) {
        if (!directed || !edge.directed()) {
            return connects(edge, from, to);
        }
        return edge.from().equals(from) && edge.to().equals(to);
    }

    private String edgeId(String left, String right, boolean directed) {
        if (directed) {
            return left + ">" + right;
        }
        return left.compareTo(right) < 0 ? left + right : right + left;
    }

    private String nextVertexId() {
        Set<String> used = graph.vertices().stream().map(Vertex::id).collect(java.util.stream.Collectors.toSet());
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            String candidate = Character.toString(letter);
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private double normalizedX(double coordinate) {
        double usableWidth = Math.max(graphPane.getWidth(), 420.0) - GRAPH_MARGIN * 2;
        return clamp((coordinate - GRAPH_MARGIN) / usableWidth);
    }

    private double normalizedY(double coordinate) {
        double usableHeight = Math.max(graphPane.getHeight(), 390.0) - GRAPH_MARGIN * 2;
        return clamp((coordinate - GRAPH_MARGIN) / usableHeight);
    }

    private double clampToCanvasX(double coordinate) {
        double width = Math.max(graphPane.getWidth(), 420.0);
        return Math.max(GRAPH_MARGIN, Math.min(width - GRAPH_MARGIN, coordinate));
    }

    private double clampToCanvasY(double coordinate) {
        double height = Math.max(graphPane.getHeight(), 390.0);
        return Math.max(GRAPH_MARGIN, Math.min(height - GRAPH_MARGIN, coordinate));
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private Algorithm algorithmFor(String type) {
        if (type == null || type.isBlank()) {
            return Algorithm.DIJKSTRA;
        }
        try {
            return Algorithm.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Algorithm.DIJKSTRA;
        }
    }

    private String configuredStart(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        try {
            JsonNode config = objectMapper.readTree(configJson);
            if (config == null || !config.isObject()) {
                return null;
            }
            String requested = config.path("start").asText("").trim();
            if (requested.isBlank()) {
                return null;
            }
            return graph.vertices().stream()
                    .map(Vertex::id)
                    .filter(id -> id.equals(requested) || id.equalsIgnoreCase(requested))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String algorithmName(Algorithm algorithm) {
        return switch (algorithm) {
            case DIJKSTRA -> "Dijkstra";
            case PRIM -> "Prim";
            case KRUSKAL -> "Kruskal";
            case BELLMAN_FORD -> "Bellman-Ford";
            case FLOYD_WARSHALL -> "Floyd-Warshall";
        };
    }

    private enum EditMode {
        MOVE("Move"),
        ADD_VERTEX("Add vertex"),
        ADD_EDGE("Add edge"),
        DELETE("Delete");

        private final String label;

        EditMode(String label) {
            this.label = label;
        }
    }

    private static final class AlgorithmNameConverter extends StringConverter<Algorithm> {
        @Override
        public String toString(Algorithm algorithm) {
            return algorithm == null ? "" : algorithmName(algorithm);
        }

        @Override
        public Algorithm fromString(String text) {
            return Algorithm.valueOf(text.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_').replace(' ', '_'));
        }
    }

    private static final class AlgorithmCell extends ListCell<Algorithm> {
        @Override
        protected void updateItem(Algorithm algorithm, boolean empty) {
            super.updateItem(algorithm, empty);
            setText(empty || algorithm == null ? null : algorithmName(algorithm));
        }
    }
}
