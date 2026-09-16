package dev.codetrail.desktop.simulation.strings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.List;

/** Linear-time Z-function construction with explicit inclusive-window state. */
public final class ZFunctionEngine implements SimulationEngine {
    public static final String TYPE = "Z_FUNCTION";
    public static final int MIN_TEXT_LENGTH = 0;
    public static final int MAX_TEXT_LENGTH = StringSimulationSupport.MAX_TEXT_LENGTH;
    public static final int MAX_TRACE_STEPS = StringSimulationSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_ZERO_CONVENTION = 2;
    private static final int LINE_WINDOW_INITIALIZE = 3;
    private static final int LINE_LOOP = 4;
    private static final int LINE_COPY_WINDOW = 5;
    private static final int LINE_EXTEND = 6;
    private static final int LINE_UPDATE_WINDOW = 7;
    private static final int LINE_RETURN = 8;

    private static final List<String> PSEUDOCODE = List.of(
            "zFunction(s):",
            "    z[0] = 0  // convention used by this lesson",
            "    l = 0; r = 0  // inclusive matching window",
            "    for i = 1 to s.length - 1:",
            "        if i <= r: z[i] = min(r - i + 1, z[i - l])",
            "        while i + z[i] < n and s[z[i]] == s[i + z[i]]: z[i]++",
            "        if i + z[i] - 1 > r: l = i; r = i + z[i] - 1",
            "return z");

    private static final List<String> TABLE_COLUMNS = List.of(
            "index", "char", "z[i]", "window", "l", "r", "comparison");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        String text = StringSimulationSupport.readLowercaseString(
                input, TYPE, "text", MIN_TEXT_LENGTH, MAX_TEXT_LENGTH);
        Model model = new Model(text);
        List<SimulationStep> steps = new ArrayList<>();

        add(
                steps,
                model,
                0,
                "Initialize the Z-function for text " + StringSimulationSupport.boundedText(text),
                StepEventType.INITIALIZE);
        add(
                steps,
                model,
                LINE_METHOD,
                "Run the linear Z-function procedure over every text position",
                StepEventType.EXECUTE_LINE);
        if (text.isEmpty()) {
            model.phase = "return";
            model.comparison = "empty text has an empty Z array";
            add(
                    steps,
                    model,
                    LINE_RETURN,
                    "Return the empty Z array",
                    StepEventType.EXECUTE_LINE);
            model.phase = "complete";
            add(
                    steps,
                    model,
                    0,
                    "Complete: Z-function is []",
                    StepEventType.COMPLETE);
            return List.copyOf(steps);
        }

        model.phase = "initialize";
        model.z[0] = 0;
        model.known[0] = true;
        model.currentIndex = 0;
        model.comparison = "z[0] = 0 by convention; it is not a self-comparison";
        add(
                steps,
                model,
                LINE_ZERO_CONVENTION,
                "Set z[0] = 0 by convention",
                StepEventType.EXECUTE_LINE);
        model.currentIndex = -1;
        model.l = 0;
        model.r = 0;
        model.comparison = "start with inclusive window [0, 0]";
        add(
                steps,
                model,
                LINE_WINDOW_INITIALIZE,
                "Initialize the inclusive Z-window with l = 0 and r = 0",
                StepEventType.EXECUTE_LINE);

        model.phase = "scan";
        for (int index = 1; index < text.length(); index++) {
            model.currentIndex = index;
            model.lastCompareIndex = -1;
            model.prefixIndex = -1;
            model.reuseIndex = -1;
            model.comparison = "inspect i = " + index + " with window [" + model.l + ", " + model.r + "]";
            add(
                    steps,
                    model,
                    LINE_LOOP,
                    "Inspect position i = " + index + " with current window [" + model.l + ", " + model.r + "]",
                    StepEventType.EXECUTE_LINE);

            if (index <= model.r) {
                model.reuseIndex = index - model.l;
                int copied = Math.min(model.r - index + 1, model.z[index - model.l]);
                model.z[index] = copied;
                model.known[index] = true;
                model.comparison = "copy z[" + index + "] = min(" + (model.r - index + 1)
                        + ", z[" + (index - model.l) + "] = " + model.z[index - model.l]
                        + ") = " + copied;
                add(
                        steps,
                        model,
                        LINE_COPY_WINDOW,
                        "Reuse the known window prefix and initialize z[" + index + "] = " + copied,
                        StepEventType.EXECUTE_LINE);
            } else {
                model.z[index] = 0;
                model.known[index] = true;
                model.comparison = "i is outside the window; initialize z[" + index + "] = 0";
                add(
                        steps,
                        model,
                        LINE_COPY_WINDOW,
                        "Position " + index + " is outside the window; start z[" + index + "] at 0",
                        StepEventType.EXECUTE_LINE);
            }

            while (index + model.z[index] < text.length()) {
                int prefixIndex = model.z[index];
                int textIndex = index + model.z[index];
                model.lastCompareIndex = textIndex;
                model.prefixIndex = prefixIndex;
                model.comparison = "compare s[" + prefixIndex + "] "
                        + StringSimulationSupport.quotedChar(text, prefixIndex)
                        + " with s[" + textIndex + "] "
                        + StringSimulationSupport.quotedChar(text, textIndex);
                if (text.charAt(prefixIndex) == text.charAt(textIndex)) {
                    model.z[index]++;
                    model.comparison += " => match; extend z[" + index + "] to " + model.z[index];
                    add(
                            steps,
                            model,
                            LINE_EXTEND,
                            "Matching characters extend z[" + index + "] to " + model.z[index],
                            StepEventType.EXECUTE_LINE);
                } else {
                    model.comparison += " => mismatch; stop extending";
                    add(
                            steps,
                            model,
                            LINE_EXTEND,
                            "Mismatch stops extension at z[" + index + "] = " + model.z[index],
                            StepEventType.EXECUTE_LINE);
                    break;
                }
            }
            if (index + model.z[index] >= text.length()) {
                model.lastCompareIndex = -1;
                model.prefixIndex = -1;
                model.comparison = "stop at the string boundary with z[" + index + "] = " + model.z[index];
                add(
                        steps,
                        model,
                        LINE_EXTEND,
                        "Stop extension at the end of the text",
                        StepEventType.EXECUTE_LINE);
            }

            if (index + model.z[index] - 1 > model.r) {
                model.l = index;
                model.r = index + model.z[index] - 1;
                model.comparison = "update inclusive window to [" + model.l + ", " + model.r + "]";
                model.currentIndex = -1;
                model.lastComputedIndex = index;
                add(
                        steps,
                        model,
                        LINE_UPDATE_WINDOW,
                        "A farther match ends at " + model.r + "; update window to ["
                                + model.l + ", " + model.r + "]",
                        StepEventType.EXECUTE_LINE);
            } else {
                model.comparison = "keep inclusive window [" + model.l + ", " + model.r + "]";
                model.currentIndex = -1;
                model.lastComputedIndex = index;
                add(
                        steps,
                        model,
                        LINE_UPDATE_WINDOW,
                        "z[" + index + "] does not reach past r; keep window ["
                                + model.l + ", " + model.r + "]",
                        StepEventType.EXECUTE_LINE);
            }
        }

        model.phase = "return";
        model.prefixIndex = -1;
        model.reuseIndex = -1;
        model.currentIndex = -1;
        model.lastCompareIndex = -1;
        model.comparison = "all positions computed; z = " + StringSimulationSupport.formatArray(model.z);
        add(
                steps,
                model,
                LINE_RETURN,
                "Return the computed Z array " + StringSimulationSupport.formatArray(model.z),
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: Z-function values are " + StringSimulationSupport.formatArray(model.z),
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private static void add(
            List<SimulationStep> steps,
            Model model,
            int line,
            String narration,
            StepEventType eventType) {
        if (steps.size() >= MAX_TRACE_STEPS) {
            throw new IllegalStateException(TYPE + " trace exceeded bounded step limit");
        }
        steps.add(StringSimulationSupport.tableStep(
                TABLE_COLUMNS,
                rows(model),
                facts(model),
                line,
                narration,
                eventType));
    }

    private static List<List<TypedCell>> rows(Model model) {
        int rowCount = Math.max(1, model.text.length());
        List<List<TypedCell>> rows = new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            boolean hasText = index < model.text.length();
            boolean insideWindow = hasText && index >= model.l && index <= model.r;
            SnapshotStatus characterStatus = !hasText
                    ? SnapshotStatus.DEFAULT
                    : index == model.lastCompareIndex || index == model.prefixIndex
                            ? (model.comparison.contains("mismatch") ? SnapshotStatus.REJECTED : SnapshotStatus.ACTIVE)
                            : index == model.currentIndex ? SnapshotStatus.ACTIVE
                            : model.known[index] ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
            SnapshotStatus zStatus = !hasText
                    ? SnapshotStatus.DEFAULT
                    : index == model.currentIndex || index == model.reuseIndex
                            ? SnapshotStatus.ACTIVE
                            : model.known[index] ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
            SnapshotStatus windowStatus = insideWindow ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            SnapshotStatus comparisonStatus = index == model.currentIndex
                    ? SnapshotStatus.ACTIVE
                    : index == model.lastCompareIndex
                            ? (model.comparison.contains("mismatch")
                                    ? SnapshotStatus.REJECTED
                                    : SnapshotStatus.DONE)
                            : SnapshotStatus.DEFAULT;
            rows.add(StringSimulationSupport.row(
                    StringSimulationSupport.cell("index-" + index,
                            hasText ? Integer.toString(index) : "-", characterStatus),
                    StringSimulationSupport.cell("char-" + index,
                            hasText ? Character.toString(model.text.charAt(index)) : "·", characterStatus),
                    StringSimulationSupport.cell("z-" + index,
                            hasText
                                    ? (model.known[index] ? Integer.toString(model.z[index]) : "?")
                                    : "·",
                            zStatus),
                    StringSimulationSupport.cell("window-" + index,
                            insideWindow ? "inside" : "·", windowStatus),
                    StringSimulationSupport.cell("l-" + index,
                            Integer.toString(model.l), SnapshotStatus.DEFAULT),
                    StringSimulationSupport.cell("r-" + index,
                            Integer.toString(model.r), SnapshotStatus.DEFAULT),
                    StringSimulationSupport.cell("comparison-" + index,
                            index == model.lastCompareIndex || index == model.currentIndex
                                    ? model.comparison
                                    : "·",
                            comparisonStatus)));
        }
        return List.copyOf(rows);
    }

    private static List<Fact> facts(Model model) {
        SnapshotStatus stateStatus = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        String window = model.text.isEmpty()
                ? "empty"
                : "[" + model.l + ", " + model.r + "] (inclusive)";
        return List.of(
                new Fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                new Fact("prefix-index", Integer.toString(model.prefixIndex), stateStatus),
                new Fact("compare-index", Integer.toString(model.lastCompareIndex), stateStatus),
                new Fact("reuse-index", Integer.toString(model.reuseIndex), stateStatus),
                new Fact("teaching-equation", model.comparison, stateStatus),
                new Fact("teaching-detail", "Compare the prefix character with the character at i + z[i]; reuse only inside [l, r].", SnapshotStatus.DEFAULT),
                new Fact("phase", model.phase, SnapshotStatus.DEFAULT),
                new Fact("text", StringSimulationSupport.boundedText(model.text), SnapshotStatus.DEFAULT),
                new Fact("z", StringSimulationSupport.formatKnownArray(model.z, model.known), stateStatus),
                new Fact("i", Integer.toString(model.currentIndex >= 0 ? model.currentIndex : model.lastComputedIndex),
                        SnapshotStatus.ACTIVE),
                new Fact("l", Integer.toString(model.l), SnapshotStatus.ACTIVE),
                new Fact("r", Integer.toString(model.r), SnapshotStatus.ACTIVE),
                new Fact("window", window, SnapshotStatus.ACTIVE),
                new Fact("comparison", model.comparison, stateStatus),
                new Fact("z[0]-convention", "0 (the first position is defined as zero)", SnapshotStatus.DEFAULT));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("text", "aabxaabxcaabxaabxay");
        return new SimulationMetadata(
                TYPE,
                "Z-function",
                "O(n)",
                "O(n)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"text\":\"aabxaabxcaabxaabxay\"}; text length must be "
                        + MIN_TEXT_LENGTH + ".." + MAX_TEXT_LENGTH
                        + " and text must contain only lowercase ASCII letters. The returned array has one value per "
                        + "position, with z[0] defined as 0 by convention; an empty text produces [].",
                PSEUDOCODE);
    }

    private static final class Model {
        private final String text;
        private final int[] z;
        private final boolean[] known;
        private String phase = "initialize";
        private String comparison = "pending";
        private int l;
        private int r;
        private int currentIndex = -1;
        private int lastCompareIndex = -1;
        private int lastComputedIndex = -1;
        private int prefixIndex = -1;
        private int reuseIndex = -1;

        private Model(String text) {
            this.text = text;
            this.z = new int[text.length()];
            this.known = new boolean[text.length()];
        }
    }
}
