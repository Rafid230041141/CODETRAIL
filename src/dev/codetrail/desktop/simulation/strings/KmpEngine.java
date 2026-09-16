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

/** Knuth-Morris-Pratt prefix-table construction and overlapping-match trace. */
public final class KmpEngine implements SimulationEngine {
    public static final String TYPE = "KMP";
    public static final int MIN_TEXT_LENGTH = 0;
    public static final int MAX_TEXT_LENGTH = StringSimulationSupport.MAX_TEXT_LENGTH;
    public static final int MIN_PATTERN_LENGTH = 1;
    public static final int MAX_PATTERN_LENGTH = StringSimulationSupport.MAX_PATTERN_LENGTH;
    public static final int MAX_TRACE_STEPS = StringSimulationSupport.MAX_TRACE_STEPS;

    private static final int LINE_BUILD = 1;
    private static final int LINE_LPS_INITIALIZE = 2;
    private static final int LINE_BUILD_LOOP = 3;
    private static final int LINE_COMPARE = 4;
    private static final int LINE_LPS_ASSIGN = 5;
    private static final int LINE_LPS_ADVANCE = 6;
    private static final int LINE_LPS_FALLBACK = 7;
    private static final int LINE_LPS_ZERO = 8;
    private static final int LINE_SEARCH = 9;
    private static final int LINE_SEARCH_INITIALIZE = 10;
    private static final int LINE_SEARCH_LOOP = 11;
    private static final int LINE_MATCH_ADVANCE = 12;
    private static final int LINE_REPORT_MATCH = 13;
    private static final int LINE_MATCH_FALLBACK = 14;
    private static final int LINE_MISMATCH_FALLBACK = 15;
    private static final int LINE_MISMATCH_ADVANCE = 16;
    private static final int LINE_RETURN = 17;

    private static final List<String> PSEUDOCODE = List.of(
            "buildLps(pattern):",
            "    lps[0] = 0; length = 0; i = 1",
            "    while i < pattern.length:",
            "        if pattern[i] == pattern[length]:",
            "            lps[i] = length + 1",
            "            i++; length++",
            "        else if length > 0: length = lps[length - 1]",
            "        else: lps[i] = 0; i++",
            "search(text, pattern, lps):",
            "    i = 0; j = 0",
            "    while i < text.length:",
            "        if text[i] == pattern[j]: i++; j++",
            "            if j == pattern.length: report i - pattern.length",
            "                j = lps[pattern.length - 1]  // keep overlaps",
            "        else if j > 0: j = lps[j - 1]  // mismatch jump",
            "        else: i++  // shift one character",
            "return all match starting indexes");

    private static final List<String> TABLE_COLUMNS = List.of(
            "text-index", "text-char", "aligned-pattern", "pattern-index", "lps", "comparison");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        String text = StringSimulationSupport.readLowercaseString(
                input, TYPE, "text", MIN_TEXT_LENGTH, MAX_TEXT_LENGTH);
        String pattern = StringSimulationSupport.readLowercaseString(
                input, TYPE, "pattern", MIN_PATTERN_LENGTH, MAX_PATTERN_LENGTH);
        Model model = new Model(text, pattern);
        List<SimulationStep> steps = new ArrayList<>();

        add(
                steps,
                model,
                0,
                "Initialize KMP for text " + StringSimulationSupport.boundedText(text)
                        + " and pattern " + pattern,
                StepEventType.INITIALIZE);
        model.phase = "lps";
        model.lpsIndex = 0;
        model.lpsLength = 0;
        add(
                steps,
                model,
                LINE_BUILD,
                "Build the longest-proper-prefix/suffix table for the pattern",
                StepEventType.EXECUTE_LINE);
        model.lps[0] = 0;
        model.lpsKnown[0] = true;
        add(
                steps,
                model,
                LINE_LPS_INITIALIZE,
                "Set lps[0] = 0 because a one-character prefix has no proper border",
                StepEventType.EXECUTE_LINE);
        model.lpsIndex = 1;

        while (model.lpsIndex < pattern.length()) {
            int index = model.lpsIndex;
            add(
                    steps,
                    model,
                    LINE_BUILD_LOOP,
                    "Inspect pattern index " + index + " with reusable prefix length " + model.lpsLength,
                    StepEventType.EXECUTE_LINE);
            model.lastPatternIndex = index;
            model.compareLeftIndex = model.lpsLength;
            model.compareRightIndex = index;
            model.fallbackIndex = -1;
            model.comparison = "compare pattern[" + index + "] "
                    + StringSimulationSupport.quotedChar(pattern, index)
                    + " with pattern[" + model.lpsLength + "] "
                    + StringSimulationSupport.quotedChar(pattern, model.lpsLength);
            boolean equal = pattern.charAt(index) == pattern.charAt(model.lpsLength);
            model.comparison += equal ? " => match" : " => mismatch";
            add(
                    steps,
                    model,
                    LINE_COMPARE,
                    equal
                            ? "Pattern characters match while building lps[" + index + "]"
                            : "Pattern characters mismatch at index " + index,
                    StepEventType.EXECUTE_LINE);
            if (equal) {
                model.lps[index] = model.lpsLength + 1;
                model.lpsKnown[index] = true;
                add(
                        steps,
                        model,
                        LINE_LPS_ASSIGN,
                        "Write lps[" + index + "] = " + model.lps[index],
                        StepEventType.EXECUTE_LINE);
                model.lpsIndex++;
                model.lpsLength++;
                model.comparison = "advance lps builder to i=" + model.lpsIndex
                        + ", length=" + model.lpsLength;
                add(
                        steps,
                        model,
                        LINE_LPS_ADVANCE,
                        "Advance the lps builder to the next pattern index",
                        StepEventType.EXECUTE_LINE);
            } else if (model.lpsLength > 0) {
                int previousLength = model.lpsLength;
                model.fallbackIndex = previousLength - 1;
                model.lpsLength = model.lps[model.lpsLength - 1];
                model.comparison = "fallback length from " + previousLength + " to " + model.lpsLength;
                add(
                        steps,
                        model,
                        LINE_LPS_FALLBACK,
                        "Mismatch: jump length from " + previousLength + " to lps["
                                + (previousLength - 1) + "] = " + model.lpsLength
                                + "; retry the same i",
                        StepEventType.EXECUTE_LINE);
            } else {
                model.lps[index] = 0;
                model.lpsKnown[index] = true;
                model.comparison = "no reusable border; set lps[" + index + "] = 0";
                add(
                        steps,
                        model,
                        LINE_LPS_ZERO,
                        "No border remains; set lps[" + index + "] = 0 and advance i",
                        StepEventType.EXECUTE_LINE);
                model.lpsIndex++;
                add(
                        steps,
                        model,
                        LINE_LPS_ADVANCE,
                        "Advance past the zero LPS entry",
                        StepEventType.EXECUTE_LINE);
            }
        }

        model.phase = "search";
        model.compareLeftIndex = -1;
        model.compareRightIndex = -1;
        model.fallbackIndex = -1;
        model.lpsIndex = -1;
        model.lpsLength = 0;
        model.i = 0;
        model.j = 0;
        model.currentTextIndex = text.isEmpty() ? -1 : 0;
        model.currentPatternIndex = text.isEmpty() ? -1 : 0;
        model.alignment = text.isEmpty() ? -1 : 0;
        model.comparison = "lps complete; start scanning text";
        add(
                steps,
                model,
                LINE_SEARCH,
                "Search the text with the completed LPS table",
                StepEventType.EXECUTE_LINE);
        add(
                steps,
                model,
                LINE_SEARCH_INITIALIZE,
                "Initialize text index i = 0 and pattern index j = 0",
                StepEventType.EXECUTE_LINE);

        while (model.i < text.length()) {
            model.fallbackIndex = -1;
            int textIndex = model.i;
            int patternIndex = model.j;
            model.alignment = textIndex - patternIndex;
            model.currentTextIndex = textIndex;
            model.currentPatternIndex = patternIndex;
            model.lastTextIndex = -1;
            model.lastPatternIndex = -1;
            model.comparison = "compare text[" + textIndex + "] "
                    + StringSimulationSupport.quotedChar(text, textIndex)
                    + " with pattern[" + patternIndex + "] "
                    + StringSimulationSupport.quotedChar(pattern, patternIndex);
            add(
                    steps,
                    model,
                    LINE_SEARCH_LOOP,
                    "Align pattern at text start " + model.alignment + " and inspect text index " + textIndex,
                    StepEventType.EXECUTE_LINE);

            boolean equal = text.charAt(textIndex) == pattern.charAt(patternIndex);
            model.examinedText[textIndex] = true;
            model.lastTextIndex = textIndex;
            model.lastPatternIndex = patternIndex;
            model.comparison += equal ? " => match" : " => mismatch";
            if (equal) {
                model.i++;
                model.j++;
                model.currentTextIndex = model.i < text.length() ? model.i : -1;
                model.currentPatternIndex = model.j < pattern.length() ? model.j : -1;
                model.alignment = model.i - model.j;
                add(
                        steps,
                        model,
                        LINE_MATCH_ADVANCE,
                        "Match at text[" + textIndex + "]; advance to i=" + model.i + ", j=" + model.j,
                        StepEventType.EXECUTE_LINE);
                if (model.j == pattern.length()) {
                    int matchStart = model.i - pattern.length();
                    model.matches.add(matchStart);
                    for (int matchedIndex = matchStart;
                            matchedIndex < model.i;
                            matchedIndex++) {
                        model.matchedText[matchedIndex] = true;
                    }
                    model.alignment = matchStart;
                    model.currentPatternIndex = -1;
                    model.comparison = "complete match at text start " + matchStart;
                    add(
                            steps,
                            model,
                            LINE_REPORT_MATCH,
                            "Report match starting at text index " + matchStart
                                    + "; keep searching for overlaps",
                            StepEventType.EXECUTE_LINE);
                    model.fallbackIndex = pattern.length() - 1;
                    int fallback = model.lps[pattern.length() - 1];
                    model.j = fallback;
                    model.currentTextIndex = model.i < text.length() ? model.i : -1;
                    model.currentPatternIndex = model.j < pattern.length() ? model.j : -1;
                    model.alignment = model.i - model.j;
                    model.comparison = "after match, fallback j to lps[m - 1] = " + fallback;
                    add(
                            steps,
                            model,
                            LINE_MATCH_FALLBACK,
                            "After the match, jump j to " + fallback + " so overlapping matches remain possible",
                            StepEventType.EXECUTE_LINE);
                }
            } else if (model.j > 0) {
                int previousPatternIndex = model.j;
                model.fallbackIndex = previousPatternIndex - 1;
                model.j = model.lps[model.j - 1];
                model.currentPatternIndex = model.j;
                model.alignment = model.i - model.j;
                model.comparison = "mismatch; jump j from " + previousPatternIndex
                        + " to lps[" + (previousPatternIndex - 1) + "] = " + model.j;
                add(
                        steps,
                        model,
                        LINE_MISMATCH_FALLBACK,
                        "Mismatch at text index " + textIndex + "; jump pattern index from "
                                + previousPatternIndex + " to " + model.j + " without advancing i",
                        StepEventType.EXECUTE_LINE);
            } else {
                model.i++;
                model.currentTextIndex = model.i < text.length() ? model.i : -1;
                model.currentPatternIndex = 0;
                model.alignment = model.i;
                model.comparison = "mismatch at j=0; advance text index to " + model.i;
                add(
                        steps,
                        model,
                        LINE_MISMATCH_ADVANCE,
                        "Mismatch with no reusable prefix; advance to text index " + model.i,
                        StepEventType.EXECUTE_LINE);
            }
        }

        model.currentTextIndex = -1;
        model.currentPatternIndex = -1;
        model.comparison = "text exhausted; matches = " + model.matches;
        add(
                steps,
                model,
                LINE_RETURN,
                "Return all match starting indexes " + model.matches,
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: KMP found " + model.matches.size() + " match(es) at " + model.matches,
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
        int slotCount = Math.max(1, model.text.length() + model.pattern.length());
        List<List<TypedCell>> rows = new ArrayList<>(slotCount);
        for (int slot = 0; slot < slotCount; slot++) {
            boolean hasText = slot < model.text.length();
            int patternSlot = patternSlot(model, slot);
            boolean hasPattern = patternSlot >= 0 && patternSlot < model.pattern.length();
            boolean hasLps = slot < model.pattern.length();
            SnapshotStatus textStatus = hasText ? textStatus(model, slot) : SnapshotStatus.DEFAULT;
            SnapshotStatus patternStatus = hasPattern
                    ? patternStatus(model, patternSlot)
                    : SnapshotStatus.DEFAULT;
            SnapshotStatus lpsStatus = hasLps ? lpsStatus(model, slot) : SnapshotStatus.DEFAULT;
            SnapshotStatus comparisonStatus = slot == model.currentTextIndex
                    ? SnapshotStatus.ACTIVE
                    : slot == model.lastTextIndex
                            ? comparisonStatus(model)
                            : SnapshotStatus.DEFAULT;
            rows.add(StringSimulationSupport.row(
                    StringSimulationSupport.cell("text-index-" + slot,
                            hasText ? Integer.toString(slot) : "-", textStatus),
                    StringSimulationSupport.cell("text-char-" + slot,
                            hasText ? Character.toString(model.text.charAt(slot)) : "·", textStatus),
                    StringSimulationSupport.cell("aligned-pattern-" + slot,
                            hasPattern ? Character.toString(model.pattern.charAt(patternSlot)) : "·", patternStatus),
                    StringSimulationSupport.cell("pattern-index-" + slot,
                            hasPattern ? Integer.toString(patternSlot) : "·", patternStatus),
                    StringSimulationSupport.cell("lps-" + slot,
                            hasLps
                                    ? (model.lpsKnown[slot] ? Integer.toString(model.lps[slot]) : "?")
                                    : "·",
                            lpsStatus),
                    StringSimulationSupport.cell("comparison-" + slot,
                            slot == model.lastTextIndex ? model.comparison : "·", comparisonStatus)));
        }
        return List.copyOf(rows);
    }

    private static int patternSlot(Model model, int slot) {
        if (model.phase.equals("search") || model.phase.equals("complete")) {
            int alignment = model.alignment;
            if (alignment >= 0) {
                return slot - alignment;
            }
        }
        return slot < model.pattern.length() ? slot : -1;
    }

    private static SnapshotStatus textStatus(Model model, int index) {
        if (model.phase.equals("search") && index == model.currentTextIndex) {
            return SnapshotStatus.ACTIVE;
        }
        if (model.matchedText[index]) {
            return SnapshotStatus.DONE;
        }
        if (model.examinedText[index]) {
            return SnapshotStatus.REJECTED;
        }
        return SnapshotStatus.DEFAULT;
    }

    private static SnapshotStatus patternStatus(Model model, int patternIndex) {
        if (model.phase.equals("complete")) {
            return SnapshotStatus.DONE;
        }
        if (model.phase.equals("lps")) {
            if (patternIndex == model.lpsIndex || patternIndex == model.compareLeftIndex
                    || patternIndex == model.compareRightIndex) {
                return SnapshotStatus.ACTIVE;
            }
            return model.lpsKnown[patternIndex] ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
        }
        int currentPattern = model.currentPatternIndex;
        if (currentPattern >= 0 && patternIndex == currentPattern) {
            return SnapshotStatus.ACTIVE;
        }
        if (model.lastPatternIndex == patternIndex && model.comparison.contains("mismatch")) {
            return SnapshotStatus.REJECTED;
        }
        if (model.lastPatternIndex == patternIndex && model.comparison.contains("match")) {
            return SnapshotStatus.DONE;
        }
        if (currentPattern > patternIndex && patternIndex >= 0) {
            return SnapshotStatus.DONE;
        }
        return SnapshotStatus.DEFAULT;
    }

    private static SnapshotStatus lpsStatus(Model model, int index) {
        if (index == model.fallbackIndex || model.phase.equals("lps") && index == model.lpsIndex) {
            return SnapshotStatus.ACTIVE;
        }
        return model.lpsKnown[index] ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
    }

    private static SnapshotStatus comparisonStatus(Model model) {
        return model.comparison.contains("mismatch") ? SnapshotStatus.REJECTED : SnapshotStatus.DONE;
    }

    private static List<Fact> facts(Model model) {
        String alignment = model.phase.equals("lps")
                ? "not scanning while LPS is built"
                : StringSimulationSupport.alignment(model.text, model.pattern, model.i, model.j);
        SnapshotStatus stateStatus = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        return List.of(
                new Fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                new Fact("compare-left-index", Integer.toString(model.compareLeftIndex), stateStatus),
                new Fact("compare-right-index", Integer.toString(model.compareRightIndex), stateStatus),
                new Fact("fallback-index", Integer.toString(model.fallbackIndex), stateStatus),
                new Fact("lps-index", Integer.toString(model.lpsIndex), stateStatus),
                new Fact("lps-length", Integer.toString(model.lpsLength), stateStatus),
                new Fact("alignment-start", Integer.toString(model.alignment), stateStatus),
                new Fact("text-compare-index", Integer.toString(model.lastTextIndex >= 0 ? model.lastTextIndex : model.currentTextIndex), stateStatus),
                new Fact("pattern-compare-index", Integer.toString(model.lastPatternIndex >= 0 ? model.lastPatternIndex : model.currentPatternIndex), stateStatus),
                new Fact("teaching-equation", model.comparison, stateStatus),
                new Fact("teaching-detail", model.fallbackIndex >= 0
                        ? "The highlighted LPS entry chooses the shorter border; the text position stays fixed."
                        : "LPS stores the longest proper prefix that is also a suffix.", SnapshotStatus.DEFAULT),
                new Fact("phase", model.phase, SnapshotStatus.DEFAULT),
                new Fact("text", StringSimulationSupport.boundedText(model.text), SnapshotStatus.DEFAULT),
                new Fact("pattern", model.pattern, SnapshotStatus.DEFAULT),
                new Fact("lps", StringSimulationSupport.formatKnownArray(model.lps, model.lpsKnown), stateStatus),
                new Fact("i", Integer.toString(model.i), SnapshotStatus.ACTIVE),
                new Fact("j", Integer.toString(model.j), SnapshotStatus.ACTIVE),
                new Fact("alignment", alignment, SnapshotStatus.ACTIVE),
                new Fact("comparison", model.comparison, stateStatus),
                new Fact("matches", StringSimulationSupport.formatMatches(model.matches), stateStatus),
                new Fact("match-count", Integer.toString(model.matches.size()), stateStatus));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("text", "ababcabcabababd");
        defaultInput.put("pattern", "ababd");
        return new SimulationMetadata(
                TYPE,
                "Knuth-Morris-Pratt (KMP)",
                "O(n + m)",
                "O(m)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"text\":\"ababcabcabababd\",\"pattern\":\"ababd\"}; text length must be "
                        + MIN_TEXT_LENGTH + ".." + MAX_TEXT_LENGTH
                        + ", pattern length must be " + MIN_PATTERN_LENGTH + ".." + MAX_PATTERN_LENGTH
                        + ", and both strings must contain only lowercase ASCII letters. KMP reports every match, "
                        + "including overlapping matches, or an empty list when the pattern is absent.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final String text;
        private final String pattern;
        private final int[] lps;
        private final boolean[] lpsKnown;
        private final boolean[] examinedText;
        private final boolean[] matchedText;
        private final List<Integer> matches = new ArrayList<>();
        private String phase = "initialize";
        private String comparison = "pending";
        private int lpsIndex = -1;
        private int lpsLength;
        private int i;
        private int j;
        private int alignment = -1;
        private int currentTextIndex = -1;
        private int currentPatternIndex = -1;
        private int lastTextIndex = -1;
        private int lastPatternIndex = -1;
        private int compareLeftIndex = -1;
        private int compareRightIndex = -1;
        private int fallbackIndex = -1;

        private Model(String text, String pattern) {
            this.text = text;
            this.pattern = pattern;
            this.lps = new int[pattern.length()];
            this.lpsKnown = new boolean[pattern.length()];
            this.examinedText = new boolean[text.length()];
            this.matchedText = new boolean[text.length()];
        }
    }
}
