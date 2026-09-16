package dev.codetrail.desktop.simulation.strings.remaining;

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

/** Comparison-sorted suffix array with a directly computed adjacent LCP array. */
public final class SuffixArrayEngine implements SimulationEngine {
    public static final String TYPE = "SUFFIX_ARRAY";
    public static final int MIN_TEXT_LENGTH = 0;
    public static final int MAX_TEXT_LENGTH = RemainingStringSupport.MAX_SUFFIX_TEXT_LENGTH;
    public static final int MAX_TRACE_STEPS = RemainingStringSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_INITIALIZE = 2;
    private static final int LINE_LOOP = 3;
    private static final int LINE_COMPARE = 4;
    private static final int LINE_SHIFT = 5;
    private static final int LINE_LCP = 6;
    private static final int LINE_RETURN = 7;

    private static final List<String> PSEUDOCODE = List.of(
            "suffixArray(text):",
            "    starts = [0, 1, ..., n - 1]",
            "    for each start in starts: insert it into sorted suffix order",
            "        compare text[start..] and text[other..] lexicographically",
            "        shift larger suffixes right and place start",
            "    lcp[0] = 0; lcp[k] = LCP(suffixArray[k - 1], suffixArray[k])",
            "return suffixArray and lcp");

    private static final List<String> TABLE_COLUMNS = List.of(
            "rank", "start", "suffix", "lcp", "comparison");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        JsonNode object = RemainingStringSupport.requireExactObject(input, TYPE, "text");
        String text = RemainingStringSupport.readLowercaseString(
                object, TYPE, "text", MIN_TEXT_LENGTH, MAX_TEXT_LENGTH);
        Model model = new Model(text);
        List<SimulationStep> steps = new ArrayList<>();

        add(
                steps,
                model,
                0,
                "Initialize suffix-array construction for text "
                        + RemainingStringSupport.boundedText(text),
                StepEventType.INITIALIZE);
        add(
                steps,
                model,
                LINE_METHOD,
                "Build suffix order by comparing complete suffixes",
                StepEventType.EXECUTE_LINE);

        if (text.isEmpty()) {
            model.phase = "return";
            model.comparison = "empty text has an empty suffix array and LCP array";
            add(steps, model, LINE_RETURN, "Return suffix-array = [] and lcp = []", StepEventType.EXECUTE_LINE);
            model.phase = "complete";
            add(steps, model, 0, "Complete: empty suffix array and LCP array", StepEventType.COMPLETE);
            return List.copyOf(steps);
        }

        model.phase = "sort";
        model.order[0] = 0;
        model.orderKnown[0] = true;
        model.sortedCount = 1;
        model.comparison = "suffix 0 is the initial one-item order";
        add(steps, model, LINE_INITIALIZE, "Start the sorted suffix order with suffix start 0", StepEventType.EXECUTE_LINE);

        for (int candidate = 1; candidate < text.length(); candidate++) {
            model.candidate = candidate;
            model.compared = -1;
            model.comparedOffset = -1;
            model.comparison = "insert suffix starting at " + candidate;
            add(
                    steps,
                    model,
                    LINE_LOOP,
                    "Insert suffix starting at text index " + candidate,
                    StepEventType.EXECUTE_LINE);

            int position = model.sortedCount;
            while (position > 0) {
                int existing = model.order[position - 1];
                int comparison = compareSuffixes(model, steps, candidate, existing);
                if (comparison >= 0) {
                    break;
                }
                model.order[position] = existing;
                model.orderKnown[position] = true;
                model.compared = existing;
                model.comparison = "shift suffix " + existing + " right to rank " + position;
                add(
                        steps,
                        model,
                        LINE_SHIFT,
                        "Shift suffix " + existing + " right because it follows suffix " + candidate,
                        StepEventType.EXECUTE_LINE);
                position--;
            }
            model.order[position] = candidate;
            model.orderKnown[position] = true;
            model.sortedCount++;
            model.compared = -1;
            model.comparison = "place suffix " + candidate + " at rank " + position;
            add(
                    steps,
                    model,
                    LINE_SHIFT,
                    "Place suffix " + candidate + " at sorted rank " + position,
                    StepEventType.EXECUTE_LINE);
            model.candidate = -1;
        }

        model.phase = "lcp";
        model.compared = -1;
        model.comparison = "suffix order is complete; compute adjacent LCP values";
        add(
                steps,
                model,
                LINE_LCP,
                "Compute the LCP of every adjacent pair in the suffix array",
                StepEventType.EXECUTE_LINE);
        model.lcp[0] = 0;
        model.lcpKnown[0] = true;
        model.lcpRank = 0;
        model.comparison = "lcp[0] = 0 by convention because it has no predecessor";
        add(steps, model, LINE_LCP, "Set lcp[0] = 0 because the first suffix has no adjacent predecessor", StepEventType.EXECUTE_LINE);

        for (int rank = 1; rank < text.length(); rank++) {
            int leftStart = model.order[rank - 1];
            int rightStart = model.order[rank];
            model.lcpRank = rank;
            model.candidate = rightStart;
            model.compared = leftStart;
            model.comparedOffset = -1;
            model.lcpOffset = 0;
            model.lcp[rank] = 0;
            model.lcpKnown[rank] = true;
            model.comparison = "compute LCP of suffixes " + leftStart + " and " + rightStart;
            add(
                    steps,
                    model,
                    LINE_LCP,
                    "Compare adjacent suffixes at ranks " + (rank - 1) + " and " + rank,
                    StepEventType.EXECUTE_LINE);

            int limit = Math.min(text.length() - leftStart, text.length() - rightStart);
            while (model.lcpOffset < limit) {
                int offset = model.lcpOffset;
                model.comparedOffset = offset;
                char left = text.charAt(leftStart + offset);
                char right = text.charAt(rightStart + offset);
                if (left != right) {
                    model.comparison = "LCP compare offset " + offset + ": "
                            + RemainingStringSupport.quotedChar(text, leftStart + offset)
                            + " != " + RemainingStringSupport.quotedChar(text, rightStart + offset)
                            + "; stop at length " + model.lcpOffset;
                    add(
                            steps,
                            model,
                            LINE_LCP,
                            "A mismatch fixes lcp[" + rank + "] = " + model.lcpOffset,
                            StepEventType.EXECUTE_LINE);
                    break;
                }
                model.lcpOffset++;
                model.lcp[rank] = model.lcpOffset;
                model.comparison = "LCP compare offset " + offset + ": matching "
                        + RemainingStringSupport.quotedChar(text, leftStart + offset)
                        + "; length = " + model.lcpOffset;
                add(
                        steps,
                        model,
                        LINE_LCP,
                        "Matching characters extend lcp[" + rank + "] to " + model.lcpOffset,
                        StepEventType.EXECUTE_LINE);
            }
            if (model.lcpOffset == limit) {
                model.comparedOffset = -1;
                model.comparison = "one suffix ended or all characters matched; lcp["
                        + rank + "] = " + model.lcp[rank];
                add(
                        steps,
                        model,
                        LINE_LCP,
                        "Finish lcp[" + rank + "] = " + model.lcp[rank],
                        StepEventType.EXECUTE_LINE);
            }
        }

        model.phase = "return";
        model.candidate = -1;
        model.compared = -1;
        model.lcpRank = -1;
        model.comparedOffset = -1;
        model.comparison = "all suffixes ordered; lcp = " + RemainingStringSupport.formatKnown(model.lcp, model.lcpKnown);
        add(
                steps,
                model,
                LINE_RETURN,
                "Return suffix-array " + RemainingStringSupport.formatKnown(model.order, model.orderKnown)
                        + " and lcp " + RemainingStringSupport.formatKnown(model.lcp, model.lcpKnown),
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: suffix array and adjacent LCP values are ready",
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private static int compareSuffixes(
            Model model,
            List<SimulationStep> steps,
            int candidate,
            int existing) {
        int limit = Math.min(model.text.length() - candidate, model.text.length() - existing);
        for (int offset = 0; offset < limit; offset++) {
            model.compared = existing;
            model.comparedOffset = offset;
            char candidateChar = model.text.charAt(candidate + offset);
            char existingChar = model.text.charAt(existing + offset);
            boolean equal = candidateChar == existingChar;
            model.comparison = "compare suffix " + candidate + " and " + existing
                    + " at offset " + offset + ": "
                    + RemainingStringSupport.quotedChar(model.text, candidate + offset)
                    + (equal ? " == " : " != ")
                    + RemainingStringSupport.quotedChar(model.text, existing + offset);
            add(
                    steps,
                    model,
                    LINE_COMPARE,
                    equal
                            ? "Suffix characters match at offset " + offset
                            : "Suffix comparison differs at offset " + offset,
                    StepEventType.EXECUTE_LINE);
            if (!equal) {
                return Character.compare(candidateChar, existingChar);
            }
        }
        model.comparedOffset = limit;
        model.comparison = "one suffix is a prefix of the other after " + limit + " matching character(s)";
        add(
                steps,
                model,
                LINE_COMPARE,
                "Use suffix length to break the prefix tie after " + limit + " character(s)",
                StepEventType.EXECUTE_LINE);
        return Integer.compare(model.text.length() - candidate, model.text.length() - existing);
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
        steps.add(RemainingStringSupport.tableStep(
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
        for (int rank = 0; rank < rowCount; rank++) {
            boolean hasSuffix = rank < model.sortedCount;
            int start = hasSuffix ? model.order[rank] : -1;
            SnapshotStatus rowStatus = hasSuffix
                    ? statusForSuffix(model, start, rank)
                    : SnapshotStatus.DEFAULT;
            SnapshotStatus lcpStatus = rank < model.text.length() && model.lcpKnown[rank]
                    ? (rank == model.lcpRank ? SnapshotStatus.ACTIVE : SnapshotStatus.DONE)
                    : SnapshotStatus.DEFAULT;
            String suffix = hasSuffix ? model.text.substring(start) : "·";
            String comparison = model.compared == start || model.candidate == start
                    ? model.comparison
                    : "·";
            SnapshotStatus comparisonStatus = model.compared == start
                    ? (model.comparison.contains("!=") ? SnapshotStatus.REJECTED : SnapshotStatus.ACTIVE)
                    : model.candidate == start ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            rows.add(RemainingStringSupport.row(
                    RemainingStringSupport.cell("rank-" + rank, Integer.toString(rank), rowStatus),
                    RemainingStringSupport.cell("start-" + rank, hasSuffix ? Integer.toString(start) : "-", rowStatus),
                    RemainingStringSupport.cell("suffix-" + rank, suffix, rowStatus),
                    RemainingStringSupport.cell(
                            "lcp-" + rank,
                            rank < model.text.length() ? (model.lcpKnown[rank] ? Integer.toString(model.lcp[rank]) : "?") : "·",
                            lcpStatus),
                    RemainingStringSupport.cell("comparison-" + rank, comparison, comparisonStatus)));
        }
        return List.copyOf(rows);
    }

    private static SnapshotStatus statusForSuffix(Model model, int start, int rank) {
        if (model.phase.equals("lcp") && (rank == model.lcpRank || rank == model.lcpRank - 1)) {
            return SnapshotStatus.ACTIVE;
        }
        if (start == model.candidate) {
            return SnapshotStatus.ACTIVE;
        }
        if (start == model.compared && model.comparison.contains("!=")) {
            return SnapshotStatus.REJECTED;
        }
        return rank < model.sortedCount ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
    }

    private static List<Fact> facts(Model model) {
        SnapshotStatus status = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        String suffixArray = RemainingStringSupport.formatKnown(model.order, model.orderKnown);
        String lcp = RemainingStringSupport.formatKnown(model.lcp, model.lcpKnown);
        return List.of(
                new Fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                new Fact("candidate-suffix", model.candidate < 0 ? "" : model.text.substring(model.candidate), status),
                new Fact("reference-suffix", model.compared < 0 ? "" : model.text.substring(model.compared), status),
                new Fact("reference-start", Integer.toString(model.compared), status),
                new Fact("comparison-offset", Integer.toString(model.comparedOffset), status),
                new Fact("lcp-rank", Integer.toString(model.lcpRank), status),
                new Fact("lcp-value", model.lcpRank < 0 ? "none" : Integer.toString(model.lcp[model.lcpRank]), status),
                new Fact("teaching-equation", model.comparison, status),
                new Fact("teaching-detail", model.phase.equals("lcp")
                        ? "LCP counts equal leading characters of the two adjacent suffixes."
                        : "Compare from the left; the first different character decides lexicographic order.", SnapshotStatus.DEFAULT),
                new Fact("phase", model.phase, SnapshotStatus.DEFAULT),
                new Fact("text", RemainingStringSupport.boundedText(model.text), SnapshotStatus.DEFAULT),
                new Fact("suffix-array", suffixArray, status),
                new Fact("order", suffixArray, status),
                new Fact("lcp", lcp, status),
                new Fact("lcp-array", lcp, status),
                new Fact("candidate", Integer.toString(model.candidate),
                        model.candidate >= 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                new Fact("comparison", model.comparison, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("text", "banana");
        return new SimulationMetadata(
                TYPE,
                "Suffix Array",
                "O(n^3)",
                "O(n)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter exactly {\"text\":\"banana\"}; text must be lowercase ASCII and have length "
                        + MIN_TEXT_LENGTH + ".." + MAX_TEXT_LENGTH
                        + ". Empty text returns empty suffix-array and LCP arrays. This bounded lesson uses"
                        + " comparison sorting and computes each adjacent LCP character by character.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final String text;
        private final int[] order;
        private final boolean[] orderKnown;
        private final int[] lcp;
        private final boolean[] lcpKnown;
        private String phase = "initialize";
        private String comparison = "pending";
        private int sortedCount;
        private int candidate = -1;
        private int compared = -1;
        private int comparedOffset = -1;
        private int lcpRank = -1;
        private int lcpOffset;

        private Model(String text) {
            this.text = text;
            this.order = new int[text.length()];
            this.orderKnown = new boolean[text.length()];
            this.lcp = new int[text.length()];
            this.lcpKnown = new boolean[text.length()];
        }
    }
}
