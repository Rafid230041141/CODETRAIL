package dev.codetrail.desktop.simulation;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fixed-size allocation boxes with explicit links and reversal pointers. */
public final class LinkedCanvasRenderer implements SimulationRenderer {
    private static final double BOX_WIDTH = 80.0;
    private static final double BOX_HEIGHT = 44.0;
    private static final double COLUMN_GAP = 44.0;

    @Override
    public RendererFamily family() { return RendererFamily.LINKED; }

    @Override
    public void render(GraphicsContext graphics, SimulationSnapshot snapshot, LayoutFrame frame) {
        LinkedState state = requireState(snapshot);
        RenderSupport.clearAndFrame(graphics, frame);
        Map<String, String> facts = new HashMap<>();
        for (Fact fact : state.facts()) facts.put(fact.key(), fact.value());
        List<String> ids = state.nodes().stream().map(Node::id)
                .sorted(java.util.Comparator.comparingInt(LinkedCanvasRenderer::serial)).toList();
        Map<String, double[]> targets = horizontalPositions(ids, frame);
        Map<String, double[]> positions = new LinkedHashMap<>();
        for (String id : ids) {
            double[] target = targets.get(id);
            Point2D point = RenderSupport.movingPoint(graphics, "linked:" + id, target[0], target[1]);
            positions.put(id, new double[] {point.getX(), point.getY()});
        }
        Map<String, Integer> routeLanes = new HashMap<>();
        Set<String> linked = new HashSet<>();
        for (Edge edge : state.edges()) {
            linked.add(edge.fromNodeId());
            double[] from = positions.get(edge.fromNodeId());
            double[] target = positions.get(edge.toNodeId());
            if (from == null || target == null) continue;
            Point2D movingTarget = RenderSupport.movingPoint(graphics, "linked-next:" + edge.fromNodeId(),
                    target[0] + BOX_WIDTH / 2.0, target[1]);
            double[] to = {movingTarget.getX() - BOX_WIDTH / 2.0, movingTarget.getY()};
            SnapshotStatus status = snapshot.activeEdgeIds().contains(edge.id()) ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            Color color = RenderSupport.edgeColor(graphics, snapshot, status);
            if (edge.fromNodeId().equals(edge.toNodeId())) {
                double laneY = from[1] - BOX_HEIGHT / 2.0 - 28.0;
                directedRoute(graphics, color, from[0] + BOX_WIDTH * 0.75, from[1] - BOX_HEIGHT / 2.0,
                        from[0] + BOX_WIDTH * 0.75, laneY, from[0] + BOX_WIDTH + 18.0, laneY,
                        from[0] + BOX_WIDTH + 18.0, from[1], from[0] + BOX_WIDTH, from[1]);
                continue;
            }
            if (Math.hypot(from[0] - to[0], from[1] - to[1]) <= BOX_WIDTH) continue;
            boolean reciprocal = state.edges().stream().anyMatch(other -> other.fromNodeId().equals(edge.toNodeId())
                    && other.toNodeId().equals(edge.fromNodeId()));
            RenderSupport.EdgeSegment direct = RenderSupport.clippedBoxSegment(from[0] + BOX_WIDTH / 2.0, from[1],
                    to[0] + BOX_WIDTH / 2.0, to[1], BOX_WIDTH / 2.0, BOX_HEIGHT / 2.0, BOX_WIDTH / 2.0, BOX_HEIGHT / 2.0,
                    reciprocal ? 10.0 : 0.0);
            if (clearBetweenRows(direct, edge, positions, BOX_WIDTH)) RenderSupport.directedSegment(graphics, direct, color, 2.0);
            else routeAroundBoxes(graphics, edge, from, to, BOX_WIDTH, frame, color, routeLanes);
        }
        for (Node node : state.nodes()) {
            double[] point = positions.get(node.id());
            SnapshotStatus status = snapshot.activeNodeIds().contains(node.id()) ? SnapshotStatus.ACTIVE
                    : node.status() == SnapshotStatus.REJECTED ? SnapshotStatus.REJECTED : SnapshotStatus.DEFAULT;
            RenderSupport.nodeBox(graphics, point[0], point[1] - BOX_HEIGHT / 2.0, BOX_WIDTH, BOX_HEIGHT,
                    RenderSupport.color(graphics, snapshot, status), node.label());
            RenderSupport.centeredSecondaryLabel(graphics, node.id(), point[0], point[1] - 32.0, BOX_WIDTH,
                    RenderSupport.palette(graphics).secondaryText());
            if (node.id().equals(state.headId())) RenderSupport.centeredSecondaryLabel(graphics, "head", point[0],
                    point[1] - 52.0, BOX_WIDTH, RenderSupport.palette(graphics).text());
            if (!linked.contains(node.id())) {
                boolean incomingFromRight = state.edges().stream().anyMatch(edge -> edge.toNodeId().equals(node.id())
                        && positions.get(edge.fromNodeId())[0] > point[0]);
                drawNullLink(graphics, node.id(), point, incomingFromRight ? -1.0 : 1.0, frame);
            }
            List<String> roles = pointerRoles(node.id(), null, facts);
            for (int index = 0; index < roles.size(); index++) {
                RenderSupport.centeredSecondaryLabel(graphics, roles.get(index), point[0], point[1] + 43.0 + index * 20.0,
                        BOX_WIDTH, RenderSupport.palette(graphics).text());
            }
        }
        List<String> nullPointers = pointerRoles("null", state.headId() == null ? "null" : state.headId(), facts);
        if (state.nodes().isEmpty() || !nullPointers.isEmpty()) {
            String label = nullPointers.isEmpty() ? "head → null" : String.join(" / ", nullPointers) + " → null";
            RenderSupport.centeredSecondaryLabel(graphics, label, frame.centerX(), frame.contentY() + frame.contentHeight() - 6.0,
                    0.0, RenderSupport.palette(graphics).secondaryText());
        }
    }

    private static List<String> pointerRoles(String id, String head, Map<String, String> facts) {
        List<String> roles = new ArrayList<>();
        if (id.equals(head)) roles.add("head");
        for (String key : List.of("previous", "current", "next")) if (id.equals(facts.get(key))) roles.add(key);
        return roles;
    }

    private static void drawNullLink(GraphicsContext graphics, String id, double[] point,
                                     double direction, LayoutFrame frame) {
        double start = point[0] + (direction > 0.0 ? BOX_WIDTH : 0.0);
        double targetX = start + direction * 24.0;
        double portY = point[1];
        if (targetX - 9.0 < frame.contentX() || targetX + 9.0 > frame.contentX() + frame.contentWidth()) {
            direction = -direction;
            start = point[0] + (direction > 0.0 ? BOX_WIDTH : 0.0);
            targetX = start + direction * 24.0;
            portY += 12.0;
        }
        Point2D target = RenderSupport.movingPoint(graphics, "linked-next:" + id, targetX, portY);
        Color color = RenderSupport.palette(graphics).edge(SnapshotStatus.DEFAULT);
        RenderSupport.line(graphics, start, portY, target.getX() - direction * 9.0, target.getY(), color, 1.5);
        RenderSupport.centeredSecondaryLabel(graphics, "∅", target.getX(), target.getY() + 6.0, 0.0,
                RenderSupport.palette(graphics).secondaryText());
    }

    private static boolean clearBetweenRows(RenderSupport.EdgeSegment segment, Edge edge,
                                            Map<String, double[]> positions, double width) {
        for (Map.Entry<String, double[]> entry : positions.entrySet()) {
            if (entry.getKey().equals(edge.fromNodeId()) || entry.getKey().equals(edge.toNodeId())) continue;
            double[] point = entry.getValue();
            double[] start = {segment.startX(), segment.startY()};
            double[] direction = {segment.endX() - segment.startX(), segment.endY() - segment.startY()};
            double[] minimum = {point[0] - 6.0, point[1] - BOX_HEIGHT / 2.0 - 6.0};
            double[] maximum = {point[0] + width + 6.0, point[1] + BOX_HEIGHT / 2.0 + 6.0};
            double entryTime = 0.0;
            double exitTime = 1.0;
            for (int axis = 0; axis < 2; axis++) {
                if (Math.abs(direction[axis]) < 0.0001) {
                    if (start[axis] < minimum[axis] || start[axis] > maximum[axis]) { exitTime = -1.0; break; }
                } else {
                    double first = (minimum[axis] - start[axis]) / direction[axis];
                    double last = (maximum[axis] - start[axis]) / direction[axis];
                    entryTime = Math.max(entryTime, Math.min(first, last));
                    exitTime = Math.min(exitTime, Math.max(first, last));
                }
            }
            if (entryTime <= exitTime) return false;
        }
        return true;
    }

    private static void routeAroundBoxes(GraphicsContext graphics, Edge edge, double[] from, double[] to,
                                         double width, LayoutFrame frame, Color color, Map<String, Integer> lanes) {
        if (Math.abs(from[1] - to[1]) < 1.0) {
            boolean forward = to[0] > from[0];
            double side = forward ? -1.0 : 1.0;
            int lane = lanes.merge(from[1] + ":" + forward, 1, Integer::sum) - 1;
            double boundaryY = from[1] + side * BOX_HEIGHT / 2.0;
            double laneY = boundaryY + side * (28.0 + lane * 12.0);
            double direction = forward ? 1.0 : -1.0;
            double startX = from[0] + (forward ? width : 0.0);
            double endX = to[0] + (forward ? 0.0 : width);
            double portY = from[1] + side * 8.0;
            double departureX = startX + direction * 12.0;
            double arrivalX = endX - direction * 12.0;
            // Side ports keep both vertical legs outside node identities above the boxes.
            directedRoute(graphics, color, startX, portY, departureX, portY, departureX, laneY,
                    arrivalX, laneY, arrivalX, portY, endX, portY);
            return;
        }
        boolean down = to[1] > from[1];
        int lane = lanes.merge("perimeter:" + down, 1, Integer::sum) - 1;
        double sideX = down ? frame.contentX() + frame.contentWidth() - 8.0 - lane * 12.0
                : frame.contentX() + 8.0 + lane * 12.0;
        double direction = down ? 1.0 : -1.0;
        double startX = from[0] + width * (down ? 0.75 : 0.25);
        double endX = to[0] + width * (down ? 0.75 : 0.25);
        double startY = from[1] + direction * BOX_HEIGHT / 2.0;
        double endY = to[1] - direction * BOX_HEIGHT / 2.0;
        double departure = startY + direction * (16.0 + lane * 10.0);
        double arrival = endY - direction * (16.0 + lane * 10.0);
        directedRoute(graphics, color, startX, startY, startX, departure, sideX, departure,
                sideX, arrival, endX, arrival, endX, endY);
    }

    private static void directedRoute(GraphicsContext graphics, Color color, double... points) {
        for (int index = 0; index < points.length - 4; index += 2) {
            RenderSupport.line(graphics, points[index], points[index + 1], points[index + 2], points[index + 3], color, 2.0);
        }
        int end = points.length - 4;
        RenderSupport.directedSegment(graphics, new RenderSupport.EdgeSegment(points[end], points[end + 1],
                points[end + 2], points[end + 3], 0.0, 0.0, 0.0), color, 2.0);
    }

    static double boxWidth(int nodeCount, LayoutFrame frame) { return BOX_WIDTH; }

    static Map<String, double[]> horizontalPositions(List<String> ids, LayoutFrame frame) {
        Map<String, double[]> positions = new LinkedHashMap<>();
        if (ids.isEmpty()) return positions;
        int columns = Math.max(1, Math.min(8, (int) (frame.contentWidth() / (BOX_WIDTH + COLUMN_GAP))));
        int capacity = columns * Math.max(1, (int) ((frame.contentHeight() - 70.0) / 124.0));
        int minimum = ids.stream().mapToInt(LinkedCanvasRenderer::serial).min().orElse(0);
        int maximum = ids.stream().mapToInt(LinkedCanvasRenderer::serial).max().orElse(0);
        boolean retainGaps = maximum - minimum + 1 <= Math.max(capacity, ids.size());
        int slots = retainGaps ? maximum - minimum + 1 : ids.size();
        int rows = (slots + columns - 1) / columns;
        double stride = Math.min(124.0, Math.max(104.0, (frame.contentHeight() - 86.0) / Math.max(1, rows - 1)));
        double left = frame.centerX() - Math.min(columns, slots) * (BOX_WIDTH + COLUMN_GAP) / 2.0 + COLUMN_GAP / 2.0;
        double top = frame.centerY() - (rows - 1) * stride / 2.0 - 14.0;
        for (int index = 0; index < ids.size(); index++) {
            int slot = retainGaps ? serial(ids.get(index)) - minimum : index;
            int row = slot / columns;
            int column = row % 2 == 0 ? slot % columns : columns - 1 - slot % columns;
            positions.put(ids.get(index), new double[] {left + column * (BOX_WIDTH + COLUMN_GAP), top + row * stride});
        }
        return positions;
    }

    private static int serial(String id) {
        try { return Integer.parseInt(id.substring(id.lastIndexOf('-') + 1)); }
        catch (RuntimeException ignored) { return id.hashCode() & Integer.MAX_VALUE; }
    }

    private static LinkedState requireState(SimulationSnapshot snapshot) {
        if (snapshot == null || snapshot.state().rendererFamily() != RendererFamily.LINKED) {
            throw new IllegalArgumentException("LinkedCanvasRenderer requires LINKED state");
        }
        return (LinkedState) snapshot.state();
    }
}
