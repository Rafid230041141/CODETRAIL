package dev.codetrail.desktop.simulation;

import javafx.scene.canvas.GraphicsContext;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shared canvas dispatcher for all six renderer families. The map is explicit
 * and immutable after construction: an unregistered family is an error.
 */
public final class SimulationCanvasRenderer {
    private final Map<RendererFamily, SimulationRenderer> renderers;
    private boolean darkMode = true;
    private boolean projectorMode;

    public void setProjectorMode(boolean projectorMode) {
        this.projectorMode = projectorMode;
    }

    /** Applies to the next render; callers can redraw the same immutable step on a live theme change. */
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public SimulationCanvasRenderer() {
        this(defaultRenderers());
    }

    /**
     * Creates the standard dispatcher with ARRAY geometry profiled once from
     * the supplied complete trace. Other families retain their normal
     * snapshot-driven renderers.
     */
    public SimulationCanvasRenderer(SimulationTrace trace) {
        this(defaultRenderers(ArrayRenderBounds.fromTrace(trace)));
    }

    /** Creates the standard dispatcher with an already computed ARRAY profile. */
    public SimulationCanvasRenderer(ArrayRenderBounds arrayBounds) {
        this(defaultRenderers(Objects.requireNonNull(arrayBounds, "arrayBounds")));
    }

    private static java.util.List<SimulationRenderer> defaultRenderers() {
        return java.util.List.of(
                new ArrayCanvasRenderer(),
                new LinkedCanvasRenderer(),
                new TreeCanvasRenderer(),
                new GraphCanvasRenderer(),
                new TableCanvasRenderer(),
                new StackCanvasRenderer());
    }

    private static java.util.List<SimulationRenderer> defaultRenderers(ArrayRenderBounds arrayBounds) {
        return java.util.List.of(
                new ArrayCanvasRenderer(arrayBounds),
                new LinkedCanvasRenderer(),
                new TreeCanvasRenderer(),
                new GraphCanvasRenderer(),
                new TableCanvasRenderer(),
                new StackCanvasRenderer());
    }

    public SimulationCanvasRenderer(Collection<? extends SimulationRenderer> renderers) {
        Objects.requireNonNull(renderers, "renderers");
        EnumMap<RendererFamily, SimulationRenderer> registered = new EnumMap<>(RendererFamily.class);
        for (SimulationRenderer renderer : renderers) {
            Objects.requireNonNull(renderer, "renderer");
            RendererFamily family = Objects.requireNonNull(renderer.family(), "renderer.family()");
            if (registered.putIfAbsent(family, renderer) != null) {
                throw new IllegalArgumentException("duplicate renderer family: " + family);
            }
        }
        this.renderers = Map.copyOf(registered);
    }

    /** Return a new dispatcher with one explicitly registered family. */
    public SimulationCanvasRenderer withRenderer(SimulationRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        EnumMap<RendererFamily, SimulationRenderer> next = new EnumMap<>(RendererFamily.class);
        next.putAll(renderers);
        RendererFamily family = Objects.requireNonNull(renderer.family(), "renderer.family()");
        if (next.putIfAbsent(family, renderer) != null) {
            throw new IllegalArgumentException("duplicate renderer family: " + family);
        }
        SimulationCanvasRenderer nextRenderer = new SimulationCanvasRenderer(next.values());
        nextRenderer.setDarkMode(darkMode);
        nextRenderer.setProjectorMode(projectorMode);
        return nextRenderer;
    }

    public SimulationRenderer rendererFor(RendererFamily family) {
        Objects.requireNonNull(family, "family");
        SimulationRenderer renderer = renderers.get(family);
        if (renderer == null) {
            throw new IllegalArgumentException("no renderer registered for family: " + family);
        }
        return renderer;
    }

    public void render(GraphicsContext graphics, SimulationStep step, double width, double height) {
        Objects.requireNonNull(step, "step");
        render(graphics, step.snapshot(), LayoutFrame.forCanvas(width, height));
    }

    public void render(GraphicsContext graphics, SimulationSnapshot snapshot, double width, double height) {
        render(graphics, snapshot, LayoutFrame.forCanvas(width, height));
    }

    public void render(GraphicsContext graphics, SimulationSnapshot snapshot, LayoutFrame frame) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(frame, "frame");
        SimulationRenderer renderer = rendererFor(snapshot.state().rendererFamily());
        double scale = presentationScale(projectorMode);
        LayoutFrame logical = scale == 1.0 ? frame
                : LayoutFrame.forCanvas(frame.width() / scale, frame.height() / scale);
        graphics.save();
        Object previousPalette = RenderSupport.useDarkMode(graphics, darkMode);
        try {
            graphics.scale(scale, scale);
            renderer.render(graphics, snapshot, logical);
        } finally {
            RenderSupport.restorePalette(graphics, previousPalette);
            graphics.restore();
        }
    }
    /** Animate between recorded states; keyed objects preserve identity, including duplicate values. */
    public void renderTransition(GraphicsContext graphics, SimulationStep from, SimulationStep to,
                                 double width, double height, double progress) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!Double.isFinite(progress)) throw new IllegalArgumentException("progress must be finite");
        if (progress >= 1 || from == to) {
            render(graphics, to, width, height);
            return;
        }
        RenderSupport.Motion source = new RenderSupport.Motion(null, 1);
        Object previous = RenderSupport.useMotion(graphics, source);
        try {
            render(graphics, from, width, height);
            RenderSupport.useMotion(graphics, new RenderSupport.Motion(source.targets, progress));
            render(graphics, to, width, height);
        } finally {
            RenderSupport.restoreMotion(graphics, previous);
        }
    }

    /** Laptop geometry does not grow with the window; projector sizing is an explicit setting. */
    static double presentationScale(boolean projectorMode) {
        return projectorMode ? 1.2 : 1.0;
    }
}
