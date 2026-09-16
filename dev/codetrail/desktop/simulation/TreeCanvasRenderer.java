package dev.codetrail.desktop.simulation;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compact topology, values and operation-local marks for tree algorithms. */
public final class TreeCanvasRenderer implements SimulationRenderer {
    @Override
    public RendererFamily family() { return RendererFamily.TREE; }

    @Override
    public void render(GraphicsContext graphics, SimulationSnapshot snapshot, LayoutFrame frame) {
        TreeState state = requireState(snapshot);
        Map<String, String> facts = factValues(state.facts());
        RenderSupport.clearAndFrame(graphics, frame);
        boolean avl = facts.containsKey("height-by-node");
        boolean trie = facts.containsKey("terminals");
        boolean divide = facts.containsKey("minimum") && facts.containsKey("maximum");
        boolean range = divide || facts.containsKey("node-interval");
        boolean heap = facts.containsKey("heap-size");
        LayoutFrame primary = frame;
        if (trie || range) primary = drawInput(graphics, snapshot, facts, frame, trie, divide);
        if (heap) {
            int size = integer(facts.get("heap-size"), state.nodes().size());
            Set<String> retained = new HashSet<>();
            for (Node node : state.nodes()) if (integer(node.id(), -1) < size) retained.add(node.id());
            state = subset(state, retained, size == 0 ? null : state.rootId());
            if (present(facts.get("extracted-value")) && frame.contentHeight() > 180.0) primary = new LayoutFrame(frame.width(), frame.height(), frame.left(),
                    frame.top(), frame.right(), frame.bottom() + 68.0);
        }
        Set<String> focus = new HashSet<>(snapshot.activeNodeIds());
        for (String key : List.of("current-node", "active-node", "active-range")) {
            if (present(facts.get(key))) focus.add(facts.get(key));
        }
        TreeState visible = visibleTree(state, focus, primary);
        if (avl) {
            if (visible.nodes().isEmpty()) RenderSupport.centeredSecondaryLabel(graphics, "∅", primary.centerX(), primary.centerY(),
                    0.0, RenderSupport.palette(graphics).secondaryText());
            renderCompactAvl(graphics, snapshot, visible, facts, primary);
            drawFoldMarks(graphics, state, visible, compactBinaryPositions(visible, primary), 24.0);
            return;
        }
        if (visible.nodes().isEmpty()) {
            RenderSupport.centeredSecondaryLabel(graphics, "∅", primary.centerX(), primary.centerY(), 0.0,
                    RenderSupport.palette(graphics).secondaryText());
            if (heap) drawExtracted(graphics, snapshot, facts, frame);
            return;
        }
        TreeLayout layout = layout(visible, primary);
        Map<String, TreePoint> points = movingPositions(graphics, visible, layout.positions(), heap, trie, range);
        Map<String, String> covered = mapEntries(facts.get("partial-answer"));
        Set<String> combining = new HashSet<>();
        String combineParent = facts.get("active-range");
        if (divide && "combine".equals(facts.get("phase"))) {
            for (Edge edge : visible.edges()) if (edge.fromNodeId().equals(combineParent)) combining.add(edge.toNodeId());
        }
        for (Edge edge : visible.edges()) {
            TreePoint from = points.get(edge.fromNodeId());
            TreePoint to = points.get(edge.toNodeId());
            if (Math.hypot(from.x() - to.x(), from.y() - to.y()) < 54.0) continue;
            boolean active = snapshot.activeEdgeIds().contains(edge.id()) || combining.contains(edge.toNodeId());
            SnapshotStatus status = active ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            TreeNodeBox fromBox = layout.nodeBoxes().get(edge.fromNodeId());
            TreeNodeBox toBox = layout.nodeBoxes().get(edge.toNodeId());
            RenderSupport.EdgeSegment segment = range
                    ? RenderSupport.clippedBoxSegment(from.x(), from.y(), to.x(), to.y(), fromBox.width() / 2.0,
                            22.0, toBox.width() / 2.0, 22.0, 0.0)
                    : RenderSupport.clippedCircleSegment(from.x(), from.y(), to.x(), to.y(), 26.0, 26.0, 0.0);
            RenderSupport.line(graphics, segment.startX(), segment.startY(), segment.endX(), segment.endY(),
                    RenderSupport.edgeColor(graphics, snapshot, status), active ? 2.5 : 1.5);
            // A sole vertical BST child still needs its left/right relationship.
            if (!range && !trie && !heap && Math.abs(from.x() - to.x()) < 8.0 && edge.label().isPresent()) {
                String side = edge.label().orElseThrow().equals("left") ? "L" : "R";
                RenderSupport.secondaryLabel(graphics, side, segment.midX() + 12.0, segment.midY() + 6.0,
                        RenderSupport.palette(graphics).secondaryText());
            }
        }
        for (Node node : visible.nodes()) {
            TreePoint point = points.get(node.id());
            SnapshotStatus status = snapshot.activeNodeIds().contains(node.id()) ? SnapshotStatus.ACTIVE
                    : covered.containsKey(node.id()) || combining.contains(node.id()) ? SnapshotStatus.DONE
                    : node.status() == SnapshotStatus.REJECTED ? SnapshotStatus.REJECTED : SnapshotStatus.DEFAULT;
            if (range) drawRangeNode(graphics, snapshot, node, point, layout.nodeBoxes().get(node.id()), status, divide);
            else {
                String value = heap ? heapValue(node) : trie && node.id().equals(visible.rootId()) && node.label().equals("root") ? "ε" : node.label();
                RenderSupport.nodeCircle(graphics, point.x(), point.y(), 26.0,
                        RenderSupport.color(graphics, snapshot, status), value);
                if (heap) compactAnnotation(graphics, node.id(), point.x(), point.y() - 34.0);
            }
        }
        if (divide && !combining.isEmpty() && points.containsKey(combineParent)) {
            TreePoint parent = points.get(combineParent);
            for (String childId : combining) {
                Node child = visible.nodes().stream().filter(node -> node.id().equals(childId)).findFirst().orElseThrow();
                TreePoint childPoint = points.get(childId);
                RenderSupport.transfer(graphics, rangeValue(child), childPoint.x() - 58.0, childPoint.y() - 22.0,
                        parent.x() - 58.0, parent.y() - 22.0, 116.0, 44.0,
                        RenderSupport.color(graphics, snapshot, SnapshotStatus.DONE));
            }
        }
        drawFoldMarks(graphics, state, visible, layout.positions(), range ? 22.0 : 26.0);
        if (heap) drawExtracted(graphics, snapshot, facts, frame);
    }

    private static Map<String, TreePoint> movingPositions(GraphicsContext g, TreeState state,
            Map<String, TreePoint> targets, boolean heap, boolean trie, boolean range) {
        Map<String, TreePoint> result = new LinkedHashMap<>();
        Map<String, Integer> occurrences = new HashMap<>();
        for (Node node : state.nodes()) {
            String key = (trie ? "trie:" : range ? "range:" : "bst:") + node.id();
            if (heap) {
                String value = heapValue(node);
                key = "heap-value:" + value + ":" + occurrences.merge(value, 1, Integer::sum);
            }
            TreePoint target = targets.get(node.id());
            Point2D position = RenderSupport.movingPoint(g, key, target.x(), target.y());
            result.put(node.id(), new TreePoint(position.getX(), position.getY()));
        }
        return result;
    }

    private static LayoutFrame drawInput(GraphicsContext g, SimulationSnapshot snapshot, Map<String, String> facts,
                                         LayoutFrame frame, boolean trie, boolean divide) {
        String raw = facts.getOrDefault(trie ? "query" : divide ? "array" : "values", "");
        List<String> values = trie ? raw.codePoints().mapToObj(cp -> new String(Character.toChars(cp))).toList()
                : raw.replace("[", "").replace("]", "").isBlank() ? List.of()
                : List.of(raw.replace("[", "").replace("]", "").split(",\\s*"));
        if (values.isEmpty() || frame.contentHeight() < 180.0) return frame;
        int[] target = intervalBounds(facts.getOrDefault("query-range", facts.getOrDefault("current-range", "")));
        int selected = trie ? integer(facts.get("current-index"), -1) : target[0];
        boolean query = !divide && ("range-sum".equals(facts.get("operation")) || "range-min".equals(facts.get("operation")));
        String equation = query ? facts.getOrDefault("teaching-equation", "pending") : "pending";
        boolean combiningQuery = query && present(equation) && equation.contains("=")
                && !equation.contains("+=") && !equation.startsWith("Push");
        String trieResult = trie ? facts.getOrDefault("search-result", facts.getOrDefault("result", "pending")) : "pending";
        boolean showTrieResult = trie && present(trieResult);
        String trieResultLabel = switch (trieResult) {
            case "NOT_FOUND" -> "Not found";
            case "FOUND" -> "Found";
            case "YES" -> "Yes";
            case "NO" -> "No";
            case "INSERTED" -> "Inserted";
            case "DUPLICATE_IGNORED" -> "Already stored";
            default -> trieResult;
        };
        double resultWidth = combiningQuery ? Math.max(120.0, RenderSupport.textWidth(equation) + 28.0)
                : showTrieResult ? Math.max(120.0, RenderSupport.textWidth(trieResultLabel) + 28.0) : 92.0;
        double resultSpace = query || showTrieResult ? resultWidth + 32.0 : 0.0;
        int capacity = Math.max(1, (int) ((frame.contentWidth() - resultSpace - 40.0) / 48.0));
        int count = Math.min(values.size(), capacity);
        int start = Math.max(0, Math.min(values.size() - count, selected - count / 2));
        double left = frame.centerX() - (count * 48.0 + resultSpace) / 2.0;
        double top = frame.contentY() + 18.0;
        for (int offset = 0; offset < count; offset++) {
            int index = start + offset;
            double x = left + offset * 48.0;
            boolean active = trie ? index == selected : target[0] >= 0 && index >= target[0] && index <= target[1];
            compactAnnotation(g, Integer.toString(index), x + 22.0, top - 5.0);
            RenderSupport.nodeBox(g, x, top, 44.0, 44.0,
                    active ? RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE) : RenderSupport.palette(g).panel(), "");
            RenderSupport.centeredLabel(g, values.get(index), x + 22.0, top + 29.0,
                    active ? RenderSupport.palette(g).nodeText() : RenderSupport.palette(g).text());
        }
        if (start > 0) compactAnnotation(g, "‹", left - 15.0, top + 28.0);
        if (start + count < values.size()) compactAnnotation(g, "›", left + count * 48.0 + 12.0, top + 28.0);
        if (query) {
            double x = left + count * 48.0 + 28.0;
            String answer = facts.getOrDefault("answer", "pending");
            boolean number = answer.matches("-?\\d+|∞");
            String queryLabel = combiningQuery ? "query combine"
                    : "range-min".equals(facts.get("operation")) ? "query min" : "query sum";
            compactAnnotation(g, queryLabel, x + resultWidth / 2.0, top - 5.0);
            RenderSupport.nodeBox(g, x, top, resultWidth, 44.0, RenderSupport.color(g, snapshot,
                    number || combiningQuery ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT),
                    combiningQuery ? equation : number ? answer : "·");
        }
        if (showTrieResult) {
            double x = left + count * 48.0 + 28.0;
            boolean rejected = List.of("NOT_FOUND", "NO", "false").contains(trieResult);
            compactAnnotation(g, "result", x + resultWidth / 2.0, top - 5.0);
            RenderSupport.nodeBox(g, x, top, resultWidth, 44.0,
                    RenderSupport.color(g, snapshot, rejected ? SnapshotStatus.REJECTED : SnapshotStatus.DONE), trieResultLabel);
        }
        if (divide) compactAnnotation(g, "min | max", frame.centerX(), top + 67.0);
        return new LayoutFrame(frame.width(), frame.height(), frame.left(), frame.top() + (divide ? 104.0 : 82.0),
                frame.right(), frame.bottom());
    }

    private static void drawRangeNode(GraphicsContext g, SimulationSnapshot snapshot, Node node, TreePoint point,
                                      TreeNodeBox box, SnapshotStatus status, boolean divide) {
        compactAnnotation(g, rangeLabel(node), point.x(), point.y() - 30.0);
        RenderSupport.nodeBox(g, point.x() - box.width() / 2.0, point.y() - 22.0, box.width(), 44.0,
                RenderSupport.color(g, snapshot, status), rangeValue(node));
        int lazy = node.label().indexOf("lazy=");
        if (lazy >= 0) compactAnnotation(g, "lazy " + node.label().substring(lazy + 5).trim(), point.x(), point.y() + 42.0);
    }

    private static String rangeLabel(Node node) {
        String label = node.label();
        int end = label.indexOf(']');
        if (end < 0) return "";
        String interval = label.substring(label.indexOf('['), end + 1);
        return label.startsWith("a[") ? "a" + interval : interval;
    }

    private static String rangeValue(Node node) {
        int equals = node.label().indexOf('=');
        if (equals < 0) return "·";
        String value = node.label().substring(equals + 1).split(";", 2)[0].trim();
        return value.replace(Long.toString(Long.MAX_VALUE), "∞").replace("(", "").replace(")", "").replace(",", " | ");
    }

    private static int[] intervalBounds(String value) {
        if (value == null) return new int[] {-1, -1};
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[(\\d+),\\s*(\\d+)\\]").matcher(value);
        return matcher.find() ? new int[] {integer(matcher.group(1), -1), integer(matcher.group(2), -1)} : new int[] {-1, -1};
    }

    private static String heapValue(Node node) {
        int equals = node.label().indexOf('=');
        return equals < 0 ? node.label() : node.label().substring(equals + 1).trim();
    }

    private static void drawExtracted(GraphicsContext g, SimulationSnapshot snapshot, Map<String, String> facts, LayoutFrame frame) {
        String value = facts.get("extracted-value");
        if (!present(value)) return;
        double x = frame.centerX() - 32.0;
        double y = frame.contentY() + frame.contentHeight() - 44.0;
        compactAnnotation(g, "extracted", frame.centerX(), y - 10.0);
        int occurrence = 1;
        String heap = facts.getOrDefault("current-array", "[]").replace("[", "").replace("]", "");
        for (String item : heap.split(",\\s*")) if (item.trim().equals(value)) occurrence++;
        Point2D position = RenderSupport.movingPoint(g, "heap-value:" + value + ":" + occurrence, x + 32.0, y + 22.0);
        RenderSupport.nodeBox(g, position.getX() - 32.0, position.getY() - 22.0, 64.0, 44.0,
                RenderSupport.color(g, snapshot, SnapshotStatus.DONE), value);
    }

    private static void drawFoldMarks(GraphicsContext g, TreeState full, TreeState visible,
                                      Map<String, TreePoint> points, double halfHeight) {
        if (visible.nodes().isEmpty()) return;
        if (!java.util.Objects.equals(full.rootId(), visible.rootId())) {
            TreePoint point = points.get(visible.rootId());
            compactAnnotation(g, "…", point.x(), point.y() - halfHeight - 34.0);
        }
        Set<String> shown = points.keySet();
        Set<String> folded = new HashSet<>();
        for (Edge edge : full.edges()) if (shown.contains(edge.fromNodeId()) && !shown.contains(edge.toNodeId())) folded.add(edge.fromNodeId());
        for (String id : folded) {
            TreePoint point = points.get(id);
            compactAnnotation(g, "…", point.x(), point.y() + halfHeight + 36.0);
        }
    }

    static TreeState visibleTree(TreeState state, Set<String> activeIds, LayoutFrame frame) {
        if (state.nodes().isEmpty() || stateFits(state, frame)) return state;
        Map<String, String> parents = new HashMap<>();
        for (Edge edge : state.edges()) parents.put(edge.toNodeId(), edge.fromNodeId());
        Map<String, Integer> depths = layout(state, frame).depths();
        String focus = state.nodes().stream().map(Node::id).filter(activeIds::contains)
                .max(java.util.Comparator.comparingInt(id -> depths.getOrDefault(id, 0))).orElse(state.rootId());
        TreeState best = branch(state, focus, 0);
        String root = focus;
        while (root != null) {
            for (int depth = 1; depth <= state.nodes().size(); depth++) {
                TreeState candidate = branch(state, root, depth);
                if (candidate.nodes().stream().noneMatch(node -> node.id().equals(focus))) continue;
                if (stateFits(candidate, frame) && candidate.nodes().size() > best.nodes().size()) best = candidate;
                if (candidate.nodes().size() == state.nodes().size()) break;
            }
            root = parents.get(root);
        }
        return best;
    }

    private static boolean stateFits(TreeState state, LayoutFrame frame) {
        if (!fits(layout(state, frame), frame)) return false;
        Map<String, String> facts = factValues(state.facts());
        if (facts.containsKey("terminals") || facts.containsKey("node-interval") || facts.containsKey("minimum")) return true;
        boolean avl = facts.containsKey("height-by-node");
        double topMargin = avl || facts.containsKey("heap-size") ? 44.0 : 26.0;
        double bottomMargin = avl ? 48.0 : 26.0;
        List<TreePoint> positions = new ArrayList<>(compactBinaryPositions(state, frame).values());
        for (int i = 0; i < positions.size(); i++) {
            TreePoint point = positions.get(i);
            if (point.y() - topMargin < frame.contentY() || point.y() + bottomMargin > frame.contentY() + frame.contentHeight()) return false;
            for (int j = i + 1; j < positions.size(); j++) {
                TreePoint other = positions.get(j);
                if (Math.abs(point.y() - other.y()) < (avl ? 85.0 : 62.0)
                        && Math.abs(point.x() - other.x()) < (avl ? 84.0 : 62.0)) return false;
            }
        }
        return true;
    }

    private static TreeState branch(TreeState state, String root, int levels) {
        Set<String> included = new HashSet<>();
        included.add(root);
        Set<String> frontier = Set.of(root);
        for (int depth = 0; depth < levels && !frontier.isEmpty(); depth++) {
            Set<String> next = new HashSet<>();
            for (Edge edge : state.edges()) if (frontier.contains(edge.fromNodeId())) next.add(edge.toNodeId());
            included.addAll(next);
            frontier = next;
        }
        return subset(state, included, root);
    }

    private static TreeState subset(TreeState state, Set<String> ids, String root) {
        return new TreeState(state.nodes().stream().filter(node -> ids.contains(node.id())).toList(),
                state.edges().stream().filter(edge -> ids.contains(edge.fromNodeId()) && ids.contains(edge.toNodeId())).toList(), root, state.facts());
    }

    static TreeLayout layout(TreeState state, LayoutFrame frame) {
        if (state.nodes().isEmpty()) return new TreeLayout(Map.of(), Map.of(), Map.of(), Map.of());
        Map<String, String> facts = factValues(state.facts());
        boolean divide = facts.containsKey("minimum") && facts.containsKey("maximum");
        boolean range = divide || facts.containsKey("node-interval");
        boolean avl = facts.containsKey("height-by-node");
        double nodeWidth = divide ? 116.0 : range ? 84.0 : 52.0;
        double spacing = nodeWidth + (avl ? 44.0 : 28.0);
        double row = avl || range ? 100.0 : 88.0;
        Map<String, List<String>> children = new LinkedHashMap<>();
        for (Node node : state.nodes()) children.put(node.id(), new ArrayList<>());
        for (Edge edge : state.edges()) children.get(edge.fromNodeId()).add(edge.toNodeId());
        Map<String, Integer> depths = new LinkedHashMap<>();
        Map<String, Integer> widths = new LinkedHashMap<>();
        measure(state.rootId(), 0, children, depths, widths);
        int maximumDepth = depths.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (facts.containsKey("terminals")) {
            // Keep the whole word paths when fixed-size nodes and a readable gap fit.
            row = Math.min(88.0, Math.max(64.0, (frame.contentHeight() - 56.0) / Math.max(1, maximumDepth)));
        }
        double left = frame.centerX() - (widths.get(state.rootId()) - 1) * spacing / 2.0;
        double top = frame.centerY() - maximumDepth * row / 2.0;
        Map<String, TreePoint> points = new LinkedHashMap<>();
        place(state.rootId(), children, depths, widths, left, top, spacing, row, new int[] {0}, points);
        if (!range && !facts.containsKey("terminals")) points = compactBinaryPositions(state, frame);
        Map<String, TreeNodeBox> boxes = new LinkedHashMap<>();
        for (Node node : state.nodes()) boxes.put(node.id(), new TreeNodeBox(nodeWidth, range ? 44.0 : 52.0, List.of(node.label())));
        return new TreeLayout(Map.copyOf(points), Map.copyOf(depths), Map.copyOf(widths), Map.copyOf(boxes));
    }

    private static int measure(String id, int depth, Map<String, List<String>> children,
                               Map<String, Integer> depths, Map<String, Integer> widths) {
        depths.put(id, depth);
        int width = 0;
        for (String child : children.get(id)) width += measure(child, depth + 1, children, depths, widths);
        widths.put(id, Math.max(1, width));
        return Math.max(1, width);
    }

    private static void place(String id, Map<String, List<String>> children, Map<String, Integer> depths,
                               Map<String, Integer> widths, double left, double top, double spacing, double row,
                               int[] next, Map<String, TreePoint> points) {
        int start = next[0];
        for (String child : children.get(id)) place(child, children, depths, widths, left, top, spacing, row, next, points);
        if (children.get(id).isEmpty()) next[0]++;
        points.put(id, new TreePoint(left + (start + (widths.get(id) - 1) / 2.0) * spacing, top + depths.get(id) * row));
    }

    static boolean fits(TreeLayout layout, LayoutFrame frame) {
        for (Map.Entry<String, TreePoint> entry : layout.positions().entrySet()) {
            TreeNodeBox box = layout.nodeBoxes().get(entry.getKey());
            TreePoint point = entry.getValue();
            double topMargin = box.height() == 44.0 ? 44.0 : box.height() / 2.0;
            double bottomMargin = box.height() == 44.0 ? 50.0 : box.height() / 2.0;
            if (point.x() - box.width() / 2.0 < frame.contentX() || point.x() + box.width() / 2.0 > frame.contentX() + frame.contentWidth()
                    || point.y() - topMargin < frame.contentY() || point.y() + bottomMargin > frame.contentY() + frame.contentHeight()) return false;
        }
        return true;
    }

    private static Map<String, String> factValues(List<Fact> facts) {
        Map<String, String> values = new HashMap<>();
        for (Fact fact : facts) values.put(fact.key(), fact.value());
        return values;
    }

    private static Map<String, String> mapEntries(String raw) {
        Map<String, String> entries = new LinkedHashMap<>();
        if (raw == null) return entries;
        for (String entry : raw.replace("{", "").replace("}", "").split(",\\s*")) {
            String[] pair = entry.trim().split("\\s*[=:]\\s*", 2);
            if (pair.length == 2) entries.put(pair[0], pair[1]);
        }
        return entries;
    }

    private static int integer(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (RuntimeException ignored) { return fallback; }
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank() && !List.of("none", "null", "NIL", "-", "pending", "[]", "{}").contains(value);
    }

    static record TreePoint(double x, double y) { }
    static record TreeNodeBox(double width, double height, List<String> lines) { }
    static record TreeLayout(Map<String, TreePoint> positions, Map<String, Integer> depths,
                              Map<String, Integer> leafWidths, Map<String, TreeNodeBox> nodeBoxes) { }

    private static TreeState requireState(SimulationSnapshot snapshot) {
        if (snapshot == null || snapshot.state().rendererFamily() != RendererFamily.TREE) {
            throw new IllegalArgumentException("TreeCanvasRenderer requires TREE state");
        }
        return (TreeState) snapshot.state();
    }

    private static void renderCompactAvl(GraphicsContext graphics, SimulationSnapshot snapshot, TreeState state,
                                         Map<String, String> facts, LayoutFrame frame) {
        if (state.nodes().isEmpty()) return;
        Map<String, TreePoint> targets = compactBinaryPositions(state, frame);
        Map<String, TreePoint> points = new LinkedHashMap<>();
        for (Node node : state.nodes()) {
            TreePoint target = targets.get(node.id());
            // AVL rotations exchange payloads in structural slots; the unique key is the logical node identity.
            javafx.geometry.Point2D moving = RenderSupport.movingPoint(graphics, "avl-key:" + node.label(), target.x(), target.y());
            points.put(node.id(), new TreePoint(moving.getX(), moving.getY()));
        }
        Map<String, String> heights = mapEntries(facts.get("height-by-node"));
        Map<String, String> balances = mapEntries(facts.get("balance-by-node"));
        boolean rotating = facts.getOrDefault("phase", "").contains("rotat");
        Set<String> focused = new HashSet<>();
        if (present(facts.get("current-node"))) focused.add(facts.get("current-node"));
        if (rotating) {
            for (String key : List.of("rotation-root", "promoted-node", "middle-subtree")) {
                if (present(facts.get(key))) focused.add(facts.get(key));
            }
        }
        for (Edge edge : state.edges()) {
            TreePoint from = points.get(edge.fromNodeId());
            TreePoint to = points.get(edge.toNodeId());
            if (from == null || to == null) continue;
            if (Math.hypot(from.x() - to.x(), from.y() - to.y()) <= 48.0) continue;
            SnapshotStatus status = rotating && focused.contains(edge.fromNodeId()) && focused.contains(edge.toNodeId())
                    ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            RenderSupport.EdgeSegment segment = RenderSupport.clippedCircleSegment(from.x(), from.y(), to.x(), to.y(), 24.0, 24.0, 0.0);
            RenderSupport.line(graphics, segment.startX(), segment.startY(), segment.endX(), segment.endY(),
                    RenderSupport.edgeColor(graphics, snapshot, status), status == SnapshotStatus.ACTIVE ? 2.5 : 1.5);
        }
        for (Node node : state.nodes()) {
            TreePoint point = points.get(node.id());
            SnapshotStatus status = focused.contains(node.id()) ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            RenderSupport.nodeCircle(graphics, point.x(), point.y(), 24.0,
                    RenderSupport.color(graphics, snapshot, status), node.label());
            String metrics = "h=" + heights.getOrDefault(node.id(), "?")
                    + "  b=" + balances.getOrDefault(node.id(), "?");
            double metricsWidth = RenderSupport.secondaryTextWidth(metrics) + 8.0;
            graphics.setFill(RenderSupport.palette(graphics).background());
            graphics.fillRect(point.x() - metricsWidth / 2.0, point.y() + 26.0, metricsWidth, 22.0);
            compactAnnotation(graphics, metrics, point.x(), point.y() + 43.0);
            if (rotating) {
                String role = node.id().equals(facts.get("rotation-root")) ? "pivot"
                        : node.id().equals(facts.get("promoted-node")) ? "promote"
                        : node.id().equals(facts.get("middle-subtree")) ? "middle" : "";
                if (!role.isEmpty()) compactAnnotation(graphics, role, point.x(), point.y() - 34.0);
            }
        }
    }

    static Map<String, TreePoint> compactBinaryPositions(TreeState state, LayoutFrame frame) {
        if (state.nodes().isEmpty()) return Map.of();
        Map<String, List<Edge>> children = new LinkedHashMap<>();
        for (Node node : state.nodes()) children.put(node.id(), new ArrayList<>());
        for (Edge edge : state.edges()) children.get(edge.fromNodeId()).add(edge);
        Map<String, Integer> depths = new HashMap<>();
        List<String> pending = new ArrayList<>();
        pending.add(state.rootId());
        depths.put(state.rootId(), 0);
        for (int index = 0; index < pending.size(); index++) {
            String id = pending.get(index);
            for (Edge edge : children.get(id)) {
                depths.put(edge.toNodeId(), depths.get(id) + 1);
                pending.add(edge.toNodeId());
            }
        }
        int depth = depths.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        double span = Math.min(640.0, Math.max(48.0, frame.contentWidth() - 76.0));
        double row = Math.min(100.0, Math.max(80.0, (frame.contentHeight() - 90.0) / Math.max(1, depth)));
        double top = frame.centerY() - depth * row / 2.0 - 8.0;
        Map<String, String> facts = factValues(state.facts());
        if (!facts.containsKey("height-by-node")) {
            double topMargin = facts.containsKey("heap-size") ? 44.0 : 26.0;
            double bottomMargin = 26.0;
            row = Math.min(100.0, Math.max(64.0,
                    (frame.contentHeight() - topMargin - bottomMargin - 4.0) / Math.max(1, depth)));
            top = frame.centerY() - depth * row / 2.0 + (topMargin - bottomMargin) / 2.0;
        }
        Map<String, TreePoint> positions = new LinkedHashMap<>();
        positions.put(state.rootId(), new TreePoint(frame.centerX(), top));
        for (String id : pending) {
            List<Edge> branches = children.get(id);
            TreePoint parent = positions.get(id);
            for (int index = 0; index < branches.size(); index++) {
                Edge edge = branches.get(index);
                String label = edge.label().orElse("").toLowerCase(java.util.Locale.ROOT);
                boolean left = label.equals("left") || !label.equals("right") && index == 0;
                double offset = span / Math.pow(2.0, depths.get(id) + 2.0);
                positions.put(edge.toNodeId(), new TreePoint(parent.x() + (left ? -offset : offset), parent.y() + row));
            }
        }
        return Map.copyOf(positions);
    }

    private static void compactAnnotation(GraphicsContext graphics, String label, double x, double baseline) {
        RenderSupport.centeredSecondaryLabel(graphics, label, x, baseline, 0.0,
                RenderSupport.palette(graphics).secondaryText());
    }

}
