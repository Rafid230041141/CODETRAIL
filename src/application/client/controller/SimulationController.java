package application.client.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import application.client.dto.ApiModels.SimulationView;
import application.client.simulation.SimulationEngine;
import application.client.simulation.SimulationEngine.SimulationRun;
import application.client.simulation.SimulationEngine.SimulationStep;
import application.client.simulation.StepVisualizationRenderer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class SimulationController {
    private static final java.util.Set<String> GRAPH_ALGORITHMS = java.util.Set.of(
            "DIJKSTRA", "PRIM", "KRUSKAL", "BELLMAN_FORD", "FLOYD_WARSHALL");

    @FXML
    private VBox genericSimulation;
    @FXML
    private Parent graphSimulation;
    @FXML
    private GraphSimulationController graphSimulationController;
    @FXML
    private Label simulationTypeLabel;
    @FXML
    private Label simulationMessageLabel;
    @FXML
    private Label stepLabel;
    @FXML
    private VBox inputEditor;
    @FXML
    private FlowPane inputControlsPane;
    @FXML
    private Label inputStatusLabel;
    @FXML
    private Button applyInputsButton;
    @FXML
    private Pane visualizationPane;
    @FXML
    private VBox pseudocodeBox;
    @FXML
    private Button previousButton;
    @FXML
    private Button playButton;
    @FXML
    private Button nextButton;

    private final ObjectMapper objectMapper;
    private final Map<String, Control> inputControls = new LinkedHashMap<>();
    private SimulationRun run = new SimulationRun(java.util.List.of(), java.util.List.of());
    private Timeline timeline;
    private int stepIndex;
    private String simulationType = "";
    private String originalConfigJson = "{}";
    private ObjectNode inputConfig;

    public SimulationController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @FXML
    private void initialize() {
        showGraphSimulation(false);
    }

    public void load(SimulationView simulation) {
        stop();
        if (isGraphAlgorithm(simulation.type())) {
            showGraphSimulation(true);
            graphSimulationController.load(simulation);
            return;
        }
        showGraphSimulation(false);
        simulationType = simulation.type() == null ? "" : simulation.type();
        simulationTypeLabel.setText(readableType(simulation.type()));
        originalConfigJson = simulation.configJson() == null ? "{}" : simulation.configJson();
        inputConfig = readObjectConfig(originalConfigJson);
        buildInputEditor();
        rebuildRun(inputConfig);
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        if (playButton != null) {
            playButton.setText("Play");
        }
        if (graphSimulationController != null) {
            graphSimulationController.stop();
        }
    }

    private void showGraphSimulation(boolean graphVisible) {
        if (genericSimulation != null) {
            genericSimulation.setVisible(!graphVisible);
            genericSimulation.setManaged(!graphVisible);
        }
        if (graphSimulation != null) {
            graphSimulation.setVisible(graphVisible);
            graphSimulation.setManaged(graphVisible);
        }
    }

    private boolean isGraphAlgorithm(String type) {
        return type != null && GRAPH_ALGORITHMS.contains(type.toUpperCase());
    }

    @FXML
    private void applyInputs() {
        stop();
        try {
            ObjectNode nextConfig = readInputControls();
            SimulationRun nextRun = SimulationEngine.build(objectMapper, simulationType, nextConfig.toString());
            if (nextRun.steps().isEmpty()) {
                throw new IllegalArgumentException("The selected input produced no steps.");
            }
            inputConfig = nextConfig;
            run = nextRun;
            stepIndex = 0;
            buildInputEditor();
            renderPseudocode();
            render();
            showInputStatus("Inputs applied.", false);
        } catch (IllegalArgumentException exception) {
            showInputStatus(exception.getMessage(), true);
        }
    }

    @FXML
    private void restoreExampleInputs() {
        stop();
        inputConfig = readObjectConfig(originalConfigJson);
        buildInputEditor();
        rebuildRun(inputConfig);
        showInputStatus("Example restored.", false);
    }

    @FXML
    private void previousStep() {
        stop();
        if (stepIndex > 0) {
            stepIndex--;
            render();
        }
    }

    @FXML
    private void nextStep() {
        stop();
        if (stepIndex < run.steps().size() - 1) {
            stepIndex++;
            render();
        }
    }

    @FXML
    private void reset() {
        stop();
        stepIndex = 0;
        render();
    }

    @FXML
    private void togglePlay() {
        if (timeline != null && timeline.getStatus() == Timeline.Status.RUNNING) {
            stop();
            return;
        }
        if (stepIndex >= run.steps().size() - 1) {
            stepIndex = 0;
            render();
        }
        timeline = new Timeline(new KeyFrame(Duration.millis(750), event -> {
            if (stepIndex >= run.steps().size() - 1) {
                stop();
            } else {
                stepIndex++;
                render();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        playButton.setText("Pause");
        timeline.play();
    }

    private void render() {
        visualizationPane.getChildren().clear();
        if (run.steps().isEmpty()) {
            simulationMessageLabel.setText("No steps are available for this simulation.");
            stepLabel.setText("0 / 0");
            return;
        }

        SimulationStep step = run.steps().get(stepIndex);
        StepVisualizationRenderer.render(visualizationPane, simulationType, step);
        simulationMessageLabel.setText(step.message());
        stepLabel.setText((stepIndex + 1) + " / " + run.steps().size());
        previousButton.setDisable(stepIndex == 0);
        nextButton.setDisable(stepIndex >= run.steps().size() - 1);
        highlightPseudocode(step.codeLine());
    }

    private void renderPseudocode() {
        pseudocodeBox.getChildren().clear();
        for (int index = 0; index < run.pseudocode().size(); index++) {
            Label line = new Label((index + 1) + "  " + run.pseudocode().get(index));
            line.setWrapText(true);
            line.getStyleClass().add("simulation-code-line");
            pseudocodeBox.getChildren().add(line);
        }
    }

    private void rebuildRun(ObjectNode config) {
        run = SimulationEngine.build(objectMapper, simulationType, config.toString());
        stepIndex = 0;
        renderPseudocode();
        render();
    }

    private ObjectNode readObjectConfig(String json) {
        try {
            JsonNode parsed = objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
            return parsed != null && parsed.isObject()
                    ? ((ObjectNode) parsed).deepCopy() : objectMapper.createObjectNode();
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private void buildInputEditor() {
        inputControls.clear();
        inputControlsPane.getChildren().clear();
        hideInputStatus();
        inputConfig.properties().forEach(entry -> {
            if (simulationType.equalsIgnoreCase("FLOYD_WARSHALL") && entry.getKey().equals("vertices")) {
                return;
            }
            Control control = createInputControl(entry.getKey(), entry.getValue());
            inputControls.put(entry.getKey(), control);
            Label label = new Label(inputLabel(entry.getKey(), entry.getValue()));
            label.getStyleClass().add("control-label");
            VBox group = new VBox(4.0, label, control);
            group.getStyleClass().add("simulation-input-group");
            inputControlsPane.getChildren().add(group);
        });
        boolean available = !inputControls.isEmpty();
        inputEditor.setManaged(available);
        inputEditor.setVisible(available);
        applyInputsButton.setDisable(!available);
    }

    private Control createInputControl(String field, JsonNode value) {
        Control control;
        if (usesPresetDropdown(field)) {
            ComboBox<InputChoice> comboBox = new ComboBox<>(
                    FXCollections.observableArrayList(presetChoices(field, value)));
            comboBox.getSelectionModel().selectFirst();
            comboBox.setPrefWidth(field.equals("matrix") ? 220.0 : 240.0);
            comboBox.getStyleClass().add("simulation-input");
            String hint = inputHint(field);
            if (!hint.isBlank()) {
                comboBox.setTooltip(new Tooltip(hint));
            }
            control = comboBox;
        } else if (value.isBoolean()) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(value.asBoolean());
            checkBox.getStyleClass().add("simulation-input-check");
            control = checkBox;
        } else if (value.isIntegralNumber()) {
            int current = value.asInt();
            int[] bounds = numberBounds(field, current);
            Spinner<Integer> spinner = new Spinner<>();
            spinner.setEditable(true);
            spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    bounds[0], bounds[1], current));
            spinner.setPrefWidth(104.0);
            spinner.getStyleClass().add("simulation-input");
            control = spinner;
        } else if (isOptionField(field)) {
            ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(optionsFor(field)));
            comboBox.setEditable((field.equals("start") || field.equals("source") || field.equals("sink"))
                    && !simulationType.equalsIgnoreCase("BELLMAN_FORD"));
            if (comboBox.isEditable()) {
                comboBox.getItems().setAll(vertexChoices());
            }
            selectIgnoringCase(comboBox, value.asText());
            comboBox.setPrefWidth(field.equals("algorithm") ? 154.0 : 130.0);
            comboBox.getStyleClass().add("simulation-input");
            control = comboBox;
        } else {
            TextField textField = new TextField(value.isArray() ? arrayText(field, value) : value.asText());
            textField.setPrefWidth(inputWidth(field));
            textField.getStyleClass().add("simulation-input");
            String hint = inputHint(field);
            if (!hint.isBlank()) {
                textField.setTooltip(new Tooltip(hint));
            }
            control = textField;
        }
        control.setId("input-" + field.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT));
        return control;
    }

    private ObjectNode readInputControls() {
        ObjectNode result = inputConfig.deepCopy();
        for (Map.Entry<String, Control> entry : inputControls.entrySet()) {
            String field = entry.getKey();
            Control control = entry.getValue();
            JsonNode template = inputConfig.get(field);
            if (control instanceof CheckBox checkBox) {
                result.put(field, checkBox.isSelected());
            } else if (control instanceof Spinner<?> spinner) {
                String text = spinner.getEditor().getText().trim();
                result.put(field, parseInteger(text, field));
            } else if (control instanceof ComboBox<?> comboBox
                    && comboBox.getValue() instanceof InputChoice choice) {
                result.set(field, choice.value().deepCopy());
                if (choice.vertices() != null) {
                    result.set("vertices", choice.vertices().deepCopy());
                }
            } else if (control instanceof ComboBox<?> comboBox) {
                String text = comboBox.isEditable()
                        ? comboBox.getEditor().getText().trim()
                        : String.valueOf(comboBox.getValue()).trim();
                if (text.isBlank() || text.equals("null")) {
                    throw new IllegalArgumentException(inputLabel(field, template) + " is required.");
                }
                result.put(field, text);
            } else if (control instanceof TextField textField) {
                String text = textField.getText().trim();
                if (template.isArray()) {
                    result.set(field, parseArray(field, text, template));
                } else {
                    if (text.isBlank()) {
                        throw new IllegalArgumentException(inputLabel(field, template) + " is required.");
                    }
                    result.put(field, text);
                }
            }
        }
        validateConfig(result);
        return result;
    }

    private void validateConfig(ObjectNode config) {
        if (config.has("firstTrue") && config.has("limit")
                && config.path("firstTrue").asInt() > config.path("limit").asInt()) {
            throw new IllegalArgumentException("First true must be no greater than the limit.");
        }
        if (config.has("vertices")) {
            requireUniqueVertices(config.path("vertices"));
        }
        Set<String> graphVertices = configuredGraphVertices(config);
        for (String field : List.of("start", "source", "sink")) {
            if (config.has(field) && !graphVertices.isEmpty()
                    && !graphVertices.contains(config.path(field).asText())) {
                throw new IllegalArgumentException(inputLabel(field, config.get(field))
                        + " must name a vertex in the selected graph.");
            }
        }
        if (config.has("source") && config.path("source").asText().equals(config.path("sink").asText())) {
            throw new IllegalArgumentException("Source and sink must be different vertices.");
        }
        if (simulationType.equalsIgnoreCase("FLOYD_WARSHALL")) {
            JsonNode matrix = config.path("matrix");
            if (matrix.size() != matrix.path(0).size()) {
                throw new IllegalArgumentException("Floyd-Warshall requires a square matrix.");
            }
            if (config.path("vertices").size() != matrix.size()) {
                throw new IllegalArgumentException("Vertex labels must match the matrix size.");
            }
            matrix.forEach(row -> row.forEach(value -> {
                int distance = value.asInt();
                if (distance < -998 || distance > 999) {
                    throw new IllegalArgumentException(
                            "Floyd-Warshall distances must be from -998 to 998, or 999 for no edge.");
                }
            }));
        }
        if (simulationType.equalsIgnoreCase("BELLMAN_FORD")) {
            config.path("weightedEdges").forEach(edge -> {
                int weight = edge.path(2).asInt();
                if (weight < -999 || weight > 999) {
                    throw new IllegalArgumentException("Bellman-Ford edge weights must be from -999 to 999.");
                }
            });
        }
        if (simulationType.equalsIgnoreCase("LINEAR_ALGEBRA")) {
            JsonNode matrix = config.path("matrix");
            if (matrix.path(0).size() != config.path("vector").size()) {
                throw new IllegalArgumentException("Vector length must match the number of matrix columns.");
            }
        }
        if (simulationType.equalsIgnoreCase("GEOMETRY")) {
            for (String point : List.of("a", "b", "c")) {
                if (config.path(point).size() != 2) {
                    throw new IllegalArgumentException("Each geometry point needs exactly two coordinates.");
                }
            }
        }
        if (simulationType.equalsIgnoreCase("GREEDY")) {
            config.path("intervals").forEach(interval -> {
                if (interval.path(0).asInt() >= interval.path(1).asInt()) {
                    throw new IllegalArgumentException("Every interval must start before it finishes.");
                }
            });
        }
    }

    private void requireUniqueVertices(JsonNode configured) {
        LinkedHashSet<String> vertices = new LinkedHashSet<>();
        configured.forEach(vertex -> vertices.add(vertex.asText()));
        if (vertices.size() != configured.size()) {
            throw new IllegalArgumentException("Vertex labels must be unique.");
        }
        if (vertices.size() > 8) {
            throw new IllegalArgumentException("Use at most 8 vertices in this visualizer.");
        }
    }

    private Set<String> configuredGraphVertices(JsonNode config) {
        LinkedHashSet<String> vertices = new LinkedHashSet<>();
        config.path("vertices").forEach(vertex -> vertices.add(vertex.asText()));
        for (String field : List.of("edges", "weightedEdges", "capacityEdges")) {
            config.path(field).forEach(edge -> {
                if (edge.isArray() && edge.size() >= 2) {
                    vertices.add(edge.path(0).asText());
                    vertices.add(edge.path(1).asText());
                }
            });
        }
        if (vertices.size() > 8) {
            throw new IllegalArgumentException("Use at most 8 vertices in this visualizer.");
        }
        return vertices;
    }

    private ArrayNode parseArray(String field, String text, JsonNode template) {
        if (text.isBlank()) {
            throw new IllegalArgumentException(inputLabel(field, template) + " cannot be empty.");
        }
        if (field.equals("matrix")) {
            ArrayNode matrix = objectMapper.createArrayNode();
            int width = -1;
            for (String rowText : text.split(";")) {
                ArrayNode row = parseIntegerList(rowText, field);
                if (width < 0) {
                    width = row.size();
                } else if (row.size() != width) {
                    throw new IllegalArgumentException("Every matrix row must have the same length.");
                }
                matrix.add(row);
            }
            if (matrix.size() > 8 || width > 8) {
                throw new IllegalArgumentException("Use a matrix no larger than 8 by 8.");
            }
            return matrix;
        }
        if (field.equals("edges") || field.equals("intervals")
                || field.equals("weightedEdges") || field.equals("capacityEdges")) {
            ArrayNode pairs = objectMapper.createArrayNode();
            for (String pairText : text.split(",")) {
                ArrayNode pair = objectMapper.createArrayNode();
                if (field.equals("intervals")) {
                    String[] parts = pairText.trim().split("\\s*:\\s*", -1);
                    if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                        throw new IllegalArgumentException(inputHint(field));
                    }
                    pair.add(parseInteger(parts[0], field));
                    pair.add(parseInteger(parts[1], field));
                } else if (field.equals("edges")) {
                    String[] parts = pairText.trim().split("\\s*-\\s*", -1);
                    if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                        throw new IllegalArgumentException(inputHint(field));
                    }
                    pair.add(parts[0]);
                    pair.add(parts[1]);
                } else {
                    String[] weighted = pairText.trim().split("\\s*:\\s*", -1);
                    if (weighted.length != 2) {
                        throw new IllegalArgumentException(inputHint(field));
                    }
                    String[] endpoints = weighted[0].split("\\s*-\\s*", -1);
                    if (endpoints.length != 2 || endpoints[0].isBlank() || endpoints[1].isBlank()) {
                        throw new IllegalArgumentException(inputHint(field));
                    }
                    int weight = parseInteger(weighted[1], field);
                    if (field.equals("capacityEdges") && weight <= 0) {
                        throw new IllegalArgumentException("Capacities must be positive whole numbers.");
                    }
                    pair.add(endpoints[0]);
                    pair.add(endpoints[1]);
                    pair.add(weight);
                }
                pairs.add(pair);
                if (pairs.size() > 20) {
                    throw new IllegalArgumentException(inputLabel(field, template)
                            + " supports at most 20 entries.");
                }
            }
            return pairs;
        }
        boolean textual = field.equals("keys") || field.equals("words") || field.equals("vertices")
                || template.size() > 0 && template.get(0).isTextual();
        if (textual) {
            ArrayNode values = objectMapper.createArrayNode();
            for (String item : text.split(",")) {
                String normalized = item.trim();
                if (normalized.isBlank()) {
                    throw new IllegalArgumentException(inputLabel(field, template) + " contains an empty item.");
                }
                values.add(normalized);
            }
            if (values.size() > 24) {
                throw new IllegalArgumentException(inputLabel(field, template)
                        + " supports at most 24 values.");
            }
            return values;
        }
        return parseIntegerList(text, field);
    }

    private ArrayNode parseIntegerList(String text, String field) {
        ArrayNode values = objectMapper.createArrayNode();
        for (String item : text.split(",")) {
            values.add(parseInteger(item.trim(), field));
        }
        if (values.isEmpty() || values.size() > 24) {
            throw new IllegalArgumentException(inputLabel(field, inputConfig.get(field))
                    + " must contain between 1 and 24 integers.");
        }
        return values;
    }

    private int parseInteger(String text, String field) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(inputLabel(field, inputConfig.get(field))
                    + " must contain whole numbers.");
        }
    }

    private String arrayText(String field, JsonNode value) {
        List<String> parts = new ArrayList<>();
        if (field.equals("matrix")) {
            value.forEach(row -> parts.add(joinArray(row, ", ")));
            return String.join("; ", parts);
        }
        if (field.equals("edges") || field.equals("intervals")
                || field.equals("weightedEdges") || field.equals("capacityEdges")) {
            if (field.equals("intervals")) {
                value.forEach(pair -> parts.add(pair.path(0).asText() + ":" + pair.path(1).asText()));
            } else if (field.equals("edges")) {
                value.forEach(pair -> parts.add(pair.path(0).asText() + "-" + pair.path(1).asText()));
            } else {
                value.forEach(pair -> parts.add(pair.path(0).asText() + "-" + pair.path(1).asText()
                        + ":" + pair.path(2).asText()));
            }
            return String.join(", ", parts);
        }
        return joinArray(value, ", ");
    }

    private String joinArray(JsonNode value, String separator) {
        List<String> parts = new ArrayList<>();
        value.forEach(item -> parts.add(item.asText()));
        return String.join(separator, parts);
    }

    private boolean isOptionField(String field) {
        return Set.of("algorithm", "mode", "operation", "kind", "start", "source", "sink").contains(field);
    }

    private boolean usesPresetDropdown(String field) {
        return simulationType.equalsIgnoreCase("BELLMAN_FORD") && field.equals("weightedEdges")
                || simulationType.equalsIgnoreCase("FLOYD_WARSHALL") && field.equals("matrix");
    }

    private List<InputChoice> presetChoices(String field, JsonNode current) {
        List<InputChoice> choices = new ArrayList<>();
        JsonNode currentVertices = simulationType.equalsIgnoreCase("FLOYD_WARSHALL")
                ? inputConfig.path("vertices").deepCopy() : null;
        choices.add(new InputChoice("Lesson example", current.deepCopy(), currentVertices));
        if (field.equals("weightedEdges")) {
            choices.add(choice("Alternate paths", "[[\"A\",\"B\",5],[\"A\",\"C\",1],[\"C\",\"B\",1]]", null));
            choices.add(choice("Reachable negative cycle",
                    "[[\"A\",\"B\",1],[\"B\",\"C\",-2],[\"C\",\"A\",0]]", null));
            choices.add(choice("Disconnected negative cycle",
                    "[[\"A\",\"B\",1],[\"C\",\"D\",-2],[\"D\",\"C\",1]]", null));
        } else {
            choices.add(choice("Three-vertex paths",
                    "[[0,8,2],[999,0,1],[999,3,0]]", "[\"A\",\"B\",\"C\"]"));
            choices.add(choice("Finite path above 999",
                    "[[0,600,999],[999,0,600],[999,999,0]]", "[\"A\",\"B\",\"C\"]"));
            choices.add(choice("Negative cycle",
                    "[[0,1,999],[999,0,-2],[-2,999,0]]", "[\"A\",\"B\",\"C\"]"));
        }
        return List.copyOf(choices);
    }

    private InputChoice choice(String label, String valueJson, String verticesJson) {
        try {
            JsonNode value = objectMapper.readTree(valueJson);
            JsonNode vertices = verticesJson == null ? null : objectMapper.readTree(verticesJson);
            return new InputChoice(label, value, vertices);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid built-in simulation input", exception);
        }
    }

    private List<String> optionsFor(String field) {
        return switch (field) {
            case "algorithm" -> List.of("Merge", "Quick", "Heap Sort", "Counting", "Radix", "Bucket Sort");
            case "mode" -> List.of("offline", "online");
            case "operation" -> simulationType.equalsIgnoreCase("FENWICK_TREE")
                    ? List.of("prefix-sum") : List.of("sum", "lazy-range-update");
            case "kind" -> List.of("min", "max");
            default -> vertexChoices();
        };
    }

    private List<String> vertexChoices() {
        LinkedHashSet<String> vertices = new LinkedHashSet<>();
        JsonNode configuredVertices = inputConfig.path("vertices");
        if (configuredVertices.isArray()) {
            configuredVertices.forEach(vertex -> vertices.add(vertex.asText()));
        }
        for (String field : List.of("edges", "weightedEdges", "capacityEdges")) {
            JsonNode edges = inputConfig.path(field);
            if (edges.isArray()) {
                edges.forEach(edge -> {
                    if (edge.isArray() && edge.size() >= 2) {
                        vertices.add(edge.get(0).asText());
                        vertices.add(edge.get(1).asText());
                    }
                });
            }
        }
        if (vertices.isEmpty()) {
            vertices.addAll(switch (simulationType.toUpperCase(Locale.ROOT)) {
                case "BELLMAN_FORD" -> List.of("A", "B", "C", "D", "E");
                case "MAX_FLOW" -> List.of("S", "A", "B", "T");
                default -> List.of("A", "B", "C", "D", "E", "F");
            });
        }
        return List.copyOf(vertices);
    }

    private void selectIgnoringCase(ComboBox<String> comboBox, String value) {
        comboBox.getItems().stream().filter(item -> item.equalsIgnoreCase(value)).findFirst()
                .ifPresentOrElse(comboBox.getSelectionModel()::select, () -> {
                    if (comboBox.isEditable()) {
                        comboBox.getEditor().setText(value);
                    } else {
                        comboBox.getItems().add(value);
                        comboBox.getSelectionModel().select(value);
                    }
                });
    }

    private int[] numberBounds(String field, int current) {
        return switch (field) {
            case "n" -> new int[]{2, simulationType.equalsIgnoreCase("RECURSION") ? 7
                    : simulationType.equalsIgnoreCase("COMBINATORICS") ? 8 : 12};
            case "size" -> new int[]{3, simulationType.equalsIgnoreCase("BACKTRACKING") ? 5 : 10};
            case "limit", "firstTrue" -> new int[]{1, 24};
            default -> new int[]{Math.min(-1000, current), Math.max(1000, current)};
        };
    }

    private double inputWidth(String field) {
        return switch (field) {
            case "matrix", "edges", "weightedEdges", "capacityEdges", "intervals" -> 300.0;
            case "text", "values", "words", "keys", "vertices", "vector" -> 235.0;
            default -> 150.0;
        };
    }

    private String inputHint(String field) {
        return switch (field) {
            case "edges" -> "Enter edges as A-B, A-C.";
            case "weightedEdges" -> simulationType.equalsIgnoreCase("BELLMAN_FORD")
                    ? "Choose a directed weighted graph; negative weights are supported."
                    : "Enter weighted edges as A-B:4, A-C:2.";
            case "capacityEdges" -> "Enter capacity edges as S-A:3, A-T:2.";
            case "intervals" -> "Enter intervals as 1:3, 2:4.";
            case "matrix" -> simulationType.equalsIgnoreCase("FLOYD_WARSHALL")
                    ? "In each preset, 999 represents no direct edge."
                    : "Separate values with commas and rows with semicolons.";
            case "values", "vector", "a", "b", "c" -> "Enter comma-separated whole numbers.";
            case "words", "keys", "vertices" -> "Enter comma-separated values.";
            default -> "";
        };
    }

    private String inputLabel(String field, JsonNode value) {
        if (field.equals("n")) {
            return "N";
        }
        if ((field.equals("a") || field.equals("b") || field.equals("c")) && value != null && value.isArray()) {
            return "Point " + field.toUpperCase(Locale.ROOT);
        }
        if (field.equals("matrix") && simulationType.equalsIgnoreCase("FLOYD_WARSHALL")) {
            return "Matrix preset";
        }
        if (field.equals("weightedEdges") && simulationType.equalsIgnoreCase("BELLMAN_FORD")) {
            return "Weighted graph preset";
        }
        String separated = field.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
        return Character.toUpperCase(separated.charAt(0)) + separated.substring(1);
    }

    private void showInputStatus(String message, boolean error) {
        inputStatusLabel.setText(message == null || message.isBlank() ? "Check the input values." : message);
        inputStatusLabel.getStyleClass().remove("simulation-input-error");
        if (error) {
            inputStatusLabel.getStyleClass().add("simulation-input-error");
        }
        inputStatusLabel.setManaged(true);
        inputStatusLabel.setVisible(true);
    }

    private void hideInputStatus() {
        inputStatusLabel.setManaged(false);
        inputStatusLabel.setVisible(false);
        inputStatusLabel.setText("");
        inputStatusLabel.getStyleClass().remove("simulation-input-error");
    }

    private void highlightPseudocode(int line) {
        for (int index = 0; index < pseudocodeBox.getChildren().size(); index++) {
            pseudocodeBox.getChildren().get(index).getStyleClass().remove("simulation-code-active");
            if (index == line) {
                pseudocodeBox.getChildren().get(index).getStyleClass().add("simulation-code-active");
            }
        }
    }

    private String readableType(String value) {
        if (value == null || value.isBlank()) {
            return "Simulation";
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        if (java.util.Set.of("BFS", "DFS", "DSU", "BST", "AVL", "SCC").contains(normalized)) {
            return normalized;
        }
        if (normalized.equals("BELLMAN_FORD")) {
            return "Bellman-Ford";
        }
        if (normalized.equals("FLOYD_WARSHALL")) {
            return "Floyd-Warshall";
        }
        String[] words = normalized.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private record InputChoice(String label, JsonNode value, JsonNode vertices) {
        @Override
        public String toString() {
            return label;
        }
    }
}
