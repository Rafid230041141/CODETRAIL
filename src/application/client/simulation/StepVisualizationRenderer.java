package application.client.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import application.client.simulation.SimulationEngine.Cell;
import application.client.simulation.SimulationEngine.GraphLink;
import application.client.simulation.SimulationEngine.MergeRange;
import application.client.simulation.SimulationEngine.MergeSortState;
import application.client.simulation.SimulationEngine.SimulationStep;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

/** Draws the shared, non-weighted simulation families from one step snapshot. */
public final class StepVisualizationRenderer {
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    private static final double NODE_SIZE = 52.0;

    private StepVisualizationRenderer() {
    }

    public static void render(Pane canvas, String simulationType, SimulationStep step) {
        canvas.getChildren().clear();
        String type = simulationType == null ? "" : simulationType.toUpperCase(Locale.ROOT);
        canvas.getStyleClass().removeIf(style -> style.startsWith("visual-layout-"));

        if (isWeightedGraph(type)) {
            return;
        }
        if (type.equals("SORTING")) {
            renderSorting(canvas, step);
        } else if (type.equals("RECURSION") || type.equals("STACK")) {
            renderStack(canvas, step);
        } else if (type.equals("LINKED_LIST")) {
            renderLinkedList(canvas, step);
        } else if (type.equals("QUEUE")) {
            renderChain(canvas, step, true);
        } else if (isGraph(type)) {
            renderGraph(canvas, step, type);
        } else if (type.equals("FENWICK_TREE")) {
            renderFenwick(canvas, step);
        } else if (type.equals("BACKTRACKING")) {
            renderQueensBoard(canvas, step);
        } else if (isTree(type)) {
            renderTree(canvas, step, type);
        } else if (type.equals("DSU")) {
            renderDsu(canvas, step);
        } else if (type.equals("GEOMETRY")) {
            renderGeometry(canvas, step);
        } else if (isString(type)) {
            renderString(canvas, step, type);
        } else if (isGrid(type)) {
            renderGrid(canvas, step, type);
        } else {
            renderArray(canvas, step, type);
        }
    }

    private static void renderSorting(Pane canvas, SimulationStep step) {
        if (step.mergeSortState() != null) {
            renderMergeSort(canvas, step.mergeSortState());
            return;
        }
        boolean bucketState = step.cells().stream()
                .anyMatch(cell -> cell.text().contains(":") || cell.text().contains("["));
        if (bucketState) {
            renderGrid(canvas, step, "SORTING");
            return;
        }
        setLayout(canvas, "bars", 280.0);
        int maximum = step.cells().stream().mapToInt(cell -> Math.abs(number(cell.text()))).max().orElse(1);
        HBox row = new HBox(12.0);
        row.setAlignment(Pos.BOTTOM_CENTER);
        for (int index = 0; index < step.cells().size(); index++) {
            Cell cell = step.cells().get(index);
            int value = number(cell.text());
            StackPane bar = new StackPane();
            bar.setMinWidth(46.0);
            bar.setPrefWidth(46.0);
            bar.setMinHeight(42.0 + 130.0 * Math.abs(value) / Math.max(1, maximum));
            bar.getStyleClass().addAll("visual-bar", stateClass(cell));
            Label valueLabel = new Label(cell.text());
            valueLabel.getStyleClass().add("visual-bar-value");
            bar.getChildren().add(valueLabel);
            Label indexLabel = new Label(Integer.toString(index));
            indexLabel.getStyleClass().add("visual-index-label");
            VBox column = new VBox(5.0, bar, indexLabel);
            column.setAlignment(Pos.BOTTOM_CENTER);
            row.getChildren().add(column);
        }
        center(canvas, row);
    }

    private static void renderMergeSort(Pane canvas, MergeSortState state) {
        setLayout(canvas, "merge-sort", 300.0);
        VBox content = new VBox(12.0);
        content.setAlignment(Pos.CENTER);
        HBox ranges = new HBox(10.0);
        ranges.setAlignment(Pos.CENTER);
        for (MergeRange range : state.ranges()) {
            VBox group = new VBox(4.0);
            group.setAlignment(Pos.CENTER);
            group.getStyleClass().addAll("merge-range",
                    "active".equals(range.style()) ? "merge-range-active" : "merge-range-default");
            Label caption = new Label("[" + range.low() + ".." + range.high() + "]");
            caption.getStyleClass().add("merge-range-caption");
            HBox cells = new HBox(3.0);
            cells.setAlignment(Pos.CENTER);
            for (Integer value : range.values()) {
                StackPane cell = tile(new Cell(Integer.toString(value),
                        "active".equals(range.style()) ? "selected" : "default"), "merge-range-cell");
                cell.setMinSize(38.0, 38.0);
                cell.setPrefSize(38.0, 38.0);
                cells.getChildren().add(cell);
            }
            group.getChildren().addAll(caption, cells);
            ranges.getChildren().add(group);
        }
        Label phase = new Label(state.ranges().size() == 1 ? "CONTINUOUS ARRAY" : "SUBARRAYS");
        phase.getStyleClass().add("visual-structure-caption");
        content.getChildren().addAll(phase, ranges);
        center(canvas, content);
    }

    private static void renderStack(Pane canvas, SimulationStep step) {
        setLayout(canvas, "stack", 300.0);
        List<Cell> reversed = new ArrayList<>(step.cells());
        Collections.reverse(reversed);
        VBox frames = new VBox(7.0);
        frames.setAlignment(Pos.CENTER);
        frames.setFillWidth(false);
        Label top = new Label("TOP");
        top.getStyleClass().add("visual-structure-caption");
        frames.getChildren().add(top);
        for (Cell cell : reversed) {
            StackPane frame = tile(cell, "visual-stack-frame");
            frame.setMinWidth(230.0);
            frame.setPrefWidth(230.0);
            frame.setMaxWidth(230.0);
            frames.getChildren().add(frame);
        }
        center(canvas, frames);
    }

    private static void renderChain(Pane canvas, SimulationStep step, boolean queue) {
        setLayout(canvas, queue ? "queue" : "linked", 230.0);
        HBox chain = new HBox(8.0);
        chain.setAlignment(Pos.CENTER);
        if (queue) {
            chain.getChildren().add(caption("HEAD"));
        }
        for (int index = 0; index < step.cells().size(); index++) {
            chain.getChildren().add(tile(step.cells().get(index), "visual-chain-node"));
            if (index < step.cells().size() - 1) {
                Label arrow = new Label("->");
                arrow.getStyleClass().add("visual-arrow");
                chain.getChildren().add(arrow);
            }
        }
        chain.getChildren().add(caption(queue ? "TAIL" : "NULL"));
        center(canvas, chain);
    }

    private static void renderLinkedList(Pane canvas, SimulationStep step) {
        setLayout(canvas, "linked-list", 240.0);
        HBox chain = new HBox(8.0);
        chain.setAlignment(Pos.CENTER);

        List<Cell> cells = step.cells();
        int count = cells.size();

        for (int index = 0; index < count; index++) {
            Cell cell = cells.get(index);
            boolean isHead = (index == 0);
            boolean isTail = (index == count - 1);

            VBox nodeContainer = new VBox(5.0);
            nodeContainer.setAlignment(Pos.CENTER);

            // Pointer role badge at top (HEAD, TAIL, or placeholder for vertical alignment)
            Label pointerBadge;
            if (isHead && isTail) {
                pointerBadge = new Label("HEAD / TAIL");
                pointerBadge.getStyleClass().addAll("visual-linked-badge", "visual-linked-badge-head");
            } else if (isHead) {
                pointerBadge = new Label("HEAD");
                pointerBadge.getStyleClass().addAll("visual-linked-badge", "visual-linked-badge-head");
            } else if (isTail) {
                pointerBadge = new Label("TAIL");
                pointerBadge.getStyleClass().addAll("visual-linked-badge", "visual-linked-badge-tail");
            } else {
                pointerBadge = new Label(" ");
                pointerBadge.getStyleClass().add("visual-linked-badge-placeholder");
            }
            nodeContainer.getChildren().add(pointerBadge);

            // The Node Box: Square with Two Divisions [ Data | Next Pointer ]
            HBox nodeBox = new HBox();
            nodeBox.setAlignment(Pos.CENTER);
            nodeBox.getStyleClass().addAll("visual-linked-node", stateClass(cell));

            // Division 1: Data value compartment (square with number)
            StackPane dataPane = new StackPane();
            dataPane.getStyleClass().add("visual-linked-data");
            Label dataLabel = new Label(cell.text());
            dataLabel.getStyleClass().add("visual-linked-data-label");
            dataPane.getChildren().add(dataLabel);

            // Division 2: Pointer compartment (with pointer dot connecting to next node)
            StackPane ptrPane = new StackPane();
            ptrPane.getStyleClass().add("visual-linked-ptr");
            Label ptrDot = new Label("●");
            ptrDot.getStyleClass().add("visual-linked-dot");
            ptrPane.getChildren().add(ptrDot);

            nodeBox.getChildren().addAll(dataPane, ptrPane);
            nodeContainer.getChildren().add(nodeBox);

            // Index label at bottom
            Label indexLabel = new Label("node[" + index + "]");
            indexLabel.getStyleClass().add("visual-index-label");
            nodeContainer.getChildren().add(indexLabel);

            chain.getChildren().add(nodeContainer);

            // Connecting arrow from pointer division to next element
            Label arrow = new Label("──►");
            arrow.getStyleClass().add("visual-linked-arrow");
            chain.getChildren().add(arrow);
        }

        // Terminal NULL block
        VBox nullContainer = new VBox(5.0);
        nullContainer.setAlignment(Pos.CENTER);
        Label nullSpacer = new Label(" ");
        nullSpacer.getStyleClass().add("visual-linked-badge-placeholder");

        Label nullBox = new Label("NULL");
        nullBox.getStyleClass().add("visual-linked-null");

        Label nullSub = new Label("null ptr");
        nullSub.getStyleClass().add("visual-index-label");

        nullContainer.getChildren().addAll(nullSpacer, nullBox, nullSub);
        chain.getChildren().add(nullContainer);

        center(canvas, chain);
    }

    private static void renderArray(Pane canvas, SimulationStep step, String type) {
        setLayout(canvas, "array", 230.0);
        VBox content = new VBox(10.0);
        content.setAlignment(Pos.CENTER);
        HBox cells = new HBox(8.0);
        cells.setAlignment(Pos.CENTER);
        for (Cell cell : step.cells()) {
            StackPane node = tile(cell, "visual-array-cell");
            node.setMinWidth(type.equals("ALGEBRA") || type.equals("NUMBER_THEORY") ? 108.0 : NODE_SIZE);
            cells.getChildren().add(node);
        }
        content.getChildren().add(cells);
        if (type.contains("SEARCH") || type.equals("PREFIX_SUM") || type.equals("RANGE_QUERY")) {
            HBox indexes = new HBox(8.0);
            indexes.setAlignment(Pos.CENTER);
            for (int index = 0; index < step.cells().size(); index++) {
                Label label = new Label(Integer.toString(index));
                label.setMinWidth(NODE_SIZE);
                label.setAlignment(Pos.CENTER);
                label.getStyleClass().add("visual-index-label");
                indexes.getChildren().add(label);
            }
            content.getChildren().add(indexes);
        }
        center(canvas, content);
    }

    private static void renderGraph(Pane canvas, SimulationStep step, String type) {
        setLayout(canvas, "graph", 330.0);
        Pane stage = stage(canvas);
        List<Cell> cells = step.cells();
        int count = Math.max(1, Math.min(cells.size(), 8));
        double[][] positions = graphPositions(count);
        List<StackPane> nodes = new ArrayList<>();
        Map<String, StackPane> nodesById = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            StackPane node = tile(cells.get(index), "visual-graph-node");
            bindPosition(node, stage, positions[index][0], positions[index][1], NODE_SIZE);
            nodes.add(node);
            nodesById.put(vertexId(cells.get(index)), node);
        }

        if (!step.links().isEmpty()) {
            for (GraphLink edge : step.links()) {
                StackPane from = nodesById.get(edge.from());
                StackPane to = nodesById.get(edge.to());
                if (from == null || to == null) {
                    continue;
                }
                Line line = connectedLine(from, to);
                line.getStyleClass().add("visual-link");
                if (edge.active()) {
                    line.getStyleClass().add("visual-link-active");
                }
                stage.getChildren().add(line);
                if (edge.label() != null && !edge.label().isBlank()) {
                    Label label = new Label(edge.label());
                    label.getStyleClass().add("visual-link-label");
                    label.layoutXProperty().bind(from.layoutXProperty().add(to.layoutXProperty())
                            .divide(2.0).add(NODE_SIZE / 2.0).subtract(label.widthProperty().divide(2.0)));
                    label.layoutYProperty().bind(from.layoutYProperty().add(to.layoutYProperty())
                            .divide(2.0).add(NODE_SIZE / 2.0).subtract(label.heightProperty().divide(2.0)));
                    stage.getChildren().add(label);
                }
            }
        } else {
            int[][] edges = graphEdges(count, type);
            for (int[] edge : edges) {
                if (edge[0] >= nodes.size() || edge[1] >= nodes.size()) {
                    continue;
                }
                Line line = connectedLine(nodes.get(edge[0]), nodes.get(edge[1]));
                line.getStyleClass().add("visual-link");
                if (isActiveGraphEdge(step, type, cells.get(edge[0]), cells.get(edge[1]))) {
                    line.getStyleClass().add("visual-link-active");
                }
                stage.getChildren().add(line);
            }
        }
        stage.getChildren().addAll(nodes);
    }

    private static void renderTree(Pane canvas, SimulationStep step, String type) {
        setLayout(canvas, "tree", 350.0);
        if (type.equals("TRIE")) {
            renderTrie(canvas, step);
            return;
        }
        Pane stage = stage(canvas);
        int count = Math.min(step.cells().size(), 15);
        Map<Integer, StackPane> nodes = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            Cell cell = step.cells().get(index);
            if ("muted".equals(cell.style()) && cell.text().equalsIgnoreCase("empty")) {
                continue;
            }
            int level = 31 - Integer.numberOfLeadingZeros(index + 1);
            int firstAtLevel = (1 << level) - 1;
            int offset = index - firstAtLevel;
            int slots = 1 << level;
            double x = (offset + 1.0) / (slots + 1.0);
            double y = 0.14 + level * 0.22;
            StackPane node = tile(cell, "visual-tree-node");
            bindPosition(node, stage, x, Math.min(y, 0.86), NODE_SIZE);
            nodes.put(index, node);
            if (index > 0) {
                StackPane parent = nodes.get((index - 1) / 2);
                if (parent != null) {
                    Line line = connectedLine(parent, node);
                    line.getStyleClass().add("visual-link");
                    stage.getChildren().add(line);
                }
            }
        }
        stage.getChildren().addAll(nodes.values());
    }

    private static void renderTrie(Pane canvas, SimulationStep step) {
        Pane stage = stage(canvas);
        List<StackPane> nodes = new ArrayList<>();
        int count = Math.min(step.cells().size(), 14);
        for (int index = 0; index < count; index++) {
            String text = step.cells().get(index).text();
            int depth = text.equals("root") ? 0 : Math.min(4, text.length());
            long sameDepth = step.cells().subList(0, count).stream()
                    .filter(cell -> (cell.text().equals("root") ? 0 : Math.min(4, cell.text().length())) == depth)
                    .count();
            int order = 0;
            for (int prior = 0; prior < index; prior++) {
                String priorText = step.cells().get(prior).text();
                int priorDepth = priorText.equals("root") ? 0 : Math.min(4, priorText.length());
                if (priorDepth == depth) {
                    order++;
                }
            }
            double x = (order + 1.0) / (sameDepth + 1.0);
            double y = 0.12 + depth * 0.19;
            StackPane node = tile(step.cells().get(index), "visual-tree-node");
            bindPosition(node, stage, x, y, NODE_SIZE);
            nodes.add(node);
            if (index > 0) {
                int parent = trieParent(step.cells(), index);
                Line line = connectedLine(nodes.get(parent), node);
                line.getStyleClass().add("visual-link");
                stage.getChildren().add(line);
            }
        }
        stage.getChildren().addAll(nodes);
    }

    private static void renderDsu(Pane canvas, SimulationStep step) {
        setLayout(canvas, "forest", 300.0);
        Pane stage = stage(canvas);
        int count = Math.min(step.cells().size(), 10);
        List<StackPane> nodes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Cell source = step.cells().get(index);
            Cell labeled = new Cell(index + " -> " + source.text(), source.style());
            StackPane node = tile(labeled, "visual-dsu-node");
            double x = (index + 1.0) / (count + 1.0);
            int parent = Math.max(0, Math.min(count - 1, number(source.text())));
            double y = parent == index ? 0.30 : 0.68;
            bindPosition(node, stage, x, y, 68.0);
            nodes.add(node);
        }
        for (int index = 0; index < count; index++) {
            int parent = Math.max(0, Math.min(count - 1, number(step.cells().get(index).text())));
            if (parent != index) {
                Line line = connectedLine(nodes.get(parent), nodes.get(index));
                line.getStyleClass().add("visual-link");
                stage.getChildren().add(line);
            }
        }
        stage.getChildren().addAll(nodes);
    }

    private static void renderFenwick(Pane canvas, SimulationStep step) {
        setLayout(canvas, "fenwick", 320.0);
        Pane stage = stage(canvas);
        int count = Math.min(step.cells().size(), 12);
        List<StackPane> nodes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int fenwickIndex = index + 1;
            int covered = Integer.lowestOneBit(fenwickIndex);
            double level = Math.log(covered) / Math.log(2.0);
            StackPane node = tile(step.cells().get(index), "visual-fenwick-node");
            bindPosition(node, stage, (index + 1.0) / (count + 1.0), 0.76 - level * 0.19, 68.0);
            nodes.add(node);
        }
        for (int index = 0; index < count; index++) {
            int fenwickIndex = index + 1;
            int parentIndex = fenwickIndex + Integer.lowestOneBit(fenwickIndex);
            if (parentIndex <= count) {
                Line line = connectedLine(nodes.get(index), nodes.get(parentIndex - 1));
                line.getStyleClass().add("visual-link");
                stage.getChildren().add(line);
            }
        }
        stage.getChildren().addAll(nodes);
    }

    private static void renderQueensBoard(Pane canvas, SimulationStep step) {
        int size = Math.max(1, step.cells().size());
        setLayout(canvas, "board", Math.max(280.0, size * 58.0 + 40.0));
        GridPane board = new GridPane();
        board.setAlignment(Pos.CENTER);
        board.getStyleClass().add("visual-queen-board");
        for (int row = 0; row < size; row++) {
            int queenColumn = number(step.cells().get(row).text());
            boolean placed = queenColumn >= 0 && queenColumn < size;
            for (int column = 0; column < size; column++) {
                Label label = new Label(placed && queenColumn == column ? "Q" : "");
                label.setAlignment(Pos.CENTER);
                StackPane square = new StackPane(label);
                square.setMinSize(52.0, 52.0);
                square.setPrefSize(52.0, 52.0);
                square.getStyleClass().addAll("visual-board-square",
                        (row + column) % 2 == 0 ? "visual-board-light" : "visual-board-dark");
                if (placed && queenColumn == column) {
                    square.getStyleClass().add("visual-board-queen");
                }
                board.add(square, column, row);
            }
        }
        center(canvas, board);
    }

    private static void renderGrid(Pane canvas, SimulationStep step, String type) {
        int columns = switch (type) {
            case "FLOYD_WARSHALL", "LINEAR_ALGEBRA" -> Math.max(2, (int) Math.round(Math.sqrt(step.cells().size())));
            case "HASH_MAP", "SUFFIX_STRUCTURE" -> 1;
            default -> Math.min(6, Math.max(1, step.cells().size()));
        };
        int rows = Math.max(1, (int) Math.ceil(step.cells().size() / (double) columns));
        setLayout(canvas, "grid", Math.max(230.0, 70.0 + rows * 62.0));
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(7.0);
        grid.setVgap(7.0);
        grid.getStyleClass().add("visual-grid");
        for (int index = 0; index < step.cells().size(); index++) {
            StackPane node = tile(step.cells().get(index), "visual-grid-cell");
            node.setMinWidth(columns == 1 ? 280.0 : 78.0);
            grid.add(node, index % columns, index / columns);
        }
        center(canvas, grid);
    }

    private static void renderString(Pane canvas, SimulationStep step, String type) {
        if (type.equals("SUFFIX_STRUCTURE")) {
            renderGrid(canvas, step, type);
            return;
        }
        int columns = Math.min(14, Math.max(1, step.cells().size()));
        int rows = Math.max(1, (int) Math.ceil(step.cells().size() / (double) columns));
        setLayout(canvas, "string", Math.max(230.0, 90.0 + rows * 48.0));
        FlowPane characters = new FlowPane(5.0, 5.0);
        characters.setAlignment(Pos.CENTER);
        characters.setPrefWrapLength(columns * 43.0);
        for (Cell cell : step.cells()) {
            StackPane node = tile(cell, "visual-character-cell");
            node.setMinSize(38.0, 38.0);
            node.setPrefSize(38.0, 38.0);
            characters.getChildren().add(node);
        }
        center(canvas, characters);
    }

    private static void renderGeometry(Pane canvas, SimulationStep step) {
        setLayout(canvas, "geometry", 320.0);
        Pane stage = stage(canvas);
        Polygon triangle = new Polygon(150.0, 245.0, 545.0, 205.0, 350.0, 55.0);
        triangle.getStyleClass().add("visual-geometry-shape");
        stage.getChildren().add(triangle);
        addGeometryPoint(stage, "A", 150.0, 245.0);
        addGeometryPoint(stage, "B", 545.0, 205.0);
        addGeometryPoint(stage, "C", 350.0, 55.0);
        Label state = new Label(step.cells().stream().map(Cell::text).reduce((left, right) -> left + "  |  " + right).orElse(""));
        state.setWrapText(true);
        state.setMaxWidth(560.0);
        state.relocate(95.0, 272.0);
        state.getStyleClass().add("visual-geometry-state");
        stage.getChildren().add(state);
    }

    private static void addGeometryPoint(Pane stage, String name, double x, double y) {
        Circle point = new Circle(x, y, 8.0);
        point.getStyleClass().add("visual-geometry-point");
        Label label = new Label(name);
        label.relocate(x + 10.0, y - 19.0);
        label.getStyleClass().add("visual-geometry-label");
        stage.getChildren().addAll(point, label);
    }

    private static Pane stage(Pane canvas) {
        Pane stage = new Pane();
        stage.getStyleClass().add("visual-stage");
        stage.prefWidthProperty().bind(canvas.widthProperty());
        stage.prefHeightProperty().bind(canvas.heightProperty());
        stage.minWidthProperty().bind(canvas.widthProperty());
        stage.minHeightProperty().bind(canvas.heightProperty());
        canvas.getChildren().add(stage);
        return stage;
    }

    private static void center(Pane canvas, Node content) {
        StackPane stage = new StackPane(content);
        stage.setAlignment(Pos.CENTER);
        stage.getStyleClass().add("visual-stage");
        stage.prefWidthProperty().bind(canvas.widthProperty());
        stage.prefHeightProperty().bind(canvas.heightProperty());
        canvas.getChildren().add(stage);
    }

    private static StackPane tile(Cell cell, String familyClass) {
        Label label = new Label(cell.text());
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("visual-node-label");
        StackPane node = new StackPane(label);
        node.setAlignment(Pos.CENTER);
        node.setMinSize(NODE_SIZE, NODE_SIZE);
        node.setPrefSize(NODE_SIZE, NODE_SIZE);
        node.getStyleClass().addAll("simulation-cell", familyClass, stateClass(cell));
        return node;
    }

    private static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("visual-structure-caption");
        return label;
    }

    private static String stateClass(Cell cell) {
        String style = cell.style() == null || cell.style().isBlank() ? "default" : cell.style();
        return "simulation-cell-" + style;
    }

    private static boolean isActive(Cell cell) {
        return "active".equals(cell.style()) || "selected".equals(cell.style());
    }

    private static boolean isActiveGraphEdge(SimulationStep step, String type, Cell from, Cell to) {
        String left = vertexId(from);
        String right = vertexId(to);
        String message = step.message();
        if (type.equals("MAX_FLOW") && message.contains(" along [")) {
            int start = message.indexOf(" along [") + 8;
            int end = message.indexOf(']', start);
            if (end > start) {
                String[] path = message.substring(start, end).split(",\\s*");
                for (int index = 1; index < path.length; index++) {
                    if (sameEdge(left, right, path[index - 1], path[index])) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (type.equals("BELLMAN_FORD") || type.equals("SCC")) {
            return message.contains(left + " -> " + right) || message.contains(right + " -> " + left);
        }
        if (type.equals("BRIDGES") && message.toLowerCase(Locale.ROOT).contains("bridge")) {
            return message.contains(left + "-" + right) || message.contains(right + "-" + left);
        }
        return isActive(from) || isActive(to);
    }

    private static boolean sameEdge(String left, String right, String first, String second) {
        return left.equals(first) && right.equals(second) || left.equals(second) && right.equals(first);
    }

    private static String vertexId(Cell cell) {
        return cell.text().split("\\n", 2)[0].trim();
    }

    private static int number(String text) {
        Matcher matcher = INTEGER.matcher(text == null ? "" : text);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    private static void setLayout(Pane canvas, String layout, double height) {
        canvas.getStyleClass().add("visual-layout-" + layout);
        canvas.setMinHeight(height);
        canvas.setPrefHeight(height);
    }

    private static void bindPosition(Node node, Pane stage, double x, double y, double size) {
        node.layoutXProperty().bind(stage.widthProperty().multiply(x).subtract(size / 2.0));
        node.layoutYProperty().bind(stage.heightProperty().multiply(y).subtract(size / 2.0));
    }

    private static Line connectedLine(Node from, Node to) {
        Line line = new Line();
        line.startXProperty().bind(Bindings.createDoubleBinding(
                () -> (from.getBoundsInParent().getMinX() + from.getBoundsInParent().getMaxX()) / 2.0,
                from.boundsInParentProperty()));
        line.startYProperty().bind(Bindings.createDoubleBinding(
                () -> (from.getBoundsInParent().getMinY() + from.getBoundsInParent().getMaxY()) / 2.0,
                from.boundsInParentProperty()));
        line.endXProperty().bind(Bindings.createDoubleBinding(
                () -> (to.getBoundsInParent().getMinX() + to.getBoundsInParent().getMaxX()) / 2.0,
                to.boundsInParentProperty()));
        line.endYProperty().bind(Bindings.createDoubleBinding(
                () -> (to.getBoundsInParent().getMinY() + to.getBoundsInParent().getMaxY()) / 2.0,
                to.boundsInParentProperty()));
        return line;
    }

    private static int trieParent(List<Cell> cells, int index) {
        String value = cells.get(index).text();
        if (value.length() <= 1) {
            return 0;
        }
        String prefix = value.substring(0, value.length() - 1);
        for (int candidate = index - 1; candidate >= 0; candidate--) {
            if (cells.get(candidate).text().equals(prefix)
                    || prefix.isEmpty() && cells.get(candidate).text().equals("root")) {
                return candidate;
            }
        }
        return 0;
    }

    private static double[][] graphPositions(int count) {
        double[][] base = {
                {0.12, 0.50}, {0.32, 0.20}, {0.32, 0.80}, {0.58, 0.34},
                {0.58, 0.72}, {0.84, 0.50}, {0.74, 0.12}, {0.88, 0.84}
        };
        double[][] result = new double[count][2];
        for (int index = 0; index < count; index++) {
            result[index] = base[index];
        }
        return result;
    }

    private static int[][] graphEdges(int count, String type) {
        return switch (type) {
            case "GRAPH_REPRESENTATION", "TOPOLOGICAL_SORT" ->
                    new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}};
            case "BFS", "DFS" ->
                    new int[][]{{0, 1}, {0, 2}, {1, 3}, {1, 4}, {2, 5}, {4, 5}};
            case "BELLMAN_FORD" ->
                    new int[][]{{0, 1}, {0, 2}, {1, 2}, {1, 3}, {2, 3}, {3, 4}};
            case "SCC" ->
                    new int[][]{{0, 1}, {1, 2}, {1, 3}, {2, 0}, {3, 4}};
            case "BRIDGES" ->
                    new int[][]{{0, 1}, {1, 2}, {1, 3}, {2, 3}, {3, 4}};
            case "MAX_FLOW" ->
                    new int[][]{{0, 1}, {0, 2}, {1, 2}, {1, 3}, {2, 3}};
            default -> count <= 4
                    ? new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}}
                    : new int[][]{{0, 1}, {0, 2}, {1, 3}, {1, 4}, {2, 4}, {2, 5}, {3, 5}, {4, 5}};
        };
    }

    private static boolean isWeightedGraph(String type) {
        return type.equals("DIJKSTRA") || type.equals("PRIM") || type.equals("KRUSKAL");
    }

    private static boolean isGraph(String type) {
        return switch (type) {
            case "GRAPH_REPRESENTATION", "BFS", "DFS", "BELLMAN_FORD", "TOPOLOGICAL_SORT", "SCC",
                    "BRIDGES", "MAX_FLOW" -> true;
            default -> false;
        };
    }

    private static boolean isTree(String type) {
        return switch (type) {
            case "HEAP", "BST", "AVL", "TRIE", "SEGMENT_TREE" -> true;
            default -> false;
        };
    }

    private static boolean isGrid(String type) {
        return switch (type) {
            case "HASH_MAP", "FLOYD_WARSHALL", "DYNAMIC_PROGRAMMING", "SPARSE_TABLE", "LINEAR_ALGEBRA",
                    "COMBINATORICS" -> true;
            default -> false;
        };
    }

    private static boolean isString(String type) {
        return type.equals("STRING_MATCHING") || type.equals("SUFFIX_STRUCTURE") || type.equals("STRING_HASHING");
    }
}
