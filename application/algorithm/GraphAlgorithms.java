package application.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Small, deterministic algorithm model intended for the graph visualizer. */
public final class GraphAlgorithms {
    private static final int INFINITY = Integer.MAX_VALUE;

    public enum Algorithm {
        DIJKSTRA,
        PRIM,
        KRUSKAL,
        BELLMAN_FORD,
        FLOYD_WARSHALL
    }

    public record Vertex(String id, double x, double y) {
        public Vertex {
            id = requireId(id, "vertex id");
        }
    }

    public record Edge(String id, String from, String to, int weight, boolean directed) {
        public Edge(String id, String from, String to, int weight) {
            this(id, from, to, weight, false);
        }

        public Edge {
            id = requireId(id, "edge id");
            from = requireId(from, "edge from");
            to = requireId(to, "edge to");
        }
    }

    public record Graph(List<Vertex> vertices, List<Edge> edges) {
        public Graph {
            vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
            edges = List.copyOf(Objects.requireNonNull(edges, "edges"));

            Set<String> vertexIds = new LinkedHashSet<>();
            for (Vertex vertex : vertices) {
                if (!vertexIds.add(vertex.id())) {
                    throw new IllegalArgumentException("duplicate vertex id: " + vertex.id());
                }
            }
            Set<String> edgeIds = new LinkedHashSet<>();
            for (Edge edge : edges) {
                if (!edgeIds.add(edge.id())) {
                    throw new IllegalArgumentException("duplicate edge id: " + edge.id());
                }
                if (!vertexIds.contains(edge.from()) || !vertexIds.contains(edge.to())) {
                    throw new IllegalArgumentException("edge references an unknown vertex: " + edge.id());
                }
            }
        }
    }

    public record Step(
            String message,
            Set<String> visitedVertices,
            Set<String> selectedEdgeIds,
            Set<String> rejectedEdgeIds,
            String activeEdgeId,
            Map<String, Integer> values,
            int totalWeight,
            int pseudocodeLine,
            boolean complete) {
        public Step {
            message = Objects.requireNonNull(message, "message");
            visitedVertices = immutableSet(visitedVertices, "visitedVertices");
            selectedEdgeIds = immutableSet(selectedEdgeIds, "selectedEdgeIds");
            rejectedEdgeIds = immutableSet(rejectedEdgeIds, "rejectedEdgeIds");
            values = immutableMap(values, "values");
        }

        private static Set<String> immutableSet(Set<String> source, String name) {
            Objects.requireNonNull(source, name);
            return Collections.unmodifiableSet(new LinkedHashSet<>(source));
        }

        private static Map<String, Integer> immutableMap(Map<String, Integer> source, String name) {
            Objects.requireNonNull(source, name);
            return Collections.unmodifiableMap(new LinkedHashMap<>(source));
        }
    }

    private GraphAlgorithms() {
    }

    public static Graph sampleGraph() {
        return new Graph(
                List.of(
                        new Vertex("A", 0.08, 0.50),
                        new Vertex("B", 0.25, 0.20),
                        new Vertex("C", 0.25, 0.80),
                        new Vertex("D", 0.46, 0.50),
                        new Vertex("E", 0.65, 0.80),
                        new Vertex("F", 0.81, 0.22),
                        new Vertex("G", 0.94, 0.52)),
                List.of(
                        new Edge("AB", "A", "B", 4),
                        new Edge("AC", "A", "C", 3),
                        new Edge("BC", "B", "C", 2),
                        new Edge("BD", "B", "D", 5),
                        new Edge("CD", "C", "D", 3),
                        new Edge("CE", "C", "E", 6),
                        new Edge("DE", "D", "E", 1),
                        new Edge("DF", "D", "F", 6),
                        new Edge("EF", "E", "F", 2),
                        new Edge("EG", "E", "G", 5),
                        new Edge("FG", "F", "G", 3)));
    }

    public static Graph sampleGraph(Algorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        return switch (algorithm) {
            case BELLMAN_FORD -> new Graph(
                    List.of(
                            new Vertex("A", 0.08, 0.50),
                            new Vertex("B", 0.28, 0.18),
                            new Vertex("C", 0.32, 0.78),
                            new Vertex("D", 0.62, 0.48),
                            new Vertex("E", 0.90, 0.48)),
                    List.of(
                            new Edge("A>B", "A", "B", 4, true),
                            new Edge("A>C", "A", "C", 2, true),
                            new Edge("B>C", "B", "C", -1, true),
                            new Edge("B>D", "B", "D", 2, true),
                            new Edge("C>D", "C", "D", 3, true),
                            new Edge("D>E", "D", "E", 1, true)));
            case FLOYD_WARSHALL -> new Graph(
                    List.of(
                            new Vertex("A", 0.10, 0.24),
                            new Vertex("B", 0.60, 0.16),
                            new Vertex("C", 0.34, 0.82),
                            new Vertex("D", 0.88, 0.70)),
                    List.of(
                            new Edge("A>B", "A", "B", 4, true),
                            new Edge("A>C", "A", "C", 11, true),
                            new Edge("B>C", "B", "C", 2, true),
                            new Edge("B>D", "B", "D", 7, true),
                            new Edge("C>D", "C", "D", 3, true)));
            case DIJKSTRA, PRIM, KRUSKAL -> sampleGraph();
        };
    }

    public static List<Step> run(Algorithm algorithm, String startVertex) {
        return run(sampleGraph(), algorithm, startVertex);
    }

    public static List<Step> run(Graph graph, Algorithm algorithm, String startVertex) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(algorithm, "algorithm");
        String start = requireId(startVertex, "start vertex");
        if (graph.vertices().stream().noneMatch(vertex -> vertex.id().equals(start))) {
            throw new IllegalArgumentException("unknown start vertex: " + start);
        }
        return switch (algorithm) {
            case DIJKSTRA -> dijkstra(graph, start);
            case PRIM -> prim(graph, start);
            case KRUSKAL -> kruskal(graph, start);
            case BELLMAN_FORD -> bellmanFord(graph, start);
            case FLOYD_WARSHALL -> floydWarshall(graph);
        };
    }

    private static List<Step> dijkstra(Graph graph, String start) {
        List<Step> steps = new ArrayList<>();
        Map<String, Integer> distances = initialValues(graph, INFINITY);
        Map<String, String> predecessors = new HashMap<>();
        Set<String> visited = new LinkedHashSet<>();
        Set<String> selected = new LinkedHashSet<>();
        Set<String> rejected = new LinkedHashSet<>();
        Map<String, Edge> edgeById = edgeById(graph);
        distances.put(start, 0);

        if (graph.edges().stream().anyMatch(edge -> edge.weight() < 0)) {
            addStep(steps, "Dijkstra requires non-negative edge weights", visited, selected, rejected,
                    null, distances, edgeById, 0, true);
            return List.copyOf(steps);
        }

        addStep(steps, "Initialize Dijkstra from " + start, visited, selected, rejected, null, distances,
                edgeById, 0, false);

        while (visited.size() < graph.vertices().size()) {
            String current = nextVertex(graph, visited, distances);
            if (current == null) {
                break;
            }
            visited.add(current);
            addStep(steps, "Visit vertex " + current, visited, selected, rejected,
                    predecessors.get(current), distances, edgeById, 2, false);

            for (Edge edge : graph.edges()) {
                String neighbor = directedNeighbor(edge, current);
                if (neighbor == null || visited.contains(neighbor)) {
                    continue;
                }
                int currentDistance = distances.get(current);
                if (currentDistance == INFINITY) {
                    continue;
                }
                int candidate = currentDistance > INFINITY - edge.weight()
                        ? INFINITY
                        : currentDistance + edge.weight();
                if (candidate < distances.get(neighbor)) {
                    String previousEdge = predecessors.put(neighbor, edge.id());
                    if (previousEdge != null) {
                        selected.remove(previousEdge);
                    }
                    selected.add(edge.id());
                    distances.put(neighbor, candidate);
                    addStep(steps, "Relax " + edge.id() + ": " + current + " -> " + neighbor,
                            visited, selected, rejected, edge.id(), distances, edgeById, 5, false);
                }
            }
        }

        addStep(steps, "Dijkstra complete", visited, selected, rejected, null, distances, edgeById, -1, true);
        return List.copyOf(steps);
    }

    private static List<Step> prim(Graph graph, String start) {
        List<Step> steps = new ArrayList<>();
        Map<String, Integer> keys = initialValues(graph, INFINITY);
        Map<String, String> predecessors = new HashMap<>();
        Set<String> visited = new LinkedHashSet<>();
        Set<String> selected = new LinkedHashSet<>();
        Set<String> rejected = new LinkedHashSet<>();
        Map<String, Edge> edgeById = edgeById(graph);
        keys.put(start, 0);

        addStep(steps, "Initialize Prim from " + start, visited, selected, rejected, null, keys,
                edgeById, 0, false);

        while (visited.size() < graph.vertices().size()) {
            String current = nextVertex(graph, visited, keys);
            if (current == null) {
                break;
            }
            visited.add(current);
            String incoming = predecessors.get(current);
            if (incoming != null) {
                selected.add(incoming);
            }
            addStep(steps, "Add vertex " + current + " to the tree", visited, selected, rejected,
                    incoming, keys, edgeById, 3, false);

            for (Edge edge : graph.edges()) {
                String neighbor = neighbor(edge, current);
                if (neighbor == null || visited.contains(neighbor) || edge.weight() >= keys.get(neighbor)) {
                    continue;
                }
                predecessors.put(neighbor, edge.id());
                keys.put(neighbor, edge.weight());
                addStep(steps, "Update key of " + neighbor + " using " + edge.id(), visited, selected,
                        rejected, edge.id(), keys, edgeById, 5, false);
            }
        }

        String completionMessage = visited.size() == graph.vertices().size()
                ? "Prim complete"
                : "Prim stopped: graph is disconnected";
        addStep(steps, completionMessage, visited, selected, rejected, null, keys, edgeById, -1, true);
        return List.copyOf(steps);
    }

    private static List<Step> kruskal(Graph graph, String start) {
        List<Step> steps = new ArrayList<>();
        Map<String, Integer> values = initialValues(graph, INFINITY);
        Set<String> visited = new LinkedHashSet<>();
        Set<String> selected = new LinkedHashSet<>();
        Set<String> rejected = new LinkedHashSet<>();
        Map<String, Edge> edgeById = edgeById(graph);
        UnionFind unionFind = new UnionFind(graph.vertices().stream().map(Vertex::id).toList());

        addStep(steps, "Initialize Kruskal", visited, selected, rejected, null, values, edgeById, 0, false);
        List<Edge> sortedEdges = graph.edges().stream()
                .sorted(Comparator.comparingInt(Edge::weight).thenComparing(Edge::id))
                .toList();
        for (Edge edge : sortedEdges) {
            if (unionFind.union(edge.from(), edge.to())) {
                selected.add(edge.id());
                visited.add(edge.from());
                visited.add(edge.to());
                addStep(steps, "Select " + edge.id() + " (" + edge.weight() + ")", visited, selected,
                        rejected, edge.id(), values, edgeById, 4, false);
            } else {
                rejected.add(edge.id());
                addStep(steps, "Reject " + edge.id() + " (cycle)", visited, selected, rejected,
                        edge.id(), values, edgeById, 5, false);
            }
        }

        String completionMessage = selected.size() == graph.vertices().size() - 1
                ? "Kruskal complete"
                : "Kruskal complete: minimum spanning forest";
        addStep(steps, completionMessage, visited, selected, rejected, null, values, edgeById, -1, true);
        return List.copyOf(steps);
    }

    private static List<Step> bellmanFord(Graph graph, String start) {
        List<Step> steps = new ArrayList<>();
        Map<String, Integer> distances = initialValues(graph, INFINITY);
        Map<String, String> predecessors = new LinkedHashMap<>();
        Set<String> selected = new LinkedHashSet<>();
        Set<String> rejected = new LinkedHashSet<>();
        Map<String, Edge> edgeById = edgeById(graph);
        List<Arc> arcs = directedArcs(graph);
        distances.put(start, 0);

        addStep(steps, "Initialize Bellman-Ford from " + start, reachableVertices(distances), selected,
                rejected, null, distances, edgeById, 0, false);
        for (int pass = 1; pass < graph.vertices().size(); pass++) {
            boolean changed = false;
            for (Arc arc : arcs) {
                addStep(steps, "Pass " + pass + ": inspect " + arc.from() + " -> " + arc.to(),
                        reachableVertices(distances), selected, rejected, arc.edge().id(), distances,
                        edgeById, 2, false);
                int fromDistance = distances.getOrDefault(arc.from(), INFINITY);
                if (fromDistance == INFINITY) {
                    continue;
                }
                int candidate = boundedDistance((long) fromDistance + arc.edge().weight());
                if (candidate >= distances.getOrDefault(arc.to(), INFINITY)) {
                    continue;
                }
                distances.put(arc.to(), candidate);
                predecessors.put(arc.to(), arc.edge().id());
                selected = new LinkedHashSet<>(predecessors.values());
                changed = true;
                addStep(steps, "Pass " + pass + ": relax " + arc.from() + " -> " + arc.to()
                        + " to " + candidate, reachableVertices(distances), selected, rejected,
                        arc.edge().id(), distances, edgeById, 3, false);
            }
            if (!changed) {
                break;
            }
        }

        Arc cycleArc = null;
        for (Arc arc : arcs) {
            int fromDistance = distances.getOrDefault(arc.from(), INFINITY);
            if (fromDistance != INFINITY
                    && boundedDistance((long) fromDistance + arc.edge().weight())
                            < distances.getOrDefault(arc.to(), INFINITY)) {
                cycleArc = arc;
                break;
            }
        }
        if (cycleArc != null) {
            rejected.add(cycleArc.edge().id());
            addStep(steps, "Reachable negative cycle detected through " + cycleArc.from() + " -> "
                    + cycleArc.to(), reachableVertices(distances), selected, rejected,
                    cycleArc.edge().id(), distances, edgeById, 5, true);
        } else {
            addStep(steps, "Bellman-Ford complete", reachableVertices(distances), selected, rejected,
                    null, distances, edgeById, 5, true);
        }
        return List.copyOf(steps);
    }

    private static List<Step> floydWarshall(Graph graph) {
        List<Step> steps = new ArrayList<>();
        List<String> vertices = graph.vertices().stream().map(Vertex::id).toList();
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (int index = 0; index < vertices.size(); index++) {
            indices.put(vertices.get(index), index);
        }
        int[][] distances = new int[vertices.size()][vertices.size()];
        for (int row = 0; row < distances.length; row++) {
            java.util.Arrays.fill(distances[row], INFINITY);
            distances[row][row] = 0;
        }
        for (Edge edge : graph.edges()) {
            int from = indices.get(edge.from());
            int to = indices.get(edge.to());
            distances[from][to] = Math.min(distances[from][to], edge.weight());
            if (!edge.directed()) {
                distances[to][from] = Math.min(distances[to][from], edge.weight());
            }
        }

        Map<String, Edge> edgeById = edgeById(graph);
        addStep(steps, "Initialize the all-pairs distance matrix", Set.of(), Set.of(), Set.of(), null,
                matrixValues(vertices, distances), edgeById, 0, false);
        for (int middle = 0; middle < vertices.size(); middle++) {
            String middleVertex = vertices.get(middle);
            addStep(steps, "Use " + middleVertex + " as the intermediate vertex", Set.of(middleVertex),
                    Set.of(), Set.of(), null, matrixValues(vertices, distances), edgeById, 1, false);
            for (int from = 0; from < vertices.size(); from++) {
                for (int to = 0; to < vertices.size(); to++) {
                    if (distances[from][middle] == INFINITY || distances[middle][to] == INFINITY) {
                        continue;
                    }
                    int candidate = boundedDistance((long) distances[from][middle] + distances[middle][to]);
                    if (candidate >= distances[from][to]) {
                        continue;
                    }
                    distances[from][to] = candidate;
                    String fromVertex = vertices.get(from);
                    String toVertex = vertices.get(to);
                    String activeEdge = pathEdgeId(graph, fromVertex, middleVertex, toVertex);
                    addStep(steps, "Improve " + fromVertex + " -> " + toVertex + " through "
                            + middleVertex + " to " + candidate,
                            vertexSet(fromVertex, middleVertex, toVertex), Set.of(), Set.of(), activeEdge,
                            matrixValues(vertices, distances), edgeById, 3, false);
                }
            }
        }

        String negativeVertex = null;
        for (int index = 0; index < vertices.size(); index++) {
            if (distances[index][index] < 0) {
                negativeVertex = vertices.get(index);
                break;
            }
        }
        if (negativeVertex != null) {
            addStep(steps, "Negative cycle detected through " + negativeVertex, Set.of(negativeVertex),
                    Set.of(), Set.of(), null, matrixValues(vertices, distances), edgeById, 5, true);
        } else {
            addStep(steps, "Floyd-Warshall complete", Set.of(), Set.of(), Set.of(), null,
                    matrixValues(vertices, distances), edgeById, 6, true);
        }
        return List.copyOf(steps);
    }

    private static void addStep(
            List<Step> steps,
            String message,
            Set<String> visited,
            Set<String> selected,
            Set<String> rejected,
            String activeEdge,
            Map<String, Integer> values,
            Map<String, Edge> edgeById,
            int pseudocodeLine,
            boolean complete) {
        steps.add(new Step(message, visited, selected, rejected, activeEdge, values,
                totalWeight(selected, edgeById), pseudocodeLine, complete));
    }

    private static int totalWeight(Set<String> selected, Map<String, Edge> edgeById) {
        int total = 0;
        for (String edgeId : selected) {
            total += edgeById.get(edgeId).weight();
        }
        return total;
    }

    private static Map<String, Integer> initialValues(Graph graph, int value) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (Vertex vertex : graph.vertices()) {
            values.put(vertex.id(), value);
        }
        return values;
    }

    private static Map<String, Integer> matrixValues(List<String> vertices, int[][] distances) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (int row = 0; row < vertices.size(); row++) {
            for (int column = 0; column < vertices.size(); column++) {
                values.put(vertices.get(row) + "->" + vertices.get(column), distances[row][column]);
            }
        }
        return values;
    }

    private static Set<String> reachableVertices(Map<String, Integer> distances) {
        Set<String> reachable = new LinkedHashSet<>();
        distances.forEach((vertex, distance) -> {
            if (distance != INFINITY) {
                reachable.add(vertex);
            }
        });
        return reachable;
    }

    private static Set<String> vertexSet(String... vertices) {
        Set<String> result = new LinkedHashSet<>();
        Collections.addAll(result, vertices);
        return result;
    }

    private static List<Arc> directedArcs(Graph graph) {
        List<Arc> arcs = new ArrayList<>();
        for (Edge edge : graph.edges()) {
            arcs.add(new Arc(edge, edge.from(), edge.to()));
            if (!edge.directed()) {
                arcs.add(new Arc(edge, edge.to(), edge.from()));
            }
        }
        return arcs;
    }

    private static String pathEdgeId(Graph graph, String from, String middle, String to) {
        String first = edgeIdForDirection(graph, from, middle);
        if (first != null) {
            return first;
        }
        return edgeIdForDirection(graph, middle, to);
    }

    private static String edgeIdForDirection(Graph graph, String from, String to) {
        if (from.equals(to)) {
            return null;
        }
        return graph.edges().stream()
                .filter(edge -> edge.from().equals(from) && edge.to().equals(to)
                        || !edge.directed() && edge.from().equals(to) && edge.to().equals(from))
                .map(Edge::id)
                .findFirst()
                .orElse(null);
    }

    private static int boundedDistance(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min((long) Integer.MAX_VALUE - 1, value));
    }

    private static Map<String, Edge> edgeById(Graph graph) {
        Map<String, Edge> edgeById = new HashMap<>();
        for (Edge edge : graph.edges()) {
            edgeById.put(edge.id(), edge);
        }
        return edgeById;
    }

    private static String nextVertex(Graph graph, Set<String> visited, Map<String, Integer> values) {
        String best = null;
        int bestValue = INFINITY;
        for (Vertex vertex : graph.vertices()) {
            if (visited.contains(vertex.id())) {
                continue;
            }
            int value = values.get(vertex.id());
            if (best == null || value < bestValue) {
                best = vertex.id();
                bestValue = value;
            }
        }
        return bestValue == INFINITY ? null : best;
    }

    private static String neighbor(Edge edge, String vertex) {
        if (edge.from().equals(vertex)) {
            return edge.to();
        }
        if (edge.to().equals(vertex)) {
            return edge.from();
        }
        return null;
    }

    private static String directedNeighbor(Edge edge, String vertex) {
        if (edge.from().equals(vertex)) {
            return edge.to();
        }
        if (!edge.directed() && edge.to().equals(vertex)) {
            return edge.from();
        }
        return null;
    }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static final class UnionFind {
        private final Map<String, String> parent = new HashMap<>();
        private final Map<String, Integer> rank = new HashMap<>();

        private UnionFind(List<String> vertices) {
            for (String vertex : vertices) {
                parent.put(vertex, vertex);
                rank.put(vertex, 0);
            }
        }

        private boolean union(String left, String right) {
            String leftRoot = find(left);
            String rightRoot = find(right);
            if (leftRoot.equals(rightRoot)) {
                return false;
            }
            int leftRank = rank.get(leftRoot);
            int rightRank = rank.get(rightRoot);
            if (leftRank < rightRank) {
                parent.put(leftRoot, rightRoot);
            } else {
                parent.put(rightRoot, leftRoot);
                if (leftRank == rightRank) {
                    rank.put(leftRoot, leftRank + 1);
                }
            }
            return true;
        }

        private String find(String vertex) {
            String root = parent.get(vertex);
            if (!root.equals(vertex)) {
                root = find(root);
                parent.put(vertex, root);
            }
            return root;
        }
    }

    private record Arc(Edge edge, String from, String to) {
    }
}
