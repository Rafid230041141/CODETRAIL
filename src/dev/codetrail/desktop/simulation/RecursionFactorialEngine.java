package dev.codetrail.desktop.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Representative Phase 2 recursion simulation. The input is exactly an
 * object containing an integral {@code n} in the inclusive range 0..10.
 */
public final class RecursionFactorialEngine implements SimulationEngine {
    public static final String TYPE = "RECURSION";
    public static final int MIN_N = 0;
    public static final int MAX_N = 10;
    public static final int MAX_TRACE_STEPS = 64;

    private static final List<String> PSEUDOCODE = List.of(
            "factorial(n):",
            "    if n <= 1:",
            "        return 1",
            "    return n * factorial(n - 1)"
    );
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int n = readBoundedN(input);
        TraceBuilder trace = new TraceBuilder();
        long result = factorial(n, trace.nextFrameId(), trace);
        // The root return already contains this fact for n > 0; adding the
        // named result makes the final invariant direct for every input.
        trace.addFact("result", Long.toString(result), SnapshotStatus.DONE);
        trace.clearReturnedFrame();
        trace.addStep(
                trace.stack.snapshot(),
                0,
                "Complete: factorial(" + n + ") = " + result,
                StepEventType.COMPLETE,
                null);
        return List.copyOf(trace.steps);
    }

    private long factorial(int n, String frameId, TraceBuilder trace) {
        trace.push(frameId, n);
        trace.addStep(
                trace.stack.snapshot(),
                1,
                "Push " + frameId + ": factorial(" + n + ")",
                StepEventType.PUSH_FRAME,
                frameId);
        trace.addStep(
                trace.stack.snapshot(),
                2,
                "Check whether " + n + " <= 1",
                StepEventType.EXECUTE_LINE,
                frameId);

        if (n <= 1) {
            trace.finish(frameId, 1L);
            trace.addFact("factorial(" + n + ")", "1", SnapshotStatus.DONE);
            trace.addStep(
                    trace.stack.snapshot(),
                    3,
                    "Return 1 from " + frameId,
                    StepEventType.RETURN_FRAME,
                    frameId);
            return 1L;
        }

        trace.addStep(
                trace.stack.snapshot(),
                4,
                "Push recursive call factorial(" + (n - 1) + ")",
                StepEventType.EXECUTE_LINE,
                frameId);
        String childId = trace.nextFrameId();
        long childResult = factorial(n - 1, childId, trace);
        long result = n * childResult;
        trace.setResult(frameId, result);
        trace.addFact("factorial(" + n + ")", Long.toString(result), SnapshotStatus.DONE);
        trace.addStep(
                trace.stack.snapshot(),
                4,
                "Return from " + childId + "; multiply " + n + " * " + childResult,
                StepEventType.EXECUTE_LINE,
                frameId);
        trace.finish(frameId, result);
        trace.addStep(
                trace.stack.snapshot(),
                4,
                "Return " + result + " from " + frameId,
                StepEventType.RETURN_FRAME,
                frameId);
        return result;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("n", 5);
        return new SimulationMetadata(
                TYPE,
                "Recursive Factorial",
                "O(n)",
                "O(n)",
                RendererFamily.STACK,
                defaultInput,
                "Enter JSON in the form {\"n\": 5}; n must be an integer from 0 through 10.",
                PSEUDOCODE);
    }

    private static int readBoundedN(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException("RECURSION input must be a JSON object");
        }
        JsonNode nNode = input.get("n");
        if (nNode == null || !nNode.isIntegralNumber() || !nNode.canConvertToInt()) {
            throw new IllegalArgumentException("RECURSION input n must be an integer");
        }
        int n = nNode.intValue();
        if (n < MIN_N || n > MAX_N) {
            throw new IllegalArgumentException("RECURSION input n must be bounded to 0..10");
        }
        return n;
    }

    private static final class TraceBuilder {
        private final MutableStack stack = new MutableStack();
        private final List<SimulationStep> steps = new ArrayList<>();
        private int nextFrame;

        private String nextFrameId() {
            return "frame-" + nextFrame++;
        }

        private void push(String id, int argument) {
            stack.push(new CallFrame(id, "factorial", argument, SnapshotStatus.ACTIVE));
        }

        private void setResult(String id, long result) {
            stack.setResult(id, Long.toString(result));
        }

        private void finish(String id, long result) {
            stack.finish(id, Long.toString(result));
        }

        private void clearReturnedFrame() {
            stack.clearReturnedFrame();
        }

        private void addFact(String key, String value, SnapshotStatus status) {
            stack.addFact(new Fact(key, value, status));
        }

        private void addStep(
                StackState state,
                int line,
                String narration,
                StepEventType eventType,
                String frameId) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("RECURSION trace exceeded bounded step limit");
            }
            Set<String> activeNodeIds = new HashSet<>(state.activeFrameIds());
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, activeNodeIds, Set.of()),
                    line,
                    narration,
                    eventType,
                    frameId));
        }
    }

    private static final class MutableStack {
        private final List<CallFrame> frames = new ArrayList<>();
        private final List<Fact> facts = new ArrayList<>();
        private final Set<String> activeFrameIds = new HashSet<>();
        private CallFrame lastReturnedFrame;

        private void push(CallFrame frame) {
            lastReturnedFrame = null;
            if (!frames.isEmpty()) {
                CallFrame waiting = frames.get(frames.size() - 1);
                frames.set(frames.size() - 1,
                        new CallFrame(waiting.id(), waiting.functionName(), waiting.argument(), waiting.result(), SnapshotStatus.DEFAULT));
            }
            frames.add(Objects.requireNonNull(frame, "frame"));
            activeFrameIds.clear();
            activeFrameIds.add(frame.id());
        }

        private void setResult(String id, String result) {
            lastReturnedFrame = null;
            int index = indexOf(id);
            CallFrame current = frames.get(index);
            frames.set(index, new CallFrame(current.id(), current.functionName(), current.argument(), result, current.status()));
        }

        private void finish(String id, String result) {
            int index = indexOf(id);
            if (index != frames.size() - 1) {
                throw new IllegalArgumentException("only the top frame can return: " + id);
            }
            CallFrame current = frames.get(index);
            lastReturnedFrame = new CallFrame(current.id(), current.functionName(), current.argument(), result, SnapshotStatus.DONE);
            frames.remove(index);
            activeFrameIds.clear();
            if (!frames.isEmpty()) {
                CallFrame parent = frames.get(frames.size() - 1);
                frames.set(frames.size() - 1,
                        new CallFrame(parent.id(), parent.functionName(), parent.argument(), parent.result(), SnapshotStatus.ACTIVE));
                activeFrameIds.add(parent.id());
            }
        }

        private void addFact(Fact fact) {
            facts.add(Objects.requireNonNull(fact, "fact"));
        }

        private void clearReturnedFrame() {
            lastReturnedFrame = null;
        }

        private StackState snapshot() {
            return new StackState(frames, facts, activeFrameIds, lastReturnedFrame);
        }

        private int indexOf(String id) {
            for (int index = 0; index < frames.size(); index++) {
                if (frames.get(index).id().equals(id)) {
                    return index;
                }
            }
            throw new IllegalArgumentException("unknown frame: " + id);
        }
    }
}
