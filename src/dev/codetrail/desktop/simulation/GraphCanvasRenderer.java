package dev.codetrail.desktop.simulation;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Geometry renderer for graph states. */
public final class GraphCanvasRenderer implements SimulationRenderer {
    static final double NODE_RADIUS = 24.0;
    @Override
    public RendererFamily family() { return RendererFamily.GRAPH; }

    @Override
    public void render(GraphicsContext graphics, SimulationSnapshot snapshot, LayoutFrame frame) {
        GraphState state = requireState(snapshot);
        RenderSupport.clearAndFrame(graphics, frame);
        Map<String, String> facts = factsByKey(state.facts());
        if (facts.containsKey("suffix-links")) renderSuffixAutomaton(graphics, snapshot, state, facts, frame);
        else renderSearchGraph(graphics, snapshot, state, facts, frame);
    }

    /** Input vertices stay in fixed slots while badges and algorithm structures change. */
    private static void renderSearchGraph(GraphicsContext graphics, SimulationSnapshot snapshot,
                                           GraphState state, Map<String, String> facts, LayoutFrame frame) {
        final double radius = 24.0;
        boolean weighted = facts.containsKey("priority-queue");
        double auxiliaryHeight = 80.0;
        double auxiliaryTop = Math.max(frame.contentY(), frame.height() - frame.bottom() - auxiliaryHeight);
        double graphBottom = Math.min(frame.height() - frame.top() - 1.0,
                Math.max(frame.bottom(), frame.height() - auxiliaryTop + 10.0 + (facts.containsKey("low") ? 18.0 : 0.0)));
        LayoutFrame graphFrame = new LayoutFrame(frame.width(), frame.height(), frame.left(), frame.top(),
                frame.right(), graphBottom);
        Map<String, double[]> positions = state.nodeCoordinates().isEmpty()
                ? searchGraphPositions(state, facts, graphFrame) : coordinatePositions(state.nodeCoordinates(), graphFrame);
        Map<String, String> distances = distanceLabels(state.facts());
        boolean flow = facts.containsKey("maxFlow");
        List<String> hullTestNodes = new ArrayList<>();
        if (facts.containsKey("hull-size") && meaningful(facts.get("orientation-points"))) {
            for (String label : facts.get("orientation-points").split(",\\s*")) {
                state.nodes().stream().filter(node -> primaryNodeLabel(node).equals(label)).findFirst()
                        .ifPresent(node -> hullTestNodes.add(node.id()));
            }
        }
        boolean hullTesting = hullTestNodes.size() == 3;
        String rejectedHullNode = hullTesting && List.of("clockwise: pop", "collinear: pop").contains(facts.get("orientation"))
                ? hullTestNodes.get(1) : "";
        Map<String, List<String>> badges = graphBadges(state, facts, distances);
        List<LabelBox> occupied = new ArrayList<>();
        for (Node node : state.nodes()) {
            double[] point = positions.get(node.id());
            occupied.add(new LabelBox(point[0] - radius - 3.0, point[1] - radius - 3.0,
                    radius * 2.0 + 6.0, radius * 2.0 + 6.0));
            List<String> nodeBadges = badges.getOrDefault(node.id(), List.of());
            double badgeWidth = nodeBadges.stream().mapToDouble(GraphCanvasRenderer::secondaryTextWidth).max().orElse(0.0) + 14.0;
            occupied.add(nodeBadgeBox(node.id(), point, badgeWidth, nodeBadges.size() * 22.0,
                    true, state, positions, badges, graphFrame));
        }
        List<GraphLabel> weights = new ArrayList<>();
        List<Double> routeOffsets = edgeRouteOffsets(state.edges());
        List<RenderSupport.EdgeSegment> labelRoutes = new ArrayList<>();
        for (int index = 0; index < state.edges().size(); index++) {
            Edge edge = state.edges().get(index);
            if (edge.fromNodeId().equals(edge.toNodeId())) continue;
            double[] from = positions.get(edge.fromNodeId());
            double[] to = positions.get(edge.toNodeId());
            labelRoutes.add(RenderSupport.clippedCircleSegment(from[0], from[1], to[0], to[1],
                    radius, radius, routeOffsets.get(index)));
        }
        for (int index = 0; index < state.edges().size(); index++) {
            Edge edge = state.edges().get(index);
            double[] from = positions.get(edge.fromNodeId());
            double[] to = positions.get(edge.toNodeId());
            if (facts.containsKey("component-sizes")) {
                javafx.geometry.Point2D target = RenderSupport.movingPoint(graphics, "parent:" + edge.id(), to[0], to[1]);
                to = new double[] {target.getX(), target.getY()};
            }
            boolean reverse = flow && edge.id().equals(facts.get("residual-edge"))
                    && "reverse".equals(facts.get("residual-direction"));
            boolean active = !reverse && snapshot.activeEdgeIds().contains(edge.id());
            if (hullTesting) active = testedHullEdge(edge, hullTestNodes);
            Color color = RenderSupport.edgeColor(graphics, snapshot, reverse ? SnapshotStatus.DEFAULT
                    : active ? SnapshotStatus.ACTIVE : hullTesting ? SnapshotStatus.DEFAULT : edge.status());
            if (edge.fromNodeId().equals(edge.toNodeId())) {
                RenderSupport.selfLoop(graphics, from[0], from[1], radius, color, state.directed());
                edge.label().filter(label -> !isEndpointLabel(edge, label)).ifPresent(label ->
                        weights.add(placeLabel(List.of(flow ? flowRatio(label) : label),
                                List.of(new double[] {from[0] + 50.0, from[1] - 30.0}), from[0], from[1],
                                color, graphFrame, occupied)));
                continue;
            }
            RenderSupport.EdgeSegment segment = RenderSupport.clippedCircleSegment(
                    from[0], from[1], to[0], to[1], radius, radius, routeOffsets.get(index));
            if (state.directed()) RenderSupport.directedSegment(graphics, segment, color, active ? 3.5 : 2.0);
            else RenderSupport.line(graphics, segment.startX(), segment.startY(), segment.endX(), segment.endY(),
                    color, active ? 3.5 : 2.0);
            edge.label().ifPresent(label -> {
                String value = flow ? flowRatio(label) : label;
                if (!facts.containsKey("hull-size") && !facts.containsKey("component-sizes")
                        && !isEndpointLabel(edge, label)) {
                    weights.add(placeEdgeLabel(List.of(value), segment, color, graphFrame, occupied, labelRoutes));
                }
            });
        }
        if (flow) drawReverseResidual(graphics, snapshot, facts, positions, graphFrame, occupied, weights, labelRoutes);
        for (GraphLabel weight : weights) drawLabel(graphics, weight);
        java.util.Set<String> drawnCoordinates = new java.util.HashSet<>();
        for (Node node : state.nodes()) {
            double[] point = positions.get(node.id());
            NodeCoordinate coordinate = state.nodeCoordinates().get(node.id());
            if (coordinate != null && !drawnCoordinates.add(coordinate.x() + ":" + coordinate.y())) continue;
            SnapshotStatus status = snapshot.activeNodeIds().contains(node.id()) ? SnapshotStatus.ACTIVE : node.status();
            if (coordinate != null && state.nodes().stream().anyMatch(other ->
                    sameCoordinate(coordinate, state.nodeCoordinates().get(other.id()))
                            && snapshot.activeNodeIds().contains(other.id()))) status = SnapshotStatus.ACTIVE;
            boolean rejectedHullPoint = coordinate != null && !rejectedHullNode.isEmpty()
                    && sameCoordinate(coordinate, state.nodeCoordinates().get(rejectedHullNode));
            if (hullTesting) {
                status = hullTestNodes.stream().anyMatch(id -> sameCoordinate(coordinate, state.nodeCoordinates().get(id)))
                        ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            }
            RenderSupport.nodeCircle(graphics, point[0], point[1], radius,
                    RenderSupport.color(graphics, snapshot, status), primaryNodeLabel(node));
            if (rejectedHullPoint) {
                Color rejected = RenderSupport.palette(graphics).edge(SnapshotStatus.REJECTED);
                graphics.setStroke(rejected);
                graphics.setLineWidth(3.0);
                graphics.strokeOval(point[0] - radius - 3.0, point[1] - radius - 3.0,
                        radius * 2.0 + 6.0, radius * 2.0 + 6.0);
                double crossX = point[0] + radius + 5.0;
                double crossY = point[1] - radius - 5.0;
                RenderSupport.line(graphics, crossX - 4.0, crossY - 4.0, crossX + 4.0, crossY + 4.0, rejected, 3.0);
                RenderSupport.line(graphics, crossX - 4.0, crossY + 4.0, crossX + 4.0, crossY - 4.0, rejected, 3.0);
            }
            if (node.id().equals(facts.get("source")) || integerMap(facts.get("component-sizes")).containsKey(node.id())) {
                graphics.setStroke(RenderSupport.palette(graphics).outline());
                graphics.setLineWidth(1.0);
                graphics.strokeOval(point[0] - radius + 4.0, point[1] - radius + 4.0,
                        radius * 2.0 - 8.0, radius * 2.0 - 8.0);
            }
            List<String> nodeBadges = badges.getOrDefault(node.id(), List.of());
            double badgeWidth = nodeBadges.stream().mapToDouble(GraphCanvasRenderer::secondaryTextWidth).max().orElse(0.0) + 14.0;
            LabelBox badgeBox = nodeBadgeBox(node.id(), point, badgeWidth, nodeBadges.size() * 22.0,
                    true, state, positions, badges, graphFrame);
            for (int row = 0; row < nodeBadges.size(); row++) {
                String badge = nodeBadges.get(row);
                graphics.setFill(RenderSupport.palette(graphics).background());
                graphics.fillRoundRect(badgeBox.left(), badgeBox.top() + row * 22.0,
                        badgeBox.width(), 22.0, 6.0, 6.0);
                graphics.setStroke(RenderSupport.palette(graphics).outline());
                graphics.setLineWidth(1.0);
                graphics.strokeRoundRect(badgeBox.left(), badgeBox.top() + row * 22.0,
                        badgeBox.width(), 22.0, 6.0, 6.0);
                secondaryLabel(graphics, badge, badgeBox.centerX() - secondaryTextWidth(badge) / 2.0,
                        badgeBox.top() + 17.0 + row * 22.0, RenderSupport.palette(graphics).text());
            }
        }
        if (facts.containsKey("priority-queue")
                || facts.containsKey("queue") && facts.containsKey("distance") && !flow) {
            drawSearchFrontier(graphics, facts, frame, auxiliaryTop, weighted);
        } else {
            drawGraphAuxiliary(graphics, state, facts, frame, auxiliaryTop);
        }
    }

    private static LabelBox nodeBadgeBox(String nodeId, double[] point, double width, double height,
                                          boolean avoidIncidentEdges, GraphState state,
                                          Map<String, double[]> positions, Map<String, List<String>> badges,
                                          LayoutFrame frame) {
        if (height == 0.0) {
            return new LabelBox(point[0] - width / 2.0, point[1] + NODE_RADIUS + 4.0, width, height);
        }
        List<RenderSupport.EdgeSegment> incident = new ArrayList<>();
        List<Double> offsets = edgeRouteOffsets(state.edges());
        for (int index = 0; index < state.edges().size(); index++) {
            Edge edge = state.edges().get(index);
            if (edge.fromNodeId().equals(edge.toNodeId())
                    || !edge.fromNodeId().equals(nodeId) && !edge.toNodeId().equals(nodeId)) continue;
            double[] from = positions.get(edge.fromNodeId());
            double[] to = positions.get(edge.toNodeId());
            if (from != null && to != null) {
                incident.add(RenderSupport.clippedCircleSegment(from[0], from[1], to[0], to[1],
                        NODE_RADIUS, NODE_RADIUS, offsets.get(index)));
            }
        }

        // Expanded choices covering all sides with safe clearance
        List<LabelBox> choices = List.of(
                new LabelBox(point[0] - width / 2.0, point[1] - NODE_RADIUS - height - 6.0, width, height), // above
                new LabelBox(point[0] - width / 2.0, point[1] + NODE_RADIUS + 6.0, width, height),          // below
                new LabelBox(point[0] - NODE_RADIUS - width - 8.0, point[1] - height / 2.0, width, height), // left
                new LabelBox(point[0] + NODE_RADIUS + 8.0, point[1] - height / 2.0, width, height),          // right
                new LabelBox(point[0] - width - 8.0, point[1] - NODE_RADIUS - height, width, height),       // above-left
                new LabelBox(point[0] + 8.0, point[1] - NODE_RADIUS - height, width, height),               // above-right
                new LabelBox(point[0] - width - 8.0, point[1] + NODE_RADIUS + 4.0, width, height),          // below-left
                new LabelBox(point[0] + 8.0, point[1] + NODE_RADIUS + 4.0, width, height)                   // below-right
        );

        LabelBox best = choices.get(1);
        double bestScore = Double.POSITIVE_INFINITY;
        for (LabelBox box : choices) {
            double outside = Math.max(0.0, frame.contentX() - box.left())
                    + Math.max(0.0, frame.contentY() - box.top())
                    + Math.max(0.0, box.right() - (frame.contentX() + frame.contentWidth()))
                    + Math.max(0.0, box.bottom() - (frame.contentY() + frame.contentHeight()));
            double score = outside * 50000.0;

            for (RenderSupport.EdgeSegment segment : incident) {
                if (segmentIntersectsBox(segment, box, 6.0)) {
                    score += 25000.0;
                }
                double dist = segmentDistance(box.centerX(), box.centerY(), segment);
                double safeDist = Math.hypot(width, height) / 2.0 + 16.0;
                if (dist < safeDist) {
                    score += (safeDist - dist) * 250.0;
                }
            }

            for (Map.Entry<String, double[]> entry : positions.entrySet()) {
                if (entry.getKey().equals(nodeId)) continue;
                double[] other = entry.getValue();
                score += box.overlapArea(new LabelBox(other[0] - NODE_RADIUS - 4.0, other[1] - NODE_RADIUS - 4.0,
                        NODE_RADIUS * 2.0 + 8.0, NODE_RADIUS * 2.0 + 8.0)) * 30000.0;
            }

            if (score < bestScore) {
                best = box;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean segmentIntersectsBox(RenderSupport.EdgeSegment segment, LabelBox box, double padding) {
        double first = 0.0;
        double last = 1.0;
        double[] starts = {segment.startX(), segment.startY()};
        double[] deltas = {segment.endX() - segment.startX(), segment.endY() - segment.startY()};
        double[] minimums = {box.left() - padding, box.top() - padding};
        double[] maximums = {box.right() + padding, box.bottom() + padding};
        for (int axis = 0; axis < 2; axis++) {
            if (Math.abs(deltas[axis]) < 1.0e-9) {
                if (starts[axis] < minimums[axis] || starts[axis] > maximums[axis]) return false;
                continue;
            }
            double enter = (minimums[axis] - starts[axis]) / deltas[axis];
            double leave = (maximums[axis] - starts[axis]) / deltas[axis];
            first = Math.max(first, Math.min(enter, leave));
            last = Math.min(last, Math.max(enter, leave));
            if (first > last) return false;
        }
        return true;
    }

    public static Map<String, double[]> searchGraphPositions(List<Node> nodes, LayoutFrame frame) {
        return searchGraphPositions(new GraphState(nodes, List.of(), false), Map.of(), frame);
    }

    public static Map<String, double[]> searchGraphPositions(GraphState state, Map<String, String> facts, LayoutFrame frame) {
        List<Node> nodes = state.nodes();
        if (nodes.isEmpty()) return new LinkedHashMap<>();
        if (nodes.size() == 1) {
            Map<String, double[]> single = new LinkedHashMap<>();
            single.put(nodes.get(0).id(), new double[] {frame.centerX(), frame.contentY() + frame.contentHeight() / 2.0});
            return single;
        }

        boolean isFlow = facts.containsKey("maxFlow") || facts.containsKey("sink");
        boolean isTopo = facts.containsKey("indegree") || facts.containsKey("order") || facts.containsKey("ready");
        boolean hasSource = facts.containsKey("source");

        Map<String, double[]> layered = tryLayeredLayout(state, facts, frame, isFlow, isTopo, hasSource);
        if (layered != null) {
            return layered;
        }

        return expansiveCircularPositions(state, frame);
    }

    private static Map<String, double[]> tryLayeredLayout(GraphState state, Map<String, String> facts,
                                                          LayoutFrame frame, boolean isFlow,
                                                          boolean isTopo, boolean hasSource) {
        List<Node> nodes = state.nodes();
        int n = nodes.size();
        Map<String, Integer> nodeIndex = new HashMap<>();
        for (int i = 0; i < n; i++) nodeIndex.put(nodes.get(i).id(), i);

        List<List<Integer>> forwardAdj = new ArrayList<>(n);
        List<List<Integer>> undirectedAdj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            forwardAdj.add(new ArrayList<>());
            undirectedAdj.add(new ArrayList<>());
        }
        for (Edge edge : state.edges()) {
            Integer u = nodeIndex.get(edge.fromNodeId());
            Integer v = nodeIndex.get(edge.toNodeId());
            if (u != null && v != null && !u.equals(v)) {
                forwardAdj.get(u).add(v);
                undirectedAdj.get(u).add(v);
                undirectedAdj.get(v).add(u);
            }
        }

        int[] layers = new int[n];
        Arrays.fill(layers, -1);

        if (isFlow) {
            String sourceStr = facts.getOrDefault("source", nodes.get(0).id());
            String sinkStr = facts.getOrDefault("sink", nodes.get(n - 1).id());
            int sourceIdx = nodeIndex.getOrDefault(sourceStr, 0);
            int sinkIdx = nodeIndex.getOrDefault(sinkStr, n - 1);

            int[] distFromSource = new int[n];
            Arrays.fill(distFromSource, -1);
            Deque<Integer> q = new ArrayDeque<>();
            distFromSource[sourceIdx] = 0;
            q.add(sourceIdx);
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : forwardAdj.get(u)) {
                    if (distFromSource[v] < 0) {
                        distFromSource[v] = distFromSource[u] + 1;
                        q.add(v);
                    }
                }
            }

            int maxIntermediate = 1;
            for (int i = 0; i < n; i++) {
                if (i != sourceIdx && i != sinkIdx && distFromSource[i] > 0) {
                    maxIntermediate = Math.max(maxIntermediate, distFromSource[i]);
                }
            }
            int sinkLayer = Math.max(2, maxIntermediate + 1);

            for (int i = 0; i < n; i++) {
                if (i == sourceIdx) layers[i] = 0;
                else if (i == sinkIdx) layers[i] = sinkLayer;
                else if (distFromSource[i] > 0) layers[i] = Math.min(distFromSource[i], sinkLayer - 1);
                else layers[i] = 1;
            }
        } else if (isTopo) {
            int[] inDeg = new int[n];
            for (int u = 0; u < n; u++) {
                for (int v : forwardAdj.get(u)) inDeg[v]++;
            }
            Deque<Integer> q = new ArrayDeque<>();
            for (int i = 0; i < n; i++) {
                if (inDeg[i] == 0) {
                    layers[i] = 0;
                    q.add(i);
                }
            }
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : forwardAdj.get(u)) {
                    layers[v] = Math.max(layers[v], layers[u] + 1);
                    inDeg[v]--;
                    if (inDeg[v] == 0) q.add(v);
                }
            }
            int maxL = 0;
            for (int l : layers) maxL = Math.max(maxL, l);
            for (int i = 0; i < n; i++) {
                if (layers[i] < 0) layers[i] = maxL + 1;
            }
        } else {
            int startIdx = 0;
            if (hasSource) {
                String sourceStr = facts.get("source");
                if (sourceStr != null && nodeIndex.containsKey(sourceStr)) {
                    startIdx = nodeIndex.get(sourceStr);
                }
            }
            List<List<Integer>> adj = state.directed() ? forwardAdj : undirectedAdj;
            Deque<Integer> q = new ArrayDeque<>();
            layers[startIdx] = 0;
            q.add(startIdx);
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : adj.get(u)) {
                    if (layers[v] < 0) {
                        layers[v] = layers[u] + 1;
                        q.add(v);
                    }
                }
            }
            int maxL = 0;
            for (int l : layers) maxL = Math.max(maxL, l);
            for (int i = 0; i < n; i++) {
                if (layers[i] < 0) {
                    layers[i] = maxL + 1;
                    maxL++;
                }
            }
        }

        int numLayers = 0;
        for (int l : layers) numLayers = Math.max(numLayers, l + 1);
        if (numLayers < 2) return null;

        Map<Integer, List<Integer>> layerGroups = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            layerGroups.computeIfAbsent(layers[i], ignored -> new ArrayList<>()).add(i);
        }

        int maxInLayer = 0;
        for (List<Integer> grp : layerGroups.values()) {
            maxInLayer = Math.max(maxInLayer, grp.size());
        }
        if (maxInLayer > 5 && !isFlow && !isTopo) {
            return null;
        }

        // Barycenter heuristic for crossing minimization
        for (int l = 1; l < numLayers; l++) {
            List<Integer> currentLayer = layerGroups.get(l);
            if (currentLayer == null || currentLayer.size() <= 1) continue;
            List<Integer> prevLayer = layerGroups.get(l - 1);
            if (prevLayer == null) continue;
            Map<Integer, Double> barycenters = new HashMap<>();
            for (int u : currentLayer) {
                double sum = 0.0;
                int count = 0;
                for (int p = 0; p < prevLayer.size(); p++) {
                    int v = prevLayer.get(p);
                    if (undirectedAdj.get(u).contains(v)) {
                        sum += p;
                        count++;
                    }
                }
                barycenters.put(u, count > 0 ? (sum / count) : (double) u);
            }
            currentLayer.sort(Comparator.comparingDouble(barycenters::get));
        }

        boolean hasLongSpan = false;
        for (Edge edge : state.edges()) {
            Integer u = nodeIndex.get(edge.fromNodeId());
            Integer v = nodeIndex.get(edge.toNodeId());
            if (u != null && v != null && Math.abs(layers[u] - layers[v]) >= 2) {
                hasLongSpan = true;
                break;
            }
        }

        double left = frame.contentX() + 90.0;
        double right = frame.contentX() + frame.contentWidth() - 90.0;
        double layerSpacingX = Math.max(1.0, right - left) / Math.max(1, numLayers - 1);
        double centerY = frame.contentY() + Math.max(1.0, frame.contentHeight() - 28.0) / 2.0;
        double availableHeight = Math.max(1.0, frame.contentHeight() - 110.0);

        Map<String, double[]> positions = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : layerGroups.entrySet()) {
            int l = entry.getKey();
            List<Integer> inLayer = entry.getValue();
            int m = inLayer.size();
            double x = left + l * layerSpacingX;
            double spacingY = Math.min(180.0, Math.max(80.0, availableHeight / Math.max(1, m)));
            for (int i = 0; i < m; i++) {
                int nodeIdx = inLayer.get(i);
                double y;
                if (m == 1 && hasLongSpan && numLayers >= 3) {
                    y = centerY + (l % 2 == 1 ? -68.0 : 48.0);
                } else {
                    y = centerY + (i - (m - 1) / 2.0) * spacingY;
                }
                positions.put(nodes.get(nodeIdx).id(), new double[] {x, y});
            }
        }

        // Collision clearance: ensure no edge cuts through any node circle
        for (int iter = 0; iter < 5; iter++) {
            boolean adjusted = false;
            for (Edge edge : state.edges()) {
                double[] p1 = positions.get(edge.fromNodeId());
                double[] p2 = positions.get(edge.toNodeId());
                if (p1 == null || p2 == null) continue;
                for (Node node : nodes) {
                    if (node.id().equals(edge.fromNodeId()) || node.id().equals(edge.toNodeId())) continue;
                    double[] np = positions.get(node.id());
                    if (np == null) continue;
                    double d = pointToSegmentDistance(np[0], np[1], p1[0], p1[1], p2[0], p2[1]);
                    if (d < NODE_RADIUS + 16.0) {
                        double shift = (np[1] >= (p1[1] + p2[1]) / 2.0) ? 36.0 : -36.0;
                        np[1] = Math.max(frame.contentY() + 40.0,
                                Math.min(frame.contentY() + frame.contentHeight() - 40.0, np[1] + shift));
                        adjusted = true;
                    }
                }
            }
            if (!adjusted) break;
        }

        return positions;
    }

    private static double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;
        if (lenSq < 1.0e-9) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0.0, Math.min(1.0, ((px - x1) * dx + (py - y1) * dy) / lenSq));
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        return Math.hypot(px - projX, py - projY);
    }

    private static Map<String, double[]> expansiveCircularPositions(GraphState state, LayoutFrame frame) {
        List<Node> nodes = state.nodes();
        Map<String, double[]> positions = new LinkedHashMap<>();
        if (nodes.isEmpty()) return positions;
        double centerX = frame.centerX();
        double centerY = frame.contentY() + Math.max(1.0, frame.contentHeight() - 28.0) / 2.0;
        double radiusX = Math.max(1.0, Math.min(420.0, (frame.contentWidth() - 140.0) / 2.0));
        double radiusY = Math.max(1.0, Math.min(210.0, (frame.contentHeight() - 110.0) / 2.0));

        List<Integer> order = new ArrayList<>();
        boolean[] seen = new boolean[nodes.size()];
        for (int start = 0; start < nodes.size(); start++) {
            if (seen[start]) continue;
            Deque<Integer> q = new ArrayDeque<>();
            q.add(start);
            seen[start] = true;
            while (!q.isEmpty()) {
                int u = q.poll();
                order.add(u);
                for (Edge edge : state.edges()) {
                    if (edge.fromNodeId().equals(nodes.get(u).id())) {
                        for (int v = 0; v < nodes.size(); v++) {
                            if (nodes.get(v).id().equals(edge.toNodeId()) && !seen[v]) {
                                seen[v] = true;
                                q.add(v);
                            }
                        }
                    }
                }
            }
        }
        for (int rank = 0; rank < order.size(); rank++) {
            int nodeIdx = order.get(rank);
            double angle = -Math.PI / 2.0 + 2.0 * Math.PI * rank / nodes.size();
            positions.put(nodes.get(nodeIdx).id(), nodes.size() == 1
                    ? new double[] {centerX, centerY}
                    : new double[] {centerX + radiusX * Math.cos(angle), centerY + radiusY * Math.sin(angle)});
        }
        return positions;
    }

    private static void drawSearchFrontier(GraphicsContext graphics, Map<String, String> facts,
                                           LayoutFrame frame, double top, boolean weighted) {
        List<String> entries = orderedValues(facts.get(weighted ? "priority-queue" : "queue"));
        double labelWidth = weighted ? 114.0 : 84.0;
        double left = frame.contentX() + labelWidth;
        double cellWidth = weighted ? 66.0 : 40.0;
        double gap = 8.0;
        double cellHeight = weighted ? 50.0 : 36.0;
        RenderSupport.label(graphics, weighted ? "Min queue" : "Queue", frame.contentX(), top + 23.0,
                RenderSupport.palette(graphics).text());
        if (entries.isEmpty()) {
            secondaryLabel(graphics, "empty", left, top + 23.0, RenderSupport.palette(graphics).secondaryText());
        } else {
            int capacity = Math.max(1, (int) ((frame.contentWidth() - labelWidth - 56.0) / (cellWidth + gap)));
            int visible = Math.min(capacity, entries.size());
            for (int index = 0; index < visible; index++) {
                double targetX = left + index * (cellWidth + gap);
                javafx.geometry.Point2D cell = RenderSupport.movingPoint(graphics,
                        (weighted ? "pq:" : "queue:") + entries.get(index), targetX, top);
                double x = cell.getX();
                graphics.setFill(index == 0 ? RenderSupport.palette(graphics).fill(SnapshotStatus.ACTIVE)
                        : RenderSupport.palette(graphics).panel());
                graphics.fillRoundRect(x, top, cellWidth, cellHeight, 6.0, 6.0);
                Color text = index == 0 ? RenderSupport.palette(graphics).nodeText() : RenderSupport.palette(graphics).text();
                String[] pair = weighted ? entries.get(index).split(":", 2) : new String[] {entries.get(index)};
                RenderSupport.centeredLabel(graphics, pair[0], x + cellWidth / 2.0, top + 24.0, text);
                if (pair.length == 2) {
                    String distance = "d=" + pair[1];
                    secondaryLabel(graphics, distance, x + (cellWidth - secondaryTextWidth(distance)) / 2.0,
                            top + 44.0, text);
                }
            }
            secondaryLabel(graphics, "front", left, top + cellHeight + 20.0, RenderSupport.palette(graphics).secondaryText());
            if (visible < entries.size()) secondaryLabel(graphics, "+" + (entries.size() - visible),
                    left + visible * (cellWidth + gap), top + 24.0, RenderSupport.palette(graphics).secondaryText());
        }
        if (!weighted) {
            List<String> order = orderedValues(facts.get("visited"));
            String orderText = "Order  " + String.join("  ", order);
            double x = entries.isEmpty() ? left : left + 84.0;
            if (secondaryTextWidth(orderText) <= frame.contentX() + frame.contentWidth() - x) {
                secondaryLabel(graphics, orderText, x, top + 59.0, RenderSupport.palette(graphics).secondaryText());
            }
        }
    }

    private static void renderSuffixAutomaton(GraphicsContext g, SimulationSnapshot snapshot, GraphState state,
                                               Map<String, String> facts, LayoutFrame frame) {
        final double halfWidth = 46.0;
        final double halfHeight = 30.0;
        String input = facts.getOrDefault("text", "");
        if ("(empty)".equals(input)) input = "";
        Map<String, String> lengths = integerMap(facts.get("lengths"));
        Map<String, String> links = integerMap(facts.get("suffix-links"));
        int columns = Math.max(1, (int) (frame.contentWidth() / 108.0));
        // Keep the input/index strip and each row's transition route band separate.
        double firstY = frame.contentY() + 124.0;
        double rowStride = 112.0;
        double witnessTop = frame.height() - frame.bottom() - 58.0;
        int rowCapacity = Math.max(1, (int) ((witnessTop - firstY - halfHeight) / rowStride) + 1);
        int capacity = Math.max(2, columns * rowCapacity);
        List<String> visibleIds = visibleAutomatonIds(snapshot, state, facts, capacity);
        Map<String, double[]> positions = new LinkedHashMap<>();
        double spacing = Math.min(126.0, frame.contentWidth() / columns);
        double originX = frame.centerX() - (columns - 1) * spacing / 2.0;
        for (int slot = 0; slot < visibleIds.size(); slot++) {
            positions.put(visibleIds.get(slot), new double[] {
                    originX + slot % columns * spacing, firstY + slot / columns * rowStride});
        }
        drawAutomatonInput(g, input, facts, frame);
        int hiddenCount = state.nodes().size() - visibleIds.size();
        if (hiddenCount > 0) secondaryLabel(g, "+" + hiddenCount + " states", frame.contentX(),
                frame.height() - 12.0, RenderSupport.palette(g).secondaryText());
        List<LabelBox> occupied = new ArrayList<>();
        for (double[] point : positions.values()) occupied.add(new LabelBox(point[0] - halfWidth - 3.0,
                point[1] - halfHeight - 3.0, halfWidth * 2.0 + 6.0, halfHeight * 2.0 + 6.0));
        List<GraphLabel> labels = new ArrayList<>();
        for (Edge edge : state.edges()) {
            double[] from = positions.get(edge.fromNodeId());
            double[] target = positions.get(edge.toNodeId());
            if (from == null || target == null) continue;
            boolean suffix = edge.id().startsWith("s-q") && edge.label().filter("link"::equals).isPresent();
            boolean active = snapshot.activeEdgeIds().contains(edge.id());
            javafx.geometry.Point2D movedTarget = RenderSupport.movingPoint(g, "sam-target:" + edge.id(), target[0], target[1]);
            if (suffix && !active) continue;
            double[] to = new double[] {movedTarget.getX(), movedTarget.getY()};
            Color color = RenderSupport.edgeColor(g, snapshot, active ? SnapshotStatus.ACTIVE : edge.status());
            if (suffix) g.setLineDashes(6.0, 4.0);
            if (Math.abs(from[1] - to[1]) < 1.0 && Math.abs(from[0] - to[0]) > spacing * 1.5) {
                double sign = Math.signum(to[0] - from[0]);
                double lane = from[1] - halfHeight - (suffix ? 12.0 : 20.0);
                double startX = from[0] + sign * halfWidth;
                double endX = to[0] - sign * halfWidth;
                g.setStroke(color);
                g.setLineWidth(active ? 3.0 : 1.8);
                g.beginPath();
                g.moveTo(startX, from[1]);
                g.lineTo(startX + sign * 8.0, from[1]);
                g.lineTo(startX + sign * 8.0, lane);
                g.lineTo(endX - sign * 8.0, lane);
                g.lineTo(endX - sign * 8.0, to[1]);
                g.lineTo(endX, to[1]);
                g.stroke();
                directedTip(g, endX, to[1], sign, 0.0, color);
                if (!suffix) edge.label().ifPresent(text -> {
                    labels.add(placeLabel(List.of(text),
                            List.of(new double[] {(startX + endX) / 2.0, lane}),
                            (startX + endX) / 2.0, lane, color, frame, occupied));
                });
            } else {
                RenderSupport.EdgeSegment segment = RenderSupport.clippedBoxSegment(from[0], from[1], to[0], to[1],
                        halfWidth, halfHeight, halfWidth, halfHeight, suffix ? 9.0 : -4.0);
                RenderSupport.directedSegment(g, segment, color, active ? 3.0 : 1.8);
                if (!suffix) edge.label().ifPresent(text -> labels.add(placeLabel(List.of(text), edgeLabelCandidates(segment),
                        segment.midX(), segment.midY(), color, frame, occupied)));
            }
            g.setLineDashes();
        }
        for (GraphLabel label : labels) drawLabel(g, label);
        for (Node node : state.nodes()) {
            double[] point = positions.get(node.id());
            if (point == null) continue;
            boolean clone = node.id().equals(facts.get("clone-state"));
            SnapshotStatus status = snapshot.activeNodeIds().contains(node.id()) ? SnapshotStatus.ACTIVE : node.status();
            g.setFill(RenderSupport.color(g, snapshot, status));
            g.fillRoundRect(point[0] - halfWidth, point[1] - halfHeight, halfWidth * 2.0, halfHeight * 2.0, 8.0, 8.0);
            g.setStroke(RenderSupport.palette(g).outline());
            g.setLineWidth(clone ? 2.5 : 1.0);
            g.strokeRoundRect(point[0] - halfWidth, point[1] - halfHeight, halfWidth * 2.0, halfHeight * 2.0, 8.0, 8.0);
            RenderSupport.centeredLabel(g, node.id() + (clone ? " *" : ""), point[0], point[1] - 11.0,
                    RenderSupport.palette(g).nodeText());
            String length = "len " + lengths.getOrDefault(node.id(), "?");
            secondaryLabel(g, length, point[0] - secondaryTextWidth(length) / 2.0, point[1] + 7.0,
                    RenderSupport.palette(g).nodeText());
            String target = links.get(node.id());
            String link = meaningful(target) && !"-1".equals(target) ? "↳ " + target : "root";
            secondaryLabel(g, link, point[0] - secondaryTextWidth(link) / 2.0, point[1] + 25.0,
                    RenderSupport.palette(g).nodeText());
        }
        if (meaningful(facts.get("clone-state"))) {
            String shortest = facts.getOrDefault("clone-shortest", "?");
            String longest = facts.getOrDefault("clone-longest", "?");
            String cloneWord = shortest.equals(longest) ? shortest : shortest + "…" + longest;
            auxiliaryText(g, facts.get("clone-source") + "  “" + facts.getOrDefault("clone-source-substring", "?")
                    + "”  ends " + facts.getOrDefault("clone-source-end-positions", "?"), frame, witnessTop + 18.0);
            auxiliaryText(g, facts.get("clone-state") + " *  “" + cloneWord + "”  ends "
                    + facts.getOrDefault("clone-end-positions", "?"), frame, witnessTop + 44.0);
        } else {
            auxiliaryText(g, "↳ suffix link", frame, witnessTop + 36.0);
        }
    }

    /** Replace nonessential window slots instead of hiding the current operation's endpoints. */
    static List<String> visibleAutomatonIds(SimulationSnapshot snapshot, GraphState state,
                                             Map<String, String> facts, int capacity) {
        List<String> allIds = state.nodes().stream().map(Node::id).toList();
        if (allIds.size() <= capacity) return allIds;
        java.util.Set<String> required = new java.util.LinkedHashSet<>();
        for (String key : List.of("current-state", "clone-state", "clone-source", "parent-state", "existing-state")) {
            String id = facts.get(key);
            if (allIds.contains(id)) required.add(id);
        }
        for (Edge edge : state.edges()) {
            if (snapshot.activeEdgeIds().contains(edge.id())) {
                required.add(edge.fromNodeId());
                required.add(edge.toNodeId());
            }
        }
        for (String id : allIds) if (snapshot.activeNodeIds().contains(id)) required.add(id);
        int slots = Math.min(allIds.size(), Math.max(capacity, required.size()));
        List<String> visible = new ArrayList<>();
        visible.add(allIds.get(0));
        visible.addAll(allIds.subList(allIds.size() - slots + 1, allIds.size()));
        for (String id : required) {
            if (visible.contains(id)) continue;
            int replacement = -1;
            for (int slot = 1; slot < visible.size(); slot++) {
                if (!required.contains(visible.get(slot))) { replacement = slot; break; }
            }
            if (replacement < 0) replacement = 0;
            visible.set(replacement, id);
        }
        return List.copyOf(visible);
    }

    private static void drawAutomatonInput(GraphicsContext g, String input, Map<String, String> facts, LayoutFrame frame) {
        RenderSupport.label(g, "Text", frame.contentX(), frame.contentY() + 22.0, RenderSupport.palette(g).text());
        int current = Integer.parseInt(facts.getOrDefault("extension-index", "-1"));
        List<String> ends = orderedValues(facts.get("clone-end-positions"));
        double start = frame.contentX() + 56.0;
        for (int index = 0; index < input.length(); index++) {
            double x = start + index * 28.0;
            boolean active = index == current || ends.contains(Integer.toString(index));
            RenderSupport.nodeBox(g, x, frame.contentY(), 26.0, 28.0,
                    RenderSupport.palette(g).fill(active ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                    Character.toString(input.charAt(index)));
            secondaryLabel(g, Integer.toString(index), x + 6.0, frame.contentY() + 45.0,
                    RenderSupport.palette(g).secondaryText());
        }
    }

    private static void directedTip(GraphicsContext g, double x, double y, double dx, double dy, Color color) {
        g.setFill(color);
        g.fillPolygon(new double[] {x, x - dx * 10.0 - dy * 5.0, x - dx * 10.0 + dy * 5.0},
                new double[] {y, y - dy * 10.0 + dx * 5.0, y - dy * 10.0 - dx * 5.0}, 3);
    }

    private static Map<String, List<String>> graphBadges(GraphState state, Map<String, String> facts,
                                                        Map<String, String> distances) {
        Map<String, List<String>> badges = new LinkedHashMap<>();
        Map<String, String> discovery = integerMap(facts.get("discovery"));
        Map<String, String> finish = integerMap(facts.get("finish"));
        Map<String, String> low = integerMap(facts.get("low"));
        Map<String, String> indexes = integerMap(facts.get("index"));
        Map<String, String> indegree = integerMap(facts.get("indegree"));
        Map<String, String> components = integerMap(facts.get("component"));
        Map<String, String> roots = integerMap(facts.get("component-roots"));
        Map<String, String> sizes = integerMap(facts.get("component-sizes"));
        for (Node node : state.nodes()) {
            String id = node.id();
            List<String> values = new ArrayList<>();
            if (distances.containsKey(id)) values.add("d=" + distances.get(id)
                    + (id.equals(facts.get("source")) ? " · source" : ""));
            else if (indegree.containsKey(id)) values.add("in=" + indegree.get(id));
            else if (low.containsKey(id)) {
                values.add((indexes.isEmpty() ? "d=" + discovery.getOrDefault(id, "?")
                        : "i=" + indexes.getOrDefault(id, "?")) + " low=" + low.get(id));
                if (meaningful(components.get(id)) && !"?".equals(components.get(id))) values.add("SCC " + components.get(id));
                else if (orderedValues(facts.get("articulation")).contains(id)) values.add("cut");
            } else if (facts.containsKey("callStack")) {
                values.add(knownTime(discovery.get(id)) + "/" + knownTime(finish.get(id)));
            } else if (sizes.containsKey(id)) values.add("size " + sizes.get(id));
            else if (roots.containsKey(id)) values.add("root " + roots.get(id));
            if (facts.containsKey("maxFlow")) {
                if (id.equals(facts.get("source"))) values.add("source");
                if (id.equals(facts.get("sink"))) values.add("sink");
            }
            if (state.nodeCoordinates().containsKey(id)) {
                NodeCoordinate coordinate = state.nodeCoordinates().get(id);
                values.add("(" + coordinateText(coordinate.x()) + "," + coordinateText(coordinate.y()) + ")");
                List<String> aliases = state.nodes().stream().filter(other -> sameCoordinate(coordinate,
                        state.nodeCoordinates().get(other.id()))).map(GraphCanvasRenderer::primaryNodeLabel).toList();
                if (aliases.size() > 1) values.add(String.join("=", aliases.subList(0, Math.min(4, aliases.size())))
                        + (aliases.size() > 4 ? " +" + (aliases.size() - 4) : ""));
            }
            badges.put(id, List.copyOf(values));
        }
        return badges;
    }

    private static boolean testedHullEdge(Edge edge, List<String> testNodes) {
        for (int index = 0; index < 2; index++) {
            String first = testNodes.get(index);
            String second = testNodes.get(index + 1);
            if (edge.fromNodeId().equals(first) && edge.toNodeId().equals(second)
                    || edge.fromNodeId().equals(second) && edge.toNodeId().equals(first)) return true;
        }
        return false;
    }

    private static boolean sameCoordinate(NodeCoordinate first, NodeCoordinate second) {
        return second != null && first.x() == second.x() && first.y() == second.y();
    }

    private static void drawGraphAuxiliary(GraphicsContext g, GraphState state, Map<String, String> facts,
                                            LayoutFrame frame, double top) {
        if (facts.containsKey("maxFlow")) {
            List<String> path = orderedValues(facts.get("path"));
            drawSequence(g, path.isEmpty() ? "Queue" : "Path", path.isEmpty()
                    ? orderedValues(facts.get("queue")) : path, frame, top, false, path.isEmpty(), "flow-queue:");
            String residual = meaningful(facts.get("residual-from"))
                    ? "r(" + facts.get("residual-from") + "→" + facts.get("residual-to") + ") " + residualCapacityChange(facts)
                    : meaningful(facts.get("bottleneck-calculation")) ? "Δ = " + facts.get("bottleneck-calculation") : "";
            String result = "F=" + facts.get("maxFlow")
                    + (meaningful(facts.get("cutCapacity")) ? "   cut=" + facts.get("cutCapacity") : "");
            auxiliaryText(g, residual.isBlank() ? result : residual + "     " + result, frame, top + 68.0);
        } else if (facts.containsKey("component-sizes")) {
            Map<String, String> rootSizes = integerMap(facts.get("component-sizes"));
            String firstRoot = facts.get("root-a");
            String secondRoot = facts.get("root-b");
            boolean finished = "complete".equals(facts.get("operation"));
            if (!finished && rootSizes.containsKey(firstRoot) && rootSizes.containsKey(secondRoot)) {
                drawSequence(g, "Roots", List.of(firstRoot + " · " + rootSizes.get(firstRoot),
                        secondRoot + " · " + rootSizes.get(secondRoot)), frame, top, false, false, "");
            }
            String rewrite = facts.get("parent-rewrite");
            String detail = meaningful(rewrite) ? rewrite.substring(rewrite.lastIndexOf(';') + 1).trim()
                    : "Sets " + facts.get("component-count");
            auxiliaryText(g, detail, frame, top + 68.0);
        } else if (facts.containsKey("hull-size")) {
            boolean complete = List.of("return", "complete").contains(facts.getOrDefault("phase", ""));
            List<String> chain = orderedValues(facts.get(complete ? "hull-points" : "chain")).stream()
                    .map(value -> value.substring(0, value.indexOf('='))).toList();
            drawSequence(g, complete ? "Hull" : "Chain", chain, frame, top, false, false, "chain:");
            if (meaningful(facts.get("orientation-points"))) {
                auxiliaryText(g, facts.get("orientation-points").replace(", ", " → ")
                        + "     cross=" + facts.get("last-cross"), frame, top + 68.0);
            }
        } else if (facts.containsKey("ready")) {
            drawSequence(g, "Ready", orderedValues(facts.get("ready")), frame, top, false, true, "ready:");
            drawSequence(g, "Output", orderedValues(facts.get("order")), frame, top + 42.0, false, false, "output:");
        } else if (facts.containsKey("callStack")) {
            drawSequence(g, "Stack", orderedValues(facts.get("callStack")), frame, top, true, true, "dfs:");
            auxiliaryText(g, "entry / exit", frame, top + 68.0);
        } else if (facts.containsKey("dfsStack")) {
            drawSequence(g, "Stack", orderedValues(facts.get("dfsStack")), frame, top, true, true, "dfs:");
            List<String> bridges = orderedValues(facts.get("bridges"));
            if (!bridges.isEmpty()) drawSequence(g, "Bridges", bridges, frame, top + 42.0, false, false, "");
        } else if (facts.containsKey("stack") && facts.containsKey("low")) {
            drawSequence(g, "SCC stack", orderedValues(facts.get("stack")), frame, top, true, true, "tarjan:");
            List<String> components = orderedValues(facts.get("components"));
            if (!components.isEmpty()) drawSequence(g, "SCCs", components, frame, top + 42.0, false, false, "");
        } else if (facts.containsKey("sorted-edges")) {
            if ("complete".equals(facts.get("phase"))) {
                List<String> selected = orderedValues(facts.get("selected-edges")).stream()
                        .map(value -> value.substring(value.indexOf(':') + 1)).toList();
                drawSequence(g, "Forest", selected, frame, top, false, false, "");
                auxiliaryText(g, "Weight " + facts.get("total-weight") + "     Components " + facts.get("components"), frame, top + 68.0);
                return;
            }
            List<String> order = new ArrayList<>(orderedValues(facts.get("sorted-edges")));
            String current = facts.getOrDefault("current-edge", "NIL");
            int selected = -1;
            for (int index = 0; index < order.size(); index++) if (order.get(index).startsWith(current + ":")) selected = index;
            if (selected > 0) order = order.subList(selected, order.size());
            List<String> edges = order.stream().map(entry -> orderedEdge(state, entry)).toList();
            drawSequence(g, "By weight", edges, frame, top, false, true, "");
            auxiliaryText(g, "Weight " + facts.get("total-weight") + "     Components " + facts.get("components"), frame, top + 68.0);
        } else if (facts.containsKey("frontier") && facts.containsKey("total-weight")) {
            List<String> frontier = orderedValues(facts.get("frontier")).stream()
                    .map(value -> value.substring(value.indexOf(':') + 1)).toList();
            drawSequence(g, "Frontier", frontier, frame, top, false, true, "frontier:");
            auxiliaryText(g, "Weight " + facts.get("total-weight") + "     Components " + facts.get("components"), frame, top + 68.0);
        } else if (facts.containsKey("negative-cycle")) {
            auxiliaryText(g, "Pass " + facts.get("pass")
                    + ("true".equals(facts.get("negative-cycle")) ? "     negative cycle" : ""), frame, top + 26.0);
        }
    }

    private static void drawSequence(GraphicsContext g, String label, List<String> entries, LayoutFrame frame,
                                      double top, boolean fromTail, boolean markNext, String motionPrefix) {
        double x = frame.contentX() + Math.max(86.0, RenderSupport.textWidth(label) + 16.0);
        double right = frame.contentX() + frame.contentWidth();
        RenderSupport.label(g, label, frame.contentX(), top + 24.0, RenderSupport.palette(g).text());
        if (entries.isEmpty()) {
            secondaryLabel(g, "∅", x, top + 24.0, RenderSupport.palette(g).secondaryText());
            return;
        }
        double remainingWidth = right - x;
        int start = 0;
        int end = entries.size();
        while (start < end - 1 && sequenceWidth(entries.subList(start, end)) > remainingWidth - 44.0) {
            if (fromTail) start++; else end--;
        }
        if (start > 0) {
            secondaryLabel(g, "+" + start, x, top + 24.0, RenderSupport.palette(g).secondaryText());
            x += 44.0;
        }
        for (int index = start; index < end; index++) {
            String entry = entries.get(index);
            double width = Math.max(34.0, RenderSupport.textWidth(entry) + 18.0);
            boolean next = markNext && index == (fromTail ? entries.size() - 1 : 0);
            javafx.geometry.Point2D point = motionPrefix.isBlank() ? new javafx.geometry.Point2D(x, top)
                    : RenderSupport.movingPoint(g, motionPrefix + entry, x, top);
            RenderSupport.nodeBox(g, point.getX(), point.getY(), width, 34.0,
                    RenderSupport.palette(g).fill(next ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT), entry);
            if (next) {
                graphicsPointer(g, fromTail ? "top" : "next", point.getX() + width / 2.0, top - 3.0);
            }
            x += width + 8.0;
        }
        if (end < entries.size()) secondaryLabel(g, "+" + (entries.size() - end), x, top + 24.0,
                RenderSupport.palette(g).secondaryText());
    }

    private static void graphicsPointer(GraphicsContext g, String text, double center, double baseline) {
        secondaryLabel(g, text, center - secondaryTextWidth(text) / 2.0, baseline, RenderSupport.palette(g).secondaryText());
    }

    private static double sequenceWidth(List<String> entries) {
        return entries.stream().mapToDouble(value -> Math.max(34.0, RenderSupport.textWidth(value) + 18.0) + 8.0).sum();
    }

    private static void auxiliaryText(GraphicsContext g, String text, LayoutFrame frame, double y) {
        if (secondaryTextWidth(text) <= frame.contentWidth()) secondaryLabel(g, text, frame.contentX(), y,
                RenderSupport.palette(g).secondaryText());
    }

    private static String orderedEdge(GraphState state, String entry) {
        String[] pair = entry.split(":", 2);
        if (pair.length != 2) return entry;
        return state.edges().stream().filter(edge -> edge.id().equals("edge-" + pair[0])).findFirst()
                .map(edge -> edge.fromNodeId() + "–" + edge.toNodeId() + "(" + pair[1] + ")").orElse(entry);
    }

    private static void drawReverseResidual(GraphicsContext g, SimulationSnapshot snapshot, Map<String, String> facts,
                                            Map<String, double[]> positions, LayoutFrame frame,
                                            List<LabelBox> occupied, List<GraphLabel> labels,
                                            List<RenderSupport.EdgeSegment> routes) {
        if (!"reverse".equals(facts.get("residual-direction"))) return;
        double[] from = positions.get(facts.get("residual-from"));
        double[] to = positions.get(facts.get("residual-to"));
        if (from == null || to == null || java.util.Arrays.equals(from, to)) return;
        Color color = RenderSupport.edgeColor(g, snapshot, SnapshotStatus.ACTIVE);
        RenderSupport.EdgeSegment segment = RenderSupport.clippedCircleSegment(from[0], from[1], to[0], to[1],
                NODE_RADIUS, NODE_RADIUS, 16.0);
        g.setLineDashes(6.0, 4.0);
        RenderSupport.directedSegment(g, segment, color, 3.5);
        g.setLineDashes();
        String value = meaningful(facts.get("residual-after")) ? facts.get("residual-after") : facts.get("residual-before");
        labels.add(placeEdgeLabel(List.of("r=" + value), segment, color, frame, occupied, routes));
    }

    private static void secondaryLabel(GraphicsContext graphics, String text, double x, double y, Color color) {
        RenderSupport.secondaryLabel(graphics, text, x, y, color);
    }

    private static double secondaryTextWidth(String value) {
        return RenderSupport.secondaryTextWidth(value);
    }

    private static boolean isEndpointLabel(Edge edge, String label) {
        return label.equals(edge.fromNodeId() + "→" + edge.toNodeId())
                || label.equals(edge.fromNodeId() + "—" + edge.toNodeId());
    }

    private static String residualCapacityChange(Map<String, String> facts) {
        String before = facts.getOrDefault("residual-before", "?");
        String after = facts.get("residual-after");
        return meaningful(after) ? before + " → " + after : before;
    }

    /** MaxFlowEngine labels begin with flow=f/c; the current residual has its own teaching rail. */
    private static String flowRatio(String label) {
        int start = label.indexOf("flow=");
        if (start < 0) return label;
        start += "flow=".length();
        int end = label.indexOf(';', start);
        return label.substring(start, end < 0 ? label.length() : end).trim();
    }

    private static Map<String, String> factsByKey(List<Fact> facts) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Fact fact : facts) values.put(fact.key(), fact.value());
        return values;
    }

    private static Map<String, String> integerMap(String value) {
        if (value == null) return Map.of();
        if (value.startsWith("[") && value.endsWith("]")) {
            Map<String, String> indexed = new LinkedHashMap<>();
            List<String> entries = orderedValues(value);
            for (int index = 0; index < entries.size(); index++) indexed.put(Integer.toString(index), entries.get(index));
            return indexed;
        }
        if (!value.startsWith("{") || !value.endsWith("}")) return Map.of();
        Map<String, String> values = new LinkedHashMap<>();
        for (String entry : value.substring(1, value.length() - 1).split(",")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length == 2) values.put(pair[0].trim(), pair[1].trim());
        }
        return values;
    }

    /** Split trace lists while preserving commas within coordinate tuples. */
    static List<String> orderedValues(String value) {
        if (value == null || !value.startsWith("[") || !value.endsWith("]")) return List.of();
        List<String> values = new ArrayList<>();
        String body = value.substring(1, value.length() - 1);
        int depth = 0;
        int start = 0;
        for (int index = 0; index < body.length(); index++) {
            char character = body.charAt(index);
            if (character == '(' || character == '[') depth++;
            else if (character == ')' || character == ']') depth--;
            else if (character == ',' && depth == 0) {
                values.add(body.substring(start, index).trim());
                start = index + 1;
            }
        }
        if (!body.substring(start).isBlank()) values.add(body.substring(start).trim());
        return List.copyOf(values);
    }

    private static boolean meaningful(String value) {
        return value != null && !value.isBlank() && !List.of("NIL", "none", "-", "[]", "{}").contains(value);
    }

    private static String coordinateText(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static String knownTime(String value) {
        return meaningful(value) && !"-1".equals(value) ? value : "—";
    }

    /**
     * Assigns separate perpendicular lanes to parallel/reciprocal edges.
     * The result is indexed by the immutable edge list, so duplicate labels
     * or IDs cannot collapse two routes into one map entry.
     */
    static List<Double> edgeRouteOffsets(List<Edge> edges) {
        Map<List<String>, List<Integer>> groups = new LinkedHashMap<>();
        List<Double> offsets = new ArrayList<>();
        for (int index = 0; index < edges.size(); index++) {
            Edge edge = edges.get(index);
            offsets.add(0.0);
            if (edge.fromNodeId().equals(edge.toNodeId())) {
                continue;
            }
            String first = edge.fromNodeId().compareTo(edge.toNodeId()) <= 0
                    ? edge.fromNodeId() : edge.toNodeId();
            String second = edge.fromNodeId().compareTo(edge.toNodeId()) <= 0
                    ? edge.toNodeId() : edge.fromNodeId();
            groups.computeIfAbsent(List.of(first, second), ignored -> new ArrayList<>()).add(index);
        }
        for (Map.Entry<List<String>, List<Integer>> entry : groups.entrySet()) {
            List<String> canonicalPair = entry.getKey();
            List<Integer> group = entry.getValue();
            double midpoint = (group.size() - 1) / 2.0;
            for (int rank = 0; rank < group.size(); rank++) {
                int edgeIndex = group.get(rank);
                Edge edge = edges.get(edgeIndex);
                double lane = (rank - midpoint) * 18.0;
                // A reversed edge has the opposite geometric normal. Flip
                // its signed lane so a reciprocal pair remains separated in
                // canvas space rather than collapsing onto one route.
                double orientation = edge.fromNodeId().equals(canonicalPair.get(0)) ? 1.0 : -1.0;
                offsets.set(edgeIndex, orientation * lane);
            }
        }
        return List.copyOf(offsets);
    }

    static Map<String, double[]> circlePositions(List<String> ids, LayoutFrame frame) {
        Map<String, double[]> positions = new HashMap<>();
        if (ids.isEmpty()) {
            return positions;
        }
        if (ids.size() == 1) {
            positions.put(ids.get(0), new double[] {frame.centerX(), frame.centerY()});
            return positions;
        }
        double insetX = Math.min(78.0, frame.contentWidth() * 0.18);
        double insetTop = Math.min(60.0, frame.contentHeight() * 0.22);
        double insetBottom = Math.min(80.0, frame.contentHeight() * 0.25);
        double left = frame.contentX() + insetX;
        double top = frame.contentY() + insetTop;
        double width = Math.max(1.0, frame.contentWidth() - insetX * 2.0);
        double height = Math.max(1.0, frame.contentHeight() - insetTop - insetBottom);
        double[] xs = new double[ids.size()];
        double[] ys = new double[ids.size()];
        for (int index = 0; index < ids.size(); index++) {
            double angle = ids.size() == 2 ? index * Math.PI : -Math.PI / 2.0 + 2.0 * Math.PI * index / ids.size();
            xs[index] = Math.cos(angle);
            ys[index] = ids.size() == 2 ? 0.0 : Math.sin(angle);
        }
        double minX = java.util.Arrays.stream(xs).min().orElse(-1.0);
        double maxX = java.util.Arrays.stream(xs).max().orElse(1.0);
        double minY = java.util.Arrays.stream(ys).min().orElse(-1.0);
        double maxY = java.util.Arrays.stream(ys).max().orElse(1.0);
        for (int index = 0; index < ids.size(); index++) {
            positions.put(ids.get(index), new double[] {
                    left + (xs[index] - minX) / Math.max(1.0e-9, maxX - minX) * width,
                    maxY - minY < 1.0e-9 ? top + height / 2.0
                            : top + (ys[index] - minY) / (maxY - minY) * height
            });
        }
        return positions;
    }

    static Map<String, double[]> coordinatePositions(Map<String, NodeCoordinate> coordinates, LayoutFrame frame) {
        Map<String, double[]> positions = new HashMap<>();
        if (coordinates.isEmpty()) {
            return positions;
        }
        double minX = coordinates.values().stream().mapToDouble(NodeCoordinate::x).min().orElse(0.0);
        double maxX = coordinates.values().stream().mapToDouble(NodeCoordinate::x).max().orElse(1.0);
        double minY = coordinates.values().stream().mapToDouble(NodeCoordinate::y).min().orElse(0.0);
        double maxY = coordinates.values().stream().mapToDouble(NodeCoordinate::y).max().orElse(1.0);
        double rangeX = Math.max(1.0e-9, maxX - minX);
        double rangeY = Math.max(1.0e-9, maxY - minY);
        double availableWidth = Math.max(1.0, frame.contentWidth() - 156.0);
        double availableHeight = Math.max(1.0, frame.contentHeight() - 140.0);
        double scale = Math.min(availableWidth / rangeX, availableHeight / rangeY);
        double usedWidth = rangeX * scale;
        double usedHeight = rangeY * scale;
        double originX = frame.centerX() - usedWidth / 2.0;
        double originY = frame.centerY() + usedHeight / 2.0;
        for (Map.Entry<String, NodeCoordinate> entry : coordinates.entrySet()) {
            NodeCoordinate coordinate = entry.getValue();
            positions.put(entry.getKey(), new double[] {
                    originX + (coordinate.x() - minX) * scale,
                    originY - (coordinate.y() - minY) * scale
            });
        }
        return positions;
    }

    private static String primaryNodeLabel(Node node) {
        int split = node.label().indexOf(' ');
        return split < 0 ? node.label() : node.label().substring(0, split);
    }

    private static List<double[]> edgeLabelCandidates(RenderSupport.EdgeSegment segment) {
        List<double[]> candidates = new ArrayList<>();
        double nx = segment.normalX();
        double ny = segment.normalY();
        double offset = 16.0;
        // Prioritize candidates offset along the normal (above/below or beside the shaft)
        for (double fraction : new double[] {0.5, 0.4, 0.6, 0.35, 0.65, 0.25, 0.75}) {
            double px = segment.startX() + (segment.endX() - segment.startX()) * fraction;
            double py = segment.startY() + (segment.endY() - segment.startY()) * fraction;
            candidates.add(new double[] {px + nx * offset, py + ny * offset});
            candidates.add(new double[] {px - nx * offset, py - ny * offset});
            candidates.add(new double[] {px, py});
        }
        return candidates;
    }

    private static GraphLabel placeEdgeLabel(List<String> lines, RenderSupport.EdgeSegment segment,
                                             Color color, LayoutFrame frame, List<LabelBox> occupied,
                                             List<RenderSupport.EdgeSegment> routes) {
        double width = lines.stream().mapToDouble(RenderSupport::textWidth).max().orElse(0.0) + 16.0;
        double height = lines.size() * 24.0 + 4.0;
        List<double[]> candidates = edgeLabelCandidates(segment);
        return placeLabel(lines, candidates, segment.midX(), segment.midY(),
                color, frame, occupied, segment, routes);
    }

    private static GraphLabel placeLabel(List<String> lines, List<double[]> candidates,
                                        double anchorX, double anchorY, Color color,
                                        LayoutFrame frame, List<LabelBox> occupied) {
        return placeLabel(lines, candidates, anchorX, anchorY, color, frame, occupied, null, List.of());
    }

    private static GraphLabel placeLabel(List<String> lines, List<double[]> candidates,
                                        double anchorX, double anchorY, Color color,
                                        LayoutFrame frame, List<LabelBox> occupied,
                                        RenderSupport.EdgeSegment ownRoute, List<RenderSupport.EdgeSegment> routes) {
        double width = lines.stream().mapToDouble(RenderSupport::textWidth).max().orElse(0.0) + 16.0;
        double height = lines.size() * 24.0 + 4.0;
        LabelBox best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (double[] candidate : candidates) {
            LabelBox box = new LabelBox(candidate[0] - width / 2.0, candidate[1] - height / 2.0, width, height);
            double overlap = occupied.stream().mapToDouble(box::overlapArea).sum();
            double outside = Math.max(0.0, frame.contentX() - box.left())
                    + Math.max(0.0, frame.contentY() - box.top())
                    + Math.max(0.0, box.right() - (frame.contentX() + frame.contentWidth()))
                    + Math.max(0.0, box.bottom() - (frame.contentY() + frame.contentHeight()));
            double crossings = 0.0;
            for (RenderSupport.EdgeSegment route : routes) {
                if (!route.equals(ownRoute)
                        && segmentDistance(candidate[0], candidate[1], route) < Math.hypot(width, height) / 2.0 + 6.0) {
                    crossings += 1.0;
                }
            }
            double score = overlap * 20000.0 + outside * 50000.0 + crossings * 5000.0
                    + Math.hypot(candidate[0] - anchorX, candidate[1] - anchorY) * 0.5;
            if (score < bestScore) { best = box; bestScore = score; }
        }
        if (best == null) {
            best = new LabelBox(anchorX - width / 2.0, anchorY - height / 2.0, width, height);
        }
        occupied.add(best);
        return new GraphLabel(lines, best, best.centerX(), best.centerY(), color);
    }

    private static double segmentDistance(double x, double y, RenderSupport.EdgeSegment segment) {
        double dx = segment.endX() - segment.startX();
        double dy = segment.endY() - segment.startY();
        double lengthSquared = dx * dx + dy * dy;
        double fraction = lengthSquared == 0.0 ? 0.0
                : Math.max(0.0, Math.min(1.0, ((x - segment.startX()) * dx + (y - segment.startY()) * dy) / lengthSquared));
        return Math.hypot(x - segment.startX() - fraction * dx, y - segment.startY() - fraction * dy);
    }

    private static void drawLabel(GraphicsContext graphics, GraphLabel label) {
        LabelBox box = label.box();
        graphics.setFill(RenderSupport.palette(graphics).background());
        graphics.fillRoundRect(box.left(), box.top(), box.width(), box.height(), 8.0, 8.0);
        graphics.setStroke(RenderSupport.palette(graphics).outline());
        graphics.setLineWidth(1.0);
        graphics.strokeRoundRect(box.left(), box.top(), box.width(), box.height(), 8.0, 8.0);
        for (int line = 0; line < label.lines().size(); line++) {
            String text = label.lines().get(line);
            RenderSupport.label(graphics, text, box.centerX() - RenderSupport.textWidth(text) / 2.0,
                    box.top() + 19.0 + line * 24.0, RenderSupport.palette(graphics).text());
        }
    }

    private record GraphLabel(List<String> lines, LabelBox box, double anchorX, double anchorY, Color color) { }

    private record LabelBox(double left, double top, double width, double height) {
        double centerX() { return left + width / 2.0; }
        double centerY() { return top + height / 2.0; }
        double right() { return left + width; }
        double bottom() { return top + height; }
        double overlapArea(LabelBox other) {
            double overlapX = Math.max(0.0, Math.min(left + width + 3.0, other.left + other.width + 3.0)
                    - Math.max(left - 3.0, other.left - 3.0));
            double overlapY = Math.max(0.0, Math.min(top + height + 3.0, other.top + other.height + 3.0)
                    - Math.max(top - 3.0, other.top - 3.0));
            return overlapX * overlapY;
        }
    }

    static Map<String, String> distanceLabels(List<Fact> facts) {
        Map<String, String> labels = new LinkedHashMap<>();
        facts.stream().filter(fact -> fact.key().equals("distance") && fact.value().startsWith("{"))
                .findFirst().ifPresent(fact -> {
                    String value = fact.value();
                    for (String entry : value.substring(1, value.length() - 1).split(",")) {
                        String[] pair = entry.trim().split("=", 2);
                        if (pair.length == 2) labels.put(pair[0].trim(), pair[1].trim());
                    }
                });
        return Map.copyOf(labels);
    }

    private static GraphState requireState(SimulationSnapshot snapshot) {
        if (snapshot == null || snapshot.state().rendererFamily() != RendererFamily.GRAPH) {
            throw new IllegalArgumentException("GraphCanvasRenderer requires GRAPH state");
        }
        return (GraphState) snapshot.state();
    }
}
