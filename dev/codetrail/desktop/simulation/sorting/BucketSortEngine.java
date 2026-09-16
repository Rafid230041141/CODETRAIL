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

/** Stable bucket-sort trace with visible distribution, insertion, and copy-back phases. */
public final class BucketSortEngine implements SimulationEngine {
    public static final String TYPE = "BUCKET_SORT";
    public static final int MAX_ARRAY_LENGTH = SortingSupport.MAX_ARRAY_LENGTH;
    public static final int MAX_ABS_VALUE = SortingSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = 1024;

    private static final int LINE_ENTER = 1;
    private static final int LINE_CHECK_RANGE = 2;
    private static final int LINE_RETURN = 3;
    private static final int LINE_RANGE = 4;
    private static final int LINE_CREATE_BUCKETS = 5;
    private static final int LINE_DISTRIBUTE = 6;
    private static final int LINE_BUCKET = 7;
    private static final int LINE_INSERTION = 8;
    private static final int LINE_SHIFT = 9;
    private static final int LINE_INSERT = 10;
    private static final int LINE_CONCATENATE = 11;
    private static final int LINE_COPY_BACK = 12;

    private static final int[] DEFAULT_ARRAY = {42, 32, 33, 52, 37, 47, 51};
    private static final List<String> PSEUDOCODE = List.of(
            "bucketSort(a):",
            "    if length <= 1:",
            "        return",
            "    min = minimum(a); max = maximum(a)",
            "    create k buckets spanning [min, max]",
            "    for each value: distribute it into a bucket",
            "    for each bucket:",
            "        insertion-sort the bucket",
            "            shift larger values right",
            "            insert the key (keep equal keys in order)",
            "    concatenate buckets into output",
            "    copy output back into a");
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
                "Initialize bucket sort with " + elements.length + " array value(s)",
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
        int bucketCount = Math.max(1, Math.min(8, (int) Math.ceil(Math.sqrt(elements.length))));
        List<List<SortingSupport.Element>> buckets = emptyBuckets(bucketCount);
        List<SortingSupport.Element> output = new ArrayList<>(elements.length);

        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_ENTER,
                "Enter bucketSort for " + elements.length + " values",
                StepEventType.EXECUTE_LINE,
                bucketFacts(minimum, maximum, buckets, output, -1, "range setup", SnapshotStatus.ACTIVE));
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_CHECK_RANGE,
                "Check that the array has more than one value",
                StepEventType.EXECUTE_LINE,
                bucketFacts(minimum, maximum, buckets, output, -1, "range check", SnapshotStatus.ACTIVE));
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_RANGE,
                "Use value range [" + minimum + ", " + maximum + "]",
                StepEventType.EXECUTE_LINE,
                bucketFacts(minimum, maximum, buckets, output, -1, "range selected", SnapshotStatus.ACTIVE));
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                -1,
                LINE_CREATE_BUCKETS,
                "Create " + bucketCount + " buckets spanning the value range",
                StepEventType.EXECUTE_LINE,
                bucketFacts(minimum, maximum, buckets, output, -1, "buckets created", SnapshotStatus.ACTIVE));

        for (int index = 0; index < elements.length; index++) {
            int bucket = bucketIndex(elements[index].value(), minimum, maximum, bucketCount);
            buckets.get(bucket).add(elements[index]);
            trace.add(
                    SortingSupport.activePositions(elements.length, index),
                    index,
                    LINE_DISTRIBUTE,
                    "Distribute " + SortingSupport.formatElement(elements[index])
                            + " into bucket " + bucket + " using floor((value - " + minimum
                            + ") * " + bucketCount + " / " + (maximum - minimum + 1) + ")",
                    StepEventType.EXECUTE_LINE,
                    bucketFacts(
                            minimum,
                            maximum,
                            buckets,
                            output,
                            bucket,
                            "distribution index=" + index,
                            SnapshotStatus.ACTIVE));
        }

        for (int bucketIndex = 0; bucketIndex < buckets.size(); bucketIndex++) {
            List<SortingSupport.Element> current = buckets.get(bucketIndex);
            trace.add(
                    SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                    -1,
                    LINE_BUCKET,
                    "Sort bucket " + bucketIndex + " containing " + current.size() + " value(s)",
                    StepEventType.EXECUTE_LINE,
                    bucketFacts(
                            minimum,
                            maximum,
                            buckets,
                            output,
                            bucketIndex,
                            "bucket selected",
                            SnapshotStatus.ACTIVE));
            for (int index = 1; index < current.size(); index++) {
                SortingSupport.Element key = current.get(index);
                int position = index;
                trace.add(
                        SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                        -1,
                        LINE_INSERTION,
                        "Insert " + SortingSupport.formatElement(key)
                                + " within bucket " + bucketIndex,
                        StepEventType.EXECUTE_LINE,
                        SortingSupport.withFacts(bucketFacts(
                                minimum,
                                maximum,
                                buckets,
                                output,
                                bucketIndex,
                                "insertion key=" + SortingSupport.formatElement(key),
                                SnapshotStatus.ACTIVE),
                                SortingSupport.fact("insertion-key", SortingSupport.formatElement(key), SnapshotStatus.ACTIVE),
                                SortingSupport.fact("insertion-position", Integer.toString(position), SnapshotStatus.ACTIVE)));
                while (position > 0 && current.get(position - 1).value() > key.value()) {
                    current.set(position, current.get(position - 1));
                    position--;
                    trace.add(
                            SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                            -1,
                            LINE_SHIFT,
                            "Shift a larger value right inside bucket " + bucketIndex,
                            StepEventType.EXECUTE_LINE,
                            SortingSupport.withFacts(bucketFacts(
                                    minimum,
                                    maximum,
                                    buckets,
                                    output,
                                    bucketIndex,
                                    "shift position=" + position,
                                    SnapshotStatus.ACTIVE),
                                SortingSupport.fact("insertion-key", SortingSupport.formatElement(key), SnapshotStatus.ACTIVE),
                                SortingSupport.fact("insertion-position", Integer.toString(position), SnapshotStatus.ACTIVE)));
                }
                current.set(position, key);
                trace.add(
                        SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                        -1,
                        LINE_INSERT,
                        "Insert key at bucket " + bucketIndex + " position " + position,
                        StepEventType.EXECUTE_LINE,
                        SortingSupport.withFacts(bucketFacts(
                                minimum,
                                maximum,
                                buckets,
                                output,
                                bucketIndex,
                                "bucket insertion complete",
                                SnapshotStatus.ACTIVE),
                                SortingSupport.fact("insertion-key", SortingSupport.formatElement(key), SnapshotStatus.ACTIVE),
                                SortingSupport.fact("insertion-position", Integer.toString(position), SnapshotStatus.ACTIVE)));
            }
        }

        for (int bucketIndex = 0; bucketIndex < buckets.size(); bucketIndex++) {
            for (SortingSupport.Element element : buckets.get(bucketIndex)) {
                output.add(element);
                trace.add(
                        SortingSupport.statuses(elements.length, SnapshotStatus.ACTIVE),
                        -1,
                        LINE_CONCATENATE,
                        "Append " + SortingSupport.formatElement(element)
                                + " from bucket " + bucketIndex + " to output",
                        StepEventType.EXECUTE_LINE,
                        bucketFacts(
                                minimum,
                                maximum,
                                buckets,
                                output,
                                bucketIndex,
                                "concatenate",
                                SnapshotStatus.ACTIVE));
            }
        }

        SortingSupport.Element[] outputArray = output.toArray(new SortingSupport.Element[0]);
        for (int index = 0; index < outputArray.length; index++) {
            elements[index] = outputArray[index];
            SnapshotStatus status = index + 1 == outputArray.length
                    ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
            trace.add(
                    SortingSupport.donePrefixWithActiveSuffix(elements.length, index + 1),
                    index,
                    LINE_COPY_BACK,
                    "Copy output[" + index + "] into a[" + index + "]",
                    StepEventType.EXECUTE_LINE,
                    bucketFacts(
                            minimum,
                            maximum,
                            buckets,
                            output,
                            -1,
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

    private static List<List<SortingSupport.Element>> emptyBuckets(int count) {
        List<List<SortingSupport.Element>> buckets = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            buckets.add(new ArrayList<>());
        }
        return buckets;
    }

    private static int bucketIndex(int value, int minimum, int maximum, int bucketCount) {
        long span = (long) maximum - minimum + 1L;
        long numerator = ((long) value - minimum) * bucketCount;
        int index = (int) (numerator / span);
        return Math.min(bucketCount - 1, Math.max(0, index));
    }

    private static String bucketRanges(int minimum, int maximum, int count) {
        long span = (long) maximum - minimum + 1;
        java.util.StringJoiner ranges = new java.util.StringJoiner("; ");
        for (int bucket = 0; bucket < count; bucket++) {
            long first = minimum + (bucket * span + count - 1) / count;
            long last = minimum + ((bucket + 1) * span + count - 1) / count - 1;
            ranges.add(bucket + ":" + (first <= last ? "[" + first + ".." + last + "]" : "empty"));
        }
        return ranges.toString();
    }

    private static List<Fact> bucketFacts(
            int minimum,
            int maximum,
            List<List<SortingSupport.Element>> buckets,
            List<SortingSupport.Element> output,
            int activeBucket,
            String phase,
            SnapshotStatus status) {
        List<Fact> facts = new ArrayList<>();
        if (phase.startsWith("a[")) {
            facts.add(SortingSupport.fact("copy-back", phase, status));
        } else {
            facts.add(SortingSupport.fact("range", minimum + ".." + maximum + "; " + phase, status));
        }
        facts.add(SortingSupport.fact("buckets", SortingSupport.formatBuckets(buckets), status));
        facts.add(SortingSupport.fact("bucket-ranges", bucketRanges(minimum, maximum, buckets.size()), status));
        facts.add(SortingSupport.fact("bucket-index", Integer.toString(activeBucket), status));
        facts.add(SortingSupport.fact(
                "active-bucket",
                activeBucket < 0 ? phase : "bucket " + activeBucket + "; " + phase,
                status));
        facts.add(SortingSupport.fact("output", SortingSupport.formatElements(output), status));
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
                "Bucket Sort",
                "O(n + k) average; O(n²) worst",
                "O(n + k)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"array\":[42,32,33,52,37,47,51]}; signed integers and duplicates are allowed; "
                        + "array length must be 0 through " + MAX_ARRAY_LENGTH
                        + " and every integer must have absolute value at most " + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }
}
