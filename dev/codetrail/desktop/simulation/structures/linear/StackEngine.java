package dev.codetrail.desktop.simulation.structures.linear;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.CallFrame;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StackState;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** LIFO stack trace using the existing typed STACK state family. */
public final class StackEngine implements SimulationEngine {
    public static final String TYPE = "STACK";
    public static final int MAX_OPERATIONS = LinearStructureSupport.MAX_OPERATIONS;
    public static final int MAX_ABS_VALUE = LinearStructureSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = LinearStructureSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_LOOP = 2;
    private static final int LINE_KIND = 3;
    private static final int LINE_PUSH = 4;
    private static final int LINE_EMPTY = 5;
    private static final int LINE_READ_TOP = 6;
    private static final int LINE_POP = 7;
    private static final int LINE_RETURN = 8;

    private static final int[] DEFAULT_VALUES = {4, 9, 1};
    private static final List<String> PSEUDOCODE = List.of(
            "processStack(operations):",
            "    for each operation:",
            "        if operation.kind == push:",
            "            stack.push(operation.value)",
            "        else if stack is empty: report EMPTY",
            "        else value = stack.top()",
            "            stack.pop()",
            "            return value");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        Trace trace = new Trace();
        trace.add(
                0,
                "Initialize an empty LIFO stack",
                StepEventType.INITIALIZE,
                facts("initialize", "none", "EMPTY", "0", SnapshotStatus.ACTIVE));

        for (int operationIndex = 0; operationIndex < request.operations().size(); operationIndex++) {
            Operation operation = request.operations().get(operationIndex);
            trace.returned = null;
            trace.resetActiveTop();
            trace.add(
                    LINE_METHOD,
                    "Start operation " + (operationIndex + 1) + ": " + operation.describe(),
                    StepEventType.EXECUTE_LINE,
                    facts(operation.kind(), trace.stackText(), trace.topText(),
                            Integer.toString(trace.frames.size()), SnapshotStatus.ACTIVE));
            trace.add(
                    LINE_LOOP,
                    "Read operation " + (operationIndex + 1),
                    StepEventType.EXECUTE_LINE,
                    facts(operation.kind(), trace.stackText(), trace.topText(),
                            Integer.toString(trace.frames.size()), SnapshotStatus.ACTIVE));
            trace.add(
                    LINE_KIND,
                    "Dispatch kind " + operation.kind(),
                    StepEventType.EXECUTE_LINE,
                    facts(operation.kind(), trace.stackText(), trace.topText(),
                            Integer.toString(trace.frames.size()), SnapshotStatus.ACTIVE));
            if (operation.isPush()) {
                trace.push(operation.value());
                trace.add(
                        LINE_PUSH,
                        "Push " + operation.value() + "; top = " + operation.value(),
                        StepEventType.PUSH_FRAME,
                        facts("push", trace.stackText(), trace.topText(),
                                Integer.toString(trace.frames.size()), SnapshotStatus.ACTIVE));
            } else if (trace.frames.isEmpty()) {
                trace.add(
                        LINE_EMPTY,
                        "Pop on an empty stack returns EMPTY",
                        StepEventType.EXECUTE_LINE,
                        facts("pop", trace.stackText(), "EMPTY", "0", SnapshotStatus.REJECTED));
            } else {
                trace.add(
                        LINE_READ_TOP,
                        "Read top value " + trace.topValue(),
                        StepEventType.EXECUTE_LINE,
                        facts("pop", trace.stackText(), trace.topText(),
                                Integer.toString(trace.frames.size()), SnapshotStatus.ACTIVE));
                int value = trace.pop();
                trace.add(
                        LINE_POP,
                        "Remove top value " + value,
                        StepEventType.RETURN_FRAME,
                        facts("pop", trace.stackText(), trace.topText(),
                                Integer.toString(trace.frames.size()), SnapshotStatus.DONE));
                trace.add(
                        LINE_RETURN,
                        "Return " + value,
                        StepEventType.EXECUTE_LINE,
                        facts("pop", trace.stackText(), trace.topText(),
                                Integer.toString(trace.frames.size()), SnapshotStatus.DONE));
            }
        }

        trace.returned = null;
        trace.resetActiveTop();
        trace.add(
                0,
                "Complete: stack = " + trace.stackText(),
                StepEventType.COMPLETE,
                facts("complete", trace.stackText(), trace.topText(),
                        Integer.toString(trace.frames.size()), SnapshotStatus.DONE));
        return trace.steps();
    }

    private static List<Fact> facts(
            String operation,
            String stack,
            String top,
            String size,
            SnapshotStatus status) {
        return List.of(
                LinearStructureSupport.fact("operation", operation, SnapshotStatus.DEFAULT),
                LinearStructureSupport.fact("stack", stack, status),
                LinearStructureSupport.fact("top", top, status),
                LinearStructureSupport.fact("size", size, status),
                LinearStructureSupport.fact(
                        "outcome",
                        operation.equals("pop") && top.equals("EMPTY") && status == SnapshotStatus.REJECTED
                                ? "EMPTY"
                                : operation.equals("push")
                                        ? "PUSH"
                                        : operation.equals("pop") ? "POP" : "none",
                        status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode operations = defaultInput.putArray("operations");
        addPush(operations, DEFAULT_VALUES[0]);
        addPush(operations, DEFAULT_VALUES[1]);
        addPop(operations);
        addPush(operations, DEFAULT_VALUES[2]);
        return new SimulationMetadata(
                TYPE,
                "Stack",
                "O(1) push/pop",
                "O(n)",
                RendererFamily.STACK,
                defaultInput,
                "Enter JSON as {\"operations\":[{\"kind\":\"push\",\"value\":4},{\"kind\":\"pop\"}]}; use at most "
                        + MAX_OPERATIONS + " operations, push values must have absolute value at most "
                        + MAX_ABS_VALUE + ", and an empty pop reports EMPTY.",
                PSEUDOCODE);
    }

    private static void addPush(ArrayNode operations, int value) {
        ObjectNode operation = operations.addObject();
        operation.put("kind", "push");
        operation.put("value", value);
    }

    private static void addPop(ArrayNode operations) {
        operations.addObject().put("kind", "pop");
    }

    private record Operation(String kind, Integer value) {
        boolean isPush() { return kind.equals("push"); }

        String describe() {
            return isPush() ? "push(" + value + ")" : "pop()";
        }
    }

    private record Request(List<Operation> operations) {
        static Request parse(JsonNode input) {
            ObjectNode object = LinearStructureSupport.objectInput(input, TYPE);
            LinearStructureSupport.exactFields(object, Set.of("operations"), TYPE);
            JsonNode operationsNode = object.get("operations");
            if (operationsNode == null || !operationsNode.isArray()) {
                throw new IllegalArgumentException(TYPE + " operations must be a JSON array");
            }
            if (operationsNode.size() > MAX_OPERATIONS) {
                throw new IllegalArgumentException(TYPE + " operations must contain at most " + MAX_OPERATIONS + " entries");
            }
            List<Operation> operations = new ArrayList<>(operationsNode.size());
            for (int index = 0; index < operationsNode.size(); index++) {
                JsonNode operationNode = operationsNode.get(index);
                String context = TYPE + " operation " + index;
                LinearStructureSupport.exactFields(operationNode, Set.of("kind", "value"), context);
                ObjectNode operation = (ObjectNode) operationNode;
                String kind = LinearStructureSupport.text(operation.get("kind"), context + " kind");
                if (kind.equals("push")) {
                    if (!operation.has("value")) {
                        throw new IllegalArgumentException(context + " push requires value");
                    }
                    int value = LinearStructureSupport.boundedValue(operation.get("value"), context + " value");
                    operations.add(new Operation(kind, value));
                } else if (kind.equals("pop")) {
                    if (operation.has("value")) {
                        throw new IllegalArgumentException(context + " pop does not accept value");
                    }
                    operations.add(new Operation(kind, null));
                } else {
                    throw new IllegalArgumentException(context + " kind must be exactly push or pop");
                }
            }
            return new Request(List.copyOf(operations));
        }
    }

    private static final class Trace {
        private final List<CallFrame> frames = new ArrayList<>();
        private final List<SimulationStep> steps = new ArrayList<>();
        private CallFrame returned;
        private int serial;

        private void resetActiveTop() {
            for (int index = 0; index < frames.size(); index++) {
                CallFrame frame = frames.get(index);
                frames.set(index, withStatus(frame, index == frames.size() - 1
                        ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT));
            }
        }

        private void push(int value) {
            resetActiveTop();
            frames.add(new CallFrame(
                    "stack-" + serial++, "push", value, SnapshotStatus.ACTIVE));
        }

        private int topValue() {
            if (frames.isEmpty()) {
                throw new IllegalStateException("empty stack has no top value");
            }
            return frames.get(frames.size() - 1).argument();
        }

        private int pop() {
            if (frames.isEmpty()) {
                throw new IllegalStateException("cannot pop empty stack");
            }
            CallFrame frame = frames.remove(frames.size() - 1);
            returned = new CallFrame(
                    frame.id(), frame.functionName(), frame.argument(),
                    Integer.toString(frame.argument()), SnapshotStatus.DONE);
            resetActiveTop();
            return frame.argument();
        }

        private String stackText() {
            StringBuilder result = new StringBuilder("[");
            for (int index = 0; index < frames.size(); index++) {
                if (index > 0) {
                    result.append(", ");
                }
                result.append(frames.get(index).argument());
            }
            return result.append(']').toString();
        }

        private String topText() {
            return frames.isEmpty() ? "EMPTY" : Integer.toString(topValue());
        }

        private void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("linear structure trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            List<CallFrame> frameSnapshot = new ArrayList<>(frames.size());
            for (int index = 0; index < frames.size(); index++) {
                CallFrame frame = frames.get(index);
                frameSnapshot.add(withStatus(frame, index == frames.size() - 1
                        ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT));
            }
            Set<String> activeFrameIds = new HashSet<>();
            if (!frameSnapshot.isEmpty()) {
                activeFrameIds.add(frames.get(frames.size() - 1).id());
            }
            StackState state = new StackState(frameSnapshot, facts, activeFrameIds, returned);
            String eventFrameId = eventType == StepEventType.PUSH_FRAME && !frames.isEmpty()
                    ? frames.get(frames.size() - 1).id()
                    : returned == null ? null : returned.id();
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, activeFrameIds, Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    eventFrameId));
        }

        private List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        private static CallFrame withStatus(CallFrame frame, SnapshotStatus status) {
            return new CallFrame(frame.id(), frame.functionName(), frame.argument(), frame.result(), status);
        }
    }
}
