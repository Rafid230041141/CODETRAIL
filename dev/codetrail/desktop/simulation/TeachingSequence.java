package dev.codetrail.desktop.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** A teaching view over original immutable trace indices; it never rewrites a step. */
public final class TeachingSequence {
    private final List<Integer> indices;

    private TeachingSequence(List<Integer> indices) {
        this.indices = List.copyOf(indices);
    }

    public static TeachingSequence of(SimulationTrace trace) {
        Objects.requireNonNull(trace, "trace");
        String type = trace.metadata().type();
        Set<Integer> lines = teachingLines(type);
        int first = preparedIndex(trace, type);
        ArrayList<Integer> selected = new ArrayList<>();
        selected.add(first);
        SimulationStep previous = trace.stepAt(first);
        for (int index = first + 1; index < trace.size() - 1; index++) {
            SimulationStep step = trace.stepAt(index);
            boolean semanticEvent = step.eventType() == StepEventType.PUSH_FRAME
                    || step.eventType() == StepEventType.RETURN_FRAME;
            if (!semanticEvent && !lines.contains(step.highlightedLine())) continue;
            if (!meaningfulDecision(type, step, trace, index)) continue;
            if (step.highlightedLine() == previous.highlightedLine()
                    && step.eventType() == previous.eventType()
                    && step.snapshot().equals(previous.snapshot())) continue;
            selected.add(index);
            previous = step;
        }
        if (selected.get(selected.size() - 1) != trace.size() - 1) selected.add(trace.size() - 1);
        return new TeachingSequence(selected);
    }

    public int firstIndex() { return indices.get(0); }
    public int lastIndex() { return indices.get(indices.size() - 1); }
    public int size() { return indices.size(); }
    public List<Integer> indices() { return indices; }

    /** Next strictly later original index, clamped at the final checkpoint. */
    public int nextIndex(int originalIndex) {
        int found = Collections.binarySearch(indices, originalIndex);
        int next = found >= 0 ? found + 1 : -found - 1;
        return indices.get(Math.min(next, indices.size() - 1));
    }

    /** Previous strictly earlier original index, clamped at the first checkpoint. */
    public int previousIndex(int originalIndex) {
        int found = Collections.binarySearch(indices, originalIndex);
        int previous = found >= 0 ? found - 1 : -found - 2;
        return indices.get(Math.max(previous, 0));
    }

    /** One-based checkpoint ordinal at or before an arbitrary original index. */
    public int ordinal(int originalIndex) {
        int found = Collections.binarySearch(indices, originalIndex);
        return Math.max(1, Math.min(indices.size(), found >= 0 ? found + 1 : -found - 1));
    }

    private static int preparedIndex(SimulationTrace trace, String type) {
        return switch (type) {
            case "BFS" -> firstLine(trace, 5);
            case "DFS" -> firstLine(trace, 5);
            case "DIJKSTRA" -> firstLine(trace, 5);
            case "BELLMAN_FORD" -> firstLine(trace, 4);
            case "FLOYD_WARSHALL" -> firstLine(trace, 7);
            case "KRUSKAL_MST" -> firstLine(trace, 4);
            case "PRIM_MST" -> firstLine(trace, 7);
            case "TOPOLOGICAL_SORT" -> firstLine(trace, 5);
            case "MAX_FLOW" -> firstLine(trace, 2);
            case "BRIDGES_ARTICULATION" -> firstLine(trace, 2);
            case "BST", "AVL_TREE", "TRIE", "HASH_MAP" -> finalOperationStart(trace);
            case "FENWICK_TREE" -> firstPhase(trace, Set.of("update", "query"));
            case "SEGMENT_TREE" -> firstLine(trace, 6, 11);
            case "SEGMENT_TREE_LAZY" -> firstLine(trace, 6, 11);
            case "PREFIX_SUM" -> firstLine(trace, 5);
            case "SPARSE_TABLE" -> firstLine(trace, 6);
            case "SQRT_DECOMPOSITION" -> firstPhase(trace, Set.of("query", "update"));
            // Begin the requested insertion/extraction itself, after heap construction has finished.
            case "HEAP" -> firstLine(trace, 4, 7);
            case "BINARY_SEARCH" -> firstLine(trace, 2);
            case "BINARY_SEARCH_ANSWER" -> firstLine(trace, 2);
            case "KMP" -> firstLine(trace, 10);
            case "STRING_HASHING" -> firstLine(trace, 5);
            case "MODULAR_EXPONENTIATION" -> firstLine(trace, 3);
            case "EXTENDED_GCD" -> firstLine(trace, 3);
            case "SIEVE_OF_ERATOSTHENES" -> firstLine(trace, 4);
            case "DYNAMIC_PROGRAMMING" -> firstLine(trace, 2);
            case "GAUSSIAN_ELIMINATION" -> firstLine(trace, 2);
            case "RECURSION", "MERGE_SORT", "QUICK_SORT", "HEAP_SORT", "COUNTING_SORT", "RADIX_SORT",
                    "BUCKET_SORT", "LINEAR_SEARCH", "ARRAY", "LINKED_LIST", "STACK", "QUEUE", "DSU",
                    "GRAPH_REPRESENTATION", "TARJAN_SCC", "MO_RANGE_QUERY", "ONLINE_RANGE_QUERY", "Z_FUNCTION",
                    "SUFFIX_ARRAY", "SUFFIX_AUTOMATON", "PASCAL_TRIANGLE", "CONVEX_HULL", "DIVIDE_AND_CONQUER",
                    "GREEDY_ACTIVITY_SELECTION", "BACKTRACKING_N_QUEENS" -> firstExecutable(trace);
            default -> throw new IllegalArgumentException("No prepared teaching state registered for type: " + type);
        };
    }

    private static int firstPhase(SimulationTrace trace, Set<String> phases) {
        for (int index = 0; index < trace.size(); index++) {
            String phase = fact(trace.stepAt(index), "phase");
            if (phases.stream().anyMatch(candidate -> phase.equals(candidate) || phase.startsWith(candidate + ";"))) return index;
        }
        return firstExecutable(trace);
    }

    /** Omit vacuous matrix checks, while retaining every actual update and its preceding decision. */
    private static boolean meaningfulDecision(String type, SimulationStep step, SimulationTrace trace, int index) {
        if (!type.equals("FLOYD_WARSHALL") || step.highlightedLine() != 10) return true;
        if (index + 1 < trace.size() && trace.stepAt(index + 1).highlightedLine() == 11) return true;
        int leftRow = integerFact(step, "operand-left-row");
        int leftColumn = integerFact(step, "operand-left-column");
        int rightRow = integerFact(step, "operand-right-row");
        int rightColumn = integerFact(step, "operand-right-column");
        if (leftRow == rightRow || leftColumn == rightColumn) return false;
        if (!(step.stateSnapshot() instanceof TableState table)) return true;
        return finiteCell(table, leftRow, leftColumn) && finiteCell(table, rightRow, rightColumn);
    }

    private static boolean finiteCell(TableState table, int row, int column) {
        if (row < 0 || row >= table.rows().size() || column < 0 || column >= table.rows().get(row).size()) return false;
        try { return Double.isFinite(Double.parseDouble(table.rows().get(row).get(column).value())); }
        catch (NumberFormatException nonNumeric) { return false; }
    }

    private static int integerFact(SimulationStep step, String key) {
        try { return Integer.parseInt(fact(step, key)); }
        catch (NumberFormatException absent) { return -1; }
    }

    /** Tree construction is preparation; the final contiguous operation is the lesson. */
    private static int finalOperationStart(SimulationTrace trace) {
        for (int end = trace.size() - 2; end >= 0; end--) {
            String phase = fact(trace.stepAt(end), "phase");
            String operation = fact(trace.stepAt(end), "operation");
            if (operation.isEmpty() || phase.equals("return") || phase.equals("complete")) continue;
            int first = end;
            while (first > 0 && fact(trace.stepAt(first - 1), "operation").equals(operation)) first--;
            // Repeating the final setup insertion has the same operation text. Its new root
            // visit still has an explicit reset path/root phase, so setup need not be replayed.
            for (int candidate = end; candidate >= first; candidate--) {
                SimulationStep step = trace.stepAt(candidate);
                String candidatePhase = fact(step, "phase");
                String path = fact(step, "path");
                if (candidatePhase.equals("insert; root")
                        || candidatePhase.equals("insert; compare") && path.startsWith("[")
                        && path.endsWith("]") && path.length() > 2 && !path.contains(",")) return candidate;
            }
            return first;
        }
        return firstExecutable(trace);
    }

    private static int firstLine(SimulationTrace trace, int... lines) {
        for (int index = 0; index < trace.size(); index++) {
            int current = trace.stepAt(index).highlightedLine();
            for (int line : lines) if (current == line) return index;
        }
        return firstExecutable(trace);
    }

    private static int firstExecutable(SimulationTrace trace) {
        for (int index = 0; index < trace.size(); index++) {
            SimulationStep step = trace.stepAt(index);
            if (step.highlightedLine() > 0 && step.eventType() != StepEventType.COMPLETE) return index;
        }
        return 0; // A terminal-only trace has no executable highlight to invent.
    }

    private static String fact(SimulationStep step, String key) {
        List<Fact> facts = switch (step.stateSnapshot()) {
            case ArrayState state -> state.facts();
            case GraphState state -> state.facts();
            case TreeState state -> state.facts();
            case TableState state -> state.facts();
            case LinkedState state -> state.facts();
            case StackState state -> state.facts();
        };
        return facts.stream().filter(fact -> fact.key().equals(key)).map(Fact::value).findFirst().orElse("");
    }

    /** Explicit algorithm contracts refer to structured pseudocode line IDs, never narration. */
    private static Set<Integer> teachingLines(String type) {
        return switch (type) {
            case "RECURSION" -> Set.of(1, 2, 3, 4);
            case "MERGE_SORT" -> Set.of(3, 4, 8, 11, 12, 14, 15, 16);
            case "QUICK_SORT" -> Set.of(3, 4, 5, 7, 8, 9, 10);
            case "HEAP_SORT" -> Set.of(4, 6, 7, 11, 12, 14, 15);
            case "COUNTING_SORT" -> Set.of(4, 5, 6, 7, 9, 10, 11);
            case "RADIX_SORT" -> Set.of(4, 5, 6, 7, 9, 10, 11);
            case "BUCKET_SORT" -> Set.of(4, 5, 6, 7, 8, 9, 10, 11, 12);
            case "LINEAR_SEARCH" -> Set.of(3, 4, 5);
            case "BINARY_SEARCH" -> Set.of(2, 4, 5, 6, 8, 9, 10, 11);
            case "BINARY_SEARCH_ANSWER" -> Set.of(2, 4, 7, 8, 10, 12, 13);
            case "ARRAY" -> Set.of(2, 4, 5, 6, 8, 9);
            case "LINKED_LIST" -> Set.of(2, 3, 4, 5, 6, 9, 10, 11, 12, 13);
            case "STACK" -> Set.of(4, 5, 6, 7, 8);
            case "QUEUE" -> Set.of(3, 4, 5, 6, 7, 8, 9, 10);
            case "BST" -> Set.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
            case "AVL_TREE" -> Set.of(2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14);
            case "HEAP" -> Set.of(4, 7, 8, 9, 10, 11, 14, 15, 19, 20, 21, 23);
            case "TRIE" -> Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            case "HASH_MAP" -> Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            case "DSU" -> Set.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            case "GRAPH_REPRESENTATION" -> Set.of(3, 5, 6, 8, 9, 10);
            case "BFS" -> Set.of(5, 7, 9, 11, 12, 13);
            case "DFS" -> Set.of(5, 7, 9, 10, 11, 12, 13);
            case "TOPOLOGICAL_SORT" -> Set.of(5, 7, 8, 10, 11, 12, 13);
            case "TARJAN_SCC" -> Set.of(3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14);
            case "BRIDGES_ARTICULATION" -> Set.of(2, 3, 5, 6, 7, 9, 10, 11, 12, 13, 14);
            case "DIJKSTRA" -> Set.of(5, 7, 8, 10, 12, 13, 14, 15);
            case "BELLMAN_FORD" -> Set.of(4, 5, 8, 9, 10, 11, 12, 13, 14);
            case "FLOYD_WARSHALL" -> Set.of(7, 10, 11, 12, 13);
            case "KRUSKAL_MST" -> Set.of(4, 6, 7, 9, 10, 11);
            case "PRIM_MST" -> Set.of(5, 6, 7, 9, 10, 11, 13, 14, 16, 17);
            case "MAX_FLOW" -> Set.of(2, 5, 6, 7, 9, 10, 11, 12, 13);
            case "PREFIX_SUM" -> Set.of(5, 6, 7);
            case "FENWICK_TREE" -> Set.of(3, 4, 5, 6, 7, 8, 9, 10);
            case "SEGMENT_TREE" -> Set.of(6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
            case "SEGMENT_TREE_LAZY" -> Set.of(6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17);
            case "SPARSE_TABLE" -> Set.of(6, 7);
            case "SQRT_DECOMPOSITION" -> Set.of(3, 4, 5, 6, 7);
            case "MO_RANGE_QUERY" -> Set.of(3, 5, 6, 7);
            case "ONLINE_RANGE_QUERY" -> Set.of(4, 5, 7, 8);
            case "KMP" -> Set.of(10, 12, 13, 14, 15, 16, 17);
            case "Z_FUNCTION" -> Set.of(5, 6, 7, 8);
            case "STRING_HASHING" -> Set.of(4, 6, 7, 8, 9, 10);
            case "SUFFIX_ARRAY" -> Set.of(4, 5, 6, 7);
            case "SUFFIX_AUTOMATON" -> Set.of(3, 4, 7, 8, 9, 10, 11, 12, 13, 14, 15);
            case "EXTENDED_GCD" -> Set.of(3, 5, 6, 7, 8, 9, 10, 11, 12);
            case "SIEVE_OF_ERATOSTHENES" -> Set.of(4, 6, 7, 9, 12);
            case "MODULAR_EXPONENTIATION" -> Set.of(3, 5, 6, 7, 8, 9);
            case "PASCAL_TRIANGLE" -> Set.of(4, 6, 7, 8, 9);
            case "GAUSSIAN_ELIMINATION" -> Set.of(2, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14);
            case "CONVEX_HULL" -> Set.of(2, 3, 5, 6, 7, 8, 9, 10, 11);
            case "DIVIDE_AND_CONQUER" -> Set.of(3, 4, 7, 8);
            case "DYNAMIC_PROGRAMMING" -> Set.of(2, 5, 6, 7, 8, 9);
            case "GREEDY_ACTIVITY_SELECTION" -> Set.of(2, 5, 6, 7, 8);
            case "BACKTRACKING_N_QUEENS" -> Set.of(2, 5, 6, 8, 9);
            default -> throw new IllegalArgumentException("No teaching sequence registered for type: " + type);
        };
    }
}
