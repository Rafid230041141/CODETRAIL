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

/** Stable least-significant-digit radix-sort trace for the bounded signed range. */
public final class RadixSortEngine implements SimulationEngine {
    public static final String TYPE = "RADIX_SORT";
    public static final int MAX_ARRAY_LENGTH = SortingSupport.MAX_ARRAY_LENGTH;
    public static final int MAX_ABS_VALUE = SortingSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = 1024;

    private static final int OFFSET = SortingSupport.MAX_ABS_VALUE;
    private static final int BASE = 10;
    private static final int LINE_ENTER = 1;
    private static final int LINE_CHECK_RANGE = 2;
    private static final int LINE_RETURN = 3;
    private static final int LINE_SETUP = 4;
    private static final int LINE_PASS = 5;
    private static final int LINE_COUNT = 6;
    private static final int LINE_CUMULATIVE = 7;
    private static final int LINE_OUTPUT = 8;
    private static final int LINE_PLACE = 9;
    private static final int LINE_COPY_BACK = 10;
    private static final int LINE_ADVANCE_PLACE = 11;

    private static final int[] DEFAULT_ARRAY = {170, 45, 75, 90, 802, 24, 2, 66};
    private static final List<String> PSEUDOCODE = List.of(
            "radixSort(a):",
            "    if length <= 1:",
            "        return",
            "    offset = 999; place = 1",
            "    while place <= max(1, max(a[i] + offset)):",
            "        count digit = floor((value + offset) / place) % 10",
            "        count = cumulative end positions",
            "        output = empty array",
            "        scan input right to left; place by digit",
            "        copy output back into a",
            "        place *= 10");
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
                "Initialize radix sort with " + elements.length + " array value(s)",
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

        int maximumKey = 0;
        for (SortingSupport.Element element : elements) {
            maximumKey = Math.max(maximumKey, transformed(element.value()));
        }
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_ENTER,
                "Enter radixSort for " + elements.length + " values",
                StepEventType.EXECUTE_LINE,
                List.of(SortingSupport.fact(
                        "digit-pass", "offset=" + OFFSET + "; max-key=" + maximumKey, SnapshotStatus.ACTIVE)));
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_CHECK_RANGE,
                "Check that the array has more than one value",
                StepEventType.EXECUTE_LINE,
                List.of(SortingSupport.fact(
                        "digit-pass", "offset=" + OFFSET + "; max-key=" + maximumKey, SnapshotStatus.ACTIVE)));
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_SETUP,
                "Shift signed values by " + OFFSET + " and start at decimal place 1",
                StepEventType.EXECUTE_LINE,
                List.of(SortingSupport.fact(
                        "digit-pass", "offset=" + OFFSET + "; place=1", SnapshotStatus.ACTIVE)));

        int place = 1;
        int passNumber = 1;
        do {
            int[] frequencies = new int[BASE];
            List<List<SortingSupport.Element>> buckets = emptyBuckets();
            trace.add(
                    SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                    -1,
                    LINE_PASS,
                    "Start stable digit pass " + passNumber + " at place " + place,
                    StepEventType.EXECUTE_LINE,
                    passFacts(place, passNumber, frequencies, buckets, null, null, SnapshotStatus.ACTIVE));

            for (int index = 0; index < elements.length; index++) {
                int digit = digit(elements[index].value(), place);
                frequencies[digit]++;
                trace.add(
                        SortingSupport.activePositions(elements.length, index),
                        index,
                        LINE_COUNT,
                        "Value " + elements[index].value() + " becomes key " + transformed(elements[index].value())
                                + "; count digit " + digit + " at place " + place,
                        StepEventType.EXECUTE_LINE,
                        digitFacts(countFacts(place, passNumber, frequencies, null, null, SnapshotStatus.ACTIVE),
                                elements[index], place, -1));
            }

            int running = 0;
            int[] positions = new int[BASE];
            for (int digit = 0; digit < BASE; digit++) {
                running += frequencies[digit];
                positions[digit] = running;
            }
            trace.add(
                    SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                    -1,
                    LINE_CUMULATIVE,
                    "Convert digit frequencies to cumulative positions",
                    StepEventType.EXECUTE_LINE,
                    countFacts(place, passNumber, frequencies, positions, null, SnapshotStatus.ACTIVE));

            trace.add(
                    SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                    -1,
                    LINE_OUTPUT,
                    "Create an empty output buffer for digit pass " + passNumber,
                    StepEventType.EXECUTE_LINE,
                    passFacts(place, passNumber, frequencies, buckets, null, null, SnapshotStatus.ACTIVE));

            SortingSupport.Element[] output = new SortingSupport.Element[elements.length];
            for (int index = elements.length - 1; index >= 0; index--) {
                int digit = digit(elements[index].value(), place);
                int outputIndex = --positions[digit];
                output[outputIndex] = elements[index];
                buckets.get(digit).add(0, elements[index]);
                trace.add(
                        SortingSupport.activePositions(elements.length, index),
                        index,
                        LINE_PLACE,
                        "Place " + SortingSupport.formatElement(elements[index])
                                + " in digit bucket " + digit + " at output[" + outputIndex + "]",
                        StepEventType.EXECUTE_LINE,
                        SortingSupport.withFacts(digitFacts(
                                passFacts(place, passNumber, frequencies, buckets, output, null, SnapshotStatus.ACTIVE),
                                elements[index], place, outputIndex),
                                SortingSupport.fact("next-position", SortingSupport.formatPositions(positions, frequencies, 0), SnapshotStatus.ACTIVE)));
            }

            for (int index = 0; index < elements.length; index++) {
                elements[index] = output[index];
                SnapshotStatus status = index + 1 == elements.length
                        ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
                trace.add(
                        SortingSupport.donePrefixWithActiveSuffix(elements.length, index + 1),
                        index,
                        LINE_COPY_BACK,
                        "Copy output[" + index + "] into a[" + index + "] for digit pass " + passNumber,
                        StepEventType.EXECUTE_LINE,
                        passFacts(
                                place,
                                passNumber,
                                frequencies,
                                buckets,
                                output,
                                "a[" + index + "] <- output[" + index + "] ("
                                        + (index + 1) + "/" + elements.length + ")",
                                status));
            }

            int previousPlace = place;
            place *= BASE;
            passNumber++;
            trace.add(
                    SortingSupport.statuses(elements.length, SnapshotStatus.DEFAULT),
                    -1,
                    LINE_ADVANCE_PLACE,
                    "Digit pass is complete; advance place from " + previousPlace + " to " + place,
                    StepEventType.EXECUTE_LINE,
                    List.of(SortingSupport.fact("digit-pass", "next pass=" + passNumber + "; place=" + place, SnapshotStatus.ACTIVE),
                            SortingSupport.fact("place", Integer.toString(place), SnapshotStatus.ACTIVE),
                            SortingSupport.fact("key-offset", Integer.toString(OFFSET), SnapshotStatus.DEFAULT)));
        } while (place <= Math.max(1, maximumKey));

        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.DONE),
                -1,
                0,
                "Complete: array is sorted in nondecreasing order",
                StepEventType.COMPLETE);
        return trace.steps();
    }

    private static int transformed(int value) {
        return value + OFFSET;
    }

    private static int digit(int value, int place) {
        return (transformed(value) / place) % BASE;
    }

    private static List<List<SortingSupport.Element>> emptyBuckets() {
        List<List<SortingSupport.Element>> buckets = new ArrayList<>(BASE);
        for (int index = 0; index < BASE; index++) {
            buckets.add(new ArrayList<>());
        }
        return buckets;
    }

    private static List<Fact> countFacts(
            int place,
            int passNumber,
            int[] frequencies,
            int[] positions,
            SortingSupport.Element[] output,
            SnapshotStatus status) {
        List<Fact> facts = new ArrayList<>();
        facts.add(SortingSupport.fact("digit-pass", "pass=" + passNumber + "; place=" + place, status));
        facts.add(SortingSupport.fact("place", Integer.toString(place), status));
        facts.add(SortingSupport.fact("key-offset", Integer.toString(OFFSET), status));
        facts.add(SortingSupport.fact("frequency", SortingSupport.formatFrequency(frequencies, 0, BASE - 1), status));
        facts.add(SortingSupport.fact("digits", formatDigits(frequencies, positions), status));
        if (positions != null) {
            facts.add(SortingSupport.fact("next-position", SortingSupport.formatPositions(positions, frequencies, 0), status));
        }
        if (output != null) {
            facts.add(SortingSupport.fact("output", SortingSupport.formatOutput(output), status));
        }
        return List.copyOf(facts);
    }

    private static List<Fact> passFacts(
            int place,
            int passNumber,
            int[] frequencies,
            List<List<SortingSupport.Element>> buckets,
            SortingSupport.Element[] output,
            String copyBack,
            SnapshotStatus status) {
        List<Fact> facts = new ArrayList<>();
        facts.add(SortingSupport.fact("digit-pass", "pass=" + passNumber + "; place=" + place, status));
        facts.add(SortingSupport.fact("place", Integer.toString(place), status));
        facts.add(SortingSupport.fact("key-offset", Integer.toString(OFFSET), status));
        facts.add(SortingSupport.fact("frequency", SortingSupport.formatFrequency(frequencies, 0, BASE - 1), status));
        facts.add(SortingSupport.fact("buckets", SortingSupport.formatBuckets(buckets), status));
        facts.add(SortingSupport.fact("output", output == null ? "[]" : SortingSupport.formatOutput(output), status));
        if (copyBack != null) {
            facts.add(SortingSupport.fact("copy-back", copyBack, status));
        }
        return List.copyOf(facts);
    }

    private static List<Fact> digitFacts(
            List<Fact> facts, SortingSupport.Element element, int place, int outputIndex) {
        int key = transformed(element.value());
        int digit = digit(element.value(), place);
        return SortingSupport.withFacts(facts,
                SortingSupport.fact("current-value", Integer.toString(element.value()), SnapshotStatus.ACTIVE),
                SortingSupport.fact("shifted-key", Integer.toString(key), SnapshotStatus.ACTIVE),
                SortingSupport.fact("digit", Integer.toString(digit), SnapshotStatus.ACTIVE),
                SortingSupport.fact("output-index", Integer.toString(outputIndex), SnapshotStatus.ACTIVE),
                SortingSupport.fact("calculation", element.value() + " + " + OFFSET + " = " + key
                        + "; floor(" + key + " / " + place + ") % 10 = " + digit, SnapshotStatus.ACTIVE));
    }

    private static String formatDigits(int[] frequencies, int[] positions) {
        StringBuilder builder = new StringBuilder();
        builder.append("frequency=");
        appendCounts(builder, frequencies);
        if (positions != null) {
            builder.append("; end=");
            appendCounts(builder, positions);
        }
        return builder.toString();
    }

    private static void appendCounts(StringBuilder builder, int[] values) {
        builder.append('{');
        boolean first = true;
        for (int index = 0; index < values.length; index++) {
            if (values[index] == 0) {
                continue;
            }
            if (!first) {
                builder.append(", ");
            }
            builder.append(index).append('=').append(values[index]);
            first = false;
        }
        builder.append('}');
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode array = defaultInput.putArray("array");
        for (int value : DEFAULT_ARRAY) {
            array.add(value);
        }
        return new SimulationMetadata(
                TYPE,
                "Radix Sort",
                "O(d(n + 10))",
                "O(n + 10)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"array\":[170,45,75,90,802,24,2,66]}; signed integers and duplicates are allowed; "
                        + "stable decimal digit passes use an offset for the range [-" + MAX_ABS_VALUE + ", "
                        + MAX_ABS_VALUE + "]; array length must be 0 through " + MAX_ARRAY_LENGTH + ".",
                PSEUDOCODE);
    }
}
