package dev.codetrail.desktop.simulation.range;

import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TreeState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/** Shared bounded models and immutable snapshot builders for range engines. */
public final class RangeTreeSupport {
    public static final int MAX_TRACE_STEPS = 4096;

    private RangeTreeSupport() {
    }

    /**
     * Mutable segment-tree model used only while a complete trace is produced.
     * Every call to either trace builder turns its current contents into a
     * defensive immutable state snapshot.
     */
    public static final class SegmentTreeModel {
        private final int length;
        private final boolean minimum;
        private final int[] left;
        private final int[] right;
        private final int[] leftChild;
        private final int[] rightChild;
        private final boolean[] present;
        private final long[] tree;
        private final long[] lazy;
        private final long[] sourceValues;
        private final List<Integer> nodeOrder = new ArrayList<>();

        public SegmentTreeModel(int[] values, boolean minimum) {
            Objects.requireNonNull(values, "values");
            if (values.length < RangeInput.MIN_VALUES || values.length > RangeInput.MAX_VALUES) {
                throw new IllegalArgumentException("segment values length is outside the bounded range");
            }
            this.length = values.length;
            this.minimum = minimum;
            int capacity = 4 * values.length + 8;
            this.left = new int[capacity];
            this.right = new int[capacity];
            this.leftChild = new int[capacity];
            this.rightChild = new int[capacity];
            this.present = new boolean[capacity];
            Arrays.fill(this.leftChild, -1);
            Arrays.fill(this.rightChild, -1);
            this.tree = new long[capacity];
            this.lazy = new long[capacity];
            this.sourceValues = new long[values.length];
            for (int index = 0; index < values.length; index++) {
                if (Math.abs((long) values[index]) > RangeInput.MAX_ABS_VALUE) {
                    throw new IllegalArgumentException("segment values contain an out-of-bounds integer");
                }
                sourceValues[index] = values[index];
            }
            shape(1, 0, values.length - 1);
        }

        private void shape(int node, int low, int high) {
            left[node] = low;
            right[node] = high;
            present[node] = true;
            nodeOrder.add(node);
            if (low == high) {
                return;
            }
            int middle = low + (high - low) / 2;
            leftChild[node] = node * 2;
            rightChild[node] = node * 2 + 1;
            shape(leftChild[node], low, middle);
            shape(rightChild[node], middle + 1, high);
        }

        /** Build all tree aggregates from the original leaf values. */
        public void build() {
            build(1);
        }

        /** Write the source value for one leaf during an instructional build trace. */
        public void buildLeaf(int node) {
            checkNode(node);
            if (!isLeaf(node)) {
                throw new IllegalArgumentException("only leaves can be initialized directly");
            }
            tree[node] = sourceValues[left[node]];
        }

        private void build(int node) {
            if (isLeaf(node)) {
                tree[node] = sourceValues[left[node]];
                return;
            }
            build(leftChild[node]);
            build(rightChild[node]);
            recompute(node);
        }

        public int length() {
            return length;
        }

        public boolean minimum() {
            return minimum;
        }

        public int root() {
            return 1;
        }

        public int left(int node) {
            checkNode(node);
            return left[node];
        }

        public int right(int node) {
            checkNode(node);
            return right[node];
        }

        public int leftChild(int node) {
            checkNode(node);
            return leftChild[node];
        }

        public int rightChild(int node) {
            checkNode(node);
            return rightChild[node];
        }

        public boolean isLeaf(int node) {
            checkNode(node);
            return left[node] == right[node];
        }

        public long value(int node) {
            checkNode(node);
            return tree[node];
        }

        public long lazyTag(int node) {
            checkNode(node);
            return lazy[node];
        }

        public List<Integer> nodeOrder() {
            return List.copyOf(nodeOrder);
        }

        public String nodeId(int node) {
            checkNode(node);
            return nodeIdFor(node);
        }

        private static String nodeIdFor(int node) {
            return "node-" + node;
        }

        /** Change a leaf and update its ancestors for a point-set trace. */
        public void setLeaf(int node, long value) {
            checkNode(node);
            if (!isLeaf(node)) {
                throw new IllegalArgumentException("point-set value can only be written to a leaf");
            }
            tree[node] = value;
            sourceValues[left[node]] = value;
        }

        /** Recompute an internal node after its children changed. */
        public void recompute(int node) {
            checkNode(node);
            if (isLeaf(node)) {
                return;
            }
            tree[node] = combine(tree[leftChild[node]], tree[rightChild[node]]);
        }

        /** Apply a lazy range-add delta to one complete node interval. */
        public void applyLazy(int node, long delta) {
            checkNode(node);
            tree[node] += delta * (right[node] - left[node] + 1L);
            lazy[node] += delta;
        }

        /** Push a pending tag into both children and clear the parent tag. */
        public long pushLazy(int node) {
            checkNode(node);
            long pending = lazy[node];
            if (pending == 0L || isLeaf(node)) {
                return 0L;
            }
            applyLazy(leftChild[node], pending);
            applyLazy(rightChild[node], pending);
            lazy[node] = 0L;
            return pending;
        }

        private long combine(long first, long second) {
            return minimum ? Math.min(first, second) : first + second;
        }

        private void checkNode(int node) {
            if (node <= 0 || node >= left.length || !present[node]) {
                throw new IllegalArgumentException("unknown segment-tree node: " + node);
            }
        }
    }

    /** Mutable Fenwick values and one-based bit array used during trace production. */
    public static final class FenwickModel {
        private final int length;
        private final long[] values;
        private final long[] bit;

        public FenwickModel(int[] initialValues) {
            Objects.requireNonNull(initialValues, "initialValues");
            if (initialValues.length < RangeInput.MIN_VALUES
                    || initialValues.length > RangeInput.MAX_VALUES) {
                throw new IllegalArgumentException("Fenwick values length is outside the bounded range");
            }
            this.length = initialValues.length;
            this.values = new long[length];
            this.bit = new long[length + 1];
            for (int index = 0; index < length; index++) {
                if (Math.abs((long) initialValues[index]) > RangeInput.MAX_ABS_VALUE) {
                    throw new IllegalArgumentException("Fenwick values contain an out-of-bounds integer");
                }
                values[index] = initialValues[index];
            }
        }

        public int length() {
            return length;
        }

        public long value(int zeroIndex) {
            checkIndex(zeroIndex);
            return values[zeroIndex];
        }

        public long bitValue(int oneIndex) {
            checkBitIndex(oneIndex);
            return bit[oneIndex];
        }

        public long[] values() {
            return values.clone();
        }

        public long[] bitValues() {
            return bit.clone();
        }

        /** Add one initial leaf value to the internal bit array during build. */
        public void buildValue(int zeroIndex) {
            checkIndex(zeroIndex);
            addInternal(zeroIndex + 1, values[zeroIndex]);
        }

        /** Apply a public zero-based point delta. */
        public void add(int zeroIndex, long delta) {
            checkIndex(zeroIndex);
            values[zeroIndex] += delta;
            addInternal(zeroIndex + 1, delta);
        }

        /** Change only the public value; callers can then expose each BIT cell update. */
        public void changeValue(int zeroIndex, long delta) {
            checkIndex(zeroIndex);
            values[zeroIndex] += delta;
        }

        /** Apply one cell of a one-based BIT walk and leave the remaining walk to the caller. */
        public void addBitCell(int oneIndex, long delta) {
            checkBitIndex(oneIndex);
            bit[oneIndex] += delta;
        }

        private void addInternal(int oneIndex, long delta) {
            for (int position = oneIndex; position <= length; position += lowbit(position)) {
                bit[position] += delta;
            }
        }

        /** Return the inclusive prefix sum through a public zero-based index. */
        public long prefixSum(int zeroIndex) {
            checkIndex(zeroIndex);
            long answer = 0L;
            for (int position = zeroIndex + 1; position > 0; position -= lowbit(position)) {
                answer += bit[position];
            }
            return answer;
        }

        public static int lowbit(int oneIndex) {
            if (oneIndex <= 0) {
                throw new IllegalArgumentException("Fenwick internal index must be positive");
            }
            return oneIndex & -oneIndex;
        }

        private void checkIndex(int zeroIndex) {
            if (zeroIndex < 0 || zeroIndex >= length) {
                throw new IllegalArgumentException("Fenwick public index is out of range: " + zeroIndex);
            }
        }

        private void checkBitIndex(int oneIndex) {
            if (oneIndex <= 0 || oneIndex > length) {
                throw new IllegalArgumentException("Fenwick internal index is out of range: " + oneIndex);
            }
        }
    }

    /** Mutable-status builder that snapshots a segment tree as a TREE state. */
    public static final class TreeTraceBuilder {
        private final SegmentTreeModel model;
        private final SnapshotStatus[] statuses;
        private final Set<Integer> activeNodes = new LinkedHashSet<>();
        private final List<SimulationStep> steps = new ArrayList<>();

        public TreeTraceBuilder(SegmentTreeModel model) {
            this.model = Objects.requireNonNull(model, "model");
            this.statuses = new SnapshotStatus[model.nodeOrder().stream().mapToInt(Integer::intValue).max().orElse(1) + 1];
            resetStatuses();
        }

        public List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        public void resetStatuses() {
            Arrays.fill(statuses, SnapshotStatus.DEFAULT);
            activeNodes.clear();
        }

        public void status(int node, SnapshotStatus status) {
            model.nodeId(node);
            SnapshotStatus checked = Objects.requireNonNull(status, "status");
            statuses[node] = checked;
            if (checked == SnapshotStatus.ACTIVE) {
                activeNodes.add(node);
            } else {
                activeNodes.remove(node);
            }
        }

        public void activate(int node) {
            status(node, SnapshotStatus.ACTIVE);
        }

        public void done(int node) {
            status(node, SnapshotStatus.DONE);
        }

        public void reject(int node) {
            status(node, SnapshotStatus.REJECTED);
        }

        public void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("range tree trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            if (narration == null || narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(facts, "facts");
            List<Node> nodes = new ArrayList<>(model.nodeOrder().size());
            for (int node : model.nodeOrder()) {
                nodes.add(new Node(
                        model.nodeId(node),
                        "[" + model.left(node) + "," + model.right(node) + "] = " + model.value(node)
                                + (model.lazyTag(node) != 0 ? "; lazy=" + model.lazyTag(node) : ""),
                        statuses[node]));
            }
            List<Edge> edges = new ArrayList<>();
            Set<String> activeEdgeIds = new LinkedHashSet<>();
            for (int node : model.nodeOrder()) {
                if (!model.isLeaf(node)) {
                    int leftChild = model.leftChild(node);
                    int rightChild = model.rightChild(node);
                    String leftEdgeId = edgeId(node, leftChild);
                    String rightEdgeId = edgeId(node, rightChild);
                    edges.add(new Edge(
                            leftEdgeId,
                            model.nodeId(node),
                            model.nodeId(leftChild),
                            statuses[leftChild]));
                    edges.add(new Edge(
                            rightEdgeId,
                            model.nodeId(node),
                            model.nodeId(rightChild),
                            statuses[rightChild]));
                    if (statuses[leftChild] == SnapshotStatus.ACTIVE) {
                        activeEdgeIds.add(leftEdgeId);
                    }
                    if (statuses[rightChild] == SnapshotStatus.ACTIVE) {
                        activeEdgeIds.add(rightEdgeId);
                    }
                }
            }
            TreeState state = new TreeState(nodes, edges, model.nodeId(model.root()), facts);
            Set<String> activeIds = activeNodes.stream()
                    .map(node -> model.nodeId(node))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, activeIds, Set.copyOf(activeEdgeIds)),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private static String edgeId(int parent, int child) {
            return "edge-" + parent + "-" + child;
        }
    }

    /** Mutable-status builder that snapshots a Fenwick table as a TABLE state. */
    public static final class FenwickTraceBuilder {
        private final FenwickModel model;
        private final SnapshotStatus[] statuses;
        private final Set<Integer> activeRows = new LinkedHashSet<>();
        private final List<SimulationStep> steps = new ArrayList<>();

        public FenwickTraceBuilder(FenwickModel model) {
            this.model = Objects.requireNonNull(model, "model");
            this.statuses = new SnapshotStatus[model.length()];
            resetStatuses();
        }

        public List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        public void resetStatuses() {
            Arrays.fill(statuses, SnapshotStatus.DEFAULT);
            activeRows.clear();
        }

        public void status(int zeroIndex, SnapshotStatus status) {
            if (zeroIndex < 0 || zeroIndex >= statuses.length) {
                throw new IllegalArgumentException("Fenwick row index is out of range: " + zeroIndex);
            }
            SnapshotStatus checked = Objects.requireNonNull(status, "status");
            statuses[zeroIndex] = checked;
            if (checked == SnapshotStatus.ACTIVE) {
                activeRows.add(zeroIndex);
            } else {
                activeRows.remove(zeroIndex);
            }
        }

        public void activateOneBased(int oneIndex) {
            status(oneIndex - 1, SnapshotStatus.ACTIVE);
        }

        public void doneOneBased(int oneIndex) {
            status(oneIndex - 1, SnapshotStatus.DONE);
        }

        public void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("Fenwick trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            if (narration == null || narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(facts, "facts");
            List<String> columns = List.of("BIT index", "Binary", "Sum", "Covers input");
            List<List<TypedCell>> rows = new ArrayList<>(model.length());
            for (int zeroIndex = 0; zeroIndex < model.length(); zeroIndex++) {
                int oneIndex = zeroIndex + 1;
                SnapshotStatus status = statuses[zeroIndex];
                rows.add(List.of(
                        new TypedCell("bit-index-" + oneIndex, Integer.toString(oneIndex), status),
                        new TypedCell("bit-binary-" + oneIndex, binaryIndex(oneIndex, model.length()), status),
                        new TypedCell("bit-value-" + oneIndex, Long.toString(model.bitValue(oneIndex)), status),
                        new TypedCell("covers-" + oneIndex, bitRange(oneIndex, model.length()), status)));
            }
            TableState state = new TableState(columns, rows, facts);
            Set<String> activeIds = activeRows.stream()
                    .map(row -> "row-" + row)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, activeIds, Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }
    }

    public static String binaryIndex(int index, int length) {
        int width = Integer.toBinaryString(length * 2).length();
        return String.format("%" + width + "s", Integer.toBinaryString(index)).replace(' ', '0');
    }

    public static String formatValues(long[] values) {
        Objects.requireNonNull(values, "values");
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (long value : values) {
            joiner.add(Long.toString(value));
        }
        return joiner.toString();
    }

    public static String formatValues(int[] values) {
        Objects.requireNonNull(values, "values");
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (int value : values) {
            joiner.add(Integer.toString(value));
        }
        return joiner.toString();
    }

    public static String formatTree(SegmentTreeModel model) {
        Objects.requireNonNull(model, "model");
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (int node : model.nodeOrder()) {
            joiner.add(model.nodeId(node) + "[" + model.left(node) + "," + model.right(node) + "]= " + model.value(node));
        }
        return joiner.toString();
    }

    public static String formatLazyTags(SegmentTreeModel model) {
        Objects.requireNonNull(model, "model");
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (int node : model.nodeOrder()) {
            if (model.lazyTag(node) != 0L) {
                joiner.add(model.nodeId(node) + "[" + model.left(node) + "," + model.right(node) + "]+="
                        + model.lazyTag(node));
            }
        }
        return joiner.toString();
    }

    public static String bitRange(int oneBasedIndex, int length) {
        if (oneBasedIndex <= 0 || oneBasedIndex > length) {
            throw new IllegalArgumentException("Fenwick index is outside the table");
        }
        int low = oneBasedIndex - FenwickModel.lowbit(oneBasedIndex) + 1;
        return "[" + (low - 1) + ".." + (oneBasedIndex - 1) + "]";
    }

    public static String formatBitRanges(int length) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (int oneIndex = 1; oneIndex <= length; oneIndex++) {
            joiner.add(oneIndex + "->" + bitRange(oneIndex, length));
        }
        return joiner.toString();
    }

    public static Fact fact(String key, String value, SnapshotStatus status) {
        return new Fact(key, value, status);
    }
}
