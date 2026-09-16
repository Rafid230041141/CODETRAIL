package dev.codetrail.desktop.simulation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

/** Input-specific goals and small alternate examples, separate from public engine metadata. */
public final class SimulationTeachingGuide {
    private static final ObjectMapper JSON = new ObjectMapper();
    private SimulationTeachingGuide() { }

    /** Public array indices remain zero-based, matching the existing input forms. */
    public static String goal(String type, JsonNode input) {
        Objects.requireNonNull(input, "input");
        return switch (type) {
            case "RECURSION" -> "Compute " + number(input, "n") + "! by calls and returned products.";
            case "MERGE_SORT" -> sortGoal(input, "merge ordered runs");
            case "QUICK_SORT" -> sortGoal(input, "partition around each pivot");
            case "HEAP_SORT" -> sortGoal(input, "repeatedly extract the largest value");
            case "COUNTING_SORT" -> sortGoal(input, "count values and place them in order");
            case "RADIX_SORT" -> sortGoal(input, "order one digit at a time");
            case "BUCKET_SORT" -> sortGoal(input, "group ranges, sort buckets, and join them");
            case "LINEAR_SEARCH" -> "Find " + number(input, "target") + " among " + count(input, "array") + " values, scanning left to right.";
            case "BINARY_SEARCH" -> "Find the first " + number(input, "target") + " among " + count(input, "array") + " sorted values.";
            case "BINARY_SEARCH_ANSWER" -> "Find the earliest time to make " + number(input, "target") + " items with " + count(input, "machines") + " machines.";
            case "ARRAY" -> input.has("operations") ? batchGoal(input, "array") : indexedAction(input, "array");
            case "LINKED_LIST" -> input.has("operations") ? batchGoal(input, "linked-list")
                    : input.path("operation").asText().equals("reverse")
                    ? "Reverse the links joining " + count(input, "values") + " list values."
                    : indexedAction(input, "linked list");
            case "STACK" -> "Run " + count(input, "operations") + " stack operations; the last value in comes out first.";
            case "QUEUE" -> "Run " + count(input, "operations") + " FIFO operations in " + number(input, "capacity") + " circular slots.";
            case "BST" -> treeAction(input) + " while preserving search-tree order.";
            case "AVL_TREE" -> treeAction(input) + " while restoring tree balance.";
            case "HEAP" -> input.path("operation").asText().equals("insert")
                    ? "Insert " + number(input, "value") + " into the " + count(input, "values") + "-value max heap."
                    : "Remove the largest of " + count(input, "values") + " heap values and restore heap order.";
            case "TRIE" -> trieGoal(input);
            case "HASH_MAP" -> mapGoal(input);
            case "DSU" -> "Track connectivity among " + number(input, "vertices") + " vertices through " + count(input, "operations") + " operations.";
            case "GRAPH_REPRESENTATION" -> "Store " + count(input, "edges") + " edges in an "
                    + (input.path("representation").asText().equals("adjacency-matrix") ? "adjacency matrix." : "adjacency list.");
            case "BFS" -> "Reach vertices in distance order from vertex " + number(input, "source") + ".";
            case "DFS" -> "Explore from vertex " + number(input, "source") + ", finishing each branch before returning.";
            case "TOPOLOGICAL_SORT" -> "Order " + number(input, "n") + " tasks so every dependency comes first.";
            case "TARJAN_SCC" -> "Group " + number(input, "n") + " vertices by mutual reachability.";
            case "BRIDGES_ARTICULATION" -> "Find edges and vertices whose removal separates this " + number(input, "n") + "-vertex graph.";
            case "DIJKSTRA" -> "Find shortest paths from vertex " + number(input, "source") + " using the cheapest next candidate.";
            case "BELLMAN_FORD" -> "Find shortest paths from vertex " + number(input, "source") + ", or detect a reachable negative cycle.";
            case "FLOYD_WARSHALL" -> "Find shortest routes between all " + number(input, "n") + " vertices through allowed intermediates.";
            case "KRUSKAL_MST" -> "Connect each component of this " + number(input, "n") + "-vertex graph at minimum total weight.";
            case "PRIM_MST" -> "Grow a minimum-weight forest from vertex " + number(input, "source") + ".";
            case "MAX_FLOW" -> "Send the most flow from vertex " + number(input, "source") + " to vertex " + number(input, "sink") + ".";
            case "PREFIX_SUM" -> "Sum values at indices " + interval(input) + " by subtracting two prefixes.";
            case "FENWICK_TREE" -> input.path("operation").asText().equals("update")
                    ? "Add " + number(input, "delta") + " to value at index " + number(input, "index") + " through Fenwick blocks."
                    : "Sum values at indices 0–" + number(input, "index") + " using Fenwick blocks.";
            case "SEGMENT_TREE" -> segmentGoal(input);
            case "SEGMENT_TREE_LAZY" -> "Apply " + count(input, "operations") + " range operations to " + count(input, "values") + " values using deferred updates.";
            case "SPARSE_TABLE" -> "Find the minimum at indices " + interval(input) + " using two power-of-two blocks.";
            case "SQRT_DECOMPOSITION" -> "Run " + count(input, "operations") + " operations on " + count(input, "values") + " values using whole blocks and edge values.";
            case "MO_RANGE_QUERY" -> "Count distinct values in " + count(input, "queries") + " ranges by reusing one moving window.";
            case "ONLINE_RANGE_QUERY" -> "Process " + count(input, "operations") + " queries and updates in their given order.";
            case "KMP" -> "Find “" + input.path("pattern").asText() + "” in " + input.path("text").asText().length() + " text characters without restarting matches.";
            case "Z_FUNCTION" -> "Find prefix-match lengths at each position of “" + input.path("text").asText() + "”.";
            case "STRING_HASHING" -> "Find and verify “" + input.path("pattern").asText() + "” using a rolling text-window hash.";
            case "SUFFIX_ARRAY" -> "Put the suffixes of “" + input.path("text").asText() + "” in dictionary order.";
            case "SUFFIX_AUTOMATON" -> "Group the substrings of “" + input.path("text").asText() + "” into shared states.";
            case "EXTENDED_GCD" -> "Find gcd(" + number(input, "a") + ", " + number(input, "b") + ") and coefficients that produce it.";
            case "SIEVE_OF_ERATOSTHENES" -> "Find every prime up to " + number(input, "limit") + " by eliminating multiples.";
            case "MODULAR_EXPONENTIATION" -> "Compute " + number(input, "base") + "^" + number(input, "exponent") + " mod " + number(input, "modulus") + " using exponent bits.";
            case "PASCAL_TRIANGLE" -> "Build Pascal’s triangle through row " + number(input, "n") + " by adding parent values.";
            case "GAUSSIAN_ELIMINATION" -> "Solve " + count(input, "matrix") + " equations in " + Math.max(0, input.path("matrix").path(0).size() - 1) + " variables using equivalent row operations.";
            case "CONVEX_HULL" -> "Find the outer boundary of " + count(input, "points") + " points using turn direction.";
            case "DIVIDE_AND_CONQUER" -> "Find the minimum and maximum of " + count(input, "array") + " values by combining smaller ranges.";
            case "DYNAMIC_PROGRAMMING" -> "Choose the most valuable combination of " + count(input, "items") + " items within capacity " + number(input, "capacity") + ".";
            case "GREEDY_ACTIVITY_SELECTION" -> "Choose the most nonoverlapping activities from " + count(input, "activities") + " candidates.";
            case "BACKTRACKING_N_QUEENS" -> "Place " + number(input, "n") + " queens on a " + number(input, "n") + "×" + number(input, "n") + " board without attacks.";
            default -> throw unsupported(type);
        };
    }

    /** Every call returns a fresh independent node, suitable for the existing ordinary input form. */
    public static JsonNode alternateInput(String type) {
        String json = switch (type) {
            case "ARRAY" -> "{\"values\":[4,7,2,9],\"operation\":\"delete\",\"index\":1}";
            case "AVL_TREE" -> "{\"values\":[30,10],\"operation\":\"insert\",\"value\":20}";
            case "BACKTRACKING_N_QUEENS" -> "{\"n\":3}";
            case "BELLMAN_FORD" -> "{\"n\":4,\"edges\":[{\"to\":1,\"weight\":2,\"from\":0},{\"to\":2,\"weight\":-4,\"from\":1},{\"to\":1,\"weight\":1,\"from\":2},{\"to\":3,\"weight\":2,\"from\":2}],\"directed\":true,\"source\":0}";
            case "BFS" -> "{\"n\":5,\"edges\":[[0,1],[0,2],[1,2],[2,3]],\"source\":0,\"directed\":false}";
            case "BINARY_SEARCH" -> "{\"array\":[1,2,2,2,5,8],\"target\":2}";
            case "BINARY_SEARCH_ANSWER" -> "{\"machines\":[2,5],\"target\":7}";
            case "BRIDGES_ARTICULATION" -> "{\"n\":5,\"edges\":[[0,1],[1,2],[2,0],[2,3],[3,4]],\"source\":0,\"directed\":false}";
            case "BST" -> "{\"values\":[8,3,10,1,6,9,14],\"operation\":\"delete\",\"value\":8}";
            case "BUCKET_SORT" -> "{\"array\":[8,1,7,2,8,3]}";
            case "CONVEX_HULL" -> "{\"points\":[[0,0],[1,0],[2,0],[2,2],[0,2],[1,1]]}";
            case "COUNTING_SORT" -> "{\"array\":[-2,1,-2,0,1]}";
            case "DFS" -> "{\"n\":5,\"edges\":[[0,1],[1,2],[2,0],[1,3]],\"source\":0,\"directed\":false}";
            case "DIJKSTRA" -> "{\"n\":5,\"edges\":[{\"to\":1,\"weight\":7,\"from\":0},{\"to\":2,\"weight\":2,\"from\":0},{\"to\":1,\"weight\":1,\"from\":2},{\"to\":3,\"weight\":2,\"from\":1}],\"directed\":true,\"source\":0}";
            case "DIVIDE_AND_CONQUER" -> "{\"algorithm\":\"MIN_MAX\",\"array\":[4,-3,4,8,0]}";
            case "DSU" -> "{\"vertices\":5,\"operations\":[{\"kind\":\"union\",\"a\":0,\"b\":1},{\"kind\":\"union\",\"a\":2,\"b\":3},{\"kind\":\"union\",\"a\":0,\"b\":2},{\"kind\":\"find\",\"x\":3},{\"kind\":\"union\",\"a\":1,\"b\":3}]}";
            case "DYNAMIC_PROGRAMMING" -> "{\"algorithm\":\"KNAPSACK_01\",\"capacity\":5,\"items\":[{\"weight\":3,\"value\":7},{\"weight\":2,\"value\":4},{\"weight\":4,\"value\":9}]}";
            case "EXTENDED_GCD" -> "{\"a\":35,\"b\":12}";
            case "FENWICK_TREE" -> "{\"values\":[2,1,3,5,4,2,6,1],\"operation\":\"query\",\"index\":6}";
            case "FLOYD_WARSHALL" -> "{\"n\":4,\"edges\":[{\"to\":1,\"weight\":4,\"from\":0},{\"to\":2,\"weight\":-2,\"from\":1},{\"to\":2,\"weight\":7,\"from\":0},{\"to\":3,\"weight\":3,\"from\":2}],\"directed\":true}";
            case "GAUSSIAN_ELIMINATION" -> "{\"matrix\":[[1,1,2],[2,2,5]]}";
            case "GRAPH_REPRESENTATION" -> "{\"n\":4,\"edges\":[[0,1],[1,2],[2,2]],\"directed\":true,\"representation\":\"adjacency-matrix\"}";
            case "GREEDY_ACTIVITY_SELECTION" -> "{\"activities\":[{\"start\":0,\"finish\":4},{\"start\":1,\"finish\":2},{\"start\":2,\"finish\":3},{\"start\":3,\"finish\":5}]}";
            case "HASH_MAP" -> "{\"capacity\":5,\"entries\":[[1,10],[6,20],[11,30]],\"operation\":\"remove\",\"key\":6,\"collision\":\"linear-probing\"}";
            case "HEAP" -> "{\"values\":[9,7,8,2,4],\"operation\":\"extract\"}";
            case "HEAP_SORT" -> "{\"array\":[3,1,5,2,4]}";
            case "KMP" -> "{\"text\":\"ababababac\",\"pattern\":\"ababac\"}";
            case "KRUSKAL_MST" -> "{\"n\":5,\"edges\":[{\"to\":1,\"weight\":1,\"from\":0},{\"to\":2,\"weight\":2,\"from\":1},{\"to\":2,\"weight\":3,\"from\":0},{\"to\":4,\"weight\":1,\"from\":3}],\"directed\":false}";
            case "LINEAR_SEARCH" -> "{\"array\":[4,1,7,2],\"target\":9}";
            case "LINKED_LIST" -> "{\"values\":[4,7,2],\"operation\":\"reverse\"}";
            case "MAX_FLOW" -> "{\"n\":6,\"edges\":[{\"from\":0,\"to\":1,\"capacity\":1},{\"from\":0,\"to\":2,\"capacity\":1},{\"from\":1,\"to\":3,\"capacity\":1},{\"from\":1,\"to\":4,\"capacity\":1},{\"from\":2,\"to\":3,\"capacity\":1},{\"from\":3,\"to\":5,\"capacity\":1},{\"from\":4,\"to\":5,\"capacity\":1}],\"source\":0,\"sink\":5,\"directed\":true}";
            case "MERGE_SORT" -> "{\"array\":[4,2,4,1,2]}";
            case "MODULAR_EXPONENTIATION" -> "{\"base\":5,\"exponent\":10,\"modulus\":13}";
            case "MO_RANGE_QUERY" -> "{\"values\":[1,2,1,3,2,3],\"queries\":[{\"left\":2,\"right\":5},{\"left\":0,\"right\":2},{\"left\":1,\"right\":4}]}";
            case "ONLINE_RANGE_QUERY" -> "{\"values\":[2,1,3,5,4],\"operations\":[{\"kind\":\"query\",\"left\":1,\"right\":3},{\"kind\":\"update\",\"index\":2,\"value\":8},{\"kind\":\"query\",\"left\":1,\"right\":3}]}";
            case "PASCAL_TRIANGLE" -> "{\"n\":4}";
            case "PREFIX_SUM" -> "{\"values\":[3,-2,5,1,-1],\"left\":1,\"right\":3}";
            case "PRIM_MST" -> "{\"n\":5,\"edges\":[{\"to\":1,\"weight\":1,\"from\":0},{\"to\":2,\"weight\":2,\"from\":1},{\"to\":2,\"weight\":3,\"from\":0},{\"to\":4,\"weight\":1,\"from\":3}],\"directed\":false,\"source\":0}";
            case "QUEUE" -> "{\"capacity\":3,\"operations\":[{\"kind\":\"enqueue\",\"value\":4},{\"kind\":\"enqueue\",\"value\":7},{\"kind\":\"enqueue\",\"value\":2},{\"kind\":\"dequeue\"},{\"kind\":\"enqueue\",\"value\":9},{\"kind\":\"enqueue\",\"value\":5}]}";
            case "QUICK_SORT" -> "{\"array\":[4,2,4,1,4]}";
            case "RADIX_SORT" -> "{\"array\":[-12,3,-12,0,21]}";
            case "RECURSION" -> "{\"n\":0}";
            case "SEGMENT_TREE" -> "{\"values\":[2,1,3,5,4],\"operation\":\"point-set\",\"index\":2,\"value\":8}";
            case "SEGMENT_TREE_LAZY" -> "{\"values\":[2,1,3,5,4,2,6,1],\"operations\":[{\"kind\":\"range-add\",\"left\":0,\"right\":7,\"delta\":2},{\"kind\":\"range-sum\",\"left\":2,\"right\":5}]}";
            case "SIEVE_OF_ERATOSTHENES" -> "{\"limit\":20}";
            case "SPARSE_TABLE" -> "{\"values\":[7,2,5,1,6,3,4],\"operation\":\"range-min\",\"left\":1,\"right\":5}";
            case "SQRT_DECOMPOSITION" -> "{\"values\":[2,1,3,5,4,2,6,1],\"blockSize\":3,\"operations\":[{\"kind\":\"query\",\"left\":1,\"right\":6},{\"kind\":\"update\",\"index\":4,\"value\":9},{\"kind\":\"query\",\"left\":1,\"right\":6}]}";
            case "STACK" -> "{\"operations\":[{\"kind\":\"push\",\"value\":4},{\"kind\":\"push\",\"value\":7},{\"kind\":\"pop\"},{\"kind\":\"pop\"},{\"kind\":\"pop\"}]}";
            case "STRING_HASHING" -> "{\"text\":\"ababa\",\"pattern\":\"aba\"}";
            case "SUFFIX_ARRAY" -> "{\"text\":\"miss\"}";
            case "SUFFIX_AUTOMATON" -> "{\"text\":\"abb\"}";
            case "TARJAN_SCC" -> "{\"n\":4,\"edges\":[[0,1],[1,2],[2,0],[2,3]],\"directed\":true}";
            case "TOPOLOGICAL_SORT" -> "{\"n\":4,\"edges\":[[0,1],[1,2],[2,1],[2,3]],\"directed\":true}";
            case "TRIE" -> "{\"words\":[\"cat\",\"car\",\"dog\"],\"operation\":\"search\",\"word\":\"ca\"}";
            case "Z_FUNCTION" -> "{\"text\":\"abacabab\"}";
            default -> throw unsupported(type);
        };
        try { return JSON.readTree(json); }
        catch (JsonProcessingException invalidExample) { throw new IllegalStateException("Invalid teaching example for " + type, invalidExample); }
    }

    public static String alternateLabel(String type) {
        return switch (type) {
            case "ARRAY" -> "Deletion closes a gap";
            case "AVL_TREE" -> "Double rotation";
            case "BACKTRACKING_N_QUEENS" -> "Backtracking proves no solution";
            case "BELLMAN_FORD" -> "Reachable negative cycle";
            case "BFS" -> "Cycle and unreachable vertex";
            case "BINARY_SEARCH" -> "Find the first equal value";
            case "BINARY_SEARCH_ANSWER" -> "Different machine speeds";
            case "BRIDGES_ARTICULATION" -> "A cycle attached to a fragile chain";
            case "BST" -> "Delete a node with two children";
            case "BUCKET_SORT" -> "Uneven buckets and duplicates";
            case "CONVEX_HULL" -> "Collinear and interior points";
            case "COUNTING_SORT" -> "Repeated and negative values";
            case "DFS" -> "Back edge and unreachable vertex";
            case "DIJKSTRA" -> "Improve an earlier candidate; one vertex is unreachable";
            case "DIVIDE_AND_CONQUER" -> "Repeated values and a negative minimum";
            case "DSU" -> "Compression and an already joined pair";
            case "DYNAMIC_PROGRAMMING" -> "Two lighter items beat one heavier item";
            case "EXTENDED_GCD" -> "Coprime numbers and Bezout coefficients";
            case "FENWICK_TREE" -> "Prefix query across three blocks";
            case "FLOYD_WARSHALL" -> "A negative edge improves an indirect route";
            case "GAUSSIAN_ELIMINATION" -> "An inconsistent system";
            case "GRAPH_REPRESENTATION" -> "Directed matrix and self-loop";
            case "GREEDY_ACTIVITY_SELECTION" -> "An early finish leaves more choices";
            case "HASH_MAP" -> "Deletion leaves a tombstone";
            case "HEAP" -> "Extract and restore the heap";
            case "HEAP_SORT" -> "Build a heap before extraction";
            case "KMP" -> "Fallback retains matched characters";
            case "KRUSKAL_MST" -> "A cycle and disconnected components";
            case "LINEAR_SEARCH" -> "Target is absent";
            case "LINKED_LIST" -> "Reverse the links";
            case "MAX_FLOW" -> "Reverse residual edge reroutes earlier flow";
            case "MERGE_SORT" -> "Equal values keep their order";
            case "MODULAR_EXPONENTIATION" -> "Zero bits skip multiplication";
            case "MO_RANGE_QUERY" -> "Reorder queries to reuse the window";
            case "ONLINE_RANGE_QUERY" -> "An update changes a later answer";
            case "PASCAL_TRIANGLE" -> "Two parents produce each interior cell";
            case "PREFIX_SUM" -> "Subtract prefixes with negative values";
            case "PRIM_MST" -> "Restart in a disconnected component";
            case "QUEUE" -> "Wraparound and a full queue";
            case "QUICK_SORT" -> "Repeated pivot values";
            case "RADIX_SORT" -> "Signed values across digit passes";
            case "RECURSION" -> "The factorial base case";
            case "SEGMENT_TREE" -> "Update one leaf and its ancestors";
            case "SEGMENT_TREE_LAZY" -> "Deferred update before a partial query";
            case "SIEVE_OF_ERATOSTHENES" -> "Overlapping multiples";
            case "SPARSE_TABLE" -> "Overlapping power-of-two blocks";
            case "SQRT_DECOMPOSITION" -> "An update changes the same query";
            case "STACK" -> "LIFO followed by an empty pop";
            case "STRING_HASHING" -> "Overlapping verified matches";
            case "SUFFIX_ARRAY" -> "Repeated first characters";
            case "SUFFIX_AUTOMATON" -> "A clone splits a substring class";
            case "TARJAN_SCC" -> "One cycle and one separate component";
            case "TOPOLOGICAL_SORT" -> "A cycle prevents a full order";
            case "TRIE" -> "A prefix is not a complete word";
            case "Z_FUNCTION" -> "Extend a reused prefix match";
            default -> throw unsupported(type);
        };
    }

    private static String number(JsonNode input, String key) { return input.path(key).asText(); }
    private static int count(JsonNode input, String key) { return input.path(key).size(); }
    private static String interval(JsonNode input) { return number(input, "left") + "–" + number(input, "right"); }
    private static String sortGoal(JsonNode input, String mechanism) {
        return "Sort " + count(input, "array") + " values into ascending order: " + mechanism + ".";
    }
    private static String indexedAction(JsonNode input, String structure) {
        return switch (input.path("operation").asText()) {
            case "insert" -> "Insert " + number(input, "value") + " at " + structure + " index " + number(input, "index") + ".";
            case "delete" -> "Delete the value at " + structure + " index " + number(input, "index") + ".";
            default -> throw new IllegalArgumentException("Unsupported indexed operation");
        };
    }
    private static String batchGoal(JsonNode input, String structure) {
        return "Apply " + count(input, "operations") + " " + structure + " operations in the given order to "
                + count(input, "values") + " starting values.";
    }
    private static String treeAction(JsonNode input) {
        String operand = number(input, input.has("value") ? "value" : "key");
        return switch (input.path("operation").asText()) {
            case "search" -> "Search for " + operand;
            case "inorder", "in-order", "in_order" -> "Visit " + count(input, "values") + " input keys in order";
            case "delete" -> "Delete " + operand;
            case "insert" -> "Insert " + operand;
            default -> throw new IllegalArgumentException("Unsupported tree operation");
        };
    }
    private static String segmentGoal(JsonNode input) {
        return switch (input.path("operation").asText()) {
            case "point-set" -> "Set value at index " + number(input, "index") + " to " + number(input, "value") + " and repair its aggregates.";
            case "range-min" -> "Find the minimum at indices " + interval(input) + " using covering tree nodes.";
            case "range-sum" -> "Sum values at indices " + interval(input) + " using covering tree nodes.";
            default -> throw new IllegalArgumentException("Unsupported segment operation");
        };
    }
    private static String trieGoal(JsonNode input) {
        return switch (input.path("operation").asText()) {
            case "insert" -> "Insert “" + input.path("word").asText() + "” while sharing its character prefix.";
            case "search", "has" -> "Check whether “" + input.path("word").asText() + "” is a complete stored word.";
            case "prefix" -> "Check whether a stored word begins with “" + input.path("word").asText() + "”.";
            case "match" -> "Find the stored words in “" + input.path("text").asText() + "”.";
            default -> throw new IllegalArgumentException("Unsupported trie operation");
        };
    }
    private static String mapGoal(JsonNode input) {
        return switch (input.path("operation").asText()) {
            case "put" -> "Store key " + number(input, "key") + " → " + number(input, "value") + " through collisions.";
            case "get" -> "Find key " + number(input, "key") + " along its probe path.";
            case "remove" -> "Remove key " + number(input, "key") + " while preserving the probe path.";
            default -> throw new IllegalArgumentException("Unsupported hash map operation");
        };
    }
    private static IllegalArgumentException unsupported(String type) {
        return new IllegalArgumentException("No teaching guide registered for type: " + type);
    }
}
