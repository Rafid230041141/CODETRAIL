package dev.codetrail.desktop.simulation.structures;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.GraphState;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Disjoint-set union with union-by-size and traced path compression. */
public final class DsuEngine implements SimulationEngine {
    public static final String TYPE = "DSU";
    public static final int MIN_VERTICES = 1;
    public static final int MAX_VERTICES = 12;
    public static final int MAX_OPERATIONS = 20;
    public static final int MAX_TRACE_STEPS = 1024;

    private static final int LINE_MAKE_SET = 1;
    private static final int LINE_FIND = 2;
    private static final int LINE_FIND_ROOT = 3;
    private static final int LINE_FIND_RECURSE = 4;
    private static final int LINE_FIND_COMPRESS = 5;
    private static final int LINE_FIND_RETURN = 6;
    private static final int LINE_UNION = 7;
    private static final int LINE_UNION_ROOTS = 8;
    private static final int LINE_UNION_SAME = 9;
    private static final int LINE_UNION_SIZE = 10;
    private static final int LINE_UNION_ATTACH = 11;
    private static final int LINE_UNION_UPDATE = 12;

    private static final List<String> PSEUDOCODE = List.of(
            "makeSet(v): parent[v] = v; size[v] = 1",
            "find(x):",
            "    if parent[x] == x: return x",
            "    root = find(parent[x])",
            "    parent[x] = root  // path compression",
            "    return root",
            "union(a, b):",
            "    rootA = find(a); rootB = find(b)",
            "    if rootA == rootB: return",
            "    if size[rootA] < size[rootB]: swap(rootA, rootB)",
            "    parent[rootB] = rootA",
            "    size[rootA] += size[rootB]; components--");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        Model model = new Model(request.vertices());
        Trace trace = new Trace(model);

        trace.add(
                0,
                "Initialize DSU with " + request.vertices() + " singleton component(s)",
                StepEventType.INITIALIZE,
                facts(model, "initialize", "none", SnapshotStatus.ACTIVE));
        for (int vertex = 0; vertex < request.vertices(); vertex++) {
            trace.activate(vertex);
            trace.add(
                    LINE_MAKE_SET,
                    "Make singleton set for vertex " + vertex,
                    StepEventType.EXECUTE_LINE,
                    facts(model, "makeSet(" + vertex + ")", "none", SnapshotStatus.ACTIVE));
            trace.markDone(vertex);
        }

        for (int operationIndex = 0; operationIndex < request.operations().size(); operationIndex++) {
            Operation operation = request.operations().get(operationIndex);
            trace.beginOperation();
            OperationContext context = new OperationContext(operation.describe(operationIndex));
            if (operation.isUnion()) {
                executeUnion(model, trace, context, operation.first(), operation.second());
            } else {
                executeFind(model, trace, context, operation.first());
            }
            trace.finishOperation();
        }

        trace.markAllDone();
        trace.add(
                LINE_FIND_RETURN,
                "Return parent pointers, component sizes, and " + model.components + " component(s)",
                StepEventType.EXECUTE_LINE,
                facts(model, "return", "none", SnapshotStatus.DONE));
        trace.add(
                0,
                "Complete: DSU has " + model.components + " component(s)",
                StepEventType.COMPLETE,
                facts(model, "complete", "none", SnapshotStatus.DONE));
        return trace.steps();
    }

    private static void executeUnion(
            Model model,
            Trace trace,
            OperationContext context,
            int first,
            int second) {
        trace.activate(first);
        trace.activate(second);
        trace.add(
                LINE_UNION,
                "Start union(" + first + ", " + second + ")",
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));

        trace.add(
                LINE_UNION_ROOTS,
                "Find roots for vertices " + first + " and " + second,
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        int rootFirst = find(model, trace, context, first);
        int rootSecond = find(model, trace, context, second);
        model.rootFirst = rootFirst;
        model.rootSecond = rootSecond;
        trace.add(
                LINE_UNION_ROOTS,
                "Roots are " + rootFirst + " and " + rootSecond,
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));

        if (rootFirst == rootSecond) {
            trace.add(
                    LINE_UNION_SAME,
                    "Vertices " + first + " and " + second + " are already connected at root " + rootFirst,
                    StepEventType.EXECUTE_LINE,
                    facts(model, context.operation, context.rewriteText(), SnapshotStatus.DONE));
            return;
        }

        trace.activate(rootFirst);
        trace.activate(rootSecond);
        trace.add(
                LINE_UNION_SIZE,
                "Compare component sizes " + model.size[rootFirst] + " and " + model.size[rootSecond],
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        if (model.size[rootFirst] < model.size[rootSecond]) {
            int temporary = rootFirst;
            rootFirst = rootSecond;
            rootSecond = temporary;
            model.rootFirst = rootFirst;
            model.rootSecond = rootSecond;
            trace.add(
                    LINE_UNION_SIZE,
                    "Keep larger component rooted at " + rootFirst,
                    StepEventType.EXECUTE_LINE,
                    facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        }

        int oldParent = model.parent[rootSecond];
        model.parent[rootSecond] = rootFirst;
        context.rewrites.add("parent[" + rootSecond + "]: " + oldParent + " -> " + rootFirst);
        trace.activate(rootSecond);
        trace.activate(rootFirst);
        trace.add(
                LINE_UNION_ATTACH,
                "Rewrite parent[" + rootSecond + "] from " + oldParent + " to " + rootFirst,
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));

        model.size[rootFirst] += model.size[rootSecond];
        model.components--;
        trace.add(
                LINE_UNION_UPDATE,
                "Merge sizes into root " + rootFirst + " = " + model.size[rootFirst]
                        + " and decrement components to " + model.components,
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
    }

    private static void executeFind(Model model, Trace trace, OperationContext context, int vertex) {
        trace.activate(vertex);
        trace.add(
                LINE_FIND,
                "Start find(" + vertex + ")",
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        int root = find(model, trace, context, vertex);
        trace.add(
                LINE_FIND_RETURN,
                "Return root " + root + " for find(" + vertex + ")",
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.DONE));
    }

    /** Recursive find exposes every real recursive walk and pointer rewrite. */
    private static int find(Model model, Trace trace, OperationContext context, int vertex) {
        trace.activate(vertex);
        model.currentVertex = vertex;
        trace.add(
                LINE_FIND,
                "Inspect parent[" + vertex + "] = " + model.parent[vertex],
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        if (model.parent[vertex] == vertex) {
            trace.add(
                    LINE_FIND_ROOT,
                    "Vertex " + vertex + " is a root",
                    StepEventType.EXECUTE_LINE,
                    facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
            return vertex;
        }

        int parent = model.parent[vertex];
        trace.activate(parent);
        trace.add(
                LINE_FIND_RECURSE,
                "Follow parent[" + vertex + "] to find(" + parent + ")",
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        int root = find(model, trace, context, parent);
        int oldParent = model.parent[vertex];
        model.currentVertex = vertex;
        trace.add(
                LINE_FIND_COMPRESS,
                "Set parent[" + vertex + "] to returned root " + root,
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        if (oldParent != root) {
            model.parent[vertex] = root;
            context.rewrites.add("parent[" + vertex + "]: " + oldParent + " -> " + root);
            trace.add(
                    LINE_FIND_COMPRESS,
                    "Rewrite parent[" + vertex + "] from " + oldParent + " to " + root
                            + " (path compression)",
                    StepEventType.EXECUTE_LINE,
                    facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        }
        trace.add(
                LINE_FIND_RETURN,
                "Return root " + root + " while unwinding find(" + vertex + ")",
                StepEventType.EXECUTE_LINE,
                facts(model, context.operation, context.rewriteText(), SnapshotStatus.ACTIVE));
        return root;
    }

    private static List<Fact> facts(
            Model model,
            String operation,
            String rewrite,
            SnapshotStatus status) {
        String parent = model.formatParents();
        String sizes = model.formatComponentSizes();
        String components = Integer.toString(model.components);
        return List.of(
                new Fact("operation", operation, SnapshotStatus.DEFAULT),
                new Fact("current-node", model.currentVertex < 0 ? "none" : Integer.toString(model.currentVertex), status),
                new Fact("root-a", model.rootFirst < 0 ? "none" : Integer.toString(model.rootFirst), status),
                new Fact("root-b", model.rootSecond < 0 ? "none" : Integer.toString(model.rootSecond), status),
                new Fact("size-a", model.rootFirst < 0 ? "none" : Integer.toString(model.size[model.rootFirst]), status),
                new Fact("size-b", model.rootSecond < 0 ? "none" : Integer.toString(model.size[model.rootSecond]), status),
                new Fact("parent", parent, status),
                new Fact("parent-pointers", parent, status),
                new Fact("size", sizes, status),
                new Fact("component-sizes", sizes, status),
                new Fact("components", components, status),
                new Fact("component-count", components, status),
                new Fact("rewrite", rewrite, status),
                new Fact("parent-rewrite", rewrite, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("vertices", 6);
        ArrayNode operations = defaultInput.putArray("operations");
        addUnion(operations, 0, 1);
        addUnion(operations, 2, 3);
        addUnion(operations, 0, 2);
        addFind(operations, 3);
        addUnion(operations, 4, 5);
        return new SimulationMetadata(
                TYPE,
                "DSU (Union-Find)",
                "O(α(n)) amortized per operation",
                "O(n)",
                RendererFamily.GRAPH,
                defaultInput,
                "Enter JSON as {\"vertices\":6,\"operations\":[{\"kind\":\"union\",\"a\":0,\"b\":1},{\"kind\":\"find\",\"x\":1}]}; "
                        + "vertices must be " + MIN_VERTICES + ".." + MAX_VERTICES
                        + ", there may be at most " + MAX_OPERATIONS
                        + " operations, and each operation must be exactly a union(a,b) or find(x) form.",
                PSEUDOCODE);
    }

    private static void addUnion(ArrayNode operations, int first, int second) {
        ObjectNode operation = operations.addObject();
        operation.put("kind", "union");
        operation.put("a", first);
        operation.put("b", second);
    }

    private static void addFind(ArrayNode operations, int vertex) {
        ObjectNode operation = operations.addObject();
        operation.put("kind", "find");
        operation.put("x", vertex);
    }

    private static final class Model {
        private final int[] parent;
        private final int[] size;
        private int components;
        private int currentVertex = -1;
        private int rootFirst = -1;
        private int rootSecond = -1;

        private Model(int vertices) {
            parent = new int[vertices];
            size = new int[vertices];
            for (int vertex = 0; vertex < vertices; vertex++) {
                parent[vertex] = vertex;
                size[vertex] = 1;
            }
            components = vertices;
        }

        private String formatParents() {
            StringBuilder formatted = new StringBuilder("{");
            for (int vertex = 0; vertex < parent.length; vertex++) {
                if (vertex > 0) {
                    formatted.append(", ");
                }
                formatted.append(vertex).append('=').append(parent[vertex]);
            }
            return formatted.append('}').toString();
        }

        private String formatComponentSizes() {
            StringBuilder formatted = new StringBuilder("{");
            boolean first = true;
            for (int vertex = 0; vertex < parent.length; vertex++) {
                if (parent[vertex] == vertex) {
                    if (!first) {
                        formatted.append(", ");
                    }
                    formatted.append(vertex).append('=').append(size[vertex]);
                    first = false;
                }
            }
            return formatted.append('}').toString();
        }
    }

    private static final class OperationContext {
        private final String operation;
        private final List<String> rewrites = new ArrayList<>();

        private OperationContext(String operation) {
            this.operation = operation;
        }

        private String rewriteText() {
            return rewrites.isEmpty() ? "none" : String.join("; ", rewrites);
        }
    }

    private static final class Trace {
        private final Model model;
        private final SnapshotStatus[] statuses;
        private final Set<Integer> activeVertices = new LinkedHashSet<>();
        private final List<SimulationStep> steps = new ArrayList<>();

        private Trace(Model model) {
            this.model = Objects.requireNonNull(model, "model");
            statuses = new SnapshotStatus[model.parent.length];
            Arrays.fill(statuses, SnapshotStatus.DEFAULT);
        }

        private void beginOperation() {
            model.currentVertex = -1;
            model.rootFirst = -1;
            model.rootSecond = -1;
            for (int vertex : activeVertices) {
                statuses[vertex] = SnapshotStatus.DEFAULT;
            }
            activeVertices.clear();
        }

        private void activate(int vertex) {
            if (vertex < 0 || vertex >= statuses.length) {
                throw new IllegalArgumentException("DSU trace vertex out of range: " + vertex);
            }
            statuses[vertex] = SnapshotStatus.ACTIVE;
            activeVertices.add(vertex);
        }

        private void markDone(int vertex) {
            if (vertex < 0 || vertex >= statuses.length) {
                throw new IllegalArgumentException("DSU trace vertex out of range: " + vertex);
            }
            statuses[vertex] = SnapshotStatus.DONE;
            activeVertices.remove(vertex);
        }

        private void finishOperation() {
            for (int vertex : List.copyOf(activeVertices)) {
                statuses[vertex] = SnapshotStatus.DONE;
            }
            activeVertices.clear();
        }

        private void markAllDone() {
            Arrays.fill(statuses, SnapshotStatus.DONE);
            activeVertices.clear();
        }

        private void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("DSU trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            if (narration == null || narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(facts, "facts");

            List<Node> nodes = new ArrayList<>(model.parent.length);
            for (int vertex = 0; vertex < model.parent.length; vertex++) {
                nodes.add(new Node(Integer.toString(vertex), Integer.toString(vertex), statuses[vertex]));
            }
            List<Edge> edges = new ArrayList<>();
            Set<String> activeEdges = new LinkedHashSet<>();
            for (int child = 0; child < model.parent.length; child++) {
                int parent = model.parent[child];
                if (parent == child) {
                    continue;
                }
                String id = edgeId(child);
                SnapshotStatus status = statuses[child];
                edges.add(new Edge(id, Integer.toString(child), Integer.toString(parent), status));
                if (status == SnapshotStatus.ACTIVE) {
                    activeEdges.add(id);
                }
            }
            GraphState state = new GraphState(nodes, edges, true, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(
                            state,
                            activeVertices.stream()
                                    .map(vertex -> Integer.toString(vertex))
                                    .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                            Set.copyOf(activeEdges)),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        private static String edgeId(int child) {
            return "parent-" + child;
        }
    }

    private record Operation(String kind, int first, int second) {
        private boolean isUnion() {
            return kind.equals("union");
        }

        private String describe(int index) {
            return isUnion()
                    ? "operation " + index + ": union(" + first + "," + second + ")"
                    : "operation " + index + ": find(" + first + ")";
        }
    }

    private record Request(int vertices, List<Operation> operations) {
        private static Request parse(JsonNode input) {
            if (input == null || !input.isObject()) {
                throw new IllegalArgumentException(TYPE + " input must be a JSON object");
            }
            int vertices = readInt(input.get("vertices"), "DSU vertices");
            if (vertices < MIN_VERTICES || vertices > MAX_VERTICES) {
                throw new IllegalArgumentException(
                        "DSU vertices must be in the inclusive range " + MIN_VERTICES + ".." + MAX_VERTICES);
            }
            JsonNode operationsNode = input.get("operations");
            if (operationsNode == null || !operationsNode.isArray()) {
                throw new IllegalArgumentException("DSU operations must be a JSON array");
            }
            if (operationsNode.size() > MAX_OPERATIONS) {
                throw new IllegalArgumentException("DSU operations must contain at most " + MAX_OPERATIONS + " entries");
            }

            List<Operation> operations = new ArrayList<>(operationsNode.size());
            for (int index = 0; index < operationsNode.size(); index++) {
                JsonNode operationNode = operationsNode.get(index);
                if (operationNode == null || !operationNode.isObject()) {
                    throw new IllegalArgumentException("DSU operation " + index + " must be an object");
                }
                ObjectNode operation = (ObjectNode) operationNode;
                JsonNode kindNode = operation.get("kind");
                if (kindNode == null || !kindNode.isTextual()) {
                    throw new IllegalArgumentException("DSU operation " + index + " kind must be text");
                }
                String kind = kindNode.textValue();
                if (kind.equals("union")) {
                    requireExactFields(operation, Set.of("kind", "a", "b"), index);
                    int first = readInt(operation.get("a"), "DSU operation " + index + " a");
                    int second = readInt(operation.get("b"), "DSU operation " + index + " b");
                    checkVertex(first, vertices, "DSU operation " + index + " a");
                    checkVertex(second, vertices, "DSU operation " + index + " b");
                    operations.add(new Operation(kind, first, second));
                } else if (kind.equals("find")) {
                    requireExactFields(operation, Set.of("kind", "x"), index);
                    int vertex = readInt(operation.get("x"), "DSU operation " + index + " x");
                    checkVertex(vertex, vertices, "DSU operation " + index + " x");
                    operations.add(new Operation(kind, vertex, -1));
                } else {
                    throw new IllegalArgumentException(
                            "DSU operation " + index + " kind must be exactly union or find");
                }
            }
            return new Request(vertices, List.copyOf(operations));
        }

        private static void requireExactFields(ObjectNode operation, Set<String> allowed, int index) {
            Iterator<String> fields = operation.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!allowed.contains(field)) {
                    throw new IllegalArgumentException(
                            "DSU operation " + index + " has unsupported field: " + field);
                }
            }
        }

        private static int readInt(JsonNode node, String field) {
            if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
                throw new IllegalArgumentException(field + " must be an integer");
            }
            return node.intValue();
        }

        private static void checkVertex(int vertex, int vertices, String field) {
            if (vertex < 0 || vertex >= vertices) {
                throw new IllegalArgumentException(field + " must be in the range 0.." + (vertices - 1));
            }
        }
    }
}
