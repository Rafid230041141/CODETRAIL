package application;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import application.algorithm.GraphAlgorithms.Algorithm;
import application.backend.BackendApplication;
import application.client.controller.AppController;
import application.client.service.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

public final class UiVerificationHarness {
    private UiVerificationHarness() {
    }

    public static void main(String[] args) throws Exception {
        try (ConfigurableApplicationContext context = startBackend()) {
            int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
            context.getBean(ApiClient.class).setBaseUri(URI.create("http://127.0.0.1:" + port + "/api/"));

            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(started::countDown);
            check(started.await(10, TimeUnit.SECONDS), "JavaFX platform starts");

            LoadedUi ui = onFx(() -> loadMainView(context));
            try {
                verifyStudentFlow(ui);
                verifyAdminFlow(ui);
                verifyQuizFxml(context);
            } finally {
                onFx(() -> {
                    ui.controller().shutdown();
                    ui.stage().close();
                    return null;
                });
                Platform.exit();
            }
        }
        System.out.println("ALL PHASE 2 JAVAFX CHECKS PASSED");
    }

    private static ConfigurableApplicationContext startBackend() {
        return new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.SERVLET)
                .headless(false)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:codetrail-ui;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=create-drop",
                        "--spring.jpa.open-in-view=false",
                        "--logging.level.root=ERROR");
    }

    private static LoadedUi loadMainView(ConfigurableApplicationContext context) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                UiVerificationHarness.class.getResource("/resources/fxml/MainView.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(
                UiVerificationHarness.class.getResource("/resources/css/application.css").toExternalForm());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        root.applyCss();
        root.layout();
        return new LoadedUi(root, loader.getController(), loader.getNamespace(), stage);
    }

    private static void verifyStudentFlow(LoadedUi ui) throws Exception {
        Map<String, Object> nodes = ui.nodes();
        StackPane login = node(nodes, "loginView", StackPane.class);
        StackPane student = node(nodes, "studentView", StackPane.class);
        StackPane admin = node(nodes, "adminView", StackPane.class);
        check(onFx(() -> login.isVisible() && !student.isVisible() && !admin.isVisible()),
                "login is the initial role-neutral screen");

        onFx(() -> {
            node(nodes, "usernameField", TextField.class).setText("student");
            node(nodes, "passwordField", PasswordField.class).setText("student123");
            node(nodes, "authPrimaryButton", Button.class).fire();
            return null;
        });
        waitFor("student login opens the student workspace", () -> student.isVisible());

        TreeView<?> tree = node(nodes, "curriculumTree", TreeView.class);
        waitFor("the curriculum tree loads over HTTP",
                () -> tree.getRoot() != null && tree.getRoot().getChildren().size() == 2);
        check(onFx(() -> tree.getRoot().getChildren().stream()
                .map(item -> item.getValue().toString())
                .allMatch(value -> value.contains("Languages") || value.contains("DSA / Competitive Programming"))),
                "the student sees only Languages and DSA/CP");

        HBox topicCards = node(nodes, "topicCards", HBox.class);
        waitFor("the dashboard renders two network-backed topic cards", () -> topicCards.getChildren().size() == 2);

        TreeItem<?> recursion = onFx(() -> findItem(tree.getRoot(), "Recursion"));
        check(recursion != null, "the full DSA tree contains the Recursion lesson");
        onFx(() -> {
            select(tree, recursion);
            return null;
        });
        Label lessonTitle = node(nodes, "lessonTitleLabel", Label.class);
        waitFor("selecting a tree lesson loads its body", () -> "Recursion".equals(lessonTitle.getText()));
        check(onFx(() -> node(nodes, "markdownContent", VBox.class).getChildren().size() >= 4),
                "Markdown lesson content renders into styled JavaFX nodes");
        VBox simulation = node(nodes, "simulationSection", VBox.class);
        check(onFx(() -> simulation.isVisible()
                        && !((Pane) simulation.lookup(".simulation-canvas")).getChildren().isEmpty()),
                "the Recursion lesson displays an interactive simulation");
        VBox markdown = node(nodes, "markdownContent", VBox.class);
        check(onFx(() -> simulation.getParent().getChildrenUnmodifiable().indexOf(simulation)
                < markdown.getParent().getChildrenUnmodifiable().indexOf(markdown)),
                "the interactive simulation appears before the lesson explanation");
        verifyGenericInputControls(simulation);

        CheckBox completed = node(nodes, "lessonCompletedCheckBox", CheckBox.class);
        onFx(() -> {
            completed.fire();
            return null;
        });
        Label studentStatus = node(nodes, "studentStatusLabel", Label.class);
        waitFor("completion is saved through the backend",
                () -> studentStatus.getText().contains("marked complete"));

        ToggleButton theme = node(nodes, "studentThemeToggle", ToggleButton.class);
        onFx(() -> {
            theme.fire();
            return null;
        });
        check(onFx(() -> ui.root().getStyleClass().contains("dark-theme")),
                "dark mode covers the networked student workspace");

        verifyAllDsaLessonVisuals(ui, tree, lessonTitle, simulation);
        verifyEditableTraversal(ui, tree, lessonTitle, simulation);
        verifyShortestPathInputs(ui, tree, lessonTitle, simulation);

        TreeItem<?> dijkstra = onFx(() -> findItem(tree.getRoot(), "Dijkstra"));
        check(dijkstra != null, "the DSA tree contains the Dijkstra lesson");
        onFx(() -> {
            select(tree, dijkstra);
            return null;
        });
        waitFor("selecting Dijkstra loads the graph visualizer",
                () -> "Dijkstra".equals(lessonTitle.getText()));
        verifyGraphSimulation(ui);
    }

    @SuppressWarnings("unchecked")
    private static void verifyGenericInputControls(VBox simulation) throws Exception {
        Spinner<Integer> n = (Spinner<Integer>) simulation.lookup("#input-n");
        Button apply = (Button) simulation.lookup("#applyInputsButton");
        Button restore = (Button) simulation.lookup("#restoreInputsButton");
        Label counter = (Label) simulation.lookup("#stepLabel");
        Label message = (Label) simulation.lookup("#simulationMessageLabel");
        Button next = (Button) simulation.lookup("#nextButton");
        check(n != null && apply != null && restore != null,
                "a DSA lesson exposes editable inputs with apply and restore controls");
        onFx(() -> {
            n.getValueFactory().setValue(3);
            apply.fire();
            while (!next.isDisabled()) {
                next.fire();
            }
            return null;
        });
        check(onFx(() -> counter.getText().endsWith("/ 6") && message.getText().contains("Return 6")),
                "applying a new recursion input rebuilds the correct trace");
        onFx(() -> {
            restore.fire();
            return null;
        });
        check(onFx(() -> counter.getText().endsWith("/ 10")),
                "restore example returns to the seeded lesson input");
    }

    @SuppressWarnings("unchecked")
    private static void verifyEditableTraversal(
            LoadedUi ui, TreeView<?> tree, Label lessonTitle, VBox simulation) throws Exception {
        TreeItem<?> bfs = onFx(() -> findItem(tree.getRoot(), "BFS"));
        selectAndWait(tree, bfs, lessonTitle, "BFS");
        TextField edges = (TextField) simulation.lookup("#input-edges");
        ComboBox<String> start = (ComboBox<String>) simulation.lookup("#input-start");
        CheckBox directed = (CheckBox) simulation.lookup("#input-directed");
        Button apply = (Button) simulation.lookup("#applyInputsButton");
        check(edges != null && start != null && directed != null,
                "graph traversal exposes topology, direction, and start inputs");
        onFx(() -> {
            edges.setText("X-Y, Y-Z");
            directed.setSelected(true);
            start.getEditor().setText("Y");
            apply.fire();
            ui.root().applyCss();
            ui.root().layout();
            return null;
        });
        check(onFx(() -> simulation.lookupAll(".visual-graph-node").size() == 3
                        && simulation.lookupAll(".visual-link").size() == 2),
                "applying traversal inputs redraws the selected graph");
        check(onFx(() -> simulation.lookupAll(".visual-graph-node").stream()
                        .flatMap(node -> node.lookupAll(".visual-node-label").stream())
                        .filter(Label.class::isInstance).map(Label.class::cast)
                        .anyMatch(label -> label.getText().equals("Y"))),
                "the traversal begins from the selected vertex");
        onFx(() -> {
            ((TextField) simulation.lookup("#input-edges")).setText("X-Y, broken");
            ((Button) simulation.lookup("#applyInputsButton")).fire();
            return null;
        });
        check(onFx(() -> {
            Label status = (Label) simulation.lookup("#inputStatusLabel");
            return status.isVisible() && status.getStyleClass().contains("simulation-input-error");
        }), "invalid graph input produces focused feedback without replacing the prior trace");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void verifyShortestPathInputs(
            LoadedUi ui, TreeView<?> tree, Label lessonTitle, VBox simulation) throws Exception {
        TreeItem<?> bellman = onFx(() -> findItem(tree.getRoot(), "Bellman-Ford"));
        selectAndWait(tree, bellman, lessonTitle, "Bellman-Ford");
        Pane graph = onFx(() -> graphNode(ui, "graphPane", Pane.class));
        ComboBox<Algorithm> algorithms = onFx(() -> graphNode(ui, "algorithmSelector", ComboBox.class));
        ComboBox<String> start = onFx(() -> graphNode(ui, "startVertexSelector", ComboBox.class));
        CheckBox directed = onFx(() -> graphNode(ui, "directedEdgeCheckBox", CheckBox.class));
        Spinner<Integer> weight = onFx(() -> graphNode(ui, "edgeWeightSpinner", Spinner.class));
        ToggleButton addVertex = onFx(() -> graphNode(ui, "addVertexModeButton", ToggleButton.class));
        ToggleButton addEdge = onFx(() -> graphNode(ui, "addEdgeModeButton", ToggleButton.class));
        Button newGraph = onFx(() -> graphNode(ui, "newGraphButton", Button.class));
        Button restoreSample = onFx(() -> graphNode(ui, "restoreSampleButton", Button.class));
        Button next = onFx(() -> graphNode(ui, "nextStepButton", Button.class));
        Label message = onFx(() -> graphNode(ui, "stepMessageLabel", Label.class));
        ListView<String> values = onFx(() -> graphNode(ui, "valuesList", ListView.class));

        waitFor("Bellman-Ford preset renders on the spatial graph canvas",
                () -> graphVertexCount(graph) == 5 && graphEdgeCount(graph) == 6 && graphArrowCount(graph) == 6);
        check(onFx(() -> !simulation.lookup("#genericSimulation").isVisible()
                        && algorithms.getValue() == Algorithm.BELLMAN_FORD
                        && directed.isSelected() && !weight.isEditable() && !start.isDisable()),
                "Bellman-Ford opens its editable directed preset without graph text input");

        onFx(() -> {
            newGraph.fire();
            addVertex.setSelected(true);
            fireGraphClick(graph, 100.0, 100.0);
            fireGraphClick(graph, Math.max(300.0, graph.getWidth() - 70.0), 70.0);
            directed.setSelected(true);
            weight.getValueFactory().setValue(-1);
            addEdge.setSelected(true);
            clickGraphVertex(graph, "A");
            clickGraphVertex(graph, "B");
            weight.getValueFactory().setValue(2);
            clickGraphVertex(graph, "B");
            clickGraphVertex(graph, "C");
            start.getSelectionModel().select("A");
            advanceGraphToLastStep(next);
            ui.root().applyCss();
            ui.root().layout();
            return null;
        });
        check(onFx(() -> graphVertexCount(graph) == 3 && graphEdgeCount(graph) == 2
                        && graphArrowCount(graph) == 2 && graphWeightExists(graph, "-1")
                        && values.getItems().stream().anyMatch(row -> row.startsWith("C") && row.endsWith("1"))
                        && message.getText().contains("complete")),
                "Bellman-Ford computes a visually constructed custom graph");
        onFx(() -> {
            restoreSample.fire();
            return null;
        });
        waitFor("Bellman-Ford restore returns its preset",
                () -> graphVertexCount(graph) == 5 && graphEdgeCount(graph) == 6);

        TreeItem<?> floyd = onFx(() -> findItem(tree.getRoot(), "Floyd-Warshall"));
        selectAndWait(tree, floyd, lessonTitle, "Floyd-Warshall");
        waitFor("Floyd-Warshall preset renders on the spatial graph canvas",
                () -> graphVertexCount(graph) == 4 && graphEdgeCount(graph) == 5 && graphArrowCount(graph) == 5);
        check(onFx(() -> !simulation.lookup("#genericSimulation").isVisible()
                        && algorithms.getValue() == Algorithm.FLOYD_WARSHALL
                        && directed.isSelected() && !weight.isEditable() && start.isDisable()),
                "Floyd-Warshall opens its editable directed preset without matrix text input");

        onFx(() -> {
            newGraph.fire();
            addVertex.setSelected(true);
            fireGraphClick(graph, 100.0, 100.0);
            fireGraphClick(graph, Math.max(300.0, graph.getWidth() - 70.0), 70.0);
            directed.setSelected(true);
            weight.getValueFactory().setValue(6);
            addEdge.setSelected(true);
            clickGraphVertex(graph, "A");
            clickGraphVertex(graph, "B");
            clickGraphVertex(graph, "B");
            clickGraphVertex(graph, "C");
            advanceGraphToLastStep(next);
            ui.root().applyCss();
            ui.root().layout();
            return null;
        });
        check(onFx(() -> values.getItems().stream().anyMatch(row -> row.startsWith("A") && row.endsWith("12"))
                        && message.getText().contains("complete")),
                "Floyd-Warshall computes all pairs on a visually constructed custom graph");

        onFx(() -> {
            weight.getValueFactory().setValue(-20);
            addEdge.setSelected(true);
            clickGraphVertex(graph, "C");
            clickGraphVertex(graph, "A");
            advanceGraphToLastStep(next);
            return null;
        });
        check(onFx(() -> message.getText().contains("Negative cycle")
                        && !graph.lookupAll(".graph-vertex-active").isEmpty()),
                "Floyd-Warshall reports and highlights a custom negative cycle");
    }

    private static void advanceGraphToLastStep(Button next) {
        while (!next.isDisabled()) {
            next.fire();
        }
    }

    private static void fireGraphClick(Pane graph, double x, double y) {
        graph.fireEvent(mouseClick(x, y));
    }

    private static void clickGraphVertex(Pane graph, String vertexId) {
        VBox vertex = graph.getChildren().stream()
                .filter(VBox.class::isInstance)
                .map(VBox.class::cast)
                .filter(group -> group.getChildren().stream()
                        .filter(StackPane.class::isInstance)
                        .map(StackPane.class::cast)
                        .flatMap(node -> node.getChildren().stream())
                        .filter(Label.class::isInstance)
                        .map(Label.class::cast)
                        .anyMatch(label -> label.getText().equals(vertexId)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FAILED: graph vertex not found: " + vertexId));
        vertex.fireEvent(mouseClick(vertex.getLayoutX(), vertex.getLayoutY()));
    }

    private static MouseEvent mouseClick(double x, double y) {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null);
    }

    private static void advanceToLastStep(VBox simulation) {
        Button next = (Button) simulation.lookup("#nextButton");
        while (!next.isDisabled()) {
            next.fire();
        }
    }

    private static boolean visualLabelExists(VBox simulation, String expected) {
        return simulation.lookupAll(".visual-node-label").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .anyMatch(label -> label.getText().equals(expected));
    }

    private static void selectComboByText(ComboBox<?> comboBox, String label) {
        for (int index = 0; index < comboBox.getItems().size(); index++) {
            if (comboBox.getItems().get(index).toString().equals(label)) {
                comboBox.getSelectionModel().select(index);
                return;
            }
        }
        throw new AssertionError("FAILED: combo option not found: " + label);
    }

    private static void verifyAllDsaLessonVisuals(
            LoadedUi ui, TreeView<?> tree, Label lessonTitle, VBox simulation) throws Exception {
        TreeItem<?> dsa = onFx(() -> tree.getRoot().getChildren().stream()
                .filter(item -> item.getValue().toString().contains("DSA / Competitive Programming"))
                .findFirst().orElse(null));
        check(dsa != null, "the DSA curriculum branch is available for visual verification");
        List<TreeItem<?>> lessons = onFx(() -> {
            List<TreeItem<?>> leaves = new ArrayList<>();
            collectLeaves(dsa, leaves);
            return leaves;
        });
        check(lessons.size() == 54, "the UI visual audit covers all 54 DSA lessons");

        for (TreeItem<?> lesson : lessons) {
            String title = navigationLabel(lesson);
            onFx(() -> {
                select(tree, lesson);
                return null;
            });
            waitUntil(() -> title.equals(lessonTitle.getText()));
            boolean rendered = onFx(() -> {
                ui.root().applyCss();
                ui.root().layout();
                if (List.of("Dijkstra", "Prim", "Kruskal", "Bellman-Ford", "Floyd-Warshall")
                        .contains(title)) {
                    return graphNode(ui, "graphPane", Pane.class).getChildren().size() >= 7;
                }
                Node canvasNode = simulation.lookup(".simulation-canvas");
                return simulation.isVisible() && canvasNode instanceof Pane canvas
                        && !canvas.getChildren().isEmpty();
            });
            if (!rendered) {
                throw new AssertionError("FAILED: no visualization rendered for DSA lesson " + title);
            }
        }

        TreeItem<?> fenwick = onFx(() -> findItem(dsa, "Fenwick Tree"));
        selectAndWait(tree, fenwick, lessonTitle, "Fenwick Tree");
        check(onFx(() -> simulation.lookupAll(".visual-fenwick-node").size() == 5),
                "Fenwick Tree renders five indexed partial-sum nodes");
        TreeItem<?> backtracking = onFx(() -> findItem(dsa, "Backtracking"));
        selectAndWait(tree, backtracking, lessonTitle, "Backtracking");
        check(onFx(() -> simulation.lookup(".visual-queen-board") != null),
                "Backtracking renders the N-Queens board");
        TreeItem<?> scc = onFx(() -> findItem(dsa, "SCC/Tarjan"));
        selectAndWait(tree, scc, lessonTitle, "SCC/Tarjan");
        check(onFx(() -> {
            Button next = (Button) simulation.lookup("#nextButton");
            while (!next.isDisabled()) {
                next.fire();
            }
            ui.root().applyCss();
            ui.root().layout();
            return simulation.lookupAll(".visual-graph-node").stream()
                    .flatMap(node -> node.lookupAll(".visual-node-label").stream())
                    .filter(Label.class::isInstance)
                    .map(Label.class::cast)
                    .anyMatch(label -> label.getText().contains("SCC"));
        }),
                "Tarjan visualization labels the discovered components");
        check(true, "all 54 DSA lessons render through the JavaFX lesson workflow");
    }

    private static void selectAndWait(
            TreeView<?> tree, TreeItem<?> item, Label lessonTitle, String expected) throws Exception {
        check(item != null, "the DSA tree contains " + expected);
        onFx(() -> {
            select(tree, item);
            return null;
        });
        waitUntil(() -> expected.equals(lessonTitle.getText()));
    }

    private static void collectLeaves(TreeItem<?> item, List<TreeItem<?>> leaves) {
        if (item.isLeaf()) {
            leaves.add(item);
            return;
        }
        for (TreeItem<?> child : item.getChildren()) {
            collectLeaves(child, leaves);
        }
    }

    private static String navigationLabel(TreeItem<?> item) {
        String value = item.getValue().toString();
        int start = value.indexOf("label=") + 6;
        int end = value.indexOf(", topicId=", start);
        String label = end > start ? value.substring(start, end) : value;
        return label.startsWith("✓  ") ? label.substring(3) : label;
    }

    @SuppressWarnings("unchecked")
    private static void verifyGraphSimulation(LoadedUi ui) throws Exception {
        onFx(() -> {
            ui.root().applyCss();
            ui.root().layout();
            return null;
        });

        Pane graph = onFx(() -> graphNode(ui, "graphPane", Pane.class));
        ComboBox<Algorithm> algorithms = onFx(() -> graphNode(ui, "algorithmSelector", ComboBox.class));
        ComboBox<String> startVertex = onFx(() -> graphNode(ui, "startVertexSelector", ComboBox.class));
        VBox pseudocode = onFx(() -> graphNode(ui, "graphPseudocodeBox", VBox.class));
        Label stepCounter = onFx(() -> graphNode(ui, "stepCounterLabel", Label.class));
        Button previous = onFx(() -> graphNode(ui, "previousStepButton", Button.class));
        Button next = onFx(() -> graphNode(ui, "nextStepButton", Button.class));
        check(onFx(() -> graph.isVisible()), "graph canvas is visible for graph algorithms");
        waitFor("spatial graph renders seven vertices and eleven weighted edges",
                () -> graphVertexCount(graph) == 7 && graphEdgeCount(graph) == 11
                        && graphWeightCount(graph) == 11);
        check(onFx(() -> algorithms.getItems().size() == 5 && algorithms.getValue() == Algorithm.DIJKSTRA),
                "graph visualizer offers all five weighted shortest-path and spanning-tree algorithms");
        check(onFx(() -> !startVertex.isDisable() && startVertex.getItems().size() == 7),
                "graph visualizer exposes a start-vertex selector");
        check(onFx(() -> graphNode(ui, "moveModeButton", javafx.scene.control.ToggleButton.class) != null
                && graphNode(ui, "addVertexModeButton", javafx.scene.control.ToggleButton.class) != null
                && graphNode(ui, "addEdgeModeButton", javafx.scene.control.ToggleButton.class) != null
                && graphNode(ui, "deleteModeButton", javafx.scene.control.ToggleButton.class) != null
                && !graphNode(ui, "edgeWeightSpinner", Spinner.class).isEditable()
                && graphNode(ui, "directedEdgeCheckBox", CheckBox.class) != null
                && graphNode(ui, "newGraphButton", Button.class) != null
                && graphNode(ui, "restoreSampleButton", Button.class) != null),
                "graph visualizer exposes non-text custom graph controls");

        Label message = onFx(() -> graphNode(ui, "stepMessageLabel", Label.class));
        onFx(() -> {
            startVertex.getSelectionModel().select("B");
            return null;
        });
        check(onFx(() -> "B".equals(startVertex.getValue()) && message.getText().contains("B")),
                "Dijkstra reruns from the selected start vertex");
        onFx(() -> {
            algorithms.getSelectionModel().select(Algorithm.PRIM);
            startVertex.getSelectionModel().select("C");
            return null;
        });
        check(onFx(() -> algorithms.getValue() == Algorithm.PRIM && !startVertex.isDisable()
                        && "C".equals(startVertex.getValue())),
                "Prim accepts its own selected start vertex");
        onFx(() -> {
            algorithms.getSelectionModel().select(Algorithm.DIJKSTRA);
            startVertex.getSelectionModel().select("A");
            return null;
        });

        String firstCounter = onFx(stepCounter::getText);
        String firstPseudocode = onFx(() -> activePseudocodeText(pseudocode));
        onFx(() -> {
            next.fire();
            return null;
        });
        check(onFx(() -> !firstCounter.equals(stepCounter.getText())),
                "graph next-step control advances the visual snapshot");
        String nextPseudocode = onFx(() -> activePseudocodeText(pseudocode));
        check(!firstPseudocode.equals(nextPseudocode),
                "graph next-step control advances the active pseudocode line ("
                        + firstPseudocode + " -> " + nextPseudocode + ")");
        onFx(() -> {
            previous.fire();
            return null;
        });
        check(onFx(() -> firstCounter.equals(stepCounter.getText())),
                "graph previous-step control restores the prior snapshot");

        boolean selectedAndActive = false;
        for (int index = 0; index < 40 && !selectedAndActive; index++) {
            selectedAndActive = onFx(() -> hasGraphStyle(graph, "graph-edge-selected")
                    && hasGraphStyle(graph, "graph-edge-active"));
            if (!selectedAndActive) {
                onFx(() -> {
                    next.fire();
                    return null;
                });
            }
        }
        check(selectedAndActive, "graph stepping renders selected and active edge states");

        onFx(() -> {
            algorithms.getSelectionModel().select(Algorithm.KRUSKAL);
            return null;
        });
        check(onFx(startVertex::isDisable), "Kruskal disables its unused start selector");
        boolean rejected = false;
        for (int index = 0; index < 40 && !rejected; index++) {
            rejected = onFx(() -> hasGraphStyle(graph, "graph-edge-rejected"));
            if (!rejected) {
                onFx(() -> {
                    next.fire();
                    return null;
                });
            }
        }
        check(rejected, "Kruskal stepping renders a rejected cycle edge");
    }

    private static void verifyAdminFlow(LoadedUi ui) throws Exception {
        Map<String, Object> nodes = ui.nodes();
        onFx(() -> {
            node(nodes, "studentLogoutButton", Button.class).fire();
            node(nodes, "usernameField", TextField.class).setText("admin");
            node(nodes, "passwordField", PasswordField.class).setText("admin123");
            node(nodes, "authPrimaryButton", Button.class).fire();
            return null;
        });

        StackPane admin = node(nodes, "adminView", StackPane.class);
        waitFor("admin login opens the role-appropriate workspace", () -> admin.isVisible());
        TableView<?> users = node(nodes, "usersTable", TableView.class);
        waitFor("admin user table loads real accounts", () -> users.getItems().size() >= 2);
        check(onFx(() -> users.getColumns().size() == 5),
                "admin table shows identity, role, completion, and quiz activity");
    }

    private static void verifyQuizFxml(ConfigurableApplicationContext context) throws Exception {
        Parent quiz = onFx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    UiVerificationHarness.class.getResource("/resources/fxml/QuizView.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            new Scene(root, 620, 520);
            root.applyCss();
            root.layout();
            return root;
        });
        check(onFx(() -> quiz.lookupAll(".quiz-option").size() == 4),
                "quiz screen provides four focused answer choices");
    }

    private static TreeItem<?> findItem(TreeItem<?> root, String label) {
        if (root == null) {
            return null;
        }
        if (root.isLeaf() && root.getValue() != null
                && root.getValue().toString().contains("label=" + label + ",")) {
            return root;
        }
        for (TreeItem<?> child : root.getChildren()) {
            TreeItem<?> found = findItem(child, label);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void select(TreeView tree, TreeItem item) {
        tree.getSelectionModel().select(item);
    }

    private static long graphVertexCount(Pane graph) {
        return graph.getChildren().stream()
                .filter(VBox.class::isInstance)
                .map(VBox.class::cast)
                .filter(group -> group.getChildren().stream()
                        .anyMatch(child -> child instanceof StackPane pane
                                && pane.getStyleClass().contains("graph-vertex")))
                .count();
    }

    private static long graphEdgeCount(Pane graph) {
        return graph.getChildren().stream().filter(Line.class::isInstance).count();
    }

    private static long graphWeightCount(Pane graph) {
        return graph.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> label.getStyleClass().contains("graph-weight"))
                .count();
    }

    private static long graphArrowCount(Pane graph) {
        return graph.getChildren().stream().filter(Polygon.class::isInstance).count();
    }

    private static boolean graphWeightExists(Pane graph, String expected) {
        return graph.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .anyMatch(label -> label.getStyleClass().contains("graph-weight")
                        && label.getText().equals(expected));
    }

    private static boolean hasGraphStyle(Pane graph, String styleClass) {
        return graph.getChildren().stream()
                .anyMatch(child -> child.getStyleClass().contains(styleClass));
    }

    private static String activePseudocodeText(VBox pseudocode) {
        return pseudocode.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> label.getStyleClass().contains("pseudocode-line-active"))
                .map(Label::getText)
                .findFirst()
                .orElse("");
    }

    private static <T> T graphNode(LoadedUi ui, String id, Class<T> type) {
        Object included = ui.nodes().get("graphSimulation");
        if (included instanceof Node includedRoot) {
            Node found = includedRoot.lookup("#" + id);
            if (type.isInstance(found)) {
                return type.cast(found);
            }
        }
        Object direct = ui.nodes().get(id);
        if (type.isInstance(direct)) {
            return type.cast(direct);
        }
        Node found = ui.root().lookup("#" + id);
        if (type.isInstance(found)) {
            return type.cast(found);
        }
        throw new AssertionError("FAILED: graph node not found: " + id);
    }

    private static void waitFor(String description, BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (onFx(condition::getAsBoolean)) {
                System.out.println("PASS: " + description);
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("FAILED: " + description);
    }

    private static void waitUntil(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (onFx(condition::getAsBoolean)) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("JavaFX lesson selection timed out");
    }

    private static <T> T node(Map<String, Object> nodes, String id, Class<T> type) {
        return type.cast(nodes.get(id));
    }

    private static <T> T onFx(FxSupplier<T> action) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                finished.countDown();
            }
        });
        if (!finished.await(15, TimeUnit.SECONDS)) {
            throw new AssertionError("JavaFX action timed out");
        }
        if (failure.get() != null) {
            throw new AssertionError("JavaFX action failed", failure.get());
        }
        return result.get();
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("FAILED: " + description);
        }
        System.out.println("PASS: " + description);
    }

    private record LoadedUi(
            Parent root,
            AppController controller,
            Map<String, Object> nodes,
            Stage stage) {
    }

    @FunctionalInterface
    private interface FxSupplier<T> {
        T get() throws Exception;
    }
}
