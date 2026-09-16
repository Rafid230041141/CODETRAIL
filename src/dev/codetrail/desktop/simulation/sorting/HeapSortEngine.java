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
import java.util.Arrays;
import java.util.List;

/** In-place max-heap construction and extraction trace. */
public final class HeapSortEngine implements SimulationEngine {
    public static final String TYPE = "HEAP_SORT";
    public static final int MAX_ARRAY_LENGTH = SortingSupport.MAX_ARRAY_LENGTH;
    public static final int MAX_ABS_VALUE = SortingSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = 1024;

    private static final int LINE_ENTER = 1;
    private static final int LINE_CHECK_RANGE = 2;
    private static final int LINE_RETURN = 3;
    private static final int LINE_BUILD_HEAP = 4;
    private static final int LINE_SIFT_BUILD = 5;
    private static final int LINE_EXTRACT = 6;
    private static final int LINE_SWAP_ROOT = 7;
    private static final int LINE_SIFT_EXTRACT = 8;
    private static final int LINE_SIFT = 9;
    private static final int LINE_WHILE = 10;
    private static final int LINE_CHILD = 11;
    private static final int LINE_COMPARE_CHILDREN = 12;
    private static final int LINE_ADVANCE_CHILD = 13;
    private static final int LINE_CHECK_PARENT = 14;
    private static final int LINE_SWAP_CHILD = 15;
    private static final int LINE_ADVANCE_ROOT = 16;

    private static final int[] DEFAULT_ARRAY = {4, 10, 3, 5, 1};
    private static final List<String> PSEUDOCODE = List.of(
            "heapSort(a):",
            "    if length <= 1:",
            "        return",
            "    for root = floor(n / 2) - 1 down to 0:",
            "        siftDown(a, root, n)",
            "    for end = n - 1 down to 1:",
            "        swap a[0] and a[end]",
            "        siftDown(a, 0, end)",
            "siftDown(a, root, end):",
            "    while 2 * root + 1 < end:",
            "        child = 2 * root + 1",
            "        if child + 1 < end and a[child] < a[child + 1]:",
            "            child++",
            "        if a[root] >= a[child]: return",
            "        swap a[root] and a[child]",
            "        root = child");
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
                "Initialize heap sort with " + elements.length + " array value(s)",
                StepEventType.INITIALIZE);
        if (elements.length > 1) {
            heapSort(elements, trace);
        } else if (elements.length == 1) {
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

    private static void heapSort(SortingSupport.Element[] elements, SortingSupport.Trace trace) {
        int size = elements.length;
        trace.add(
                SortingSupport.statuses(size, SnapshotStatus.ACTIVE),
                -1,
                LINE_ENTER,
                "Enter heapSort for " + size + " values",
                StepEventType.EXECUTE_LINE);
        trace.add(
                SortingSupport.statuses(size, SnapshotStatus.ACTIVE),
                -1,
                LINE_CHECK_RANGE,
                "Check that the array has more than one value",
                StepEventType.EXECUTE_LINE);

        for (int root = size / 2 - 1; root >= 0; root--) {
            trace.add(
                    heapStatuses(size, size, root, -1),
                    root,
                    LINE_BUILD_HEAP,
                    "Heapify subtree rooted at index " + root,
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, size, root, -1, "building max heap", SnapshotStatus.ACTIVE));
            trace.add(
                    heapStatuses(size, size, root, -1),
                    root,
                    LINE_SIFT_BUILD,
                    "Sift down root " + root + " inside the active heap",
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, size, root, -1, "heapify", SnapshotStatus.ACTIVE));
            siftDown(elements, root, size, trace, "heapify");
        }

        for (int end = size - 1; end >= 1; end--) {
            trace.add(
                    heapStatuses(size, end + 1, 0, end),
                    0,
                    LINE_EXTRACT,
                    "Extract the maximum to index " + end,
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end + 1, 0, end, "extracting maximum", SnapshotStatus.ACTIVE));
            swap(elements, 0, end);
            trace.add(
                    heapStatuses(size, end, 0, end),
                    0,
                    LINE_SWAP_ROOT,
                    "Swap root maximum into sorted suffix index " + end,
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end, 0, end, "maximum placed", SnapshotStatus.DONE));
            trace.add(
                    heapStatuses(size, end, 0, -1),
                    0,
                    LINE_SIFT_EXTRACT,
                    "Restore the max heap in range [0, " + end + ")",
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end, 0, -1, "sifting after extraction", SnapshotStatus.ACTIVE));
            siftDown(elements, 0, end, trace, "extract");
        }
    }

    private static void siftDown(
            SortingSupport.Element[] elements,
            int root,
            int end,
            SortingSupport.Trace trace,
            String phase) {
        while (2 * root + 1 < end) {
            trace.add(
                    heapStatuses(elements.length, end, root, -1),
                    root,
                    LINE_SIFT,
                    "Sift down root " + root + " within heap end " + end,
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end, root, -1, phase, SnapshotStatus.ACTIVE));
            trace.add(
                    heapStatuses(elements.length, end, root, -1),
                    root,
                    LINE_WHILE,
                    "Check whether root " + root + " has a child",
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end, root, -1, phase + "; checking children", SnapshotStatus.ACTIVE));
            int child = 2 * root + 1;
            trace.add(
                    heapStatuses(elements.length, end, root, child),
                    child,
                    LINE_CHILD,
                    "Choose left child index " + child,
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end, root, child, phase + "; left child", SnapshotStatus.ACTIVE));
            if (child + 1 < end) {
                trace.add(
                        heapStatuses(elements.length, end, child, child + 1),
                        child + 1,
                        LINE_COMPARE_CHILDREN,
                        "Compare children " + child + " and " + (child + 1),
                        StepEventType.EXECUTE_LINE,
                        heapFacts(elements, end, root, child, phase + "; choose larger child", SnapshotStatus.ACTIVE));
                if (elements[child].value() < elements[child + 1].value()) {
                    child++;
                    trace.add(
                            heapStatuses(elements.length, end, root, child),
                            child,
                            LINE_ADVANCE_CHILD,
                            "Advance to larger child index " + child,
                            StepEventType.EXECUTE_LINE,
                            heapFacts(elements, end, root, child, phase + "; right child selected", SnapshotStatus.ACTIVE));
                }
            }
            trace.add(
                    heapStatuses(elements.length, end, root, child),
                    root,
                    LINE_CHECK_PARENT,
                    "Compare parent " + elements[root].value() + " with child " + elements[child].value(),
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end, root, child, phase + "; parent check", SnapshotStatus.ACTIVE));
            if (elements[root].value() >= elements[child].value()) {
                trace.add(
                        heapStatuses(elements.length, end, root, child),
                        root,
                        LINE_CHECK_PARENT,
                        "Max-heap order holds; stop sifting",
                        StepEventType.EXECUTE_LINE,
                        heapFacts(elements, end, root, child, phase + "; heap order holds", SnapshotStatus.DONE));
                return;
            }
            swap(elements, root, child);
            trace.add(
                    heapStatuses(elements.length, end, root, child),
                    child,
                    LINE_SWAP_CHILD,
                    "Swap parent index " + root + " with child index " + child,
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end, root, child, phase + "; sift swap", SnapshotStatus.ACTIVE));
            root = child;
            trace.add(
                    heapStatuses(elements.length, end, root, -1),
                    root,
                    LINE_ADVANCE_ROOT,
                    "Continue sifting from index " + root,
                    StepEventType.EXECUTE_LINE,
                    heapFacts(elements, end, root, -1, phase + "; continue", SnapshotStatus.ACTIVE));
        }
        trace.add(
                heapStatuses(elements.length, end, root, -1),
                root < elements.length ? root : -1,
                LINE_WHILE,
                "No child remains before heap end " + end,
                StepEventType.EXECUTE_LINE,
                heapFacts(elements, end, root, -1, phase + "; sift complete", SnapshotStatus.DONE));
    }

    private static SnapshotStatus[] heapStatuses(int size, int heapEnd, int firstActive, int secondActive) {
        SnapshotStatus[] statuses = SortingSupport.statuses(size, SnapshotStatus.DEFAULT);
        if (heapEnd < size) {
            Arrays.fill(statuses, heapEnd, size, SnapshotStatus.DONE);
        }
        if (firstActive >= 0 && firstActive < size && firstActive < heapEnd) {
            statuses[firstActive] = SnapshotStatus.ACTIVE;
        }
        if (secondActive >= 0 && secondActive < size && secondActive < heapEnd) {
            statuses[secondActive] = SnapshotStatus.ACTIVE;
        }
        return statuses;
    }

    private static List<Fact> heapFacts(
            SortingSupport.Element[] elements,
            int heapEnd,
            int root,
            int child,
            String phase,
            SnapshotStatus status) {
        String rootValue = root >= 0 && root < elements.length && root < heapEnd
                ? SortingSupport.formatElement(elements[root])
                : "none";
        String childValue = child >= 0 && child < elements.length && child < heapEnd
                ? SortingSupport.formatElement(elements[child])
                : "none";
        return List.of(
                SortingSupport.fact("heap-boundary", "active [0.." + heapEnd + ")", status),
                SortingSupport.fact("root", rootValue, status),
                SortingSupport.fact("child", childValue, status),
                SortingSupport.fact("phase", phase, status),
                SortingSupport.fact("heap-end", Integer.toString(heapEnd), status),
                SortingSupport.fact("root-index", Integer.toString(root < heapEnd ? root : -1), status),
                SortingSupport.fact("child-index", Integer.toString(child >= 0 && child < heapEnd ? child : -1), status),
                SortingSupport.fact("left-child-index", Integer.toString(2 * root + 1 < heapEnd ? 2 * root + 1 : -1), status),
                SortingSupport.fact("right-child-index", Integer.toString(2 * root + 2 < heapEnd ? 2 * root + 2 : -1), status));
    }

    private static void swap(SortingSupport.Element[] elements, int first, int second) {
        SortingSupport.Element temporary = elements[first];
        elements[first] = elements[second];
        elements[second] = temporary;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode array = defaultInput.putArray("array");
        for (int value : DEFAULT_ARRAY) {
            array.add(value);
        }
        return new SimulationMetadata(
                TYPE,
                "Heap Sort",
                "O(n log n)",
                "O(1)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"array\":[4,10,3,5,1]}; signed integers and duplicates are allowed; "
                        + "array length must be 0 through "
                        + MAX_ARRAY_LENGTH + " and every integer must have absolute value at most " + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }
}
