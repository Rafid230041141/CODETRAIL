package dev.codetrail.desktop.simulation.structures.linear;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
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
import java.util.Set;

/** Singly linked list trace with explicit predecessor and next-pointer rewiring. */
public final class LinkedListEngine implements SimulationEngine {
    public static final String TYPE = "LINKED_LIST";
    public static final int MAX_VALUES = 16;
    public static final int MAX_NODES = MAX_VALUES;
    public static final int MAX_ARRAY_LENGTH = MAX_VALUES;
    public static final int MAX_ABS_VALUE = LinearStructureSupport.MAX_ABS_VALUE;
    public static final int MAX_OPERATIONS = LinearStructureSupport.MAX_OPERATIONS;
    public static final int MAX_TRACE_STEPS = LinearStructureSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_FIND_PREDECESSOR = 2;
    private static final int LINE_NEW_NEXT = 3;
    private static final int LINE_REWIRE_NEXT = 4;
    private static final int LINE_HEAD = 5;
    private static final int LINE_DELETE = 6;
    private static final int LINE_REVERSE_INIT = 7;
    private static final int LINE_REVERSE_LOOP = 8;
    private static final int LINE_SAVE_NEXT = 9;
    private static final int LINE_REVERSE_LINK = 10;
    private static final int LINE_ADVANCE_PREVIOUS = 11;
    private static final int LINE_ADVANCE_CURRENT = 12;
    private static final int LINE_RETURN_HEAD = 13;

    private static final int DEFAULT_INDEX = 2;
    private static final int DEFAULT_VALUE = 5;
    private static final int[] DEFAULT_VALUES = {4, 7, 2, 9};
    private static final List<String> PSEUDOCODE = List.of(
            "editList(head, operation):",
            "    predecessor = node at index - 1",
            "    new.next = head if index == 0 else predecessor.next",
            "    predecessor.next = new",
            "    if index == 0: head = new",
            "    delete target; reconnect predecessor or update head",
            "    previous = null; current = head",
            "    while current != null:",
            "        next = current.next",
            "        current.next = previous",
            "        previous = current",
            "        current = next",
            "    head = previous");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        LinearStructureSupport.LinkedTrace trace = new LinearStructureSupport.LinkedTrace(request.values());
        int nextSerial = request.values().length;
        trace.add(
                0,
                "Initialize linked list with head " + LinearStructureSupport.headLabel(trace),
                StepEventType.INITIALIZE,
                Set.of(),
                facts("initialize", -1, null, "initialize", trace, SnapshotStatus.ACTIVE));

        for (int operationIndex = 0; operationIndex < request.operations().size(); operationIndex++) {
            Operation operation = request.operations().get(operationIndex);
            trace.beginOperation();
            trace.add(
                    LINE_METHOD,
                    "Start edit " + (operationIndex + 1) + ": " + operation.describe(),
                    StepEventType.EXECUTE_LINE,
                    Set.of(),
                    facts(operation.kind(), operation.index(), operation.value(), "start", trace,
                            SnapshotStatus.ACTIVE));
            if (operation.isInsert()) {
                nextSerial = insert(trace, operation, nextSerial);
            } else if (operation.isDelete()) {
                delete(trace, operation);
            } else {
                reverse(trace);
            }
            trace.allDone();
            trace.add(
                    0,
                    "Edit complete; head " + LinearStructureSupport.headLabel(trace),
                    StepEventType.EXECUTE_LINE,
                    Set.of(),
                    facts(operation.kind(), operation.index(), operation.value(), "complete", trace,
                            SnapshotStatus.DONE));
        }

        trace.allDone();
        trace.add(
                0,
                "Complete: list = " + LinearStructureSupport.linkSummary(trace),
                StepEventType.COMPLETE,
                Set.of(),
                facts("complete", -1, null, "complete", trace, SnapshotStatus.DONE));
        return trace.steps();
    }

    private static int insert(
            LinearStructureSupport.LinkedTrace trace,
            Operation operation,
            int nextSerial) {
        int index = operation.index();
        if (index == 0) {
            String newId = LinearStructureSupport.LinkedTrace.nodeId(nextSerial);
            LinearStructureSupport.LinkNode newNode =
                    new LinearStructureSupport.LinkNode(newId, operation.value(), trace.headId);
            trace.nodes.add(newNode);
            String oldHead = trace.headId;
            trace.status(newId, SnapshotStatus.ACTIVE);
            if (oldHead != null) {
                trace.status(oldHead, SnapshotStatus.ACTIVE);
            }
            trace.add(
                    LINE_NEW_NEXT,
                    "Set new node " + newId + ".next = " + (oldHead == null ? "null" : oldHead),
                    StepEventType.EXECUTE_LINE,
                    activeNodes(newId, oldHead),
                    LinearStructureSupport.activeLinks(trace, activeNodes(newId, oldHead)),
                    facts(operation.kind(), index, operation.value(), "new.next", trace,
                            SnapshotStatus.ACTIVE));
            trace.beginOperation();
            trace.headId = newId;
            trace.status(newId, SnapshotStatus.ACTIVE);
            trace.add(
                    LINE_HEAD,
                    "Set head = " + newId,
                    StepEventType.EXECUTE_LINE,
                    Set.of(newId),
                    LinearStructureSupport.activeLinks(trace, Set.of(newId)),
                    facts(operation.kind(), index, operation.value(), "head", trace,
                            SnapshotStatus.ACTIVE));
            return nextSerial + 1;
        }

        String predecessor = nodeAt(trace, index - 1);
        trace.status(predecessor, SnapshotStatus.ACTIVE);
        trace.add(
                LINE_FIND_PREDECESSOR,
                "Stop at predecessor " + predecessor,
                StepEventType.EXECUTE_LINE,
                Set.of(predecessor),
                LinearStructureSupport.activeLinks(trace, Set.of(predecessor)),
                facts(operation.kind(), index, operation.value(), "predecessor", trace,
                        SnapshotStatus.ACTIVE));
        String oldNext = trace.node(predecessor).nextId;
        String newId = LinearStructureSupport.LinkedTrace.nodeId(nextSerial);
        LinearStructureSupport.LinkNode newNode =
                new LinearStructureSupport.LinkNode(newId, operation.value(), oldNext);
        trace.nodes.add(newNode);
        trace.status(predecessor, SnapshotStatus.ACTIVE);
        trace.status(newId, SnapshotStatus.ACTIVE);
        trace.add(
                LINE_NEW_NEXT,
                "Set " + newId + ".next = " + (oldNext == null ? "null" : oldNext),
                StepEventType.EXECUTE_LINE,
                activeNodes(predecessor, newId),
                LinearStructureSupport.activeLinks(trace, activeNodes(predecessor, newId)),
                facts(operation.kind(), index, operation.value(), "new.next", trace,
                        SnapshotStatus.ACTIVE));

        trace.node(predecessor).nextId = newId;
        trace.status(predecessor, SnapshotStatus.ACTIVE);
        trace.status(newId, SnapshotStatus.ACTIVE);
        trace.add(
                LINE_REWIRE_NEXT,
                "Rewire " + predecessor + ".next from " + (oldNext == null ? "null" : oldNext)
                        + " to " + newId,
                StepEventType.EXECUTE_LINE,
                activeNodes(predecessor, newId),
                LinearStructureSupport.activeLinks(trace, activeNodes(predecessor, newId)),
                facts(operation.kind(), index, operation.value(),
                        predecessor + ".next", trace, SnapshotStatus.ACTIVE));
        return nextSerial + 1;
    }

    private static void delete(LinearStructureSupport.LinkedTrace trace, Operation operation) {
        String target;
        String predecessor = null;
        if (operation.index() == 0) {
            target = trace.headId;
            String next = target == null ? null : trace.node(target).nextId;
            if (target != null) {
                trace.status(target, SnapshotStatus.ACTIVE);
            }
            trace.add(
                    LINE_DELETE,
                    "Remove head " + (target == null ? "null" : target),
                    StepEventType.EXECUTE_LINE,
                    target == null ? Set.of() : Set.of(target),
                    target == null ? Set.of() : LinearStructureSupport.activeLinks(trace, Set.of(target)),
                    facts(operation.kind(), operation.index(), null, "head delete", trace,
                            SnapshotStatus.ACTIVE));
            if (target == null) {
                return;
            }
            removeNode(trace, target);
            trace.headId = next;
            trace.beginOperation();
            if (next != null) trace.status(next, SnapshotStatus.ACTIVE);
            trace.add(LINE_DELETE, "Remove " + target + " and set head = " + pointer(next),
                    StepEventType.EXECUTE_LINE, next == null ? Set.of() : Set.of(next),
                    facts(operation.kind(), operation.index(), null, "head deleted", trace, SnapshotStatus.ACTIVE));
            return;
        }

        predecessor = nodeAt(trace, operation.index() - 1);
        target = trace.node(predecessor).nextId;
        String next = target == null ? null : trace.node(target).nextId;
        trace.status(predecessor, SnapshotStatus.ACTIVE);
        if (target != null) {
            trace.status(target, SnapshotStatus.ACTIVE);
        }
        trace.add(
                LINE_FIND_PREDECESSOR,
                "Stop at predecessor " + predecessor + " before " + target,
                StepEventType.EXECUTE_LINE,
                activeNodes(predecessor, target),
                LinearStructureSupport.activeLinks(trace, activeNodes(predecessor, target)),
                facts(operation.kind(), operation.index(), null, "predecessor", trace,
                        SnapshotStatus.ACTIVE));
        trace.node(predecessor).nextId = next;
        removeNode(trace, target);
        trace.status(predecessor, SnapshotStatus.ACTIVE);
        trace.add(
                LINE_DELETE,
                "Rewire " + predecessor + ".next to " + (next == null ? "null" : next)
                        + " and remove " + target,
                StepEventType.EXECUTE_LINE,
                Set.of(predecessor),
                LinearStructureSupport.activeLinks(trace, Set.of(predecessor)),
                facts(operation.kind(), operation.index(), null, "delete", trace,
                        SnapshotStatus.ACTIVE));
    }

    private static void reverse(LinearStructureSupport.LinkedTrace trace) {
        String previous = null;
        String current = trace.headId;
        String next = null;
        reverseStep(trace, LINE_REVERSE_INIT, "Initialize previous = null and current = head",
                "initialize pointers", previous, current, next);
        while (current != null) {
            reverseStep(trace, LINE_REVERSE_LOOP, "Current is " + current + "; process this node",
                    "check current", previous, current, next);
            LinearStructureSupport.LinkNode currentNode = trace.node(current);
            next = currentNode.nextId;
            reverseStep(trace, LINE_SAVE_NEXT, "Save next = " + pointer(next) + " before changing the link",
                    "save next", previous, current, next);
            currentNode.nextId = previous;
            reverseStep(trace, LINE_REVERSE_LINK,
                    "Reverse " + current + ".next to " + pointer(previous) + "; next still preserves the unprocessed list",
                    "reverse link", previous, current, next);
            previous = current;
            reverseStep(trace, LINE_ADVANCE_PREVIOUS, "Move previous to " + previous + ", the reversed prefix head",
                    "advance previous", previous, current, next);
            current = next;
            reverseStep(trace, LINE_ADVANCE_CURRENT, "Move current to saved next = " + pointer(current),
                    "advance current", previous, current, next);
        }
        trace.headId = previous;
        reorderByHead(trace);
        trace.allDone();
        trace.add(
                LINE_RETURN_HEAD,
                "All links are reversed; now set head = previous = " + pointer(previous),
                StepEventType.EXECUTE_LINE,
                Set.of(),
                reverseFacts(trace, "reversed", previous, current, next, SnapshotStatus.DONE));
    }

    private static void reverseStep(LinearStructureSupport.LinkedTrace trace, int line, String narration,
            String phase, String previous, String current, String next) {
        trace.beginOperation();
        Set<String> active = new HashSet<>(activeNodes(previous, current));
        if (next != null) active.add(next);
        for (String id : active) trace.status(id, SnapshotStatus.ACTIVE);
        trace.add(line, narration, StepEventType.EXECUTE_LINE, active,
                LinearStructureSupport.activeLinks(trace, active),
                reverseFacts(trace, phase, previous, current, next, SnapshotStatus.ACTIVE));
    }

    private static List<Fact> reverseFacts(LinearStructureSupport.LinkedTrace trace, String phase,
            String previous, String current, String next, SnapshotStatus status) {
        List<Fact> result = new ArrayList<>(facts("reverse", -1, null, phase, trace, status));
        result.add(LinearStructureSupport.fact("previous", pointer(previous), status));
        result.add(LinearStructureSupport.fact("current", pointer(current), status));
        result.add(LinearStructureSupport.fact("next", pointer(next), status));
        return List.copyOf(result);
    }

    private static String pointer(String id) {
        return id == null ? "null" : id;
    }

    private static void removeNode(LinearStructureSupport.LinkedTrace trace, String id) {
        for (int index = 0; index < trace.nodes.size(); index++) {
            if (trace.nodes.get(index).id.equals(id)) {
                trace.nodes.remove(index);
                return;
            }
        }
    }

    private static void reorderByHead(LinearStructureSupport.LinkedTrace trace) {
        List<LinearStructureSupport.LinkNode> ordered = new ArrayList<>(trace.nodes.size());
        Set<String> seen = new HashSet<>();
        String current = trace.headId;
        while (current != null && seen.add(current)) {
            LinearStructureSupport.LinkNode node = trace.node(current);
            ordered.add(node);
            current = node.nextId;
        }
        for (LinearStructureSupport.LinkNode node : trace.nodes) {
            if (seen.add(node.id)) {
                ordered.add(node);
            }
        }
        trace.nodes.clear();
        trace.nodes.addAll(ordered);
    }

    private static String nodeAt(LinearStructureSupport.LinkedTrace trace, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("linked list index cannot be negative");
        }
        String current = trace.headId;
        for (int position = 0; position < index; position++) {
            if (current == null) {
                throw new IllegalArgumentException("linked list index is outside the current list");
            }
            current = trace.node(current).nextId;
        }
        if (current == null) {
            throw new IllegalArgumentException("linked list index is outside the current list");
        }
        return current;
    }

    private static Set<String> activeNodes(String first, String second) {
        Set<String> active = new HashSet<>();
        if (first != null) {
            active.add(first);
        }
        if (second != null) {
            active.add(second);
        }
        return Set.copyOf(active);
    }

    private static List<Fact> facts(
            String operation,
            int index,
            Integer value,
            String phase,
            LinearStructureSupport.LinkedTrace trace,
            SnapshotStatus status) {
        return List.of(
                LinearStructureSupport.fact("operation", operation, SnapshotStatus.DEFAULT),
                LinearStructureSupport.fact("index", Integer.toString(index), status),
                LinearStructureSupport.fact("value", LinearStructureSupport.integerText(value), status),
                LinearStructureSupport.fact("head", LinearStructureSupport.headLabel(trace), status),
                LinearStructureSupport.fact("links", LinearStructureSupport.linkSummary(trace), status),
                LinearStructureSupport.fact("phase", phase, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode values = defaultInput.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        defaultInput.put("operation", "insert");
        defaultInput.put("index", DEFAULT_INDEX);
        defaultInput.put("value", DEFAULT_VALUE);
        return new SimulationMetadata(
                TYPE,
                "Linked List",
                "O(n) indexed edit",
                "O(n)",
                RendererFamily.LINKED,
                defaultInput,
                "Enter JSON as {\"values\":[4,7,2,9],\"operation\":\"insert\",\"index\":2,\"value\":5}; values length must be 0.."
                        + MAX_VALUES + ", each value must have absolute value at most " + MAX_ABS_VALUE
                        + ". A delete uses operation=delete and index; reverse uses operation=reverse. For a multi-edit trace use operations with kind insert, delete, or reverse.",
                PSEUDOCODE);
    }

    private record Operation(String kind, int index, Integer value) {
        boolean isInsert() { return kind.equals("insert"); }
        boolean isDelete() { return kind.equals("delete"); }

        String describe() {
            if (isInsert()) {
                return "insert(" + index + ", " + value + ")";
            }
            if (isDelete()) {
                return "delete(" + index + ")";
            }
            return "reverse()";
        }
    }

    private record Request(int[] values, List<Operation> operations) {
        static Request parse(JsonNode input) {
            ObjectNode object = LinearStructureSupport.objectInput(input, TYPE);
            int[] values;
            List<Operation> operations;
            if (object.has("operations")) {
                LinearStructureSupport.exactFields(object, Set.of("values", "operations"), TYPE);
                values = LinearStructureSupport.values(object, "values", TYPE, MAX_VALUES);
                JsonNode operationsNode = object.get("operations");
                if (!operationsNode.isArray()) {
                    throw new IllegalArgumentException(TYPE + " operations must be a JSON array");
                }
                if (operationsNode.size() > MAX_OPERATIONS) {
                    throw new IllegalArgumentException(TYPE + " operations must contain at most " + MAX_OPERATIONS + " entries");
                }
                operations = new ArrayList<>(operationsNode.size());
                int length = values.length;
                for (int operationIndex = 0; operationIndex < operationsNode.size(); operationIndex++) {
                    JsonNode operationNode = operationsNode.get(operationIndex);
                    String context = TYPE + " operation " + operationIndex;
                    LinearStructureSupport.exactFields(operationNode, Set.of("kind", "index", "value"), context);
                    ObjectNode operation = (ObjectNode) operationNode;
                    String kind = LinearStructureSupport.text(operation.get("kind"), context + " kind");
                    if (kind.equals("reverse")) {
                        if (operation.has("index") || operation.has("value")) {
                            throw new IllegalArgumentException(context + " reverse accepts only kind");
                        }
                        operations.add(new Operation(kind, -1, null));
                    } else if (kind.equals("insert")) {
                        int index = LinearStructureSupport.integer(operation.get("index"), context + " index");
                        LinearStructureSupport.requireIndex(index, 0, length, context + " index");
                        if (!operation.has("value")) {
                            throw new IllegalArgumentException(context + " insert requires value");
                        }
                        int value = LinearStructureSupport.boundedValue(operation.get("value"), context + " value");
                        if (length >= MAX_VALUES) {
                            throw new IllegalArgumentException(TYPE + " insertion would exceed " + MAX_VALUES + " nodes");
                        }
                        operations.add(new Operation(kind, index, value));
                        length++;
                    } else if (kind.equals("delete")) {
                        int index = LinearStructureSupport.integer(operation.get("index"), context + " index");
                        LinearStructureSupport.requireIndex(index, 0, length - 1, context + " index");
                        if (operation.has("value")) {
                            throw new IllegalArgumentException(context + " delete does not accept value");
                        }
                        operations.add(new Operation(kind, index, null));
                        length--;
                    } else {
                        throw new IllegalArgumentException(context + " kind must be exactly insert, delete, or reverse");
                    }
                }
                return new Request(values, List.copyOf(operations));
            }

            values = LinearStructureSupport.values(object, "values", TYPE, MAX_VALUES);
            String operation = LinearStructureSupport.text(object.get("operation"), TYPE + " operation");
            if (operation.equals("insert")) {
                LinearStructureSupport.exactFields(object, Set.of("values", "operation", "index", "value"), TYPE);
                int index = LinearStructureSupport.integer(object.get("index"), TYPE + " insert index");
                LinearStructureSupport.requireIndex(index, 0, values.length, TYPE + " insert index");
                int value = LinearStructureSupport.boundedValue(object.get("value"), TYPE + " value");
                if (values.length >= MAX_VALUES) {
                    throw new IllegalArgumentException(TYPE + " insertion would exceed " + MAX_VALUES + " nodes");
                }
                operations = List.of(new Operation(operation, index, value));
            } else if (operation.equals("delete")) {
                LinearStructureSupport.exactFields(object, Set.of("values", "operation", "index"), TYPE);
                int index = LinearStructureSupport.integer(object.get("index"), TYPE + " delete index");
                LinearStructureSupport.requireIndex(index, 0, values.length - 1, TYPE + " delete index");
                operations = List.of(new Operation(operation, index, null));
            } else if (operation.equals("reverse")) {
                LinearStructureSupport.exactFields(object, Set.of("values", "operation"), TYPE);
                operations = List.of(new Operation(operation, -1, null));
            } else {
                throw new IllegalArgumentException(TYPE + " operation must be exactly insert, delete, or reverse");
            }
            return new Request(values, operations);
        }
    }
}
