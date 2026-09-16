package dev.codetrail.desktop.simulation.structures.trees;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TreeState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Binary search tree with real links, successor deletion, and bounded tracing. */
public final class BstEngine implements SimulationEngine {
    public static final String TYPE = "BST";
    public static final int MIN_VALUES = 0;
    public static final int MAX_VALUES = 15;
    public static final int MAX_ABS_VALUE = 999;
    public static final int MAX_TRACE_STEPS = 2048;

    private static final int LINE_INSERT = 1;
    private static final int LINE_COMPARE = 2;
    private static final int LINE_LEFT = 3;
    private static final int LINE_RIGHT = 4;
    private static final int LINE_CREATE = 5;
    private static final int LINE_DUPLICATE = 6;
    private static final int LINE_INSERT_REWIRE = 7;
    private static final int LINE_SEARCH = 8;
    private static final int LINE_DELETE = 9;
    private static final int LINE_CHILD = 10;
    private static final int LINE_SUCCESSOR = 11;
    private static final int LINE_REWIRE = 12;
    private static final int LINE_INORDER = 13;
    private static final int LINE_RETURN = 14;

    private static final List<String> PSEUDOCODE = List.of(
            "insert(node, key):",
            "    compare key with node.key",
            "    if key < node.key: recurse into left",
            "    if key > node.key: recurse into right",
            "    if node is null: create and return a node",
            "    if key == node.key: keep one copy",
            "    after insertion returns: reconnect the matching child link",
            "search(node, key): follow the comparison path",
            "delete(node, key):",
            "    if one child is missing: return the other child",
            "    successor = minimum node in the right subtree",
            "    reconnect the successor and both child subtrees",
            "inorder(node): visit left, node, right",
            "return the tree, traversal, and operation result");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        Model model = new Model();
        Trace trace = new Trace(model);
        trace.add(
                0,
                "Initialize an empty BST",
                StepEventType.INITIALIZE,
                facts(model, trace, "initialize", "none", "initialize", SnapshotStatus.ACTIVE));

        for (int value : request.values()) {
            trace.beginOperation();
            trace.operation = "insert(" + value + ")";
            trace.result = "pending";
            trace.path.clear();
            model.root = insert(model.root, value, model, trace);
            trace.finishOperation();
        }

        trace.beginOperation();
        trace.operation = request.operationDescription();
        trace.result = "pending";
        trace.rewrite = "none";
        trace.path.clear();
        switch (request.operation()) {
            case "insert" -> model.root = insert(model.root, request.operand(), model, trace);
            case "search" -> search(model.root, request.operand(), trace);
            case "delete" -> model.root = delete(model.root, request.operand(), model, trace);
            case "inorder" -> {
                List<Integer> values = new ArrayList<>();
                inorder(model.root, values, trace);
                trace.result = OptionalSmallHelper.formatInts(values);
            }
            default -> throw new IllegalStateException("unsupported validated BST operation: " + request.operation());
        }
        trace.finishOperation();

        trace.markAllDone();
        List<Integer> finalOrder = new ArrayList<>();
        inorderValues(model.root, finalOrder);
        trace.add(
                LINE_RETURN,
                "Return the BST with in-order keys " + finalOrder,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, request.operation(), trace.result, "return", SnapshotStatus.DONE));
        trace.add(
                0,
                "Complete: BST contains " + finalOrder.size() + " distinct key(s)",
                StepEventType.COMPLETE,
                facts(model, trace, "complete", trace.result, "complete", SnapshotStatus.DONE));
        return trace.steps();
    }

    private static TreeNode insert(TreeNode node, int key, Model model, Trace trace) {
        if (node == null) {
            TreeNode created = model.newNode(key);
            trace.activate(created.id);
            trace.result = "INSERTED";
            trace.rewrite = "new node " + created.id + "(" + key + ")";
            trace.add(
                    LINE_CREATE,
                    "Create node " + key + " at the empty child position",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; create", SnapshotStatus.ACTIVE));
            return created;
        }

        trace.activate(node.id);
        trace.path.add(node.key);
        trace.add(
                LINE_COMPARE,
                "Compare key " + key + " with node " + node.key,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, "pending", "insert; compare", SnapshotStatus.ACTIVE));
        if (key < node.key) {
            trace.add(
                    LINE_LEFT,
                    "Key " + key + " is smaller; follow the left link from " + node.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, "pending", "insert; left", SnapshotStatus.ACTIVE));
            TreeNode oldChild = node.left;
            node.left = insert(node.left, key, model, trace);
            if (oldChild != node.left) {
                trace.rewrite = "left(" + node.key + ") -> " + node.left.id;
                trace.add(
                        LINE_INSERT_REWIRE,
                        "Rewrite left link of " + node.key + " to " + node.left.key,
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, trace.result, "insert; rewire", SnapshotStatus.ACTIVE));
            }
        } else if (key > node.key) {
            trace.add(
                    LINE_RIGHT,
                    "Key " + key + " is larger; follow the right link from " + node.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, "pending", "insert; right", SnapshotStatus.ACTIVE));
            TreeNode oldChild = node.right;
            node.right = insert(node.right, key, model, trace);
            if (oldChild != node.right) {
                trace.rewrite = "right(" + node.key + ") -> " + node.right.id;
                trace.add(
                        LINE_INSERT_REWIRE,
                        "Rewrite right link of " + node.key + " to " + node.right.key,
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, trace.result, "insert; rewire", SnapshotStatus.ACTIVE));
            }
        } else {
            trace.result = "DUPLICATE_IGNORED";
            trace.add(
                    LINE_DUPLICATE,
                    "Key " + key + " already exists; keep one node",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; duplicate", SnapshotStatus.DONE));
        }
        return node;
    }

    private static boolean search(TreeNode node, int key, Trace trace) {
        if (node == null) {
            trace.result = "NOT_FOUND";
            trace.add(
                    LINE_SEARCH,
                    "Reach an empty child; key " + key + " is NOT_FOUND",
                    StepEventType.EXECUTE_LINE,
                    facts(trace.model, trace, trace.operation, trace.result, "search; miss", SnapshotStatus.REJECTED));
            return false;
        }
        trace.activate(node.id);
        trace.path.add(node.key);
        trace.add(
                LINE_SEARCH,
                "Search compares key " + key + " with node " + node.key,
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, "pending", "search; compare", SnapshotStatus.ACTIVE));
        if (key == node.key) {
            trace.result = "FOUND";
            trace.add(
                    LINE_SEARCH,
                    "Key " + key + " is FOUND at node " + node.id,
                    StepEventType.EXECUTE_LINE,
                    facts(trace.model, trace, trace.operation, trace.result, "search; found", SnapshotStatus.DONE));
            return true;
        }
        if (key < node.key) {
            trace.add(
                    LINE_LEFT,
                    "Search key is smaller; follow the left link",
                    StepEventType.EXECUTE_LINE,
                    facts(trace.model, trace, trace.operation, "pending", "search; left", SnapshotStatus.ACTIVE));
            return search(node.left, key, trace);
        }
        trace.add(
                LINE_RIGHT,
                "Search key is larger; follow the right link",
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, "pending", "search; right", SnapshotStatus.ACTIVE));
        return search(node.right, key, trace);
    }

    private static TreeNode delete(TreeNode node, int key, Model model, Trace trace) {
        if (node == null) {
            trace.result = "NOT_FOUND";
            trace.add(
                    LINE_DELETE,
                    "Reach an empty child; key " + key + " is NOT_FOUND",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "delete; miss", SnapshotStatus.REJECTED));
            return null;
        }
        trace.activate(node.id);
        trace.path.add(node.key);
        trace.add(
                LINE_DELETE,
                "Inspect node " + node.key + " while deleting key " + key,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, "pending", "delete; compare", SnapshotStatus.ACTIVE));
        if (key < node.key) {
            TreeNode oldChild = node.left;
            node.left = delete(node.left, key, model, trace);
            if (oldChild != node.left) {
                trace.rewrite = "left(" + node.key + ") -> " + idOrEmpty(node.left);
                trace.add(
                        LINE_REWIRE,
                        "Rewrite left link of " + node.key + " after deletion",
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, trace.result, "delete; rewire", SnapshotStatus.ACTIVE));
            }
            return node;
        }
        if (key > node.key) {
            TreeNode oldChild = node.right;
            node.right = delete(node.right, key, model, trace);
            if (oldChild != node.right) {
                trace.rewrite = "right(" + node.key + ") -> " + idOrEmpty(node.right);
                trace.add(
                        LINE_REWIRE,
                        "Rewrite right link of " + node.key + " after deletion",
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, trace.result, "delete; rewire", SnapshotStatus.ACTIVE));
            }
            return node;
        }

        trace.activate(node.id);
        trace.result = "DELETED";
        if (node.left == null || node.right == null) {
            TreeNode replacement = node.left != null ? node.left : node.right;
            trace.rewrite = node.id + " removed; replacement " + idOrEmpty(replacement);
            trace.add(
                    LINE_CHILD,
                    "Delete node " + node.key + " and return its only child",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "delete; one child", SnapshotStatus.DONE));
            trace.deactivate(node.id);
            return replacement;
        }

        TreeNode successor = minimum(node.right, trace);
        trace.activate(successor.id);
        trace.add(
                LINE_SUCCESSOR,
                "Choose in-order successor " + successor.key + " from the right subtree",
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, trace.result, "delete; successor", SnapshotStatus.ACTIVE));
        TreeNode oldRight = node.right;
        int oldKey = node.key;
        node.right = deleteMinimum(node.right, model, trace);
        if (oldRight != node.right) {
            trace.rewrite = "right(" + node.key + ") -> " + idOrEmpty(node.right);
            trace.add(
                    LINE_REWIRE,
                    "Detach successor " + successor.key + " from its old right-subtree position",
                    StepEventType.EXECUTE_LINE,
                                facts(model, trace, trace.operation, trace.result, "delete; detach successor", SnapshotStatus.ACTIVE));
        }
        node.key = successor.key;
        trace.rewrite = "node " + node.id + " key: " + oldKey + " -> successor " + successor.key;
        trace.add(
                LINE_REWIRE,
                "Copy successor " + successor.key + " into node " + node.id
                        + " and keep the remaining child links",
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, trace.result, "delete; two-child successor", SnapshotStatus.ACTIVE));
        return node;
    }

    private static TreeNode minimum(TreeNode node, Trace trace) {
        TreeNode current = node;
        while (current.left != null) {
            trace.activate(current.id);
            trace.path.add(current.key);
            trace.add(
                    LINE_SUCCESSOR,
                    "Move left from " + current.key + " while locating the successor",
                    StepEventType.EXECUTE_LINE,
                    facts(trace.model, trace, trace.operation, trace.result, "delete; successor walk", SnapshotStatus.ACTIVE));
            current = current.left;
        }
        trace.activate(current.id);
        return current;
    }

    private static TreeNode deleteMinimum(TreeNode node, Model model, Trace trace) {
        if (node.left == null) {
            TreeNode replacement = node.right;
            trace.rewrite = node.id + " removed from successor path; replacement " + idOrEmpty(replacement);
            trace.deactivate(node.id);
            trace.add(
                    LINE_REWIRE,
                    "Remove successor node " + node.key + " from its old position",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "delete; detach", SnapshotStatus.ACTIVE));
            return replacement;
        }
        TreeNode oldChild = node.left;
        node.left = deleteMinimum(node.left, model, trace);
        if (oldChild != node.left) {
            trace.rewrite = "left(" + node.key + ") -> " + idOrEmpty(node.left);
            trace.add(
                    LINE_REWIRE,
                    "Rewrite successor-path link below " + node.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "delete; detach", SnapshotStatus.ACTIVE));
        }
        return node;
    }

    private static void inorder(TreeNode node, List<Integer> values, Trace trace) {
        if (node == null) {
            return;
        }
        trace.activate(node.id);
        inorder(node.left, values, trace);
        values.add(node.key);
        trace.add(
                LINE_INORDER,
                "Visit node " + node.key + " in left-node-right order",
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, "pending", "inorder; visit", SnapshotStatus.ACTIVE));
        inorder(node.right, values, trace);
    }

    private static void inorderValues(TreeNode node, List<Integer> values) {
        if (node == null) {
            return;
        }
        inorderValues(node.left, values);
        values.add(node.key);
        inorderValues(node.right, values);
    }

    private static List<Fact> facts(
            Model model,
            Trace trace,
            String operation,
            String result,
            String phase,
            SnapshotStatus status) {
        List<Integer> order = new ArrayList<>();
        inorderValues(model.root, order);
        List<String> path = trace.path.stream().map(value -> Integer.toString(value)).toList();
        return List.of(
                new Fact("operation", operation, SnapshotStatus.DEFAULT),
                new Fact("phase", phase, status),
                new Fact("result", result, status),
                new Fact("outcome", result, status),
                new Fact("search-result", result, status),
                new Fact("inorder", OptionalSmallHelper.formatInts(order), status),
                new Fact("keys", OptionalSmallHelper.formatInts(order), status),
                new Fact("size", Integer.toString(order.size()), status),
                new Fact("root", model.root == null ? "none" : Integer.toString(model.root.key), status),
                new Fact("path", OptionalSmallHelper.formatStrings(path), status),
                new Fact("rewrite", trace.rewrite, status),
                new Fact("duplicate-policy", "ignore", SnapshotStatus.DEFAULT));
    }

    private static String idOrEmpty(TreeNode node) {
        return node == null ? "empty" : node.id;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode values = defaultInput.putArray("values");
        for (int value : new int[] {8, 3, 10, 1, 6, 14}) {
            values.add(value);
        }
        defaultInput.put("operation", "insert");
        defaultInput.put("value", 4);
        return new SimulationMetadata(
                TYPE,
                "BST",
                "O(h) search, insert, and delete; O(n) in-order traversal",
                "O(n)",
                RendererFamily.TREE,
                defaultInput,
                "Enter JSON as {\"values\":[8,3,10,1,6,14],\"operation\":\"insert\",\"value\":4}; values are bounded integers and operation is insert, search, delete, or inorder.",
                PSEUDOCODE);
    }

    private static final class Model {
        private TreeNode root;
        private int nextId;

        private TreeNode newNode(int key) {
            return new TreeNode("n" + nextId++, key);
        }
    }

    private static final class TreeNode {
        private final String id;
        private int key;
        private TreeNode left;
        private TreeNode right;

        private TreeNode(String id, int key) {
            this.id = id;
            this.key = key;
        }
    }

    private static final class Trace {
        private final Model model;
        private final Map<String, SnapshotStatus> statuses = new LinkedHashMap<>();
        private final Set<String> activeNodes = new LinkedHashSet<>();
        private final List<SimulationStep> steps = new ArrayList<>();
        private final List<Integer> path = new ArrayList<>();
        private String operation = "initialize";
        private String result = "none";
        private String rewrite = "none";
        private Trace(Model model) {
            this.model = Objects.requireNonNull(model, "model");
        }

        private void beginOperation() {
            for (String id : activeNodes) {
                statuses.put(id, SnapshotStatus.DEFAULT);
            }
            activeNodes.clear();
            result = "pending";
            rewrite = "none";
        }

        private void activate(String id) {
            statuses.putIfAbsent(id, SnapshotStatus.DEFAULT);
            statuses.put(id, SnapshotStatus.ACTIVE);
            activeNodes.add(id);
        }

        private void deactivate(String id) {
            activeNodes.remove(id);
            statuses.put(id, SnapshotStatus.DEFAULT);
        }

        private void finishOperation() {
            for (String id : List.copyOf(activeNodes)) {
                statuses.put(id, SnapshotStatus.DONE);
            }
            activeNodes.clear();
        }

        private void markAllDone() {
            for (String id : statuses.keySet()) {
                statuses.put(id, SnapshotStatus.DONE);
            }
            activeNodes.clear();
        }

        private void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("BST trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            List<Node> nodes = new ArrayList<>();
            List<Edge> edges = new ArrayList<>();
            collect(model.root, null, nodes, edges);
            Set<String> activeEdges = new LinkedHashSet<>();
            for (Edge edge : edges) {
                if (edge.status() == SnapshotStatus.ACTIVE) {
                    activeEdges.add(edge.id());
                }
            }
            TreeState state = new TreeState(nodes, edges, model.root == null ? null : model.root.id, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.copyOf(activeNodes), Set.copyOf(activeEdges)),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private void collect(TreeNode node, TreeNode parent, List<Node> nodes, List<Edge> edges) {
            if (node == null) {
                return;
            }
            SnapshotStatus status = statuses.getOrDefault(node.id, SnapshotStatus.DEFAULT);
            nodes.add(new Node(node.id, Integer.toString(node.key), status));
            if (parent != null) {
                String edgeId = "edge-" + parent.id + "-" + node.id;
                String side = parent.left == node ? "left" : "right";
                edges.add(new Edge(edgeId, parent.id, node.id, status, side));
            }
            collect(node.left, node, nodes, edges);
            collect(node.right, node, nodes, edges);
        }

        private List<SimulationStep> steps() {
            return List.copyOf(steps);
        }
    }

    private record Request(List<Integer> values, String operation, int operand, boolean hasOperand) {
        private Request {
            values = List.copyOf(values);
        }

        private static Request parse(JsonNode input) {
            ObjectNode object = OptionalSmallHelper.requireObject(input, TYPE);
            OptionalSmallHelper.requireExactFields(object, Set.of("values", "operation", "value", "key"), TYPE + " input");
            List<Integer> values = OptionalSmallHelper.readIntList(
                    object.get("values"), "values", TYPE, MIN_VALUES, MAX_VALUES, MAX_ABS_VALUE);
            String operation = OptionalSmallHelper.readText(object.get("operation"), "operation", TYPE);
            if (operation.equals("in-order") || operation.equals("in_order")) {
                operation = "inorder";
            }
            if (!Set.of("insert", "search", "delete", "inorder").contains(operation)) {
                throw new IllegalArgumentException(TYPE + " operation must be insert, search, delete, or inorder");
            }
            boolean hasValue = object.has("value");
            boolean hasKey = object.has("key");
            boolean needsOperand = !operation.equals("inorder");
            if (needsOperand && hasValue == hasKey) {
                throw new IllegalArgumentException(TYPE + " " + operation + " requires exactly one value or key field");
            }
            if (!needsOperand && (hasValue || hasKey)) {
                throw new IllegalArgumentException(TYPE + " inorder does not accept value or key");
            }
            int operand = 0;
            if (hasValue) {
                operand = OptionalSmallHelper.readInt(object.get("value"), "value", TYPE);
            } else if (hasKey) {
                operand = OptionalSmallHelper.readInt(object.get("key"), "key", TYPE);
            }
            if (Math.abs((long) operand) > MAX_ABS_VALUE) {
                throw new IllegalArgumentException(TYPE + " operation operand must have absolute value at most "
                        + MAX_ABS_VALUE);
            }
            return new Request(values, operation, operand, needsOperand);
        }

        private String operationDescription() {
            return hasOperand ? operation + "(" + operand + ")" : operation + "()";
        }
    }
}
