package dev.codetrail.desktop.simulation;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Readable table/grid geometry. Large tables follow the active cell without reducing type. */
public final class TableCanvasRenderer implements SimulationRenderer {
    static final double FONT_SIZE = 20.0;
    private static final double LINE_HEIGHT = 26.0;
    private static final double MIN_ROW_HEIGHT = 38.0;
    private static final Pattern RANGE = Pattern.compile(
            "(?:text)?\\[\\s*(\\d+)\\s*(?:,|\\.\\.)\\s*(\\d+)\\s*\\](?:.*)?");
    private static final Pattern KNAPSACK_ITEM = Pattern.compile("item \\d+ \\(w=(-?\\d+),v=(-?\\d+)\\)");

    @Override
    public RendererFamily family() { return RendererFamily.TABLE; }

    @Override
    public void render(GraphicsContext graphics, SimulationSnapshot snapshot, LayoutFrame frame) {
        TableState state = requireState(snapshot);
        RenderSupport.clearAndFrame(graphics, frame);
        if (isFenwickTable(state)) {
            drawFenwickDiagram(graphics, snapshot, state, frame);
            return;
        }
        String algorithm = factValue(state, "algorithm");
        String renderer = factValue(state, "renderer");
        if (List.of("prefix", "sparse", "mo", "online").contains(renderer)) {
            drawRangeDiagram(graphics, snapshot, state, frame);
        } else if (isStringStrip(state) || "SUFFIX_ARRAY".equals(algorithm)) {
            drawStrings(graphics, snapshot, state, frame);
        } else if ("PASCAL_TRIANGLE".equals(algorithm)) {
            drawPascalTriangle(graphics, snapshot, state, frame);
        } else if ("GREEDY_ACTIVITY_SELECTION".equals(algorithm)) {
            drawGreedyIntervals(graphics, snapshot, state, frame);
        } else if (List.of("SIEVE_OF_ERATOSTHENES", "MODULAR_EXPONENTIATION", "EXTENDED_GCD").contains(algorithm)) {
            drawMathDiagram(graphics, snapshot, state, frame);
        } else if ("HASH_MAP".equals(algorithm)) {
            drawHashSlots(graphics, snapshot, state, frame);
        } else if ("adjacency-list".equals(factValue(state, "representation"))) {
            drawAdjacencyLists(graphics, snapshot, state, frame);
        } else {
            drawMatrixDiagram(graphics, snapshot, state, frame);
        }
    }

    /** A BIT cell is a covered interval, with its walk explained by the isolated bit. */
    private static void drawFenwickDiagram(GraphicsContext graphics, SimulationSnapshot snapshot,
                                           TableState state, LayoutFrame frame) {
        List<String> source = listValues(factValue(state, "values"));
        int count = Math.min(source.size(), state.rows().size());
        if (count == 0) {
            RenderSupport.centeredLabel(graphics, "No values", frame.centerX(), frame.centerY(),
                    RenderSupport.palette(graphics).secondaryText());
            return;
        }
        int current = integer(factValue(state, "bit-index"));
        boolean active = current > 0 && current <= count;
        int[] covered = range(factValue(state, "bit-range"));
        double minimumWidth = 48.0;
        for (int index = 0; index < count; index++) {
            minimumWidth = Math.max(minimumWidth, Math.max(RenderSupport.textWidth(source.get(index)),
                    RenderSupport.textWidth(state.rows().get(index).get(2).value())) + 20.0);
        }
        int visible = Math.min(count, Math.max(1, (int) (frame.contentWidth() / minimumWidth)));
        int start = windowStart(active ? current - 1 : 0, visible, count);
        if (covered[0] >= 0 && covered[1] < count && covered[1] - covered[0] + 1 <= visible) {
            start = Math.min(covered[0], Math.max(start, covered[1] - visible + 1));
        }
        double cellWidth = Math.min(92.0, frame.contentWidth() / visible);
        double origin = frame.centerX() - visible * cellWidth / 2.0;
        // Projector mode provides a shorter logical canvas. Pack the gaps and
        // tile padding, preserving the same 20/18 px type and all three bit rows.
        boolean compact = frame.height() < 420.0;
        boolean veryShort = frame.height() < 330.0;
        double top = compact ? 22.0 : Math.max(22.0, frame.contentY() - 20.0);
        double sourceY = top + (compact ? 25.0 : 28.0);
        double bitY = sourceY + (veryShort ? 94.0 : compact ? 100.0 : 138.0);
        double sourceHeight = compact ? 36.0 : 42.0;
        double bitHeight = compact ? 36.0 : 44.0;
        smallLabel(graphics, "Input · index 0…" + (count - 1), origin, top - 3.0);
        smallLabel(graphics, "BIT · index 1…" + count, origin, bitY + (compact ? 59.0 : 65.0));
        for (int offset = 0; offset < visible; offset++) {
            int index = start + offset;
            double x = origin + offset * cellWidth;
            SnapshotStatus sourceStatus = active && index >= covered[0] && index <= covered[1]
                    ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            smallCenteredLabel(graphics, Integer.toString(index), x + cellWidth / 2.0, sourceY - 7.0);
            fenwickTile(graphics, source.get(index), x, sourceY, cellWidth, sourceHeight,
                    RenderSupport.color(graphics, snapshot, sourceStatus));
            TypedCell sum = state.rows().get(index).get(2);
            smallCenteredLabel(graphics, Integer.toString(index + 1), x + cellWidth / 2.0, bitY - 8.0);
            fenwickTile(graphics, sum.value(), x, bitY, cellWidth, bitHeight,
                    RenderSupport.color(graphics, snapshot, sum.status()));
        }
        if (active && covered[0] >= 0) {
            double left = origin + Math.max(0, covered[0] - start) * cellWidth + 5.0;
            double right = origin + Math.min(visible, covered[1] - start + 1) * cellWidth - 5.0;
            double bracketY = sourceY + (veryShort ? 44.0 : compact ? 48.0 : 55.0);
            Color edge = RenderSupport.palette(graphics).edge(SnapshotStatus.ACTIVE);
            RenderSupport.line(graphics, left, bracketY - 7.0, left, bracketY, edge, 2.0);
            RenderSupport.line(graphics, left, bracketY, right, bracketY, edge, 2.0);
            RenderSupport.line(graphics, right, bracketY - 7.0, right, bracketY, edge, 2.0);
            smallCenteredLabel(graphics, "input [" + covered[0] + "…" + covered[1] + "]",
                    (left + right) / 2.0, bracketY + 20.0);
            double targetX = origin + (current - start - 0.5) * cellWidth;
            // Route beside the row label, then finish at the actual BIT tile edge.
            double bendX = targetX + cellWidth / 2.0 - 7.0;
            RenderSupport.line(graphics, (left + right) / 2.0, bracketY + 24.0,
                    bendX, bracketY + 24.0, edge, 1.5);
            arrow(graphics, bendX, bracketY + 24.0, bendX, bitY - 1.0, SnapshotStatus.ACTIVE);
        }
        double binaryY = bitY + (veryShort ? 64.0 : compact ? 72.0 : 96.0);
        if (active) {
            drawFenwickBits(graphics, state, frame, binaryY);
        }
        if (visible < count) {
            smallLabel(graphics, "Showing input " + start + "…" + (start + visible - 1)
                    + " · follows the active BIT cell", frame.contentX(), frame.height() - 12.0);
        }
    }

    private static void drawFenwickBits(GraphicsContext graphics, TableState state, LayoutFrame frame, double y) {
        String binary = factValue(state, "index-binary");
        String mask = factValue(state, "lowbit-binary");
        int index = integer(factValue(state, "bit-index"));
        if (!binary.matches("[01]+") || !mask.matches("[01]+")) return;
        int binaryMask = (1 << binary.length()) - 1;
        String negative = Integer.toBinaryString((-index) & binaryMask);
        negative = "0".repeat(Math.max(0, binary.length() - negative.length())) + negative;
        double labelWidth = 82.0;
        double bitWidth = 28.0;
        double bitStart = frame.contentX() + labelWidth;
        String[] labels = {"i = " + index, "−i", "i & −i"};
        String[] values = {binary, negative, mask};
        int isolated = binary.lastIndexOf('1');
        for (int row = 0; row < values.length; row++) {
            RenderSupport.label(graphics, labels[row], frame.contentX(), y + 23.0 + row * 30.0,
                    RenderSupport.palette(graphics).text());
            for (int bit = 0; bit < values[row].length(); bit++) {
                Color fill = bit == isolated ? RenderSupport.palette(graphics).fill(SnapshotStatus.ACTIVE)
                        : RenderSupport.palette(graphics).panel();
                double x = bitStart + bit * bitWidth;
                graphics.setFill(fill);
                graphics.fillRoundRect(x, y + row * 30.0, bitWidth - 3.0, 27.0, 5.0, 5.0);
                graphics.setFill(bit == isolated ? RenderSupport.palette(graphics).nodeText()
                        : RenderSupport.palette(graphics).text());
                graphics.setFont(Font.font("Monospaced", 20.0));
                graphics.fillText(values[row].substring(bit, bit + 1), x + 6.0, y + row * 30.0 + 21.0);
            }
        }
        double nextX = Math.max(frame.centerX() + 8.0, bitStart + binary.length() * bitWidth + 28.0);
        double available = frame.width() - frame.right() - nextX;
        if (available < 160.0) return;
        RenderSupport.label(graphics, "lowbit = " + factValue(state, "lowbit"), nextX, y + 23.0,
                RenderSupport.palette(graphics).text());
        String transition = factValue(state, "bit-transition");
        int next = integer(factValue(state, "next-bit-index"));
        if (transition.isBlank()) {
            String interval = factValue(state, "bit-range");
            smallLabel(graphics, interval + " → " + factValue(state, "lowbit") + " values", nextX, y + 51.0);
            return;
        }
        Color edge = RenderSupport.palette(graphics).edge(SnapshotStatus.ACTIVE);
        double arrowY = y + 51.0;
        RenderSupport.line(graphics, nextX, arrowY, nextX + 34.0, arrowY, edge, 2.0);
        RenderSupport.line(graphics, nextX + 34.0, arrowY, nextX + 26.0, arrowY - 5.0, edge, 2.0);
        RenderSupport.line(graphics, nextX + 34.0, arrowY, nextX + 26.0, arrowY + 5.0, edge, 2.0);
        RenderSupport.label(graphics, transition, nextX + 44.0, arrowY + 7.0,
                RenderSupport.palette(graphics).text());
        String direction = factValue(state, "next-index-binary") + "₂";
        if (next == 0) direction = "0 · stop";
        else if (next > state.rows().size()) direction = next + " > n=" + state.rows().size() + " · stop";
        smallLabel(graphics, direction, nextX, y + 83.0);
    }

    private static void fenwickTile(GraphicsContext graphics, String value, double x, double y,
                                    double width, double height, Color fill) {
        graphics.setFill(fill);
        graphics.fillRoundRect(x + 3.0, y, width - 6.0, height, 9.0, 9.0);
        RenderSupport.centeredLabel(graphics, value, x + width / 2.0, y + height / 2.0 + 7.0,
                RenderSupport.palette(graphics).nodeText());
    }

    private static void smallLabel(GraphicsContext graphics, String value, double x, double y) {
        RenderSupport.secondaryLabel(graphics, value, x, y, RenderSupport.palette(graphics).secondaryText());
    }

    private static void smallCenteredLabel(GraphicsContext graphics, String value, double x, double y) {
        RenderSupport.centeredSecondaryLabel(graphics, value, x, y, 0.0, RenderSupport.palette(graphics).secondaryText());
    }

    private static void drawRangeDiagram(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        String renderer = factValue(state, "renderer");
        double top = Math.max(26.0, frame.contentY() - 16.0);
        if ("prefix".equals(renderer)) {
            List<String> input = new ArrayList<>();
            List<String> prefix = new ArrayList<>();
            List<SnapshotStatus> statuses = new ArrayList<>();
            for (int row = 0; row < state.rows().size(); row++) {
                prefix.add(state.rows().get(row).get(2).value());
                statuses.add(state.rows().get(row).get(2).status());
                if (row > 0) input.add(state.rows().get(row).get(1).value());
            }
            int focus = focusedRowIndex(state);
            int left = integer(factValue(state, "prefix-left"));
            int right = integer(factValue(state, "prefix-right"));
            boolean query = List.of("query", "formula", "return", "complete").contains(factValue(state, "phase"));
            int first = query ? left : focus;
            int last = query ? right : focus;
            RibbonWindow window = ribbonWindow(prefix, frame.contentWidth() - 100.0, focus, first, last);
            double x = frame.contentX() + 100.0;
            smallLabel(g, "Input", frame.contentX(), top + 54.0);
            smallLabel(g, "Prefix", frame.contentX(), top + 141.0);
            for (int offset = 0; offset < window.count(); offset++) {
                int i = window.start() + offset;
                double cellX = x + offset * window.cellWidth();
                smallCenteredLabel(g, Integer.toString(i), cellX + window.cellWidth() / 2.0, top + 13.0);
                if (i < input.size()) {
                    SnapshotStatus status = query && i >= left && i < right ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
                    fenwickTile(g, input.get(i), cellX, top + 24.0, window.cellWidth(), 43.0,
                            RenderSupport.color(g, snapshot, status));
                }
                smallCenteredLabel(g, "P[" + i + "]", cellX + window.cellWidth() / 2.0, top + 104.0);
                fenwickTile(g, shortUnknown(prefix.get(i)), cellX, top + 114.0, window.cellWidth(), 43.0,
                        RenderSupport.color(g, snapshot, statuses.get(i)));
            }
            if (query && validRow(state, left) && validRow(state, right)) {
                drawOperandResult(g, frame, top + 217.0,
                        "P[" + right + "]", prefix.get(right), "−", "P[" + left + "]", prefix.get(left),
                        "Range sum", factValue(state, "answer"));
            } else if (focus > 0 && focus < prefix.size()) {
                drawOperandResult(g, frame, top + 217.0,
                        "P[" + (focus - 1) + "]", prefix.get(focus - 1), "+",
                        "input[" + (focus - 1) + "]", input.get(focus - 1), "P[" + focus + "]", prefix.get(focus));
            }
            windowLabel(g, window.start(), window.count(), prefix.size(), frame);
            return;
        }
        if ("sparse".equals(renderer)) {
            drawSparseBlocks(g, snapshot, state, frame);
            return;
        }
        List<String> input = listValues(factValue(state, "values"));
        int[] active = range(factValue(state, "window"));
        if (active[0] < 0) active = range(factValue(state, "target"));
        int focus = integer(factValue(state, "focus-index"));
        int viewFocus = focus >= 0 ? focus : active[0] >= 0 ? (active[0] + active[1]) / 2 : 0;
        List<SnapshotStatus> statuses = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) statuses.add(i == focus ? SnapshotStatus.ACTIVE
                : i >= active[0] && i <= active[1] ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT);
        drawRibbon(g, snapshot, input, statuses, frame.contentX(), top + 28.0,
                frame.contentWidth(), viewFocus, active[0], active[1], "Input");
        if ("mo".equals(renderer)) {
            String raw = factValue(state, "frequencies");
            String content = raw.length() > 1 ? raw.substring(1, raw.length() - 1) : "";
            List<String> labels = new ArrayList<>();
            List<String> values = new ArrayList<>();
            for (String entry : content.split(",\\s*")) {
                String[] pair = entry.trim().split("=");
                if (pair.length == 2) { labels.add(pair[0]); values.add(pair[1]); }
            }
            double cardsY = top + 150.0;
            int visible = Math.min(values.size(), Math.max(1, (int) (frame.contentWidth() / 90.0)));
            for (int i = 0; i < visible; i++) {
                valueCard(g, labels.get(i), values.get(i), frame.contentX() + i * 90.0, cardsY,
                        78.0, SnapshotStatus.DONE);
            }
            if (values.isEmpty()) smallLabel(g, "Window ∅", frame.contentX(), cardsY + 35.0);
            smallLabel(g, "Distinct = " + factValue(state, "distinct-count"), frame.contentX(), cardsY - 40.0);
            drawOperationCards(g, snapshot, state, frame, top + 279.0, "Query", 2);
        } else {
            String partial = factValue(state, "partial-answer");
            String operation = factValue(state, "operation");
            String phase = factValue(state, "phase");
            if ("query".equals(operation) && phase.startsWith("query") && known(partial)) {
                valueCard(g, "query-answer".equals(phase) ? "Range sum" : "Running sum", partial,
                        frame.contentX(), top + 140.0, Math.min(190.0, frame.contentWidth()),
                        "query-answer".equals(phase) ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE);
            } else if ("update".equals(operation) && phase.startsWith("update") && focus >= 0 && focus < input.size()) {
                valueCard(g, "values[" + focus + "]", input.get(focus), frame.contentX(), top + 140.0,
                        Math.min(190.0, frame.contentWidth()), SnapshotStatus.ACTIVE);
            }
            drawOperationCards(g, snapshot, state, frame, top + 255.0, "Operation", 3);
        }
    }

    private static void drawSparseBlocks(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        int n = state.rows().size();
        int levels = Math.max(0, state.columns().size() - 1);
        if (n == 0 || levels == 0) return;
        List<String> input = new ArrayList<>();
        for (List<TypedCell> row : state.rows()) input.add(row.get(1).value());
        int focus = Math.max(0, integer(factValue(state, "start")));
        int left = integer(factValue(state, "left-block"));
        int right = integer(factValue(state, "right-block"));
        RibbonWindow win = ribbonWindow(input, frame.contentWidth() - 65.0, focus, left, right);
        double x = frame.contentX() + 65.0;
        double top = Math.max(26.0, frame.contentY() - 16.0);
        int selectedLevel = integer(factValue(state, "level"));
        int visibleLevels = Math.min(levels, Math.max(1, (int) ((frame.contentHeight() - 50.0) / 64.0)));
        int levelStart = windowStart(Math.max(0, selectedLevel), visibleLevels, levels);
        for (int offset = 0; offset < win.count(); offset++) {
            smallCenteredLabel(g, Integer.toString(win.start() + offset), x + (offset + 0.5) * win.cellWidth(), top + 8.0);
        }
        for (int offset = 0; offset < visibleLevels; offset++) {
            int level = levelStart + offset;
            int length = 1 << level;
            double y = top + 23.0 + offset * 64.0;
            smallLabel(g, "2^" + level, frame.contentX(), y + 27.0);
            // Each row is a power-of-two level. A block's width is its exact
            // covered interval; overlapping query blocks use separate lanes.
            List<Integer> starts = new ArrayList<>();
            if (level == selectedLevel && left >= 0 && right >= 0) {
                starts.add(left);
                if (right != left) starts.add(right);
            } else if (level == selectedLevel && focus + length <= n) {
                starts.add(focus);
            } else if (level == selectedLevel - 1 && focus + 2 * length <= n) {
                starts.add(focus);
                starts.add(focus + length);
            } else {
                for (int start = win.start(); start < win.start() + win.count(); start += length) {
                    if (start + length <= n) starts.add(start);
                }
            }
            for (int number = 0; number < starts.size(); number++) {
                int start = starts.get(number);
                if (start < win.start() || start >= win.start() + win.count() || start + length > n) continue;
                double width = Math.min(length, win.start() + win.count() - start) * win.cellWidth();
                TypedCell cell = state.rows().get(start).get(level + 1);
                double laneY = y + (starts.size() == 2 && right - left < length && number == 1 ? 23.0 : 0.0);
                fenwickTile(g, shortUnknown(cell.value()), x + (start - win.start()) * win.cellWidth(), laneY,
                        width, 35.0, RenderSupport.color(g, snapshot, cell.status()));
            }
        }
        drawEquation(g, factValue(state, "teaching-equation"), frame, frame.height() - frame.bottom() - 12.0);
        windowLabel(g, win.start(), win.count(), n, frame);
    }

    private static void drawMatrixDiagram(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        boolean gaussian = "GAUSSIAN_ELIMINATION".equals(factValue(state, "algorithm"));
        boolean queens = "N_QUEENS".equals(factValue(state, "algorithm"));
        boolean dp = isKnapsackTable(state);
        List<Integer> solution = queens && isResult(state) ? firstQueenSolution(state) : List.of();
        List<String> selected = dp && isResult(state) ? listValues(factValue(state, "selected-items")) : List.of();
        List<String> targetBefore = gaussian ? listValues(factValue(state, "target-row-before")) : List.of();
        List<String> pivotValues = gaussian ? listValues(factValue(state, "pivot-row-values")) : List.of();
        List<String> targetAfter = gaussian ? listValues(factValue(state, "target-row-after")) : List.of();
        boolean rowOperation = !targetBefore.isEmpty() && targetBefore.size() == pivotValues.size()
                && targetBefore.size() == targetAfter.size();
        int totalColumns = Math.max(0, state.columns().size() - 1);
        if (state.rows().isEmpty() || totalColumns == 0) return;
        double bottomSpace = rowOperation ? 150.0 : queens ? 14.0 : 72.0;
        double rowLabelWidth = dp ? 116.0 : 48.0;
        double availableWidth = frame.contentWidth() - rowLabelWidth;
        double cellMinimum = queens ? 42.0 : 43.0;
        for (List<TypedCell> row : state.rows()) {
            for (int col = 1; col < row.size(); col++) cellMinimum = Math.max(cellMinimum,
                    Math.min(128.0, RenderSupport.textWidth(shortUnknown(row.get(col).value())) + 18.0));
        }
        int columns = Math.min(totalColumns, Math.max(1, (int) (availableWidth / cellMinimum)));
        int focusColumn = Math.max(0, focusedColumnIndex(state, focusedRowIndex(state)) - 1);
        int columnStart = windowStart(focusColumn, columns, totalColumns);
        double cellWidth = Math.min(queens ? 52.0 : 82.0, availableWidth / columns);
        double availableHeight = frame.contentHeight() - bottomSpace - 28.0;
        double rowHeight = queens ? Math.min(52.0, cellWidth) : 43.0;
        int rows = Math.min(state.rows().size(), Math.max(1, (int) (availableHeight / rowHeight)));
        int rowStart = matrixRowStart(state, rows);
        if (queens) rowHeight = Math.min(rowHeight, availableHeight / rows);
        double x = frame.centerX() - (columns * cellWidth + rowLabelWidth) / 2.0 + rowLabelWidth;
        double y = Math.max(28.0, frame.contentY() - 12.0) + 20.0;
        smallLabel(g, dp ? "i · w/v" : gaussian || queens ? "Row" : "From", x - rowLabelWidth, y - 12.0);
        for (int c = 0; c < columns; c++) {
            String label = state.columns().get(columnStart + c + 1);
            smallCenteredLabel(g, label, x + (c + 0.5) * cellWidth, y - 12.0);
        }
        for (int r = 0; r < rows; r++) {
            int rowIndex = rowStart + r;
            List<TypedCell> row = state.rows().get(rowIndex);
            String label = dp ? cellValue(state, rowIndex, 0).replace(" · w=", " · ").replace(" v=", "/")
                    : gaussian ? "R" + row.get(0).value() : row.get(0).value();
            boolean chosenItem = dp && selected.contains(Integer.toString(rowIndex - 1));
            if (chosenItem) {
                label = "✓ " + label;
                g.setFill(RenderSupport.palette(g).fill(SnapshotStatus.ACTIVE));
                g.fillRoundRect(x - rowLabelWidth - 4.0, y + r * rowHeight + 1.0, rowLabelWidth - 4.0, rowHeight - 5.0, 7.0, 7.0);
                RenderSupport.secondaryLabel(g, label, x - rowLabelWidth, y + r * rowHeight + rowHeight / 2.0 + 6.0,
                        RenderSupport.palette(g).nodeText());
            } else smallLabel(g, label, x - rowLabelWidth, y + r * rowHeight + rowHeight / 2.0 + 6.0);
            for (int c = 0; c < columns; c++) {
                int columnIndex = columnStart + c + 1;
                TypedCell cell = row.get(columnIndex);
                Color fill = RenderSupport.color(g, snapshot, cell.status());
                if (queens && cell.status() == SnapshotStatus.DEFAULT) fill = (rowIndex + columnIndex) % 2 == 0
                        ? RenderSupport.palette(g).panel() : RenderSupport.palette(g).header();
                boolean undoneQueen = queens && "undo".equals(factValue(state, "phase"))
                        && rowIndex == integer(factValue(state, "undo-row"))
                        && columnIndex - 1 == integer(factValue(state, "undo-column"));
                boolean savedQueen = !solution.isEmpty() && rowIndex < solution.size() && solution.get(rowIndex) == columnIndex - 1;
                if (savedQueen) fill = RenderSupport.palette(g).fill(SnapshotStatus.DONE);
                String value = queens ? !solution.isEmpty() ? savedQueen ? "♛" : ""
                        : cell.value().equals("Q") ? "♛" : cell.value().equals(".") ? "" : cell.value()
                        : shortUnknown(cell.value());
                fenwickTile(g, value, x + c * cellWidth, y + r * rowHeight, cellWidth, rowHeight - 3.0, fill);
                if (undoneQueen) {
                    g.setStroke(RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE));
                    g.setLineWidth(1.8);
                    g.strokeRoundRect(x + c * cellWidth + 3.0, y + r * rowHeight + 1.0,
                            cellWidth - 6.0, rowHeight - 5.0, 7.0, 7.0);
                    RenderSupport.centeredLabel(g, "×", x + (c + 0.5) * cellWidth,
                            y + (r + 0.5) * rowHeight + 6.0, RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE));
                }
                if (gaussian && columnIndex == state.columns().size() - 1) {
                    RenderSupport.line(g, x + c * cellWidth - 1.0, y + r * rowHeight,
                            x + c * cellWidth - 1.0, y + (r + 1) * rowHeight - 4.0, RenderSupport.palette(g).outline(), 1.5);
                }
            }
        }
        if (!queens) {
            int targetRow = focusedRowIndex(state);
            int targetColumn = focusedColumnIndex(state, targetRow);
            if ("FLOYD_WARSHALL".equals(factValue(state, "algorithm"))) {
                matrixArrow(g, state, rowStart, rows, columnStart + 1, columns, x, y, cellWidth, rowHeight,
                        integer(factValue(state, "operand-left-row")), integer(factValue(state, "operand-left-column")), targetRow, targetColumn);
                matrixArrow(g, state, rowStart, rows, columnStart + 1, columns, x, y, cellWidth, rowHeight,
                        integer(factValue(state, "operand-right-row")), integer(factValue(state, "operand-right-column")), targetRow, targetColumn);
            } else if (dp) {
                for (int r = rowStart; r < rowStart + rows; r++) {
                    if (r == targetRow) continue;
                    for (int c = columnStart + 1; c <= columnStart + columns; c++) {
                        if (state.rows().get(r).get(c).status() == SnapshotStatus.ACTIVE) {
                            matrixArrow(g, state, rowStart, rows, columnStart + 1, columns, x, y, cellWidth, rowHeight,
                                    r, c, targetRow, targetColumn);
                        }
                    }
                }
            }
            Matcher take = Pattern.compile("(-?\\d+) \\+ dp\\[(\\d+)\\]\\[(\\d+)\\] = -?\\d+ \\+ (-?\\d+) = (-?\\d+)")
                    .matcher(factValue(state, "take"));
            if (rowOperation) {
                drawGaussianOperation(g, state, frame, y + rows * rowHeight + 24.0,
                        targetBefore, pivotValues, targetAfter);
            } else if (dp && take.matches()) {
                drawOperandResult(g, frame, y + rows * rowHeight + 30.0,
                        "Item value", take.group(1), "+", "dp[" + take.group(2) + "][" + take.group(3) + "]",
                        take.group(4), "Take", take.group(5));
            } else drawEquation(g, factValue(state, "teaching-equation"), frame,
                    Math.min(frame.height() - frame.bottom() - 30.0, y + rows * rowHeight + 35.0));
        } else {
            if (!solution.isEmpty()) smallLabel(g, "Solution 1 / " + factValue(state, "solutions"),
                    x, y + rows * rowHeight + 29.0);
            int attackerRow = integer(factValue(state, "attacker-row"));
            int attackerColumn = integer(factValue(state, "attacker-column")) + 1;
            matrixArrow(g, state, rowStart, rows, columnStart + 1, columns, x, y, cellWidth, rowHeight,
                    attackerRow, attackerColumn, integer(factValue(state, "attempt-row")),
                    integer(factValue(state, "attempt-column")) + 1);
        }
        if (rows < state.rows().size() || columns < totalColumns) smallLabel(g,
                "Rows " + rowStart + "…" + (rowStart + rows - 1) + " · columns " + columnStart + "…" + (columnStart + columns - 1),
                frame.contentX(), frame.height() - 12.0);
    }

    private static void drawGaussianOperation(GraphicsContext g, TableState state, LayoutFrame frame, double y,
                                               List<String> before, List<String> pivot, List<String> after) {
        String target = "R" + factValue(state, "target-row");
        String[] labels = {target + " before", "−" + factValue(state, "elimination-factor") + " × R" + factValue(state, "pivot-row"),
                target + " after"};
        List<List<String>> values = List.of(before, pivot, after);
        double labelWidth = 125.0;
        double width = Math.min(91.0, (frame.contentWidth() - labelWidth) / before.size());
        double x = frame.contentX() + labelWidth;
        for (int row = 0; row < values.size(); row++) {
            double ry = y + row * 40.0;
            smallLabel(g, labels[row], frame.contentX(), ry + 25.0);
            for (int col = 0; col < before.size(); col++) {
                fenwickTile(g, values.get(row).get(col), x + col * width, ry, width, 33.0,
                        RenderSupport.palette(g).fill(row == 2 ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE));
            }
            if (row == 1) RenderSupport.line(g, x, ry + 37.0, x + before.size() * width, ry + 37.0,
                    RenderSupport.palette(g).outline(), 1.5);
        }
    }

    private static int matrixRowStart(TableState state, int rows) {
        int start = windowStart(focusedRowIndex(state), rows, state.rows().size());
        int low = state.rows().size(), high = -1;
        for (int i = 0; i < state.rows().size(); i++) {
            if (state.rows().get(i).stream().anyMatch(cell -> cell.status() == SnapshotStatus.ACTIVE)) {
                low = Math.min(low, i); high = i;
            }
        }
        if (high >= low && high - low + 1 <= rows) start = Math.min(low, Math.max(start, high - rows + 1));
        return start;
    }

    private static void matrixArrow(GraphicsContext g, TableState state, int rowStart, int rows, int colStart, int cols,
                                    double x, double y, double width, double height, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow < rowStart || fromRow >= rowStart + rows || toRow < rowStart || toRow >= rowStart + rows
                || fromCol < colStart || fromCol >= colStart + cols || toCol < colStart || toCol >= colStart + cols
                || fromRow == toRow && fromCol == toCol) return;
        double x1 = x + (fromCol - colStart + 0.5) * width;
        double y1 = y + (fromRow - rowStart + 0.5) * height;
        double x2 = x + (toCol - colStart + 0.5) * width;
        double y2 = y + (toRow - rowStart + 0.5) * height;
        RenderSupport.directedSegment(g, RenderSupport.clippedBoxSegment(x1, y1, x2, y2,
                width / 2.0 - 4.0, height / 2.0 - 4.0, width / 2.0 - 4.0, height / 2.0 - 4.0, 0.0),
                RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE), 2.0);
    }

    private static void drawStrings(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        if ("SUFFIX_ARRAY".equals(factValue(state, "algorithm"))) {
            drawSuffixRibbons(g, snapshot, state, frame);
            return;
        }
        boolean lps = isKmpTable(state) && "lps".equals(factValue(state, "phase"));
        String text = factValue(state, lps ? "pattern" : "text");
        String pattern = factValue(state, "pattern");
        int current = integer(factValue(state, lps ? "lps-index" : "i"));
        int compareLeft = integer(factValue(state, lps ? "compare-left-index" : "prefix-index"));
        int compareRight = integer(factValue(state, lps ? "compare-right-index" : "compare-index"));
        int textCompare = integer(factValue(state, "text-compare-index"));
        int patternCompare = integer(factValue(state, "pattern-compare-index"));
        int alignment = Math.max(0, integer(factValue(state, "alignment-start")));
        if (hasShiftedKmpAlignment(state)) {
            textCompare = integer(factValue(state, "i"));
            patternCompare = integer(factValue(state, "j"));
        }
        int[] activeWindow = range(factValue(state, "window"));
        List<Integer> verified = listValues(factValue(state, "matches")).stream().map(TableCanvasRenderer::integer)
                .filter(at -> at >= 0 && at + pattern.length() <= text.length()).toList();
        boolean result = isResult(state) && (isKmpTable(state) || isHashTable(state));
        if (result) {
            textCompare = -1;
            patternCompare = -1;
            activeWindow = new int[] {-1, -1};
            if (!verified.isEmpty()) {
                alignment = isKmpTable(state) ? verified.get(verified.size() - 1) : verified.get(0);
                current = alignment + pattern.length() / 2;
                activeWindow = new int[] {alignment, alignment + pattern.length() - 1};
            }
        }
        if (current < 0) current = textCompare >= 0 ? textCompare : activeWindow[0] >= 0 ? activeWindow[0] : 0;
        int available = Math.max(1, (int) ((frame.contentWidth() - 88.0) / 38.0));
        int visible = Math.min(text.length(), available);
        if (visible == 0) {
            smallLabel(g, "Text ∅", frame.contentX(), frame.centerY());
            return;
        }
        int start = windowStart(current, visible, text.length());
        double cellWidth = Math.min(44.0, (frame.contentWidth() - 88.0) / visible);
        double x = frame.contentX() + 88.0;
        double top = Math.max(28.0, frame.contentY() - 12.0);
        smallLabel(g, lps ? "Pattern" : "Text", frame.contentX(), top + 50.0);
        for (int offset = 0; offset < visible; offset++) {
            int index = start + offset;
            double cx = x + offset * cellWidth;
            smallCenteredLabel(g, Integer.toString(index), cx + cellWidth / 2.0, top + 8.0);
            boolean compared = lps || isZTable(state) ? index == compareLeft || index == compareRight : index == textCompare;
            boolean verifiedCharacter = result && verified.stream().anyMatch(at -> index >= at && index < at + pattern.length());
            SnapshotStatus status = verifiedCharacter ? SnapshotStatus.DONE : compared ? SnapshotStatus.ACTIVE
                    : !result && index >= activeWindow[0] && index <= activeWindow[1] ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
            fenwickTile(g, text.substring(index, index + 1), cx, top + 22.0, cellWidth, 44.0,
                    RenderSupport.color(g, snapshot, status));
        }
        if (isKmpTable(state)) {
            List<String> lpsValues = listValues(factValue(state, "lps"));
            smallLabel(g, lps ? "LPS" : "Pattern", frame.contentX(), top + 124.0);
            for (int offset = 0; offset < visible; offset++) {
                int index = start + offset;
                int at = lps ? index : index - alignment;
                if (at < 0 || at >= (lps ? lpsValues.size() : pattern.length())) continue;
                String value = lps ? lpsValues.get(at) : pattern.substring(at, at + 1);
                SnapshotStatus status = result && !verified.isEmpty() ? SnapshotStatus.DONE
                        : lps && (at == current || at == integer(factValue(state, "fallback-index")))
                        || !lps && at == patternCompare ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
                smallCenteredLabel(g, Integer.toString(at), x + (offset + 0.5) * cellWidth, top + 85.0);
                fenwickTile(g, value, x + offset * cellWidth, top + 98.0, cellWidth, 42.0,
                        RenderSupport.color(g, snapshot, status));
            }
            if (!lps) {
                List<SnapshotStatus> statuses = new ArrayList<>();
                for (int i = 0; i < lpsValues.size(); i++) statuses.add(result ? SnapshotStatus.DONE
                        : i == integer(factValue(state, "fallback-index")) ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT);
                drawRibbon(g, snapshot, lpsValues, statuses, frame.contentX(), top + 205.0,
                        frame.contentWidth(), Math.max(0, patternCompare), -1, -1, "LPS");
            }
            drawEquation(g, result ? "Matches = " + factValue(state, "matches") : factValue(state, "teaching-equation"), frame,
                    Math.min(frame.height() - frame.bottom() - 25.0, top + (lps ? 224.0 : 317.0)));
        } else if (isZTable(state)) {
            List<String> z = listValues(factValue(state, "z"));
            smallLabel(g, "Z[i]", frame.contentX(), top + 124.0);
            for (int offset = 0; offset < visible; offset++) {
                int i = start + offset;
                if (i >= z.size()) break;
                fenwickTile(g, shortUnknown(z.get(i)), x + offset * cellWidth, top + 98.0, cellWidth, 43.0,
                        RenderSupport.color(g, snapshot, i == current ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT));
            }
            if (activeWindow[0] >= 0) bracket(g, x + Math.max(0, activeWindow[0] - start) * cellWidth,
                    x + Math.min(visible, activeWindow[1] - start + 1) * cellWidth, top + 162.0,
                    "[l,r] = [" + activeWindow[0] + "," + activeWindow[1] + "]");
            drawEquation(g, factValue(state, "teaching-equation"), frame, top + 237.0);
        } else {
            List<String> patternChars = chars(pattern);
            drawRibbon(g, snapshot, patternChars,
                    result && !verified.isEmpty() ? java.util.Collections.nCopies(patternChars.size(), SnapshotStatus.DONE) : List.of(),
                    frame.contentX(), top + 123.0,
                    frame.contentWidth(), 0, -1, -1, "Pattern");
            if (result) {
                valueCard(g, "Verified starts", factValue(state, "matches"), frame.contentX(), top + 242.0,
                        Math.min(270.0, frame.contentWidth()), SnapshotStatus.DONE);
            } else {
                String windowHash = factValue(state, "window-hash");
                String patternHash = factValue(state, "pattern-hash");
                String comparison = known(windowHash) && known(patternHash)
                        ? windowHash.equals(patternHash) ? "=" : "≠" : "?";
                drawOperandResult(g, frame, top + 242.0, "Window hash", windowHash, comparison,
                        "Pattern hash", patternHash, "Verify", factValue(state, "verification"));
                drawEquation(g, factValue(state, "rolling-equation"), frame, frame.height() - frame.bottom() - 18.0);
            }
        }
        windowLabel(g, start, visible, text.length(), frame);
    }

    private static void drawSuffixRibbons(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        int count = state.rows().size();
        if (count == 0) return;
        double y = Math.max(28.0, frame.contentY() - 14.0) + 16.0;
        String inputText = factValue(state, "text");
        boolean inputOrder = !inputText.isEmpty() && state.rows().stream()
                .allMatch(row -> integer(row.get(1).value()) < 0);
        if (inputText.isEmpty()) {
            smallLabel(g, "Text ∅", frame.contentX(), y + 25.0);
            return;
        }
        boolean insertionComparison = "sort".equals(factValue(state, "phase"))
                && integer(factValue(state, "candidate")) >= 0
                && integer(factValue(state, "reference-start")) >= 0
                && integer(factValue(state, "comparison-offset")) >= 0;
        double rowHeight = inputOrder ? 42.0 : 48.0;
        double comparisonHeight = insertionComparison ? 108.0 : 0.0;
        int visible = Math.min(count, Math.max(1, (int) ((frame.contentHeight()
                - (inputOrder ? 38.0 : 74.0) - comparisonHeight) / rowHeight)));
        int start = inputOrder ? 0 : matrixRowStart(state, visible);
        int lcpRank = integer(factValue(state, "lcp-rank"));
        if (lcpRank > 0 && lcpRank < count && visible >= 2) {
            start = Math.min(lcpRank - 1, Math.max(start, lcpRank - visible + 1));
        }
        double origin = frame.contentX() + 96.0;
        double suffixWidth = frame.contentWidth() - 218.0;
        smallLabel(g, inputOrder ? "start" : "rank/start", frame.contentX(), y - 12.0);
        smallLabel(g, inputOrder ? "Input suffixes" : "Sorted suffixes", origin, y - 12.0);
        if (!inputOrder) smallLabel(g, "LCP", frame.width() - frame.right() - 75.0, y - 12.0);
        for (int index = start; index < start + visible; index++) {
            List<TypedCell> row = state.rows().get(index);
            double ry = y + (index - start) * rowHeight;
            smallLabel(g, inputOrder ? Integer.toString(index) : row.get(0).value() + " / " + row.get(1).value(),
                    frame.contentX(), ry + 27.0);
            String suffix = inputOrder && index < inputText.length() ? inputText.substring(index) : row.get(2).value();
            int characterCount = Math.max(1, (int) (suffixWidth / 26.0));
            int comparisonOffset = integer(factValue(state, "comparison-offset"));
            int charStart = windowStart(Math.max(0, comparisonOffset), Math.min(characterCount, suffix.length()), suffix.length());
            int shown = Math.min(characterCount, suffix.length());
            int prefixLength = lcpRank > 0 && lcpRank < count ? integer(state.rows().get(lcpRank).get(3).value()) : -1;
            for (int c = 0; c < shown; c++) {
                int at = charStart + c;
                boolean lcpPair = "lcp".equals(factValue(state, "phase")) && (index == lcpRank || index == lcpRank - 1);
                SnapshotStatus status = lcpPair ? at < prefixLength ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT : row.get(2).status();
                if (at == comparisonOffset && row.get(2).status() == SnapshotStatus.ACTIVE) status = SnapshotStatus.ACTIVE;
                fenwickTile(g, suffix.substring(at, at + 1), origin + c * 26.0, ry, 26.0, 37.0,
                        RenderSupport.color(g, snapshot, status));
            }
            if (shown < suffix.length()) smallLabel(g, "…", origin + shown * 26.0 + 3.0, ry + 26.0);
            if (!inputOrder) fenwickTile(g, shortUnknown(row.get(3).value()), frame.width() - frame.right() - 78.0,
                    ry, 73.0, 37.0, RenderSupport.color(g, snapshot, row.get(3).status()));
        }
        if (insertionComparison) drawSuffixComparison(g, snapshot, state, frame, y + visible * rowHeight + 9.0);
        windowLabel(g, start, visible, count, frame);
    }

    private static void drawSuffixComparison(GraphicsContext g, SimulationSnapshot snapshot, TableState state,
                                             LayoutFrame frame, double y) {
        String reference = factValue(state, "reference-suffix");
        String candidate = factValue(state, "candidate-suffix");
        int comparedOffset = integer(factValue(state, "comparison-offset"));
        String[] suffixes = {reference, candidate};
        String[] labels = {"Existing " + factValue(state, "reference-start"), "Candidate " + factValue(state, "candidate")};
        double labelWidth = 143.0;
        double cellWidth = 26.0;
        int length = Math.max(reference.length(), candidate.length()) + 1;
        int capacity = Math.max(1, (int) ((frame.contentWidth() - labelWidth - 73.0) / cellWidth));
        int shown = Math.min(capacity, length);
        int charStart = windowStart(Math.max(0, comparedOffset), shown, length);
        double origin = frame.contentX() + labelWidth;
        for (int row = 0; row < suffixes.length; row++) {
            String suffix = suffixes[row];
            double ry = y + row * 46.0;
            smallLabel(g, labels[row], frame.contentX(), ry + 26.0);
            for (int offset = 0; offset < shown; offset++) {
                int index = charStart + offset;
                double cx = origin + offset * cellWidth;
                if (index < suffix.length()) {
                    SnapshotStatus status = index < comparedOffset ? SnapshotStatus.DONE
                            : index == comparedOffset ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
                    fenwickTile(g, suffix.substring(index, index + 1), cx, ry, cellWidth, 35.0,
                            RenderSupport.color(g, snapshot, status));
                } else if (index == suffix.length()) {
                    RenderSupport.line(g, cx + 2.0, ry + 4.0, cx + 2.0, ry + 31.0,
                            RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE), 2.0);
                    smallLabel(g, "end", cx + 7.0, ry + 25.0);
                }
            }
            smallLabel(g, "len " + suffix.length(), frame.width() - frame.right() - 60.0, ry + 26.0);
        }
    }

    private static void drawPascalTriangle(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        int highest = -1;
        double minimum = 35.0;
        for (List<TypedCell> row : state.rows()) {
            highest = Math.max(highest, integer(row.get(0).value()));
            minimum = Math.max(minimum, RenderSupport.textWidth(shortUnknown(row.get(2).value())) + 17.0);
        }
        if (highest < 0) return;
        double rowHeight = 38.0;
        int visibleRows = Math.min(highest + 1, Math.max(1, (int) ((frame.contentHeight() - 30.0) / rowHeight)));
        int current = integer(factValue(state, "current-row"));
        int currentColumn = integer(factValue(state, "current-column"));
        int rowStart = windowStart(Math.max(0, current), visibleRows, highest + 1);
        int columns = Math.max(1, (int) ((frame.contentWidth() - 45.0) / minimum));
        int colStart = windowStart(Math.max(0, currentColumn), Math.min(columns, highest + 1), highest + 1);
        double top = Math.max(28.0, frame.contentY() - 14.0);
        for (int r = rowStart; r < rowStart + visibleRows; r++) {
            int count = Math.min(r + 1, columns);
            int first = Math.min(Math.max(0, r + 1 - count), colStart);
            double x = frame.centerX() - count * minimum / 2.0;
            smallLabel(g, "n=" + r, frame.contentX(), top + (r - rowStart) * rowHeight + 26.0);
            for (List<TypedCell> cellRow : state.rows()) {
                if (integer(cellRow.get(0).value()) != r) continue;
                int c = integer(cellRow.get(1).value());
                if (c < first || c >= first + count) continue;
                TypedCell value = cellRow.get(2);
                fenwickTile(g, shortUnknown(value.value()), x + (c - first) * minimum,
                        top + (r - rowStart) * rowHeight, minimum, 33.0,
                        RenderSupport.color(g, snapshot, value.status()));
            }
            if (r == current && currentColumn > 0 && currentColumn < r && r > rowStart && r + 1 <= columns) {
                double childX = frame.centerX() - (r + 1) * minimum / 2.0 + (currentColumn + 0.5) * minimum;
                double parentY = top + (r - rowStart - 1) * rowHeight + 33.0;
                double childY = top + (r - rowStart) * rowHeight;
                arrow(g, childX - minimum / 2.0, parentY, childX - 5.0, childY, SnapshotStatus.ACTIVE);
                arrow(g, childX + minimum / 2.0, parentY, childX + 5.0, childY, SnapshotStatus.ACTIVE);
            }
        }
        if (visibleRows <= highest || columns <= highest) smallLabel(g,
                "Rows " + rowStart + "…" + (rowStart + visibleRows - 1) + " · follows C(" + current + "," + currentColumn + ")",
                frame.contentX(), frame.height() - 12.0);
    }

    private static void drawGreedyIntervals(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        int minimum = 0, maximum = 1;
        for (List<TypedCell> row : state.rows()) {
            minimum = Math.min(minimum, integer(row.get(1).value()));
            maximum = Math.max(maximum, integer(row.get(2).value()));
        }
        double x = frame.contentX() + 50.0;
        double width = frame.contentWidth() - 60.0;
        double top = Math.max(30.0, frame.contentY() - 12.0);
        double rowHeight = 40.0;
        int visible = Math.min(state.rows().size(), Math.max(1, (int) ((frame.contentHeight() - 52.0) / rowHeight)));
        boolean noCandidate = integer(factValue(state, "target-row")) < 0
                && state.rows().stream().flatMap(List::stream).noneMatch(cell -> cell.status() == SnapshotStatus.ACTIVE);
        int start = noCandidate && !isResult(state) ? 0 : matrixRowStart(state, visible);
        double unit = width / Math.max(1, maximum - minimum);
        int tickStep = Math.max(1, (int) Math.ceil((maximum - minimum) / 10.0));
        for (int time = minimum; time <= maximum; time += tickStep) {
            double tx = x + (time - minimum) * unit;
            RenderSupport.line(g, tx, top + 15.0, tx, top + 22.0 + visible * rowHeight,
                    RenderSupport.palette(g).panel(), 1.0);
            smallCenteredLabel(g, Integer.toString(time), tx, top + 5.0);
        }
        for (int offset = 0; offset < visible; offset++) {
            List<TypedCell> row = state.rows().get(start + offset);
            double y = top + 22.0 + offset * rowHeight;
            double left = x + (integer(row.get(1).value()) - minimum) * unit;
            double length = Math.max(10.0, (integer(row.get(2).value()) - integer(row.get(1).value())) * unit);
            smallLabel(g, row.get(0).value(), frame.contentX(), y + 24.0);
            g.setFill(RenderSupport.color(g, snapshot, row.get(0).status()));
            g.fillRoundRect(left, y, length, 28.0, 7.0, 7.0);
            String decision = row.get(3).value();
            if (!"pending".equals(decision)) {
                double labelX = Math.min(left + length + 7.0, x + width - RenderSupport.secondaryTextWidth(decision) - 4.0);
                RenderSupport.secondaryLabel(g, decision, labelX, y + 24.0,
                        labelX < left + length ? RenderSupport.palette(g).nodeText() : RenderSupport.palette(g).secondaryText());
            }
        }
        String lastFinish = factValue(state, "last-finish");
        if (lastFinish.matches("-?\\d+")) {
            double marker = x + (integer(lastFinish) - minimum) * unit;
            RenderSupport.line(g, marker, top + 14.0, marker, top + 22.0 + visible * rowHeight,
                    RenderSupport.palette(g).edge(SnapshotStatus.DONE), 2.0);
        }
        windowLabel(g, start, visible, state.rows().size(), frame);
    }

    private static void drawMathDiagram(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        String algorithm = factValue(state, "algorithm");
        double top = Math.max(28.0, frame.contentY() - 12.0);
        if ("SIEVE_OF_ERATOSTHENES".equals(algorithm)) {
            int columns = Math.max(1, (int) (frame.contentWidth() / 56.0));
            double width = Math.min(64.0, frame.contentWidth() / columns);
            int visibleRows = Math.max(1, (int) ((frame.contentHeight() - 57.0) / 43.0));
            int visible = Math.min(state.rows().size(), columns * visibleRows);
            int focus = focusedRowIndex(state);
            int start = Math.max(0, Math.min(Math.max(0, state.rows().size() - visible),
                    (focus / columns - visibleRows / 2) * columns));
            for (int offset = 0; offset < visible; offset++) {
                List<TypedCell> row = state.rows().get(start + offset);
                double x = frame.contentX() + offset % columns * width;
                double y = top + offset / columns * 43.0;
                fenwickTile(g, row.get(0).value(), x, y, width, 36.0,
                        RenderSupport.color(g, snapshot, row.get(0).status()));
                boolean crossedOut = row.get(0).status() == SnapshotStatus.REJECTED
                        || row.get(1).value().startsWith("rejected") || "notprime".equals(row.get(1).value());
                if (crossedOut) {
                    RenderSupport.line(g, x + 10.0, y + 27.0, x + width - 10.0, y + 8.0,
                            RenderSupport.palette(g).edge(SnapshotStatus.REJECTED), 1.5);
                }
            }
            drawEquation(g, factValue(state, "factor-equation"), frame, frame.height() - frame.bottom() - 10.0);
            windowLabel(g, start, visible, state.rows().size(), frame);
        } else if ("MODULAR_EXPONENTIATION".equals(algorithm)) {
            String binary = factValue(state, "remaining-binary");
            if (binary.matches("[01]+")) {
                List<SnapshotStatus> statuses = new ArrayList<>();
                for (int i = 0; i < binary.length(); i++) statuses.add(i == binary.length() - 1 ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT);
                drawRibbon(g, snapshot, chars(binary), statuses, frame.contentX(), top + 25.0,
                        frame.contentWidth(), binary.length() - 1, -1, -1, "Exponent₂");
            }
            double cardWidth = Math.min(180.0, (frame.contentWidth() - 42.0) / 3.0);
            valueCard(g, "Base", factValue(state, "current-base"), frame.contentX(), top + 132.0, cardWidth, SnapshotStatus.DEFAULT);
            valueCard(g, "Result", factValue(state, "result"), frame.contentX() + cardWidth + 21.0, top + 132.0, cardWidth, SnapshotStatus.ACTIVE);
            valueCard(g, "Modulus", factValue(state, "modulus"), frame.contentX() + 2.0 * (cardWidth + 21.0), top + 132.0, cardWidth, SnapshotStatus.DEFAULT);
            drawEquation(g, factValue(state, "multiply-equation"), frame, top + 245.0);
            drawEquation(g, factValue(state, "square-equation"), frame, top + 280.0);
            drawEquation(g, factValue(state, "shift-equation"), frame, top + 315.0);
        } else {
            double cardWidth = Math.min(168.0, (frame.contentWidth() - 100.0) / 3.0);
            valueCard(g, "r₀", factValue(state, "old-r"), frame.contentX(), top + 30.0, cardWidth, SnapshotStatus.DEFAULT);
            valueCard(g, "r₁", factValue(state, "r"), frame.contentX() + cardWidth + 50.0, top + 30.0, cardWidth, SnapshotStatus.ACTIVE);
            valueCard(g, "q", factValue(state, "quotient"), frame.contentX() + 2 * (cardWidth + 50.0), top + 30.0, cardWidth, SnapshotStatus.DEFAULT);
            drawEquation(g, factValue(state, "remainder-equation"), frame, top + 146.0);
            drawEquation(g, factValue(state, "coefficient-x-equation"), frame, top + 194.0);
            drawEquation(g, factValue(state, "coefficient-y-equation"), frame, top + 239.0);
            String bezout = factValue(state, "bezout");
            if (List.of("return", "complete").contains(factValue(state, "phase"))) drawEquation(g, bezout, frame, top + 298.0);
        }
    }

    private static void drawHashSlots(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        int count = state.rows().size();
        if (count == 0) return;
        double width = 112.0;
        int columns = Math.max(1, (int) (frame.contentWidth() / width));
        width = Math.min(126.0, frame.contentWidth() / columns);
        int rows = Math.max(1, (int) ((frame.contentHeight() - 77.0) / 100.0));
        int visible = Math.min(count, rows * columns);
        int active = integer(factValue(state, "probe-slot"));
        int start = windowStart(Math.max(0, active), visible, count);
        double top = Math.max(30.0, frame.contentY() - 12.0);
        for (int offset = 0; offset < visible; offset++) {
            int slot = start + offset;
            List<TypedCell> row = state.rows().get(slot);
            double x = frame.contentX() + offset % columns * width;
            double y = top + offset / columns * 100.0;
            smallCenteredLabel(g, "slot " + row.get(0).value(), x + width / 2.0, y + 8.0);
            String occupancy = row.get(3).value();
            String value = "OCCUPIED".equals(occupancy) ? row.get(1).value() + " → " + row.get(2).value()
                    : "TOMBSTONE".equals(occupancy) ? "†" : "∅";
            fenwickTile(g, value, x, y + 22.0, width, 43.0, RenderSupport.color(g, snapshot, row.get(0).status()));
            if ("TOMBSTONE".equals(occupancy)) smallCenteredLabel(g, "deleted", x + width / 2.0, y + 85.0);
        }
        List<String> path = listValues(factValue(state, "probe-path"));
        int offset = integer(factValue(state, "probe-offset"));
        int visited = Math.min(path.size(), Math.max(0, offset + 1));
        if (visited > 0) {
            String sequence = String.join(" → ", path.subList(0, visited));
            smallLabel(g, "Probes: " + sequence, frame.contentX(), frame.height() - frame.bottom() - 45.0);
        }
        if (active >= 0 && offset >= 0) drawEquation(g,
                "(" + factValue(state, "probe-start") + " + " + offset + ") mod " + count + " = " + active,
                frame, frame.height() - frame.bottom() - 12.0);
        windowLabel(g, start, visible, count, frame);
    }

    private static void drawAdjacencyLists(GraphicsContext g, SimulationSnapshot snapshot, TableState state, LayoutFrame frame) {
        double top = Math.max(32.0, frame.contentY() - 10.0);
        int rows = Math.min(state.rows().size(), Math.max(1, (int) ((frame.contentHeight() - 20.0) / 51.0)));
        int start = matrixRowStart(state, rows);
        for (int r = 0; r < rows; r++) {
            List<TypedCell> row = state.rows().get(start + r);
            double y = top + r * 51.0;
            fenwickTile(g, row.get(0).value(), frame.contentX(), y, 53.0, 37.0,
                    RenderSupport.color(g, snapshot, row.get(0).status()));
            List<String> neighbors = listValues(row.get(1).value());
            arrow(g, frame.contentX() + 58.0, y + 18.0, frame.contentX() + 84.0, y + 18.0, row.get(1).status());
            if (neighbors.isEmpty()) smallLabel(g, "∅", frame.contentX() + 94.0, y + 25.0);
            int visible = Math.min(neighbors.size(), Math.max(1, (int) ((frame.contentWidth() - 92.0) / 51.0)));
            for (int i = 0; i < visible; i++) {
                fenwickTile(g, neighbors.get(i), frame.contentX() + 90.0 + i * 51.0, y, 49.0, 37.0,
                        RenderSupport.color(g, snapshot, row.get(1).status()));
            }
            if (visible < neighbors.size()) smallLabel(g, "…", frame.width() - frame.right() - 15.0, y + 25.0);
        }
        windowLabel(g, start, rows, state.rows().size(), frame);
    }

    private static void drawOperationCards(GraphicsContext g, SimulationSnapshot snapshot, TableState state,
                                           LayoutFrame frame, double y, String label, int resultColumn) {
        if (state.rows().isEmpty() || y + 54.0 > frame.height() - frame.bottom()) return;
        int visible = Math.min(state.rows().size(), Math.max(1, (int) (frame.contentWidth() / 178.0)));
        int active = integer(factValue(state, "active-query"));
        if (active < 0) active = integer(factValue(state, "operation-index"));
        int start = windowStart(Math.max(0, active), visible, state.rows().size());
        double width = Math.min(205.0, frame.contentWidth() / visible);
        for (int offset = 0; offset < visible; offset++) {
            List<TypedCell> row = state.rows().get(start + offset);
            double x = frame.contentX() + offset * width;
            smallLabel(g, label + " " + row.get(0).value(), x + 4.0, y - 10.0);
            String target = "Query".equals(label) ? row.get(1).value() : row.get(1).value() + " " + row.get(2).value();
            String result = shortUnknown(row.get(resultColumn).value());
            valueCard(g, "", target + " → " + result, x, y - 4.0, width, row.get(0).status());
        }
    }

    private static void drawRibbon(GraphicsContext g, SimulationSnapshot snapshot, List<String> values,
                                    List<SnapshotStatus> statuses, double x, double y, double width,
                                    int focus, int first, int last, String label) {
        if (values.isEmpty()) return;
        double labelWidth = Math.min(105.0, Math.max(68.0, RenderSupport.secondaryTextWidth(label) + 15.0));
        RibbonWindow window = ribbonWindow(values, width - labelWidth, focus, first, last);
        smallLabel(g, label, x, y + 29.0);
        for (int offset = 0; offset < window.count(); offset++) {
            int i = window.start() + offset;
            double cellX = x + labelWidth + offset * window.cellWidth();
            smallCenteredLabel(g, Integer.toString(i), cellX + window.cellWidth() / 2.0, y - 8.0);
            SnapshotStatus status = i < statuses.size() ? statuses.get(i) : SnapshotStatus.DEFAULT;
            fenwickTile(g, shortUnknown(values.get(i)), cellX, y, window.cellWidth(), 43.0,
                    RenderSupport.color(g, snapshot, status));
        }
        if (window.count() < values.size()) smallLabel(g, window.start() + "…" + (window.start() + window.count() - 1), x, y + 61.0);
    }

    private static RibbonWindow ribbonWindow(List<String> values, double width, int focus, int first, int last) {
        double minimum = 42.0;
        for (String value : values) minimum = Math.max(minimum, Math.min(160.0, RenderSupport.textWidth(shortUnknown(value)) + 18.0));
        int count = Math.min(values.size(), Math.max(1, (int) (Math.max(1.0, width) / minimum)));
        int start = windowStart(Math.max(0, focus), count, values.size());
        if (first >= 0 && last >= first && last < values.size() && last - first + 1 <= count) {
            start = Math.min(first, Math.max(start, last - count + 1));
        }
        return new RibbonWindow(start, count, Math.min(81.0, Math.max(1.0, width) / Math.max(1, count)));
    }

    private static void drawOperandResult(GraphicsContext g, LayoutFrame frame, double y,
                                           String leftLabel, String left, String operator, String rightLabel, String right,
                                           String resultLabel, String result) {
        if (y + 59.0 > frame.height() - frame.bottom()) return;
        double cardWidth = Math.min(180.0, (frame.contentWidth() - 108.0) / 3.0);
        double x = frame.centerX() - (3 * cardWidth + 108.0) / 2.0;
        valueCard(g, leftLabel, shortUnknown(left), x, y, cardWidth, SnapshotStatus.ACTIVE);
        RenderSupport.centeredLabel(g, operator, x + cardWidth + 26.0, y + 50.0, RenderSupport.palette(g).text());
        valueCard(g, rightLabel, shortUnknown(right), x + cardWidth + 53.0, y, cardWidth, SnapshotStatus.ACTIVE);
        arrow(g, x + 2 * cardWidth + 65.0, y + 43.0, x + 2 * cardWidth + 94.0, y + 43.0, SnapshotStatus.ACTIVE);
        valueCard(g, resultLabel, shortUnknown(result), x + 2 * cardWidth + 108.0, y, cardWidth, SnapshotStatus.DONE);
    }

    private static void valueCard(GraphicsContext g, String label, String value, double x, double y,
                                   double width, SnapshotStatus status) {
        smallLabel(g, label, x + 4.0, y - 8.0);
        List<String> lines = wrapText(shortUnknown(value), Math.max(1.0, width - 16.0));
        g.setFill(RenderSupport.palette(g).fill(status));
        g.fillRoundRect(x + 3.0, y + 4.0, width - 6.0, 55.0, 9.0, 9.0);
        for (int i = 0; i < Math.min(2, lines.size()); i++) RenderSupport.centeredLabel(g, lines.get(i),
                x + width / 2.0, y + (lines.size() > 1 ? 23.0 + i * 23.0 : 38.0), RenderSupport.palette(g).nodeText());
    }

    private static void drawEquation(GraphicsContext g, String text, LayoutFrame frame, double baseline) {
        if (text == null || text.isBlank() || !known(text) || baseline > frame.height() - 12.0) return;
        // Equations annotate the operation; explanatory prose belongs beside the canvas.
        if (!(text.contains("=") || text.contains("→") || text.contains("<") || text.contains(">") || text.contains("mod"))) return;
        List<String> lines = wrapText(text, frame.contentWidth());
        int available = Math.max(0, (int) ((frame.height() - frame.bottom() - baseline) / 26.0) + 1);
        for (int i = 0; i < Math.min(Math.min(2, available), lines.size()); i++) {
            RenderSupport.label(g, lines.get(i), frame.contentX(), baseline + i * 26.0, RenderSupport.palette(g).text());
        }
    }

    private static void arrow(GraphicsContext g, double x1, double y1, double x2, double y2, SnapshotStatus status) {
        double dx = x2 - x1, dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length < 1.0) return;
        Color edge = RenderSupport.palette(g).edge(status);
        RenderSupport.line(g, x1, y1, x2, y2, edge, 1.8);
        double ux = dx / length, uy = dy / length;
        RenderSupport.line(g, x2, y2, x2 - ux * 7.0 + uy * 4.0, y2 - uy * 7.0 - ux * 4.0, edge, 1.8);
        RenderSupport.line(g, x2, y2, x2 - ux * 7.0 - uy * 4.0, y2 - uy * 7.0 + ux * 4.0, edge, 1.8);
    }

    private static void bracket(GraphicsContext g, double left, double right, double y, String label) {
        Color edge = RenderSupport.palette(g).edge(SnapshotStatus.ACTIVE);
        RenderSupport.line(g, left, y - 6.0, left, y, edge, 1.7);
        RenderSupport.line(g, left, y, right, y, edge, 1.7);
        RenderSupport.line(g, right, y - 6.0, right, y, edge, 1.7);
        smallCenteredLabel(g, label, (left + right) / 2.0, y + 23.0);
    }

    private static void windowLabel(GraphicsContext g, int start, int count, int total, LayoutFrame frame) {
        if (count < total) smallLabel(g, start + "…" + (start + count - 1) + " / " + total,
                frame.contentX(), frame.height() - 12.0);
    }

    private static List<String> chars(String text) {
        return text.codePoints().mapToObj(point -> new String(Character.toChars(point))).toList();
    }

    private static boolean isResult(TableState state) {
        return List.of("return", "complete").contains(factValue(state, "phase"));
    }

    private static List<Integer> firstQueenSolution(TableState state) {
        Matcher match = Pattern.compile("\\[([^\\[\\]]+)\\]").matcher(factValue(state, "solution-placements"));
        if (!match.find()) return List.of();
        List<Integer> values = java.util.Arrays.stream(match.group(1).split(",\\s*"))
                .map(String::trim).map(TableCanvasRenderer::integer).toList();
        int n = integer(factValue(state, "n"));
        return values.size() == n && values.stream().allMatch(value -> value >= 0 && value < n) ? values : List.of();
    }

    private static boolean validRow(TableState state, int row) { return row >= 0 && row < state.rows().size(); }
    private static boolean known(String value) {
        return value != null && !value.isBlank() && !List.of("none", "pending", "not evaluated", "NIL").contains(value);
    }
    private static String shortUnknown(String value) { return known(value) ? value : "?"; }
    private record RibbonWindow(int start, int count, double cellWidth) { }

    /** Exact cell values and headers determine the window; type never scales to fit. */
    static TableWindow window(TableState state, LayoutFrame frame) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(frame, "frame");
        if (displayedColumnCount(state) == 0) return new TableWindow(0, 0, 0, 0, MIN_ROW_HEIGHT);
        frame = tableFrame(state, frame);
        int activeRow = focusedRowIndex(state);
        TableMetrics metrics = measureTable(state, frame);
        int visibleRows = Math.min(state.rows().size(), Math.max(1,
                (int) Math.floor((frame.contentHeight() - metrics.headerHeight() - 8.0) / metrics.rowHeight())));
        int rowStart = windowStart(activeRow, visibleRows, state.rows().size());
        if (isHashTable(state)) {
            int[] active = range(factValue(state, "window"));
            if (active[0] >= 0 && active[1] < state.rows().size()
                    && active[1] - active[0] + 1 <= visibleRows) {
                rowStart = Math.max(0, Math.min(state.rows().size() - visibleRows,
                        Math.min(active[0], Math.max(rowStart, active[1] - visibleRows + 1))));
            }
        }
        // A focused table must show both operands, not merely the first active
        // row. Keep their exact contiguous span whenever it fits this window.
        int firstActive = state.rows().size();
        int lastActive = -1;
        for (int row = 0; row < state.rows().size(); row++) {
            if (state.rows().get(row).stream().anyMatch(cell -> cell.status() == SnapshotStatus.ACTIVE)) {
                firstActive = Math.min(firstActive, row);
                lastActive = row;
            }
        }
        if (lastActive >= 0) {
            firstActive = Math.min(firstActive, activeRow);
            lastActive = Math.max(lastActive, activeRow);
        }
        if ("prefix".equals(factValue(state, "renderer"))
                && List.of("query", "formula", "return", "complete").contains(factValue(state, "phase"))) {
            int left = integer(factValue(state, "prefix-left"));
            int right = integer(factValue(state, "prefix-right"));
            if (left >= 0 && right >= left && right < state.rows().size()) {
                firstActive = Math.min(firstActive, left);
                lastActive = Math.max(lastActive, right);
            }
        }
        if (lastActive >= firstActive && lastActive - firstActive + 1 <= visibleRows) {
            rowStart = Math.max(0, Math.min(state.rows().size() - visibleRows,
                    Math.min(firstActive, Math.max(rowStart, lastActive - visibleRows + 1))));
        }
        return new TableWindow(rowStart, rowStart + visibleRows, metrics.columnStart(),
                metrics.columnEnd(), metrics.headerHeight());
    }

    /** Width determines the same column window and row budget before and after context layout. */
    private static TableMetrics measureTable(TableState state, LayoutFrame frame) {
        int columns = displayedColumnCount(state);
        if (columns == 0) return new TableMetrics(0, 0, MIN_ROW_HEIGHT, MIN_ROW_HEIGHT);
        int activeRow = focusedRowIndex(state);
        int activeColumn = focusedColumnIndex(state, activeRow);
        double[] desiredWidths = desiredColumnWidths(state);
        int columnStart = Math.min(activeColumn, columns - 1);
        int columnEnd = columnStart + 1;
        double occupiedWidth = desiredWidths[columnStart];
        // Fit each column independently. A long explanation must not make
        // every numeric/index column equally wide and hide the operands.
        while (columnStart > 0 || columnEnd < columns) {
            boolean addLeft = columnStart > 0 && (columnEnd >= columns
                    || activeColumn - columnStart <= columnEnd - activeColumn - 1);
            int candidate = addLeft ? columnStart - 1 : columnEnd;
            if (occupiedWidth + desiredWidths[candidate] > frame.contentWidth()) {
                int alternate = addLeft ? columnEnd : columnStart - 1;
                if (alternate < 0 || alternate >= columns
                        || occupiedWidth + desiredWidths[alternate] > frame.contentWidth()) break;
                addLeft = !addLeft;
                candidate = alternate;
            }
            occupiedWidth += desiredWidths[candidate];
            if (addLeft) columnStart--; else columnEnd++;
        }
        double[] widths = fittedColumnWidths(state, frame, columnStart, columnEnd);
        int headerLines = 1;
        int valueLines = 1;
        for (int column = columnStart; column < columnEnd; column++) {
            double cellWidth = widths[column - columnStart];
            headerLines = Math.max(headerLines, wrapText(columnLabel(state, column), cellWidth - 24.0).size());
            for (int row = 0; row < state.rows().size(); row++) {
                valueLines = Math.max(valueLines, wrapText(cellValue(state, row, column), cellWidth - 24.0).size());
            }
        }
        // Fenwick's short binary/numeric cells need less padding, so a five-cell
        // teaching example fits beside the code without reducing its 24 px type.
        double minimumHeight = isFenwickTable(state) ? 38.0 : MIN_ROW_HEIGHT;
        double padding = isFenwickTable(state) ? 8.0 : 14.0;
        double headerHeight = Math.max(minimumHeight, headerLines * LINE_HEIGHT + padding);
        double requiredHeight = Math.max(minimumHeight, valueLines * LINE_HEIGHT + padding);
        return new TableMetrics(columnStart, columnEnd, headerHeight, requiredHeight);
    }

    private static double[] desiredColumnWidths(TableState state) {
        int count = displayedColumnCount(state);
        double[] widths = new double[count];
        for (int column = 0; column < count; column++) {
            double width = Math.min(230.0, RenderSupport.textWidth(columnLabel(state, column)) + 28.0);
            for (int row = 0; row < state.rows().size(); row++) {
                for (String line : cellValue(state, row, column).split("\\n", -1)) {
                    width = Math.max(width, Math.min(340.0, RenderSupport.textWidth(line) + 28.0));
                }
            }
            widths[column] = Math.max(isKnapsackTable(state) ? 60.0 : 72.0, width);
        }
        return widths;
    }

    private static double[] fittedColumnWidths(TableState state, LayoutFrame frame, int start, int end) {
        double[] desired = desiredColumnWidths(state);
        double total = 0.0;
        for (int column = start; column < end; column++) total += desired[column];
        double[] widths = new double[end - start];
        double extra = Math.max(0.0, frame.contentWidth() - total) / widths.length;
        for (int column = start; column < end; column++) {
            widths[column - start] = total > frame.contentWidth()
                    ? desired[column] * frame.contentWidth() / total : desired[column] + extra;
        }
        return widths;
    }

    static boolean usesFocusedLayout(TableState state, LayoutFrame frame) {
        TableWindow window = window(state, frame);
        return window.rowCount() < state.rows().size() || window.columnCount() < displayedColumnCount(state);
    }

    static int focusedRowIndex(TableState state) {
        // Window membership can mark many cells ACTIVE. Use the explicit
        // current index before considering those broader highlights.
        int current = integer(factValue(state, "target-row"));
        if ("GAUSSIAN_ELIMINATION".equals(factValue(state, "algorithm")) && current > 0) current--;
        if (current < 0) current = integer(factValue(state, "active-row"));
        if (current < 0) current = integer(factValue(state, "probe-slot"));
        if (current < 0 && "sparse".equals(factValue(state, "renderer"))) current = integer(factValue(state, "start"));
        if (current < 0 && "SIEVE_OF_ERATOSTHENES".equals(factValue(state, "algorithm"))) {
            String number = factValue(state, "focus-number");
            for (int row = 0; row < state.rows().size(); row++) {
                if (state.rows().get(row).get(0).value().equals(number)) { current = row; break; }
            }
        }
        if (current < 0 && "PASCAL_TRIANGLE".equals(factValue(state, "algorithm"))) {
            String targetRow = factValue(state, "current-row");
            String targetColumn = factValue(state, "current-column");
            for (int row = 0; row < state.rows().size(); row++) {
                if (state.rows().get(row).get(0).value().equals(targetRow)
                        && state.rows().get(row).get(1).value().equals(targetColumn)) { current = row; break; }
            }
        }
        if (current < 0 && isKmpTable(state)) current = "lps".equals(factValue(state, "phase"))
                ? integer(factValue(state, "lps-index"))
                : integer(factValue(state, hasShiftedKmpAlignment(state) ? "i" : "text-compare-index"));
        if (current < 0 && isFenwickTable(state)) current = integer(factValue(state, "bit-index")) - 1;
        if (current < 0 && isZTable(state)) current = integer(factValue(state, "i"));
        if (current < 0 && "SUFFIX_ARRAY".equals(factValue(state, "algorithm"))
                && List.of("lcp", "complete").contains(factValue(state, "phase"))) {
            current = integer(factValue(state, "lcp-rank"));
        }
        if (isHashTable(state)) {
            int[] active = range(factValue(state, "window"));
            if (active[0] >= 0) current = (active[0] + active[1] + 1) / 2;
        }
        if (current >= 0 && current < state.rows().size()) return current;
        for (int row = 0; row < state.rows().size(); row++) {
            if (state.rows().get(row).stream().anyMatch(cell -> cell.status() == SnapshotStatus.ACTIVE)) return row;
        }
        return isFenwickTable(state) ? 0 : Math.max(0, state.rows().size() - 1);
    }

    private static int focusedColumnIndex(TableState state, int row) {
        if (state.rows().isEmpty()) return 0;
        int explicit = integer(factValue(state, "target-column"));
        if (explicit < 0) {
            explicit = integer(factValue(state, "active-column"));
            if (explicit >= 0 && "GAUSSIAN_ELIMINATION".equals(factValue(state, "algorithm"))) explicit++;
        }
        if (explicit < 0 && "sparse".equals(factValue(state, "renderer"))) {
            int level = integer(factValue(state, "level"));
            if (level >= 0) explicit = level + 1;
        }
        if (explicit >= 0 && explicit < displayedColumnCount(state)) return explicit;
        // Whole-row highlights do not specify a column. Keep its identifying
        // column visible; a single active cell is a genuine focused target.
        long activeCount = state.rows().get(row).stream()
                .filter(cell -> cell.status() == SnapshotStatus.ACTIVE).count();
        if (activeCount != 1) return 0;
        for (int column = 0; column < state.columns().size(); column++) {
            if (state.rows().get(row).get(column).status() == SnapshotStatus.ACTIVE) return column;
        }
        return 0;
    }

    private static int windowStart(int focus, int visible, int total) {
        return Math.max(0, Math.min(Math.max(0, total - visible), focus - visible / 2));
    }

    private static boolean isFenwickTable(TableState state) {
        return "fenwick".equals(factValue(state, "renderer"))
                || state.columns().equals(List.of("BIT index", "Binary", "Sum", "Covers input"));
    }

    private static boolean isZTable(TableState state) {
        return state.columns().equals(List.of("index", "char", "z[i]", "window", "l", "r", "comparison"));
    }

    private static boolean isHashTable(TableState state) {
        return state.columns().equals(List.of("index", "char", "window", "window-hash", "pattern-hash", "verification"));
    }


    private static int displayedColumnCount(TableState state) {
        // These trailing columns repeat whole-table facts. Their references
        // are shown outside the row window so index and character stay visible.
        if (isZTable(state) || isHashTable(state)) return 4;
        if ("MODULAR_EXPONENTIATION".equals(factValue(state, "algorithm"))) return Math.min(8, state.columns().size());
        return state.columns().size();
    }

    private static String columnLabel(TableState state, int column) {
        if (isKnapsackTable(state) && column == 0) return "i · w, v";
        String label = state.columns().get(column);
        return switch (label) {
            case "exponent-before" -> "Exponent";
            case "base-before" -> "Base";
            case "result-before" -> "Result";
            case "multiply" -> "Bit = 1?";
            case "result-after-multiply" -> "New result";
            case "base-after-square" -> "New base";
            case "exponent-after" -> "Next exponent";
            case "aligned-pattern" -> "Pattern";
            case "processing order" -> "Visit order";
            default -> label.replace('-', ' ');
        };
    }

    private static boolean isKnapsackTable(TableState state) {
        return "DP_KNAPSACK".equals(factValue(state, "algorithm"))
                && !state.columns().isEmpty() && state.columns().get(0).equals("item");
    }

    private static String cellValue(TableState state, int row, int column) {
        String value = state.rows().get(row).get(column).value();
        if (isKnapsackTable(state) && column == 0) {
            Matcher item = KNAPSACK_ITEM.matcher(value);
            if (item.matches()) return row + " · w=" + item.group(1) + " v=" + item.group(2);
            if ("base".equals(value)) return "0 · base";
        }
        return value;
    }

    private static boolean isKmpTable(TableState state) {
        return "KMP".equals(factValue(state, "algorithm"));
    }

    private static boolean hasShiftedKmpAlignment(TableState state) {
        if (!isKmpTable(state) || !"search".equals(factValue(state, "phase"))) return false;
        int textIndex = integer(factValue(state, "text-compare-index"));
        int patternIndex = integer(factValue(state, "pattern-compare-index"));
        int alignment = integer(factValue(state, "alignment-start"));
        return textIndex >= 0 && patternIndex >= 0 && alignment >= 0
                && alignment != textIndex - patternIndex;
    }

    private static boolean isStringStrip(TableState state) {
        return isKmpTable(state) || isZTable(state) || isHashTable(state);
    }


    /** Retained for focused-window checks; diagrams no longer reserve a prose band. */
    static LayoutFrame tableFrame(TableState state, LayoutFrame frame) { return frame; }

    private static List<String> listValues(String raw) {
        if (!raw.startsWith("[") || !raw.endsWith("]")) return List.of();
        String content = raw.substring(1, raw.length() - 1).trim();
        return content.isEmpty() ? List.of() : List.of(content.split(",\\s*"));
    }


    private static String factValue(TableState state, String key) {
        for (Fact fact : state.facts()) if (fact.key().equals(key)) return fact.value();
        return "";
    }

    private static int integer(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return -1; }
    }

    private static int[] range(String value) {
        Matcher match = RANGE.matcher(value);
        if (!match.matches()) return new int[] {-1, -1};
        int left = integer(match.group(1));
        int right = integer(match.group(2));
        return left >= 0 && right >= left ? new int[] {left, right} : new int[] {-1, -1};
    }


    static List<String> wrapText(String raw, double width) {
        String text = raw == null ? "" : raw;
        if (text.isEmpty()) return List.of("");
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\n", -1)) {
            if (paragraph.isEmpty()) { lines.add(""); continue; }
            int start = 0;
            while (start < paragraph.length()) {
                int end = start;
                while (end < paragraph.length()) {
                    int next = paragraph.offsetByCodePoints(end, 1);
                    if (end > start && RenderSupport.textWidth(paragraph.substring(start, next)) > width) break;
                    end = next;
                }
                int cut = end;
                if (end < paragraph.length()) {
                    int space = paragraph.lastIndexOf(' ', end - 1);
                    if (space > start) cut = space;
                }
                lines.add(paragraph.substring(start, cut));
                start = cut;
                while (start < paragraph.length() && paragraph.charAt(start) == ' ') start++;
            }
        }
        return List.copyOf(lines);
    }

    private record TableMetrics(int columnStart, int columnEnd, double headerHeight, double rowHeight) { }

    static record TableWindow(int rowStart, int rowEnd, int columnStart, int columnEnd, double headerHeight) {
        int rowCount() { return rowEnd - rowStart; }
        int columnCount() { return columnEnd - columnStart; }
    }

    private static TableState requireState(SimulationSnapshot snapshot) {
        if (snapshot == null || snapshot.state().rendererFamily() != RendererFamily.TABLE) {
            throw new IllegalArgumentException("TableCanvasRenderer requires TABLE state");
        }
        return (TableState) snapshot.state();
    }
}
