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

/** Rolling polynomial hashes with mandatory character verification on hash hits. */
public final class StringHashingEngine implements SimulationEngine {
    public static final String TYPE = "STRING_HASHING";
    public static final int MIN_TEXT_LENGTH = 0;
    public static final int MAX_TEXT_LENGTH = RemainingStringSupport.MAX_HASH_TEXT_LENGTH;
    public static final int MIN_PATTERN_LENGTH = 1;
    public static final int MAX_PATTERN_LENGTH = RemainingStringSupport.MAX_HASH_PATTERN_LENGTH;
    public static final int MAX_TRACE_STEPS = RemainingStringSupport.MAX_TRACE_STEPS;

    /** Deliberately small, documented constants keep collision verification visible in the lesson. */
    public static final int HASH_BASE = 31;
    public static final int HASH_MODULUS = 101;
    public static final int BASE = HASH_BASE;
    public static final int MOD = HASH_MODULUS;
    public static final int MODULUS = HASH_MODULUS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_PATTERN_HASH = 2;
    private static final int LINE_POWER = 3;
    private static final int LINE_INITIAL_WINDOW = 4;
    private static final int LINE_SCAN = 5;
    private static final int LINE_HASH_COMPARE = 6;
    private static final int LINE_VERIFY = 7;
    private static final int LINE_REPORT_MATCH = 8;
    private static final int LINE_SLIDE = 9;
    private static final int LINE_RETURN = 10;

    private static final List<String> PSEUDOCODE = List.of(
            "find(text, pattern):",
            "    patternHash = hash(pattern)",
            "    highPower = base^(pattern.length - 1) mod modulus",
            "    windowHash = hash(text[0..pattern.length - 1])",
            "    for each text window:",
            "        if windowHash == patternHash:",
            "            verify every character before accepting a match",
            "            report the window only after verification succeeds",
            "        windowHash = ((windowHash - out * highPower) * base + in) mod modulus",
            "return verified match starting indexes");

    private static final List<String> TABLE_COLUMNS = List.of(
            "index", "char", "window", "window-hash", "pattern-hash", "verification");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        JsonNode object = RemainingStringSupport.requireExactObject(input, TYPE, "text", "pattern");
        String text = RemainingStringSupport.readLowercaseString(
                object, TYPE, "text", MIN_TEXT_LENGTH, MAX_TEXT_LENGTH);
        String pattern = RemainingStringSupport.readLowercaseString(
                object, TYPE, "pattern", MIN_PATTERN_LENGTH, MAX_PATTERN_LENGTH);
        Model model = new Model(text, pattern);
        List<SimulationStep> steps = new ArrayList<>();

        add(
                steps,
                model,
                0,
                "Initialize rolling-hash search for text " + RemainingStringSupport.boundedText(text)
                        + " and pattern " + pattern,
                StepEventType.INITIALIZE);
        add(
                steps,
                model,
                LINE_METHOD,
                "Build the pattern hash and scan fixed-size text windows",
                StepEventType.EXECUTE_LINE);

        model.phase = "pattern-hash";
        model.highPower = 1L;
        for (int offset = 1; offset < pattern.length(); offset++) {
            model.highPower = (model.highPower * HASH_BASE) % HASH_MODULUS;
        }
        model.operation = "highPower = " + model.highPower;
        add(
                steps,
                model,
                LINE_POWER,
                "Compute base^(pattern length - 1) modulo " + HASH_MODULUS + " = " + model.highPower,
                StepEventType.EXECUTE_LINE);
        for (int offset = 0; offset < pattern.length(); offset++) {
            model.patternHash = appendHash(model.patternHash, pattern.charAt(offset));
            model.patternKnown = offset + 1;
            model.operation = "patternHash after pattern[" + offset + "] = " + model.patternHash;
            add(
                    steps,
                    model,
                    LINE_PATTERN_HASH,
                    "Append pattern[" + offset + "] and update pattern hash to " + model.patternHash,
                    StepEventType.EXECUTE_LINE);
        }

        model.phase = "window-hash";
        if (text.length() < pattern.length()) {
            model.operation = "text is shorter than pattern; no window exists";
            add(
                    steps,
                    model,
                    LINE_INITIAL_WINDOW,
                    "The text is shorter than the pattern, so the scan has no windows",
                    StepEventType.EXECUTE_LINE);
        } else {
            model.windowStart = 0;
            model.lastWindowStart = 0;
            model.currentHash = 0L;
            model.hashKnown = true;
            for (int offset = 0; offset < pattern.length(); offset++) {
                model.currentHash = appendHash(model.currentHash, text.charAt(offset));
                model.operation = "windowHash for text[0.." + offset + "] = " + model.currentHash;
                add(
                        steps,
                        model,
                        LINE_INITIAL_WINDOW,
                        "Build the initial window hash through text index " + offset,
                        StepEventType.EXECUTE_LINE);
            }

            model.phase = "scan";
            int windowCount = text.length() - pattern.length() + 1;
            for (int start = 0; start < windowCount; start++) {
                if (start > 0) {
                    slideWindow(model, text, start);
                    add(
                            steps,
                            model,
                            LINE_SLIDE,
                            "Slide to text window starting at " + start + " and update hash to " + model.currentHash,
                            StepEventType.EXECUTE_LINE);
                }
                model.windowStart = start;
                model.lastWindowStart = start;
                model.activeWindow = true;
                model.comparisonTextIndex = -1;
                model.verification = "pending";
                model.operation = "inspect window text[" + start + ".."
                        + (start + pattern.length() - 1) + "]";
                add(
                        steps,
                        model,
                        LINE_SCAN,
                        "Inspect text window starting at " + start,
                        StepEventType.EXECUTE_LINE);

                if (model.currentHash != model.patternHash) {
                    model.windowResults[start] = WindowResult.HASH_MISS.code;
                    model.verification = "not needed: hash mismatch";
                    model.operation = "window hash " + model.currentHash
                            + " != pattern hash " + model.patternHash;
                    add(
                            steps,
                            model,
                            LINE_HASH_COMPARE,
                            "Hash mismatch rejects window " + start + " without a character match",
                            StepEventType.EXECUTE_LINE);
                } else {
                    model.hashHitCount++;
                    model.operation = "window hash equals pattern hash; verify characters";
                    add(
                            steps,
                            model,
                            LINE_HASH_COMPARE,
                            "Hash hit at window " + start + "; verify every character",
                            StepEventType.EXECUTE_LINE);
                    boolean equal = verifyWindow(model, steps, text, pattern, start);
                    if (equal) {
                        model.windowResults[start] = WindowResult.MATCH.code;
                        model.matches.add(start);
                        model.verification = "all characters match";
                        model.operation = "verified match at " + start;
                        add(
                                steps,
                                model,
                                LINE_REPORT_MATCH,
                                "Character verification accepts match at text index " + start,
                                StepEventType.EXECUTE_LINE);
                    } else {
                        model.windowResults[start] = WindowResult.COLLISION.code;
                        model.collisionCount++;
                        model.verification = "hash collision rejected by character check";
                        model.operation = "hash hit at " + start + " was not a character match";
                        add(
                                steps,
                                model,
                                LINE_VERIFY,
                                "Character verification rejects the hash hit as a collision",
                                StepEventType.EXECUTE_LINE);
                    }
                }
                model.activeWindow = false;
                model.comparisonTextIndex = -1;
            }
        }

        model.phase = "return";
        model.windowStart = -1;
        model.activeWindow = false;
        model.comparisonTextIndex = -1;
        model.operation = "verified matches = " + model.matches;
        add(
                steps,
                model,
                LINE_RETURN,
                "Return only character-verified match starts " + model.matches,
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: " + model.matches.size() + " verified match(es) at " + model.matches,
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private static boolean verifyWindow(
            Model model,
            List<SimulationStep> steps,
            String text,
            String pattern,
            int start) {
        for (int offset = 0; offset < pattern.length(); offset++) {
            model.comparisonTextIndex = start + offset;
            char textCharacter = text.charAt(start + offset);
            char patternCharacter = pattern.charAt(offset);
            boolean equal = textCharacter == patternCharacter;
            model.operation = "verify text[" + (start + offset) + "] "
                    + RemainingStringSupport.quotedChar(text, start + offset)
                    + (equal ? " == " : " != ")
                    + " pattern[" + offset + "] "
                    + RemainingStringSupport.quotedChar(pattern, offset);
            add(
                    steps,
                    model,
                    LINE_VERIFY,
                    equal
                            ? "Verify matching character at offset " + offset
                            : "Verification mismatch at offset " + offset,
                    StepEventType.EXECUTE_LINE);
            if (!equal) {
                return false;
            }
        }
        return true;
    }

    private static void slideWindow(Model model, String text, int start) {
        int previousStart = start - 1;
        char outgoing = text.charAt(previousStart);
        char incoming = text.charAt(start + model.pattern.length() - 1);
        model.oldWindowHash = model.currentHash;
        model.outgoingValue = value(outgoing);
        model.incomingValue = value(incoming);
        long removed = (value(outgoing) * model.highPower) % HASH_MODULUS;
        model.currentHash = normalize(model.currentHash - removed);
        model.currentHash = (model.currentHash * HASH_BASE + value(incoming)) % HASH_MODULUS;
        model.windowStart = start;
        model.lastWindowStart = start;
        model.activeWindow = true;
        model.comparisonTextIndex = -1;
        model.verification = "pending";
        model.rollingEquation = "((" + model.oldWindowHash + " − " + model.outgoingValue + " × "
                + model.highPower + ") × " + HASH_BASE + " + " + model.incomingValue + ") mod "
                + HASH_MODULUS + " = " + model.currentHash;
        model.operation = "Slide to text[" + start + ".." + (start + model.pattern.length() - 1)
                + "]: remove '" + outgoing + "', append '" + incoming + "'";
    }

    private static long appendHash(long current, char character) {
        return (current * HASH_BASE + value(character)) % HASH_MODULUS;
    }

    private static int value(char character) {
        return character - 'a' + 1;
    }

    private static long normalize(long value) {
        long result = value % HASH_MODULUS;
        return result < 0 ? result + HASH_MODULUS : result;
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
                facts(model, line),
                line,
                narration,
                eventType));
    }

    private static List<List<TypedCell>> rows(Model model) {
        int rowCount = Math.max(1, model.text.length());
        List<List<TypedCell>> rows = new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            boolean hasText = index < model.text.length();
            SnapshotStatus characterStatus = characterStatus(model, index);
            boolean inWindow = model.activeWindow
                    && model.windowStart >= 0
                    && index >= model.windowStart
                    && index < model.windowStart + model.pattern.length();
            SnapshotStatus windowStatus = inWindow ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT;
            SnapshotStatus hashStatus = index == model.windowStart && model.hashKnown
                    ? SnapshotStatus.ACTIVE
                    : SnapshotStatus.DEFAULT;
            SnapshotStatus patternHashStatus = index == 0 && model.patternKnown == model.pattern.length()
                    ? SnapshotStatus.DONE
                    : SnapshotStatus.DEFAULT;
            SnapshotStatus verificationStatus = index == model.comparisonTextIndex
                    ? (model.operation.contains(" != ") ? SnapshotStatus.REJECTED : SnapshotStatus.ACTIVE)
                    : SnapshotStatus.DEFAULT;
            String windowValue = inWindow
                    ? (index == model.windowStart ? "start=" + model.windowStart : "inside")
                    : "·";
            String hashValue = index == model.windowStart && model.hashKnown
                    ? Long.toString(model.currentHash)
                    : "·";
            String patternHashValue = index == 0 && model.patternKnown > 0
                    ? Long.toString(model.patternHash)
                    : "·";
            String verification = index == model.comparisonTextIndex
                    ? model.operation
                    : index == model.windowStart ? model.verification : "·";
            rows.add(RemainingStringSupport.row(
                    RemainingStringSupport.cell("index-" + index,
                            hasText ? Integer.toString(index) : "-", characterStatus),
                    RemainingStringSupport.cell("char-" + index,
                            hasText ? Character.toString(model.text.charAt(index)) : "·", characterStatus),
                    RemainingStringSupport.cell("window-" + index, windowValue, windowStatus),
                    RemainingStringSupport.cell("window-hash-" + index, hashValue, hashStatus),
                    RemainingStringSupport.cell("pattern-hash-" + index, patternHashValue, patternHashStatus),
                    RemainingStringSupport.cell("verification-" + index, verification, verificationStatus)));
        }
        return List.copyOf(rows);
    }

    private static SnapshotStatus characterStatus(Model model, int index) {
        if (model.activeWindow
                && model.windowStart >= 0
                && index >= model.windowStart
                && index < model.windowStart + model.pattern.length()) {
            return SnapshotStatus.ACTIVE;
        }
        for (int start = 0; start < model.windowResults.length; start++) {
            if (model.windowResults[start] == WindowResult.MATCH.code
                    && index >= start
                    && index < start + model.pattern.length()) {
                return SnapshotStatus.DONE;
            }
            if (model.windowResults[start] == WindowResult.COLLISION.code
                    && index >= start
                    && index < start + model.pattern.length()) {
                return SnapshotStatus.REJECTED;
            }
            if (model.windowResults[start] == WindowResult.HASH_MISS.code
                    && index >= start
                    && index < start + model.pattern.length()) {
                return SnapshotStatus.DONE;
            }
        }
        return SnapshotStatus.DEFAULT;
    }

    private static List<Fact> facts(Model model, int line) {
        SnapshotStatus status = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        String currentWindow = model.lastWindowStart < 0
                ? "none"
                : "text[" + model.lastWindowStart + ".."
                        + (model.lastWindowStart + model.pattern.length() - 1) + "]";
        String hash = model.hashKnown ? Long.toString(model.currentHash) : "none";
        return List.of(
                new Fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                new Fact("window-start", Integer.toString(model.lastWindowStart), status),
                new Fact("window-text", model.lastWindowStart < 0 ? ""
                        : model.text.substring(model.lastWindowStart, model.lastWindowStart + model.pattern.length()), status),
                new Fact("rolling-equation", model.rollingEquation, status),
                new Fact("old-window-hash", Long.toString(model.oldWindowHash), status),
                new Fact("outgoing-value", Integer.toString(model.outgoingValue), status),
                new Fact("incoming-value", Integer.toString(model.incomingValue), status),
                new Fact("teaching-equation", line == LINE_SLIDE ? model.rollingEquation : model.operation, status),
                new Fact("teaching-detail", "a=1, b=2, …, z=26. Equal hashes still require character verification.", SnapshotStatus.DEFAULT),
                new Fact("phase", model.phase, SnapshotStatus.DEFAULT),
                new Fact("text", RemainingStringSupport.boundedText(model.text), SnapshotStatus.DEFAULT),
                new Fact("pattern", model.pattern, SnapshotStatus.DEFAULT),
                new Fact("pattern-hash", Long.toString(model.patternHash), status),
                new Fact("hash-base", Integer.toString(HASH_BASE), SnapshotStatus.DEFAULT),
                new Fact("hash-modulus", Integer.toString(HASH_MODULUS), SnapshotStatus.DEFAULT),
                new Fact("high-power", Long.toString(model.highPower), SnapshotStatus.ACTIVE),
                new Fact("window", currentWindow, SnapshotStatus.ACTIVE),
                new Fact("hash", hash, status),
                new Fact("window-hash", hash, status),
                new Fact("verification", model.verification, status),
                new Fact("matches", model.matches.toString(), status),
                new Fact("hash-hits", Integer.toString(model.hashHitCount), status),
                new Fact("false-hash-hits", Integer.toString(model.collisionCount), status),
                new Fact("operation", model.operation, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("text", "abracadabra");
        defaultInput.put("pattern", "cada");
        return new SimulationMetadata(
                TYPE,
                "String Hashing",
                "O(n + m)",
                "O(n + m)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter exactly {\"text\":\"abracadabra\",\"pattern\":\"cada\"}; text must be lowercase ASCII"
                        + " with length " + MIN_TEXT_LENGTH + ".." + MAX_TEXT_LENGTH
                        + " and pattern must be lowercase ASCII with length " + MIN_PATTERN_LENGTH + ".."
                        + MAX_PATTERN_LENGTH + ". Empty text is valid and produces no matches. A rolling hash uses"
                        + " base " + HASH_BASE + " modulo " + HASH_MODULUS + "; every equal-hash window is checked"
                        + " character by character before it is reported, so collisions cannot become matches.",
                PSEUDOCODE);
    }

    private enum WindowResult {
        UNKNOWN(0),
        HASH_MISS(1),
        COLLISION(2),
        MATCH(3);

        private final int code;

        WindowResult(int code) {
            this.code = code;
        }
    }

    private static final class Model {
        private final String text;
        private final String pattern;
        private final int[] windowResults;
        private String phase = "initialize";
        private String operation = "pending";
        private String verification = "pending";
        private long patternHash;
        private long currentHash;
        private long oldWindowHash;
        private int outgoingValue;
        private int incomingValue;
        private String rollingEquation = "";
        private long highPower = 1L;
        private int patternKnown;
        private int windowStart = -1;
        private int lastWindowStart = -1;
        private int comparisonTextIndex = -1;
        private boolean hashKnown;
        private boolean activeWindow;
        private int hashHitCount;
        private int collisionCount;
        private final List<Integer> matches = new ArrayList<>();

        private Model(String text, String pattern) {
            this.text = text;
            this.pattern = pattern;
            this.windowResults = new int[Math.max(0, text.length() - pattern.length() + 1)];
        }
    }
}
