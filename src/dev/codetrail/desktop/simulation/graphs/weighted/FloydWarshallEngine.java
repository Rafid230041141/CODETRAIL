package dev.codetrail.desktop.simulation.graphs.weighted;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/** Floyd-Warshall trace exposing each intermediate distance matrix. */
public final class FloydWarshallEngine implements SimulationEngine {
    public static final String TYPE = "FLOYD_WARSHALL";
    public static final int MIN_N = WeightedGraphSupport.MIN_N;
    public static final int MAX_N = WeightedGraphSupport.MAX_N;
    public static final int MAX_EDGES = WeightedGraphSupport.MAX_EDGES;
    public static final long MAX_ABS_WEIGHT = WeightedGraphSupport.MAX_ABS_WEIGHT;
    public static final long INF = WeightedGraphSupport.INF;
    public static final int MAX_TRACE_STEPS = WeightedGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_FOR_I = 2;
    private static final int LINE_FILL = 3;
    private static final int LINE_DIAGONAL = 4;
    private static final int LINE_FOR_EDGE = 5;
    private static final int LINE_DIRECT_EDGE = 6;
    private static final int LINE_INTERMEDIATE = 7;
    private static final int LINE_ROW = 8;
    private static final int LINE_COLUMN = 9;
    private static final int LINE_CHECK = 10;
    private static final int LINE_UPDATE = 11;
    private static final int LINE_NEGATIVE_CYCLE = 12;
    private static final int LINE_RETURN = 13;

    private static final List<String> PSEUDOCODE = List.of(
            "floydWarshall(G):",
            "    for each row i:",
            "        for each column j: distance[i][j] = ∞",
            "        distance[i][i] = 0",
            "    for each edge (u, v, weight):",
            "        distance[u][v] = min(distance[u][v], weight)",
            "    for k = 0 to |V| - 1:",
            "        for i = 0 to |V| - 1:",
            "            for j = 0 to |V| - 1:",
            "                if distance[i][k] + distance[k][j] < distance[i][j]:",
            "                    distance[i][j] = distance[i][k] + distance[k][j]",
            "    if any distance[i][i] < 0: report a negative cycle",
            "    return distance");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        WeightedGraphSupport.Input graph = WeightedGraphSupport.parse(
                input, TYPE, false, true, false);
        int n = graph.n();
        long[][] matrix = new long[n][n];
        for (long[] row : matrix) {
            Arrays.fill(row, WeightedGraphSupport.INF);
        }
        List<String> columns = new ArrayList<>(n + 1);
        columns.add("from");
        for (int vertex = 0; vertex < n; vertex++) {
            columns.add(Integer.toString(vertex));
        }
        WeightedGraphSupport.TableTraceBuilder trace = new WeightedGraphSupport.TableTraceBuilder(columns);
        int currentK = -1;
        int currentI = -1;
        int currentJ = -1;
        boolean negativeCycle = false;
        String phase = "initialize";
        String teachingEquation = "";

        trace.add(
                WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                0,
                "Initialize Floyd-Warshall for all " + n + " vertices",
                StepEventType.INITIALIZE);
        trace.add(
                WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                LINE_METHOD,
                "Run all-pairs shortest paths through intermediate vertices",
                StepEventType.EXECUTE_LINE);
        trace.add(
                WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                LINE_FOR_I,
                "Prepare one distance row for each source vertex",
                StepEventType.EXECUTE_LINE);
        trace.add(
                WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                LINE_FILL,
                "Fill unknown pairs with ∞ before adding direct edges",
                StepEventType.EXECUTE_LINE);

        for (int vertex = 0; vertex < n; vertex++) {
            matrix[vertex][vertex] = 0L;
        }
        currentI = -1;
        currentJ = -1;
        trace.add(
                WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                LINE_DIAGONAL,
                "Set every diagonal distance[i][i] = 0",
                StepEventType.EXECUTE_LINE);

        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            WeightedGraphSupport.WeightedEdge edge = graph.edges().get(edgeIndex);
            currentI = edge.from();
            currentJ = edge.to();
            trace.add(
                    WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                    facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                    LINE_FOR_EDGE,
                    "Read direct edge " + edge.from() + " -> " + edge.to() + " with weight " + edge.weight(),
                    StepEventType.EXECUTE_LINE);
            matrix[edge.from()][edge.to()] = Math.min(matrix[edge.from()][edge.to()], edge.weight());
            if (!graph.directed() && edge.from() != edge.to()) {
                matrix[edge.to()][edge.from()] = Math.min(matrix[edge.to()][edge.from()], edge.weight());
            }
            trace.add(
                    WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                    facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                    LINE_DIRECT_EDGE,
                    "Store the direct edge weight in the initial distance matrix",
                    StepEventType.EXECUTE_LINE);
        }

        phase = "intermediate-relaxation";
        for (int k = 0; k < n; k++) {
            currentK = k;
            teachingEquation = "";
            currentI = -1;
            currentJ = -1;
            trace.add(
                    WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                    facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                    LINE_INTERMEDIATE,
                    "Use vertex " + k + " as the next allowed intermediate",
                    StepEventType.EXECUTE_LINE);
            for (int i = 0; i < n; i++) {
                currentI = i;
                teachingEquation = "";
                currentJ = -1;
                trace.add(
                        WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                        facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                        LINE_ROW,
                        "Inspect source row i = " + i + " through intermediate " + k,
                        StepEventType.EXECUTE_LINE);
                for (int j = 0; j < n; j++) {
                    currentJ = j;
                    long candidate = safeAdd(matrix[i][k], matrix[k][j]);
                    teachingEquation = "d[" + i + "][" + j + "] = min("
                            + WeightedGraphSupport.formatDistance(matrix[i][j]) + ", "
                            + WeightedGraphSupport.formatDistance(matrix[i][k]) + " + "
                            + WeightedGraphSupport.formatDistance(matrix[k][j]) + ") = "
                            + WeightedGraphSupport.formatDistance(Math.min(matrix[i][j], candidate));
                    trace.add(
                            WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                            facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                            LINE_COLUMN,
                            "Check destination column j = " + j + " via " + i + " -> " + k + " -> " + j,
                            StepEventType.EXECUTE_LINE);
                    trace.add(
                            WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                            facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                            LINE_CHECK,
                            "Compare candidate " + WeightedGraphSupport.formatDistance(candidate)
                                    + " with distance[" + i + "][" + j + "] = "
                                    + WeightedGraphSupport.formatDistance(matrix[i][j]),
                            StepEventType.EXECUTE_LINE);
                    if (candidate < matrix[i][j]) {
                        matrix[i][j] = candidate;
                        trace.add(
                                WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                                facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                                LINE_UPDATE,
                                "Update distance[" + i + "][" + j + "] to " + candidate
                                        + " through intermediate " + k,
                                StepEventType.EXECUTE_LINE);
                    }
                }
            }
        }

        phase = "cycle-check";
        teachingEquation = "";
        currentK = -1;
        currentJ = -1;
        for (int vertex = 0; vertex < n; vertex++) {
            currentI = vertex;
            boolean hasNegativeDiagonal = matrix[vertex][vertex] < 0L;
            if (hasNegativeDiagonal) {
                negativeCycle = true;
            }
            trace.add(
                    WeightedGraphSupport.matrixRows(matrix, currentK, currentI, currentJ, false),
                    facts(graph, matrix, currentK, currentI, currentJ, negativeCycle, phase, teachingEquation, false),
                    LINE_NEGATIVE_CYCLE,
                    hasNegativeDiagonal
                            ? "distance[" + vertex + "][" + vertex + "] < 0; report a negative cycle"
                            : "distance[" + vertex + "][" + vertex + "] is nonnegative",
                    StepEventType.EXECUTE_LINE);
        }
        currentI = -1;
        currentJ = -1;
        trace.add(
                WeightedGraphSupport.matrixRows(matrix, -1, -1, -1, true),
                facts(graph, matrix, -1, currentI, currentJ, negativeCycle, "return", "", true),
                LINE_RETURN,
                negativeCycle
                        ? "Return the matrix and report a negative cycle"
                        : "Return the all-pairs shortest-distance matrix",
                StepEventType.EXECUTE_LINE);
        trace.add(
                WeightedGraphSupport.matrixRows(matrix, -1, -1, -1, true),
                facts(graph, matrix, -1, currentI, currentJ, negativeCycle, "complete", "", true),
                0,
                negativeCycle
                        ? "Complete: the distance matrix contains a negative cycle"
                        : "Complete: all-pairs shortest distances are finalized",
                StepEventType.COMPLETE);
        return trace.steps();
    }

    private static List<Fact> facts(
            WeightedGraphSupport.Input graph,
            long[][] matrix,
            int currentK,
            int currentI,
            int currentJ,
            boolean negativeCycle,
            String phase,
            String teachingEquation,
            boolean complete) {
        SnapshotStatus resultStatus = complete ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
        String current = currentK < 0
                ? currentI < 0 ? "NIL" : "i=" + currentI
                : "k=" + currentK + (currentI < 0 ? "" : ", i=" + currentI)
                        + (currentJ < 0 ? "" : ", j=" + currentJ);
        boolean selected = currentK >= 0 && currentI >= 0 && currentJ >= 0;
        return List.of(
                WeightedGraphSupport.fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("teaching-equation", teachingEquation, SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("teaching-detail", selected
                        ? "Known inputs d[" + currentI + "][" + currentK + "] and d[" + currentK + "]["
                                + currentJ + "]; target d[" + currentI + "][" + currentJ + "]"
                        : "Rows are starting vertices; columns are destinations", SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("target-row", Integer.toString(currentI), SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("target-column", Integer.toString(currentJ < 0 ? -1 : currentJ + 1), SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("operand-left-row", Integer.toString(currentI), SnapshotStatus.DONE),
                WeightedGraphSupport.fact("operand-left-column", Integer.toString(currentK < 0 ? -1 : currentK + 1), SnapshotStatus.DONE),
                WeightedGraphSupport.fact("operand-right-row", Integer.toString(currentK), SnapshotStatus.DONE),
                WeightedGraphSupport.fact("operand-right-column", Integer.toString(currentJ < 0 ? -1 : currentJ + 1), SnapshotStatus.DONE),
                WeightedGraphSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("intermediate", currentK < 0 ? "NONE" : Integer.toString(currentK),
                        currentK < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("current", current, current.equals("NIL")
                        ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("matrix", formatMatrix(matrix), resultStatus),
                WeightedGraphSupport.fact("negative-cycle", Boolean.toString(negativeCycle),
                        negativeCycle ? SnapshotStatus.REJECTED : resultStatus),
                WeightedGraphSupport.fact("invariant", "Every update allows only the intermediates 0..k",
                        resultStatus),
                WeightedGraphSupport.fact("directed", Boolean.toString(graph.directed()), SnapshotStatus.DEFAULT));
    }

    private static String formatMatrix(long[][] matrix) {
        StringJoiner rows = new StringJoiner("; ", "[", "]");
        for (long[] row : matrix) {
            StringJoiner values = new StringJoiner(", ", "[", "]");
            for (long value : row) {
                values.add(WeightedGraphSupport.formatDistance(value));
            }
            rows.add(values.toString());
        }
        return rows.toString();
    }

    private static long safeAdd(long first, long second) {
        if (first >= WeightedGraphSupport.INF || second >= WeightedGraphSupport.INF) {
            return WeightedGraphSupport.INF;
        }
        if (second > 0L && first > WeightedGraphSupport.INF - second) {
            return WeightedGraphSupport.INF;
        }
        if (second < 0L && first < -WeightedGraphSupport.INF - second) {
            return -WeightedGraphSupport.INF;
        }
        return first + second;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = WeightedGraphSupport.inputWithEdges(
                4,
                List.of(
                        new WeightedGraphSupport.WeightedEdge(0, 1, 3),
                        new WeightedGraphSupport.WeightedEdge(0, 2, 8),
                        new WeightedGraphSupport.WeightedEdge(1, 2, 2),
                        new WeightedGraphSupport.WeightedEdge(1, 3, 12),
                        new WeightedGraphSupport.WeightedEdge(2, 3, 4)),
                null,
                true);
        return new SimulationMetadata(
                TYPE,
                "Floyd-Warshall",
                "O(V³)",
                "O(V²)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"n\":4,\"edges\":[{\"from\":0,\"to\":1,\"weight\":3}],\"directed\":true}; n must be "
                        + MIN_N + ".." + MAX_N + ", there may be at most " + MAX_EDGES
                        + " unique logical edges, weights are integers from -" + MAX_ABS_WEIGHT + " through "
                        + MAX_ABS_WEIGHT + ", and a negative diagonal after the intermediate passes reports a negative cycle.",
                PSEUDOCODE);
    }
}
