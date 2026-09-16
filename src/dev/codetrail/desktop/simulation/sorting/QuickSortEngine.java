package dev.codetrail.desktop.simulation.sorting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.List;

/** Lomuto-partition quick-sort trace with explicit pivot and swap snapshots. */
public final class QuickSortEngine implements SimulationEngine {
    public static final String TYPE = "QUICK_SORT";
    public static final int MAX_ARRAY_LENGTH = SortingSupport.MAX_ARRAY_LENGTH;
    public static final int MAX_ABS_VALUE = SortingSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = 1024;

    private static final int LINE_ENTER = 1;
    private static final int LINE_CHECK_RANGE = 2;
    private static final int LINE_RETURN = 3;
    private static final int LINE_PIVOT = 4;
    private static final int LINE_BOUNDARY = 5;
    private static final int LINE_SCAN = 6;
    private static final int LINE_COMPARE = 7;
    private static final int LINE_SWAP_PARTITION = 8;
    private static final int LINE_ADVANCE_BOUNDARY = 9;
    private static final int LINE_SWAP_PIVOT = 10;
    private static final int LINE_LEFT_RECURSION = 11;
    private static final int LINE_RIGHT_RECURSION = 12;

    private static final int[] DEFAULT_ARRAY = {10, 7, 8, 9, 1, 5};
    private static final List<String> PSEUDOCODE = List.of(
            "quickSort(a, low, high):",
            "    if low >= high:",
            "        return",
            "    pivot = a[high]",
            "    boundary = low",
            "    for scan = low to high - 1:",
            "        if a[scan] <= pivot:",
            "            swap a[boundary] and a[scan]",
            "            boundary++",
            "    swap a[boundary] and a[high]",
            "    quickSort(a, low, boundary - 1)",
            "    quickSort(a, boundary + 1, high)");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<dev.codetrail.desktop.simulation.SimulationStep> generateSteps(JsonNode input) {
        int[] values = SortingSupport.readBoundedArray(input, TYPE);
        SortingSupport.Element[] elements = SortingSupport.elements(values);
        SortingSupport.Trace trace = new SortingSupport.Trace(elements, MAX_TRACE_STEPS);
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.DEFAULT),
                -1,
                0,
                "Initialize quick sort with " + elements.length + " array value(s)",
                StepEventType.INITIALIZE);
        if (elements.length > 0) {
            quickSort(elements, 0, elements.length - 1, new boolean[elements.length], trace);
        }
        trace.add(
                SortingSupport.statuses(elements.length, SnapshotStatus.DONE),
                -1,
                0,
                "Complete: array is sorted in nondecreasing order",
                StepEventType.COMPLETE);
        return trace.steps();
    }

    private static void quickSort(
            SortingSupport.Element[] elements,
            int low,
            int high,
            boolean[] fixed,
            SortingSupport.Trace trace) {
        trace.add(
                rangeStatuses(fixed, low, high),
                low == high ? low : -1,
                LINE_ENTER,
                "Enter quickSort on range [" + low + ", " + high + "]",
                StepEventType.EXECUTE_LINE);
        trace.add(
                rangeStatuses(fixed, low, high),
                low == high ? low : -1,
                LINE_CHECK_RANGE,
                "Check whether range [" + low + ", " + high + "] has at most one value",
                StepEventType.EXECUTE_LINE);
        if (low >= high) {
            if (low == high) {
                fixed[low] = true;
            }
            trace.add(
                    rangeStatuses(fixed, low, high),
                    focusIndex(elements.length, low, high),
                    LINE_RETURN,
                    "Return the empty or one-value range [" + low + ", " + high + "]",
                    StepEventType.EXECUTE_LINE);
            return;
        }

        SortingSupport.Element pivot = elements[high];
        int boundary = low;
        trace.add(
                partitionStatuses(fixed, -1, high),
                high,
                LINE_PIVOT,
                "Choose pivot " + SortingSupport.formatElement(pivot) + " at index " + high,
                StepEventType.EXECUTE_LINE,
                partitionFacts(pivot, low, high, boundary, -1, "pivot selected", SnapshotStatus.ACTIVE));
        trace.add(
                partitionStatuses(fixed, -1, high),
                boundary,
                LINE_BOUNDARY,
                "Set partition boundary to index " + boundary,
                StepEventType.EXECUTE_LINE,
                partitionFacts(pivot, low, high, boundary, -1, "partition begins", SnapshotStatus.ACTIVE));
        trace.add(
                partitionStatuses(fixed, -1, high),
                low,
                LINE_SCAN,
                "Scan indices " + low + " through " + (high - 1) + " for the low partition",
                StepEventType.EXECUTE_LINE,
                partitionFacts(pivot, low, high, boundary, -1, "scan loop", SnapshotStatus.ACTIVE));

        for (int scan = low; scan < high; scan++) {
            trace.add(
                    partitionStatuses(fixed, scan, high),
                    scan,
                    LINE_COMPARE,
                    "Compare " + elements[scan].value() + " with pivot " + pivot.value(),
                    StepEventType.EXECUTE_LINE,
                    partitionFacts(pivot, low, high, boundary, scan, "compare", SnapshotStatus.ACTIVE));
            if (elements[scan].value() <= pivot.value()) {
                if (boundary != scan) {
                    swap(elements, boundary, scan);
                    trace.add(
                            partitionStatuses(fixed, scan, high),
                            boundary,
                            LINE_SWAP_PARTITION,
                            "Swap indices " + boundary + " and " + scan + " into the low partition",
                            StepEventType.EXECUTE_LINE,
                            partitionFacts(pivot, low, high, boundary, scan, "partition swap", SnapshotStatus.ACTIVE));
                } else {
                    trace.add(
                            partitionStatuses(fixed, scan, high),
                            scan,
                            LINE_SWAP_PARTITION,
                            "Keep index " + scan + " in place; the low partition needs no swap",
                            StepEventType.EXECUTE_LINE,
                            partitionFacts(pivot, low, high, boundary, scan, "partition position kept", SnapshotStatus.ACTIVE));
                }
                boundary++;
                trace.add(
                        partitionStatuses(fixed, scan, high),
                        boundary - 1,
                        LINE_ADVANCE_BOUNDARY,
                        "Advance boundary to index " + boundary,
                        StepEventType.EXECUTE_LINE,
                        partitionFacts(pivot, low, high, boundary, scan, "low partition grows", SnapshotStatus.ACTIVE));
            }
        }

        if (boundary != high) {
            swap(elements, boundary, high);
        }
        fixed[boundary] = true;
        trace.add(
                rangeStatuses(fixed, -1, -1),
                boundary,
                LINE_SWAP_PIVOT,
                "Place pivot " + pivot.value() + " at final index " + boundary,
                StepEventType.EXECUTE_LINE,
                partitionFacts(pivot, low, high, boundary, -1, "pivot placed", SnapshotStatus.DONE));

        trace.add(
                rangeStatuses(fixed, low, high),
                focusIndex(elements.length, boundary - 1, low),
                LINE_LEFT_RECURSION,
                "Recurse into left partition [" + low + ", " + (boundary - 1) + "]",
                StepEventType.EXECUTE_LINE);
        if (low <= boundary - 1) {
            quickSort(elements, low, boundary - 1, fixed, trace);
        }
        trace.add(
                rangeStatuses(fixed, low, high),
                focusIndex(elements.length, boundary + 1, high),
                LINE_RIGHT_RECURSION,
                "Recurse into right partition [" + (boundary + 1) + ", " + high + "]",
                StepEventType.EXECUTE_LINE);
        if (boundary + 1 <= high) {
            quickSort(elements, boundary + 1, high, fixed, trace);
        }
    }

    private static void swap(SortingSupport.Element[] elements, int first, int second) {
        SortingSupport.Element temporary = elements[first];
        elements[first] = elements[second];
        elements[second] = temporary;
    }

    private static int focusIndex(int size, int preferred, int fallback) {
        if (preferred >= 0 && preferred < size) {
            return preferred;
        }
        return fallback >= 0 && fallback < size ? fallback : -1;
    }

    private static SnapshotStatus[] rangeStatuses(boolean[] fixed, int low, int high) {
        SnapshotStatus[] statuses = SortingSupport.statuses(fixed.length, SnapshotStatus.DEFAULT);
        for (int index = 0; index < fixed.length; index++) {
            if (fixed[index]) {
                statuses[index] = SnapshotStatus.DONE;
            } else if (index >= low && index <= high) {
                statuses[index] = SnapshotStatus.ACTIVE;
            }
        }
        return statuses;
    }

    private static SnapshotStatus[] partitionStatuses(
            boolean[] fixed,
            int scan,
            int pivot) {
        int size = fixed.length;
        SnapshotStatus[] statuses = rangeStatuses(fixed, -1, -1);
        if (scan >= 0 && scan < size) {
            statuses[scan] = SnapshotStatus.ACTIVE;
        }
        if (pivot >= 0 && pivot < size) {
            statuses[pivot] = SnapshotStatus.ACTIVE;
        }
        return statuses;
    }

    private static List<Fact> partitionFacts(
            SortingSupport.Element pivot,
            int low,
            int high,
            int boundary,
            int scan,
            String phase,
            SnapshotStatus status) {
        return List.of(
                SortingSupport.fact("pivot", SortingSupport.formatElement(pivot), status),
                SortingSupport.fact("partition", "[" + low + ".." + high + "] boundary=" + boundary, status),
                SortingSupport.fact("scan", scan < 0 ? phase : phase + " index=" + scan, status),
                SortingSupport.fact("phase", phase, status),
                SortingSupport.fact("low", Integer.toString(low), status),
                SortingSupport.fact("high", Integer.toString(high), status),
                SortingSupport.fact("boundary", Integer.toString(boundary), status),
                SortingSupport.fact("scan-index", Integer.toString(scan), status),
                SortingSupport.fact("pivot-index", Integer.toString(phase.equals("pivot placed") ? boundary : high), status),
                SortingSupport.fact("partition-rule", phase.equals("pivot placed")
                        ? "Only the pivot is final; sort both remaining partitions"
                        : "[" + low + ", " + boundary + ") contains values <= " + pivot.value(), status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode array = input.putArray("array");
        for (int value : DEFAULT_ARRAY) {
            array.add(value);
        }
        return new SimulationMetadata(
                TYPE,
                "Quick Sort",
                "O(n log n) average; O(n²) worst",
                "O(n) worst recursion stack",
                RendererFamily.ARRAY,
                input,
                "Enter JSON as {\"array\":[10,7,8,9,1,5]}; signed integers and duplicates are allowed; "
                        + "array length must be 0 through "
                        + MAX_ARRAY_LENGTH + " and every integer must have absolute value at most " + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }
}
