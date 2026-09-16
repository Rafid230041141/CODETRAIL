package application;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import application.algorithm.GraphAlgorithms;
import application.algorithm.GraphAlgorithms.Algorithm;
import application.algorithm.GraphAlgorithms.Edge;
import application.algorithm.GraphAlgorithms.Graph;
import application.algorithm.GraphAlgorithms.Step;
import application.algorithm.GraphAlgorithms.Vertex;
import application.client.simulation.SimulationEngine;
import application.client.simulation.SimulationEngine.Cell;
import application.client.simulation.SimulationEngine.SimulationRun;

public final class AlgorithmVerificationHarness {
    private AlgorithmVerificationHarness() {
    }

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();

        SimulationRun recursion = run(mapper, "RECURSION", "{\"n\":5}");
        check(recursion.steps().size() >= 10, "recursion animates call-stack pushes and returns");

        SimulationRun merge = run(mapper, "SORTING", "{\"algorithm\":\"Merge Sort\",\"values\":[8,3,1,6,2]}");
        check(texts(merge).equals(List.of(1, 2, 3, 6, 8)), "shared sorting view finishes Merge Sort in order");
        SimulationRun quick = run(mapper, "SORTING", "{\"algorithm\":\"Quick Sort\",\"values\":[9,4,7,1,3]}");
        check(texts(quick).equals(List.of(1, 3, 4, 7, 9)), "shared sorting view finishes Quick Sort in order");

        SimulationRun binary = run(mapper, "BINARY_SEARCH", "{\"values\":[1,3,5,7,9],\"target\":7}");
        check(binary.steps().getLast().message().contains("found"), "binary search finds the requested target");
        SimulationRun answer = run(mapper, "BINARY_SEARCH_ANSWER", "{\"limit\":12,\"firstTrue\":7}");
        check(answer.steps().getLast().message().endsWith("7."), "binary search on answer finds the monotonic boundary");

        for (String type : List.of("BFS", "DFS")) {
            SimulationRun traversal = run(mapper, type, "{}");
            check(traversal.steps().getLast().cells().stream()
                    .allMatch(cell -> cell.style().equals("done")), type + " visits every sample vertex");
        }

        for (String type : List.of("SEGMENT_TREE", "FENWICK_TREE", "DSU", "HEAP")) {
            check(run(mapper, type, "{}").steps().size() > 2, type + " produces multiple inspectable states");
        }
        check(texts(run(mapper, "HEAP", "{\"values\":[8,3,6,1,5,2]}")).getFirst() == 1,
                "heap simulation keeps the minimum at its root");
        check(texts(run(mapper, "HEAP", "{\"values\":[8,3,11,1],\"kind\":\"max\"}"))
                .getFirst() == 11, "heap input can select max-heap semantics");

        SimulationRun representation = run(mapper, "GRAPH_REPRESENTATION",
                "{\"vertices\":[\"X\",\"Y\",\"Z\"],\"edges\":[[\"X\",\"Y\"],[\"Y\",\"Z\"]],\"directed\":true}");
        check(representation.steps().getLast().links().size() == 2
                        && representation.steps().getLast().message().contains("X=[Y]"),
                "graph representation uses selected vertices, edges, and direction");
        SimulationRun traversalInput = run(mapper, "BFS",
                "{\"edges\":[[\"X\",\"Y\"],[\"Y\",\"Z\"]],\"directed\":true,\"start\":\"Y\"}");
        check(traversalInput.steps().getLast().cells().stream()
                        .anyMatch(cell -> cell.text().equals("X") && cell.style().equals("default")),
                "BFS honors the selected directed graph and start vertex");
        SimulationRun bellmanInput = run(mapper, "BELLMAN_FORD",
                "{\"weightedEdges\":[[\"A\",\"B\",5],[\"A\",\"C\",1],[\"C\",\"B\",1]],\"start\":\"A\"}");
        check(bellmanInput.steps().getLast().cells().stream()
                        .anyMatch(cell -> cell.text().equals("B\n2")),
                "Bellman-Ford uses selected weighted edges");
        check(run(mapper, "BELLMAN_FORD",
                "{\"weightedEdges\":[[\"A\",\"B\",1],[\"B\",\"A\",-2]],\"start\":\"A\"}")
                        .steps().getLast().message().contains("negative cycle"),
                "Bellman-Ford reports a selected reachable negative cycle");
        check(run(mapper, "BELLMAN_FORD",
                "{\"weightedEdges\":[[\"A\",\"B\",1],[\"C\",\"D\",-2],[\"D\",\"C\",1]],\"start\":\"A\"}")
                        .steps().getLast().message().contains("complete"),
                "Bellman-Ford ignores a negative cycle unreachable from the selected start");
        SimulationRun floydInput = run(mapper, "FLOYD_WARSHALL",
                "{\"vertices\":[\"A\",\"B\",\"C\"],\"matrix\":[[0,8,2],[999,0,1],[999,3,0]]}");
        check(floydInput.steps().getLast().cells().stream()
                        .anyMatch(cell -> cell.text().equals("A→B\n5")),
                "Floyd-Warshall computes from the selected matrix");
        check(run(mapper, "FLOYD_WARSHALL",
                "{\"vertices\":[\"A\",\"B\",\"C\"],\"matrix\":[[0,600,999],[999,0,600],[999,999,0]]}")
                        .steps().getLast().cells().stream()
                        .anyMatch(cell -> cell.text().equals("A→C\n1200")),
                "Floyd-Warshall keeps finite paths larger than the 999 no-edge marker");
        SimulationRun floydCycle = run(mapper, "FLOYD_WARSHALL",
                "{\"vertices\":[\"A\",\"B\",\"C\"],\"matrix\":[[0,1,999],[999,0,-2],[-2,999,0]]}");
        check(floydCycle.steps().getLast().message().contains("negative cycle")
                        && floydCycle.steps().getLast().cells().stream()
                                .anyMatch(cell -> cell.style().equals("active")),
                "Floyd-Warshall reports and highlights a selected negative cycle");
        check(run(mapper, "BRIDGES",
                "{\"vertices\":[\"A\",\"B\",\"C\",\"D\"],\"edges\":[[\"A\",\"B\"],[\"B\",\"C\"],[\"C\",\"A\"],[\"C\",\"D\"]]}")
                        .steps().getLast().message().contains("C-D"),
                "bridge detection uses the selected topology");
        check(run(mapper, "TOPOLOGICAL_SORT",
                "{\"vertices\":[\"X\",\"Y\",\"Z\"],\"edges\":[[\"X\",\"Y\"],[\"Y\",\"Z\"]]}")
                        .steps().getLast().message().contains("[X, Y, Z]"),
                "topological sort uses the selected directed graph");
        check(run(mapper, "SCC",
                "{\"vertices\":[\"X\",\"Y\",\"Z\"],\"edges\":[[\"X\",\"Y\"],[\"Y\",\"X\"],[\"Y\",\"Z\"]]}")
                        .steps().getLast().message().contains("[X, Y]"),
                "Tarjan SCC uses the selected directed graph");
        check(run(mapper, "MAX_FLOW",
                "{\"capacityEdges\":[[\"S\",\"A\",4],[\"A\",\"T\",4],[\"S\",\"T\",1]],\"source\":\"S\",\"sink\":\"T\"}")
                        .steps().getLast().message().contains("max flow is 5"),
                "max-flow uses selected capacities, source, and sink");
        check(run(mapper, "GREEDY", "{\"intervals\":[[4,7],[1,2],[2,3],[3,8]]}")
                        .steps().getLast().message().contains("3 compatible"),
                "greedy activity selection uses and sorts selected intervals");
        check(texts(run(mapper, "LINEAR_ALGEBRA",
                "{\"matrix\":[[2,0],[1,3]],\"vector\":[4,5]}"))
                        .equals(List.of(8, 19)),
                "linear algebra uses the selected matrix and vector");
        check(run(mapper, "GEOMETRY", "{\"a\":[0,0],\"b\":[2,2],\"c\":[4,4]}")
                        .steps().getLast().cells().getFirst().text().equals("collinear"),
                "geometry uses all selected point coordinates");

        Step dijkstraFinal = finalStep(GraphAlgorithms.run(Algorithm.DIJKSTRA, "A"));
        check(dijkstraFinal.values().equals(java.util.Map.of(
                "A", 0,
                "B", 4,
                "C", 3,
                "D", 6,
                "E", 7,
                "F", 9,
                "G", 12)), "Dijkstra final distances are correct");

        Step primFinal = finalStep(GraphAlgorithms.run(Algorithm.PRIM, "A"));
        check(primFinal.selectedEdgeIds().size() == 6 && primFinal.totalWeight() == 14,
                "Prim selects six edges with total weight 14");

        Step kruskalFinal = finalStep(GraphAlgorithms.run(Algorithm.KRUSKAL, "A"));
        check(kruskalFinal.selectedEdgeIds().size() == 6 && kruskalFinal.totalWeight() == 14,
                "Kruskal selects six edges with total weight 14");
        check(!kruskalFinal.rejectedEdgeIds().isEmpty(), "Kruskal rejects a cycle edge");

        Graph customGraph = new Graph(
                List.of(new Vertex("A", 0.1, 0.5), new Vertex("B", 0.8, 0.2), new Vertex("C", 0.8, 0.8)),
                List.of(new Edge("AB", "A", "B", 10), new Edge("AC", "A", "C", 1),
                        new Edge("CB", "C", "B", 2)));
        check(finalStep(GraphAlgorithms.run(customGraph, Algorithm.DIJKSTRA, "A")).values().get("B") == 3,
                "Dijkstra computes the selected custom graph");
        check(finalStep(GraphAlgorithms.run(customGraph, Algorithm.PRIM, "B")).totalWeight() == 3,
                "Prim computes the selected custom graph and start");
        check(finalStep(GraphAlgorithms.run(customGraph, Algorithm.KRUSKAL, "A")).totalWeight() == 3,
                "Kruskal computes the selected custom graph");

        Graph directedShortestPaths = new Graph(
                List.of(new Vertex("A", 0.1, 0.5), new Vertex("B", 0.8, 0.2), new Vertex("C", 0.8, 0.8)),
                List.of(new Edge("A>B", "A", "B", 5, true),
                        new Edge("A>C", "A", "C", 1, true),
                        new Edge("C>B", "C", "B", 1, true)));
        Step bellmanCustom = finalStep(GraphAlgorithms.run(
                directedShortestPaths, Algorithm.BELLMAN_FORD, "A"));
        check(bellmanCustom.values().get("B") == 2 && bellmanCustom.message().contains("complete"),
                "Bellman-Ford computes an editable directed graph");

        Graph reachableNegativeCycle = new Graph(
                directedShortestPaths.vertices(),
                List.of(new Edge("A>B", "A", "B", 1, true),
                        new Edge("B>C", "B", "C", -2, true),
                        new Edge("C>A", "C", "A", 0, true)));
        check(finalStep(GraphAlgorithms.run(reachableNegativeCycle, Algorithm.BELLMAN_FORD, "A"))
                        .message().contains("negative cycle"),
                "Bellman-Ford flags a reachable negative cycle on the custom graph");

        Graph finiteLargePath = new Graph(
                directedShortestPaths.vertices(),
                List.of(new Edge("A>B", "A", "B", 600, true),
                        new Edge("B>C", "B", "C", 600, true)));
        Step floydCustom = finalStep(GraphAlgorithms.run(finiteLargePath, Algorithm.FLOYD_WARSHALL, "A"));
        check(floydCustom.values().get("A->C") == 1200,
                "Floyd-Warshall computes all pairs on an editable directed graph");
        check(finalStep(GraphAlgorithms.run(reachableNegativeCycle, Algorithm.FLOYD_WARSHALL, "A"))
                        .message().contains("Negative cycle"),
                "Floyd-Warshall flags a negative cycle on the custom graph");

        for (String type : List.of("DIJKSTRA", "PRIM", "KRUSKAL")) {
            SimulationRun graph = run(mapper, type, "{}");
            check(graph.steps().size() > 5, type + " exposes the existing graph algorithm trace");
        }

        System.out.println("ALL SIMULATION CHECKS PASSED");
    }

    private static SimulationRun run(ObjectMapper mapper, String type, String json) {
        SimulationRun run = SimulationEngine.build(mapper, type, json);
        check(!run.pseudocode().isEmpty() && !run.steps().isEmpty(), type + " has pseudocode and steps");
        return run;
    }

    private static List<Integer> texts(SimulationRun run) {
        return run.steps().getLast().cells().stream()
                .map(Cell::text)
                .map(Integer::valueOf)
                .toList();
    }

    private static Step finalStep(List<Step> steps) {
        if (steps.isEmpty()) {
            throw new AssertionError("FAILED: graph algorithm returned no snapshots");
        }
        return steps.getLast();
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("FAILED: " + description);
        }
        System.out.println("PASS: " + description);
    }
}
