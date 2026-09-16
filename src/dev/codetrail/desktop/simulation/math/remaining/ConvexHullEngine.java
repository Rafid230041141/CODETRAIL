package dev.codetrail.desktop.simulation.math.remaining;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.GraphState;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.NodeCoordinate;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Monotonic-chain convex hull trace with real sort, cross, and pop operations. */
public final class ConvexHullEngine implements SimulationEngine {
    public static final String TYPE = "CONVEX_HULL";
    public static final int MIN_POINTS = 1;
    public static final int MAX_POINTS = 12;
    public static final long MIN_COORDINATE = -1_000_000L;
    public static final long MAX_COORDINATE = 1_000_000L;
    public static final int MAX_TRACE_STEPS = RemainingMathSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_SORT = 2;
    private static final int LINE_DEDUPLICATE = 3;
    private static final int LINE_LOWER = 4;
    private static final int LINE_CANDIDATE = 5;
    private static final int LINE_CROSS = 6;
    private static final int LINE_POP = 7;
    private static final int LINE_PUSH = 8;
    private static final int LINE_UPPER = 9;
    private static final int LINE_EDGES = 10;
    private static final int LINE_RETURN = 11;

    private static final List<String> PSEUDOCODE = List.of(
            "convexHull(points):",
            "    sort points by (x, y)",
            "    remove duplicate coordinates",
            "    lower = empty stack",
            "    for point in sorted points:",
            "        while lower has two points and cross(last two, point) <= 0:",
            "            pop lower",
            "        push point onto lower",
            "    repeat the scan in reverse for upper",
            "    join chains and emit hull edges",
            "    return strict hull vertices");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        List<Point> inputPoints = parsePoints(input);
        Model model = new Model(inputPoints);
        List<SimulationStep> steps = new ArrayList<>();

        add(steps, model, 0,
                "Initialize the convex-hull trace for " + inputPoints.size() + " point(s)",
                StepEventType.INITIALIZE);
        model.phase = "method";
        add(steps, model, LINE_METHOD,
                "Build a strict convex hull with the monotonic-chain scan",
                StepEventType.EXECUTE_LINE);

        model.phase = "sort";
        model.sorted = new ArrayList<>(inputPoints);
        model.sorted.sort(Comparator
                .comparingLong((Point point) -> point.x)
                .thenComparingLong(point -> point.y)
                .thenComparingInt(point -> point.index));
        add(steps, model, LINE_SORT,
                "Sort points lexicographically by x, then y",
                StepEventType.EXECUTE_LINE);

        model.phase = "deduplicate";
        Point previous = null;
        for (Point point : model.sorted) {
            if (previous != null && previous.x == point.x && previous.y == point.y) {
                model.duplicate[point.index] = true;
                model.statuses[point.index] = SnapshotStatus.REJECTED;
                model.operation = "duplicate " + point.id();
                add(steps, model, LINE_DEDUPLICATE,
                        "Discard duplicate coordinate " + point.id() + " from the geometric scan",
                        StepEventType.EXECUTE_LINE);
            } else {
                model.unique.add(point);
                previous = point;
                add(steps, model, LINE_DEDUPLICATE,
                        "Retain unique coordinate " + point.id(),
                        StepEventType.EXECUTE_LINE);
            }
        }
        if (model.unique.isEmpty()) {
            throw new IllegalStateException("convex-hull deduplication removed every point");
        }

        model.phase = "lower";
        model.chain.clear();
        for (Point point : model.unique) {
            processCandidate(model, steps, point, false);
        }
        model.lowerSize = model.chain.size();
        model.lower = new ArrayList<>(model.chain);

        model.phase = "upper";
        if (model.unique.size() > 1) {
            for (int index = model.unique.size() - 2; index >= 0; index--) {
                processCandidate(model, steps, model.unique.get(index), true);
            }
        }

        model.phase = "join";
        model.lastCross = null;
        model.crossEquation = "";
        model.orientationPoints = "";
        if (model.chain.size() > 1) {
            model.chain.remove(model.chain.size() - 1);
        }
        model.hull = new ArrayList<>(model.chain);
        Set<Integer> hullIndexes = new HashSet<>();
        for (Point point : model.hull) {
            hullIndexes.add(point.index);
        }
        for (Point point : model.inputPoints) {
            model.statuses[point.index] = model.duplicate[point.index]
                    ? SnapshotStatus.REJECTED
                    : hullIndexes.contains(point.index) ? SnapshotStatus.DONE : SnapshotStatus.REJECTED;
        }
        model.edges = buildEdges(model.hull);
        model.currentIndex = -1;
        model.operation = "joined lower and upper chains";
        add(steps, model, LINE_EDGES,
                "Join the chains and create one edge for each strict hull side",
                StepEventType.EXECUTE_LINE);

        model.phase = "return";
        add(steps, model, LINE_RETURN,
                "Return hull vertices, supporting edges, and point classifications",
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(steps, model, 0,
                "Complete convex hull with " + model.hull.size() + " strict vertex/vertices",
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private static void processCandidate(
            Model model,
            List<SimulationStep> steps,
            Point point,
            boolean upper) {
        if (model.currentIndex >= 0 && !model.duplicate[model.currentIndex]) {
            if (model.statuses[model.currentIndex] == SnapshotStatus.ACTIVE) {
                model.statuses[model.currentIndex] = SnapshotStatus.DONE;
            }
        }
        model.lastCross = null;
        model.crossEquation = "";
        model.orientationPoints = "";
        model.currentIndex = point.index;
        model.statuses[point.index] = SnapshotStatus.ACTIVE;
        model.operation = (upper ? "upper" : "lower") + " candidate " + point.id();
        add(steps, model, upper ? LINE_UPPER : LINE_CANDIDATE,
                "Consider " + point.id() + " for the " + (upper ? "upper" : "lower") + " chain",
                StepEventType.EXECUTE_LINE);

        while (model.chain.size() >= 2
                && (!upper || model.chain.size() > model.lowerSize)) {
            Point a = model.chain.get(model.chain.size() - 2);
            Point b = model.chain.get(model.chain.size() - 1);
            model.lastCross = cross(a, b, point);
            model.orientationPoints = a.id() + ", " + b.id() + ", " + point.id();
            model.crossEquation = "(" + b.x + " − " + a.x + ") × (" + point.y + " − " + a.y
                    + ") − (" + b.y + " − " + a.y + ") × (" + point.x + " − " + a.x + ") = " + model.lastCross;
            model.operation = "cross(" + a.id() + "," + b.id() + "," + point.id() + ")";
            add(steps, model, LINE_CROSS,
                    "cross(" + a.id() + ", " + b.id() + ", " + point.id() + ") = " + model.lastCross
                            + (model.lastCross > 0 ? "; counterclockwise, keep the corner"
                                    : model.lastCross < 0 ? "; clockwise, remove " + b.id() : "; collinear, remove middle point " + b.id()),
                    StepEventType.EXECUTE_LINE);
            if (model.lastCross <= 0L) {
                Point removed = model.chain.remove(model.chain.size() - 1);
                if (!model.duplicate[removed.index]) {
                    model.statuses[removed.index] = SnapshotStatus.REJECTED;
                }
                model.operation = "pop " + removed.id();
                add(steps, model, LINE_POP,
                        "Pop a clockwise or middle-collinear point from the strict chain",
                        StepEventType.EXECUTE_LINE);
            } else {
                break;
            }
        }
        model.chain.add(point);
        model.statuses[point.index] = SnapshotStatus.ACTIVE;
        model.operation = "push " + point.id();
        add(steps, model, LINE_PUSH,
                "Push the candidate onto the current chain",
                StepEventType.EXECUTE_LINE);
    }

    private static void add(
            List<SimulationStep> steps,
            Model model,
            int line,
            String narration,
            StepEventType eventType) {
        if (steps.size() >= MAX_TRACE_STEPS) {
            throw new IllegalStateException(TYPE + " trace exceeded bounded step limit");
        }
        List<Node> nodes = nodes(model);
        List<Edge> edges = model.phase.equals("lower") || model.phase.equals("upper")
                ? buildChainEdges(model) : model.edges;
        Set<String> activeNodes = new HashSet<>();
        for (Point point : model.chain) {
            activeNodes.add(nodeId(point.index));
        }
        if (model.currentIndex >= 0) {
            activeNodes.add(nodeId(model.currentIndex));
        }
        Set<String> activeEdges = edgeIds(edges);
        steps.add(RemainingMathSupport.graphStep(
                nodes,
                edges,
                coordinates(model.inputPoints),
                facts(model),
                activeNodes,
                activeEdges,
                line,
                narration,
                eventType));
    }

    private static List<Node> nodes(Model model) {
        List<Node> nodes = new ArrayList<>(model.inputPoints.size());
        for (Point point : model.inputPoints) {
            nodes.add(new Node(nodeId(point.index), "P" + point.index, model.statuses[point.index]));
        }
        return List.copyOf(nodes);
    }

    private static Map<String, NodeCoordinate> coordinates(List<Point> points) {
        java.util.LinkedHashMap<String, NodeCoordinate> coordinates = new java.util.LinkedHashMap<>();
        for (Point point : points) {
            coordinates.put(nodeId(point.index), new NodeCoordinate(point.x, point.y));
        }
        return Map.copyOf(coordinates);
    }

    private static List<Edge> buildChainEdges(Model model) {
        List<Edge> edges = new ArrayList<>();
        for (int index = 1; index < model.chain.size(); index++) {
            Point from = model.chain.get(index - 1);
            Point to = model.chain.get(index);
            edges.add(new Edge("chain-edge-" + index, nodeId(from.index), nodeId(to.index),
                    SnapshotStatus.ACTIVE));
        }
        if (model.currentIndex >= 0 && !model.chain.isEmpty()
                && model.chain.get(model.chain.size() - 1).index != model.currentIndex) {
            Point from = model.chain.get(model.chain.size() - 1);
            edges.add(new Edge("candidate-edge", nodeId(from.index), nodeId(model.currentIndex),
                    SnapshotStatus.ACTIVE, "test"));
        }
        return List.copyOf(edges);
    }

    private static List<Edge> buildEdges(List<Point> hull) {
        if (hull.size() <= 1) {
            return List.of();
        }
        List<Edge> edges = new ArrayList<>();
        int edgeCount = hull.size() == 2 ? 1 : hull.size();
        for (int index = 0; index < edgeCount; index++) {
            Point from = hull.get(index);
            Point to = hull.get((index + 1) % hull.size());
            edges.add(new Edge(
                    "hull-edge-" + index,
                    nodeId(from.index),
                    nodeId(to.index),
                    SnapshotStatus.DONE,
                    "boundary"));
        }
        return List.copyOf(edges);
    }

    private static Set<String> edgeIds(List<Edge> edges) {
        Set<String> result = new HashSet<>();
        for (Edge edge : edges) {
            result.add(edge.id());
        }
        return Set.copyOf(result);
    }

    private static List<Fact> facts(Model model) {
        SnapshotStatus status = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        List<String> duplicateIds = new ArrayList<>();
        for (Point point : model.inputPoints) {
            if (model.duplicate[point.index]) {
                duplicateIds.add(point.id());
            }
        }
        List<Integer> hullIndexes = new ArrayList<>();
        for (Point point : model.hull) {
            hullIndexes.add(point.index);
        }
        List<String> hullEdges = new ArrayList<>();
        for (Edge edge : model.edges == null ? List.<Edge>of() : model.edges) {
            hullEdges.add(edge.fromNodeId() + "->" + edge.toNodeId());
        }
        return List.of(
                RemainingMathSupport.fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("orientation-points", model.orientationPoints, status),
                RemainingMathSupport.fact("cross-equation", model.crossEquation, status),
                RemainingMathSupport.fact("orientation", model.lastCross == null ? ""
                        : model.lastCross > 0 ? "counterclockwise: keep"
                        : model.lastCross < 0 ? "clockwise: pop" : "collinear: pop", status),
                RemainingMathSupport.fact("phase", model.phase, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("point-count", Integer.toString(model.inputPoints.size()), SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("unique-count", Integer.toString(model.unique.size()), SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("sorted", formatPoints(model.sorted), status),
                RemainingMathSupport.fact("unique", formatPoints(model.unique), status),
                RemainingMathSupport.fact("duplicates", RemainingMathSupport.formatStringList(duplicateIds), status),
                RemainingMathSupport.fact("chain", formatPoints(model.chain), status),
                RemainingMathSupport.fact("lower-chain", formatPoints(model.lower), status),
                RemainingMathSupport.fact("hull", RemainingMathSupport.formatIntList(hullIndexes), status),
                RemainingMathSupport.fact("hull-indices", RemainingMathSupport.formatIntList(hullIndexes), status),
                RemainingMathSupport.fact("hull-points", formatPoints(model.hull), status),
                RemainingMathSupport.fact("hull-size", Integer.toString(model.hull.size()), status),
                RemainingMathSupport.fact("hull-edges", RemainingMathSupport.formatStringList(hullEdges), status),
                RemainingMathSupport.fact("last-cross", model.lastCross == null ? "-" : model.lastCross.toString(),
                        model.lastCross == null ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                RemainingMathSupport.fact("operation", model.operation, status),
                RemainingMathSupport.fact(
                        "invariant",
                        "every retained hull edge supports all unique points on one side",
                        status));
    }

    private static String formatPoints(List<Point> points) {
        List<String> values = new ArrayList<>();
        for (Point point : points) {
            values.add(point.id() + "=(" + point.x + "," + point.y + ")");
        }
        return RemainingMathSupport.formatStringList(values);
    }

    private static long cross(Point a, Point b, Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static List<Point> parsePoints(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(TYPE + " input must be a JSON object");
        }
        JsonNode pointsNode = input.get("points");
        if (pointsNode == null || !pointsNode.isArray()) {
            throw new IllegalArgumentException(TYPE + " points must be an array");
        }
        if (pointsNode.size() < MIN_POINTS || pointsNode.size() > MAX_POINTS) {
            throw new IllegalArgumentException(TYPE + " points must contain " + MIN_POINTS + ".." + MAX_POINTS + " entries");
        }
        List<Point> points = new ArrayList<>(pointsNode.size());
        for (int index = 0; index < pointsNode.size(); index++) {
            JsonNode pair = pointsNode.get(index);
            if (pair == null || !pair.isArray() || pair.size() != 2) {
                throw new IllegalArgumentException(TYPE + " point " + index + " must be an integer pair [x,y]");
            }
            points.add(new Point(
                    index,
                    readCoordinate(pair.get(0), index, "x"),
                    readCoordinate(pair.get(1), index, "y")));
        }
        return List.copyOf(points);
    }

    private static long readCoordinate(JsonNode node, int index, String axis) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToLong()) {
            throw new IllegalArgumentException(TYPE + " point " + index + " " + axis + " must be an integer");
        }
        long value = node.longValue();
        if (value < MIN_COORDINATE || value > MAX_COORDINATE) {
            throw new IllegalArgumentException(TYPE + " coordinates must be in the inclusive range "
                    + MIN_COORDINATE + ".." + MAX_COORDINATE);
        }
        return value;
    }

    private static String nodeId(int index) {
        return "point-" + index;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode points = defaultInput.putArray("points");
        int[][] defaults = {{0, 0}, {2, 0}, {2, 2}, {0, 2}, {1, 1}, {3, 1}};
        for (int[] point : defaults) {
            ArrayNode pair = points.addArray();
            pair.add(point[0]);
            pair.add(point[1]);
        }
        return new SimulationMetadata(
                TYPE,
                "Convex Hull",
                "O(n log n)",
                "O(n)",
                RendererFamily.GRAPH,
                defaultInput,
                "Enter JSON as {\"points\":[[0,0],[2,0],[2,2],[0,2],[1,1],[3,1]]}; use "
                        + MIN_POINTS + ".." + MAX_POINTS + " integer points with coordinates in "
                        + MIN_COORDINATE + ".." + MAX_COORDINATE + ". Duplicate points are retained visually but deduplicated geometrically.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final List<Point> inputPoints;
        private final SnapshotStatus[] statuses;
        private final boolean[] duplicate;
        private final List<Point> unique = new ArrayList<>();
        private final List<Point> chain = new ArrayList<>();
        private List<Point> sorted = List.of();
        private List<Point> lower = List.of();
        private List<Point> hull = List.of();
        private List<Edge> edges = List.of();
        private int lowerSize;
        private int currentIndex = -1;
        private Long lastCross;
        private String crossEquation = "";
        private String orientationPoints = "";
        private String phase = "initialize";
        private String operation = "none";

        private Model(List<Point> inputPoints) {
            this.inputPoints = inputPoints;
            this.statuses = new SnapshotStatus[inputPoints.size()];
            java.util.Arrays.fill(this.statuses, SnapshotStatus.DEFAULT);
            this.duplicate = new boolean[inputPoints.size()];
        }
    }

    private record Point(int index, long x, long y) {
        private String id() {
            return "P" + index;
        }
    }
}
