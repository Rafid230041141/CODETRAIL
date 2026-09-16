package dev.codetrail.desktop.simulation;

import javafx.scene.canvas.GraphicsContext;

/** Renderer for one typed state family. */
public interface SimulationRenderer {
    RendererFamily family();

    void render(GraphicsContext graphics, SimulationSnapshot snapshot, LayoutFrame frame);
}
