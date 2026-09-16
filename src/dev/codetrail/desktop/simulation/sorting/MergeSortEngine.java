package dev.codetrail.desktop.simulation.sorting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.ArrayState;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Bounded, stable merge-sort trace producer for the shared ARRAY renderer.
 *
 * <p>The trace keeps an identity key for every input element. During a merge,
 * the sorted halves and the auxiliary temporary buffer are kept separately:
 * comparison and copy-to-temp snapshots leave the primary array untouched,
 * then each real copy-back write updates its destination cell. Equal values
 * select the left element first, so their identity keys retain stable input
 * order.</p>
 */
public final class MergeSortEngine implements SimulationEngine {
    public static final String TYPE = "MERGE_SORT";
    public static final int MAX_ARRAY_LENGTH = 24;
    public static final int MAX_ABS_VALUE = 999;
    public static final int MAX_TRACE_STEPS = 1024;

    private static final int LINE_ENTER = 1;
    private static final int LINE_CHECK_RANGE = 2;
    private static final int LINE_RETURN = 3;
    private static final int LINE_MIDDLE = 4;
    private static final int LINE_LEFT_RECURSION = 5;
    private static final int LINE_RIGHT_RECURSION = 6;
    private static final int LINE_MERGE_CALL = 7;
    private static final int LINE_MERGE = 8;
    private static final int LINE_POINTERS = 9;
    private static final int LINE_WHILE = 10;
    private static final int LINE_COMPARE = 11;
    private static final int LINE_COPY_LEFT = 12;
    private static final int LINE_ELSE = 13;
    private static final int LINE_COPY_RIGHT = 14;
    private static final int LINE_COPY_REMAINDER = 15;
    private static final int LINE_COPY_BACK = 16;

    private static final int[] DEFAULT_ARRAY = {38, 27, 43, 3, 9, 82, 10};
    private static final List<String> PSEUDOCODE = List.of(
            "mergeSort(a, low, high):",
            "    if low >= high:",
            "        return",
            "    middle = floor((low + high) / 2)",
            "    mergeSort(a, low, middle)",
            "    mergeSort(a, middle + 1, high)",
            "    merge(a, low, middle, high)",
            "merge(a, low, middle, high):",
            "    i = low; j = middle + 1; temp = []",
            "    while i <= middle and j <= high:",
            "        if a[i] <= a[j]:",
            "            copy a[i] to temp; i++",
            "        else:",
            "            copy a[j] to temp; j++",
            "    copy remaining values to temp",
            "    copy temp into a[low..high]");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int[] inputValues = readArray(input);
        Element[] elements = new Element[inputValues.length];
        for (int index = 0; index < inputValues.length; index++) {
            elements[index] = new Element(Integer.toString(index), inputValues[index]);
        }

        TraceBuilder trace = new TraceBuilder(elements);
        trace.add(
                trace.allStatuses(SnapshotStatus.DEFAULT),
                -1,
                0,
                "Initialize merge sort with " + elements.length + " array value(s)",
                StepEventType.INITIALIZE);

        if (elements.length > 0) {
            mergeSort(elements, 0, elements.length - 1, trace);
        }

        trace.add(
                trace.allStatuses(SnapshotStatus.DONE),
                -1,
                0,
                "Complete: array is sorted in nondecreasing order",
                StepEventType.COMPLETE);
        return List.copyOf(trace.steps());
    }

    private static void mergeSort(Element[] elements, int low, int high, TraceBuilder trace) {
        trace.add(
                trace.rangeStatuses(low, high, SnapshotStatus.ACTIVE),
                low == high ? low : -1,
                LINE_ENTER,
                "Enter mergeSort on range [" + low + ", " + high + "]",
                StepEventType.EXECUTE_LINE);
        trace.add(
                trace.rangeStatuses(low, high, SnapshotStatus.ACTIVE),
                low == high ? low : -1,
                LINE_CHECK_RANGE,
                "Check whether range [" + low + ", " + high + "] has at most one value",
                StepEventType.EXECUTE_LINE);
        if (low >= high) {
            trace.add(
                    trace.rangeStatuses(low, high, SnapshotStatus.DONE),
                    low,
                    LINE_RETURN,
                    "Return the one-value range [" + low + ", " + high + "]",
                    StepEventType.EXECUTE_LINE);
            return;
        }

        int middle = low + (high - low) / 2;
        trace.add(
                trace.rangeStatuses(low, high, SnapshotStatus.ACTIVE),
                middle,
                LINE_MIDDLE,
                "Split [" + low + ", " + high + "] into [" + low + ", " + middle
                        + "] and [" + (middle + 1) + ", " + high + "]",
                StepEventType.EXECUTE_LINE,
                List.of(
                        new Fact("phase", "split", SnapshotStatus.DEFAULT),
                        new Fact("split-low", Integer.toString(low), SnapshotStatus.ACTIVE),
                        new Fact("split-mid", Integer.toString(middle), SnapshotStatus.ACTIVE),
                        new Fact("split-high", Integer.toString(high), SnapshotStatus.ACTIVE)));
        trace.add(
                trace.rangeStatuses(low, high, SnapshotStatus.ACTIVE),
                low,
                LINE_LEFT_RECURSION,
                "Recurse into left half [" + low + ", " + middle + "]",
                StepEventType.EXECUTE_LINE);
        mergeSort(elements, low, middle, trace);
        trace.add(
                trace.rangeStatuses(low, high, SnapshotStatus.ACTIVE),
                middle + 1,
                LINE_RIGHT_RECURSION,
                "Recurse into right half [" + (middle + 1) + ", " + high + "]",
                StepEventType.EXECUTE_LINE);
        mergeSort(elements, middle + 1, high, trace);
        trace.add(
                trace.rangeStatuses(low, high, SnapshotStatus.ACTIVE),
                -1,
                LINE_MERGE_CALL,
                "Merge the sorted halves [" + low + ", " + middle + "] and ["
                        + (middle + 1) + ", " + high + "]",
                StepEventType.EXECUTE_LINE);
        merge(elements, low, middle, high, trace);
    }

    private static void merge(
            Element[] elements,
            int low,
            int middle,
            int high,
            TraceBuilder trace) {
        Element[] left = Arrays.copyOfRange(elements, low, middle + 1);
        Element[] right = Arrays.copyOfRange(elements, middle + 1, high + 1);
        List<Element> temp = new ArrayList<>(high - low + 1);
        trace.add(
                trace.rangeStatuses(low, high, SnapshotStatus.ACTIVE),
                -1,
                LINE_MERGE,
                "Merge range [" + low + ", " + high + "]",
                StepEventType.EXECUTE_LINE,
                mergeFacts(left, 0, right, 0, temp, null, -1, -1, high - low + 1));
        trace.add(
                trace.rangeStatuses(low, high, SnapshotStatus.ACTIVE),
                low,
                LINE_POINTERS,
                "Copy the left and right halves into temporary cursors",
                StepEventType.EXECUTE_LINE,
                mergeFacts(left, 0, right, 0, temp, null, -1, -1, high - low + 1));

        int leftCursor = 0;
        int rightCursor = 0;
        int write = low;
        while (leftCursor < left.length && rightCursor < right.length) {
            Element leftElement = left[leftCursor];
            Element rightElement = right[rightCursor];
            int leftPosition = low + leftCursor;
            int rightPosition = middle + 1 + rightCursor;
            trace.add(
                    trace.mergeStatuses(low, high, -1, -1),
                    -1,
                    LINE_WHILE,
                    "Check whether both merge cursors still have values",
                    StepEventType.EXECUTE_LINE,
                    mergeFacts(left, leftCursor, right, rightCursor, temp, null, -1, -1, high - low + 1));
            trace.add(
                    trace.mergeStatuses(low, high, leftPosition, rightPosition),
                    leftPosition,
                    LINE_COMPARE,
                    "Compare " + leftElement.value + " and " + rightElement.value
                            + "; choose the left value when they are equal",
                    StepEventType.EXECUTE_LINE,
                    mergeFacts(left, leftCursor, right, rightCursor, temp, null, -1, -1, high - low + 1));

            if (leftElement.value <= rightElement.value) {
                temp.add(leftElement);
                leftCursor++;
                trace.add(
                        trace.mergeStatuses(low, high, leftPosition, -1),
                        leftPosition,
                        LINE_COPY_LEFT,
                        "Copy left value " + leftElement.value + " into temp slot " + (write - low),
                        StepEventType.EXECUTE_LINE,
                        transferFacts(mergeFacts(left, leftCursor, right, rightCursor, temp, null, -1, -1, high - low + 1),
                                "left", leftCursor - 1, temp.size() - 1));
            } else {
                trace.add(
                        trace.mergeStatuses(low, high, -1, rightPosition),
                        rightPosition,
                        LINE_ELSE,
                        "Left value is larger, so take the right value",
                        StepEventType.EXECUTE_LINE,
                        mergeFacts(left, leftCursor, right, rightCursor, temp, null, -1, -1, high - low + 1));
                temp.add(rightElement);
                rightCursor++;
                trace.add(
                        trace.mergeStatuses(low, high, -1, rightPosition),
                        rightPosition,
                        LINE_COPY_RIGHT,
                        "Copy right value " + rightElement.value + " into temp slot " + (write - low),
                        StepEventType.EXECUTE_LINE,
                        transferFacts(mergeFacts(left, leftCursor, right, rightCursor, temp, null, -1, -1, high - low + 1),
                                "right", rightCursor - 1, temp.size() - 1));
            }
            write++;
        }

        while (leftCursor < left.length) {
            Element element = left[leftCursor++];
            temp.add(element);
            trace.add(
                    trace.mergeStatuses(low, high, low + leftCursor - 1, -1),
                    low + leftCursor - 1,
                    LINE_COPY_REMAINDER,
                    "Copy remaining left value " + element.value + " into temp slot " + (write - low),
                    StepEventType.EXECUTE_LINE,
                    transferFacts(mergeFacts(left, leftCursor, right, rightCursor, temp, null, -1, -1, high - low + 1),
                            "left", leftCursor - 1, temp.size() - 1));
            write++;
        }
        while (rightCursor < right.length) {
            Element element = right[rightCursor++];
            temp.add(element);
            trace.add(
                    trace.mergeStatuses(low, high, -1, middle + rightCursor),
                    middle + rightCursor,
                    LINE_COPY_REMAINDER,
                    "Copy remaining right value " + element.value + " into temp slot " + (write - low),
                    StepEventType.EXECUTE_LINE,
                    transferFacts(mergeFacts(left, leftCursor, right, rightCursor, temp, null, -1, -1, high - low + 1),
                            "right", rightCursor - 1, temp.size() - 1));
            write++;
        }

        for (int offset = 0; offset < temp.size(); offset++) {
            int outputPosition = low + offset;
            Element copied = temp.get(offset);
            elements[outputPosition] = copied;
            trace.add(
                    trace.copyBackStatuses(low, high, outputPosition),
                    outputPosition,
                    LINE_COPY_BACK,
                    "Copy temp value " + copied.value + " into a[" + outputPosition + "]",
                    StepEventType.EXECUTE_LINE,
                    mergeFacts(left, left.length, right, right.length, temp, copied, offset, outputPosition, temp.size()));
        }
    }

    private static List<Fact> mergeFacts(
            Element[] left,
            int leftCursor,
            Element[] right,
            int rightCursor,
            List<Element> temp,
            Element copied,
            int copyBackOffset,
            int outputPosition,
            int total) {
        List<Fact> facts = new ArrayList<>(10);
        SnapshotStatus tempStatus = copied == null
                ? (temp.isEmpty() ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE)
                : SnapshotStatus.DONE;
        facts.add(new Fact("temp", formatElements(temp), tempStatus));
        facts.add(new Fact("left-run", formatElements(Arrays.asList(left)), SnapshotStatus.DEFAULT));
        facts.add(new Fact("right-run", formatElements(Arrays.asList(right)), SnapshotStatus.DEFAULT));
        facts.add(new Fact("left-cursor", Integer.toString(leftCursor),
                leftCursor == left.length ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE));
        facts.add(new Fact("right-cursor", Integer.toString(rightCursor),
                rightCursor == right.length ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE));
        facts.add(new Fact(
                "left-unread",
                formatRemaining(left, leftCursor),
                leftCursor == left.length ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE));
        facts.add(new Fact(
                "right-unread",
                formatRemaining(right, rightCursor),
                rightCursor == right.length ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE));
        if (copied != null) {
            SnapshotStatus copyBackStatus = copyBackOffset + 1 == total
                    ? SnapshotStatus.DONE
                    : SnapshotStatus.ACTIVE;
            facts.add(new Fact(
                    "copy-back",
                    "a[" + outputPosition + "] <- temp[" + copyBackOffset + "] = " + formatElement(copied)
                            + " (" + (copyBackOffset + 1) + "/" + total + ")",
                    copyBackStatus));
            facts.add(new Fact("copy-back-index", Integer.toString(copyBackOffset), copyBackStatus));
            facts.add(new Fact("output-index", Integer.toString(outputPosition), copyBackStatus));
        }
        return List.copyOf(facts);
    }

    private static List<Fact> transferFacts(List<Fact> mergeFacts, String side, int sourceIndex, int tempIndex) {
        List<Fact> facts = new ArrayList<>(mergeFacts);
        facts.add(new Fact("source-side", side, SnapshotStatus.ACTIVE));
        facts.add(new Fact("source-index", Integer.toString(sourceIndex), SnapshotStatus.ACTIVE));
        facts.add(new Fact("temp-index", Integer.toString(tempIndex), SnapshotStatus.ACTIVE));
        return List.copyOf(facts);
    }

    private static String formatRemaining(Element[] elements, int cursor) {
        return formatElements(Arrays.asList(elements).subList(cursor, elements.length));
    }

    private static String formatElements(Iterable<Element> elements) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Element element : elements) {
            joiner.add(formatElement(element));
        }
        return joiner.toString();
    }

    private static String formatElement(Element element) {
        return element.value + "@" + element.key;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode array = defaultInput.putArray("array");
        for (int value : DEFAULT_ARRAY) {
            array.add(value);
        }
        return new SimulationMetadata(
                TYPE,
                "Merge Sort",
                "O(n log n)",
                "O(n)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"array\":[38,27,43,3,9,82,10]}; array length must be 0 through "
                        + MAX_ARRAY_LENGTH + " and every integer must have absolute value at most " + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }

    private static int[] readArray(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException("MERGE_SORT input must be a JSON object");
        }
        JsonNode arrayNode = input.get("array");
        if (arrayNode == null || !arrayNode.isArray()) {
            throw new IllegalArgumentException("MERGE_SORT input array must be a JSON array");
        }
        if (arrayNode.size() > MAX_ARRAY_LENGTH) {
            throw new IllegalArgumentException("MERGE_SORT array length must be at most " + MAX_ARRAY_LENGTH);
        }
        int[] values = new int[arrayNode.size()];
        for (int index = 0; index < arrayNode.size(); index++) {
            JsonNode valueNode = arrayNode.get(index);
            if (valueNode == null || !valueNode.isIntegralNumber() || !valueNode.canConvertToInt()) {
                throw new IllegalArgumentException("MERGE_SORT array values must be bounded integers");
            }
            int value = valueNode.intValue();
            if (Math.abs((long) value) > MAX_ABS_VALUE) {
                throw new IllegalArgumentException(
                        "MERGE_SORT array values must have absolute value at most " + MAX_ABS_VALUE);
            }
            values[index] = value;
        }
        return values;
    }

    private record Element(String key, int value) {
        private Element {
            Objects.requireNonNull(key, "key");
        }
    }

    private static final class TraceBuilder {
        private final Element[] elements;
        private final List<SimulationStep> steps = new ArrayList<>();

        private TraceBuilder(Element[] elements) {
            this.elements = elements;
        }

        private List<SimulationStep> steps() {
            return steps;
        }

        private SnapshotStatus[] allStatuses(SnapshotStatus status) {
            SnapshotStatus[] statuses = new SnapshotStatus[elements.length];
            Arrays.fill(statuses, status);
            return statuses;
        }

        private SnapshotStatus[] rangeStatuses(int low, int high, SnapshotStatus status) {
            SnapshotStatus[] statuses = allStatuses(SnapshotStatus.DEFAULT);
            if (low <= high) {
                Arrays.fill(statuses, low, high + 1, status);
            }
            return statuses;
        }

        private SnapshotStatus[] mergeStatuses(
                int low,
                int high,
                int firstActivePosition,
                int secondActivePosition) {
            SnapshotStatus[] statuses = allStatuses(SnapshotStatus.DEFAULT);
            if (firstActivePosition >= 0) {
                statuses[firstActivePosition] = SnapshotStatus.ACTIVE;
            }
            if (secondActivePosition >= 0) {
                statuses[secondActivePosition] = SnapshotStatus.ACTIVE;
            }
            return statuses;
        }

        private SnapshotStatus[] copyBackStatuses(int low, int high, int copiedThrough) {
            SnapshotStatus[] statuses = allStatuses(SnapshotStatus.DEFAULT);
            Arrays.fill(statuses, low, copiedThrough + 1, SnapshotStatus.DONE);
            if (copiedThrough < high) {
                Arrays.fill(statuses, copiedThrough + 1, high + 1, SnapshotStatus.ACTIVE);
            }
            return statuses;
        }

        private void add(
                SnapshotStatus[] statuses,
                int focusIndex,
                int highlightedLine,
                String narration,
                StepEventType eventType) {
            add(statuses, focusIndex, highlightedLine, narration, eventType, List.of());
        }

        private void add(
                SnapshotStatus[] statuses,
                int focusIndex,
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("MERGE_SORT trace exceeded bounded step limit");
            }
            List<TypedCell> cells = new ArrayList<>(elements.length);
            for (int index = 0; index < elements.length; index++) {
                Element element = elements[index];
                cells.add(new TypedCell(element.key, Integer.toString(element.value), statuses[index]));
            }
            List<Fact> taggedFacts = new ArrayList<>(facts.size() + 1);
            taggedFacts.add(new Fact("renderer", "merge", SnapshotStatus.DEFAULT));
            taggedFacts.addAll(facts);
            ArrayState state = new ArrayState(cells, focusIndex, taggedFacts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.of(), Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }
    }
}
