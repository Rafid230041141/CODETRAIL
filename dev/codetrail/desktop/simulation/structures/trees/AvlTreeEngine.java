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

/** AVL insertion with real height maintenance and LL/RR/LR/RL rotations. */
public final class AvlTreeEngine implements SimulationEngine {
    public static final String TYPE = "AVL_TREE";
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
    private static final int LINE_HEIGHT = 7;
    private static final int LINE_BALANCE = 8;
    private static final int LINE_ROTATE = 9;
    private static final int LINE_ROTATE_CHILD = 10;
    private static final int LINE_ROTATE_ROOT = 11;
    private static final int LINE_SEARCH = 12;
    private static final int LINE_INORDER = 13;
    private static final int LINE_RETURN = 14;

    private static final List<String> PSEUDOCODE = List.of(
            "insert(node, key):",
            "    compare key with node.key",
            "    if key < node.key: recurse into left",
            "    if key > node.key: recurse into right",
            "    if node is null: create a node",
            "    if key == node.key: keep one copy",
            "    node.height = 1 + max(height(left), height(right))",
            "    balance = height(left) - height(right)",
            "    if balance > 1 or balance < -1: classify the case",
            "    LR/RL: rotate the inner child first",
            "    rotate the unbalanced node",
            "search(node, key): follow the comparison path",
            "inorder(node): visit left, node, right",
            "return the balanced tree, heights, and result");

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
                "Initialize an empty AVL tree",
                StepEventType.INITIALIZE,
                facts(model, trace, "initialize", "none", "initialize", SnapshotStatus.ACTIVE));

        for (int value : request.values()) {
            trace.beginOperation();
            trace.operation = "insert(" + value + ")";
            trace.path.clear();
            trace.result = "pending";
            model.root = insert(model.root, value, model, trace);
            trace.finishOperation();
        }

        trace.beginOperation();
        trace.operation = request.operationDescription();
        trace.path.clear();
        trace.result = "pending";
        trace.rotation = "none";
        switch (request.operation()) {
            case "insert" -> model.root = insert(model.root, request.operand(), model, trace);
            case "search" -> search(model.root, request.operand(), trace);
            case "inorder" -> {
                List<Integer> values = new ArrayList<>();
                inorder(model.root, values, trace);
                trace.result = OptionalSmallHelper.formatInts(values);
            }
            default -> throw new IllegalStateException("unsupported validated AVL operation: " + request.operation());
        }
        trace.finishOperation();

        trace.markAllDone();
        List<Integer> finalOrder = new ArrayList<>();
        inorderValues(model.root, finalOrder);
        trace.add(
                LINE_RETURN,
                "Return the AVL tree with in-order keys " + finalOrder,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, request.operation(), trace.result, "return", SnapshotStatus.DONE));
        trace.add(
                0,
                "Complete: AVL tree contains " + finalOrder.size() + " distinct key(s)",
                StepEventType.COMPLETE,
                facts(model, trace, "complete", trace.result, "complete", SnapshotStatus.DONE));
        return trace.steps();
    }

    private static TreeNode insert(TreeNode node, int key, Model model, Trace trace) {
        return insert(node, key, model, trace, null, false);
    }

    private static TreeNode insert(TreeNode node, int key, Model model, Trace trace, TreeNode parent, boolean left) {
        if (node == null) {
            TreeNode created = model.newNode(key);
            if (parent == null) model.root = created;
            else if (left) parent.left = created;
            else parent.right = created;
            trace.activate(created.id);
            trace.result = "INSERTED";
            trace.rewrite = "new node " + created.id + "(" + key + ")";
            trace.add(
                    LINE_CREATE,
                    "Create AVL node " + key + " at the empty child position",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; create", SnapshotStatus.ACTIVE));
            return created;
        }

        trace.activate(node.id);
        trace.path.add(node.key);
        trace.add(
                LINE_COMPARE,
                "Compare key " + key + " with AVL node " + node.key,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, "pending", "insert; compare", SnapshotStatus.ACTIVE));
        if (key < node.key) {
            trace.add(
                    LINE_LEFT,
                    "Key " + key + " is smaller; follow the left link",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, "pending", "insert; left", SnapshotStatus.ACTIVE));
            TreeNode oldChild = node.left;
            node.left = insert(node.left, key, model, trace, node, true);
            if (oldChild != node.left) {
                trace.rewrite = "left(" + node.key + ") -> " + node.left.id;
                trace.add(
                        LINE_LEFT,
                        "Rewrite left link of node " + node.key + " to " + node.left.key,
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, trace.result, "insert; rewire", SnapshotStatus.ACTIVE));
            }
        } else if (key > node.key) {
            trace.add(
                    LINE_RIGHT,
                    "Key " + key + " is larger; follow the right link",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, "pending", "insert; right", SnapshotStatus.ACTIVE));
            TreeNode oldChild = node.right;
            node.right = insert(node.right, key, model, trace, node, false);
            if (oldChild != node.right) {
                trace.rewrite = "right(" + node.key + ") -> " + node.right.id;
                trace.add(
                        LINE_RIGHT,
                        "Rewrite right link of node " + node.key + " to " + node.right.key,
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, trace.result, "insert; rewire", SnapshotStatus.ACTIVE));
            }
        } else {
            trace.result = "DUPLICATE_IGNORED";
            trace.add(
                    LINE_DUPLICATE,
                    "Key " + key + " already exists; keep one AVL node",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; duplicate", SnapshotStatus.DONE));
            return node;
        }

        updateHeight(node);
        trace.activate(node.id);
        trace.add(
                LINE_HEIGHT,
                "Height(" + node.key + ") = 1 + max(" + height(node.left) + ", " + height(node.right) + ") = " + node.height,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, trace.result, "insert; height", SnapshotStatus.ACTIVE));
        int balance = balance(node);
        trace.add(
                LINE_BALANCE,
                "Balance(" + node.key + ") = left height " + height(node.left)
                        + " - right height " + height(node.right) + " = " + balance,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, trace.result, "insert; balance", SnapshotStatus.ACTIVE));

        if (balance > 1 && balance(node.left) >= 0) {
            trace.rotation = "LL";
            trace.add(
                    LINE_ROTATE,
                    "LL rotation: rotate right at node " + node.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; LL", SnapshotStatus.ACTIVE));
            rotateRight(node, trace, LINE_ROTATE_ROOT);
        } else if (balance < -1 && balance(node.right) <= 0) {
            trace.rotation = "RR";
            trace.add(
                    LINE_ROTATE,
                    "RR rotation: rotate left at node " + node.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; RR", SnapshotStatus.ACTIVE));
            rotateLeft(node, trace, LINE_ROTATE_ROOT);
        } else if (balance > 1) {
            trace.rotation = "LR";
            trace.add(
                    LINE_ROTATE_CHILD,
                    "LR rotation: rotate left at the inner child " + node.left.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; LR child", SnapshotStatus.ACTIVE));
            rotateLeft(node.left, trace, LINE_ROTATE_CHILD);
            trace.add(
                    LINE_ROTATE_ROOT,
                    "LR rotation: rotate right at node " + node.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; LR root", SnapshotStatus.ACTIVE));
            rotateRight(node, trace, LINE_ROTATE_ROOT);
        } else if (balance < -1) {
            trace.rotation = "RL";
            trace.add(
                    LINE_ROTATE_CHILD,
                    "RL rotation: rotate right at the inner child " + node.right.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; RL child", SnapshotStatus.ACTIVE));
            rotateRight(node.right, trace, LINE_ROTATE_CHILD);
            trace.add(
                    LINE_ROTATE_ROOT,
                    "RL rotation: rotate left at node " + node.key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; RL root", SnapshotStatus.ACTIVE));
            rotateLeft(node, trace, LINE_ROTATE_ROOT);
        }
        return node;
    }

    /** Rotate in place so every intermediate snapshot remains a connected tree. */
    private static void rotateRight(TreeNode root, Trace trace, int line) {
        TreeNode child = root.left;
        if (child == null) {
            throw new IllegalStateException("right rotation requires a left child");
        }
        TreeNode middle = child.right;
        prepareRotation(root, child, middle, "right", trace, line);
        TreeNode oldRight = root.right;
        int oldRootKey = root.key;
        root.key = child.key;
        child.key = oldRootKey;
        root.left = child.left;
        child.left = middle;
        child.right = oldRight;
        root.right = child;
        updateHeight(child);
        updateHeight(root);
        trace.activate(root.id);
        trace.activate(child.id);
        trace.currentNode = root.id;
        trace.rotationRoot = child.id;
        trace.promotedNode = root.id;
        trace.rewrite = "promote " + root.key + "; move middle subtree "
                + (middle == null ? "empty" : middle.key) + " to the left of " + child.key;
        trace.add(
                line,
                "Rotate right: " + root.key + " moves above " + child.key
                        + "; middle subtree " + (middle == null ? "is empty" : middle.key + " becomes the left child of " + child.key),
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, trace.result, "insert; rotate right", SnapshotStatus.ACTIVE));
    }

    /** Rotate in place so every intermediate snapshot remains a connected tree. */
    private static void rotateLeft(TreeNode root, Trace trace, int line) {
        TreeNode child = root.right;
        if (child == null) {
            throw new IllegalStateException("left rotation requires a right child");
        }
        TreeNode leftSubtree = root.left;
        TreeNode middle = child.left;
        prepareRotation(root, child, middle, "left", trace, line);
        TreeNode oldRight = child.right;
        int oldRootKey = root.key;
        root.key = child.key;
        child.key = oldRootKey;
        child.left = leftSubtree;
        child.right = middle;
        root.left = child;
        root.right = oldRight;
        updateHeight(child);
        updateHeight(root);
        trace.activate(root.id);
        trace.activate(child.id);
        trace.currentNode = root.id;
        trace.rotationRoot = child.id;
        trace.promotedNode = root.id;
        trace.rewrite = "promote " + root.key + "; move middle subtree "
                + (middle == null ? "empty" : middle.key) + " to the right of " + child.key;
        trace.add(
                line,
                "Rotate left: " + root.key + " moves above " + child.key
                        + "; middle subtree " + (middle == null ? "is empty" : middle.key + " becomes the right child of " + child.key),
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, trace.result, "insert; rotate left", SnapshotStatus.ACTIVE));
    }

    private static void prepareRotation(TreeNode root, TreeNode child, TreeNode middle,
            String direction, Trace trace, int line) {
        trace.rotationRoot = root.id;
        trace.promotedNode = child.id;
        trace.middleSubtree = middle == null ? "null" : middle.id;
        trace.rotationDirection = direction;
        trace.activate(root.id);
        trace.activate(child.id);
        if (middle != null) trace.activate(middle.id);
        trace.currentNode = root.id;
        trace.add(line, "Rotate " + direction + " at " + root.key + ": promote " + child.key
                        + "; preserve the middle subtree " + (middle == null ? "(empty)" : "rooted at " + middle.key),
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, trace.result, "insert; before rotation", SnapshotStatus.ACTIVE));
    }

    private static TreeNode nodeById(TreeNode node, String id) {
        if (node == null || id.equals("none")) return null;
        if (node.id.equals(id)) return node;
        TreeNode found = nodeById(node.left, id);
        return found == null ? nodeById(node.right, id) : found;
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
                "Search compares key " + key + " with AVL node " + node.key,
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, "pending", "search; compare", SnapshotStatus.ACTIVE));
        if (key == node.key) {
            trace.result = "FOUND";
            trace.add(
                    LINE_SEARCH,
                    "Key " + key + " is FOUND at AVL node " + node.id,
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

    private static void inorder(TreeNode node, List<Integer> values, Trace trace) {
        if (node == null) {
            return;
        }
        trace.activate(node.id);
        inorder(node.left, values, trace);
        values.add(node.key);
        trace.add(
                LINE_INORDER,
                "Visit AVL node " + node.key + " in left-node-right order",
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

    private static int height(TreeNode node) {
        return node == null ? 0 : node.height;
    }

    private static int balance(TreeNode node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private static void updateHeight(TreeNode node) {
        node.height = 1 + Math.max(height(node.left), height(node.right));
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
        List<String> keys = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        List<Integer> balances = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<Integer> nodeHeights = new ArrayList<>();
        List<Integer> nodeBalances = new ArrayList<>();
        metrics(model.root, keys, heights, balances, ids, nodeHeights, nodeBalances);
        TreeNode current = nodeById(model.root, trace.currentNode);
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
                new Fact("height", Integer.toString(height(model.root)), status),
                new Fact("root-height", Integer.toString(height(model.root)), status),
                new Fact("heights", OptionalSmallHelper.formatMap(keys, heights), status),
                new Fact("balance", OptionalSmallHelper.formatMap(keys, balances), status),
                new Fact("balance-factors", OptionalSmallHelper.formatMap(keys, balances), status),
                new Fact("height-by-node", OptionalSmallHelper.formatMap(ids, nodeHeights), status),
                new Fact("balance-by-node", OptionalSmallHelper.formatMap(ids, nodeBalances), status),
                new Fact("current-node", trace.currentNode, status),
                new Fact("current-height", current == null ? "none" : Integer.toString(current.height), status),
                new Fact("current-balance", current == null ? "none" : Integer.toString(balance(current)), status),
                new Fact("rotation-root", trace.rotationRoot, status),
                new Fact("promoted-node", trace.promotedNode, status),
                new Fact("middle-subtree", trace.middleSubtree, status),
                new Fact("rotation-direction", trace.rotationDirection, status),
                new Fact("rotation", trace.rotation, status),
                new Fact("path", OptionalSmallHelper.formatStrings(trace.path.stream().map(value -> Integer.toString(value)).toList()), status),
                new Fact("rewrite", trace.rewrite, status),
                new Fact("duplicate-policy", "ignore", SnapshotStatus.DEFAULT));
    }

    private static void metrics(
            TreeNode node,
            List<String> keys,
            List<Integer> heights,
            List<Integer> balances,
            List<String> ids,
            List<Integer> nodeHeights,
            List<Integer> nodeBalances) {
        if (node == null) {
            return;
        }
        keys.add(Integer.toString(node.key));
        heights.add(node.height);
        balances.add(balance(node));
        ids.add(node.id);
        nodeHeights.add(node.height);
        nodeBalances.add(balance(node));
        metrics(node.left, keys, heights, balances, ids, nodeHeights, nodeBalances);
        metrics(node.right, keys, heights, balances, ids, nodeHeights, nodeBalances);
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode values = defaultInput.putArray("values");
        for (int value : new int[] {30, 20, 10, 25, 28}) {
            values.add(value);
        }
        defaultInput.put("operation", "insert");
        defaultInput.put("value", 27);
        return new SimulationMetadata(
                TYPE,
                "AVL Tree",
                "O(log n) search, insert, and rotation",
                "O(n)",
                RendererFamily.TREE,
                defaultInput,
                "Enter JSON as {\"values\":[30,20,10,25,28],\"operation\":\"insert\",\"value\":27}; values are bounded integers and operation is insert, search, or inorder.",
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
        private int height = 1;
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
        private String rotation = "none";
        private String currentNode = "none";
        private String rotationRoot = "none";
        private String promotedNode = "none";
        private String middleSubtree = "none";
        private String rotationDirection = "none";

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
            rotation = "none";
            currentNode = "none";
            rotationRoot = "none";
            promotedNode = "none";
            middleSubtree = "none";
            rotationDirection = "none";
        }

        private void activate(String id) {
            currentNode = id;
            statuses.putIfAbsent(id, SnapshotStatus.DEFAULT);
            statuses.put(id, SnapshotStatus.ACTIVE);
            activeNodes.add(id);
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
                throw new IllegalStateException("AVL trace exceeded " + MAX_TRACE_STEPS + " steps");
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
                String side = parent.left == node ? "left" : "right";
                edges.add(new Edge("edge-" + parent.id + "-" + node.id, parent.id, node.id, status, side));
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
            if (!Set.of("insert", "search", "inorder").contains(operation)) {
                throw new IllegalArgumentException(TYPE + " operation must be insert, search, or inorder");
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
            if (operation.equals("insert")) {
                Set<Integer> distinct = new java.util.HashSet<>(values);
                if (distinct.size() >= MAX_VALUES && !distinct.contains(operand)) {
                    throw new IllegalArgumentException(TYPE + " tree cannot exceed " + MAX_VALUES + " nodes");
                }
            }
            return new Request(values, operation, operand, needsOperand);
        }

        private String operationDescription() {
            return hasOperand ? operation + "(" + operand + ")" : operation + "()";
        }
    }
}
