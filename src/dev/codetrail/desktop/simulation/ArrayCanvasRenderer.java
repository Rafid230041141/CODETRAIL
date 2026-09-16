package dev.codetrail.desktop.simulation;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Numbered-tile operations for array-based algorithms. */
public final class ArrayCanvasRenderer implements SimulationRenderer {
    private final ArrayRenderBounds renderBounds;

    /** Snapshot-only renderer used by standalone visual checks. */
    public ArrayCanvasRenderer() {
        this.renderBounds = null;
    }

    /** Renderer with fixed slot bounds from a complete array trace. */
    public ArrayCanvasRenderer(ArrayRenderBounds renderBounds) {
        this.renderBounds = Objects.requireNonNull(renderBounds, "renderBounds");
    }

    /** Convenience constructor for a precomputed trace profile. */
    public ArrayCanvasRenderer(SimulationTrace trace) {
        this(ArrayRenderBounds.fromTrace(trace));
    }

    @Override
    public RendererFamily family() { return RendererFamily.ARRAY; }

    @Override
    public void render(GraphicsContext graphics, SimulationSnapshot snapshot, LayoutFrame frame) {
        ArrayState state = requireState(snapshot);
        RenderSupport.clearAndFrame(graphics, frame);
        Map<String, String> facts = facts(state);
        if ("merge".equals(facts.get("renderer")) || facts.containsKey("split-low") || facts.containsKey("left-unread")) {
            renderMergeTiles(graphics, snapshot, state, frame, facts);
            return;
        }
        String type = renderBounds == null ? "" : renderBounds.simulationType();
        if (facts.containsKey("heap-boundary")) heapTiles(graphics, snapshot, state, frame, facts);
        else if (facts.containsKey("digit-pass") || facts.containsKey("frequency"))
            countingTiles(graphics, snapshot, state, frame, facts, facts.containsKey("digit-pass"));
        else if (facts.containsKey("buckets")) bucketTiles(graphics, snapshot, state, frame, facts);
        else if (facts.containsKey("capacity") && facts.containsKey("front")) queueTiles(graphics, snapshot, state, frame, facts);
        else if (facts.containsKey("block-size")) sqrtTiles(graphics, snapshot, state, frame, facts);
        else if (facts.containsKey("production")) productionTiles(graphics, snapshot, state, frame, facts);
        else if (facts.containsKey("partition")) quickTiles(graphics, snapshot, state, frame, facts);
        else if (facts.containsKey("shift") && facts.containsKey("length")) editTiles(graphics, snapshot, state, frame, facts);
        else if (facts.containsKey("target")) searchTiles(graphics, snapshot, state, frame, facts);
        else {
            TileLane row = tileLane(frame.centerX(), frame.centerY() - 22.0,
                    frame.contentWidth() - 70.0, Math.max(state.cells().size(), maximumSlots(state)), state.focusIndex());
            dataLane(graphics, snapshot, state, row, primaryTokens(state), state.cells(),
                    state.focusIndex(), "a", type.endsWith("SORT"), false);
        }
    }

    private static void renderMergeTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        int low = number(facts, "split-low", number(facts, "low", 0));
        int high = number(facts, "split-high", number(facts, "high", state.cells().size() - 1));
        int middle = number(facts, "split-mid", number(facts, "middle", low + (high - low) / 2));
        String phase = facts.getOrDefault("phase", "merge");
        boolean hasHalves = facts.containsKey("left-run") && facts.containsKey("right-run");
        double center = frame.centerX();
        double available = Math.max(64.0, frame.contentWidth() - 40.0);
        if (!hasHalves) {
            double parentY = frame.centerY() - ("split".equals(phase) ? 90.0 : 22.0);
            TileLane whole = tileLane(center, parentY, available, state.cells().size(), state.focusIndex());
            drawMergeLane(g, snapshot, state, whole, primaryTokens(state), 0, -1, -1,
                    0, "a");
            bracket(g, whole, low, high, -14.0, "[" + low + ".." + high + "]");
            if ("split".equals(phase) && low < high) {
                List<String> left = primaryTokens(state).subList(Math.max(0, low), Math.min(state.cells().size(), middle + 1));
                List<String> right = primaryTokens(state).subList(Math.max(0, middle + 1), Math.min(state.cells().size(), high + 1));
                double childrenWidth = Math.min(available, (left.size() + right.size()) * 70.0 + 40.0);
                double leftWidth = childrenWidth * left.size() / Math.max(1.0, left.size() + right.size());
                TileLane l = tileLane(center - childrenWidth / 2.0 + leftWidth / 2.0, parentY + 155.0,
                        leftWidth - 18.0, left.size(), 0);
                TileLane r = tileLane(center + leftWidth / 2.0, parentY + 155.0,
                        childrenWidth - leftWidth - 18.0, right.size(), 0);
                transferArrow(g, whole.centerAt(Math.max(low, Math.min(middle, high))), parentY + 72.0,
                        l.middleX(), l.y() - 38.0);
                transferArrow(g, whole.centerAt(Math.min(high, middle + 1)), parentY + 72.0,
                        r.middleX(), r.y() - 38.0);
                drawMergeLane(g, snapshot, state, l, left, low, -1, -1, 0, "L");
                drawMergeLane(g, snapshot, state, r, right, middle + 1, -1, -1, 0, "R");
                bracket(g, l, 0, left.size() - 1, -14.0, "[" + low + ".." + middle + "]");
                bracket(g, r, 0, right.size() - 1, -14.0, "[" + (middle + 1) + ".." + high + "]");
            }
            return;
        }
        List<String> left = tokens(facts.get("left-run"));
        List<String> right = tokens(facts.get("right-run"));
        // Merge-sort identities are original array indices; a run contains the
        // same contiguous original range even after its values have been sorted.
        low = java.util.stream.Stream.concat(left.stream(), right.stream())
                .mapToInt(token -> Integer.parseInt(token.substring(token.indexOf('@') + 1))).min().orElse(low);
        middle = low + left.size() - 1;
        high = middle + right.size();
        List<String> temporary = tokens(facts.get("temp"));
        int leftCursor = number(facts, "left-cursor", 0);
        int rightCursor = number(facts, "right-cursor", 0);
        int sourceIndex = number(facts, "source-index", -1);
        String sourceSide = facts.getOrDefault("source-side", "");
        int temporaryIndex = number(facts, "temp-index", -1);
        int copyIndex = number(facts, "copy-back-index", -1);
        int destinationIndex = number(facts, "output-index", -1);
        double top = frame.centerY() - 145.0;
        // Leave room for the range brackets in short projector canvases.
        double sourceTop = Math.max(top, 20.0 + RenderSupport.SECONDARY_FONT_SIZE + 4.0);
        double totalWidth = Math.min(available, (left.size() + right.size()) * 70.0 + 40.0);
        double leftWidth = totalWidth * left.size() / Math.max(1.0, left.size() + right.size());
        TileLane l = tileLane(center - totalWidth / 2.0 + leftWidth / 2.0, sourceTop,
                leftWidth - 18.0, left.size(), Math.max(leftCursor, sourceSide.equals("left") ? sourceIndex : -1));
        TileLane r = tileLane(center + leftWidth / 2.0, sourceTop,
                totalWidth - leftWidth - 18.0, right.size(), Math.max(rightCursor, sourceSide.equals("right") ? sourceIndex : -1));
        TileLane temp = tileLane(center, top + 123.0, available, left.size() + right.size(),
                copyIndex >= 0 ? copyIndex : temporaryIndex);
        TileLane destination = tileLane(center, top + 246.0, available, state.cells().size(), destinationIndex);
        int activeLeft = sourceSide.equals("left") ? sourceIndex : copyIndex < 0 ? leftCursor : -1;
        int activeRight = sourceSide.equals("right") ? sourceIndex : copyIndex < 0 ? rightCursor : -1;
        drawMergeLane(g, snapshot, state, l, left, low, activeLeft, -1, leftCursor, "L");
        drawMergeLane(g, snapshot, state, r, right, middle + 1, activeRight, -1, rightCursor, "R");
        drawMergeLane(g, snapshot, state, temp, temporary, 0,
                copyIndex >= 0 ? copyIndex : temporaryIndex, -1, 0, "temp");
        drawMergeLane(g, snapshot, state, destination, primaryTokens(state), 0,
                destinationIndex, -1, 0, "a");
        if (sourceIndex >= 0 && temporaryIndex >= 0) {
            TileLane source = sourceSide.equals("left") ? l : r;
            transferArrow(g, source.centerAt(sourceIndex), source.y() + 72.0,
                    temp.centerAt(temporaryIndex), temp.y() - 8.0);
            List<String> sourceValues = sourceSide.equals("left") ? left : right;
            if (sourceIndex < sourceValues.size()) RenderSupport.transfer(g,
                    sourceValues.get(sourceIndex).split("@", 2)[0], source.centerAt(sourceIndex) - 32.0, source.y(),
                    temp.centerAt(temporaryIndex) - 32.0, temp.y(), 64.0, 44.0,
                    RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
        } else if (copyIndex >= 0 && destinationIndex >= 0) {
            transferArrow(g, temp.centerAt(copyIndex), temp.y() + 72.0,
                    destination.centerAt(destinationIndex), destination.y() - 8.0);
            if (copyIndex < temporary.size()) RenderSupport.transfer(g, temporary.get(copyIndex).split("@", 2)[0],
                    temp.centerAt(copyIndex) - 32.0, temp.y(), destination.centerAt(destinationIndex) - 32.0,
                    destination.y(), 64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
        }
        bracket(g, l, 0, left.size() - 1, -14.0, "[" + low + ".." + middle + "]");
        bracket(g, r, 0, right.size() - 1, -14.0, "[" + (middle + 1) + ".." + high + "]");
    }

    static TileLane tileLane(double center, double y, double available, int count, int focus) {
        return tileLane(center, y, available, count, focus, 64.0);
    }

    private static TileLane tileLane(double center, double y, double available, int count, int focus, double tileWidth) {
        double pitch = tileWidth + 6.0;
        int visible = Math.max(1, Math.min(Math.max(1, count), (int) Math.floor((Math.max(tileWidth, available) + 6.0) / pitch)));
        int first = Math.max(0, Math.min(count - visible, Math.max(0, focus) - visible / 2));
        double width = visible * pitch - 6.0;
        return new TileLane(center - width / 2.0, y, first, visible, count, tileWidth);
    }

    private static void drawMergeLane(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            TileLane lane, List<String> values, int base, int active, int rejected, int consumed, String symbol) {
        RenderSupport.label(g, symbol, lane.x() - (symbol.equals("temp") ? 46.0 : 22.0), lane.y() + 28.0,
                RenderSupport.palette(g).secondaryText());
        if (lane.count() == 0) {
            RenderSupport.centeredLabel(g, "∅", lane.middleX(), lane.y() + 29.0, RenderSupport.palette(g).secondaryText());
            return;
        }
        for (int slot = 0; slot < lane.visible(); slot++) {
            int index = lane.first() + slot;
            double x = lane.x() + slot * 70.0;
            String token = index < values.size() ? values.get(index) : "_";
            boolean empty = token.equals("_");
            SnapshotStatus status = index == active ? SnapshotStatus.ACTIVE
                    : index == rejected ? SnapshotStatus.REJECTED
                    : symbol.equals("a") && index < state.cells().size()
                            && state.cells().get(index).status() == SnapshotStatus.DONE ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
            Color fill = empty || index < consumed && index != active
                    ? RenderSupport.palette(g).background() : RenderSupport.color(g, snapshot, status);
            RenderSupport.nodeBox(g, x, lane.y(), 64.0, 44.0, fill, "");
            if (!empty) {
                String value = token.split("@", 2)[0];
                RenderSupport.centeredLabel(g, value, x + 32.0, lane.y() + 29.0,
                        index < consumed && index != active ? RenderSupport.palette(g).secondaryText() : RenderSupport.palette(g).nodeText());
            }
            String position = Integer.toString(base + index);
            int separator = token.indexOf('@');
            if (separator >= 0) {
                String value = token.substring(0, separator);
                if (state.cells().stream().filter(cell -> cell.value().equals(value)).map(TypedCell::key).distinct().count() > 1)
                    position += "/#" + token.substring(separator + 1);
            }
            mergeSecondary(g, position, x + 32.0, lane.y() + 65.0, true);
        }
        if (lane.first() > 0) mergeSecondary(g, "…", lane.x() - 10.0, lane.y() + 56.0, true);
        if (lane.first() + lane.visible() < lane.count())
            mergeSecondary(g, "…", lane.x() + lane.visible() * 70.0 + 4.0, lane.y() + 56.0, true);
    }

    private static void mergeSecondary(GraphicsContext g, String text, double x, double y, boolean centered) {
        RenderSupport.secondaryLabel(g, text, centered ? x - RenderSupport.secondaryTextWidth(text) / 2.0 : x,
                y, RenderSupport.palette(g).secondaryText());
    }

    private static void bracket(GraphicsContext g, TileLane lane, int start, int end, double offset, String label) {
        if (start > end || end < lane.first() || start >= lane.first() + lane.visible()) return;
        double x1 = lane.x() + (Math.max(start, lane.first()) - lane.first()) * 70.0;
        double x2 = lane.x() + (Math.min(end, lane.first() + lane.visible() - 1) - lane.first()) * 70.0 + 64.0;
        double y = lane.y() + offset;
        Color color = RenderSupport.palette(g).outline();
        RenderSupport.line(g, x1, y + 6.0, x1, y, color, 1.5);
        RenderSupport.line(g, x1, y, x2, y, color, 1.5);
        RenderSupport.line(g, x2, y, x2, y + 6.0, color, 1.5);
        mergeSecondary(g, label, (x1 + x2) / 2.0, y - 6.0, true);
    }

    private static void transferArrow(GraphicsContext g, double x1, double y1, double x2, double y2) {
        Color color = RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE);
        RenderSupport.line(g, x1, y1, x2, y2, color, 2.0);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double length = 8.0;
        g.setFill(color);
        g.fillPolygon(new double[] {x2, x2 - length * Math.cos(angle - .5), x2 - length * Math.cos(angle + .5)},
                new double[] {y2, y2 - length * Math.sin(angle - .5), y2 - length * Math.sin(angle + .5)}, 3);
    }

    static record TileLane(double x, double y, int first, int visible, int count, double tileWidth) {
        double pitch() { return tileWidth + 6.0; }
        double centerAt(int index) { return x + (Math.max(first, Math.min(first + visible - 1, index)) - first) * pitch() + tileWidth / 2.0; }
        double middleX() { return x + (visible * pitch() - 6.0) / 2.0; }
    }

    private static Map<String, String> facts(ArrayState state) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Fact fact : state.facts()) result.put(fact.key(), fact.value());
        return result;
    }

    private static int number(Map<String, String> facts, String key, int fallback) {
        try { return Integer.parseInt(facts.getOrDefault(key, "")); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static List<String> tokens(String value) {
        if (value == null || value.equals("[]") || value.isBlank()) return List.of();
        String text = value.trim();
        if (text.startsWith("[") && text.endsWith("]")) text = text.substring(1, text.length() - 1);
        return text.isBlank() ? List.of() : java.util.Arrays.stream(text.split(",\\s*")).map(String::trim).toList();
    }

    private static List<String> primaryTokens(ArrayState state) {
        return state.cells().stream().map(cell -> cell.value() + "@" + cell.key()).toList();
    }

    private int maximumSlots(ArrayState state) {
        return renderBounds == null ? state.cells().size() : renderBounds.maximumCellCount();
    }

    private void dataLane(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state, TileLane lane,
            List<String> values, List<TypedCell> cells, int active, String symbol, boolean identities, boolean motion) {
        dataLane(g, snapshot, state, lane, values, cells, active, symbol, identities, motion, true);
    }

    private void dataLane(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state, TileLane lane,
            List<String> values, List<TypedCell> cells, int active, String symbol, boolean identities,
            boolean motion, boolean positions) {
        if (!symbol.isBlank()) RenderSupport.secondaryLabel(g, symbol,
                lane.x() - RenderSupport.secondaryTextWidth(symbol) - 12.0, lane.y() + 28.0,
                RenderSupport.palette(g).secondaryText());
        if (lane.count() == 0) {
            RenderSupport.centeredLabel(g, "∅", lane.middleX(), lane.y() + 29.0, RenderSupport.palette(g).secondaryText());
            return;
        }
        for (int slot = 0; slot < lane.visible(); slot++) {
            int index = lane.first() + slot;
            double x = lane.x() + slot * lane.pitch();
            String token = index < values.size() ? values.get(index) : "_";
            String[] parts = token.split("@", 2);
            String value = parts[0];
            boolean empty = value.equals("_") || value.equals("∅") || value.equals("null");
            SnapshotStatus status = index < cells.size() ? cells.get(index).status() : SnapshotStatus.DEFAULT;
            if (index == active && status != SnapshotStatus.DONE) status = SnapshotStatus.ACTIVE;
            Color color = empty ? RenderSupport.palette(g).background() : RenderSupport.color(g, snapshot, status);
            javafx.geometry.Point2D position = motion && parts.length == 2 && !empty
                    ? RenderSupport.movingPoint(g, "array:" + symbol + ":" + parts[1], x, lane.y())
                    : new javafx.geometry.Point2D(x, lane.y());
            RenderSupport.nodeBox(g, position.getX(), position.getY(), lane.tileWidth(), 44.0, color, empty ? "" : value);
            if (positions) {
                String marker = Integer.toString(index);
                boolean duplicate = renderBounds != null ? renderBounds.hasDuplicateValue(value)
                        : state.cells().stream().filter(cell -> cell.value().equals(value)).map(TypedCell::key).distinct().count() > 1;
                if (identities && duplicate && parts.length == 2) marker += "/#" + parts[1];
                mergeSecondary(g, marker, x + lane.tileWidth() / 2.0, lane.y() + 65.0, true);
            }
        }
        if (lane.first() > 0) mergeSecondary(g, "…", lane.x() - 9.0, lane.y() + 57.0, true);
        if (lane.first() + lane.visible() < lane.count())
            mergeSecondary(g, "…", lane.x() + lane.visible() * lane.pitch() + 4.0, lane.y() + 57.0, true);
    }

    private static Map<String, String> counts(String value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value == null) return result;
        for (String pair : value.replace("{", "").replace("}", "").split(",\\s*")) {
            String[] parts = pair.split("->|=", 2);
            if (parts.length == 2) result.put(parts[0].trim(), parts[1].trim());
        }
        return result;
    }

    private static void marker(GraphicsContext g, TileLane lane, int index, double y, String text) {
        if (index < lane.first() || index >= lane.first() + lane.visible() && index != lane.count()) return;
        double x = index == lane.count() ? lane.x() + lane.visible() * lane.pitch() : lane.centerAt(index);
        mergeSecondary(g, text, x, y, true);
        transferArrow(g, x, y + 7.0, x, lane.y() - 5.0);
    }

    private void searchTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        int current = facts.containsKey("middle") ? number(facts, "middle", -1) : state.focusIndex();
        int candidate = number(facts, "candidate", -1);
        int focus = current >= 0 ? current : candidate;
        TileLane row = tileLane(frame.centerX(), frame.centerY() - 15.0,
                frame.contentWidth() - 70.0, state.cells().size(), focus);
        dataLane(g, snapshot, state, row, primaryTokens(state), state.cells(), current, "a", false, false);
        RenderSupport.secondaryLabel(g, "target", frame.centerX() - 28.0, row.y() - 94.0,
                RenderSupport.palette(g).secondaryText());
        RenderSupport.nodeBox(g, frame.centerX() - 32.0, row.y() - 84.0, 64.0, 44.0,
                RenderSupport.color(g, snapshot, SnapshotStatus.DEFAULT), facts.get("target"));
        if (facts.containsKey("middle")) {
            int low = number(facts, "low", 0), high = number(facts, "high", state.cells().size());
            if (low < high) bracket(g, row, low, high - 1, 96.0, "[" + low + ", " + high + ")");
            else mergeSecondary(g, "[" + low + ", " + high + ") = ∅", row.middleX(), row.y() + 105.0, true);
            if (current >= 0) marker(g, row, current, row.y() - 15.0, "mid " + current);
            if (candidate >= 0 && candidate < state.cells().size()) {
                boolean visible = candidate >= row.first() && candidate < row.first() + row.visible();
                double x = visible ? row.centerAt(candidate) : row.middleX();
                if (visible) {
                    g.setStroke(RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE));
                    g.setLineWidth(2.5);
                    g.strokeRoundRect(x - 35.0, row.y() - 3.0, 70.0, 50.0, 8.0, 8.0);
                }
                mergeSecondary(g, "candidate " + candidate
                        + (visible ? "" : " = " + state.cells().get(candidate).value()), x, row.y() + 143.0, true);
            }
        } else if (current >= 0 && current < state.cells().size()) marker(g, row, current, row.y() - 15.0, "i " + current);
        String result = facts.getOrDefault("result", "");
        if (result.matches("-?\\d+")) mergeSecondary(g, "result = " + result,
                frame.centerX(), row.y() + 175.0, true);
    }

    private void quickTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        boolean placed = "pivot placed".equals(facts.get("phase"));
        int pivot = number(facts, "pivot-index", -1), scan = number(facts, "scan-index", -1);
        int boundary = number(facts, "boundary", 0), low = number(facts, "low", 0);
        int high = number(facts, "high", state.cells().size() - 1);
        TileLane row = tileLane(frame.centerX(), frame.centerY() - 20.0,
                frame.contentWidth() - 70.0, state.cells().size(), scan >= 0 ? scan : pivot);
        dataLane(g, snapshot, state, row, primaryTokens(state), state.cells(), placed ? -1 : scan,
                "a", true, true);
        if (pivot >= 0) marker(g, row, pivot, row.y() - 40.0, placed ? "pivot ✓" : "pivot");
        if (!placed && scan >= 0 && scan != pivot) marker(g, row, scan, row.y() - 12.0, "scan");
        if (!placed && boundary >= 0) {
            double x = boundary < row.count() ? row.centerAt(boundary) - 35.0 : row.x() + row.visible() * 70.0;
            RenderSupport.line(g, x, row.y() - 7.0, x, row.y() + 72.0,
                    RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE), 2.0);
            mergeSecondary(g, "b=" + boundary, x, row.y() + 129.0, true);
        }
        if (boundary > low) bracket(g, row, low, boundary - 1, 94.0, "≤ " + facts.get("pivot").split("@")[0]);
        int greaterStart = placed ? boundary + 1 : boundary;
        int greaterEnd = placed ? high : scan - 1;
        if (greaterEnd >= greaterStart) bracket(g, row, greaterStart, greaterEnd, 94.0,
                "> " + facts.get("pivot").split("@")[0]);
    }

    private void heapTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        int end = Math.max(0, Math.min(state.cells().size(), number(facts, "heap-end", state.cells().size())));
        int root = number(facts, "root-index", -1), child = number(facts, "child-index", -1);
        TileLane array = tileLane(frame.centerX(), frame.centerY() + 107.0,
                frame.contentWidth() - 70.0, state.cells().size(), Math.max(root, child));
        dataLane(g, snapshot, state, array, primaryTokens(state), state.cells(), -1, "a", true, true);
        if (end > 0) bracket(g, array, 0, end - 1, 91.0, "heap [0, " + end + ")");
        double[] x = new double[end], y = new double[end];
        List<Integer> visible = new ArrayList<>();
        if (state.cells().size() <= 15) {
            int levels = state.cells().isEmpty() ? 1 : 32 - Integer.numberOfLeadingZeros(state.cells().size());
            for (int i = 0; i < end; i++) {
                int level = 31 - Integer.numberOfLeadingZeros(i + 1);
                int slot = i - ((1 << level) - 1);
                double width = Math.min(frame.contentWidth() - 60.0, (1 << (levels - 1)) * 82.0);
                x[i] = frame.centerX() + (slot + .5 - (1 << level) / 2.0) * width / (1 << level);
                y[i] = frame.centerY() - 150.0 + level * 69.0;
                visible.add(i);
            }
        } else {
            int parent = root < 0 ? 0 : root;
            int[] indices = {parent, parent * 2 + 1, parent * 2 + 2};
            for (int slot = 0; slot < indices.length; slot++) if (indices[slot] < end) {
                int index = indices[slot];
                x[index] = frame.centerX() + (slot == 0 ? 0 : slot == 1 ? -104 : 104);
                y[index] = frame.centerY() + (slot == 0 ? -115 : -20);
                visible.add(index);
            }
        }
        for (int index : visible) if (index > 0 && visible.contains((index - 1) / 2)) {
            int parent = (index - 1) / 2;
            boolean comparedChild = parent == root && state.cells().get(index).status() == SnapshotStatus.ACTIVE;
            RenderSupport.line(g, x[parent], y[parent] + 44.0, x[index], y[index],
                    RenderSupport.palette(g).edge(comparedChild ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                    comparedChild ? 3.0 : 1.5);
        }
        for (int index : visible) {
            TypedCell cell = state.cells().get(index);
            SnapshotStatus status = cell.status();
            RenderSupport.movingBox(g, "heap:" + cell.key(), x[index] - 32.0, y[index], 64.0, 44.0,
                    RenderSupport.color(g, snapshot, status), cell.value());
            mergeSecondary(g, Integer.toString(index), x[index] + 40.0, y[index] + 27.0, true);
        }
    }

    private void countingTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts, boolean radix) {
        double top = frame.centerY() - 153.0;
        if (radix) {
            String context = "place " + facts.getOrDefault("place", "1");
            if (facts.containsKey("shifted-key")) context += "   " + facts.get("current-value")
                    + " + " + facts.getOrDefault("key-offset", "999") + " → " + facts.get("shifted-key")
                    + " → d=" + facts.get("digit");
            mergeSecondary(g, context, frame.centerX(), top - 17.0, true);
        }
        TileLane input = tileLane(frame.centerX(), top, frame.contentWidth() - 90.0,
                state.cells().size(), state.focusIndex());
        dataLane(g, snapshot, state, input, primaryTokens(state), state.cells(), state.focusIndex(), "a", true, false);
        Map<String, String> frequency = counts(facts.get("frequency")), ends = counts(facts.get("next-position"));
        List<String> keys = new ArrayList<>(frequency.keySet());
        for (String key : ends.keySet()) if (!keys.contains(key)) keys.add(key);
        if (!radix) for (TypedCell cell : state.cells()) if (!keys.contains(cell.value())) keys.add(cell.value());
        if (radix) for (int i = 0; i < 10; i++) if (!keys.contains(Integer.toString(i))) keys.add(Integer.toString(i));
        keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
        int keyIndex = keys.indexOf(facts.get(radix ? "digit" : "active-value"));
        // Decimal counters are a complete fixed domain, never an array window.
        // Each count is at most the 16-item input size, so 44px fits its exact value.
        TileLane keyRow = radix
                ? new TileLane(frame.centerX() - 247.0, top + 115.0, 0, 10, 10, 44.0)
                : tileLane(frame.centerX(), top + 115.0, frame.contentWidth() - 126.0, keys.size(), keyIndex);
        mergeSecondary(g, radix ? "digit" : "value", keyRow.x() - 40.0, keyRow.y() + 2.0, true);
        mergeSecondary(g, "count", keyRow.x() - 42.0, keyRow.y() + 34.0, true);
        mergeSecondary(g, "end", keyRow.x() - 34.0, keyRow.y() + 69.0, true);
        for (int slot = 0; slot < keyRow.visible() && keyRow.first() + slot < keys.size(); slot++) {
            int index = keyRow.first() + slot;
            String key = keys.get(index);
            double x = keyRow.x() + slot * keyRow.pitch();
            if (index == keyIndex) {
                g.setStroke(RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE)); g.setLineWidth(2.0);
                g.strokeRoundRect(x, keyRow.y() - 19.0, keyRow.tileWidth(), 97.0, 7.0, 7.0);
            }
            mergeSecondary(g, key, x + keyRow.tileWidth() / 2.0, keyRow.y() + 2.0, true);
            RenderSupport.centeredLabel(g, frequency.getOrDefault(key, "0"), x + keyRow.tileWidth() / 2.0, keyRow.y() + 34.0, RenderSupport.palette(g).text());
            RenderSupport.centeredLabel(g, ends.getOrDefault(key, "–"), x + keyRow.tileWidth() / 2.0, keyRow.y() + 69.0, RenderSupport.palette(g).text());
        }
        List<String> output = tokens(facts.get("output"));
        int outputIndex = number(facts, "output-index", -1);
        TileLane out = tileLane(frame.centerX(), top + 225.0, frame.contentWidth() - 90.0,
                state.cells().size(), outputIndex);
        dataLane(g, snapshot, state, out, output, List.of(), outputIndex, "out", true, false);
        if (outputIndex >= 0 && outputIndex < output.size() && state.focusIndex() >= 0
                && !output.get(outputIndex).equals("_") && !facts.containsKey("copy-back")) {
            if (keyIndex >= 0) {
                transferArrow(g, input.centerAt(state.focusIndex()), input.y() + 74.0,
                        keyRow.centerAt(keyIndex), keyRow.y() - 24.0);
                transferArrow(g, keyRow.centerAt(keyIndex), keyRow.y() + 83.0,
                        out.centerAt(outputIndex), out.y() - 8.0);
            }
            RenderSupport.transfer(g, output.get(outputIndex).split("@", 2)[0],
                    input.centerAt(state.focusIndex()) - 32.0, input.y(), out.centerAt(outputIndex) - 32.0, out.y(),
                    64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
        } else if (facts.containsKey("copy-back") && state.focusIndex() >= 0 && state.focusIndex() < output.size()) {
            int index = state.focusIndex();
            transferArrow(g, out.centerAt(index), out.y() - 5.0, input.centerAt(index), input.y() + 73.0);
            RenderSupport.transfer(g, output.get(index).split("@", 2)[0], out.centerAt(index) - 32.0, out.y(),
                    input.centerAt(index) - 32.0, input.y(), 64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
        }
    }

    private void bucketTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        TileLane input = tileLane(frame.centerX(), frame.centerY() - 157.0,
                frame.contentWidth() - 80.0, state.cells().size(), state.focusIndex());
        dataLane(g, snapshot, state, input, primaryTokens(state), state.cells(), state.focusIndex(), "a", true, false);
        String[] entries = facts.get("buckets").split(";\\s*");
        Map<String, String> ranges = new LinkedHashMap<>();
        for (String entry : facts.getOrDefault("bucket-ranges", "").split(";\\s*")) {
            String[] pair = entry.split(":", 2); if (pair.length == 2) ranges.put(pair[0], pair[1]);
        }
        int active = number(facts, "bucket-index", -1);
        int visible = Math.min(entries.length, 3);
        int first = Math.max(0, Math.min(entries.length - visible, Math.max(0, active) - visible / 2));
        TileLane activeLane = null;
        List<String> activeValues = List.of();
        for (int row = 0; row < visible; row++) {
            int index = first + row;
            String[] pair = entries[index].split(":", 2);
            List<String> values = pair.length == 2 ? tokens(pair[1]) : List.of();
            int capacity = renderBounds == null ? values.size() : renderBounds.bucketCapacity(index);
            TileLane bucket = tileLane(frame.centerX() + 60.0, frame.centerY() - 58.0 + row * 48.0,
                    frame.contentWidth() - 240.0, Math.max(1, capacity), values.size() - 1);
            String role = "B" + pair[0] + " " + ranges.getOrDefault(pair[0], "");
            int activeSlot = values.size() - 1;
            if (facts.containsKey("insertion-position")) activeSlot = number(facts, "insertion-position", -1)
                    + (facts.getOrDefault("active-bucket", "").contains("shift position") ? 1 : 0);
            else if (facts.getOrDefault("active-bucket", "").contains("concatenate")) {
                List<String> written = tokens(facts.get("output"));
                if (!written.isEmpty()) activeSlot = values.indexOf(written.get(written.size() - 1));
            }
            dataLane(g, snapshot, state, bucket, values, List.of(), index == active ? activeSlot : -1,
                    role, true, false, false);
            if (index == active) { activeLane = bucket; activeValues = values; }
        }
        if (first > 0 || first + visible < entries.length) mergeSecondary(g,
                "B" + first + "…B" + (first + visible - 1), frame.contentX() + 50.0,
                frame.centerY() - 69.0, true);
        List<String> output = tokens(facts.get("output"));
        TileLane out = tileLane(frame.centerX(), frame.centerY() + 114.0,
                frame.contentWidth() - 80.0, state.cells().size(), output.size() - 1);
        dataLane(g, snapshot, state, out, output, List.of(), output.size() - 1, "out", true, false);
        String phase = facts.getOrDefault("active-bucket", "");
        if (activeLane != null && !activeValues.isEmpty() && phase.contains("distribut") && state.focusIndex() >= 0) {
            int last = activeValues.size() - 1;
            transferArrow(g, input.centerAt(state.focusIndex()), input.y() + 73.0,
                    activeLane.centerAt(last), activeLane.y() - 5.0);
            RenderSupport.transfer(g, activeValues.get(last).split("@", 2)[0],
                    input.centerAt(state.focusIndex()) - 32.0, input.y(), activeLane.centerAt(last) - 32.0,
                    activeLane.y(), 64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
        }
        if (facts.containsKey("insertion-key") && activeLane != null) {
            int position = number(facts, "insertion-position", -1);
            String key = facts.get("insertion-key").split("@", 2)[0];
            mergeSecondary(g, "key " + key + " → " + position, frame.centerX(), frame.centerY() + 104.0, true);
            if (phase.contains("shift position") && position >= 0 && position + 1 < activeValues.size()) {
                transferArrow(g, activeLane.centerAt(position), activeLane.y() + 45.0,
                        activeLane.centerAt(position + 1), activeLane.y() + 45.0);
                RenderSupport.transfer(g, activeValues.get(position + 1).split("@", 2)[0],
                        activeLane.centerAt(position) - 32.0, activeLane.y(), activeLane.centerAt(position + 1) - 32.0,
                        activeLane.y(), 64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
            }
        }
        if (activeLane != null && phase.contains("concatenate") && !output.isEmpty()) {
            int index = activeValues.indexOf(output.get(output.size() - 1));
            if (index >= 0) {
                transferArrow(g, activeLane.centerAt(index), activeLane.y() + 45.0,
                        out.centerAt(output.size() - 1), out.y() - 5.0);
                RenderSupport.transfer(g, output.get(output.size() - 1).split("@", 2)[0],
                        activeLane.centerAt(index) - 32.0, activeLane.y(), out.centerAt(output.size() - 1) - 32.0,
                        out.y(), 64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
            }
        } else if (facts.containsKey("copy-back") && state.focusIndex() >= 0 && state.focusIndex() < output.size()) {
            int index = state.focusIndex();
            RenderSupport.transfer(g, output.get(index).split("@", 2)[0], out.centerAt(index) - 32.0,
                    out.y(), input.centerAt(index) - 32.0, input.y(), 64.0, 44.0,
                    RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
        }
    }

    private void queueTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        int count = state.cells().size(), front = number(facts, "front", 0), rear = number(facts, "rear", 0);
        TileLane row = tileLane(frame.centerX(), frame.centerY() - 36.0,
                frame.contentWidth() - 90.0, count, state.focusIndex());
        dataLane(g, snapshot, state, row, state.cells().stream().map(TypedCell::value).toList(), state.cells(),
                state.focusIndex(), "slots", false, false);
        marker(g, row, front, row.y() - 45.0, "front");
        marker(g, row, rear, row.y() - 14.0, "rear");
        double left = row.centerAt(0), right = row.centerAt(count - 1), y = row.y() + 99.0;
        RenderSupport.line(g, right, row.y() + 73.0, right, y, RenderSupport.palette(g).outline(), 1.5);
        RenderSupport.line(g, right, y, left, y, RenderSupport.palette(g).outline(), 1.5);
        transferArrow(g, left, y, left, row.y() + 73.0);
        mergeSecondary(g, "(" + (count - 1) + " + 1) mod " + count + " = 0", frame.centerX(), y + 29.0, true);
        mergeSecondary(g, "size " + facts.getOrDefault("size", "0") + "/" + facts.getOrDefault("capacity", "0"),
                frame.centerX(), row.y() + 164.0, true);
        String outcome = facts.getOrDefault("outcome", "");
        String value = facts.getOrDefault("value", "none");
        if (!value.equals("none") && !value.equals("∅") && (outcome.equals("write") || outcome.equals("read"))) {
            double cardY = row.y() - 139.0;
            RenderSupport.nodeBox(g, frame.centerX() - 32.0, cardY, 64.0, 44.0,
                    RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE), value);
            int index = outcome.equals("write") ? rear : front;
            RenderSupport.transfer(g, value, outcome.equals("write") ? frame.centerX() - 32.0 : row.centerAt(index) - 32.0,
                    outcome.equals("write") ? cardY : row.y(), outcome.equals("write") ? row.centerAt(index) - 32.0 : frame.centerX() - 32.0,
                    outcome.equals("write") ? row.y() : cardY, 64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
        }
    }

    private void sqrtTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        int size = Math.max(1, number(facts, "block-size", 1));
        int focus = number(facts, "focus-block", -1);
        TileLane row = tileLane(frame.centerX(), frame.centerY() - 68.0,
                frame.contentWidth() - 70.0, state.cells().size(), state.focusIndex());
        dataLane(g, snapshot, state, row, state.cells().stream().map(TypedCell::value).toList(), state.cells(),
                state.focusIndex(), "a", false, false);
        List<String> sums = tokens(facts.get("block-sums"));
        for (int block = 0; block < sums.size(); block++) {
            int first = block * size, last = Math.min(state.cells().size() - 1, first + size - 1);
            if (last < row.first() || first >= row.first() + row.visible()) continue;
            bracket(g, row, first, last, 91.0, "B" + block);
            double x = (row.centerAt(first) + row.centerAt(last)) / 2.0;
            RenderSupport.nodeBox(g, x - 32.0, row.y() + 106.0, 64.0, 44.0,
                    RenderSupport.color(g, snapshot, block == focus ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT), sums.get(block));
            if (block == focus && facts.getOrDefault("phase", "").equals("whole-block"))
                transferArrow(g, x, row.y() + 154.0, frame.centerX(), row.y() + 195.0);
        }
        String partial = facts.getOrDefault("partial-answer", "none");
        String operation = facts.getOrDefault("operation", "");
        String resultLabel = "";
        if (operation.equals("update")) {
            String target = facts.getOrDefault("target", "");
            if (target.startsWith("index ")) {
                int index = Integer.parseInt(target.substring(6));
                if (index >= 0 && index < state.cells().size())
                    resultLabel = "values[" + index + "] = " + state.cells().get(index).value();
            }
        } else if (facts.getOrDefault("phase", "").equals("complete")) {
            List<String> results = tokens(facts.get("operation-results"));
            if (!results.isEmpty()) {
                String last = results.get(results.size() - 1);
                if (last.startsWith("values[")) resultLabel = last;
                else if (last.matches("-?\\d+")) resultLabel = "Σ = " + last;
            }
        } else if (operation.equals("query") && !partial.equals("none") && !partial.equals("pending"))
            resultLabel = "Σ = " + partial;
        if (!resultLabel.isEmpty()) mergeSecondary(g,
                resultLabel, frame.centerX(), row.y() + 221.0, true);
        int left = number(facts, "query-left", -1), right = number(facts, "query-right", -1);
        if (left >= 0 && right >= left) bracket(g, row, left, right, -19.0, "[" + left + ".." + right + "]");
    }

    private void editTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        TileLane row = tileLane(frame.centerX(), frame.centerY() - 8.0,
                frame.contentWidth() - 70.0, maximumSlots(state), state.focusIndex());
        dataLane(g, snapshot, state, row, state.cells().stream().map(TypedCell::value).toList(), state.cells(),
                state.focusIndex(), "a", false, false);
        int editIndex = number(facts, "index", -1);
        marker(g, row, editIndex, row.y() - 16.0, "index " + editIndex);
        String operation = facts.getOrDefault("operation", "");
        String value = facts.getOrDefault("value", "none");
        if (operation.equals("insert") && !value.equals("none")) {
            double x = row.centerAt(Math.max(0, editIndex));
            RenderSupport.nodeBox(g, x - 32.0, row.y() - 108.0, 64.0, 44.0,
                    RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE), value);
            if (facts.getOrDefault("shift", "").equals("placed")) {
                transferArrow(g, x, row.y() - 61.0, x, row.y() - 4.0);
                RenderSupport.transfer(g, value, x - 32.0, row.y() - 108.0, x - 32.0, row.y(),
                        64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
            }
        }
        String[] shift = facts.getOrDefault("shift", "").split(" -> ");
        if (shift.length == 2) {
            int from = Integer.parseInt(shift[0]), to = Integer.parseInt(shift[1]);
            transferArrow(g, row.centerAt(from), row.y() + 88.0, row.centerAt(to), row.y() + 88.0);
            if (to < state.cells().size()) RenderSupport.transfer(g, state.cells().get(to).value(),
                    row.centerAt(from) - 32.0, row.y(), row.centerAt(to) - 32.0, row.y(),
                    64.0, 44.0, RenderSupport.color(g, snapshot, SnapshotStatus.ACTIVE));
        }
        mergeSecondary(g, "length " + facts.getOrDefault("length", "0"), frame.centerX(), row.y() + 125.0, true);
    }

    private void productionTiles(GraphicsContext g, SimulationSnapshot snapshot, ArrayState state,
            LayoutFrame frame, Map<String, String> facts) {
        double width = 64.0;
        for (TypedCell cell : state.cells()) width = Math.max(width, Math.min(260.0, RenderSupport.textWidth(cell.value()) + 18.0));
        int current = number(facts, "machine-index", state.focusIndex());
        TileLane machines = tileLane(frame.centerX(), frame.centerY() - 34.0,
                frame.contentWidth() - 90.0, state.cells().size(), current, width);
        dataLane(g, snapshot, state, machines, state.cells().stream().map(TypedCell::value).toList(), state.cells(),
                current, "t", false, false);
        mergeSecondary(g, "[" + facts.get("low") + ", " + facts.get("high") + "]",
                frame.centerX(), machines.y() - 86.0, true);
        String middle = facts.getOrDefault("middle", "-1");
        String answer = facts.getOrDefault("answer", "pending");
        if (!answer.equals("pending")) mergeSecondary(g, "answer " + answer,
                frame.centerX(), machines.y() - 51.0, true);
        else if (!middle.equals("-1")) mergeSecondary(g, "trial " + middle,
                frame.centerX(), machines.y() - 51.0, true);
        if (facts.containsKey("machine-time")) mergeSecondary(g,
                "⌊" + facts.get("middle") + "/" + facts.get("machine-time") + "⌋ = "
                        + facts.getOrDefault("capacity", facts.getOrDefault("contribution", "0")),
                frame.centerX(), machines.y() + 111.0, true);
        String production = facts.getOrDefault("production", "");
        String produced = production.split(" ", 2)[0];
        if (production.contains("cap = ")) mergeSecondary(g,
                "target " + production.substring(production.indexOf("cap = ") + 6),
                frame.centerX(), machines.y() + 153.0, true);
        else if (produced.contains("/")) mergeSecondary(g, "Σ " + produced + "   "
                + (facts.getOrDefault("predicate", "unknown").equals("true") ? "✓" : facts.getOrDefault("predicate", "unknown").equals("false") ? "✗" : "?"),
                frame.centerX(), machines.y() + 153.0, true);
    }


    private static ArrayState requireState(SimulationSnapshot snapshot) {
        if (snapshot == null || snapshot.state().rendererFamily() != RendererFamily.ARRAY) {
            throw new IllegalArgumentException("ArrayCanvasRenderer requires ARRAY state");
        }
        return (ArrayState) snapshot.state();
    }
}
