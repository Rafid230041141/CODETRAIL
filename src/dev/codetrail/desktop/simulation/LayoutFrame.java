package dev.codetrail.desktop.simulation;

/**
 * Explicit geometry for one canvas render. It is recomputed from the current
 * canvas dimensions, so custom input cardinality never changes the coordinate
 * contract or relies on stale positions.
 */
public final class LayoutFrame {
    private final double width;
    private final double height;
    private final double left;
    private final double top;
    private final double right;
    private final double bottom;

    public LayoutFrame(double width, double height, double left, double top, double right, double bottom) {
        requireDimension(width, "width");
        requireDimension(height, "height");
        if (left < 0 || top < 0 || right < 0 || bottom < 0) {
            throw new IllegalArgumentException("layout padding cannot be negative");
        }
        if (left + right >= width || top + bottom >= height) {
            throw new IllegalArgumentException("layout padding must leave a drawable area");
        }
        this.width = width;
        this.height = height;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static LayoutFrame forCanvas(double width, double height) {
        requireDimension(width, "width");
        requireDimension(height, "height");
        double horizontalPadding = Math.min(28.0, Math.max(8.0, width * 0.06));
        double verticalPadding = Math.min(28.0, Math.max(8.0, height * 0.08));
        // Tiny canvases are valid during JavaFX layout; leave at least one
        // drawable pixel on each axis while retaining explicit geometry.
        horizontalPadding = Math.min(horizontalPadding, Math.max(0.0, (width - 1.0) / 2.0));
        verticalPadding = Math.min(verticalPadding, Math.max(0.0, (height - 1.0) / 2.0));
        return new LayoutFrame(
                width,
                height,
                horizontalPadding,
                verticalPadding,
                horizontalPadding,
                verticalPadding);
    }

    public double width() { return width; }
    public double height() { return height; }
    public double left() { return left; }
    public double top() { return top; }
    public double right() { return right; }
    public double bottom() { return bottom; }
    public double contentX() { return left; }
    public double contentY() { return top; }
    public double contentWidth() { return width - left - right; }
    public double contentHeight() { return height - top - bottom; }
    public double centerX() { return left + contentWidth() / 2.0; }
    public double centerY() { return top + contentHeight() / 2.0; }

    private static void requireDimension(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
