package dev.codetrail.desktop.simulation.structures.linear;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.ArrayState;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.LinkedState;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Small bounded parsing, formatting, and snapshot helpers for linear structures. */
final class LinearStructureSupport {
    static final int MAX_ABS_VALUE = 999;
    static final int MAX_OPERATIONS = 20;
    static final int MAX_TRACE_STEPS = 2048;

    private LinearStructureSupport() {
    }

    static ObjectNode objectInput(JsonNode input, String type) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        return (ObjectNode) input;
    }

    static int integer(JsonNode node, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        return node.intValue();
    }

    static int boundedValue(JsonNode node, String field) {
        int value = integer(node, field);
        if (Math.abs((long) value) > MAX_ABS_VALUE) {
            throw new IllegalArgumentException(field + " must have absolute value at most " + MAX_ABS_VALUE);
        }
        return value;
    }

    static String text(JsonNode node, String field) {
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw new IllegalArgumentException(field + " must be nonblank text");
        }
        return node.textValue();
    }

    static boolean optionalBoolean(ObjectNode input, String field, boolean defaultValue) {
        JsonNode node = input.get(field);
        if (node == null) {
            return defaultValue;
        }
        if (!node.isBoolean()) {
            throw new IllegalArgumentException(field + " must be boolean when present");
        }
        return node.booleanValue();
    }

    static int[] values(ObjectNode input, String field, String type, int maxLength) {
        JsonNode valuesNode = input.get(field);
        if (valuesNode == null || !valuesNode.isArray()) {
            throw new IllegalArgumentException(type + " " + field + " must be a JSON array");
        }
        if (valuesNode.size() > maxLength) {
            throw new IllegalArgumentException(
                    type + " " + field + " length must be at most " + maxLength);
        }
        int[] values = new int[valuesNode.size()];
        for (int index = 0; index < valuesNode.size(); index++) {
            values[index] = boundedValue(valuesNode.get(index), type + " " + field + "[" + index + "]");
        }
        return values;
    }

    static void exactFields(JsonNode node, Set<String> allowed, String context) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(context + " must be an object");
        }
        java.util.Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException(context + " has unsupported field: " + field);
            }
        }
    }

    static void requireIndex(int index, int minimum, int maximum, String field) {
        if (index < minimum || index > maximum) {
            throw new IllegalArgumentException(
                    field + " must be in the inclusive range " + minimum + ".." + maximum);
        }
    }

    static String integerText(Integer value) {
        return value == null ? "∅" : Integer.toString(value);
    }

    static String formatValues(Iterable<Integer> values) {
        StringBuilder result = new StringBuilder("[");
        boolean first = true;
        for (Integer value : values) {
            if (!first) {
                result.append(", ");
            }
            result.append(integerText(value));
            first = false;
        }
        return result.append(']').toString();
    }

    static Fact fact(String key, String value, SnapshotStatus status) {
        return new Fact(key, value, status);
    }

    /** Mutable array-backed trace facade. Every add() copies the current cells. */
    static final class ArrayTrace {
        final List<Integer> cells;
        final List<SnapshotStatus> statuses;
        private final List<SimulationStep> steps = new ArrayList<>();

        ArrayTrace(int[] initial) {
            cells = new ArrayList<>(initial.length);
            for (int value : initial) {
                cells.add(value);
            }
            statuses = new ArrayList<>(initial.length);
            for (int ignored : initial) {
                statuses.add(SnapshotStatus.DEFAULT);
            }
        }

        void appendEmpty() {
            cells.add(null);
            statuses.add(SnapshotStatus.DEFAULT);
        }

        void removeLast() {
            if (cells.isEmpty()) {
                throw new IllegalStateException("cannot remove the last array cell from an empty array");
            }
            cells.remove(cells.size() - 1);
            statuses.remove(statuses.size() - 1);
        }

        void beginOperation() {
            for (int index = 0; index < statuses.size(); index++) {
                statuses.set(index, SnapshotStatus.DEFAULT);
            }
        }

        void status(int index, SnapshotStatus status) {
            if (index < 0 || index >= statuses.size()) {
                throw new IllegalArgumentException("array trace index out of range: " + index);
            }
            statuses.set(index, Objects.requireNonNull(status, "status"));
        }

        void statuses(SnapshotStatus status) {
            for (int index = 0; index < statuses.size(); index++) {
                statuses.set(index, status);
            }
        }

        int safeFocus(int requestedIndex) {
            return cells.isEmpty() ? -1 : Math.min(requestedIndex, cells.size() - 1);
        }

        void add(
                int focusIndex,
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("linear structure trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            if (narration == null || narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(facts, "facts");
            List<TypedCell> cellsSnapshot = new ArrayList<>(cells.size());
            for (int index = 0; index < cells.size(); index++) {
                cellsSnapshot.add(new TypedCell(
                        "index " + index,
                        integerText(cells.get(index)),
                        statuses.get(index)));
            }
            ArrayState state = new ArrayState(cellsSnapshot, cellsSnapshot.isEmpty() ? -1 : focusIndex, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.of(), Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        List<SimulationStep> steps() {
            return List.copyOf(steps);
        }
    }

    /** Mutable node model used to expose actual pointer rewiring. */
    static final class LinkNode {
        final String id;
        final int value;
        String nextId;
        SnapshotStatus status = SnapshotStatus.DEFAULT;

        LinkNode(String id, int value, String nextId) {
            this.id = Objects.requireNonNull(id, "id");
            this.value = value;
            this.nextId = nextId;
        }
    }

    /** Mutable linked-state trace facade with explicit one-outgoing-edge links. */
    static final class LinkedTrace {
        final List<LinkNode> nodes = new ArrayList<>();
        String headId;
        private final List<SimulationStep> steps = new ArrayList<>();

        LinkedTrace(int[] initial) {
            for (int index = 0; index < initial.length; index++) {
                nodes.add(new LinkNode(nodeId(index), initial[index], index + 1 < initial.length ? nodeId(index + 1) : null));
            }
            headId = initial.length == 0 ? null : nodeId(0);
        }

        static String nodeId(int serial) {
            return "node-" + serial;
        }

        static String edgeId(String nodeId) {
            return "link-" + nodeId;
        }

        LinkNode node(String id) {
            for (LinkNode node : nodes) {
                if (node.id.equals(id)) {
                    return node;
                }
            }
            throw new IllegalArgumentException("unknown linked node: " + id);
        }

        int indexOf(String id) {
            for (int index = 0; index < nodes.size(); index++) {
                if (nodes.get(index).id.equals(id)) {
                    return index;
                }
            }
            return -1;
        }

        void beginOperation() {
            for (LinkNode node : nodes) {
                node.status = SnapshotStatus.DEFAULT;
            }
        }

        void status(String id, SnapshotStatus status) {
            node(id).status = Objects.requireNonNull(status, "status");
        }

        void allDone() {
            for (LinkNode node : nodes) {
                node.status = SnapshotStatus.DONE;
            }
        }

        void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Set<String> activeNodeIds,
                Set<String> activeEdgeIds,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("linear structure trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            if (narration == null || narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(activeNodeIds, "activeNodeIds");
            Objects.requireNonNull(activeEdgeIds, "activeEdgeIds");
            Objects.requireNonNull(facts, "facts");
            List<Node> nodeSnapshot = new ArrayList<>(nodes.size());
            Set<String> knownNodes = new HashSet<>();
            for (LinkNode node : nodes) {
                nodeSnapshot.add(new Node(node.id, Integer.toString(node.value), node.status));
                knownNodes.add(node.id);
            }
            Set<String> activeNodes = new LinkedHashSet<>();
            for (String id : activeNodeIds) {
                if (!knownNodes.contains(id)) {
                    throw new IllegalArgumentException("active linked node is not visible: " + id);
                }
                activeNodes.add(id);
            }
            List<Edge> edgeSnapshot = new ArrayList<>();
            Set<String> knownEdges = new HashSet<>();
            for (LinkNode node : nodes) {
                if (node.nextId == null) {
                    continue;
                }
                if (!knownNodes.contains(node.nextId)) {
                    throw new IllegalArgumentException("linked next pointer is not visible: " + node.nextId);
                }
                String edgeId = edgeId(node.id);
                edgeSnapshot.add(new Edge(edgeId, node.id, node.nextId, node.status));
                knownEdges.add(edgeId);
            }
            Set<String> activeEdges = new LinkedHashSet<>();
            for (String id : activeEdgeIds) {
                if (!knownEdges.contains(id)) {
                    throw new IllegalArgumentException("active linked edge is not visible: " + id);
                }
                activeEdges.add(id);
            }
            LinkedState state = new LinkedState(nodeSnapshot, edgeSnapshot, headId, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.copyOf(activeNodes), Set.copyOf(activeEdges)),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Set<String> activeNodeIds,
                List<Fact> facts) {
            add(highlightedLine, narration, eventType, activeNodeIds, Set.of(), facts);
        }

        List<SimulationStep> steps() {
            return List.copyOf(steps);
        }
    }

    static String linkSummary(LinkedTrace trace) {
        StringBuilder result = new StringBuilder();
        String current = trace.headId;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            if (result.length() > 0) {
                result.append(" -> ");
            }
            LinkNode node = trace.node(current);
            result.append(node.id).append(':').append(node.value);
            current = node.nextId;
        }
        if (current != null) {
            result.append(" -> cycle");
        }
        return result.length() == 0 ? "[]" : result.toString();
    }

    static String headLabel(LinkedTrace trace) {
        return trace.headId == null ? "EMPTY" : trace.headId;
    }

    static Set<String> activeLinks(LinkedTrace trace, Set<String> activeNodes) {
        Set<String> activeEdges = new LinkedHashSet<>();
        for (LinkNode node : trace.nodes) {
            if (node.nextId != null && activeNodes.contains(node.id)) {
                activeEdges.add(LinkedTrace.edgeId(node.id));
            }
        }
        return activeEdges;
    }

    static String formatPhysicalSlots(Integer[] slots) {
        return formatValues(Arrays.asList(slots));
    }

    static String formatDeque(Iterable<Integer> values) {
        return formatValues(values);
    }
}
