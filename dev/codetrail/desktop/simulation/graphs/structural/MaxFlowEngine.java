package dev.codetrail.desktop.simulation.graphs.structural;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Edmonds-Karp maximum flow with explicit forward and reverse residual arcs. */
public final class MaxFlowEngine implements SimulationEngine {
    public static final String TYPE = "MAX_FLOW";
    public static final int MIN_N = StructuralGraphSupport.MIN_N;
    public static final int MAX_N = StructuralGraphSupport.MAX_N;
    public static final int MAX_EDGES = StructuralGraphSupport.MAX_EDGES;
    public static final int MIN_CAPACITY = StructuralGraphSupport.MIN_CAPACITY;
    public static final int MAX_CAPACITY = StructuralGraphSupport.MAX_CAPACITY;
    public static final int MAX_TRACE_STEPS = StructuralGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_RESIDUAL = 2;
    private static final int LINE_WHILE = 3;
    private static final int LINE_BFS = 4;
    private static final int LINE_STOP = 5;
    private static final int LINE_PATH = 6;
    private static final int LINE_BOTTLENECK = 7;
    private static final int LINE_UPDATE = 8;
    private static final int LINE_FORWARD = 9;
    private static final int LINE_REVERSE = 10;
    private static final int LINE_FLOW = 11;
    private static final int LINE_CUT = 12;
    private static final int LINE_RETURN = 13;

    private static final List<String> PSEUDOCODE = List.of(
            "edmondsKarp(G, source, sink):",
            "    residual = capacity graph with reverse arcs",
            "    while true:",
            "        find a source-to-sink path by BFS in residual",
            "        if no path exists: break",
            "        path = the discovered residual path",
            "        bottleneck = minimum residual capacity on path",
            "        for each residual arc on path:",
            "            if forward: flow += bottleneck; update both residual directions",
            "            else: flow -= bottleneck; update both residual directions",
            "        add bottleneck to total flow",
            "    find the source side of the residual cut",
            "    return max flow and a matching minimum cut");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        StructuralGraphSupport.GraphInput graph = StructuralGraphSupport.parseFlowGraph(input);
        FlowRun run = new FlowRun(graph);
        run.add(
                0,
                "Initialize Edmonds-Karp from source " + graph.source() + " to sink " + graph.sink(),
                StepEventType.INITIALIZE,
                List.of(),
                List.of());
        run.add(
                LINE_METHOD,
                "Use breadth-first augmenting paths so every residual update is explicit",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                List.of());
        run.add(
                LINE_RESIDUAL,
                "Build residual arcs; parallel capacities remain separate and are summed by flow",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source(), graph.sink()),
                List.of());

        while (true) {
            run.focusedArc = null;
            run.residualBefore = "";
            run.residualAfter = "";
            run.bottleneckCalculation = "";
            run.bottleneck = 0;
            run.bfsQueue.clear();
            run.pathVertices.clear();
            run.pathArcs.clear();
            run.add(
                    LINE_WHILE,
                    "Search for another augmenting path; current flow = " + run.totalFlow,
                    StepEventType.EXECUTE_LINE,
                    List.of(graph.source()),
                    List.of());
            boolean found = run.findAugmentingPath();
            run.add(
                    LINE_BFS,
                    found
                            ? "BFS reached sink through residual path " + run.pathVerticesText()
                            : "BFS cannot reach the sink in the residual network",
                    StepEventType.EXECUTE_LINE,
                    found ? run.pathVertices : List.of(graph.source()),
                    run.pathEdgeIndexes(),
                    false);
            if (!found) {
                run.add(
                        LINE_STOP,
                        "Stop: no residual source-to-sink path remains",
                        StepEventType.EXECUTE_LINE,
                        List.of(graph.source()),
                        List.of());
                break;
            }

            run.add(
                    LINE_PATH,
                    "Use residual path " + run.pathDescription(),
                    StepEventType.EXECUTE_LINE,
                    run.pathVertices,
                    run.pathEdgeIndexes());
            run.computeBottleneck();
            run.add(
                    LINE_BOTTLENECK,
                    "Path capacity: " + run.bottleneckCalculation,
                    StepEventType.EXECUTE_LINE,
                    run.pathVertices,
                    run.pathEdgeIndexes());
            run.augment();
        }

        run.computeMinCut();
        run.markTerminalStatuses();
        run.add(
                LINE_CUT,
                "Residual reachability gives source side " + run.reachableText()
                        + "; min-cut capacity = " + run.cutCapacity,
                StepEventType.EXECUTE_LINE,
                run.reachableVertices(),
                run.cutEdgeIndexes(),
                true);
        run.add(
                LINE_RETURN,
                "Return max flow " + run.totalFlow + " and matching min-cut capacity " + run.cutCapacity,
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                true);
        run.add(
                0,
                "Complete: max flow = " + run.totalFlow + "; min cut = " + run.cutCapacity,
                StepEventType.COMPLETE,
                List.of(),
                List.of(),
                true);
        return run.trace.steps();
    }

    private static final class ResidualArc {
        private final int from;
        private final int to;
        private final int reverseIndex;
        private final int edgeIndex;
        private final boolean forward;
        private int residual;

        private ResidualArc(
                int from, int to, int reverseIndex, int residual, int edgeIndex, boolean forward) {
            this.from = from;
            this.to = to;
            this.reverseIndex = reverseIndex;
            this.residual = residual;
            this.edgeIndex = edgeIndex;
            this.forward = forward;
        }
    }

    private static final class FlowRun {
        private final StructuralGraphSupport.GraphInput graph;
        private final StructuralGraphSupport.TraceBuilder trace;
        private final List<List<ResidualArc>> residual;
        private final int[] flow;
        private final ResidualArc[] parentArc;
        private final int[] parentVertex;
        private final Deque<Integer> bfsQueue = new ArrayDeque<>();
        private final List<Integer> pathVertices = new ArrayList<>();
        private final List<ResidualArc> pathArcs = new ArrayList<>();
        private final boolean[] reachable;
        private final List<Integer> cutEdges = new ArrayList<>();
        private int bottleneck;
        private int totalFlow;
        private int cutCapacity;
        private boolean minCutComputed;
        private ResidualArc focusedArc;
        private String residualBefore = "";
        private String residualAfter = "";
        private String bottleneckCalculation = "";

        private FlowRun(StructuralGraphSupport.GraphInput graph) {
            this.graph = graph;
            List<String> labels = new ArrayList<>(graph.edges().size());
            for (StructuralGraphSupport.InputEdge edge : graph.edges()) {
                labels.add(flowLabel(edge.capacity(), 0, edge.from() == edge.to()));
            }
            trace = new StructuralGraphSupport.TraceBuilder(graph, labels);
            residual = new ArrayList<>(graph.n());
            for (int vertex = 0; vertex < graph.n(); vertex++) {
                residual.add(new ArrayList<>());
            }
            flow = new int[graph.edges().size()];
            parentArc = new ResidualArc[graph.n()];
            parentVertex = new int[graph.n()];
            reachable = new boolean[graph.n()];
            for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
                StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
                if (edge.from() != edge.to()) {
                    addResidualEdge(edgeIndex, edge);
                }
            }
            updateLabels();
        }

        private void addResidualEdge(int edgeIndex, StructuralGraphSupport.InputEdge edge) {
            List<ResidualArc> fromArcs = residual.get(edge.from());
            List<ResidualArc> toArcs = residual.get(edge.to());
            ResidualArc forward = new ResidualArc(
                    edge.from(), edge.to(), toArcs.size(), edge.capacity(), edgeIndex, true);
            ResidualArc reverse = new ResidualArc(
                    edge.to(), edge.from(), fromArcs.size(), 0, edgeIndex, false);
            fromArcs.add(forward);
            toArcs.add(reverse);
        }

        private boolean findAugmentingPath() {
            Arrays.fill(parentArc, null);
            Arrays.fill(parentVertex, -1);
            pathVertices.clear();
            pathArcs.clear();
            bottleneck = 0;
            focusedArc = null;
            residualBefore = "";
            residualAfter = "";
            bottleneckCalculation = "";
            bfsQueue.clear();
            boolean[] seen = new boolean[graph.n()];
            seen[graph.source()] = true;
            bfsQueue.addLast(graph.source());
            add(LINE_BFS, "BFS queue starts with source " + graph.source(),
                    StepEventType.EXECUTE_LINE, List.of(graph.source()), List.of());
            while (!bfsQueue.isEmpty() && !seen[graph.sink()]) {
                int vertex = bfsQueue.removeFirst();
                focusedArc = null;
                add(LINE_BFS, "Dequeue " + vertex + "; inspect its residual arcs",
                        StepEventType.EXECUTE_LINE, List.of(vertex), List.of());
                for (ResidualArc arc : residual.get(vertex)) {
                    focusedArc = arc;
                    residualBefore = Integer.toString(arc.residual);
                    residualAfter = "";
                    add(LINE_BFS, "Residual " + arc.from + "→" + arc.to + " = " + arc.residual
                                    + "; " + (arc.residual <= 0 ? "no capacity: skip"
                                            : seen[arc.to] ? "already reached: skip" : "positive and unseen: discover"),
                            StepEventType.EXECUTE_LINE, List.of(arc.from, arc.to), List.of(arc.edgeIndex));
                    if (arc.residual > 0 && !seen[arc.to]) {
                        seen[arc.to] = true;
                        parentArc[arc.to] = arc;
                        parentVertex[arc.to] = vertex;
                        bfsQueue.addLast(arc.to);
                        add(LINE_BFS, "Set residual parent[" + arc.to + "]=" + vertex + "; enqueue " + arc.to,
                                StepEventType.EXECUTE_LINE, List.of(arc.to), List.of(arc.edgeIndex));
                        if (arc.to == graph.sink()) {
                            break;
                        }
                    }
                }
            }
            focusedArc = null;
            residualBefore = "";
            residualAfter = "";
            if (!seen[graph.sink()]) {
                pathVertices.add(graph.source());
                return false;
            }

            for (int vertex = graph.sink(); vertex != -1; vertex = parentVertex[vertex]) {
                pathVertices.add(vertex);
                if (vertex != graph.source()) {
                    pathArcs.add(parentArc[vertex]);
                }
            }
            Collections.reverse(pathVertices);
            Collections.reverse(pathArcs);
            return true;
        }

        private void computeBottleneck() {
            bottleneck = Integer.MAX_VALUE;
            java.util.StringJoiner capacities = new java.util.StringJoiner(", ", "min(", ")");
            for (ResidualArc arc : pathArcs) {
                bottleneck = Math.min(bottleneck, arc.residual);
                capacities.add(Integer.toString(arc.residual));
            }
            bottleneckCalculation = capacities + " = " + bottleneck;
            if (bottleneck <= 0 || bottleneck == Integer.MAX_VALUE) {
                throw new IllegalStateException("augmenting path must have a positive bottleneck");
            }
        }

        private void augment() {
            for (ResidualArc arc : pathArcs) {
                focusedArc = arc;
                ResidualArc reverse = residual.get(arc.to).get(arc.reverseIndex);
                int oldResidual = arc.residual;
                int oldReverse = reverse.residual;
                int oldFlow = flow[arc.edgeIndex];
                residualBefore = Integer.toString(oldResidual);
                residualAfter = "";
                add(LINE_UPDATE, "Update residual arc " + arc.from + "→" + arc.to
                                + (arc.forward ? " (send more flow)" : " (cancel earlier flow)"),
                        StepEventType.EXECUTE_LINE, List.of(arc.from, arc.to), List.of(arc.edgeIndex));
                arc.residual -= bottleneck;
                reverse.residual += bottleneck;
                flow[arc.edgeIndex] += arc.forward ? bottleneck : -bottleneck;
                StructuralGraphSupport.InputEdge edge = graph.edges().get(arc.edgeIndex);
                if (flow[arc.edgeIndex] < 0 || flow[arc.edgeIndex] > edge.capacity()) {
                    throw new IllegalStateException("residual update exceeded original edge capacity");
                }
                residualAfter = Integer.toString(arc.residual);
                updateLabels();
                add(arc.forward ? LINE_FORWARD : LINE_REVERSE,
                        "Flow " + edge.from() + "→" + edge.to() + ": " + oldFlow
                                + (arc.forward ? " + " : " − ") + bottleneck + " = " + flow[arc.edgeIndex]
                                + "; residual " + oldResidual + "→" + arc.residual
                                + ", reverse " + oldReverse + "→" + reverse.residual,
                        StepEventType.EXECUTE_LINE, List.of(arc.from, arc.to), List.of(arc.edgeIndex));
            }
            focusedArc = null;
            residualBefore = "";
            residualAfter = "";
            int oldTotal = totalFlow;
            totalFlow += bottleneck;
            add(LINE_FLOW, "Path completed: total flow " + oldTotal + " + " + bottleneck + " = " + totalFlow,
                    StepEventType.EXECUTE_LINE, pathVertices, pathEdgeIndexes());
        }

        private void computeMinCut() {
            Arrays.fill(reachable, false);
            Deque<Integer> queue = new ArrayDeque<>();
            reachable[graph.source()] = true;
            queue.addLast(graph.source());
            while (!queue.isEmpty()) {
                int vertex = queue.removeFirst();
                for (ResidualArc arc : residual.get(vertex)) {
                    if (arc.residual > 0 && !reachable[arc.to]) {
                        reachable[arc.to] = true;
                        queue.addLast(arc.to);
                    }
                }
            }
            cutEdges.clear();
            cutCapacity = 0;
            for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
                StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
                if (edge.from() != edge.to() && reachable[edge.from()] && !reachable[edge.to()]
                        && edge.capacity() > 0) {
                    cutEdges.add(edgeIndex);
                    cutCapacity += edge.capacity();
                }
            }
            minCutComputed = true;
            if (cutCapacity != totalFlow) {
                throw new IllegalStateException(
                        "max-flow/min-cut invariant failed: flow=" + totalFlow + " cut=" + cutCapacity);
            }
        }

        private void markTerminalStatuses() {
            for (int vertex = 0; vertex < graph.n(); vertex++) {
                trace.nodeStatus(vertex, reachable[vertex] ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
            }
            boolean[] cut = new boolean[graph.edges().size()];
            for (int edgeIndex : cutEdges) {
                cut[edgeIndex] = true;
            }
            for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
                StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
                SnapshotStatus status = cut[edgeIndex] || flow[edgeIndex] > 0
                        ? SnapshotStatus.DONE
                        : edge.capacity() == 0 ? SnapshotStatus.REJECTED : SnapshotStatus.DEFAULT;
                trace.edgeStatus(edgeIndex, status);
            }
        }

        private void updateLabels() {
            for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
                StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
                trace.edgeLabel(
                        edgeIndex,
                        flowLabel(edge.capacity(), flow[edgeIndex], edge.from() == edge.to()));
            }
        }

        private void add(
                int line,
                String narration,
                StepEventType event,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges) {
            add(line, narration, event, activeVertices, activeEdges, false);
        }

        private void add(
                int line,
                String narration,
                StepEventType event,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                boolean terminal) {
            labelNodes();
            trace.add(
                    line,
                    narration,
                    event,
                    activeVertices,
                    activeEdges,
                    facts(terminal));
        }

        private List<Fact> facts(boolean terminal) {
            SnapshotStatus status = terminal ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
            String minCutValue = minCutComputed ? Integer.toString(cutCapacity) : "?";
            return List.of(
                    StructuralGraphSupport.fact("queue", StructuralGraphSupport.formatVertices(bfsQueue), status),
                    StructuralGraphSupport.fact("residual-edge", focusedArc == null ? "NIL" : "edge-" + focusedArc.edgeIndex, status),
                    StructuralGraphSupport.fact("residual-from", focusedArc == null ? "NIL" : Integer.toString(focusedArc.from), status),
                    StructuralGraphSupport.fact("residual-to", focusedArc == null ? "NIL" : Integer.toString(focusedArc.to), status),
                    StructuralGraphSupport.fact("residual-direction", focusedArc == null ? "NIL" : focusedArc.forward ? "forward" : "reverse", status),
                    StructuralGraphSupport.fact("residual-before", residualBefore, status),
                    StructuralGraphSupport.fact("residual-after", residualAfter, status),
                    StructuralGraphSupport.fact("bottleneck-calculation", bottleneckCalculation, status),
                    StructuralGraphSupport.fact("source", Integer.toString(graph.source()), status),
                    StructuralGraphSupport.fact("sink", Integer.toString(graph.sink()), status),
                    StructuralGraphSupport.fact("flow", formatFlowValues(), status),
                    StructuralGraphSupport.fact("maxFlow", Integer.toString(totalFlow), status),
                    StructuralGraphSupport.fact("path", pathVerticesText(), status),
                    StructuralGraphSupport.fact("pathEdges", pathDescription(), status),
                    StructuralGraphSupport.fact("bottleneck", bottleneck <= 0 ? "-" : Integer.toString(bottleneck), status),
                    StructuralGraphSupport.fact("residual", formatResidual(), status),
                    StructuralGraphSupport.fact("reachable", reachableText(), status),
                    StructuralGraphSupport.fact("minCut", formatCut(), status),
                    StructuralGraphSupport.fact("cutCapacity", minCutValue, status),
                    StructuralGraphSupport.fact(
                            "minCutEqualsFlow",
                            minCutComputed && minCutValue.equals(Integer.toString(totalFlow)) ? "true" : "?",
                            status));
        }

        private String formatFlowValues() {
            StringBuilder result = new StringBuilder("[");
            for (int edgeIndex = 0; edgeIndex < flow.length; edgeIndex++) {
                if (edgeIndex > 0) {
                    result.append(", ");
                }
                StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
                result.append("edge-").append(edgeIndex).append('=').append(flow[edgeIndex])
                        .append('/').append(edge.capacity());
            }
            return result.append(']').toString();
        }

        private String formatResidual() {
            StringBuilder result = new StringBuilder("[");
            for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
                if (edgeIndex > 0) {
                    result.append(", ");
                }
                StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
                int forward = edge.from() == edge.to() ? 0 : edge.capacity() - flow[edgeIndex];
                int reverse = edge.from() == edge.to() ? 0 : flow[edgeIndex];
                result.append("edge-").append(edgeIndex)
                        .append("{forward=").append(forward)
                        .append(", reverse=").append(reverse).append('}');
            }
            return result.append(']').toString();
        }

        private String pathVerticesText() {
            return StructuralGraphSupport.formatVertices(pathVertices);
        }

        private String pathDescription() {
            if (pathArcs.isEmpty()) {
                return "[]";
            }
            StringBuilder result = new StringBuilder("[");
            for (int index = 0; index < pathArcs.size(); index++) {
                if (index > 0) {
                    result.append(", ");
                }
                ResidualArc arc = pathArcs.get(index);
                result.append(arc.from).append(arc.forward ? " -[forward edge-" : " -[reverse edge-")
                        .append(arc.edgeIndex).append("]-> ").append(arc.to);
            }
            return result.append(']').toString();
        }

        private List<Integer> pathEdgeIndexes() {
            List<Integer> indexes = new ArrayList<>(pathArcs.size());
            for (ResidualArc arc : pathArcs) {
                indexes.add(arc.edgeIndex);
            }
            return indexes;
        }

        private String reachableText() {
            List<Integer> vertices = reachableVertices();
            return (vertices.isEmpty() && !reachable[graph.source()])
                    ? "?"
                    : StructuralGraphSupport.formatVertices(vertices);
        }

        private List<Integer> reachableVertices() {
            List<Integer> vertices = new ArrayList<>();
            for (int vertex = 0; vertex < reachable.length; vertex++) {
                if (reachable[vertex]) {
                    vertices.add(vertex);
                }
            }
            return vertices;
        }

        private String formatCut() {
            List<int[]> pairs = new ArrayList<>(cutEdges.size());
            for (int edgeIndex : cutEdges) {
                StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
                pairs.add(new int[] {edge.from(), edge.to()});
            }
            return StructuralGraphSupport.formatPairList(pairs, true);
        }

        private List<Integer> cutEdgeIndexes() {
            return List.copyOf(cutEdges);
        }

        private void labelNodes() {
            for (int vertex = 0; vertex < graph.n(); vertex++) {
                String role = vertex == graph.source() ? " source" : vertex == graph.sink() ? " sink" : "";
                String side = reachable[vertex] ? " source-side" : "";
                trace.nodeLabel(vertex, vertex + role + side);
            }
        }
    }

    private static String flowLabel(int capacity, int flow, boolean selfLoop) {
        if (selfLoop) {
            return "flow=0/" + capacity + "; residual forward=0 reverse=0 (self-loop)";
        }
        return "flow=" + flow + "/" + capacity
                + "; residual forward=" + (capacity - flow)
                + " reverse=" + flow;
    }

    private static SimulationMetadata createMetadata() {
        return new SimulationMetadata(
                TYPE,
                "Maximum Flow (Edmonds-Karp)",
                "O(VE²)",
                "O(V² + E)",
                RendererFamily.GRAPH,
                StructuralGraphSupport.flowDefault(
                        4,
                        new int[][] {{0, 1, 3}, {0, 2, 2}, {1, 2, 1}, {1, 3, 2}, {2, 3, 3}},
                        0,
                        3,
                        true),
                "Input is {n, edges, source, sink, directed}; each edge is an object "
                        + "{from,to,capacity}. Use n in 1..10, at most 24 edges, capacities "
                        + "0..99, distinct source/sink, and directed=true. Parallel edges "
                        + "retain stable IDs and contribute additive capacity; zero-capacity "
                        + "edges are inert and self-loops cannot carry source-to-sink flow. "
                        + "Labels show actual flow plus forward and reverse residual capacity.",
                PSEUDOCODE);
    }
}
