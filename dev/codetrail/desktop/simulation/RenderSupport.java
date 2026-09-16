package dev.codetrail.desktop.simulation;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Point2D;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Package-private geometry helpers shared by the six canvas renderers. */
final class RenderSupport {
    private static final Object PALETTE_KEY = new Object();
    static final double LABEL_FONT_SIZE = 20.0;
    static final double SECONDARY_FONT_SIZE = 18.0;
    private static final Object MOTION_KEY = new Object();
    static final double LINE_HEIGHT = 26.0;
    private RenderSupport() {
    }

    /** Scope the palette to this canvas so simultaneous light and dark views do not interfere. */
    static Object useDarkMode(GraphicsContext graphics, boolean darkMode) {
        return graphics.getCanvas().getProperties().put(PALETTE_KEY,
                darkMode ? SimulationPalette.DARK : SimulationPalette.LIGHT);
    }

    static void restorePalette(GraphicsContext graphics, Object previous) {
        if (previous == null) graphics.getCanvas().getProperties().remove(PALETTE_KEY);
        else graphics.getCanvas().getProperties().put(PALETTE_KEY, previous);
    }

    static SimulationPalette palette(GraphicsContext graphics) {
        Object palette = graphics.getCanvas().getProperties().get(PALETTE_KEY);
        return palette instanceof SimulationPalette value ? value : SimulationPalette.DARK;
    }

    static Color color(GraphicsContext graphics, SimulationSnapshot snapshot, SnapshotStatus status) {
        Objects.requireNonNull(snapshot, "snapshot");
        return palette(graphics).fill(status);
    }

    static Color edgeColor(GraphicsContext graphics, SimulationSnapshot snapshot, SnapshotStatus status) {
        Objects.requireNonNull(snapshot, "snapshot");
        return palette(graphics).edge(status);
    }

    static void clearAndFrame(GraphicsContext graphics, LayoutFrame frame) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(frame, "frame");
        graphics.clearRect(0.0, 0.0, frame.width(), frame.height());
        graphics.setFill(palette(graphics).background());
        graphics.fillRect(0.0, 0.0, frame.width(), frame.height());
    }

    static void label(GraphicsContext graphics, String text, double x, double y, Color color) {
        graphics.setFill(color);
        graphics.setFont(Font.font("System", LABEL_FONT_SIZE));
        graphics.fillText(text, x, y);
    }

    static void secondaryLabel(GraphicsContext g, String text, double x, double y, Color color) {
        g.setFill(color);
        g.setFont(Font.font("System", SECONDARY_FONT_SIZE));
        g.fillText(text == null ? "" : text, x, y);
    }

    static double secondaryTextWidth(String text) {
        Text measurement = new Text(text == null ? "" : text);
        measurement.setFont(Font.font("System", SECONDARY_FONT_SIZE));
        return measurement.getLayoutBounds().getWidth();
    }

    static void centeredSecondaryLabel(GraphicsContext g, String text, double x, double y,
                                       double width, Color color) {
        secondaryLabel(g, text, x + (width - secondaryTextWidth(text)) / 2.0, y, color);
    }

    /** Per-frame anchors belong to one immutable trace transition, never to a global history. */
    static final class Motion {
        final Map<String, Point2D> source;
        final Map<String, Point2D> targets = new HashMap<>();
        final double progress;
        Motion(Map<String, Point2D> source, double progress) {
            this.source = source;
            this.progress = Math.max(0, Math.min(1, progress));
        }
    }

    static Object useMotion(GraphicsContext g, Motion motion) {
        return g.getCanvas().getProperties().put(MOTION_KEY, motion);
    }

    static void restoreMotion(GraphicsContext g, Object previous) {
        if (previous == null) g.getCanvas().getProperties().remove(MOTION_KEY);
        else g.getCanvas().getProperties().put(MOTION_KEY, previous);
    }

    static Point2D movingPoint(GraphicsContext g, String stableKey, double x, double y) {
        Point2D target = new Point2D(x, y);
        Object value = g.getCanvas().getProperties().get(MOTION_KEY);
        if (!(value instanceof Motion motion)) return target;
        motion.targets.put(stableKey, target);
        Point2D source = motion.source == null ? null : motion.source.get(stableKey);
        if (source == null) return target;
        double t = motion.progress * motion.progress * (3 - 2 * motion.progress);
        return new Point2D(source.getX() + (x - source.getX()) * t,
                source.getY() + (y - source.getY()) * t);
    }

    static void movingBox(GraphicsContext g, String stableKey, double x, double y,
                          double width, double height, Color color, String label) {
        Point2D position = movingPoint(g, stableKey, x, y);
        nodeBox(g, position.getX(), position.getY(), width, height, color, label);
    }

    /** Copy/return overlay: coordinates are tile top-lefts; originals remain in their slots. */
    static void transfer(GraphicsContext g, String label, double fromX, double fromY,
                         double toX, double toY, double width, double height, Color color) {
        Object value = g.getCanvas().getProperties().get(MOTION_KEY);
        if (!(value instanceof Motion motion) || motion.source == null || motion.progress >= 1) return;
        double t = motion.progress * motion.progress * (3 - 2 * motion.progress);
        nodeBox(g, fromX + (toX - fromX) * t, fromY + (toY - fromY) * t,
                width, height, color, label);
    }

    static void title(GraphicsContext graphics, String text, LayoutFrame frame) {
        label(graphics, text, frame.contentX(), Math.max(28.0, frame.contentY() - 10.0), palette(graphics).text());
    }

    static void nodeCircle(GraphicsContext graphics, double x, double y, double radius, Color color, String label) {
        graphics.setFill(color);
        graphics.fillOval(x - radius, y - radius, radius * 2.0, radius * 2.0);
        graphics.setStroke(palette(graphics).outline());
        graphics.strokeOval(x - radius, y - radius, radius * 2.0, radius * 2.0);
        graphics.setFill(palette(graphics).nodeText());
        graphics.setFont(Font.font("System", LABEL_FONT_SIZE));
        double textWidth = textWidth(label);
        graphics.fillText(label == null ? "" : label, x - textWidth / 2.0, y + 8.0);
    }

    static void nodeBox(GraphicsContext graphics, double x, double y, double width, double height, Color color, String label) {
        graphics.setFill(color);
        graphics.fillRoundRect(x, y, width, height, 8.0, 8.0);
        graphics.setStroke(palette(graphics).outline());
        graphics.strokeRoundRect(x, y, width, height, 8.0, 8.0);
        graphics.setFill(palette(graphics).nodeText());
        graphics.setFont(Font.font("System", LABEL_FONT_SIZE));
        graphics.fillText(label == null ? "" : label, x + (width - textWidth(label)) / 2.0, y + height / 2.0 + 8.0);
    }

    static void line(GraphicsContext graphics, double x1, double y1, double x2, double y2, Color color, double width) {
        graphics.setStroke(color);
        graphics.setLineWidth(width);
        graphics.strokeLine(x1, y1, x2, y2);
    }

    /**
     * Returns a segment parallel to the line between two node centres, with
     * its endpoints clipped to circular node boundaries. The normal and
     * route offset let reciprocal graph edges occupy separate lanes.
     */
    static EdgeSegment clippedCircleSegment(
            double sourceX,
            double sourceY,
            double targetX,
            double targetY,
            double sourceRadius,
            double targetRadius,
            double routeOffset) {
        requirePositiveFinite(sourceRadius, "sourceRadius");
        requirePositiveFinite(targetRadius, "targetRadius");
        double dx = targetX - sourceX;
        double dy = targetY - sourceY;
        double length = Math.hypot(dx, dy);
        if (!Double.isFinite(length) || length <= 1.0e-9) {
            throw new IllegalArgumentException("circle segment requires distinct node centres");
        }
        double normalX = -dy / length;
        double normalY = dx / length;
        double offset = boundedOffset(routeOffset, length, Math.min(sourceRadius, targetRadius));
        double routedSourceX = sourceX + normalX * offset;
        double routedSourceY = sourceY + normalY * offset;
        double routedTargetX = targetX + normalX * offset;
        double routedTargetY = targetY + normalY * offset;
        double routedDx = routedTargetX - routedSourceX;
        double routedDy = routedTargetY - routedSourceY;
        double routedLength = Math.hypot(routedDx, routedDy);
        double unitX = routedDx / routedLength;
        double unitY = routedDy / routedLength;
        double sourceInset = circleInset(sourceRadius, offset);
        double targetInset = circleInset(targetRadius, offset);
        return new EdgeSegment(
                routedSourceX + unitX * sourceInset,
                routedSourceY + unitY * sourceInset,
                routedTargetX - unitX * targetInset,
                routedTargetY - unitY * targetInset,
                normalX,
                normalY,
                offset);
    }

    /** Returns a segment clipped to axis-aligned rectangular node bounds. */
    static EdgeSegment clippedBoxSegment(
            double sourceX,
            double sourceY,
            double targetX,
            double targetY,
            double sourceHalfWidth,
            double sourceHalfHeight,
            double targetHalfWidth,
            double targetHalfHeight,
            double routeOffset) {
        requirePositiveFinite(sourceHalfWidth, "sourceHalfWidth");
        requirePositiveFinite(sourceHalfHeight, "sourceHalfHeight");
        requirePositiveFinite(targetHalfWidth, "targetHalfWidth");
        requirePositiveFinite(targetHalfHeight, "targetHalfHeight");
        double dx = targetX - sourceX;
        double dy = targetY - sourceY;
        double length = Math.hypot(dx, dy);
        if (!Double.isFinite(length) || length <= 1.0e-9) {
            throw new IllegalArgumentException("box segment requires distinct node centres");
        }
        double normalX = -dy / length;
        double normalY = dx / length;
        double offset = boundedOffset(routeOffset, length, Math.min(sourceHalfWidth, targetHalfWidth));
        double routedSourceX = sourceX + normalX * offset;
        double routedSourceY = sourceY + normalY * offset;
        double routedTargetX = targetX + normalX * offset;
        double routedTargetY = targetY + normalY * offset;
        double routedDx = routedTargetX - routedSourceX;
        double routedDy = routedTargetY - routedSourceY;
        double routedLength = Math.hypot(routedDx, routedDy);
        double unitX = routedDx / routedLength;
        double unitY = routedDy / routedLength;
        double sourceInset = rectangleInset(unitX, unitY, normalX, normalY, offset,
                sourceHalfWidth, sourceHalfHeight, true);
        double targetInset = rectangleInset(unitX, unitY, normalX, normalY, offset,
                targetHalfWidth, targetHalfHeight, false);
        return new EdgeSegment(
                routedSourceX + unitX * sourceInset,
                routedSourceY + unitY * sourceInset,
                routedTargetX - unitX * targetInset,
                routedTargetY - unitY * targetInset,
                normalX,
                normalY,
                offset);
    }

    static void directedSegment(GraphicsContext graphics, EdgeSegment segment, Color color, double width) {
        line(graphics, segment.startX(), segment.startY(), segment.endX(), segment.endY(), color, width);
        arrowhead(graphics, segment.endX(), segment.endY(),
                segment.startX(), segment.startY(), color);
    }

    /** Draws a compact loop marker beside a node; directed loops include an arrowhead. */
    static void selfLoop(GraphicsContext graphics, double centerX, double centerY, double nodeRadius,
                         Color color, boolean directed) {
        requirePositiveFinite(nodeRadius, "nodeRadius");
        double radius = nodeRadius * 0.78;
        double loopCenterX = centerX + nodeRadius * 0.86;
        double loopCenterY = centerY - nodeRadius * 0.88;
        double diameter = radius * 2.0;
        graphics.setStroke(color);
        graphics.setLineWidth(2.0);
        graphics.strokeOval(loopCenterX - radius, loopCenterY - radius, diameter, diameter);
        if (directed) {
            // Clockwise tangent on the upper-right arc, with the tip kept on
            // the loop path so the marker remains legible beside the node.
            double angle = -Math.PI / 4.0;
            double tipX = loopCenterX + radius * Math.cos(angle);
            double tipY = loopCenterY + radius * Math.sin(angle);
            double tangentX = -Math.sin(angle);
            double tangentY = Math.cos(angle);
            arrowhead(graphics, tipX, tipY, tipX - tangentX, tipY - tangentY, color);
        }
    }

    private static void arrowhead(GraphicsContext graphics, double tipX, double tipY,
                                  double tailX, double tailY, Color color) {
        double dx = tipX - tailX;
        double dy = tipY - tailY;
        double length = Math.hypot(dx, dy);
        if (!Double.isFinite(length) || length <= 1.0e-9) {
            return;
        }
        double unitX = dx / length;
        double unitY = dy / length;
        // The tip is clipped to the target boundary and the base trails it
        // along the segment, giving the triangle the correct direction. Nodes
        // are painted first by graph/linked renderers, so the cue stays clear.
        double baseX = tipX - unitX * 16.0;
        double baseY = tipY - unitY * 16.0;
        double normalX = -unitY;
        double normalY = unitX;
        graphics.setFill(color);
        graphics.fillPolygon(
                new double[] {tipX, baseX + normalX * 8.0, baseX - normalX * 8.0},
                new double[] {tipY, baseY + normalY * 8.0, baseY - normalY * 8.0},
                3);
    }

    private static double circleInset(double radius, double offset) {
        return Math.sqrt(Math.max(0.0, radius * radius - offset * offset));
    }

    private static double rectangleInset(
            double unitX,
            double unitY,
            double normalX,
            double normalY,
            double offset,
            double halfWidth,
            double halfHeight,
            boolean forward) {
        double directionX = forward ? unitX : -unitX;
        double directionY = forward ? unitY : -unitY;
        double xDistance = boundaryDistance(directionX, normalX * offset, halfWidth);
        double yDistance = boundaryDistance(directionY, normalY * offset, halfHeight);
        return Math.min(xDistance, yDistance);
    }

    private static double boundaryDistance(double direction, double startingCoordinate, double halfExtent) {
        if (Math.abs(direction) <= 1.0e-9) {
            return Double.POSITIVE_INFINITY;
        }
        double boundary = direction > 0.0 ? halfExtent : -halfExtent;
        return Math.max(0.0, (boundary - startingCoordinate) / direction);
    }

    private static double boundedOffset(double requested, double length, double nodeClearance) {
        if (!Double.isFinite(requested)) {
            throw new IllegalArgumentException("routeOffset must be finite");
        }
        double maximum = Math.min(length * 0.45, Math.max(0.0, nodeClearance * 0.8));
        return Math.max(-maximum, Math.min(maximum, requested));
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    static FactBandLayout factBandLayout(LayoutFrame frame, List<Fact> facts) {
        return factBandLayout(frame, facts, !Objects.requireNonNull(facts, "facts").isEmpty());
    }

    private static FactBandLayout factBandLayout(LayoutFrame frame, List<Fact> facts, boolean reserveFactBand) {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(facts, "facts");
        if (!reserveFactBand) {
            return new FactBandLayout(frame.contentX(), frame.contentY() + frame.contentHeight(),
                    frame.contentWidth(), 0.0, frame.contentY() + frame.contentHeight(), List.of());
        }
        double contentBottom = frame.contentY() + frame.contentHeight();
        double bandHeight = Math.min(frame.contentHeight(), 58.0);
        double bandTop = contentBottom - bandHeight;
        List<Fact> selected = presentationFacts(facts);
        String summary = "";
        double lineWidth = Math.max(1.0, frame.contentWidth() - 28.0);
        for (Fact fact : selected) {
            String phrase = factText(fact);
            String candidate = summary.isEmpty() ? phrase : summary + "   •   " + phrase;
            // Never silently discard the highest-priority operation because it needs wrapping.
            if (summary.isEmpty() || wrappedFactLines(candidate, lineWidth).size() <= 2) summary = candidate;
        }
        List<String> wrapped = wrappedFactLines(summary, lineWidth);
        List<FactLine> lines = new ArrayList<>();
        for (int index = 0; index < Math.min(2, wrapped.size()); index++) {
            String line = wrapped.get(index);
            if (index == 1 && wrapped.size() > 2) {
                while (!line.isEmpty() && textWidth(line + "…") > lineWidth)
                    line = line.substring(0, line.length() - 1);
                line = line.stripTrailing() + "…";
            }
            double baseline = wrapped.size() == 1 ? 36.0 : 24.0 + index * 28.0;
            lines.add(new FactLine(line, SnapshotStatus.DEFAULT,
                    bandTop + Math.min(baseline, bandHeight - 2.0)));
        }
        return new FactBandLayout(frame.contentX(), bandTop, frame.contentWidth(), bandHeight,
                bandTop + Math.min(36.0, bandHeight - 2.0), lines);
    }

    private static List<String> wrappedFactLines(String text, double width) {
        List<String> lines = new ArrayList<>();
        String remaining = text.strip();
        while (!remaining.isEmpty()) {
            if (textWidth(remaining) <= width) { lines.add(remaining); break; }
            int low = 1, high = remaining.length();
            while (low < high) {
                int middle = (low + high + 1) / 2;
                if (textWidth(remaining.substring(0, middle)) <= width) low = middle;
                else high = middle - 1;
            }
            int split = remaining.lastIndexOf(' ', low);
            if (split <= 0) split = low;
            lines.add(remaining.substring(0, split).stripTrailing());
            remaining = remaining.substring(split).stripLeading();
        }
        return lines;
    }

    /** Keep evaluated decisions and active scalar cues; structures have dedicated drawings. */
    static List<Fact> presentationFacts(List<Fact> facts) {
        List<String> decisions = List.of("decision", "equation", "calculation", "transition",
                "bit-transition", "comparison");
        List<String> preferred = List.of("operation", "current", "known-distance", "result",
                "answer", "query-result", "sum", "gcd", "max-flow", "total-weight", "match-count",
                "current-prime", "pivot", "target", "argument", "phase");
        List<Fact> selected = new ArrayList<>();
        for (String key : decisions) {
            facts.stream().filter(fact -> fact.key().equals(key))
                    .forEach(fact -> addPresentationFact(selected, fact));
        }
        facts.stream().filter(fact -> fact.status() == SnapshotStatus.ACTIVE)
                .forEach(fact -> addPresentationFact(selected, fact));
        for (String key : preferred) {
            facts.stream().filter(fact -> fact.key().equals(key))
                    .forEach(fact -> addPresentationFact(selected, fact));
        }
        facts.forEach(fact -> addPresentationFact(selected, fact));
        return List.copyOf(selected);
    }

    private static void addPresentationFact(List<Fact> selected, Fact fact) {
        if (selected.size() < 3 && !fact.key().equals("renderer") && readableScalar(fact.value())
                && selected.stream().noneMatch(current -> current.key().equals(fact.key()))) {
            selected.add(fact);
        }
    }

    private static boolean readableScalar(String value) {
        return value != null && !value.isBlank() && !value.equals("NIL") && !value.equals("none")
                && !value.startsWith("{") && !value.startsWith("[") && value.length() <= 96;
    }

    static String factText(Fact fact) {
        String key = switch (fact.key()) {
            case "known-distance" -> "Distance";
            case "current" -> "Current node";
            case "query-result" -> "Query result";
            case "max-flow" -> "Flow";
            case "match-count" -> "Matches";
            case "phase" -> "Step";
            default -> humanize(fact.key());
        };
        return key + ": " + (fact.key().equals("phase") ? humanize(fact.value()) : fact.value());
    }

    private static String humanize(String value) {
        String words = value.replaceAll("([a-z])([A-Z])", "$1 $2").replace('-', ' ').replace('_', ' ');
        return words.isBlank() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    static double textWidth(String value) {
        Text text = new Text(value == null ? "" : value);
        text.setFont(Font.font("System", LABEL_FONT_SIZE));
        return text.getLayoutBounds().getWidth();
    }

    static void centeredLabel(GraphicsContext graphics, String text, double x, double y, Color color) {
        label(graphics, text, x - textWidth(text) / 2.0, y, color);
    }

    static void edgeLabel(GraphicsContext graphics, String text, double x, double y) {
        double width = textWidth(text) + 18.0;
        graphics.setFill(palette(graphics).background());
        graphics.fillRoundRect(x - width / 2.0, y - 23.0, width, 32.0, 10.0, 10.0);
        centeredLabel(graphics, text, x, y, palette(graphics).text());
    }

    /**
     * Shrinks the primary drawing area above the shared fact band while
     * preserving the full canvas coordinates used to paint that band.
     */
    static LayoutFrame frameAboveFactBand(LayoutFrame frame, List<Fact> facts) {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(facts, "facts");
        return frameAboveFactBand(frame, !facts.isEmpty());
    }

    static LayoutFrame frameAboveFactBand(LayoutFrame frame, boolean reserveFactBand) {
        Objects.requireNonNull(frame, "frame");
        if (!reserveFactBand) {
            return frame;
        }
        FactBandLayout band = factBandLayout(frame, List.of(), true);
        if (band.height() <= 0.0 || band.top() <= frame.contentY() + 1.0) {
            return frame;
        }
        double newBottomPadding = frame.height() - band.top();
        if (frame.top() + newBottomPadding >= frame.height()) {
            return frame;
        }
        return new LayoutFrame(frame.width(), frame.height(), frame.left(), frame.top(),
                frame.right(), newBottomPadding);
    }

    static void facts(GraphicsContext graphics, SimulationSnapshot snapshot, List<Fact> facts, LayoutFrame frame) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(frame, "frame");
        if (facts.isEmpty()) {
            return;
        }
        FactBandLayout band = factBandLayout(frame, facts);
        graphics.setFill(palette(graphics).panel());
        graphics.fillRoundRect(band.left(), band.top(), band.width(), band.height(), 8.0, 8.0);
        for (FactLine line : band.lines()) {
            label(graphics, line.text(), band.left() + 14.0, line.y(), palette(graphics).text());
        }
    }

    /** Compatibility overload for callers that only have a graphics context. */
    static void facts(GraphicsContext graphics, SimulationSnapshot snapshot, List<Fact> facts, double x, double y) {
        Objects.requireNonNull(graphics, "graphics");
        if (facts.isEmpty()) {
            return;
        }
        if (graphics.getCanvas() == null || graphics.getCanvas().getWidth() <= 0.0
                || graphics.getCanvas().getHeight() <= 0.0) {
            throw new IllegalArgumentException("facts renderer requires a sized canvas or LayoutFrame");
        }
        facts(graphics, snapshot, facts,
                LayoutFrame.forCanvas(graphics.getCanvas().getWidth(), graphics.getCanvas().getHeight()));
    }

    static record EdgeSegment(double startX, double startY, double endX, double endY,
                              double normalX, double normalY, double routeOffset) {
        double midX() { return (startX + endX) / 2.0; }
        double midY() { return (startY + endY) / 2.0; }
    }

    static record FactLine(String text, SnapshotStatus status, double y) {
        FactLine {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(status, "status");
            if (!Double.isFinite(y)) {
                throw new IllegalArgumentException("fact line y must be finite");
            }
        }
    }

    static record FactBandLayout(double left, double top, double width, double height,
                                 double titleY, List<FactLine> lines) {
        FactBandLayout {
            lines = List.copyOf(lines);
        }
        double bottom() { return top + height; }
    }
}
