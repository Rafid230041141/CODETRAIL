package dev.codetrail.desktop.simulation.paradigms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TreeState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Recursive range minimum/maximum with an explicit immutable call tree. */
public final class DivideConquerEngine implements SimulationEngine {
    public static final String TYPE = "DIVIDE_AND_CONQUER";
    public static final String ALGORITHM = "MIN_MAX";
    public static final int MIN_ARRAY_LENGTH = 1;
    public static final int MAX_ARRAY_LENGTH = 16;
    public static final int MAX_ABS_VALUE = 999;
    public static final int MAX_TRACE_STEPS = 4096;

    private static final int LINE_METHOD = 1;
    private static final int LINE_BASE = 2;
    private static final int LINE_RETURN_LEAF = 3;
    private static final int LINE_SPLIT = 4;
    private static final int LINE_LEFT = 5;
    private static final int LINE_RIGHT = 6;
    private static final int LINE_COMBINE = 7;
    private static final int LINE_RETURN = 8;

    private static final int[] DEFAULT_ARRAY = {7, 2, 9, 4, 1, 8};
    private static final List<String> PSEUDOCODE = List.of(
            "rangeMinMax(a, left, right):",
            "    if left == right:",
            "        return (a[left], a[left])",
            "    middle = left + floor((right - left) / 2)",
            "    leftPair = rangeMinMax(a, left, middle)",
            "    rightPair = rangeMinMax(a, middle + 1, right)",
            "    pair = (min(leftPair.min, rightPair.min), max(leftPair.max, rightPair.max))",
            "    return pair");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        ParadigmSupport.requireObject(input, TYPE);
        ParadigmSupport.requireAlgorithm(input, TYPE, ALGORITHM);
        int[] values = ParadigmSupport.intArray(
                input, TYPE, "array", MAX_ARRAY_LENGTH, MAX_ABS_VALUE);
        Model model = new Model(values);
        List<SimulationStep> steps = new ArrayList<>();

        String rootId = model.nodeId(0, values.length - 1);
        model.ensureNode(0, values.length - 1, null, null);
        model.phase = "initialize";
        ParadigmSupport.add(
                steps,
                model.state(),
                model.activeNodeIds(),
                model.activeEdgeIds(),
                0,
                "Initialize the recursive range tree for " + values.length + " value(s)",
                StepEventType.INITIALIZE,
                TYPE,
                MAX_TRACE_STEPS);

        model.activeNodeId = rootId;
        model.node(rootId).status = SnapshotStatus.ACTIVE;
        model.phase = "enter";
        add(steps, model, LINE_METHOD, "Solve the inclusive range [0," + (values.length - 1) + "]");
        Extremes result = solve(model, 0, values.length - 1, null, steps);

        model.activeNodeId = null;
        model.phase = "complete";
        model.minimum = result.minimum;
        model.maximum = result.maximum;
        add(
                steps,
                model,
                0,
                "Complete: minimum = " + result.minimum + ", maximum = " + result.maximum,
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private Extremes solve(
            Model model,
            int left,
            int right,
            String parentId,
            List<SimulationStep> steps) {
        String id = model.nodeId(left, right);
        model.ensureNode(left, right, parentId, null);
        model.activeNodeId = id;
        model.currentRange = "[" + left + "," + right + "]";
        model.combine = "none";
        model.node(id).status = SnapshotStatus.ACTIVE;
        model.phase = "enter";
        add(steps, model, LINE_METHOD, "Enter range [" + left + "," + right + "]");
        add(
                steps,
                model,
                LINE_BASE,
                "Check whether range [" + left + "," + right + "] has one value",
                StepEventType.EXECUTE_LINE);

        if (left == right) {
            int value = model.values[left];
            model.node(id).label = model.rangeLabel(left, right, value, value);
            model.node(id).status = SnapshotStatus.DONE;
            model.activeNodeId = id;
            model.phase = "leaf-return";
            model.minimum = value;
            model.maximum = value;
            add(
                    steps,
                    model,
                    LINE_RETURN_LEAF,
                    "a[" + left + "] = " + value + ": return (" + value + ", " + value + ")",
                    StepEventType.EXECUTE_LINE);
            return new Extremes(value, value);
        }

        int middle = left + (right - left) / 2;
        model.phase = "split";
        model.activeNodeId = id;
        String leftId = model.nodeId(left, middle);
        String rightId = model.nodeId(middle + 1, right);
        model.ensureNode(left, middle, id, "left");
        model.ensureNode(middle + 1, right, id, "right");
        add(
                steps,
                model,
                LINE_SPLIT,
                "Split [" + left + "," + right + "] at " + middle + " into [" + left + ","
                        + middle + "] and [" + (middle + 1) + "," + right + "]",
                StepEventType.EXECUTE_LINE);

        model.edge(id, leftId).status = SnapshotStatus.ACTIVE;
        model.activeNodeId = leftId;
        model.currentRange = "[" + left + "," + middle + "]";
        model.combine = "none";
        model.phase = "left";
        add(
                steps,
                model,
                LINE_LEFT,
                "Recurse into left range [" + left + "," + middle + "]",
                StepEventType.EXECUTE_LINE);
        Extremes leftResult = solve(model, left, middle, id, steps);
        model.edge(id, leftId).status = SnapshotStatus.DONE;
        model.node(id).status = SnapshotStatus.ACTIVE;
        model.activeNodeId = id;
        model.minimum = leftResult.minimum;
        model.maximum = leftResult.maximum;

        model.edge(id, rightId).status = SnapshotStatus.ACTIVE;
        model.activeNodeId = rightId;
        model.currentRange = "[" + (middle + 1) + "," + right + "]";
        model.combine = "none";
        model.phase = "right";
        add(
                steps,
                model,
                LINE_RIGHT,
                "Recurse into right range [" + (middle + 1) + "," + right + "]",
                StepEventType.EXECUTE_LINE);
        Extremes rightResult = solve(model, middle + 1, right, id, steps);
        model.edge(id, rightId).status = SnapshotStatus.DONE;

        int minimum = Math.min(leftResult.minimum, rightResult.minimum);
        int maximum = Math.max(leftResult.maximum, rightResult.maximum);
        model.node(id).label = model.rangeLabel(left, right, minimum, maximum);
        model.node(id).status = SnapshotStatus.ACTIVE;
        model.activeNodeId = id;
        model.currentRange = "[" + left + "," + right + "]";
        model.combine = "min(" + leftResult.minimum + ", " + rightResult.minimum + ") = " + minimum
                + "; max(" + leftResult.maximum + ", " + rightResult.maximum + ") = " + maximum;
        model.minimum = minimum;
        model.maximum = maximum;
        model.phase = "combine";
        add(
                steps,
                model,
                LINE_COMBINE,
                model.combine,
                StepEventType.EXECUTE_LINE);
        model.phase = "return";
        model.node(id).status = SnapshotStatus.DONE;
        add(
                steps,
                model,
                LINE_RETURN,
                "Return pair for range [" + left + "," + right + "]",
                StepEventType.EXECUTE_LINE);
        return new Extremes(minimum, maximum);
    }

    private static void add(
            List<SimulationStep> steps,
            Model model,
            int line,
            String narration) {
        add(steps, model, line, narration, StepEventType.EXECUTE_LINE);
    }

    private static void add(
            List<SimulationStep> steps,
            Model model,
            int line,
            String narration,
            StepEventType eventType) {
        ParadigmSupport.add(
                steps,
                model.state(),
                model.activeNodeIds(),
                model.activeEdgeIds(),
                line,
                narration,
                eventType,
                TYPE,
                MAX_TRACE_STEPS);
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("algorithm", ALGORITHM);
        ArrayNode values = defaultInput.putArray("array");
        for (int value : DEFAULT_ARRAY) {
            values.add(value);
        }
        return new SimulationMetadata(
                TYPE,
                "Divide and Conquer: Range Min/Max",
                "O(n)",
                "O(log n)",
                RendererFamily.TREE,
                defaultInput,
                "Enter JSON as {\"algorithm\":\"MIN_MAX\",\"array\":[7,2,9,4,1,8]}; array length must be 1.."
                        + MAX_ARRAY_LENGTH + " and every value must have absolute value at most " + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }

    private record Extremes(int minimum, int maximum) {
    }

    private static final class Model {
        private final int[] values;
        private final Map<String, MutableNode> nodes = new LinkedHashMap<>();
        private final List<MutableEdge> edges = new ArrayList<>();
        private String activeNodeId;
        private String phase = "pending";
        private Integer minimum;
        private Integer maximum;
        private String currentRange = "none";
        private String combine = "none";

        private Model(int[] values) {
            this.values = values.clone();
            this.currentRange = "[0," + (values.length - 1) + "]";
        }

        private String nodeId(int left, int right) {
            return "range-" + left + "-" + right;
        }

        private String rangeLabel(int left, int right, int minimum, int maximum) {
            return "[" + left + "," + right + "] = (" + minimum + "," + maximum + ")";
        }

        private MutableNode node(String id) {
            MutableNode node = nodes.get(id);
            if (node == null) {
                throw new IllegalArgumentException("unknown range node: " + id);
            }
            return node;
        }

        private void ensureNode(
                int left,
                int right,
                String parentId,
                String edgeLabel) {
            String id = nodeId(left, right);
            nodes.computeIfAbsent(
                    id,
                    ignored -> new MutableNode(id, left == right
                            ? "a[" + left + "] = " + values[left]
                            : "[" + left + "," + right + "]", SnapshotStatus.DEFAULT));
            if (parentId != null) {
                String edgeId = parentId + "->" + id;
                boolean present = edges.stream().anyMatch(edge -> edge.id.equals(edgeId));
                if (!present) {
                    edges.add(new MutableEdge(edgeId, parentId, id, edgeLabel, SnapshotStatus.DEFAULT));
                }
            }
        }

        private MutableEdge edge(String fromId, String toId) {
            String edgeId = fromId + "->" + toId;
            for (MutableEdge edge : edges) {
                if (edge.id.equals(edgeId)) {
                    return edge;
                }
            }
            throw new IllegalArgumentException("unknown range edge: " + edgeId);
        }

        private Set<String> activeNodeIds() {
            Set<String> active = new HashSet<>();
            for (MutableNode node : nodes.values()) {
                if (node.status == SnapshotStatus.ACTIVE) {
                    active.add(node.id);
                }
            }
            return active;
        }

        private Set<String> activeEdgeIds() {
            Set<String> active = new HashSet<>();
            for (MutableEdge edge : edges) {
                if (edge.status == SnapshotStatus.ACTIVE) {
                    active.add(edge.id);
                }
            }
            return active;
        }

        private TreeState state() {
            List<Node> nodeCopies = new ArrayList<>(nodes.size());
            for (MutableNode node : nodes.values()) {
                nodeCopies.add(new Node(node.id, node.label, node.status));
            }
            List<Edge> edgeCopies = new ArrayList<>(edges.size());
            for (MutableEdge edge : edges) {
                edgeCopies.add(new Edge(edge.id, edge.fromId, edge.toId, edge.status, edge.label));
            }
            List<Fact> facts = List.of(
                    ParadigmSupport.fact("array", ParadigmSupport.formatInts(values), SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("phase", phase, phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("current-range", currentRange, SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("combine", combine, phase.equals("combine") ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("active-range", activeNodeId == null ? "none" : activeNodeId, activeNodeId == null ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("minimum", minimum == null ? "pending" : minimum.toString(), phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("maximum", maximum == null ? "pending" : maximum.toString(), phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT));
            String rootId = nodeId(0, values.length - 1);
            return new TreeState(nodeCopies, edgeCopies, rootId, facts);
        }
    }

    private static final class MutableNode {
        private final String id;
        private String label;
        private SnapshotStatus status;

        private MutableNode(String id, String label, SnapshotStatus status) {
            this.id = id;
            this.label = label;
            this.status = status;
        }
    }

    private static final class MutableEdge {
        private final String id;
        private final String fromId;
        private final String toId;
        private final String label;
        private SnapshotStatus status;

        private MutableEdge(
                String id,
                String fromId,
                String toId,
                String label,
                SnapshotStatus status) {
            this.id = id;
            this.fromId = fromId;
            this.toId = toId;
            this.label = label;
            this.status = status;
        }
    }
}
