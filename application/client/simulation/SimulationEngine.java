package application.client.simulation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import application.algorithm.GraphAlgorithms;
import application.algorithm.GraphAlgorithms.Algorithm;

public final class SimulationEngine {
    private static final int FLOYD_INFINITY = 1_000_000_000;

    private SimulationEngine() {
    }

    public static SimulationRun build(ObjectMapper mapper, String type, String configJson) {
        JsonNode config = readConfig(mapper, configJson);
        return switch (type == null ? "" : type.toUpperCase(Locale.ROOT)) {
            case "ARRAY", "LINKED_LIST", "STACK", "QUEUE", "GRAPH_REPRESENTATION", "HASH_MAP", "BST", "AVL", "TRIE" ->
                    dataStructure(type.toUpperCase(Locale.ROOT), config);
            case "RECURSION" -> recursion(config.path("n").asInt(4));
            case "SORTING" -> sorting(values(config, 7, 3, 9, 2, 6, 1), config.path("algorithm").asText("Quick Sort"));
            case "LINEAR_SEARCH" -> linearSearch(values(config, 4, 8, 1, 9, 2), config.path("target").asInt(9));
            case "BINARY_SEARCH" -> binarySearch(values(config, 2, 5, 8, 12, 16, 23, 38),
                    config.path("target").asInt(16));
            case "BINARY_SEARCH_ANSWER" -> binaryAnswer(config.path("limit").asInt(12),
                    config.path("firstTrue").asInt(7));
            case "BFS" -> graphTraversal(false, config);
            case "DFS" -> graphTraversal(true, config);
            case "SEGMENT_TREE" -> segmentTree(values(config, 2, 1, 5, 3, 4),
                    config.path("operation").asText("sum"));
            case "FENWICK_TREE" -> fenwick(values(config, 3, 2, 4, 5, 1));
            case "DSU" -> dsu(config.path("size").asInt(6));
            case "HEAP" -> heap(values(config, 8, 3, 6, 1, 5, 2), config.path("kind").asText("min"));
            case "DIJKSTRA" -> graphAlgorithm(Algorithm.DIJKSTRA, config.path("start").asText("A"));
            case "PRIM" -> graphAlgorithm(Algorithm.PRIM, config.path("start").asText("A"));
            case "KRUSKAL" -> graphAlgorithm(Algorithm.KRUSKAL, "A");
            case "BELLMAN_FORD" -> bellmanFord(config);
            case "FLOYD_WARSHALL" -> floydWarshall(config);
            case "TOPOLOGICAL_SORT" -> topologicalSort(config);
            case "SCC" -> stronglyConnectedComponents(config);
            case "BRIDGES" -> bridges(config);
            case "MAX_FLOW" -> maxFlow(config);
            case "PREFIX_SUM" -> prefixSum(values(config, 2, 1, 5, 3, 4));
            case "RANGE_QUERY" -> rangeQuery(values(config, 2, 1, 5, 3, 4), config.path("mode").asText("online"));
            case "SPARSE_TABLE" -> sparseTable(values(config, 7, 2, 5, 1, 6, 3));
            case "SQRT_DECOMPOSITION" -> sqrtDecomposition(values(config, 7, 2, 5, 1, 6, 3));
            case "DIVIDE_CONQUER" -> divideAndConquer(values(config, 8, 3, 5, 1, 9, 2));
            case "GREEDY" -> greedy(config);
            case "DYNAMIC_PROGRAMMING" -> dynamicProgramming(config.path("n").asInt(7));
            case "BACKTRACKING" -> backtracking(config.path("size").asInt(4));
            case "STRING_MATCHING" -> stringMatching(config.path("text").asText("ABABDABACDABABCABAB"),
                    config.path("pattern").asText("ABABCABAB"));
            case "SUFFIX_STRUCTURE" -> suffixStructure(config.path("text").asText("banana"));
            case "STRING_HASHING" -> stringHashing(config.path("text").asText("abracadabra"),
                    config.path("pattern").asText("abra"));
            case "ALGEBRA" -> algebra(config.path("a").asInt(3), config.path("b").asInt(2), config.path("x").asInt(4));
            case "LINEAR_ALGEBRA" -> linearAlgebra(config);
            case "NUMBER_THEORY" -> numberTheory(config.path("a").asInt(84), config.path("b").asInt(30));
            case "COMBINATORICS" -> combinatorics(config.path("n").asInt(5));
            case "GEOMETRY" -> geometry(config);
            default -> new SimulationRun(
                    List.of("Inspect the current state"),
                    List.of(step("This simulation type is not available yet.", 0,
                            List.of(new Cell("?", "muted")))));
        };
    }

    private static SimulationRun recursion(int requested) {
        int n = Math.max(2, Math.min(requested, 7));
        List<String> code = List.of(
                "factorial(n)",
                "if n <= 1: return 1",
                "return n * factorial(n - 1)",
                "resume the waiting frame");
        List<SimulationStep> steps = new ArrayList<>();
        List<Integer> stack = new ArrayList<>();
        for (int value = n; value >= 1; value--) {
            stack.add(value);
            steps.add(frameStep(stack, "Push factorial(" + value + ") onto the call stack.", value == 1 ? 1 : 2));
        }
        int result = 1;
        for (int index = stack.size() - 1; index >= 0; index--) {
            int value = stack.get(index);
            result *= value;
            steps.add(frameStep(stack.subList(0, index + 1),
                    "Return " + result + " from factorial(" + value + ").", index == 0 ? 0 : 3));
        }
        return new SimulationRun(code, steps);
    }

    private static SimulationStep frameStep(List<Integer> stack, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < stack.size(); index++) {
            String style = index == stack.size() - 1 ? "active" : "selected";
            cells.add(new Cell("f(" + stack.get(index) + ")", style));
        }
        return step(message, line, cells);
    }

    private static SimulationRun dataStructure(String type, JsonNode config) {
        int[] input = values(config, 4, 1, 7, 2, 6);
        return switch (type) {
            case "ARRAY" -> arrayStructure(input);
            case "LINKED_LIST" -> linkedList(input);
            case "STACK" -> stack(input);
            case "QUEUE" -> queue(input);
            case "GRAPH_REPRESENTATION" -> graphRepresentation(config);
            case "HASH_MAP" -> hashMap(config);
            case "BST" -> binarySearchTree(input);
            case "AVL" -> avlTree(input);
            case "TRIE" -> trie(config);
            default -> new SimulationRun(List.of("inspect the structure"),
                    List.of(textStep("No structure state is available.", 0, List.of(new Cell("?", "muted")))));
        };
    }

    private static SimulationRun arrayStructure(int[] input) {
        int[] values = Arrays.copyOf(input, input.length + 1);
        List<String> code = List.of("read a[i] in O(1)", "shift the suffix right", "write the new value", "scan the updated array");
        List<SimulationStep> steps = new ArrayList<>();
        int index = Math.min(2, input.length);
        int inserted = 5;
        steps.add(arrayStep(input, Math.min(index, input.length - 1), -1,
                "Read index " + index + " without scanning earlier values.", 0, false));
        for (int cursor = input.length; cursor > index; cursor--) {
            values[cursor] = values[cursor - 1];
            steps.add(arrayStep(values, cursor, cursor - 1,
                    "Shift index " + (cursor - 1) + " right without losing its value.", 1, false));
        }
        values[index] = inserted;
        steps.add(arrayStep(values, index, -1, "Write inserted value " + inserted + " at index " + index + ".", 2, false));
        steps.add(arrayStep(values, -1, -1, "The contiguous array is ready for the next operation.", 3, true));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun linkedList(int[] input) {
        List<String> nodes = new ArrayList<>();
        for (int value : input) {
            nodes.add(Integer.toString(value));
        }
        List<String> code = List.of(
                "HEAD points to first node [data | next]",
                "traverse: curr = curr.next",
                "insert after curr: newNode.next = curr.next; curr.next = newNode",
                "append at tail: tail.next = newNode; tail = newNode",
                "delete node: curr.next = curr.next.next"
        );
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(chainStep(nodes, 0,
                "HEAD points to first node (" + nodes.getFirst() + "). Each node is [ Data | Next Pointer ] ending in NULL.", 0));
        int traverseIdx = Math.min(1, nodes.size() - 1);
        steps.add(chainStep(nodes, traverseIdx,
                "Traversing list: move curr to node[" + traverseIdx + "] (value " + nodes.get(traverseIdx) + ") via curr = curr.next.", 1));
        nodes.add(2, "99");
        steps.add(chainStep(nodes, 2,
                "Insert node 99 at index 2: set newNode.next to next node, then redirect node[1].next = newNode.", 2));
        nodes.add("50");
        steps.add(chainStep(nodes, nodes.size() - 1,
                "Append node 50 at TAIL in O(1): set tail.next = newNode, and update TAIL to newNode.", 3));
        nodes.remove(2);
        steps.add(chainStep(nodes, 1,
                "Delete node 99: bypass it by setting node[1].next = node[1].next.next. Memory is reclaimed.", 4));
        return new SimulationRun(code, steps);
    }


    private static SimulationRun stack(int[] input) {
        List<String> values = new ArrayList<>();
        List<String> code = List.of("push value onto the top", "peek the top value", "pop the top value", "preserve LIFO order");
        List<SimulationStep> steps = new ArrayList<>();
        for (int value : input) {
            values.add(Integer.toString(value));
            steps.add(chainStep(values, values.size() - 1, "Push " + value + " onto the stack.", 0));
        }
        steps.add(chainStep(values, values.size() - 1, "Peek " + values.getLast() + " at the top.", 1));
        values.removeLast();
        steps.add(chainStep(values, values.isEmpty() ? -1 : values.size() - 1, "Pop the most recently pushed value.", 2));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun queue(int[] input) {
        ArrayDeque<String> values = new ArrayDeque<>();
        List<String> code = List.of("enqueue at the tail", "peek the head", "dequeue the head", "preserve FIFO order");
        List<SimulationStep> steps = new ArrayList<>();
        for (int value : input) {
            values.addLast(Integer.toString(value));
            steps.add(chainStep(new ArrayList<>(values), 0, "Enqueue " + value + " at the tail.", 0));
        }
        steps.add(chainStep(new ArrayList<>(values), 0, "Peek the oldest value at the head.", 1));
        String removed = values.removeFirst();
        steps.add(chainStep(new ArrayList<>(values), 0, "Dequeue " + removed + " from the head.", 2));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun graphRepresentation(JsonNode config) {
        boolean directed = config.path("directed").asBoolean(false);
        List<GraphLink> links = unweightedLinks(config, List.of(
                link("A", "B", directed), link("A", "C", directed),
                link("B", "D", directed), link("C", "D", directed)), directed);
        List<String> vertices = configuredVertices(config, List.of("A", "B", "C", "D"), links);
        Map<String, List<String>> graph = adjacency(vertices, links, !directed);
        List<String> code = List.of("create one vertex per key", "append each neighbor to its adjacency list",
                "scan neighbors of a vertex", "use a matrix when direct edge lookup matters");
        List<SimulationStep> steps = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        for (String vertex : vertices) {
            added.add(vertex);
            steps.add(graphStep(new LinkedHashSet<>(vertices), added, vertex,
                    "Add " + vertex + ": " + graph.get(vertex) + " to the adjacency list.", 1, links));
        }
        steps.add(graphStep(new LinkedHashSet<>(vertices), added, null,
                (directed ? "Directed" : "Undirected") + " adjacency lists: " + graph + ".", 2, links));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun hashMap(JsonNode config) {
        List<String> keys = new ArrayList<>();
        JsonNode configured = config.path("keys");
        if (configured.isArray()) {
            for (JsonNode key : configured) {
                keys.add(key.asText());
            }
        }
        if (keys.isEmpty()) {
            keys.addAll(List.of("cat", "dog", "ant"));
        }
        List<String> buckets = new ArrayList<>(Collections.nCopies(3, "empty"));
        List<String> code = List.of("hash the key", "select its bucket", "resolve a collision by chaining", "look up the matching key");
        List<SimulationStep> steps = new ArrayList<>();
        for (String key : keys) {
            int bucket = Math.floorMod(key.hashCode(), buckets.size());
            buckets.set(bucket, buckets.get(bucket).equals("empty") ? key : buckets.get(bucket) + " -> " + key);
            steps.add(chainStep(buckets, bucket, "Hash " + key + " into bucket " + bucket + ".", 1));
        }
        int targetBucket = Math.floorMod(keys.getFirst().hashCode(), buckets.size());
        steps.add(chainStep(buckets, targetBucket, "Follow the bucket chain to find " + keys.getFirst() + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun binarySearchTree(int[] input) {
        List<String> code = List.of("compare with the current node", "go left for a smaller key", "go right for a larger key", "visit inorder for sorted output");
        List<SimulationStep> steps = new ArrayList<>();
        SearchTreeNode root = null;
        for (int value : input) {
            root = insertBst(root, value);
            steps.add(treeStep(root, value,
                    "Insert " + value + " by following the BST comparison path.", 0));
        }
        List<Integer> inorder = new ArrayList<>();
        collectInorder(root, inorder);
        steps.add(treeStep(root, null, "Inorder traversal reports " + inorder + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun avlTree(int[] input) {
        List<String> code = List.of("insert as a BST", "compute balance factor", "rotate the heavy child", "continue with height-balanced nodes");
        List<SimulationStep> steps = new ArrayList<>();
        AvlNode root = null;
        for (int value : input) {
            List<String> rotations = new ArrayList<>();
            root = insertAvl(root, value, rotations);
            String message = rotations.isEmpty()
                    ? "Insert " + value + " and confirm every balance factor is within [-1, 1]."
                    : "Insert " + value + "; " + String.join("; ", rotations) + ".";
            steps.add(avlTreeStep(root, value, message, rotations.isEmpty() ? 1 : 2));
        }
        steps.add(avlTreeStep(root, null, "Every AVL node is height-balanced after the rotations.", 3));
        return new SimulationRun(code, steps);
    }

    private static SearchTreeNode insertBst(SearchTreeNode node, int value) {
        if (node == null) {
            return new SearchTreeNode(value);
        }
        if (value < node.value) {
            node.left = insertBst(node.left, value);
        } else if (value > node.value) {
            node.right = insertBst(node.right, value);
        }
        return node;
    }

    private static void collectInorder(SearchTreeNode node, List<Integer> values) {
        if (node == null) {
            return;
        }
        collectInorder(node.left, values);
        values.add(node.value);
        collectInorder(node.right, values);
    }

    private static SimulationStep treeStep(SearchTreeNode root, Integer active, String message, int line) {
        Map<Integer, Cell> positions = new LinkedHashMap<>();
        collectTreeCells(root, 0, active, positions);
        return step(message, line, completeTreeCells(positions));
    }

    private static void collectTreeCells(
            SearchTreeNode node, int position, Integer active, Map<Integer, Cell> positions) {
        if (node == null || position >= 31) {
            return;
        }
        positions.put(position, new Cell(Integer.toString(node.value),
                active != null && node.value == active ? "active" : "default"));
        collectTreeCells(node.left, position * 2 + 1, active, positions);
        collectTreeCells(node.right, position * 2 + 2, active, positions);
    }

    private static AvlNode insertAvl(AvlNode node, int value, List<String> rotations) {
        if (node == null) {
            return new AvlNode(value);
        }
        if (value < node.value) {
            node.left = insertAvl(node.left, value, rotations);
        } else if (value > node.value) {
            node.right = insertAvl(node.right, value, rotations);
        } else {
            return node;
        }
        updateHeight(node);
        int balance = balance(node);
        if (balance > 1) {
            if (value > node.left.value) {
                rotations.add("left rotation at " + node.left.value);
                node.left = rotateAvlLeft(node.left);
            }
            rotations.add("right rotation at " + node.value);
            return rotateAvlRight(node);
        }
        if (balance < -1) {
            if (value < node.right.value) {
                rotations.add("right rotation at " + node.right.value);
                node.right = rotateAvlRight(node.right);
            }
            rotations.add("left rotation at " + node.value);
            return rotateAvlLeft(node);
        }
        return node;
    }

    private static AvlNode rotateAvlRight(AvlNode root) {
        AvlNode nextRoot = root.left;
        AvlNode transferred = nextRoot.right;
        nextRoot.right = root;
        root.left = transferred;
        updateHeight(root);
        updateHeight(nextRoot);
        return nextRoot;
    }

    private static AvlNode rotateAvlLeft(AvlNode root) {
        AvlNode nextRoot = root.right;
        AvlNode transferred = nextRoot.left;
        nextRoot.left = root;
        root.right = transferred;
        updateHeight(root);
        updateHeight(nextRoot);
        return nextRoot;
    }

    private static int height(AvlNode node) {
        return node == null ? 0 : node.height;
    }

    private static int balance(AvlNode node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private static void updateHeight(AvlNode node) {
        node.height = Math.max(height(node.left), height(node.right)) + 1;
    }

    private static SimulationStep avlTreeStep(AvlNode root, Integer active, String message, int line) {
        Map<Integer, Cell> positions = new LinkedHashMap<>();
        collectAvlCells(root, 0, active, positions);
        return step(message, line, completeTreeCells(positions));
    }

    private static void collectAvlCells(AvlNode node, int position, Integer active, Map<Integer, Cell> positions) {
        if (node == null || position >= 31) {
            return;
        }
        String label = node.value + "\nh=" + node.height + " b=" + balance(node);
        positions.put(position, new Cell(label,
                active != null && node.value == active ? "active" : "default"));
        collectAvlCells(node.left, position * 2 + 1, active, positions);
        collectAvlCells(node.right, position * 2 + 2, active, positions);
    }

    private static List<Cell> completeTreeCells(Map<Integer, Cell> positions) {
        int last = positions.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index <= last; index++) {
            cells.add(positions.getOrDefault(index, new Cell("empty", "muted")));
        }
        return cells;
    }

    private static SimulationRun trie(JsonNode config) {
        List<String> words = new ArrayList<>();
        JsonNode configured = config.path("words");
        if (configured.isArray()) {
            for (JsonNode word : configured) {
                words.add(word.asText());
            }
        }
        if (words.isEmpty()) {
            words.addAll(List.of("cat", "car", "dog"));
        }
        List<String> code = List.of("start at the root", "follow one edge per character", "mark a complete word", "reuse shared prefixes");
        List<SimulationStep> steps = new ArrayList<>();
        List<String> nodes = new ArrayList<>(List.of("root"));
        for (String word : words) {
            String prefix = "";
            for (int index = 0; index < word.length(); index++) {
                prefix += word.charAt(index);
                if (!nodes.contains(prefix)) {
                    nodes.add(prefix);
                }
            }
            steps.add(chainStep(nodes, nodes.indexOf(prefix), "Insert word " + word + " by following its prefix edges.", 1));
        }
        String requestedPrefix = config.path("prefix").asText(words.getFirst().substring(0, 1));
        int active = nodes.indexOf(requestedPrefix);
        List<String> matches = words.stream().filter(word -> word.startsWith(requestedPrefix)).toList();
        List<Cell> result = new ArrayList<>();
        for (String node : nodes) {
            String style = node.equals(requestedPrefix) ? "active"
                    : matches.contains(node) ? "done" : "default";
            result.add(new Cell(node, style));
        }
        steps.add(step("Traverse prefix " + requestedPrefix + "; matching words: " + matches + ".", 3, result));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun linearSearch(int[] raw, int target) {
        int[] values = Arrays.copyOf(raw, raw.length);
        List<String> code = List.of("start at index 0", "compare a[i] with target", "advance i", "return the matching index");
        List<SimulationStep> steps = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            steps.add(arrayStep(values, index, -1, "Compare " + values[index] + " with target " + target + ".", 1, false));
            if (values[index] == target) {
                steps.add(arrayStep(values, index, -1, "Target found at index " + index + ".", 3, true));
                return new SimulationRun(code, steps);
            }
        }
        steps.add(arrayStep(values, -1, -1, "The target is not present.", 3, false));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun sorting(int[] input, String algorithm) {
        int[] values = Arrays.copyOf(input, input.length);
        List<SimulationStep> steps = new ArrayList<>();
        List<String> code;
        String name = algorithm == null ? "quick" : algorithm.toLowerCase(Locale.ROOT);
        if (name.contains("merge")) {
            code = List.of("split every range at its midpoint", "continue until every range has one value",
                    "merge adjacent sorted ranges", "write the merged range back in order");
            mergeSort(values, 0, values.length - 1, steps);
        } else if (name.contains("heap")) {
            code = List.of("build a max heap", "compare a node with its children", "swap the largest to the root",
                    "move the root to the sorted suffix", "sift down the remaining heap");
            heapSort(values, steps);
        } else if (name.contains("count")) {
            code = List.of("find the key range", "count each value", "prefix the counts", "place values stably");
            countingSort(values, steps);
        } else if (name.contains("radix")) {
            code = List.of("choose the current digit", "count digit frequencies", "place values stably", "advance to the next digit");
            radixSort(values, steps);
        } else if (name.contains("bucket")) {
            code = List.of("create value buckets", "distribute each value", "sort each bucket", "concatenate buckets");
            bucketSort(values, steps);
        } else {
            code = List.of("choose the final item as pivot", "scan values <= pivot", "swap into the left partition",
                    "place pivot between partitions", "recurse on both partitions");
            quickSort(values, 0, values.length - 1, steps);
        }
        if (!name.contains("merge")) {
            steps.add(arrayStep(values, -1, -1, "The array is sorted.", code.size() - 1, true));
        }
        return new SimulationRun(code, steps);
    }

    private static void heapSort(int[] values, List<SimulationStep> steps) {
        for (int index = values.length / 2 - 1; index >= 0; index--) {
            siftDown(values, index, values.length, steps);
        }
        for (int end = values.length - 1; end > 0; end--) {
            swap(values, 0, end);
            steps.add(arrayStep(values, 0, end, "Move the maximum into sorted index " + end + ".", 3, false));
            siftDown(values, 0, end, steps);
        }
    }

    private static void siftDown(int[] values, int root, int length, List<SimulationStep> steps) {
        int index = root;
        while (true) {
            int child = index * 2 + 1;
            if (child >= length) {
                return;
            }
            if (child + 1 < length && values[child + 1] > values[child]) {
                child++;
            }
            steps.add(arrayStep(values, index, child, "Compare heap node " + index + " with its largest child.", 1, false));
            if (values[index] >= values[child]) {
                return;
            }
            swap(values, index, child);
            steps.add(arrayStep(values, index, child, "Sift the larger child upward.", 2, false));
            index = child;
        }
    }

    private static void countingSort(int[] values, List<SimulationStep> steps) {
        if (values.length == 0) {
            return;
        }
        int minimum = Arrays.stream(values).min().orElse(0);
        int maximum = Arrays.stream(values).max().orElse(0);
        int[] counts = new int[maximum - minimum + 1];
        for (int value : values) {
            counts[value - minimum]++;
            steps.add(arrayStep(counts, value - minimum, -1,
                    "Count one occurrence of " + value + ".", 1, false));
        }
        int write = 0;
        for (int index = 0; index < counts.length; index++) {
            while (counts[index]-- > 0) {
                values[write] = index + minimum;
                steps.add(arrayStep(values, write, -1,
                        "Write key " + values[write] + " at index " + write + ".", 3, false));
                write++;
            }
        }
    }

    private static void radixSort(int[] values, List<SimulationStep> steps) {
        if (values.length == 0) {
            return;
        }
        int maximum = Arrays.stream(values).max().orElse(0);
        if (Arrays.stream(values).anyMatch(value -> value < 0)) {
            Arrays.sort(values);
            steps.add(arrayStep(values, -1, -1, "Use a signed-key fallback for the mixed range.", 2, false));
            return;
        }
        for (int place = 1; maximum / place > 0; place *= 10) {
            int[] output = new int[values.length];
            int[] counts = new int[10];
            for (int value : values) {
                counts[(value / place) % 10]++;
            }
            for (int index = 1; index < counts.length; index++) {
                counts[index] += counts[index - 1];
            }
            for (int index = values.length - 1; index >= 0; index--) {
                int digit = (values[index] / place) % 10;
                output[--counts[digit]] = values[index];
            }
            System.arraycopy(output, 0, values, 0, values.length);
            steps.add(arrayStep(values, -1, -1, "Stable pass by digit place " + place + ".", 2, false));
            if (place > Integer.MAX_VALUE / 10) {
                break;
            }
        }
    }

    private static void bucketSort(int[] values, List<SimulationStep> steps) {
        if (values.length == 0) {
            return;
        }
        int minimum = Arrays.stream(values).min().orElse(0);
        int maximum = Arrays.stream(values).max().orElse(0);
        int bucketCount = Math.max(1, (int) Math.ceil(Math.sqrt(values.length)));
        List<List<Integer>> buckets = new ArrayList<>();
        for (int index = 0; index < bucketCount; index++) {
            buckets.add(new ArrayList<>());
        }
        int range = Math.max(1, maximum - minimum + 1);
        for (int value : values) {
            int bucket = Math.min(bucketCount - 1, (value - minimum) * bucketCount / range);
            buckets.get(bucket).add(value);
            steps.add(bucketStep(buckets, bucket, "Place " + value + " in bucket " + bucket + ".", 1));
        }
        int write = 0;
        for (int bucket = 0; bucket < buckets.size(); bucket++) {
            buckets.get(bucket).sort(Integer::compareTo);
            steps.add(bucketStep(buckets, bucket, "Sort bucket " + bucket + ".", 2));
            for (int value : buckets.get(bucket)) {
                values[write] = value;
                steps.add(arrayStep(values, write, -1, "Concatenate bucket value " + value + ".", 3, false));
                write++;
            }
        }
    }

    private static void quickSort(int[] values, int low, int high, List<SimulationStep> steps) {
        if (low >= high) {
            return;
        }
        int pivot = values[high];
        steps.add(arrayStep(values, high, -1, "Use " + pivot + " as the pivot.", 0, false));
        int boundary = low;
        for (int scan = low; scan < high; scan++) {
            steps.add(arrayStep(values, scan, high, "Compare " + values[scan] + " with pivot " + pivot + ".", 1, false));
            if (values[scan] <= pivot) {
                swap(values, boundary, scan);
                steps.add(arrayStep(values, boundary, scan, "Move the value into the left partition.", 2, false));
                boundary++;
            }
        }
        swap(values, boundary, high);
        steps.add(arrayStep(values, boundary, high, "Place the pivot at index " + boundary + ".", 3, false));
        quickSort(values, low, boundary - 1, steps);
        quickSort(values, boundary + 1, high, steps);
    }

    private static void mergeSort(int[] values, int low, int high, List<SimulationStep> steps) {
        if (values.length == 0) {
            return;
        }
        int[] original = Arrays.copyOf(values, values.length);
        List<MergeRange> ranges = new ArrayList<>();
        ranges.add(mergeRange(original, low, high, "active"));
        steps.add(mergeSortStep(values, ranges, "Start with one continuous array.", 0));

        while (ranges.stream().anyMatch(range -> range.low() < range.high())) {
            List<MergeRange> next = new ArrayList<>();
            for (MergeRange range : ranges) {
                if (range.low() == range.high()) {
                    next.add(mergeRange(original, range.low(), range.high(), "default"));
                    continue;
                }
                int mid = (range.low() + range.high()) / 2;
                next.add(mergeRange(original, range.low(), mid, "active"));
                next.add(mergeRange(original, mid + 1, range.high(), "active"));
            }
            ranges = next;
            boolean singles = ranges.stream().allMatch(range -> range.low() == range.high());
            steps.add(mergeSortStep(values, ranges,
                    singles ? "Every subarray now contains one value." : "Split each remaining range into two subarrays.",
                    singles ? 1 : 0));
        }

        MergeNode root = mergeTree(low, high);
        mergeTrace(root, original, values, ranges, steps);
    }

    private static MergeNode mergeTree(int low, int high) {
        if (low == high) {
            return new MergeNode(low, high, null, null);
        }
        int mid = (low + high) / 2;
        return new MergeNode(low, high, mergeTree(low, mid), mergeTree(mid + 1, high));
    }

    private static List<Integer> mergeTrace(MergeNode node, int[] original, int[] values,
            List<MergeRange> ranges, List<SimulationStep> steps) {
        if (node.low() == node.high()) {
            return List.of(original[node.low()]);
        }
        List<Integer> left = mergeTrace(node.left(), original, values, ranges, steps);
        List<Integer> right = mergeTrace(node.right(), original, values, ranges, steps);
        List<Integer> merged = new ArrayList<>(left.size() + right.size());
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.size() || rightIndex < right.size()) {
            if (rightIndex >= right.size() || leftIndex < left.size()
                    && left.get(leftIndex) <= right.get(rightIndex)) {
                merged.add(left.get(leftIndex++));
            } else {
                merged.add(right.get(rightIndex++));
            }
        }
        for (int index = 0; index < merged.size(); index++) {
            values[node.low() + index] = merged.get(index);
        }
        ranges.removeIf(range -> range.low() >= node.low() && range.high() <= node.high());
        ranges.add(mergeRange(merged, node.low(), node.high(), "active"));
        ranges.sort(Comparator.comparingInt(MergeRange::low));
        String message = node.low() == 0 && node.high() == values.length - 1
                ? "The sorted subarrays merge into the final continuous array."
                : "Merge the sorted subarrays from indices " + node.low() + " through " + node.high() + ".";
        steps.add(mergeSortStep(values, ranges, message, node.low() == 0 && node.high() == values.length - 1 ? 3 : 2));
        return merged;
    }

    private static MergeRange mergeRange(int[] source, int low, int high, String style) {
        List<Integer> rangeValues = new ArrayList<>();
        for (int index = low; index <= high; index++) {
            rangeValues.add(source[index]);
        }
        return new MergeRange(low, high, List.copyOf(rangeValues), style);
    }

    private static MergeRange mergeRange(List<Integer> source, int low, int high, String style) {
        return new MergeRange(low, high, List.copyOf(source), style);
    }

    private static SimulationStep mergeSortStep(int[] values, List<MergeRange> ranges, String message, int codeLine) {
        List<Cell> cells = new ArrayList<>();
        for (int value : values) {
            cells.add(new Cell(Integer.toString(value), "default"));
        }
        List<MergeRange> snapshot = ranges.stream()
                .map(range -> new MergeRange(range.low(), range.high(), List.copyOf(range.values()), range.style()))
                .toList();
        return new SimulationStep(message, codeLine, List.copyOf(cells), List.of(), new MergeSortState(snapshot));
    }

    private static SimulationRun binarySearch(int[] raw, int target) {
        int[] values = Arrays.copyOf(raw, raw.length);
        Arrays.sort(values);
        List<String> code = List.of("low = 0; high = n - 1", "mid = (low + high) / 2", "compare a[mid] with target",
                "discard the impossible half", "return mid when equal");
        List<SimulationStep> steps = new ArrayList<>();
        int low = 0;
        int high = values.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            steps.add(windowStep(values, low, mid, high,
                    "Check middle value " + values[mid] + " against target " + target + ".", 2));
            if (values[mid] == target) {
                steps.add(windowStep(values, mid, mid, mid, "Target found at index " + mid + ".", 4));
                return new SimulationRun(code, steps);
            }
            if (values[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
            steps.add(windowStep(values, low, -1, high, "Discard the half that cannot contain the target.", 3));
        }
        steps.add(arrayStep(values, -1, -1, "The target is not present.", 4, false));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun binaryAnswer(int limit, int firstTrue) {
        int max = Math.max(4, Math.min(limit, 24));
        int boundary = Math.max(1, Math.min(firstTrue, max));
        List<String> code = List.of("define a monotonic predicate", "low = 1; high = limit", "mid = (low + high) / 2",
                "if feasible(mid): move high left", "otherwise move low right");
        List<SimulationStep> steps = new ArrayList<>();
        int low = 1;
        int high = max;
        while (low < high) {
            int mid = (low + high) / 2;
            steps.add(predicateStep(max, boundary, low, mid, high,
                    "Predicate at " + mid + " is " + (mid >= boundary) + ".", 2));
            if (mid >= boundary) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        steps.add(predicateStep(max, boundary, low, low, high, "The first feasible answer is " + low + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun graphTraversal(boolean depthFirst, JsonNode config) {
        GraphInput input = traversalGraph(config);
        Map<String, List<String>> graph = input.adjacency();
        List<String> code = depthFirst
                ? List.of("push the start node", "pop a node", "mark it visited", "push each unvisited neighbor", "repeat")
                : List.of("enqueue the start node", "dequeue a node", "mark it visited", "enqueue unvisited neighbors", "repeat");
        List<SimulationStep> steps = new ArrayList<>();
        ArrayDeque<String> frontier = new ArrayDeque<>();
        Set<String> visited = new LinkedHashSet<>();
        String start = config.path("start").asText("");
        if (!graph.containsKey(start)) {
            start = graph.keySet().iterator().next();
        }
        frontier.add(start);
        while (!frontier.isEmpty()) {
            String current = depthFirst ? frontier.removeLast() : frontier.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            steps.add(graphStep(graph.keySet(), visited, current,
                    (depthFirst ? "Visit " : "Expand ") + current + ".", 2, input.links()));
            List<String> neighbors = new ArrayList<>(graph.get(current));
            if (depthFirst) {
                neighbors.sort(Comparator.reverseOrder());
            }
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    frontier.addLast(neighbor);
                }
            }
            steps.add(graphStep(graph.keySet(), visited, null,
                    "Frontier: " + (frontier.isEmpty() ? "empty" : frontier), 3, input.links()));
        }
        return new SimulationRun(code, steps);
    }

    private static GraphInput traversalGraph(JsonNode config) {
        boolean directed = config.path("directed").asBoolean(false);
        List<GraphLink> links = unweightedLinks(config, List.of(
                link("A", "B", directed), link("A", "C", directed), link("B", "D", directed),
                link("B", "E", directed), link("C", "F", directed), link("E", "F", directed)), directed);
        List<String> vertices = configuredVertices(config, List.of("A", "B", "C", "D", "E", "F"), links);
        return new GraphInput(adjacency(vertices, links, !directed), links);
    }

    private static SimulationRun bellmanFord(JsonNode config) {
        List<WeightedEdge> edges = weightedEdges(config, "weightedEdges", List.of(
                new WeightedEdge("A", "B", 4), new WeightedEdge("A", "C", 2),
                new WeightedEdge("B", "C", -1), new WeightedEdge("B", "D", 2),
                new WeightedEdge("C", "D", 3), new WeightedEdge("D", "E", 1)));
        List<GraphLink> links = weightedLinks(edges, true);
        List<String> vertices = configuredVertices(config, List.of("A", "B", "C", "D", "E"), links);
        String start = config.path("start").asText("A");
        Map<String, Integer> distance = new LinkedHashMap<>();
        for (String vertex : vertices) {
            distance.put(vertex, Integer.MAX_VALUE);
        }
        if (!distance.containsKey(start)) {
            start = vertices.getFirst();
        }
        distance.put(start, 0);
        List<String> code = List.of("set source distance to zero", "scan every edge", "relax if a shorter path exists",
                "repeat V - 1 passes", "check one extra pass for a negative cycle");
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(valueGraphStep(vertices, distance, Set.of(), "Initialize distances from " + start + ".", 0, links));
        for (int pass = 1; pass < vertices.size(); pass++) {
            boolean changed = false;
            for (WeightedEdge edge : edges) {
                int from = distance.getOrDefault(edge.from(), Integer.MAX_VALUE);
                long candidate = (long) from + edge.weight();
                if (distance.containsKey(edge.to()) && from != Integer.MAX_VALUE
                        && candidate < distance.get(edge.to())) {
                    distance.put(edge.to(), boundedDistance(candidate));
                    changed = true;
                    steps.add(valueGraphStep(vertices, distance, Set.of(edge.to()),
                            "Pass " + pass + ": relax " + edge.from() + " -> " + edge.to() + ".", 2,
                            activate(links, edge.from(), edge.to())));
                }
            }
            if (!changed) {
                break;
            }
        }
        WeightedEdge cycleEdge = null;
        for (WeightedEdge edge : edges) {
            int from = distance.getOrDefault(edge.from(), Integer.MAX_VALUE);
            long candidate = (long) from + edge.weight();
            if (from != Integer.MAX_VALUE
                    && candidate < distance.getOrDefault(edge.to(), Integer.MAX_VALUE)) {
                cycleEdge = edge;
                break;
            }
        }
        if (cycleEdge != null) {
            steps.add(valueGraphStep(vertices, distance, Set.of(cycleEdge.from(), cycleEdge.to()),
                    "A reachable negative cycle is detected through " + cycleEdge.from() + " -> "
                            + cycleEdge.to() + ".", 4, activate(links, cycleEdge.from(), cycleEdge.to())));
        } else {
            steps.add(valueGraphStep(vertices, distance, new LinkedHashSet<>(vertices),
                    "No shorter path remains; Bellman-Ford is complete.", 4, links));
        }
        return new SimulationRun(code, steps);
    }

    private static SimulationRun floydWarshall(JsonNode config) {
        int[][] fallback = new int[][]{
                {0, 4, 11, 999},
                {999, 0, 2, 7},
                {999, 999, 0, 3},
                {999, 999, 999, 0}};
        int[][] distance = configuredMatrix(config, fallback);
        if (distance.length == 0 || distance[0].length != distance.length) {
            distance = copyMatrix(fallback);
        }
        for (int row = 0; row < distance.length; row++) {
            for (int column = 0; column < distance[row].length; column++) {
                if (distance[row][column] == 999) {
                    distance[row][column] = FLOYD_INFINITY;
                }
            }
        }
        List<String> vertices = configuredLabels(config, distance.length);
        List<String> code = List.of("initialize direct-edge distances", "choose intermediate k",
                "try dist[i][k] + dist[k][j]", "keep the smaller path",
                "if dist[v][v] < 0: report a negative cycle", "read the all-pairs matrix");
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(matrixStep(distance, vertices, "Start with direct-edge distances.", 0));
        for (int k = 0; k < vertices.size(); k++) {
            for (int i = 0; i < vertices.size(); i++) {
                for (int j = 0; j < vertices.size(); j++) {
                    if (distance[i][k] != FLOYD_INFINITY && distance[k][j] != FLOYD_INFINITY) {
                        int candidate = boundedFloydDistance((long) distance[i][k] + distance[k][j]);
                        if (candidate >= distance[i][j]) {
                            continue;
                        }
                        distance[i][j] = candidate;
                        steps.add(matrixStep(distance, vertices,
                                "Allow " + vertices.get(k) + " as an intermediate vertex.", 3));
                    }
                }
            }
        }
        int negativeCycle = -1;
        for (int vertex = 0; vertex < vertices.size(); vertex++) {
            if (distance[vertex][vertex] < 0) {
                negativeCycle = vertex;
                break;
            }
        }
        if (negativeCycle >= 0) {
            steps.add(matrixStep(distance, vertices,
                    "A negative cycle is reachable through " + vertices.get(negativeCycle) + ".", 4,
                    negativeCycle));
        } else {
            steps.add(matrixStep(distance, vertices,
                    "The all-pairs shortest-path matrix is complete.", 5));
        }
        return new SimulationRun(code, steps);
    }

    private static SimulationRun topologicalSort(JsonNode config) {
        List<GraphLink> links = unweightedLinks(config, List.of(
                link("A", "B", true), link("A", "C", true),
                link("B", "D", true), link("C", "D", true)), true);
        List<String> vertices = configuredVertices(config, List.of("A", "B", "C", "D"), links);
        Map<String, List<String>> graph = adjacency(vertices, links, false);
        Map<String, Integer> indegree = new LinkedHashMap<>();
        graph.keySet().forEach(vertex -> indegree.put(vertex, 0));
        for (List<String> neighbors : graph.values()) {
            for (String neighbor : neighbors) {
                indegree.put(neighbor, indegree.get(neighbor) + 1);
            }
        }
        ArrayDeque<String> queue = new ArrayDeque<>();
        indegree.forEach((vertex, degree) -> {
            if (degree == 0) {
                queue.addLast(vertex);
            }
        });
        List<String> order = new ArrayList<>();
        List<String> code = List.of("compute indegrees", "enqueue every zero-indegree vertex", "remove one vertex",
                "decrement each outgoing indegree", "append the topological order");
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(graphStep(graph.keySet(), Set.of(), queue.peekFirst(),
                "Queue all vertices with indegree zero: " + queue + ".", 1, links));
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            order.add(current);
            for (String neighbor : graph.get(current)) {
                indegree.put(neighbor, indegree.get(neighbor) - 1);
                if (indegree.get(neighbor) == 0) {
                    queue.addLast(neighbor);
                }
            }
            steps.add(graphStep(graph.keySet(), new LinkedHashSet<>(order), current,
                    "Emit " + current + "; order " + order + "; frontier " + queue + ".", 3, links));
        }
        steps.add(graphStep(graph.keySet(), new LinkedHashSet<>(order), null,
                (order.size() == graph.size() ? "Acyclic graph order: " + order
                        : "A cycle prevents a complete topological order; emitted " + order) + ".", 4, links));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun stronglyConnectedComponents(JsonNode config) {
        List<GraphLink> links = unweightedLinks(config, List.of(
                link("A", "B", true), link("B", "C", true), link("B", "D", true),
                link("C", "A", true), link("D", "E", true), link("E", "D", true)), true);
        List<String> vertices = configuredVertices(config, List.of("A", "B", "C", "D", "E"), links);
        Map<String, List<String>> graph = adjacency(vertices, links, false);
        Map<String, Integer> index = new LinkedHashMap<>();
        Map<String, Integer> low = new LinkedHashMap<>();
        ArrayDeque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new LinkedHashSet<>();
        List<List<String>> components = new ArrayList<>();
        List<String> code = List.of("assign index[v] = low[v]", "push v onto the Tarjan stack",
                "DFS each outgoing edge", "propagate low-link values", "pop one SCC when low[v] = index[v]");
        List<SimulationStep> steps = new ArrayList<>();
        int[] nextIndex = {0};
        for (String vertex : graph.keySet()) {
            if (!index.containsKey(vertex)) {
                tarjanDfs(vertex, graph, index, low, stack, onStack, components, nextIndex, steps, links);
            }
        }
        steps.add(componentGraphStep(graph.keySet(), components,
                "Tarjan SCCs: " + components + ".", 4, links));
        return new SimulationRun(code, steps);
    }

    private static void tarjanDfs(String vertex, Map<String, List<String>> graph,
            Map<String, Integer> index, Map<String, Integer> low, ArrayDeque<String> stack,
            Set<String> onStack, List<List<String>> components, int[] nextIndex,
            List<SimulationStep> steps, List<GraphLink> links) {
        int assigned = nextIndex[0]++;
        index.put(vertex, assigned);
        low.put(vertex, assigned);
        stack.push(vertex);
        onStack.add(vertex);
        steps.add(graphStep(graph.keySet(), index.keySet(), vertex,
                "Push " + vertex + " with index=low=" + assigned + ".", 1, links));
        for (String neighbor : graph.get(vertex)) {
            if (!index.containsKey(neighbor)) {
                tarjanDfs(neighbor, graph, index, low, stack, onStack, components, nextIndex, steps, links);
                low.put(vertex, Math.min(low.get(vertex), low.get(neighbor)));
            } else if (onStack.contains(neighbor)) {
                low.put(vertex, Math.min(low.get(vertex), index.get(neighbor)));
            }
            steps.add(graphStep(graph.keySet(), index.keySet(), vertex,
                    "After edge " + vertex + " -> " + neighbor + ", low[" + vertex + "]=" + low.get(vertex) + ".", 3,
                    activate(links, vertex, neighbor)));
        }
        if (low.get(vertex).equals(index.get(vertex))) {
            List<String> component = new ArrayList<>();
            String member;
            do {
                member = stack.pop();
                onStack.remove(member);
                component.add(member);
            } while (!member.equals(vertex));
            component.sort(String::compareTo);
            components.add(component);
            steps.add(graphStep(graph.keySet(), index.keySet(), vertex,
                    "Pop strongly connected component " + component + ".", 4, links));
        }
    }

    private static SimulationRun bridges(JsonNode config) {
        List<GraphLink> links = unweightedLinks(config, List.of(
                link("A", "B", false), link("B", "C", false), link("B", "D", false),
                link("C", "D", false), link("D", "E", false)), false);
        List<String> vertices = configuredVertices(config, List.of("A", "B", "C", "D", "E"), links);
        Map<String, List<String>> graph = adjacency(vertices, links, true);
        Map<String, Integer> discovery = new LinkedHashMap<>();
        Map<String, Integer> low = new LinkedHashMap<>();
        Set<String> visited = new LinkedHashSet<>();
        Set<String> found = new LinkedHashSet<>();
        Set<String> articulations = new LinkedHashSet<>();
        List<String> code = List.of("record discovery time", "propagate low-link values", "bridge when low[child] > disc[parent]",
                "mark articulation vertices with two separated child branches");
        List<SimulationStep> steps = new ArrayList<>();
        for (String vertex : graph.keySet()) {
            if (!visited.contains(vertex)) {
                bridgeDfs(vertex, null, graph, discovery, low, visited, found, articulations, steps,
                        new int[]{discovery.size()}, links);
            }
        }
        steps.add(graphStep(graph.keySet(), visited, null,
                "Bridges: " + found + "; articulation points: " + articulations + ".", 3, links));
        return new SimulationRun(code, steps);
    }

    private static void bridgeDfs(String vertex, String parent, Map<String, List<String>> graph,
            Map<String, Integer> discovery, Map<String, Integer> low, Set<String> visited,
            Set<String> found, Set<String> articulations, List<SimulationStep> steps, int[] clock,
            List<GraphLink> links) {
        int time = ++clock[0];
        discovery.put(vertex, time);
        low.put(vertex, time);
        visited.add(vertex);
        steps.add(graphStep(graph.keySet(), visited, vertex,
                "Discover " + vertex + " with low-link " + time + ".", 0, links));
        int children = 0;
        for (String neighbor : graph.get(vertex)) {
            if (neighbor.equals(parent)) {
                continue;
            }
            if (!visited.contains(neighbor)) {
                children++;
                bridgeDfs(neighbor, vertex, graph, discovery, low, visited, found, articulations, steps, clock, links);
                low.put(vertex, Math.min(low.get(vertex), low.get(neighbor)));
                if (low.get(neighbor) > discovery.get(vertex)) {
                    found.add(vertex + "-" + neighbor);
                    steps.add(graphStep(graph.keySet(), visited, neighbor,
                            "Edge " + vertex + "-" + neighbor + " is a bridge.", 2,
                            activate(links, vertex, neighbor)));
                }
                if (parent != null && low.get(neighbor) >= discovery.get(vertex)) {
                    articulations.add(vertex);
                }
            } else {
                low.put(vertex, Math.min(low.get(vertex), discovery.get(neighbor)));
            }
        }
        if (parent == null && children > 1) {
            articulations.add(vertex);
        }
    }

    private static SimulationRun maxFlow(JsonNode config) {
        List<WeightedEdge> edges = weightedEdges(config, "capacityEdges", List.of(
                new WeightedEdge("S", "A", 3), new WeightedEdge("S", "B", 2),
                new WeightedEdge("A", "B", 1), new WeightedEdge("A", "T", 2),
                new WeightedEdge("B", "T", 3))).stream()
                .filter(edge -> edge.weight() > 0).toList();
        List<GraphLink> links = weightedLinks(edges, true);
        List<String> vertices = configuredVertices(config, List.of("S", "A", "B", "T"), links);
        String source = config.path("source").asText("S");
        String sink = config.path("sink").asText("T");
        if (!vertices.contains(source)) {
            source = vertices.getFirst();
        }
        if (!vertices.contains(sink) || sink.equals(source)) {
            sink = vertices.getLast();
        }
        int sourceIndex = vertices.indexOf(source);
        int sinkIndex = vertices.indexOf(sink);
        int[][] capacity = new int[vertices.size()][vertices.size()];
        for (WeightedEdge edge : edges) {
            int from = vertices.indexOf(edge.from());
            int to = vertices.indexOf(edge.to());
            if (from >= 0 && to >= 0) {
                capacity[from][to] += edge.weight();
            }
        }
        int[][] residual = new int[capacity.length][capacity.length];
        for (int row = 0; row < capacity.length; row++) {
            residual[row] = Arrays.copyOf(capacity[row], capacity[row].length);
        }
        List<String> code = List.of("build residual capacities", "find an augmenting path", "send the bottleneck flow",
                "update forward and reverse residual edges", "stop when no path reaches the sink");
        List<SimulationStep> steps = new ArrayList<>();
        int flow = 0;
        while (true) {
            int[] parent = new int[vertices.size()];
            Arrays.fill(parent, -1);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(sourceIndex);
            parent[sourceIndex] = sourceIndex;
            while (!queue.isEmpty() && parent[sinkIndex] == -1) {
                int current = queue.removeFirst();
                for (int next = 0; next < vertices.size(); next++) {
                    if (parent[next] == -1 && residual[current][next] > 0) {
                        parent[next] = current;
                        queue.addLast(next);
                    }
                }
            }
            if (parent[sinkIndex] == -1) {
                break;
            }
            int pathFlow = Integer.MAX_VALUE;
            for (int current = sinkIndex; current != sourceIndex; current = parent[current]) {
                pathFlow = Math.min(pathFlow, residual[parent[current]][current]);
            }
            for (int current = sinkIndex; current != sourceIndex; current = parent[current]) {
                int previous = parent[current];
                residual[previous][current] -= pathFlow;
                residual[current][previous] += pathFlow;
            }
            flow += pathFlow;
            Set<String> activePath = new LinkedHashSet<>();
            List<GraphLink> activeLinks = links;
            for (int current = sinkIndex;; current = parent[current]) {
                activePath.add(vertices.get(current));
                if (current == sourceIndex) {
                    break;
                }
                activeLinks = activate(activeLinks, vertices.get(parent[current]), vertices.get(current));
            }
            steps.add(flowGraphStep(vertices, activePath, flow,
                    "Augment " + pathFlow + " units along " + activePath + "; total flow is " + flow + ".", 2,
                    activeLinks));
        }
        steps.add(flowGraphStep(vertices, Set.of(), flow,
                "No augmenting path remains; max flow is " + flow + " from " + source + " to " + sink + ".", 4,
                links));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun prefixSum(int[] values) {
        int[] prefix = new int[values.length];
        List<String> code = List.of("prefix[0] = a[0]", "prefix[i] = prefix[i - 1] + a[i]",
                "rangeSum(l, r) = prefix[r] - prefix[l - 1]", "answer a static range in O(1)");
        List<SimulationStep> steps = new ArrayList<>();
        int running = 0;
        for (int index = 0; index < values.length; index++) {
            running += values[index];
            prefix[index] = running;
            steps.add(arrayStep(prefix, index, -1, "Add a[" + index + "] to the running prefix total.", 1, false));
        }
        int right = Math.max(0, prefix.length - 1);
        int left = Math.min(1, right);
        int range = prefix[right] - (left == 0 ? 0 : prefix[left - 1]);
        steps.add(arrayStep(prefix, left, right, "Range sum [" + left + ", " + right + "] = " + range + ".", 2, true));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun rangeQuery(int[] values, String mode) {
        String normalized = mode == null ? "online" : mode.toLowerCase(Locale.ROOT);
        if (normalized.contains("offline")) {
            return offlineRangeQuery(values);
        }
        if (normalized.contains("lazy")) {
            return lazyRangeQuery(values);
        }
        return onlineRangeQuery(values);
    }

    private static SimulationRun offlineRangeQuery(int[] values) {
        List<String> code = List.of("sort known queries by left block then right endpoint",
                "move the current window", "add or remove boundary values", "restore answers to input order");
        List<SimulationStep> steps = new ArrayList<>();
        List<RangeRequest> queries = new ArrayList<>(List.of(
                new RangeRequest(0, 0, 2), new RangeRequest(1, 1, 4), new RangeRequest(2, 0, 4)));
        int blockSize = Math.max(1, (int) Math.sqrt(values.length));
        queries.sort(Comparator.comparingInt((RangeRequest query) -> query.left() / blockSize)
                .thenComparingInt(RangeRequest::right));
        int[] answers = new int[queries.size()];
        int currentLeft = 0;
        int currentRight = -1;
        int sum = 0;
        for (RangeRequest query : queries) {
            while (currentLeft > query.left()) {
                sum += values[--currentLeft];
            }
            while (currentRight < query.right()) {
                sum += values[++currentRight];
            }
            while (currentLeft < query.left()) {
                sum -= values[currentLeft++];
            }
            while (currentRight > query.right()) {
                sum -= values[currentRight--];
            }
            answers[query.id()] = sum;
            steps.add(rangeStep(values, currentLeft, currentRight,
                    "Offline query " + query.id() + " range [" + currentLeft + ", " + currentRight + "] = " + sum + ".", 2));
        }
        steps.add(rangeStep(values, -1, -1, "Answers restored to input order: " + Arrays.toString(answers) + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun onlineRangeQuery(int[] input) {
        int[] values = Arrays.copyOf(input, input.length);
        List<String> code = List.of("receive a point update", "update the maintained structure",
                "combine covered ranges", "return the current range result");
        List<SimulationStep> steps = new ArrayList<>();
        int index = Math.min(2, values.length - 1);
        int oldValue = values[index];
        values[index] += 2;
        steps.add(arrayStep(values, index, -1,
                "Point update changes a[" + index + "] from " + oldValue + " to " + values[index] + ".", 1, false));
        int left = Math.min(1, values.length - 1);
        int right = Math.min(3, values.length - 1);
        int sum = 0;
        for (int cursor = left; cursor <= right; cursor++) {
            sum += values[cursor];
        }
        steps.add(rangeStep(values, left, right,
                "After the update, range sum [" + left + ", " + right + "] = " + sum + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun lazyRangeQuery(int[] input) {
        int[] values = Arrays.copyOf(input, input.length);
        List<String> code = List.of("cover the update interval", "store a lazy delta on a covered node",
                "push the delta before descending", "combine updated child sums");
        List<SimulationStep> steps = new ArrayList<>();
        int left = Math.min(1, values.length - 1);
        int right = Math.min(3, values.length - 1);
        int delta = 2;
        for (int index = left; index <= right; index++) {
            values[index] += delta;
        }
        steps.add(rangeStep(values, left, right,
                "Apply lazy range addition +" + delta + " to [" + left + ", " + right + "].", 1));
        int sum = 0;
        for (int index = left; index <= right; index++) {
            sum += values[index];
        }
        steps.add(rangeStep(values, left, right,
                "Push pending deltas on demand; updated range sum is " + sum + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun sparseTable(int[] values) {
        List<String> code = List.of("store intervals of length 2^k", "build level k from level k - 1",
                "choose two overlapping intervals", "answer an idempotent range query");
        List<SimulationStep> steps = new ArrayList<>();
        List<Integer> level = Arrays.stream(values).boxed().toList();
        List<List<Integer>> levels = new ArrayList<>();
        levels.add(level);
        steps.add(chainStep(level.stream().map(String::valueOf).toList(), -1,
                "Level 0 stores every one-element interval.", 0));
        int width = 2;
        while (width <= values.length) {
            List<Integer> previous = level;
            List<Integer> next = new ArrayList<>();
            for (int index = 0; index + width <= values.length; index++) {
                next.add(Math.min(previous.get(index), previous.get(index + width / 2)));
            }
            level = next;
            levels.add(level);
            steps.add(chainStep(level.stream().map(String::valueOf).toList(), -1,
                    "Build intervals of length " + width + " from two half intervals.", 1));
            width *= 2;
        }
        int left = Math.min(1, values.length - 1);
        int right = Math.min(4, values.length - 1);
        int length = right - left + 1;
        int power = 31 - Integer.numberOfLeadingZeros(length);
        int span = 1 << power;
        int answer = Math.min(levels.get(power).get(left), levels.get(power).get(right - span + 1));
        steps.add(chainStep(levels.get(power).stream().map(String::valueOf).toList(), left,
                "RMQ [" + left + ", " + right + "] uses two length-" + span + " intervals; minimum = " + answer + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun sqrtDecomposition(int[] values) {
        int blockSize = Math.max(1, (int) Math.ceil(Math.sqrt(values.length)));
        int[] blocks = new int[(values.length + blockSize - 1) / blockSize];
        List<String> code = List.of("choose block size sqrt(n)", "add values to block summaries",
                "combine whole blocks", "scan the two boundary fragments");
        List<SimulationStep> steps = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            blocks[index / blockSize] += values[index];
            steps.add(arrayStep(blocks, index / blockSize, -1,
                    "Add value " + values[index] + " to block " + index / blockSize + ".", 1, false));
        }
        int left = Math.min(1, values.length - 1);
        int right = Math.min(4, values.length - 1);
        int cursor = left;
        int query = 0;
        while (cursor <= right && cursor % blockSize != 0) {
            query += values[cursor];
            steps.add(rangeStep(values, cursor, cursor,
                    "Scan left boundary value a[" + cursor + "]=" + values[cursor] + ".", 3));
            cursor++;
        }
        while (cursor + blockSize - 1 <= right) {
            int block = cursor / blockSize;
            query += blocks[block];
            steps.add(arrayStep(blocks, block, -1,
                    "Use complete block " + block + " with sum " + blocks[block] + ".", 2, false));
            cursor += blockSize;
        }
        while (cursor <= right) {
            query += values[cursor];
            steps.add(rangeStep(values, cursor, cursor,
                    "Scan right boundary value a[" + cursor + "]=" + values[cursor] + ".", 3));
            cursor++;
        }
        steps.add(rangeStep(values, left, right,
                "Sqrt-decomposition range sum [" + left + ", " + right + "] = " + query + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun divideAndConquer(int[] input) {
        int[] values = Arrays.copyOf(input, input.length);
        List<String> code = List.of("divide the range at its midpoint", "solve each half recursively",
                "combine the two partial results", "return the combined result");
        List<SimulationStep> steps = new ArrayList<>();
        divideTrace(values, 0, values.length - 1, steps);
        steps.add(arrayStep(values, -1, -1, "The recursive halves have been combined.", 3, true));
        return new SimulationRun(code, steps);
    }

    private static void divideTrace(int[] values, int low, int high, List<SimulationStep> steps) {
        if (low >= high) {
            return;
        }
        int mid = (low + high) / 2;
        steps.add(arrayStep(values, low, high, "Divide indices " + low + " through " + high + ".", 0, false));
        divideTrace(values, low, mid, steps);
        divideTrace(values, mid + 1, high, steps);
        int[] merged = new int[high - low + 1];
        int left = low;
        int right = mid + 1;
        int write = 0;
        while (left <= mid || right <= high) {
            if (right > high || left <= mid && values[left] <= values[right]) {
                merged[write++] = values[left++];
            } else {
                merged[write++] = values[right++];
            }
        }
        System.arraycopy(merged, 0, values, low, merged.length);
        steps.add(arrayStep(values, low, high, "Merge both sorted halves into one sorted range.", 2, false));
    }

    private static SimulationRun greedy(JsonNode config) {
        int[][] intervals = configuredPairs(config, "intervals", new int[][]{{1, 3}, {2, 4}, {3, 5}, {5, 7}});
        Arrays.sort(intervals, Comparator.comparingInt(interval -> interval[1]));
        List<String> code = List.of("sort choices by earliest finish", "take the first compatible choice",
                "discard choices that overlap", "repeat until choices are exhausted");
        List<SimulationStep> steps = new ArrayList<>();
        List<String> selected = new ArrayList<>();
        int finish = Integer.MIN_VALUE;
        for (int[] interval : intervals) {
            List<String> cells = new ArrayList<>();
            for (int[] candidate : intervals) {
                cells.add("[" + candidate[0] + "," + candidate[1] + "]");
            }
            if (interval[0] >= finish) {
                selected.add("[" + interval[0] + "," + interval[1] + "]");
                finish = interval[1];
                steps.add(chainStep(selected, selected.size() - 1,
                        "Select the earliest-finishing compatible interval.", 1));
            } else {
                steps.add(chainStep(cells, -1, "Skip the overlapping interval " + Arrays.toString(interval) + ".", 2));
            }
        }
        steps.add(chainStep(selected, -1, "Greedy selection keeps " + selected.size() + " compatible intervals.", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun dynamicProgramming(int requested) {
        int n = Math.max(3, Math.min(requested, 12));
        int[] table = new int[n + 1];
        table[0] = 0;
        table[1] = 1;
        List<String> code = List.of("set base cases", "reuse dp[i - 1]", "reuse dp[i - 2]", "store dp[i] = dp[i - 1] + dp[i - 2]");
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(arrayStep(table, 1, 0, "Initialize the two Fibonacci base cases.", 0, false));
        for (int index = 2; index <= n; index++) {
            table[index] = table[index - 1] + table[index - 2];
            steps.add(arrayStep(table, index, index - 1, "Fill dp[" + index + "] from two stored subproblems.", 3, false));
        }
        steps.add(arrayStep(table, n, -1, "The dynamic-programming table yields " + table[n] + ".", 3, true));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun backtracking(int requested) {
        int size = Math.max(3, Math.min(requested, 5));
        List<String> code = List.of("choose a candidate column", "check previous rows", "recurse to the next row",
                "undo the choice when a constraint fails", "record a complete placement");
        List<SimulationStep> steps = new ArrayList<>();
        int[] placement = new int[size];
        Arrays.fill(placement, -1);
        backtrackQueens(placement, 0, steps);
        if (steps.isEmpty()) {
            steps.add(arrayStep(placement, -1, -1, "No placement was found in the bounded board.", 3, false));
        }
        steps.add(arrayStep(placement, -1, -1, "Backtracking returns after exploring the candidate tree.", 4, true));
        return new SimulationRun(code, steps);
    }

    private static boolean backtrackQueens(int[] placement, int row, List<SimulationStep> steps) {
        if (row == placement.length) {
            steps.add(arrayStep(placement, -1, -1, "A valid placement satisfies every row constraint.", 4, true));
            return true;
        }
        for (int column = 0; column < placement.length; column++) {
            placement[row] = column;
            steps.add(arrayStep(placement, row, column, "Try row " + row + " at column " + column + ".", 0, false));
            if (safeQueens(placement, row) && backtrackQueens(placement, row + 1, steps)) {
                return true;
            }
            placement[row] = -1;
            steps.add(arrayStep(placement, row, -1, "Undo the choice and try the next column.", 3, false));
        }
        return false;
    }

    private static boolean safeQueens(int[] placement, int row) {
        for (int previous = 0; previous < row; previous++) {
            if (placement[previous] == placement[row]
                    || Math.abs(placement[previous] - placement[row]) == row - previous) {
                return false;
            }
        }
        return true;
    }

    private static SimulationRun stringMatching(String text, String pattern) {
        String safeText = text == null ? "" : text;
        String safePattern = pattern == null ? "" : pattern;
        int[] prefix = new int[safePattern.length()];
        List<String> code = List.of("build the KMP prefix-function table", "compare text[i] with pattern[j]",
                "fall back to prefix[j - 1] on a mismatch", "advance both indices on a match", "report each KMP match",
                "build pattern + separator + text", "maintain the Z-box [left, right]", "report positions with Z[i] = pattern length");
        List<SimulationStep> steps = new ArrayList<>();
        for (int index = 1; index < safePattern.length(); index++) {
            int candidate = prefix[index - 1];
            while (candidate > 0 && safePattern.charAt(index) != safePattern.charAt(candidate)) {
                candidate = prefix[candidate - 1];
            }
            if (safePattern.charAt(index) == safePattern.charAt(candidate)) {
                candidate++;
            }
            prefix[index] = candidate;
            steps.add(stringStep(safePattern, index, "Prefix length at pattern index " + index + " is " + candidate + ".", 0));
        }
        int matched = 0;
        List<Integer> kmpMatches = new ArrayList<>();
        for (int index = 0; index < safeText.length(); index++) {
            while (matched > 0 && safeText.charAt(index) != safePattern.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (!safePattern.isEmpty() && safeText.charAt(index) == safePattern.charAt(matched)) {
                matched++;
            }
            steps.add(stringStep(safeText, index, "Scan text index " + index + "; matched prefix length " + matched + ".", 1));
            if (matched == safePattern.length() && !safePattern.isEmpty()) {
                int start = index - safePattern.length() + 1;
                kmpMatches.add(start);
                steps.add(stringStep(safeText, index, "KMP match starts at text index " + start + ".", 4));
                matched = prefix[matched - 1];
            }
        }
        if (safePattern.isEmpty()) {
            steps.add(stringStep(safeText, -1, "The pattern has no characters to match.", 4));
            return new SimulationRun(code, steps);
        }

        String combined = safePattern + "$" + safeText;
        int[] z = new int[combined.length()];
        int left = 0;
        int right = 0;
        List<Integer> zMatches = new ArrayList<>();
        for (int index = 1; index < combined.length(); index++) {
            if (index <= right) {
                z[index] = Math.min(right - index + 1, z[index - left]);
            }
            while (index + z[index] < combined.length()
                    && combined.charAt(z[index]) == combined.charAt(index + z[index])) {
                z[index]++;
            }
            if (index + z[index] - 1 > right) {
                left = index;
                right = index + z[index] - 1;
            }
            steps.add(stringStep(combined, index,
                    "Z[" + index + "]=" + z[index] + " with box [" + left + ", " + right + "].", 6));
            if (z[index] == safePattern.length()) {
                zMatches.add(index - safePattern.length() - 1);
            }
        }
        steps.add(stringStep(safeText, -1,
                "KMP matches " + kmpMatches + "; Z-function matches " + zMatches + ".", 7));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun suffixStructure(String text) {
        String safeText = text == null ? "" : text;
        List<Integer> suffixes = new ArrayList<>();
        for (int index = 0; index < safeText.length(); index++) {
            suffixes.add(index);
        }
        List<String> code = List.of("list every suffix start", "compare suffixes lexicographically",
                "sort suffix starts", "read the suffix-array indices", "start the suffix automaton at state 0",
                "extend with one character", "clone a state when transition lengths disagree", "follow suffix links");
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(chainStep(suffixes.stream().map(index -> safeText.substring(index)).toList(), -1,
                "Enumerate all suffixes of the text.", 0));
        suffixes.sort(Comparator.comparing(safeText::substring));
        steps.add(chainStep(suffixes.stream().map(index -> safeText.substring(index)).toList(), 0,
                "Sort suffixes to form the suffix-array order.", 2));
        steps.add(chainStep(suffixes.stream().map(index -> Integer.toString(index)).toList(), -1,
                "Suffix starts expose lexicographic substring structure.", 3));

        List<SuffixAutomatonState> states = new ArrayList<>();
        states.add(new SuffixAutomatonState(0, -1));
        int last = 0;
        for (int index = 0; index < safeText.length(); index++) {
            char character = safeText.charAt(index);
            SamExtension extension = extendSuffixAutomaton(states, last, character);
            last = extension.last();
            steps.add(textStep("Extend the suffix automaton with '" + character + "'"
                            + (extension.cloneState() >= 0 ? " using clone s" + extension.cloneState() : "") + ".",
                    extension.cloneState() >= 0 ? 6 : 5, suffixAutomatonCells(states, last)));
        }
        steps.add(textStep("Suffix array " + suffixes + "; suffix automaton has " + states.size()
                        + " states and represents every substring of \"" + safeText + "\".",
                7, suffixAutomatonCells(states, last)));
        return new SimulationRun(code, steps);
    }

    private static SamExtension extendSuffixAutomaton(
            List<SuffixAutomatonState> states, int last, char character) {
        int current = states.size();
        states.add(new SuffixAutomatonState(states.get(last).length + 1, 0));
        int previous = last;
        while (previous >= 0 && !states.get(previous).transitions.containsKey(character)) {
            states.get(previous).transitions.put(character, current);
            previous = states.get(previous).link;
        }
        int clone = -1;
        if (previous < 0) {
            states.get(current).link = 0;
        } else {
            int next = states.get(previous).transitions.get(character);
            if (states.get(previous).length + 1 == states.get(next).length) {
                states.get(current).link = next;
            } else {
                clone = states.size();
                SuffixAutomatonState cloneState = new SuffixAutomatonState(
                        states.get(previous).length + 1, states.get(next).link);
                cloneState.transitions.putAll(states.get(next).transitions);
                states.add(cloneState);
                while (previous >= 0
                        && states.get(previous).transitions.getOrDefault(character, -1) == next) {
                    states.get(previous).transitions.put(character, clone);
                    previous = states.get(previous).link;
                }
                states.get(next).link = clone;
                states.get(current).link = clone;
            }
        }
        return new SamExtension(current, clone);
    }

    private static List<Cell> suffixAutomatonCells(List<SuffixAutomatonState> states, int active) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < states.size(); index++) {
            SuffixAutomatonState state = states.get(index);
            String transitions = state.transitions.entrySet().stream()
                    .map(entry -> entry.getKey() + "→s" + entry.getValue())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("none");
            cells.add(new Cell("s" + index + " len=" + state.length + " link=" + state.link
                    + "\n" + transitions, index == active ? "active" : "default"));
        }
        return cells;
    }

    private static SimulationRun stringHashing(String text, String pattern) {
        String safeText = text == null ? "" : text;
        String safePattern = pattern == null ? "" : pattern;
        long modulus = 1_000_000_007L;
        long base = 257L;
        List<String> code = List.of("choose a base and modulus", "extend the rolling hash",
                "remove the outgoing character", "compare equal-length fingerprints", "verify a candidate match");
        List<SimulationStep> steps = new ArrayList<>();
        if (safePattern.isEmpty() || safePattern.length() > safeText.length()) {
            steps.add(stringStep(safeText, -1, "No equal-length text window can be hashed.", 4));
            return new SimulationRun(code, steps);
        }
        int length = safePattern.length();
        long patternHash = 0;
        long windowHash = 0;
        long highestPower = 1;
        for (int index = 0; index < safePattern.length(); index++) {
            patternHash = (patternHash * base + safePattern.charAt(index)) % modulus;
            windowHash = (windowHash * base + safeText.charAt(index)) % modulus;
            if (index < safePattern.length() - 1) {
                highestPower = highestPower * base % modulus;
            }
        }
        List<Integer> matches = new ArrayList<>();
        for (int start = 0; start + length <= safeText.length(); start++) {
            steps.add(stringWindowStep(safeText, start, length,
                    "Window [" + start + ", " + (start + length - 1) + "] hash=" + windowHash
                            + "; pattern hash=" + patternHash + ".", 3));
            if (windowHash == patternHash && safeText.regionMatches(start, safePattern, 0, length)) {
                matches.add(start);
                steps.add(stringWindowStep(safeText, start, length,
                        "Hash candidate verified character-by-character at index " + start + ".", 4));
            }
            if (start + length < safeText.length()) {
                long outgoing = safeText.charAt(start) * highestPower % modulus;
                windowHash = (windowHash - outgoing + modulus) % modulus;
                windowHash = (windowHash * base + safeText.charAt(start + length)) % modulus;
            }
        }
        steps.add(stringStep(safeText, -1, "Verified rolling-hash matches at " + matches + ".", 4));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun algebra(int a, int b, int x) {
        int value = a * x + b;
        List<String> code = List.of("write the expression a*x + b", "substitute the known x", "multiply before adding",
                "evaluate the simplified expression");
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(chainStep(List.of("a=" + a, "x=" + x, "b=" + b), 1, "Substitute x = " + x + ".", 1));
        steps.add(chainStep(List.of(a + " * " + x, " + " + b), 0, "Compute the product before the sum.", 2));
        steps.add(chainStep(List.of("result=" + value), 0, "The expression simplifies to " + value + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun linearAlgebra(JsonNode config) {
        int[][] matrix = configuredMatrix(config, new int[][]{{1, 2}, {3, 4}});
        int[] vector = configuredArray(config.path("vector"), new int[]{5, 6});
        int width = matrix.length == 0 ? 0 : matrix[0].length;
        if (vector.length != width) {
            vector = Arrays.copyOf(vector, width);
        }
        int[] result = new int[matrix.length];
        List<String> code = List.of("select one matrix row", "multiply matching vector entries", "sum the products", "write the output coordinate");
        List<SimulationStep> steps = new ArrayList<>();
        for (int row = 0; row < matrix.length; row++) {
            for (int column = 0; column < matrix[row].length; column++) {
                result[row] += matrix[row][column] * vector[column];
            }
            steps.add(arrayStep(result, row, -1, "Compute row " + row + " dot vector = " + result[row] + ".", 2, false));
        }
        steps.add(arrayStep(result, -1, -1, "Matrix-vector multiplication is complete.", 3, true));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun numberTheory(int a, int b) {
        int left = Math.abs(a);
        int right = Math.abs(b);
        List<String> code = List.of("set a and b", "replace (a, b) with (b, a mod b)", "repeat while b != 0", "return the gcd");
        List<SimulationStep> steps = new ArrayList<>();
        while (right != 0) {
            steps.add(chainStep(List.of("a=" + left, "b=" + right), 1,
                    "Apply Euclid's step: a mod b = " + left % right + ".", 1));
            int remainder = left % right;
            left = right;
            right = remainder;
        }
        steps.add(chainStep(List.of("gcd=" + left), 0, "The greatest common divisor is " + left + ".", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun combinatorics(int requested) {
        int n = Math.max(2, Math.min(requested, 8));
        List<Integer> row = new ArrayList<>(List.of(1));
        List<String> code = List.of("start with C(0, 0) = 1", "add adjacent values from the previous row",
                "place one at each edge", "read combinations from Pascal's triangle");
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(chainStep(row, 0, "Initialize Pascal's triangle.", 0));
        for (int level = 1; level <= n; level++) {
            List<Integer> next = new ArrayList<>();
            next.add(1);
            for (int index = 1; index < row.size(); index++) {
                next.add(row.get(index - 1) + row.get(index));
            }
            next.add(1);
            row = next;
            steps.add(chainStep(row, row.size() / 2, "Build combinations for n = " + level + ".", 1));
        }
        steps.add(chainStep(row, -1, "The row contains all C(" + n + ", k) values.", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun geometry(JsonNode config) {
        int[] a = point(config.path("a"), 0, 0);
        int[] b = point(config.path("b"), 4, 1);
        int[] c = point(config.path("c"), 2, 5);
        int cross = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
        List<String> code = List.of("subtract point A from B and C", "compute the 2D cross product",
                "positive means counter-clockwise", "use orientation in an intersection test");
        List<SimulationStep> steps = new ArrayList<>();
        steps.add(chainStep(List.of(pointText("A", a), pointText("B", b), pointText("C", c)), 0,
                "Represent the three points.", 0));
        steps.add(chainStep(List.of("cross=" + cross), 0,
                "The orientation cross product is " + cross + ".", 1));
        String direction = cross > 0 ? "counter-clockwise" : cross < 0 ? "clockwise" : "collinear";
        steps.add(chainStep(List.of(direction), 0,
                "Classify the turn from A to B to C.", 2));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun segmentTree(int[] values, String operation) {
        List<String> code = List.of("create a tree array", "split the current range", "store each leaf value",
                "combine children with sum", "answer or lazily update a covered range");
        int size = 1;
        while (size < values.length) {
            size *= 2;
        }
        int[] tree = new int[size * 2];
        int[] lazy = new int[size * 2];
        List<SimulationStep> steps = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            tree[size + index] = values[index];
            steps.add(segmentTreeStep(tree, lazy, size + index,
                    "Place leaf value " + values[index] + ".", 2));
        }
        for (int index = size - 1; index > 0; index--) {
            tree[index] = tree[index * 2] + tree[index * 2 + 1];
            steps.add(segmentTreeStep(tree, lazy, index,
                    "Combine children into node " + index + ".", 3));
        }
        int left = Math.min(1, values.length - 1);
        int right = Math.min(3, values.length - 1);
        String normalized = operation == null ? "sum" : operation.toLowerCase(Locale.ROOT);
        if (normalized.contains("lazy")) {
            int delta = 2;
            segmentRangeAdd(tree, lazy, 1, 0, size - 1, left, right, delta, steps);
            int result = segmentRangeSum(tree, lazy, 1, 0, size - 1, left, right);
            steps.add(segmentTreeStep(tree, lazy, 1,
                    "After lazy add +" + delta + ", range sum [" + left + ", " + right + "] = " + result + ".", 4));
        } else {
            int result = segmentRangeSum(tree, lazy, 1, 0, size - 1, left, right);
            steps.add(segmentTreeStep(tree, lazy, 1,
                    "Segment-tree range sum [" + left + ", " + right + "] = " + result + ".", 4));
        }
        return new SimulationRun(code, steps);
    }

    private static void segmentRangeAdd(int[] tree, int[] lazy, int node, int start, int end,
            int queryLeft, int queryRight, int delta, List<SimulationStep> steps) {
        if (queryRight < start || end < queryLeft) {
            return;
        }
        if (queryLeft <= start && end <= queryRight) {
            tree[node] += delta * (end - start + 1);
            lazy[node] += delta;
            steps.add(segmentTreeStep(tree, lazy, node,
                    "Store lazy +" + delta + " on covered node [" + start + ", " + end + "].", 1));
            return;
        }
        pushSegmentLazy(tree, lazy, node, start, end);
        int middle = (start + end) / 2;
        segmentRangeAdd(tree, lazy, node * 2, start, middle, queryLeft, queryRight, delta, steps);
        segmentRangeAdd(tree, lazy, node * 2 + 1, middle + 1, end, queryLeft, queryRight, delta, steps);
        tree[node] = tree[node * 2] + tree[node * 2 + 1];
    }

    private static int segmentRangeSum(int[] tree, int[] lazy, int node, int start, int end,
            int queryLeft, int queryRight) {
        if (queryRight < start || end < queryLeft) {
            return 0;
        }
        if (queryLeft <= start && end <= queryRight) {
            return tree[node];
        }
        pushSegmentLazy(tree, lazy, node, start, end);
        int middle = (start + end) / 2;
        return segmentRangeSum(tree, lazy, node * 2, start, middle, queryLeft, queryRight)
                + segmentRangeSum(tree, lazy, node * 2 + 1, middle + 1, end, queryLeft, queryRight);
    }

    private static void pushSegmentLazy(int[] tree, int[] lazy, int node, int start, int end) {
        if (lazy[node] == 0 || start == end) {
            return;
        }
        int middle = (start + end) / 2;
        int delta = lazy[node];
        tree[node * 2] += delta * (middle - start + 1);
        tree[node * 2 + 1] += delta * (end - middle);
        lazy[node * 2] += delta;
        lazy[node * 2 + 1] += delta;
        lazy[node] = 0;
    }

    private static SimulationStep segmentTreeStep(
            int[] tree, int[] lazy, int activeNode, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int node = 1; node < tree.length; node++) {
            String label = lazy[node] == 0
                    ? Integer.toString(tree[node])
                    : tree[node] + "\nlazy:+" + lazy[node];
            cells.add(new Cell(label, node == activeNode ? "active" : "default"));
        }
        return step(message, line, cells);
    }

    private static SimulationRun fenwick(int[] values) {
        List<String> code = List.of("tree starts with zeros", "add value at index i", "tree[i] += value",
                "i += i & -i", "repeat while i <= n");
        int[] tree = new int[values.length + 1];
        List<SimulationStep> steps = new ArrayList<>();
        for (int index = 1; index <= values.length; index++) {
            int current = index;
            while (current < tree.length) {
                tree[current] += values[index - 1];
                steps.add(fenwickStep(tree, current,
                        "Add " + values[index - 1] + " to Fenwick node " + current + ".", 2, false));
                current += current & -current;
            }
        }
        int current = values.length;
        int prefix = 0;
        while (current > 0) {
            prefix += tree[current];
            steps.add(fenwickStep(tree, current,
                    "Read tree[" + current + "]=" + tree[current] + " into the prefix total.", 4, false));
            current -= current & -current;
        }
        steps.add(fenwickStep(tree, -1,
                "Fenwick prefix sum through index " + values.length + " = " + prefix + ".", 4, true));
        return new SimulationRun(code, steps);
    }

    private static SimulationStep fenwickStep(
            int[] tree, int activeNode, String message, int line, boolean done) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 1; index < tree.length; index++) {
            String style = done ? "done" : index == activeNode ? "active" : "default";
            cells.add(new Cell("i=" + index + "\n" + tree[index], style));
        }
        return step(message, line, cells);
    }

    private static SimulationRun dsu(int requestedSize) {
        int size = Math.max(4, Math.min(requestedSize, 10));
        int[] parent = new int[size];
        int[] componentSize = new int[size];
        for (int index = 0; index < size; index++) {
            parent[index] = index;
            componentSize[index] = 1;
        }
        List<String> code = List.of("make each node its own parent", "find each root", "attach the smaller tree",
                "compress the path during find");
        List<SimulationStep> steps = new ArrayList<>();
        int[][] unions = {{0, 1}, {2, 3}, {1, 2}, {4, 5}, {0, 5}};
        for (int[] union : unions) {
            if (union[1] >= size) {
                continue;
            }
            int left = find(parent, union[0]);
            int right = find(parent, union[1]);
            if (left != right) {
                if (componentSize[left] < componentSize[right]) {
                    int temporary = left;
                    left = right;
                    right = temporary;
                }
                parent[right] = left;
                componentSize[left] += componentSize[right];
            }
            steps.add(arrayStep(parent, union[0], union[1],
                    "Union " + union[0] + " and " + union[1] + "; root " + right + " now points to " + left + ".",
                    2, false));
        }
        for (int index = 0; index < size; index++) {
            parent[index] = find(parent, index);
        }
        steps.add(arrayStep(parent, -1, -1, "Path compression flattens each connected component.", 3, true));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun heap(int[] values, String kind) {
        boolean maximum = "max".equalsIgnoreCase(kind);
        String order = maximum ? "maximum" : "minimum";
        List<String> code = List.of("append the new value", "compare with its parent",
                "swap while heap order is violated", "the root is the " + order);
        List<Integer> heap = new ArrayList<>();
        List<SimulationStep> steps = new ArrayList<>();
        for (int value : values) {
            heap.add(value);
            int index = heap.size() - 1;
            steps.add(listStep(heap, index, -1, "Append " + value + " to the heap.", 0));
            while (index > 0) {
                int parent = (index - 1) / 2;
                steps.add(listStep(heap, index, parent, "Compare with parent " + heap.get(parent) + ".", 1));
                boolean ordered = maximum
                        ? heap.get(parent) >= heap.get(index)
                        : heap.get(parent) <= heap.get(index);
                if (ordered) {
                    break;
                }
                int temporary = heap.get(parent);
                heap.set(parent, heap.get(index));
                heap.set(index, temporary);
                steps.add(listStep(heap, parent, index, "Swap upward to restore heap order.", 2));
                index = parent;
            }
        }
        steps.add(listStep(heap, 0, -1, "The " + order + " value is at the root.", 3));
        return new SimulationRun(code, steps);
    }

    private static SimulationRun graphAlgorithm(Algorithm algorithm, String start) {
        List<String> code = switch (algorithm) {
            case DIJKSTRA -> List.of("set start distance to zero", "choose the nearest unvisited vertex",
                    "mark the vertex visited", "relax each outgoing edge", "update improved distances", "repeat");
            case PRIM -> List.of("start with one vertex", "inspect crossing edges", "choose the lightest edge",
                    "add its new vertex", "repeat until the tree spans the graph", "report total weight");
            case KRUSKAL -> List.of("sort edges by weight", "inspect the next edge", "find both component roots",
                    "accept if roots differ", "union accepted components", "stop after V - 1 edges");
            case BELLMAN_FORD -> List.of("set source distance to zero", "repeat V - 1 passes",
                    "inspect every directed edge", "relax shorter paths", "stop when unchanged",
                    "check for a reachable negative cycle");
            case FLOYD_WARSHALL -> List.of("initialize direct distances", "choose intermediate k",
                    "inspect each source and destination", "keep the shorter path", "repeat for every k",
                    "check the diagonal for a negative cycle", "read all-pairs distances");
        };
        List<SimulationStep> steps = GraphAlgorithms.run(algorithm, start).stream()
                .map(graphStep -> {
                    List<Cell> cells = GraphAlgorithms.sampleGraph().vertices().stream()
                            .map(vertex -> {
                                Integer value = graphStep.values().get(vertex.id());
                                String label = value == null || value == Integer.MAX_VALUE
                                        ? vertex.id() : vertex.id() + "\n" + value;
                                String style = graphStep.visitedVertices().contains(vertex.id()) ? "done" : "default";
                                return new Cell(label, style);
                            })
                            .toList();
                    String edgeState = graphStep.selectedEdgeIds().isEmpty()
                            ? "" : " Selected edges: " + graphStep.selectedEdgeIds();
                    return step(graphStep.message() + edgeState, graphStep.pseudocodeLine(), cells);
                })
                .toList();
        return new SimulationRun(code, steps);
    }

    private static int find(int[] parent, int node) {
        if (parent[node] != node) {
            parent[node] = find(parent, parent[node]);
        }
        return parent[node];
    }

    private static SimulationStep arrayStep(
            int[] values, int first, int second, String message, int line, boolean done) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            String style = done ? "done" : index == first ? "active" : index == second ? "selected" : "default";
            cells.add(new Cell(Integer.toString(values[index]), style));
        }
        return step(message, line, cells);
    }

    private static SimulationStep rangeStep(int[] values, int left, int right, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            String style = index == left || index == right
                    ? "active"
                    : left >= 0 && index > left && index < right ? "selected" : "default";
            cells.add(new Cell(Integer.toString(values[index]), style));
        }
        return step(message, line, cells);
    }

    private static SimulationStep listStep(
            List<Integer> values, int first, int second, String message, int line) {
        return arrayStep(values.stream().mapToInt(Integer::intValue).toArray(), first, second, message, line, false);
    }

    private static SimulationStep windowStep(int[] values, int low, int mid, int high, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            String style = index == mid ? "active" : index >= low && index <= high ? "selected" : "muted";
            cells.add(new Cell(Integer.toString(values[index]), style));
        }
        return step(message, line, cells);
    }

    private static SimulationStep predicateStep(
            int limit, int boundary, int low, int mid, int high, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int value = 1; value <= limit; value++) {
            String style = value == mid ? "active"
                    : value < low || value > high ? "muted"
                    : value >= boundary ? "done" : "selected";
            cells.add(new Cell(value + "\n" + (value >= boundary ? "T" : "F"), style));
        }
        return step(message, line, cells);
    }

    private static SimulationStep graphStep(
            Set<String> vertices, Set<String> visited, String current, String message, int line) {
        return graphStep(vertices, visited, current, message, line, List.of());
    }

    private static SimulationStep graphStep(
            Set<String> vertices, Set<String> visited, String current, String message, int line,
            List<GraphLink> links) {
        List<Cell> cells = vertices.stream().sorted()
                .map(vertex -> new Cell(vertex,
                        vertex.equals(current) ? "active" : visited.contains(vertex) ? "done" : "default"))
                .toList();
        return step(message, line, cells, links);
    }

    private static SimulationStep componentGraphStep(
            Set<String> vertices, List<List<String>> components, String message, int line,
            List<GraphLink> links) {
        Map<String, Integer> componentByVertex = new LinkedHashMap<>();
        for (int index = 0; index < components.size(); index++) {
            for (String vertex : components.get(index)) {
                componentByVertex.put(vertex, index + 1);
            }
        }
        List<Cell> cells = vertices.stream().sorted().map(vertex -> {
            int component = componentByVertex.getOrDefault(vertex, 0);
            String style = component % 2 == 0 ? "selected" : "done";
            return new Cell(vertex + "\nSCC " + component, style);
        }).toList();
        return step(message, line, cells, links);
    }

    private static SimulationStep chainStep(List<?> values, int active, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            String style = index == active ? "active" : active >= 0 && index < active ? "selected" : "default";
            cells.add(new Cell(String.valueOf(values.get(index)), style));
        }
        if (cells.isEmpty()) {
            cells.add(new Cell("empty", "muted"));
        }
        return step(message, line, cells);
    }

    private static SimulationStep textStep(String message, int line, List<Cell> cells) {
        return step(message, line, cells.isEmpty() ? List.of(new Cell("empty", "muted")) : cells);
    }

    private static SimulationStep bucketStep(List<List<Integer>> buckets, int active, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < buckets.size(); index++) {
            String style = index == active ? "active" : "default";
            cells.add(new Cell("b" + index + ": " + buckets.get(index), style));
        }
        return step(message, line, cells);
    }

    private static SimulationStep valueGraphStep(List<String> vertices, Map<String, Integer> values,
            Set<String> active, String message, int line, List<GraphLink> links) {
        List<Cell> cells = new ArrayList<>();
        for (String vertex : vertices) {
            Integer value = values.get(vertex);
            String label = value == null || value == Integer.MAX_VALUE ? vertex + "\n∞" : vertex + "\n" + value;
            cells.add(new Cell(label, active.contains(vertex) ? "active" : "default"));
        }
        return step(message, line, cells, links);
    }

    private static SimulationStep flowGraphStep(
            List<String> vertices, Set<String> activePath, int flow, String message, int line,
            List<GraphLink> links) {
        List<Cell> cells = new ArrayList<>();
        for (String vertex : vertices) {
            cells.add(new Cell(vertex + "\nflow=" + flow,
                    activePath.contains(vertex) ? "active" : "default"));
        }
        return step(message, line, cells, links);
    }

    private static SimulationStep matrixStep(int[][] matrix, List<String> labels, String message, int line) {
        return matrixStep(matrix, labels, message, line, -1);
    }

    private static SimulationStep matrixStep(
            int[][] matrix, List<String> labels, String message, int line, int activeDiagonal) {
        List<Cell> cells = new ArrayList<>();
        for (int row = 0; row < matrix.length; row++) {
            for (int column = 0; column < matrix[row].length; column++) {
                String value = matrix[row][column] == FLOYD_INFINITY
                        ? "∞" : Integer.toString(matrix[row][column]);
                cells.add(new Cell(labels.get(row) + "→" + labels.get(column) + "\n" + value,
                        row == activeDiagonal && column == activeDiagonal ? "active"
                                : row == column ? "selected" : "default"));
            }
        }
        return step(message, line, cells);
    }

    private static SimulationStep stringStep(String text, int active, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < text.length(); index++) {
            cells.add(new Cell(Character.toString(text.charAt(index)), index == active ? "active" : "default"));
        }
        if (cells.isEmpty()) {
            cells.add(new Cell("empty", "muted"));
        }
        return step(message, line, cells);
    }

    private static SimulationStep stringWindowStep(
            String text, int start, int length, String message, int line) {
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < text.length(); index++) {
            String style = index == start || index == start + length - 1
                    ? "active"
                    : index > start && index < start + length - 1 ? "selected" : "muted";
            cells.add(new Cell(Character.toString(text.charAt(index)), style));
        }
        return step(message, line, cells);
    }

    private static SimulationStep step(String message, int codeLine, List<Cell> cells) {
        return step(message, codeLine, cells, List.of());
    }

    private static SimulationStep step(
            String message, int codeLine, List<Cell> cells, List<GraphLink> links) {
        return new SimulationStep(message, codeLine, List.copyOf(cells), List.copyOf(links), null);
    }

    private static int[] values(JsonNode config, int... fallback) {
        JsonNode values = config.path("values");
        if (!values.isArray() || values.isEmpty()) {
            return Arrays.copyOf(fallback, fallback.length);
        }
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index).asInt();
        }
        return result;
    }

    private static int boundedDistance(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min((long) Integer.MAX_VALUE - 1, value));
    }

    private static int boundedFloydDistance(long value) {
        return (int) Math.max(-FLOYD_INFINITY, Math.min((long) FLOYD_INFINITY - 1, value));
    }

    private static GraphLink link(String from, String to, boolean directed) {
        return new GraphLink(from, to, directed ? "→" : "", false);
    }

    private static List<GraphLink> unweightedLinks(
            JsonNode config, List<GraphLink> fallback, boolean directed) {
        JsonNode configured = config.path("edges");
        if (!configured.isArray() || configured.isEmpty()) {
            return fallback;
        }
        List<GraphLink> links = new ArrayList<>();
        for (JsonNode edge : configured) {
            if (edge.isArray() && edge.size() >= 2) {
                String from = edge.get(0).asText().trim();
                String to = edge.get(1).asText().trim();
                if (!from.isEmpty() && !to.isEmpty() && !from.equals(to)) {
                    links.add(link(from, to, directed));
                }
            }
            if (links.size() == 20) {
                break;
            }
        }
        return links.isEmpty() ? fallback : List.copyOf(links);
    }

    private static List<WeightedEdge> weightedEdges(
            JsonNode config, String field, List<WeightedEdge> fallback) {
        JsonNode configured = config.path(field);
        if (!configured.isArray() || configured.isEmpty()) {
            return fallback;
        }
        List<WeightedEdge> edges = new ArrayList<>();
        for (JsonNode edge : configured) {
            if (edge.isArray() && edge.size() >= 3) {
                String from = edge.get(0).asText().trim();
                String to = edge.get(1).asText().trim();
                if (!from.isEmpty() && !to.isEmpty() && !from.equals(to)) {
                    edges.add(new WeightedEdge(from, to, edge.get(2).asInt()));
                }
            }
            if (edges.size() == 20) {
                break;
            }
        }
        return edges.isEmpty() ? fallback : List.copyOf(edges);
    }

    private static List<GraphLink> weightedLinks(List<WeightedEdge> edges, boolean directed) {
        return edges.stream()
                .map(edge -> new GraphLink(edge.from(), edge.to(),
                        edge.weight() + (directed ? " →" : ""), false))
                .toList();
    }

    private static List<GraphLink> activate(List<GraphLink> links, String from, String to) {
        return links.stream()
                .map(link -> new GraphLink(link.from(), link.to(), link.label(),
                        link.active() || link.from().equals(from) && link.to().equals(to)
                                || !link.label().contains("→")
                                        && link.from().equals(to) && link.to().equals(from)))
                .toList();
    }

    private static List<String> configuredVertices(
            JsonNode config, List<String> fallback, List<GraphLink> links) {
        LinkedHashSet<String> vertices = new LinkedHashSet<>();
        JsonNode configured = config.path("vertices");
        if (configured.isArray()) {
            for (JsonNode vertex : configured) {
                String id = vertex.asText().trim();
                if (!id.isEmpty()) {
                    vertices.add(id);
                }
                if (vertices.size() == 8) {
                    break;
                }
            }
        }
        for (GraphLink link : links) {
            if (vertices.size() < 8) {
                vertices.add(link.from());
            }
            if (vertices.size() < 8) {
                vertices.add(link.to());
            }
        }
        if (vertices.isEmpty()) {
            vertices.addAll(fallback);
        }
        return List.copyOf(vertices);
    }

    private static Map<String, List<String>> adjacency(
            List<String> vertices, List<GraphLink> links, boolean undirected) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        vertices.forEach(vertex -> graph.put(vertex, new ArrayList<>()));
        for (GraphLink link : links) {
            if (!graph.containsKey(link.from()) || !graph.containsKey(link.to())) {
                continue;
            }
            if (!graph.get(link.from()).contains(link.to())) {
                graph.get(link.from()).add(link.to());
            }
            if (undirected && !graph.get(link.to()).contains(link.from())) {
                graph.get(link.to()).add(link.from());
            }
        }
        return graph;
    }

    private static int[][] configuredMatrix(JsonNode config, int[][] fallback) {
        JsonNode configured = config.path("matrix");
        if (!configured.isArray() || configured.isEmpty() || configured.size() > 8) {
            return copyMatrix(fallback);
        }
        int width = configured.get(0).isArray() ? configured.get(0).size() : 0;
        if (width == 0 || width > 8) {
            return copyMatrix(fallback);
        }
        int[][] matrix = new int[configured.size()][width];
        for (int row = 0; row < configured.size(); row++) {
            JsonNode values = configured.get(row);
            if (!values.isArray() || values.size() != width) {
                return copyMatrix(fallback);
            }
            for (int column = 0; column < width; column++) {
                matrix[row][column] = values.get(column).asInt();
            }
        }
        return matrix;
    }

    private static int[][] copyMatrix(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int row = 0; row < matrix.length; row++) {
            copy[row] = Arrays.copyOf(matrix[row], matrix[row].length);
        }
        return copy;
    }

    private static List<String> configuredLabels(JsonNode config, int count) {
        List<String> labels = new ArrayList<>();
        JsonNode configured = config.path("vertices");
        if (configured.isArray() && configured.size() == count) {
            configured.forEach(vertex -> labels.add(vertex.asText()));
        }
        if (labels.size() != count || new LinkedHashSet<>(labels).size() != count) {
            labels.clear();
            for (int index = 0; index < count; index++) {
                labels.add(Character.toString((char) ('A' + index)));
            }
        }
        return labels;
    }

    private static int[][] configuredPairs(JsonNode config, String field, int[][] fallback) {
        JsonNode configured = config.path(field);
        if (!configured.isArray() || configured.isEmpty()) {
            return copyMatrix(fallback);
        }
        int[][] pairs = new int[Math.min(configured.size(), 20)][2];
        for (int index = 0; index < pairs.length; index++) {
            JsonNode pair = configured.get(index);
            if (!pair.isArray() || pair.size() < 2) {
                return copyMatrix(fallback);
            }
            pairs[index][0] = pair.get(0).asInt();
            pairs[index][1] = pair.get(1).asInt();
        }
        return pairs;
    }

    private static int[] configuredArray(JsonNode configured, int[] fallback) {
        if (!configured.isArray() || configured.isEmpty()) {
            return Arrays.copyOf(fallback, fallback.length);
        }
        int[] result = new int[Math.min(configured.size(), 24)];
        for (int index = 0; index < result.length; index++) {
            result[index] = configured.get(index).asInt();
        }
        return result;
    }

    private static int[] point(JsonNode configured, int fallbackX, int fallbackY) {
        if (!configured.isArray() || configured.size() < 2) {
            return new int[]{fallbackX, fallbackY};
        }
        return new int[]{configured.get(0).asInt(), configured.get(1).asInt()};
    }

    private static String pointText(String label, int[] point) {
        return label + "=(" + point[0] + "," + point[1] + ")";
    }

    private static JsonNode readConfig(ObjectMapper mapper, String configJson) {
        try {
            return mapper.readTree(configJson == null || configJson.isBlank() ? "{}" : configJson);
        } catch (Exception exception) {
            return mapper.createObjectNode();
        }
    }

    private static void swap(int[] values, int first, int second) {
        int temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    private static final class SearchTreeNode {
        private final int value;
        private SearchTreeNode left;
        private SearchTreeNode right;

        private SearchTreeNode(int value) {
            this.value = value;
        }
    }

    private static final class AvlNode {
        private final int value;
        private AvlNode left;
        private AvlNode right;
        private int height = 1;

        private AvlNode(int value) {
            this.value = value;
        }
    }

    private static final class SuffixAutomatonState {
        private final int length;
        private int link;
        private final Map<Character, Integer> transitions = new LinkedHashMap<>();

        private SuffixAutomatonState(int length, int link) {
            this.length = length;
            this.link = link;
        }
    }

    private record RangeRequest(int id, int left, int right) { }

    private record SamExtension(int last, int cloneState) { }

    private record WeightedEdge(String from, String to, int weight) { }

    private record GraphInput(Map<String, List<String>> adjacency, List<GraphLink> links) { }

    public record SimulationRun(List<String> pseudocode, List<SimulationStep> steps) {
    }

    private record MergeNode(int low, int high, MergeNode left, MergeNode right) { }

    public record MergeRange(int low, int high, List<Integer> values, String style) { }

    public record MergeSortState(List<MergeRange> ranges) { }

    public record SimulationStep(String message, int codeLine, List<Cell> cells, List<GraphLink> links,
            MergeSortState mergeSortState) {
        public SimulationStep(String message, int codeLine, List<Cell> cells) {
            this(message, codeLine, cells, List.of(), null);
        }
    }

    public record GraphLink(String from, String to, String label, boolean active) { }

    public record Cell(String text, String style) {
    }
}
