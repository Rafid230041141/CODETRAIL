package dev.codetrail.desktop.simulation;

import javafx.scene.canvas.GraphicsContext;
import java.util.List;

/** Readable stack rows with a separate returned value; deep stacks follow the top. */
public final class StackCanvasRenderer implements SimulationRenderer {

    @Override
    public RendererFamily family() { return RendererFamily.STACK; }

    @Override
    public void render(GraphicsContext graphics, SimulationSnapshot snapshot, LayoutFrame frame) {
        StackState state = requireState(snapshot);
        RenderSupport.clearAndFrame(graphics, frame);
        renderCompactCalls(graphics, snapshot, state, frame);
    }

    private static void renderCompactCalls(GraphicsContext graphics, SimulationSnapshot snapshot,
                                           StackState state, LayoutFrame frame) {
        List<CallFrame> calls = state.frames();
        String finalResult = state.facts().stream().filter(fact -> fact.key().equals("result"))
                .reduce((left, right) -> right).map(Fact::value).orElse(null);
        boolean valuesOnly = calls.stream().allMatch(call -> call.functionName().equals("push"))
                && state.lastReturnedFrame().map(call -> call.functionName().equals("push")).orElse(true) && finalResult == null;
        int capacity = Math.max(1, (int) ((frame.contentHeight() - 32.0) / 52.0));
        int first = Math.max(0, calls.size() - capacity);
        double width = Math.min(valuesOnly ? 80.0 : 240.0, frame.contentWidth() * 0.52);
        double x = frame.centerX() - (width + 192.0) / 2.0;
        double bottom = frame.contentY() + frame.contentHeight() - 20.0;
        for (int index = first; index < calls.size(); index++) {
            CallFrame call = calls.get(index);
            double y = bottom - (index - first + 1) * 52.0;
            SnapshotStatus status = index == calls.size() - 1 && state.activeFrameIds().contains(call.id())
                    ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            String label = valuesOnly ? Integer.toString(call.argument()) : call.functionName() + "(" + call.argument() + ")"
                    + (call.hasResult() ? " = " + call.result() : "");
            RenderSupport.movingBox(graphics, "call:" + call.id(), x, y, width, 44.0,
                    RenderSupport.color(graphics, snapshot, status), label);
            if (index == calls.size() - 1) RenderSupport.secondaryLabel(graphics, "top", x - 42.0, y + 29.0,
                    RenderSupport.palette(graphics).secondaryText());
        }
        if (calls.isEmpty()) {
            boolean rejected = state.facts().stream().anyMatch(fact -> fact.key().equals("outcome") && fact.value().equals("EMPTY"));
            RenderSupport.line(graphics, x, bottom - 44.0, x, bottom, RenderSupport.palette(graphics).outline(), 1.5);
            RenderSupport.line(graphics, x, bottom, x + width, bottom, RenderSupport.palette(graphics).outline(), 1.5);
            RenderSupport.line(graphics, x + width, bottom, x + width, bottom - 44.0, RenderSupport.palette(graphics).outline(), 1.5);
            RenderSupport.centeredSecondaryLabel(graphics, "top → ∅", x, bottom - 12.0, width,
                    rejected ? RenderSupport.palette(graphics).edge(SnapshotStatus.REJECTED) : RenderSupport.palette(graphics).secondaryText());
        }
        if (first > 0) RenderSupport.centeredSecondaryLabel(graphics, "…", x, bottom + 8.0, width,
                RenderSupport.palette(graphics).secondaryText());
        double resultX = x + width + 72.0;
        double resultWidth = Math.min(120.0, frame.contentX() + frame.contentWidth() - resultX);
        if (state.lastReturnedFrame().isPresent()) {
            CallFrame returned = state.lastReturnedFrame().orElseThrow();
            double y = calls.isEmpty() ? frame.centerY() - 22.0 : bottom - (calls.size() - first) * 52.0;
            javafx.geometry.Point2D returnedPosition = RenderSupport.movingPoint(graphics, "call:" + returned.id(), resultX, y);
            RenderSupport.nodeBox(graphics, returnedPosition.getX(), returnedPosition.getY(), resultWidth, 44.0,
                    RenderSupport.color(graphics, snapshot, SnapshotStatus.DONE), returned.result());
            RenderSupport.secondaryLabel(graphics, valuesOnly ? "pop" : "return", returnedPosition.getX(), returnedPosition.getY() - 12.0,
                    RenderSupport.palette(graphics).secondaryText());
            if (!calls.isEmpty()) RenderSupport.directedSegment(graphics,
                    new RenderSupport.EdgeSegment(valuesOnly ? x + width + 8.0 : resultX - 8.0, y + 22.0,
                            valuesOnly ? resultX - 8.0 : x + width + 8.0, y + 22.0, 0.0, 0.0, 0.0),
                    RenderSupport.palette(graphics).edge(SnapshotStatus.ACTIVE), 2.0);
        } else if (calls.isEmpty() && finalResult != null) {
            double y = frame.centerY() - 22.0;
            String expression = state.facts().stream().map(Fact::key).filter(key -> key.startsWith("factorial("))
                    .reduce((left, right) -> right).orElse("result");
            RenderSupport.nodeBox(graphics, resultX, y, resultWidth, 44.0,
                    RenderSupport.color(graphics, snapshot, SnapshotStatus.DONE), finalResult);
            RenderSupport.secondaryLabel(graphics, expression, resultX, y - 12.0, RenderSupport.palette(graphics).secondaryText());
        } else if (!valuesOnly && !calls.isEmpty()) {
            CallFrame current = calls.get(calls.size() - 1);
            if (current.hasResult() && current.argument() > 1) {
                String childKey = current.functionName() + "(" + (current.argument() - 1) + ")";
                String childResult = state.facts().stream().filter(fact -> fact.key().equals(childKey))
                        .reduce((left, right) -> right).map(Fact::value).orElse(null);
                if (childResult != null) {
                    double y = bottom - (calls.size() - first) * 52.0;
                    RenderSupport.nodeBox(graphics, resultX, y, resultWidth, 44.0,
                            RenderSupport.color(graphics, snapshot, SnapshotStatus.DONE), childResult);
                    RenderSupport.secondaryLabel(graphics, "return", resultX, y - 12.0,
                            RenderSupport.palette(graphics).secondaryText());
                    RenderSupport.directedSegment(graphics, new RenderSupport.EdgeSegment(
                            resultX - 8.0, y + 22.0, x + width + 8.0, y + 22.0, 0.0, 0.0, 0.0),
                            RenderSupport.palette(graphics).edge(SnapshotStatus.ACTIVE), 2.0);
                    RenderSupport.centeredSecondaryLabel(graphics,
                            current.argument() + " × " + childResult + " = " + current.result(), x, y - 12.0, width,
                            RenderSupport.palette(graphics).text());
                    RenderSupport.transfer(graphics, childResult, resultX, y, x + width - resultWidth, y,
                            resultWidth, 44.0, RenderSupport.color(graphics, snapshot, SnapshotStatus.DONE));
                }
            }
        }
    }

    private static StackState requireState(SimulationSnapshot snapshot) {
        if (snapshot == null || snapshot.state().rendererFamily() != RendererFamily.STACK) {
            throw new IllegalArgumentException("StackCanvasRenderer requires STACK state");
        }
        return (StackState) snapshot.state();
    }
}
