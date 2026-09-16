package dev.codetrail.desktop.simulation.sorting;

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
import java.util.List;

/** Stable counting-sort trace with a visible frequency table and output buffer. */
public final class CountingSortEngine implements SimulationEngine {
    public static final String TYPE = "COUNTING_SORT";
    public static final int MAX_ARRAY_LENGTH = SortingSupport.MAX_ARRAY_LENGTH;
    public static final int MAX_ABS_VALUE = SortingSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = 1024;

    private static final int LINE_ENTER = 1;
    private static final int LINE_CHECK_RANGE = 2;
    private static final int LINE_RETURN = 3;
    private static final int LINE_RANGE = 4;
    private static final int LINE_FREQUENCY = 5;
    private static final int LINE_CUMULATIVE = 6;
    private static final int LINE_OUTPUT = 7;
    private static final int LINE_SCAN = 8;
    private static final int LINE_POSITION = 9;
    private static final int LINE_OUTPUT_SLOT = 10;
    private static final int LINE_COPY_BACK = 11;

    private static final int[] DEFAULT_ARRAY = {4, 2, 2, 8, 3, 3, 1};
    private static final List<String> PSEUDOCODE = List.of(
            "countingSort(a):",
            "    if length <= 1:",
            "        return",
            "    min = minimum(a); max = maximum(a)",
            "    count[value - min]++ for each value",
            "    count = cumulative end positions",
            "    output = empty array",
            "    for index = n - 1 down to 0:",
            "        position = --count[a[index] - min]",
            "        output[position] = a[index]",
            "    copy output into a");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int[] values = SortingSupport.readBoundedArray(input, TYPE);
        SortingSupport.Element[] elements = SortingSupport.elements(values);
        SortingSupport.Trace trace = new SortingSupport.Trace(elements, MAX_TRACE_STEPS);
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.DEFAULT),
                -1,
                0,
                "Initialize counting sort with " + elements.length + " array value(s)",
                StepEventType.INITIALIZE);
        if (elements.length <= 1) {
            if (elements.length == 1) {
                trace.add(
                        SortingSupport.statuses(elements.length, SnapshotStatus.DONE),
                        0,
                        LINE_RETURN,
                        "Return the one-value array",
                        StepEventType.EXECUTE_LINE);
            }
            trace.add(
                    SortingSupport.statuses(elements.length, SnapshotStatus.DONE),
                    -1,
                    0,
                    "Complete: array is sorted in nondecreasing order",
                    StepEventType.COMPLETE);
            return trace.steps();
        }

        int minimum = values[0];
        int maximum = values[0];
        for (int value : values) {
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        int range = maximum - minimum + 1;
        int[] frequencies = new int[range];
        int[] positions = new int[range];
        SortingSupport.Element[] output = new SortingSupport.Element[elements.length];

        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_ENTER,
                "Enter countingSort for " + elements.length + " values",
                StepEventType.EXECUTE_LINE,
                countFacts(frequencies, positions, minimum, maximum, output,
                        "range " + minimum + " through " + maximum, SnapshotStatus.ACTIVE));
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_CHECK_RANGE,
                "Check that the array has more than one value",
                StepEventType.EXECUTE_LINE,
                countFacts(frequencies, positions, minimum, maximum, output,
                        "range check", SnapshotStatus.ACTIVE));
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_RANGE,
                "Use value range [" + minimum + ", " + maximum + "]",
                StepEventType.EXECUTE_LINE,
                countFacts(frequencies, positions, minimum, maximum, output,
                        "range selected", SnapshotStatus.ACTIVE));

        for (int index = 0; index < elements.length; index++) {
            int offset = elements[index].value() - minimum;
            frequencies[offset]++;
            trace.add(
                    SortingSupport.activePositions(elements.length, index),
                    index,
                    LINE_FREQUENCY,
                    "Count value " + elements[index].value() + " at input index " + index,
                    StepEventType.EXECUTE_LINE,
                    SortingSupport.withFacts(countFacts(frequencies, positions, minimum, maximum, output,
                            "count", SnapshotStatus.ACTIVE),
                            SortingSupport.fact("active-value", Integer.toString(elements[index].value()), SnapshotStatus.ACTIVE),
                            SortingSupport.fact("calculation", "count[" + elements[index].value() + "] = "
                                    + (frequencies[offset] - 1) + " + 1 = " + frequencies[offset], SnapshotStatus.ACTIVE)));
        }

        int running = 0;
        for (int offset = 0; offset < frequencies.length; offset++) {
            int before = running;
            running += frequencies[offset];
            positions[offset] = running;
            if (frequencies[offset] == 0) {
                continue;
            }
            int value = minimum + offset;
            String calculation = "end[" + value + "] = " + before + " + " + frequencies[offset]
                    + " = " + running;
            trace.add(
                    SortingSupport.statuses(elements.length, SnapshotStatus.DEFAULT),
                    -1,
                    LINE_CUMULATIVE,
                    calculation + "; " + running + " values are <= " + value,
                    StepEventType.EXECUTE_LINE,
                    SortingSupport.withFacts(countFacts(frequencies, positions, minimum, maximum, output,
                            "cumulative positions", SnapshotStatus.ACTIVE),
                            SortingSupport.fact("active-value", Integer.toString(value), SnapshotStatus.ACTIVE),
                            SortingSupport.fact("calculation", calculation, SnapshotStatus.ACTIVE)));
        }
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_OUTPUT,
                "Create an empty output buffer",
                StepEventType.EXECUTE_LINE,
                countFacts(frequencies, positions, minimum, maximum, output,
                        "output buffer ready", SnapshotStatus.ACTIVE));

        for (int index = elements.length - 1; index >= 0; index--) {
            int offset = elements[index].value() - minimum;
            trace.add(
                    SortingSupport.activePositions(elements.length, index),
                    index,
                    LINE_SCAN,
                    "Scan input index " + index + " from right to left",
                    StepEventType.EXECUTE_LINE,
                    countFacts(frequencies, positions, minimum, maximum, output,
                            "scan input index=" + index, SnapshotStatus.ACTIVE));
            int position = --positions[offset];
            trace.add(
                    SortingSupport.activePositions(elements.length, index),
                    index,
                    LINE_POSITION,
                    "Decrement end[" + elements[index].value() + "] from " + (position + 1)
                            + " to " + position + "; this is its next output slot",
                    StepEventType.EXECUTE_LINE,
                    SortingSupport.withFacts(countFacts(frequencies, positions, minimum, maximum, output,
                            "choose output position", SnapshotStatus.ACTIVE),
                            SortingSupport.fact("active-value", Integer.toString(elements[index].value()), SnapshotStatus.ACTIVE),
                            SortingSupport.fact("output-index", Integer.toString(position), SnapshotStatus.ACTIVE),
                            SortingSupport.fact("calculation", "--end[" + elements[index].value() + "] = "
                                    + (position + 1) + " - 1 = " + position, SnapshotStatus.ACTIVE)));
            output[position] = elements[index];
            trace.add(
                    SortingSupport.activePositions(elements.length, index),
                    index,
                    LINE_OUTPUT_SLOT,
                    "Place " + SortingSupport.formatElement(elements[index])
                            + " in output[" + position + "]; scanning right to left preserves equal-value order",
                    StepEventType.EXECUTE_LINE,
                    SortingSupport.withFacts(countFacts(frequencies, positions, minimum, maximum, output,
                            "stable placement", SnapshotStatus.ACTIVE),
                            SortingSupport.fact("active-value", Integer.toString(elements[index].value()), SnapshotStatus.ACTIVE),
                            SortingSupport.fact("output-index", Integer.toString(position), SnapshotStatus.ACTIVE)));
        }

        for (int index = 0; index < elements.length; index++) {
            elements[index] = output[index];
            SnapshotStatus status = index + 1 == elements.length
                    ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
            trace.add(
                    SortingSupport.donePrefixWithActiveSuffix(elements.length, index + 1),
                    index,
                    LINE_COPY_BACK,
                    "Copy output[" + index + "] into a[" + index + "]",
                    StepEventType.EXECUTE_LINE,
                    countFacts(
                            frequencies,
                            positions,
                            minimum,
                            maximum,
                            output,
                            "a[" + index + "] <- output[" + index + "] ("
                                    + (index + 1) + "/" + elements.length + ")",
                            status));
        }

        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.DONE),
                -1,
                0,
                "Complete: array is sorted in nondecreasing order",
                StepEventType.COMPLETE);
        return trace.steps();
    }

    private static List<Fact> countFacts(
            int[] frequencies,
            int[] positions,
            int minimum,
            int maximum,
            SortingSupport.Element[] output,
            String phase,
            SnapshotStatus status) {
        List<Fact> facts = new ArrayList<>();
        facts.add(SortingSupport.fact("phase", phase, status));
        if (phase.startsWith("a[")) {
            facts.add(SortingSupport.fact(
                    "copy-back", phase, status));
        } else {
            facts.add(SortingSupport.fact("range", minimum + ".." + maximum + "; " + phase, status));
        }
        facts.add(SortingSupport.fact(
                "frequency", SortingSupport.formatFrequency(frequencies, minimum, maximum), status));
        facts.add(SortingSupport.fact(
                "next-position", SortingSupport.formatPositions(positions, frequencies, minimum), status));
        facts.add(SortingSupport.fact("output", SortingSupport.formatOutput(output), status));
        return List.copyOf(facts);
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode array = defaultInput.putArray("array");
        for (int value : DEFAULT_ARRAY) {
            array.add(value);
        }
        return new SimulationMetadata(
                TYPE,
                "Counting Sort",
                "O(n + k)",
                "O(n + k)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"array\":[4,2,2,8,3,3,1]}; signed integers and duplicates are allowed; "
                        + "array length must be 0 through " + MAX_ARRAY_LENGTH
                        + " and every integer must have absolute value at most " + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }
}
