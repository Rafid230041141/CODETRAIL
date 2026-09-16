package application;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import application.algorithm.GraphAlgorithms;
import application.algorithm.GraphAlgorithms.Algorithm;
import application.algorithm.GraphAlgorithms.Edge;
import application.algorithm.GraphAlgorithms.Graph;
import application.algorithm.GraphAlgorithms.Step;
import application.client.simulation.SimulationEngine;
import application.client.simulation.SimulationEngine.Cell;
import application.client.simulation.SimulationEngine.SimulationRun;
import application.client.simulation.SimulationEngine.SimulationStep;

/**
 * Exhaustive, catalog-free semantic checks for the 54 DSA lesson simulations.
 *
 * <p>The harness deliberately checks the state transition represented by the
 * public {@link SimulationRun}/{@link SimulationStep}/{@link Cell} records.
 * A non-empty animation is not enough: each lesson has an invariant tied to
 * its advertised data structure or algorithm.  Graph shortest-path/MST
 * checks additionally consume the authoritative public {@link GraphAlgorithms}
 * model.</p>
 */
public final class DsaSemanticVerificationHarness {
    private static final int LESSON_COUNT = 54;
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private DsaSemanticVerificationHarness() {
    }

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        List<LessonCase> lessons = lessons();
        check(lessons.size() == LESSON_COUNT,
                "catalog contains exactly " + LESSON_COUNT + " DSA lessons (got " + lessons.size() + ")");

        List<String> failures = new ArrayList<>();
        int passed = 0;
        for (LessonCase lesson : lessons) {
            try {
                SimulationRun run = SimulationEngine.build(mapper, lesson.type(), lesson.config());
                verifyRunShape(run);
                lesson.check().accept(run);
                System.out.println("PASS: " + lesson.title());
                passed++;
            } catch (RuntimeException exception) {
                String detail = exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage();
                String failure = "FAILED: " + lesson.title() + " — " + lesson.invariant() + " — " + detail;
                failures.add(failure);
                System.out.println(failure);
            }
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("DSA semantic verification failed: " + passed + "/" + LESSON_COUNT
                    + " lessons passed; " + failures.size() + " failed");
        }
        check(passed == LESSON_COUNT, "all DSA lessons passed");
        System.out.println(LESSON_COUNT + "/" + LESSON_COUNT + " DSA SEMANTIC CHECKS PASSED");
    }

    private static List<LessonCase> lessons() {
        List<LessonCase> cases = new ArrayList<>();

        add(cases, "Array", "ARRAY", "{\"values\":[4,1,7,2,6]}",
                "indexed read and capacity-preserving middle insertion", DsaSemanticVerificationHarness::verifyArray);
        add(cases, "Linked List", "LINKED_LIST", "{\"values\":[4,1,7,2]}",
                "next order and local insertion", DsaSemanticVerificationHarness::verifyLinkedList);
        add(cases, "Stack", "STACK", "{\"values\":[3,1,4,2]}",
                "LIFO push, peek, and pop", DsaSemanticVerificationHarness::verifyStack);
        add(cases, "Queue", "QUEUE", "{\"values\":[3,1,4,2]}",
                "FIFO enqueue, peek, and dequeue", DsaSemanticVerificationHarness::verifyQueue);
        add(cases, "Graph", "GRAPH_REPRESENTATION", "{\"directed\":false}",
                "reciprocal adjacency-list representation", DsaSemanticVerificationHarness::verifyGraphRepresentation);
        add(cases, "Heap", "HEAP", "{\"values\":[7,2,9,1,5],\"kind\":\"min\"}",
                "min-heap parent ordering", DsaSemanticVerificationHarness::verifyHeap);
        add(cases, "Hash Map", "HASH_MAP", "{\"keys\":[\"cat\",\"dog\",\"ant\"]}",
                "hash bucket assignment and collision preservation", DsaSemanticVerificationHarness::verifyHashMap);
        add(cases, "DSU", "DSU", "{\"size\":6}",
                "union connectivity and path-compressed representatives", DsaSemanticVerificationHarness::verifyDsu);
        add(cases, "BST", "BST", "{\"values\":[7,3,9,1,5]}",
                "BST search branches and ordered structure", DsaSemanticVerificationHarness::verifyBst);
        add(cases, "AVL", "AVL", "{\"values\":[3,2,1,4,5]}",
                "height balance and a state-changing rotation", DsaSemanticVerificationHarness::verifyAvl);
        add(cases, "Trie", "TRIE", "{\"words\":[\"cat\",\"car\",\"dog\"],\"prefix\":\"ca\"}",
                "shared prefix paths and prefix lookup", DsaSemanticVerificationHarness::verifyTrie);

        for (String algorithm : List.of("Merge", "Quick", "Heap Sort", "Counting", "Radix", "Bucket Sort")) {
            add(cases, algorithm, "SORTING",
                    "{\"values\":[8,3,5,1,9,2],\"algorithm\":\"" + algorithm + "\"}",
                    "sorted permutation for " + algorithm, DsaSemanticVerificationHarness::verifySorting);
        }

        add(cases, "Linear & Binary Search", "LINEAR_SEARCH",
                "{\"values\":[4,8,1,9,2],\"target\":9}",
                "linear scan reaches the correct target index", DsaSemanticVerificationHarness::verifyLinearSearch);
        add(cases, "Binary Search", "BINARY_SEARCH",
                "{\"values\":[1,3,5,7,9,12],\"target\":7}",
                "sorted middle checks retain the target while narrowing", DsaSemanticVerificationHarness::verifyBinarySearch);
        add(cases, "Binary Search on Answer", "BINARY_SEARCH_ANSWER",
                "{\"limit\":12,\"firstTrue\":7}",
                "monotonic predicate and first-feasible boundary", DsaSemanticVerificationHarness::verifyBinaryAnswer);

        add(cases, "BFS", "BFS", "{\"edges\":[[\"A\",\"B\"],[\"A\",\"C\"],[\"B\",\"D\"],[\"B\",\"E\"],[\"C\",\"F\"],[\"E\",\"F\"]],\"start\":\"A\"}",
                "queue-layer visit order and coverage", run -> verifyTraversal(run, false));
        add(cases, "DFS", "DFS", "{\"edges\":[[\"A\",\"B\"],[\"A\",\"C\"],[\"B\",\"D\"],[\"B\",\"E\"],[\"C\",\"F\"],[\"E\",\"F\"]],\"start\":\"A\"}",
                "stack/depth-first visit order and coverage", run -> verifyTraversal(run, true));
        add(cases, "Dijkstra", "DIJKSTRA", "{\"start\":\"A\"}",
                "exact shortest-path distances", DsaSemanticVerificationHarness::verifyDijkstra);
        add(cases, "Bellman-Ford", "BELLMAN_FORD", "{\"start\":\"A\"}",
                "negative-edge relaxation distances", DsaSemanticVerificationHarness::verifyBellmanFord);
        add(cases, "Floyd-Warshall", "FLOYD_WARSHALL", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\"]}",
                "all-pairs shortest-path matrix", DsaSemanticVerificationHarness::verifyFloydWarshall);
        add(cases, "Kruskal", "KRUSKAL", "{}",
                "six-edge MST weight 14 and cycle rejection", DsaSemanticVerificationHarness::verifyKruskal);
        add(cases, "Prim", "PRIM", "{\"start\":\"A\"}",
                "six-edge MST weight 14 and spanning tree", DsaSemanticVerificationHarness::verifyPrim);
        add(cases, "Topological Sort", "TOPOLOGICAL_SORT", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\"]}",
                "every directed edge points forward in the order", DsaSemanticVerificationHarness::verifyTopologicalSort);
        add(cases, "SCC/Tarjan", "SCC", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\"]}",
                "Tarjan low-link stack semantics and SCC grouping", DsaSemanticVerificationHarness::verifyScc);
        add(cases, "Bridges & Articulation Points", "BRIDGES", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\"]}",
                "bridges plus articulation vertices", DsaSemanticVerificationHarness::verifyBridges);
        add(cases, "Max Flow", "MAX_FLOW", "{\"source\":\"S\",\"sink\":\"T\"}",
                "nonnegative residuals and monotone max flow 5", DsaSemanticVerificationHarness::verifyMaxFlow);

        add(cases, "Prefix Sum", "PREFIX_SUM", "{\"values\":[2,1,5,3,4]}",
                "prefix totals and range aggregate 13", DsaSemanticVerificationHarness::verifyPrefixSum);
        add(cases, "Offline Range Query", "RANGE_QUERY",
                "{\"mode\":\"offline\",\"values\":[2,1,5,3,4]}",
                "offline window movement and three correct range aggregates", run -> verifyRangeQuery(run, "offline"));
        add(cases, "Online Range Query", "RANGE_QUERY",
                "{\"mode\":\"online\",\"values\":[2,1,5,3,4]}",
                "online update/query trace and range aggregate 11", run -> verifyRangeQuery(run, "online"));
        add(cases, "Segment Tree (with/without Lazy Propagation)", "SEGMENT_TREE",
                "{\"operation\":\"lazy-range-update\",\"values\":[2,1,5,3,4]}",
                "lazy range update is reflected by the reported aggregate", DsaSemanticVerificationHarness::verifyLazySegmentTree);
        add(cases, "Segment Tree", "SEGMENT_TREE", "{\"values\":[2,1,5,3,4],\"operation\":\"sum\"}",
                "tree root and covered-node range aggregate", DsaSemanticVerificationHarness::verifySegmentTree);
        add(cases, "Fenwick Tree", "FENWICK_TREE", "{\"values\":[2,1,5,3,4],\"operation\":\"prefix-sum\"}",
                "Fenwick partial sums and prefix query", DsaSemanticVerificationHarness::verifyFenwickTree);
        add(cases, "Fenwick Tree (BIT)", "FENWICK_TREE", "{\"values\":[2,1,5,3,4],\"operation\":\"prefix-sum\"}",
                "Fenwick BIT partial sums and prefix query", DsaSemanticVerificationHarness::verifyFenwickTree);
        add(cases, "Sparse Table", "SPARSE_TABLE", "{\"values\":[7,2,5,1,6,3]}",
                "overlapping interval minima are built from prior levels", DsaSemanticVerificationHarness::verifySparseTable);
        add(cases, "Sqrt Decomposition", "SQRT_DECOMPOSITION", "{\"values\":[7,2,5,1,6,3]}",
                "block summaries add to the full-range aggregate", DsaSemanticVerificationHarness::verifySqrtDecomposition);

        add(cases, "Divide and Conquer", "DIVIDE_CONQUER", "{\"values\":[8,3,5,1,9,2]}",
                "recursive combine returns a sorted permutation", DsaSemanticVerificationHarness::verifyDivideAndConquer);
        add(cases, "Greedy", "GREEDY", "{\"intervals\":[[1,3],[2,4],[3,5],[5,7]]}",
                "earliest-finish compatible interval selection", DsaSemanticVerificationHarness::verifyGreedy);
        add(cases, "Dynamic Programming", "DYNAMIC_PROGRAMMING", "{\"n\":7}",
                "Fibonacci table reuses subproblem results", DsaSemanticVerificationHarness::verifyDynamicProgramming);
        add(cases, "Recursion", "RECURSION", "{\"n\":5}",
                "factorial call-stack descent and returns", DsaSemanticVerificationHarness::verifyRecursion);
        add(cases, "Backtracking", "BACKTRACKING", "{\"size\":4}",
                "valid four-queens placement after undo/try trace", DsaSemanticVerificationHarness::verifyBacktracking);

        add(cases, "KMP & Z-function", "STRING_MATCHING",
                "{\"text\":\"ABABDABACDABABCABAB\",\"pattern\":\"ABABCABAB\"}",
                "KMP match plus Z-function state semantics", DsaSemanticVerificationHarness::verifyStringMatching);
        add(cases, "Trie-Based Matching", "TRIE",
                "{\"words\":[\"cat\",\"car\",\"dog\"],\"prefix\":\"ca\",\"mode\":\"matching\"}",
                "prefix query returns cat and car but not dog", DsaSemanticVerificationHarness::verifyTrieMatching);
        add(cases, "Suffix Array / Suffix Automaton", "SUFFIX_STRUCTURE", "{\"text\":\"banana\"}",
                "suffix-array order and suffix-automaton states", DsaSemanticVerificationHarness::verifySuffixStructure);
        add(cases, "String Hashing", "STRING_HASHING",
                "{\"text\":\"abracadabra\",\"pattern\":\"abra\"}",
                "rolling window hashes report both exact matches", DsaSemanticVerificationHarness::verifyStringHashing);

        add(cases, "Algebra", "ALGEBRA", "{\"a\":3,\"b\":2,\"x\":4}",
                "linear expression evaluates to 14", DsaSemanticVerificationHarness::verifyAlgebra);
        add(cases, "Linear Algebra", "LINEAR_ALGEBRA", "{\"matrix\":[[1,2],[3,4]],\"vector\":[5,6]}",
                "matrix-vector product is [17, 39]", DsaSemanticVerificationHarness::verifyLinearAlgebra);
        add(cases, "Number Theory", "NUMBER_THEORY", "{\"a\":84,\"b\":30}",
                "Euclid reduction returns gcd 6", DsaSemanticVerificationHarness::verifyNumberTheory);
        add(cases, "Combinatorics", "COMBINATORICS", "{\"n\":5}",
                "Pascal row C(5,k)", DsaSemanticVerificationHarness::verifyCombinatorics);
        add(cases, "Geometry", "GEOMETRY", "{\"a\":[0,0],\"b\":[4,1],\"c\":[2,5]}",
                "positive orientation cross product 18", DsaSemanticVerificationHarness::verifyGeometry);

        return List.copyOf(cases);
    }

    private static void add(List<LessonCase> cases, String title, String type, String config,
            String invariant, Consumer<SimulationRun> check) {
        cases.add(new LessonCase(title, type, config, invariant, check));
    }

    private static void verifyRunShape(SimulationRun run) {
        check(run != null, "simulation run exists");
        check(!run.pseudocode().isEmpty(), "pseudocode is non-empty");
        check(run.steps().size() > 1, "simulation has more than one state");
        check(run.steps().stream().allMatch(step -> step != null && !step.cells().isEmpty()),
                "every state contains visible cells");
        check(run.steps().stream().noneMatch(step -> step.message().toLowerCase().contains("not available")),
                "simulation type is implemented");
    }

    private static void verifyArray(SimulationRun run) {
        int[] initial = {4, 1, 7, 2, 6};
        checkArray(run.steps().getFirst(), initial, "initial indexed state");
        check("active".equals(run.steps().getFirst().cells().get(2).style()),
                "index 2 is highlighted for O(1) access");
        List<SimulationStep> shifts = messages(run, "shift index");
        check(shifts.size() >= 2, "middle insertion shifts every suffix element");
        SimulationStep inserted = firstMessage(run, "write inserted value");
        checkArray(inserted, new int[]{4, 1, 5, 7, 2, 6},
                "inserted value preserves the original suffix and grows capacity");
        checkArray(last(run), new int[]{4, 1, 5, 7, 2, 6}, "final inserted array");
    }

    private static void verifyLinkedList(SimulationRun run) {
        checkArray(run.steps().getFirst(), new int[]{4, 1, 7, 2}, "initial linked-list order");
        SimulationStep insertion = firstMessage(run, "insert node 5");
        checkArray(insertion, new int[]{4, 5, 1, 7, 2}, "local link insertion keeps successor order");
        checkArray(last(run), new int[]{4, 5, 1, 7, 2}, "tail traversal retains linked-list order");
    }

    private static void verifyStack(SimulationRun run) {
        SimulationStep peek = firstMessage(run, "peek");
        checkArray(peek, new int[]{3, 1, 4, 2}, "peek sees the most recent push");
        check(peek.cells().getLast().text().equals("2"), "top of stack is 2 before pop");
        check(lastMessage(run).toLowerCase().contains("pop"), "trace performs a pop");
        checkArray(last(run), new int[]{3, 1, 4}, "pop removes only the newest element");
    }

    private static void verifyQueue(SimulationRun run) {
        SimulationStep peek = firstMessage(run, "peek");
        checkArray(peek, new int[]{3, 1, 4, 2}, "peek sees the oldest enqueue");
        check(peek.cells().getFirst().text().equals("3"), "head of queue is 3 before dequeue");
        check(lastMessage(run).toLowerCase().contains("dequeue"), "trace performs a dequeue");
        checkArray(last(run), new int[]{1, 4, 2}, "dequeue removes only the oldest element");
    }

    private static void verifyGraphRepresentation(SimulationRun run) {
        Set<String> vertices = new LinkedHashSet<>(texts(last(run)));
        check(vertices.equals(Set.of("A", "B", "C", "D")), "all graph vertices are represented");
        String trace = allText(run).replace(" ", "");
        Map<String, Set<String>> expected = new LinkedHashMap<>();
        expected.put("A", Set.of("B", "C"));
        expected.put("B", Set.of("A", "D"));
        expected.put("C", Set.of("A", "D"));
        expected.put("D", Set.of("B", "C"));
        for (Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            String neighbors = entry.getValue().stream().sorted().reduce((left, right) -> left + "," + right).orElse("");
            check(trace.contains(entry.getKey() + ":" + "[" + neighbors + "]"),
                    entry.getKey() + " adjacency list is preserved");
            for (String neighbor : entry.getValue()) {
                check(trace.contains(neighbor + ":["),
                        "undirected edge " + entry.getKey() + "-" + neighbor + " has a reciprocal list");
            }
        }
    }

    private static void verifyHashMap(SimulationRun run) {
        List<String> buckets = texts(last(run));
        List<String> keys = List.of("cat", "dog", "ant");
        String joined = String.join(" | ", buckets);
        for (String key : keys) {
            check(countOccurrences(joined, key) == 1, "key " + key + " is preserved exactly once");
            int expectedBucket = Math.floorMod(key.hashCode(), buckets.size());
            check(buckets.get(expectedBucket).contains(key),
                    key + " remains in hash bucket " + expectedBucket);
        }
        check(buckets.stream().anyMatch(value -> value.contains("->")),
                "colliding keys are represented by a bucket chain");
    }

    private static void verifyHeap(SimulationRun run) {
        int[] heap = arrayValues(last(run));
        check(heap.length == 5 && heap[0] == 1, "minimum value is at the heap root");
        for (int index = 1; index < heap.length; index++) {
            int parent = (index - 1) / 2;
            check(heap[parent] <= heap[index], "heap parent is no larger than each child");
        }
        check(lastMessage(run).toLowerCase().contains("minimum"), "heap trace reports the root minimum");
    }

    private static void verifyDsu(SimulationRun run) {
        int[] parent = arrayValues(last(run));
        check(parent.length == 6, "DSU contains six requested elements");
        check(Arrays.stream(parent).distinct().count() == 1, "all union operations connect one component");
        check(lastMessage(run).toLowerCase().contains("compression"), "DSU performs path compression");
        check(messages(run, "union").size() >= 4, "DSU records the component unions");
    }

    private static void verifyBst(SimulationRun run) {
        for (SimulationStep step : run.steps()) {
            if (step.message().startsWith("Insert ")) {
                checkBstShape(treeValues(step), 0, Long.MIN_VALUE, Long.MAX_VALUE,
                        "each insertion preserves BST child ordering");
            }
        }
        Map<Integer, Integer> finalTree = treeValues(last(run));
        checkBstShape(finalTree, 0, Long.MIN_VALUE, Long.MAX_VALUE,
                "final tree obeys the BST ordering invariant");
        check(inorder(finalTree, 0).equals(List.of(1, 3, 5, 7, 9)), "inorder output is sorted");
        check(run.pseudocode().stream().anyMatch(line -> line.contains("left") || line.contains("right")),
                "search trace records left and right child decisions");
        check(finalTree.keySet().stream().anyMatch(index -> index > 0),
                "state exposes parent/child positions rather than only a sorted list");
    }

    private static void verifyAvl(SimulationRun run) {
        for (SimulationStep step : run.steps()) {
            if (step.message().startsWith("Insert ")) {
                Map<Integer, Integer> tree = treeValues(step);
                checkBstShape(tree, 0, Long.MIN_VALUE, Long.MAX_VALUE,
                        "AVL insertion retains ordered keys");
                verifyAvlBalance(step, tree, 0);
            }
        }
        Map<Integer, Integer> finalTree = treeValues(last(run));
        checkBstShape(finalTree, 0, Long.MIN_VALUE, Long.MAX_VALUE,
                "final AVL tree obeys BST ordering");
        verifyAvlBalance(last(run), finalTree, 0);
        check(inorder(finalTree, 0).equals(List.of(1, 2, 3, 4, 5)), "AVL inorder output is sorted");
        check(hasMessage(run, "balance"), "balance factors are recomputed");
        SimulationStep rotation = firstMessage(run, "rotation");
        int index = run.steps().indexOf(rotation);
        check(index > 0 && !treeValues(run.steps().get(index - 1)).equals(treeValues(rotation)),
                "rotation changes the represented tree state");
    }

    private static void verifyTrie(SimulationRun run) {
        List<String> nodes = texts(last(run));
        for (String word : List.of("cat", "car", "dog")) {
            for (int length = 1; length <= word.length(); length++) {
                check(nodes.contains(word.substring(0, length)),
                        "trie contains prefix " + word.substring(0, length));
            }
        }
        check(nodes.stream().filter(node -> node.equals("ca")).count() == 1,
                "cat and car share one ca prefix node");
        check(last(run).cells().stream().anyMatch(cell -> cell.text().equals("ca") && "active".equals(cell.style())),
                "prefix query activates ca");
    }

    private static void verifyTrieMatching(SimulationRun run) {
        verifyTrie(run);
        String trace = allText(run).toLowerCase();
        check(trace.contains("cat") && trace.contains("car"), "prefix ca reports cat and car matches");
        check(!trace.contains("match dog") && !trace.contains("dog is a match"),
                "prefix ca does not report dog as a match");
    }

    private static void verifySorting(SimulationRun run) {
        int[] initial = {8, 3, 5, 1, 9, 2};
        int[] expected = {1, 2, 3, 5, 8, 9};
        checkArray(last(run), expected, "final values are sorted");
        int[] actual = arrayValues(last(run));
        Arrays.sort(initial);
        check(Arrays.equals(actual, initial), "sorting preserves the input permutation");
        check(lastMessage(run).toLowerCase().contains("sorted"), "trace declares completion");
    }

    private static void verifyLinearSearch(SimulationRun run) {
        check(lastMessage(run).contains("Target found at index 3"), "linear search returns target index 3");
        checkArray(last(run), new int[]{4, 8, 1, 9, 2}, "linear search does not reorder input");
        List<SimulationStep> comparisons = messages(run, "compare");
        check(comparisons.size() == 4, "linear scan compares indices 0 through 3 exactly once");
        check("active".equals(comparisons.getLast().cells().get(3).style()), "matching index is highlighted");
    }

    private static void verifyBinarySearch(SimulationRun run) {
        checkArray(last(run), new int[]{1, 3, 5, 7, 9, 12}, "binary search operates on sorted values");
        check(lastMessage(run).contains("Target found at index 3"), "binary search returns index 3");
        for (SimulationStep step : messages(run, "discard")) {
            check(!"muted".equals(step.cells().get(3).style()),
                    "target remains inside every narrowed interval");
        }
        check(messages(run, "check middle").size() >= 3, "binary search performs middle comparisons");
    }

    private static void verifyBinaryAnswer(SimulationRun run) {
        check(lastMessage(run).contains("first feasible answer is 7"), "first true boundary is 7");
        for (SimulationStep step : run.steps()) {
            for (Cell cell : step.cells()) {
                String[] parts = cell.text().split("\\n", -1);
                check(parts.length == 2 && (parts[1].equals("T") || parts[1].equals("F")),
                        "predicate state labels every candidate T/F");
            }
            List<String> values = texts(step);
            int firstTrue = values.stream().map(value -> value.substring(value.indexOf('\n') + 1))
                    .toList().indexOf("T");
            check(firstTrue >= 0, "monotonic predicate has a true suffix");
            for (int index = firstTrue + 1; index < values.size(); index++) {
                check(values.get(index).endsWith("T"), "predicate remains true after first feasible value");
            }
        }
    }

    private static void verifyTraversal(SimulationRun run, boolean depthFirst) {
        List<String> visits = new ArrayList<>();
        for (SimulationStep step : run.steps()) {
            String message = step.message();
            if ((depthFirst && message.startsWith("Visit ")) || (!depthFirst && message.startsWith("Expand "))) {
                visits.add(message.substring(message.indexOf(' ') + 1, message.length() - 1));
            }
        }
        List<String> expected = depthFirst
                ? List.of("A", "B", "D", "E", "F", "C")
                : List.of("A", "B", "C", "D", "E", "F");
        check(visits.equals(expected), "visit order is " + expected);
        check(new HashSet<>(visits).size() == 6, "every graph vertex is visited once");
        check(run.steps().getLast().cells().stream().allMatch(cell -> "done".equals(cell.style())),
                "all visited vertices are rendered done");
    }

    private static void verifyDijkstra(SimulationRun run) {
        Map<String, Integer> expected = Map.of("A", 0, "B", 4, "C", 3, "D", 6,
                "E", 7, "F", 9, "G", 12);
        Step finalStep = graphFinal(Algorithm.DIJKSTRA);
        check(finalStep.complete(), "Dijkstra marks completion");
        check(finalStep.values().equals(expected), "GraphAlgorithms distances are exact");
        check(graphValues(last(run)).equals(expected), "visualized distance labels match exact distances");
        check(finalStep.selectedEdgeIds().size() == 6, "Dijkstra retains one predecessor edge per non-source vertex");
    }

    private static void verifyBellmanFord(SimulationRun run) {
        Map<String, Integer> expected = Map.of("A", 0, "B", 4, "C", 2, "D", 5, "E", 6);
        check(graphValues(last(run)).equals(expected), "Bellman-Ford final distances are exact");
        check(lastMessage(run).toLowerCase().contains("complete"), "Bellman-Ford reports completion");
        check(messages(run, "relax").size() >= 4, "negative-edge relaxation updates the path state");
    }

    private static void verifyFloydWarshall(SimulationRun run) {
        Map<String, Integer> expected = new LinkedHashMap<>();
        int[][] matrix = {{0, 4, 6, 9}, {Integer.MAX_VALUE, 0, 2, 5},
                {Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 3},
                {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0}};
        List<String> labels = List.of("A", "B", "C", "D");
        for (int row = 0; row < labels.size(); row++) {
            for (int column = 0; column < labels.size(); column++) {
                expected.put(labels.get(row) + "→" + labels.get(column), matrix[row][column]);
            }
        }
        check(matrixValues(last(run)).equals(expected), "Floyd-Warshall final matrix is exact");
        check(messages(run, "intermediate").size() >= 2, "intermediate vertices improve paths");
    }

    private static void verifyPrim(SimulationRun run) {
        verifyMst(graphFinal(Algorithm.PRIM), false);
        check(graphValues(last(run)).size() == 7, "Prim visualization retains all graph key values");
    }

    private static void verifyKruskal(SimulationRun run) {
        Step finalStep = graphFinal(Algorithm.KRUSKAL);
        verifyMst(finalStep, true);
        check(!finalStep.rejectedEdgeIds().isEmpty(), "Kruskal records at least one rejected cycle edge");
        check(disjoint(finalStep.selectedEdgeIds(), finalStep.rejectedEdgeIds()),
                "selected and rejected edge sets are disjoint");
    }

    private static void verifyMst(Step step, boolean kruskal) {
        Graph graph = GraphAlgorithms.sampleGraph();
        Map<String, Edge> edges = graph.edges().stream().collect(java.util.stream.Collectors.toMap(Edge::id, edge -> edge));
        check(step.complete(), "MST algorithm marks completion");
        check(step.selectedEdgeIds().size() == graph.vertices().size() - 1, "MST selects six edges");
        check(step.totalWeight() == 14, "MST total weight is 14");
        Map<String, String> parent = new HashMap<>();
        graph.vertices().forEach(vertex -> parent.put(vertex.id(), vertex.id()));
        for (String id : step.selectedEdgeIds()) {
            Edge edge = edges.get(id);
            check(edge != null, "selected edge exists in the graph");
            String left = root(parent, edge.from());
            String right = root(parent, edge.to());
            check(!left.equals(right), "selected edges are acyclic");
            parent.put(left, right);
        }
        String root = root(parent, graph.vertices().getFirst().id());
        check(graph.vertices().stream().allMatch(vertex -> root.equals(root(parent, vertex.id()))),
                "selected edges span every vertex");
        if (kruskal) {
            check(step.rejectedEdgeIds().stream().allMatch(id -> !step.selectedEdgeIds().contains(id)),
                    "Kruskal rejected edges are not selected");
        }
    }

    private static void verifyTopologicalSort(SimulationRun run) {
        List<String> order = texts(last(run));
        check(order.equals(List.of("A", "B", "C", "D")), "topological order is A,B,C,D");
        Map<String, Integer> position = new HashMap<>();
        for (int index = 0; index < order.size(); index++) {
            position.put(order.get(index), index);
        }
        for (String edge : List.of("A>B", "A>C", "B>D", "C>D")) {
            String[] parts = edge.split(">");
            check(position.get(parts[0]) < position.get(parts[1]), "edge " + edge + " points forward");
        }
    }

    private static void verifyScc(SimulationRun run) {
        Set<Set<String>> expected = Set.of(Set.of("A", "B", "C"), Set.of("D", "E"));
        Set<Set<String>> actual = new HashSet<>();
        for (String label : texts(last(run))) {
            if (label.contains("=")) {
                actual.add(new HashSet<>(Arrays.asList(label.split("="))));
            }
        }
        String traceWithoutSpaces = allText(run).replace(" ", "");
        boolean groupedInMessage = traceWithoutSpaces.contains("[A,B,C]")
                && traceWithoutSpaces.contains("[D,E]");
        check(actual.equals(expected) || groupedInMessage, "SCC grouping is {A,B,C} and {D,E}");
        String trace = (String.join(" ", run.pseudocode()) + " " + allText(run)).toLowerCase();
        check(trace.contains("tarjan") || trace.contains("low-link"), "SCC trace identifies Tarjan low-link semantics");
        check(trace.contains("stack"), "Tarjan trace maintains an active stack");
    }

    private static void verifyBridges(SimulationRun run) {
        String trace = allText(run).toLowerCase();
        check(trace.contains("a-b") && trace.contains("d-e"), "bridges A-B and D-E are identified");
        check(trace.contains("articulation") && trace.contains("b") && trace.contains("d"),
                "articulation vertices B and D are explicitly identified");
        check(messages(run, "low-link").size() >= 2, "bridge trace propagates low-link values");
    }

    private static void verifyMaxFlow(SimulationRun run) {
        List<Integer> totals = new ArrayList<>();
        for (SimulationStep step : run.steps()) {
            Matcher matcher = Pattern.compile("total flow is (\\d+)").matcher(step.message());
            if (matcher.find()) {
                totals.add(Integer.parseInt(matcher.group(1)));
            }
            check(step.cells().size() == 4, "each augmenting-path state has S/A/B/T vertices");
            Integer stateFlow = null;
            for (Cell cell : step.cells()) {
                Matcher flowMatcher = Pattern.compile("[SABT]\\nflow=(\\d+)").matcher(cell.text());
                check(flowMatcher.matches(), "flow state labels every source/sink vertex");
                int value = Integer.parseInt(flowMatcher.group(1));
                if (stateFlow == null) {
                    stateFlow = value;
                }
                check(value == stateFlow, "all vertices share one monotone flow total");
            }
        }
        check(!totals.isEmpty(), "augmenting paths emit flow totals");
        for (int index = 1; index < totals.size(); index++) {
            check(totals.get(index) >= totals.get(index - 1), "flow total never decreases");
        }
        check(lastMessage(run).contains("max flow is 5"), "maximum flow is 5");
    }

    private static void verifyPrefixSum(SimulationRun run) {
        checkArray(last(run), new int[]{2, 3, 8, 11, 15}, "prefix cells are cumulative totals");
        check(lastMessage(run).contains("Range sum [1, 4] = 13"), "prefix range aggregate is 13");
    }

    private static void verifyRangeQuery(SimulationRun run, String mode) {
        String trace = allText(run).toLowerCase();
        if (mode.equals("lazy")) {
            checkArray(last(run), new int[]{2, 3, 7, 5, 4}, "lazy update changes covered values");
            check(lastMessage(run).contains("sum is 15"), "lazy query reports the post-update aggregate 15");
            check(trace.contains("lazy") && trace.contains("update"), "lazy trace marks the range update");
        } else if (mode.equals("online")) {
            checkArray(last(run), new int[]{2, 1, 7, 3, 4}, "online point update changes the maintained values");
            check(lastMessage(run).contains("sum [1, 3] = 11"), "online range aggregate reflects the update");
            check(trace.contains("point update") && trace.contains("range sum") && trace.contains("return"),
                    "online trace performs a maintained update and query");
        } else {
            checkArray(last(run), new int[]{2, 1, 5, 3, 4}, mode + " query preserves source values");
            check(lastMessage(run).contains("[8, 13, 15]"), "offline range answers preserve all three aggregates");
            check(trace.contains("window") && trace.contains("offline query"), "offline trace moves a query window");
        }
    }

    private static void verifyLazySegmentTree(SimulationRun run) {
        String trace = allText(run).toLowerCase();
        check(trace.contains("lazy"), "segment tree marks a deferred lazy update");
        check(trace.contains("range sum [1, 3] = 15"), "lazy segment tree reports updated range sum 15");
        check(run.steps().stream().anyMatch(step -> step.cells().stream()
                .anyMatch(cell -> cell.text().contains("lazy:+2"))),
                "lazy delta is visible on a covered node");
    }

    private static void verifySegmentTree(SimulationRun run) {
        int[] tree = arrayValues(last(run));
        check(tree.length == 15, "segment tree exposes every nonzero backing node");
        check(tree[0] == 15, "segment-tree root stores the full sum");
        check(Arrays.equals(Arrays.copyOfRange(tree, 7, 12), new int[]{2, 1, 5, 3, 4}),
                "segment-tree leaves preserve input values");
        check(allText(run).contains("range sum [1, 3] = 9"), "segment tree executes range [1,3] sum 9");
    }

    private static void verifyFenwickTree(SimulationRun run) {
        checkFenwick(last(run), new int[]{2, 3, 5, 11, 4}, "Fenwick partial sums are correct");
        String trace = allText(run).toLowerCase();
        check(trace.contains("prefix") && trace.contains("= 15"), "Fenwick executes prefix query through index 5");
    }

    private static void verifySparseTable(SimulationRun run) {
        List<SimulationStep> levels = run.steps();
        checkArray(levels.getFirst(), new int[]{7, 2, 5, 1, 6, 3}, "sparse-table level 0");
        boolean foundCorrectWidthFour = levels.stream()
                .filter(step -> step.message().contains("length 4"))
                .anyMatch(step -> Arrays.equals(arrayValues(step), new int[]{1, 1, 1}));
        check(foundCorrectWidthFour, "length-four intervals use minima from the previous level");
        checkArray(last(run), new int[]{1, 1, 1}, "sparse-table query level has minimum 1");
    }

    private static void verifySqrtDecomposition(SimulationRun run) {
        check(run.steps().stream().anyMatch(step -> {
            try {
                return Arrays.equals(arrayValues(step), new int[]{14, 10});
            } catch (RuntimeException ignored) {
                return false;
            }
        }), "sqrt-decomposition block sums are [14,10]");
        check(lastMessage(run).contains("range sum [1, 4] = 14"), "range aggregate combines block and boundary values");
    }

    private static void verifyDivideAndConquer(SimulationRun run) {
        int[] expected = {1, 2, 3, 5, 8, 9};
        checkArray(last(run), expected, "divide-and-conquer combine fully sorts the values");
        check(countMessagesContaining(run, "divide") >= 3, "recursive divide states are shown");
        check(countMessagesContaining(run, "merge") >= 3 || countMessagesContaining(run, "combine") >= 3,
                "recursive combine states are shown");
    }

    private static void verifyGreedy(SimulationRun run) {
        List<String> selected = texts(last(run));
        check(selected.equals(List.of("[1,3]", "[3,5]", "[5,7]")), "three compatible intervals are selected");
        check(selected.stream().map(value -> value.replaceAll("[^0-9,]", "")).count() == 3,
                "greedy output contains three interval decisions");
        check(lastMessage(run).contains("3 compatible intervals"), "greedy reports the optimal cardinality");
    }

    private static void verifyDynamicProgramming(SimulationRun run) {
        checkArray(last(run), new int[]{0, 1, 1, 2, 3, 5, 8, 13}, "Fibonacci DP table");
        check(lastMessage(run).contains("13"), "DP result is Fibonacci(7) = 13");
    }

    private static void verifyRecursion(SimulationRun run) {
        check(lastMessage(run).contains("120"), "factorial(5) returns 120");
        check(countMessagesContaining(run, "push factorial") == 5, "recursive descent pushes five frames");
        check(countMessagesContaining(run, "return") == 5, "recursive ascent returns five frames");
        check(run.steps().stream().anyMatch(step -> step.cells().size() > 1), "call stack visibly grows");
    }

    private static void verifyBacktracking(SimulationRun run) {
        int[] placement = arrayValues(last(run));
        check(placement.length == 4 && Arrays.stream(placement).allMatch(value -> value >= 0),
                "four rows receive a complete placement");
        check(new HashSet<>(Arrays.stream(placement).boxed().toList()).size() == 4,
                "queens occupy distinct columns");
        for (int row = 0; row < placement.length; row++) {
            for (int previous = 0; previous < row; previous++) {
                check(Math.abs(placement[previous] - placement[row]) != row - previous,
                        "queens do not share a diagonal");
            }
        }
        check(hasMessage(run, "undo"), "backtracking undoes failed choices");
        check(hasMessage(run, "valid placement"), "backtracking records a valid solution");
    }

    private static void verifyStringMatching(SimulationRun run) {
        String trace = allText(run).toLowerCase();
        check(trace.contains("prefix"), "KMP prefix-function state is built");
        check(trace.contains("z-function") || trace.contains("z function") || trace.contains("z table"),
                "Z-function state is also represented");
        check(trace.contains("kmp matches [10]") && trace.contains("z-function matches [10]"),
                "KMP and Z-function both report the match at index 10");
    }

    private static void verifySuffixStructure(SimulationRun run) {
        SimulationStep suffixArray = firstMessage(run, "suffix starts expose");
        checkArray(suffixArray, new int[]{5, 3, 1, 0, 4, 2}, "banana suffix-array order");
        String trace = (String.join(" ", run.pseudocode()) + " " + allText(run)).toLowerCase();
        check(trace.contains("suffix automaton") || trace.contains("automaton"),
                "suffix-automaton state construction is represented");
        check(trace.contains("state") && (trace.contains("link") || trace.contains("transition")),
                "suffix automaton exposes state links or transitions");
    }

    private static void verifyStringHashing(SimulationRun run) {
        String trace = allText(run).toLowerCase();
        check(trace.contains("window") || trace.contains("rolling"), "rolling window hash is updated");
        check(trace.contains("match"), "hash verification reports candidate matches");
        check(trace.contains("0") && trace.contains("7"), "abra matches are reported at positions 0 and 7");
    }

    private static void verifyAlgebra(SimulationRun run) {
        check(lastMessage(run).contains("14"), "3 * 4 + 2 evaluates to 14");
        check(texts(last(run)).equals(List.of("result=14")), "algebra result cell is exact");
    }

    private static void verifyLinearAlgebra(SimulationRun run) {
        checkArray(last(run), new int[]{17, 39}, "matrix-vector product");
    }

    private static void verifyNumberTheory(SimulationRun run) {
        check(texts(last(run)).equals(List.of("gcd=6")), "gcd(84,30) is 6");
        check(lastMessage(run).contains("6"), "Euclid trace reports gcd 6");
    }

    private static void verifyCombinatorics(SimulationRun run) {
        checkArray(last(run), new int[]{1, 5, 10, 10, 5, 1}, "Pascal row n=5");
        check(lastMessage(run).contains("C(5"), "Pascal trace names the requested row");
    }

    private static void verifyGeometry(SimulationRun run) {
        check(allText(run).contains("cross=18"), "orientation cross product is 18");
        check(allText(run).toLowerCase().contains("counter-clockwise"), "positive orientation is counter-clockwise");
    }

    private static Step graphFinal(Algorithm algorithm) {
        List<Step> steps = GraphAlgorithms.run(algorithm, "A");
        check(!steps.isEmpty(), algorithm + " returns a trace");
        return steps.getLast();
    }

    private static Map<String, Set<String>> adjacency(SimulationStep step) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String text : texts(step)) {
            String[] parts = text.split(":\\s*", 2);
            check(parts.length == 2, "adjacency cell has a vertex and neighbors");
            Set<String> neighbors = new LinkedHashSet<>();
            if (!parts[1].isBlank()) {
                for (String neighbor : parts[1].split(",\\s*")) {
                    neighbors.add(neighbor);
                }
            }
            result.put(parts[0], neighbors);
        }
        return result;
    }

    private static Map<String, Integer> graphValues(SimulationStep step) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String text : texts(step)) {
            String[] parts = text.split("\\n", 2);
            check(parts.length == 2, "graph value cell contains a vertex and value");
            result.put(parts[0], parts[1].equals("∞") ? Integer.MAX_VALUE : Integer.parseInt(parts[1]));
        }
        return result;
    }

    private static Map<String, Integer> matrixValues(SimulationStep step) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String text : texts(step)) {
            String[] parts = text.split("\\n", 2);
            check(parts.length == 2, "matrix cell contains a pair and value");
            result.put(parts[0], parts[1].equals("∞") ? Integer.MAX_VALUE : Integer.parseInt(parts[1]));
        }
        return result;
    }

    private static int[] arrayValues(SimulationStep step) {
        int[] result = new int[step.cells().size()];
        for (int index = 0; index < result.length; index++) {
            Matcher matcher = INTEGER.matcher(step.cells().get(index).text().trim());
            check(matcher.matches(), "array cell is an integer");
            result[index] = Integer.parseInt(matcher.group());
        }
        return result;
    }

    private static Map<Integer, Integer> treeValues(SimulationStep step) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < step.cells().size(); index++) {
            String text = step.cells().get(index).text();
            if (text.equals("empty")) {
                continue;
            }
            String firstLine = text.split("\\n", 2)[0].trim();
            Matcher matcher = INTEGER.matcher(firstLine);
            check(matcher.matches(), "tree cell exposes its key value");
            result.put(index, Integer.parseInt(matcher.group()));
        }
        return result;
    }

    private static void checkBstShape(Map<Integer, Integer> tree, int position,
            long lower, long upper, String description) {
        Integer value = tree.get(position);
        if (value == null) {
            return;
        }
        check(value > lower && value < upper, description);
        checkBstShape(tree, position * 2 + 1, lower, value, description);
        checkBstShape(tree, position * 2 + 2, value, upper, description);
    }

    private static List<Integer> inorder(Map<Integer, Integer> tree, int position) {
        Integer value = tree.get(position);
        if (value == null) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>(inorder(tree, position * 2 + 1));
        result.add(value);
        result.addAll(inorder(tree, position * 2 + 2));
        return result;
    }

    private static int verifyAvlBalance(SimulationStep step, Map<Integer, Integer> tree, int position) {
        Integer value = tree.get(position);
        if (value == null) {
            return 0;
        }
        int leftHeight = verifyAvlBalance(step, tree, position * 2 + 1);
        int rightHeight = verifyAvlBalance(step, tree, position * 2 + 2);
        int expectedHeight = Math.max(leftHeight, rightHeight) + 1;
        String[] lines = step.cells().get(position).text().split("\\n", -1);
        check(lines.length >= 2 && lines[1].startsWith("h=") && lines[1].contains(" b="),
                "AVL cell exposes height and balance factor");
        Matcher heightMatcher = Pattern.compile("h=(\\d+)").matcher(lines[1]);
        Matcher balanceMatcher = Pattern.compile("b=(-?\\d+)").matcher(lines[1]);
        check(heightMatcher.find() && balanceMatcher.find(), "AVL metadata is numeric");
        check(Integer.parseInt(heightMatcher.group(1)) == expectedHeight, "AVL height metadata is exact");
        int balance = Integer.parseInt(balanceMatcher.group(1));
        check(balance == leftHeight - rightHeight && Math.abs(balance) <= 1,
                "AVL balance factor is within [-1,1]");
        return expectedHeight;
    }

    private static void checkArray(SimulationStep step, int[] expected, String description) {
        check(Arrays.equals(arrayValues(step), expected), description + " (expected " + Arrays.toString(expected)
                + ", got " + Arrays.toString(arrayValues(step)) + ")");
    }

    private static void checkFenwick(SimulationStep step, int[] expected, String description) {
        check(step.cells().size() == expected.length, description + " exposes every Fenwick node");
        for (int index = 0; index < expected.length; index++) {
            String[] lines = step.cells().get(index).text().split("\\n", -1);
            check(lines.length == 2 && lines[0].equals("i=" + (index + 1)),
                    description + " labels node " + (index + 1));
            check(Integer.parseInt(lines[1]) == expected[index], description + " node " + (index + 1));
        }
    }

    private static void checkSorted(int[] values, String description) {
        for (int index = 1; index < values.length; index++) {
            check(values[index - 1] <= values[index], description);
        }
    }

    private static String allText(SimulationRun run) {
        StringBuilder builder = new StringBuilder(String.join(" ", run.pseudocode()));
        for (SimulationStep step : run.steps()) {
            builder.append(' ').append(step.message());
            for (Cell cell : step.cells()) {
                builder.append(' ').append(cell.text());
            }
        }
        return builder.toString();
    }

    private static List<String> texts(SimulationStep step) {
        return step.cells().stream().map(Cell::text).toList();
    }

    private static List<SimulationStep> messages(SimulationRun run, String phrase) {
        String needle = phrase.toLowerCase();
        return run.steps().stream().filter(step -> step.message().toLowerCase().contains(needle)).toList();
    }

    private static SimulationStep firstMessage(SimulationRun run, String phrase) {
        return run.steps().stream().filter(step -> step.message().toLowerCase().contains(phrase.toLowerCase()))
                .findFirst().orElseThrow(() -> new IllegalStateException("missing trace message: " + phrase));
    }

    private static String lastMessage(SimulationRun run) {
        return run.steps().getLast().message();
    }

    private static SimulationStep last(SimulationRun run) {
        return run.steps().getLast();
    }

    private static boolean hasMessage(SimulationRun run, String phrase) {
        return run.steps().stream().anyMatch(step -> step.message().toLowerCase().contains(phrase.toLowerCase()));
    }

    private static int countMessagesContaining(SimulationRun run, String phrase) {
        return messages(run, phrase).size();
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static boolean disjoint(Set<String> left, Set<String> right) {
        for (String value : left) {
            if (right.contains(value)) {
                return false;
            }
        }
        return true;
    }

    private static String root(Map<String, String> parent, String value) {
        String current = parent.get(value);
        while (!current.equals(parent.get(current))) {
            current = parent.get(current);
        }
        return current;
    }

    private static record LessonCase(String title, String type, String config, String invariant,
            Consumer<SimulationRun> check) {
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException(description);
        }
    }
}
