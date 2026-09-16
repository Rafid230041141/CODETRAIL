package dev.codetrail.desktop.simulation;

/** Immutable algorithm-space coordinate for graph nodes. */
public final class NodeCoordinate {
    private final double x;
    private final double y;

    public NodeCoordinate(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("node coordinates must be finite");
        }
        this.x = x;
        this.y = y;
    }

    public double x() { return x; }
    public double y() { return y; }
    public NodeCoordinate copy() { return new NodeCoordinate(x, y); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NodeCoordinate coordinate)) return false;
        return Double.compare(x, coordinate.x) == 0 && Double.compare(y, coordinate.y) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y);
    }
}
