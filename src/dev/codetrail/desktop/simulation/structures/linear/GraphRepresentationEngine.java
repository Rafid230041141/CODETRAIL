package dev.codetrail.desktop.simulation.structures.linear;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds an adjacency list or matrix one logical edge at a time in TABLE state. */
public final class GraphRepresentationEngine implements SimulationEngine {
    public static final String TYPE = "GRAPH_REPRESENTATION";
    public static final int MIN_N = 1;
    public static final int MAX_N = 8;
    public static final int MAX_EDGES = 24;
    public static final int MAX_TRACE_STEPS = LinearStructureSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_INITIALIZE = 2;
    private static final int LINE_EDGE = 3;
    private static final int LINE_LIST_KIND = 4;
    private static final int LINE_LIST_ADD = 5;
    private static final int LINE_LIST_REVERSE = 6;
    private static final int LINE_MATRIX_KIND = 7;
    private static final int LINE_MATRIX_ADD = 8;
    private static final int LINE_MATRIX_REVERSE = 9;
    private static final int LINE_RETURN = 10;

    private static final int DEFAULT_N = 5;
    private static final boolean DEFAULT_DIRECTED = false;
    private static final String DEFAULT_REPRESENTATION = "adjacency-list";
    private static final int[][] DEFAULT_EDGES = {{0, 1}, {0, 3}, {1, 2}, {3, 4}};
    private static final List<String> PSEUDOCODE = List.of(
            "buildRepresentation(n, edges, mode):",
            "    initialize an empty list or matrix",
            "    for each edge (u, v):",
            "        if mode is adjacency-list:",
            "            neighbors[u].add(v)",
            "            if graph is undirected: neighbors[v].add(u)",
            "        else mode is adjacency-matrix:",
            "            matrix[u][v] = 1",
            "            if graph is undirected: matrix[v][u] = 1",
            "    return representation");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        TableTrace trace = new TableTrace(request.n(), request.directed(), request.representation());
        trace.add(
                0,
                "Initialize empty " + request.representation(),
                StepEventType.INITIALIZE,
                facts(request, trace, "initialize", -1, -1, SnapshotStatus.ACTIVE));
        trace.add(
                LINE_METHOD,
                "Build " + request.representation() + " for " + request.n() + " vertices",
                StepEventType.EXECUTE_LINE,
                facts(request, trace, "start", -1, -1, SnapshotStatus.ACTIVE));
        trace.add(
                LINE_INITIALIZE,
                "Create " + request.n() + " empty row(s)",
                StepEventType.EXECUTE_LINE,
                facts(request, trace, "empty", -1, -1, SnapshotStatus.ACTIVE));

        for (int edgeIndex = 0; edgeIndex < request.edges().size(); edgeIndex++) {
            Edge edge = request.edges().get(edgeIndex);
            trace.clearStatuses();
            trace.add(
                    LINE_EDGE,
                    "Read edge " + edge.from() + " -> " + edge.to(),
                    StepEventType.EXECUTE_LINE,
                    facts(request, trace, "read edge", edge.from(), edge.to(), SnapshotStatus.ACTIVE));
            if (request.representation().equals("adjacency-list")) {
                trace.addListArc(edge.from(), edge.to());
                trace.add(
                        LINE_LIST_ADD,
                        "Append " + edge.to() + " to neighbors[" + edge.from() + "]",
                        StepEventType.EXECUTE_LINE,
                        facts(request, trace, "add arc", edge.from(), edge.to(), SnapshotStatus.ACTIVE));
                if (!request.directed() && edge.from() != edge.to()) {
                    trace.addListArc(edge.to(), edge.from());
                    trace.add(
                            LINE_LIST_REVERSE,
                            "Append " + edge.from() + " to neighbors[" + edge.to() + "]",
                            StepEventType.EXECUTE_LINE,
                            facts(request, trace, "add reverse arc", edge.to(), edge.from(), SnapshotStatus.ACTIVE));
                }
            } else {
                trace.setMatrix(edge.from(), edge.to());
                trace.add(
                        LINE_MATRIX_ADD,
                        "Set matrix[" + edge.from() + "][" + edge.to() + "] = 1",
                        StepEventType.EXECUTE_LINE,
                        facts(request, trace, "set cell", edge.from(), edge.to(), SnapshotStatus.ACTIVE));
                if (!request.directed() && edge.from() != edge.to()) {
                    trace.setMatrix(edge.to(), edge.from());
                    trace.add(
                            LINE_MATRIX_REVERSE,
                            "Set matrix[" + edge.to() + "][" + edge.from() + "] = 1",
                            StepEventType.EXECUTE_LINE,
                            facts(request, trace, "set reverse cell", edge.to(), edge.from(), SnapshotStatus.ACTIVE));
                }
            }
        }

        trace.allDone();
        trace.add(
                LINE_RETURN,
                "Return the completed " + request.representation(),
                StepEventType.EXECUTE_LINE,
                facts(request, trace, "return", -1, -1, SnapshotStatus.DONE));
        trace.add(
                0,
                "Complete: " + request.representation() + " built",
                StepEventType.COMPLETE,
                facts(request, trace, "complete", -1, -1, SnapshotStatus.DONE));
        return trace.steps();
    }

    private static List<Fact> facts(
            Request request,
            TableTrace trace,
            String phase,
            int from,
            int to,
            SnapshotStatus status) {
        String edge = from < 0 ? "none" : from + "->" + to;
        return List.of(
                LinearStructureSupport.fact("representation", request.representation(), SnapshotStatus.DEFAULT),
                LinearStructureSupport.fact("directed", Boolean.toString(request.directed()), SnapshotStatus.DEFAULT),
                LinearStructureSupport.fact("edge", edge, status),
                LinearStructureSupport.fact("arcs", Integer.toString(trace.arcCount()), status),
                LinearStructureSupport.fact("table", trace.summary(), status),
                LinearStructureSupport.fact("phase", phase, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("n", DEFAULT_N);
        ArrayNode edges = defaultInput.putArray("edges");
        for (int[] edge : DEFAULT_EDGES) {
            ArrayNode pair = edges.addArray();
            pair.add(edge[0]);
            pair.add(edge[1]);
        }
        defaultInput.put("directed", DEFAULT_DIRECTED);
        defaultInput.put("representation", DEFAULT_REPRESENTATION);
        return new SimulationMetadata(
                TYPE,
                "Graph Representation",
                "O(V + E) list or O(V²) matrix",
                "O(V + E) list or O(V²) matrix",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"n\":5,\"edges\":[[0,1],[0,3]],\"directed\":false,\"representation\":\"adjacency-list\"}; n must be "
                        + MIN_N + ".." + MAX_N + ", there may be at most " + MAX_EDGES
                        + " unique logical edges, endpoints must be in range, and representation must be adjacency-list or adjacency-matrix.",
                PSEUDOCODE);
    }

    private record Edge(int from, int to) {
    }

    private record Request(int n, List<Edge> edges, boolean directed, String representation) {
        static Request parse(JsonNode input) {
            ObjectNode object = LinearStructureSupport.objectInput(input, TYPE);
            LinearStructureSupport.exactFields(object, Set.of("n", "edges", "directed", "representation"), TYPE);
            int n = LinearStructureSupport.integer(object.get("n"), TYPE + " n");
            LinearStructureSupport.requireIndex(n, MIN_N, MAX_N, TYPE + " n");
            JsonNode edgesNode = object.get("edges");
            if (edgesNode == null || !edgesNode.isArray()) {
                throw new IllegalArgumentException(TYPE + " edges must be a JSON array");
            }
            if (edgesNode.size() > MAX_EDGES) {
                throw new IllegalArgumentException(TYPE + " edges must contain at most " + MAX_EDGES + " entries");
            }
            boolean directed = LinearStructureSupport.optionalBoolean(object, "directed", DEFAULT_DIRECTED);
            String representation = LinearStructureSupport.text(
                    object.get("representation"), TYPE + " representation");
            if (!representation.equals("adjacency-list") && !representation.equals("adjacency-matrix")) {
                throw new IllegalArgumentException(TYPE + " representation must be exactly adjacency-list or adjacency-matrix");
            }
            Set<String> seen = new HashSet<>();
            List<Edge> edges = new ArrayList<>(edgesNode.size());
            for (int index = 0; index < edgesNode.size(); index++) {
                JsonNode edgeNode = edgesNode.get(index);
                String context = TYPE + " edge " + index;
                if (edgeNode == null || !edgeNode.isArray() || edgeNode.size() != 2) {
                    throw new IllegalArgumentException(context + " must be an integer pair [from,to]");
                }
                int from = LinearStructureSupport.integer(edgeNode.get(0), context + " from");
                int to = LinearStructureSupport.integer(edgeNode.get(1), context + " to");
                LinearStructureSupport.requireIndex(from, 0, n - 1, context + " from");
                LinearStructureSupport.requireIndex(to, 0, n - 1, context + " to");
                int first = directed ? from : Math.min(from, to);
                int second = directed ? to : Math.max(from, to);
                if (!seen.add(first + ":" + second)) {
                    throw new IllegalArgumentException(context + " duplicates an existing logical edge");
                }
                edges.add(new Edge(from, to));
            }
            return new Request(n, List.copyOf(edges), directed, representation);
        }
    }

    private static final class TableTrace {
        private final int n;
        private final boolean directed;
        private final String representation;
        private final List<List<Integer>> adjacency;
        private final int[][] matrix;
        private final List<List<SnapshotStatus>> statuses;
        private final List<SimulationStep> steps = new ArrayList<>();

        private TableTrace(int n, boolean directed, String representation) {
            this.n = n;
            this.directed = directed;
            this.representation = representation;
            adjacency = new ArrayList<>(n);
            for (int vertex = 0; vertex < n; vertex++) {
                adjacency.add(new ArrayList<>());
            }
            matrix = new int[n][n];
            int columns = representation.equals("adjacency-list") ? 2 : n + 1;
            statuses = new ArrayList<>(n);
            for (int row = 0; row < n; row++) {
                List<SnapshotStatus> rowStatuses = new ArrayList<>(columns);
                for (int column = 0; column < columns; column++) {
                    rowStatuses.add(SnapshotStatus.DEFAULT);
                }
                statuses.add(rowStatuses);
            }
        }

        private void clearStatuses() {
            for (List<SnapshotStatus> row : statuses) {
                Collections.fill(row, SnapshotStatus.DEFAULT);
            }
        }

        private void allDone() {
            for (List<SnapshotStatus> row : statuses) {
                Collections.fill(row, SnapshotStatus.DONE);
            }
        }

        private void addListArc(int from, int to) {
            List<Integer> neighbors = adjacency.get(from);
            if (!neighbors.contains(to)) {
                neighbors.add(to);
                Collections.sort(neighbors);
            }
            clearStatuses();
            statuses.get(from).set(1, SnapshotStatus.ACTIVE);
        }

        private void setMatrix(int from, int to) {
            matrix[from][to] = 1;
            clearStatuses();
            statuses.get(from).set(to + 1, SnapshotStatus.ACTIVE);
        }

        private int arcCount() {
            if (representation.equals("adjacency-list")) {
                return adjacency.stream().mapToInt(List::size).sum();
            }
            int count = 0;
            for (int row = 0; row < n; row++) {
                for (int column = 0; column < n; column++) {
                    count += matrix[row][column];
                }
            }
            return count;
        }

        private String summary() {
            if (representation.equals("adjacency-list")) {
                StringBuilder result = new StringBuilder("{");
                for (int vertex = 0; vertex < n; vertex++) {
                    if (vertex > 0) {
                        result.append(", ");
                    }
                    result.append(vertex).append('=').append(adjacency.get(vertex));
                }
                return result.append('}').toString();
            }
            StringBuilder result = new StringBuilder();
            for (int row = 0; row < n; row++) {
                if (row > 0) {
                    result.append(';');
                }
                result.append('[');
                for (int column = 0; column < n; column++) {
                    if (column > 0) {
                        result.append(',');
                    }
                    result.append(matrix[row][column]);
                }
                result.append(']');
            }
            return result.toString();
        }

        private TableState state(List<Fact> facts) {
            List<String> columns = new ArrayList<>();
            List<List<TypedCell>> rows = new ArrayList<>(n);
            if (representation.equals("adjacency-list")) {
                columns.add("vertex");
                columns.add("neighbors");
                for (int vertex = 0; vertex < n; vertex++) {
                    rows.add(List.of(
                            new TypedCell("vertex " + vertex, Integer.toString(vertex), statuses.get(vertex).get(0)),
                            new TypedCell("neighbors " + vertex, adjacency.get(vertex).toString(), statuses.get(vertex).get(1))));
                }
            } else {
                columns.add("vertex");
                for (int vertex = 0; vertex < n; vertex++) {
                    columns.add(Integer.toString(vertex));
                }
                for (int row = 0; row < n; row++) {
                    List<TypedCell> cells = new ArrayList<>(n + 1);
                    cells.add(new TypedCell("vertex " + row, Integer.toString(row), statuses.get(row).get(0)));
                    for (int column = 0; column < n; column++) {
                        cells.add(new TypedCell(
                                "matrix " + row + "," + column,
                                Integer.toString(matrix[row][column]),
                                statuses.get(row).get(column + 1)));
                    }
                    rows.add(List.copyOf(cells));
                }
            }
            return new TableState(columns, rows, facts);
        }

        private void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("linear structure trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            TableState state = state(facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.of(), Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private List<SimulationStep> steps() {
            return List.copyOf(steps);
        }
    }
}
