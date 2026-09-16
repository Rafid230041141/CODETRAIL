package dev.codetrail.desktop.simulation.structures.trees;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TreeState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Trie insertion, exact/prefix search, and restart-at-each-position matching. */
public final class TrieEngine implements SimulationEngine {
    public static final String TYPE = "TRIE";
    public static final int MIN_WORDS = 0;
    public static final int MAX_WORDS = 6;
    public static final int MAX_WORD_LENGTH = 8;
    public static final int MAX_TEXT_LENGTH = 32;
    public static final int MAX_TRIE_NODES = 40;
    public static final int MAX_TRACE_STEPS = 2048;

    private static final int LINE_ROOT = 1;
    private static final int LINE_STEP = 2;
    private static final int LINE_CREATE = 3;
    private static final int LINE_TERMINAL = 4;
    private static final int LINE_DUPLICATE = 5;
    private static final int LINE_SEARCH = 6;
    private static final int LINE_PREFIX = 7;
    private static final int LINE_MATCH_START = 8;
    private static final int LINE_MATCH_STEP = 9;
    private static final int LINE_MATCH_HIT = 10;
    private static final int LINE_MATCH_MISS = 11;
    private static final int LINE_RETURN = 12;

    private static final List<String> PSEUDOCODE = List.of(
            "start at the root",
            "for each character in the word or text suffix:",
            "    follow the matching child or create it",
            "mark the final word node terminal",
            "a duplicate word keeps its terminal marker",
            "search: accept only a terminal node",
            "prefix: accept after the prefix walk",
            "for start = 0 .. text.length - 1: restart at root",
            "    follow text characters through trie edges",
            "    report every terminal node reached",
            "    stop this start at the first missing edge",
            "return the trie and operation matches");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        Model model = new Model();
        Trace trace = new Trace(model);
        trace.add(
                0,
                "Initialize the trie root",
                StepEventType.INITIALIZE,
                facts(model, trace, "initialize", "none", "initialize", SnapshotStatus.ACTIVE));

        for (String word : request.words()) {
            trace.beginOperation();
            trace.operation = "insert(" + word + ")";
            trace.query = word;
            insertWord(model, word, trace);
            trace.finishOperation();
        }

        trace.beginOperation();
        trace.operation = request.operationDescription();
        trace.query = request.query();
        trace.path.clear();
        trace.matches.clear();
        trace.result = "pending";
        switch (request.operation()) {
            case "insert" -> insertWord(model, request.query(), trace);
            case "search" -> searchWord(model.root, request.query(), trace, false);
            case "prefix" -> searchWord(model.root, request.query(), trace, true);
            case "match" -> executeMatch(model, request.query(), trace);
            default -> throw new IllegalStateException("unsupported validated Trie operation: " + request.operation());
        }
        trace.finishOperation();

        trace.markAllDone();
        trace.add(
                LINE_RETURN,
                "Return trie result " + trace.result,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, request.operation(), trace.result, "return", SnapshotStatus.DONE));
        trace.add(
                0,
                "Complete: trie stores " + model.dictionary.size() + " distinct word(s)",
                StepEventType.COMPLETE,
                facts(model, trace, "complete", trace.result, "complete", SnapshotStatus.DONE));
        return trace.steps();
    }

    private static void insertWord(Model model, String word, Trace trace) {
        TreeNode current = model.root;
        trace.activate(current.id);
        trace.add(
                LINE_ROOT,
                "Start at trie root for word " + word,
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, "pending", "insert; root", SnapshotStatus.ACTIVE));
        trace.path.clear();
        for (int index = 0; index < word.length(); index++) {
            char character = word.charAt(index);
            trace.currentIndex = index;
            trace.currentCharacter = Character.toString(character);
            trace.path.add(character);
            trace.activate(current.id);
            trace.add(
                    LINE_STEP,
                    "Follow character '" + character + "' from node " + current.id,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, "pending", "insert; follow", SnapshotStatus.ACTIVE));
            TreeNode child = current.children.get(character);
            if (child == null) {
                child = model.newNode(character);
                current.children.put(character, child);
                trace.activate(child.id);
                trace.matchedPrefix = word.substring(0, index + 1);
                trace.rewrite = "edge " + current.id + " -" + character + "-> " + child.id;
                trace.add(
                        LINE_CREATE,
                        "Create node " + child.id + " for character '" + character + "'",
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, "pending", "insert; create", SnapshotStatus.ACTIVE));
            } else {
                trace.activate(child.id);
                trace.matchedPrefix = word.substring(0, index + 1);
                trace.add(
                        LINE_STEP,
                        "Reuse existing prefix node " + child.id + " for '" + character + "'",
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, "pending", "insert; shared prefix", SnapshotStatus.ACTIVE));
            }
            current = child;
        }
        if (current.terminal) {
            trace.result = "DUPLICATE_IGNORED";
            trace.add(
                    LINE_DUPLICATE,
                    "Word " + word + " is already terminal; do not duplicate its path",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; duplicate", SnapshotStatus.DONE));
        } else {
            current.terminal = true;
            model.addWord(word);
            trace.result = "INSERTED";
            trace.rewrite = "terminal(" + current.id + "): false -> true";
            trace.add(
                    LINE_TERMINAL,
                    "Mark node " + current.id + " terminal for word " + word,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "insert; terminal", SnapshotStatus.DONE));
        }
    }

    private static boolean searchWord(TreeNode root, String query, Trace trace, boolean prefix) {
        TreeNode current = root;
        trace.activate(current.id);
        trace.add(
                prefix ? LINE_PREFIX : LINE_SEARCH,
                "Start " + (prefix ? "prefix" : "exact") + " search for " + query,
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, "pending", prefix ? "prefix; start" : "search; start",
                        SnapshotStatus.ACTIVE));
        trace.path.clear();
        for (int index = 0; index < query.length(); index++) {
            char character = query.charAt(index);
            trace.currentIndex = index;
            trace.currentCharacter = Character.toString(character);
            trace.path.add(character);
            TreeNode child = current.children.get(character);
            if (child == null) {
                trace.result = prefix ? "NO" : "NOT_FOUND";
                trace.add(
                        prefix ? LINE_PREFIX : LINE_SEARCH,
                        "Missing edge for '" + character + "'; query is " + trace.result,
                        StepEventType.EXECUTE_LINE,
                        facts(trace.model, trace, trace.operation, trace.result,
                                prefix ? "prefix; miss" : "search; miss", SnapshotStatus.REJECTED));
                return false;
            }
            trace.activate(child.id);
            trace.matchedPrefix = query.substring(0, index + 1);
            trace.add(
                    prefix ? LINE_PREFIX : LINE_SEARCH,
                    "Follow edge labeled '" + character + "' to node " + child.id,
                    StepEventType.EXECUTE_LINE,
                    facts(trace.model, trace, trace.operation, "pending",
                            prefix ? "prefix; follow" : "search; follow", SnapshotStatus.ACTIVE));
            current = child;
        }
        boolean accepted = prefix || current.terminal;
        trace.result = accepted ? (prefix ? "YES" : "FOUND") : "NOT_FOUND";
        trace.add(
                prefix ? LINE_PREFIX : LINE_SEARCH,
                accepted
                        ? "Query " + query + " is accepted by the trie"
                        : "Path exists but node is not terminal; query " + query + " is NOT_FOUND",
                StepEventType.EXECUTE_LINE,
                facts(trace.model, trace, trace.operation, trace.result,
                        prefix ? "prefix; found" : "search; terminal", accepted ? SnapshotStatus.DONE : SnapshotStatus.REJECTED));
        return accepted;
    }

    private static void executeMatch(Model model, String text, Trace trace) {
        trace.matches.clear();
        if (text.isEmpty()) {
            trace.result = "0";
            trace.add(
                    LINE_MATCH_START,
                    "Text is empty; there are no start positions to scan",
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, trace.result, "match; empty text", SnapshotStatus.DONE));
            return;
        }
        for (int start = 0; start < text.length(); start++) {
            trace.finishPhase();
            trace.currentStart = start;
            trace.currentIndex = start;
            trace.currentCharacter = Character.toString(text.charAt(start));
            trace.matchedPrefix = "";
            trace.path.clear();
            TreeNode current = model.root;
            trace.activate(current.id);
            trace.add(
                    LINE_MATCH_START,
                    "Restart the trie walk at text position " + start,
                    StepEventType.EXECUTE_LINE,
                    facts(model, trace, trace.operation, Integer.toString(trace.matches.size()),
                            "match; restart", SnapshotStatus.ACTIVE));
            StringBuilder matched = new StringBuilder();
            boolean advanced = false;
            for (int index = start; index < text.length(); index++) {
                char character = text.charAt(index);
                trace.currentIndex = index;
                trace.currentCharacter = Character.toString(character);
                trace.path.add(character);
                TreeNode child = current.children.get(character);
                if (child == null) {
                    trace.add(
                            LINE_MATCH_MISS,
                            "Stop position " + start + " at text[" + index + "]='" + character + "' (missing edge)",
                            StepEventType.EXECUTE_LINE,
                            facts(model, trace, trace.operation, Integer.toString(trace.matches.size()),
                                    "match; miss", SnapshotStatus.REJECTED));
                    break;
                }
                advanced = true;
                matched.append(character);
                trace.matchedPrefix = matched.toString();
                trace.activate(child.id);
                trace.add(
                        LINE_MATCH_STEP,
                        "Follow text[" + index + "]='" + character + "' to node " + child.id,
                        StepEventType.EXECUTE_LINE,
                        facts(model, trace, trace.operation, Integer.toString(trace.matches.size()),
                                "match; follow", SnapshotStatus.ACTIVE));
                current = child;
                if (current.terminal) {
                    Match match = new Match(start, index, matched.toString());
                    trace.matches.add(match);
                    trace.add(
                            LINE_MATCH_HIT,
                            "Report match " + match.word + " at text positions " + start + ".." + index,
                            StepEventType.EXECUTE_LINE,
                            facts(model, trace, trace.operation, Integer.toString(trace.matches.size()),
                                    "match; hit", SnapshotStatus.DONE));
                }
            }
            if (!advanced) {
                // The missing-edge snapshot above explains this start position.
            }
        }
        trace.result = Integer.toString(trace.matches.size());
        trace.add(
                LINE_MATCH_HIT,
                "Finish overlapping scan with " + trace.result + " match(es)",
                StepEventType.EXECUTE_LINE,
                facts(model, trace, trace.operation, trace.result, "match; complete", SnapshotStatus.DONE));
    }

    private static List<Fact> facts(
            Model model,
            Trace trace,
            String operation,
            String result,
            String phase,
            SnapshotStatus status) {
        List<String> terminals = new ArrayList<>();
        terminalWords(model.root, new StringBuilder(), terminals);
        String matches = formatMatches(trace.matches);
        String positions = formatPositions(trace.matches);
        List<String> path = trace.path.stream().map(character -> Character.toString(character)).toList();
        String answer = switch (result) {
            case "FOUND", "YES" -> "YES";
            case "NOT_FOUND", "NO" -> "NO";
            default -> result;
        };
        return List.of(
                new Fact("operation", operation, SnapshotStatus.DEFAULT),
                new Fact("phase", phase, status),
                new Fact("result", result, status),
                new Fact("outcome", result, status),
                new Fact("answer", answer, status),
                new Fact("found", answer, status),
                new Fact("search-result", result, status),
                new Fact("query", trace.query, SnapshotStatus.DEFAULT),
                new Fact("text", trace.operation.startsWith("match") ? trace.query : "-", SnapshotStatus.DEFAULT),
                new Fact("words", OptionalSmallHelper.formatStrings(model.dictionary), status),
                new Fact("terminals", OptionalSmallHelper.formatStrings(terminals), status),
                new Fact("node-count", Integer.toString(model.nodeCount), status),
                new Fact("current-start", Integer.toString(trace.currentStart), status),
                new Fact("current-node", trace.currentNode, status),
                new Fact("current-index", Integer.toString(trace.currentIndex), status),
                new Fact("current-character", trace.currentCharacter, status),
                new Fact("matched-prefix", trace.matchedPrefix, status),
                new Fact("terminal-meaning", "* marks a complete stored word", SnapshotStatus.DEFAULT),
                new Fact("path", OptionalSmallHelper.formatStrings(path), status),
                new Fact("matches", matches, status),
                new Fact("match-list", matches, status),
                new Fact("match-positions", positions, status),
                new Fact("match-count", Integer.toString(trace.matches.size()), status),
                new Fact("rewrite", trace.rewrite, status));
    }

    private static void terminalWords(TreeNode node, StringBuilder prefix, List<String> output) {
        if (node == null) {
            return;
        }
        if (node.terminal && node.character != 0) {
            output.add(prefix.toString());
        }
        for (Map.Entry<Character, TreeNode> entry : node.children.entrySet()) {
            prefix.append(entry.getKey());
            terminalWords(entry.getValue(), prefix, output);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    private static String formatMatches(List<Match> matches) {
        List<String> formatted = new ArrayList<>(matches.size());
        for (Match match : matches) {
            formatted.add(match.word + "@" + match.start);
        }
        return formatted.toString();
    }

    private static String formatPositions(List<Match> matches) {
        List<String> formatted = new ArrayList<>(matches.size());
        for (Match match : matches) {
            formatted.add("(" + match.start + "," + match.end + ")");
        }
        return formatted.toString();
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode words = defaultInput.putArray("words");
        words.add("cat");
        words.add("car");
        words.add("dog");
        defaultInput.put("operation", "insert");
        defaultInput.put("word", "can");
        return new SimulationMetadata(
                TYPE,
                "Trie",
                "O(L) insert/search; O(nL) restart-at-each-position matching",
                "O(total dictionary characters)",
                RendererFamily.TREE,
                defaultInput,
                "Enter JSON as {\"words\":[\"cat\",\"car\",\"dog\"],\"operation\":\"insert\",\"word\":\"can\"}; words use lowercase ASCII and the operation may be insert, search, prefix, or match.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final TreeNode root = new TreeNode("n0", (char) 0);
        private int nextId = 1;
        private int nodeCount = 1;
        private final List<String> dictionary = new ArrayList<>();

        private TreeNode newNode(char character) {
            nodeCount++;
            return new TreeNode("n" + nextId++, character);
        }

        private void addWord(String word) {
            if (!dictionary.contains(word)) {
                dictionary.add(word);
            }
        }
    }

    private static final class TreeNode {
        private final String id;
        private final char character;
        private final Map<Character, TreeNode> children = new LinkedHashMap<>();
        private boolean terminal;

        private TreeNode(String id, char character) {
            this.id = id;
            this.character = character;
        }
    }

    private record Match(int start, int end, String word) {
    }

    private static final class Trace {
        private final Model model;
        private final Map<String, SnapshotStatus> statuses = new LinkedHashMap<>();
        private final Set<String> activeNodes = new LinkedHashSet<>();
        private final List<SimulationStep> steps = new ArrayList<>();
        private final List<Character> path = new ArrayList<>();
        private final List<Match> matches = new ArrayList<>();
        private String operation = "initialize";
        private String query = "-";
        private String result = "none";
        private String rewrite = "none";
        private int currentStart = -1;
        private String currentNode = "none";
        private int currentIndex = -1;
        private String currentCharacter = "none";
        private String matchedPrefix = "";

        private Trace(Model model) {
            this.model = Objects.requireNonNull(model, "model");
        }

        private void beginOperation() {
            for (String id : activeNodes) {
                statuses.put(id, SnapshotStatus.DEFAULT);
            }
            activeNodes.clear();
            result = "pending";
            rewrite = "none";
            currentStart = -1;
            currentNode = "none";
            currentIndex = -1;
            currentCharacter = "none";
            matchedPrefix = "";
        }

        private void finishPhase() {
            for (String id : List.copyOf(activeNodes)) {
                statuses.put(id, SnapshotStatus.DONE);
            }
            activeNodes.clear();
        }

        private void activate(String id) {
            currentNode = id;
            statuses.putIfAbsent(id, SnapshotStatus.DEFAULT);
            statuses.put(id, SnapshotStatus.ACTIVE);
            activeNodes.add(id);
        }

        private void finishOperation() {
            finishPhase();
        }

        private void markAllDone() {
            for (String id : statuses.keySet()) {
                statuses.put(id, SnapshotStatus.DONE);
            }
            activeNodes.clear();
        }

        private void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("Trie trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            List<Node> nodes = new ArrayList<>();
            List<Edge> edges = new ArrayList<>();
            collect(model.root, null, nodes, edges);
            Set<String> activeEdges = new LinkedHashSet<>();
            for (Edge edge : edges) {
                if (edge.status() == SnapshotStatus.ACTIVE) {
                    activeEdges.add(edge.id());
                }
            }
            TreeState state = new TreeState(nodes, edges, model.root.id, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.copyOf(activeNodes), Set.copyOf(activeEdges)),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private void collect(TreeNode node, TreeNode parent, List<Node> nodes, List<Edge> edges) {
            if (node == null) {
                return;
            }
            SnapshotStatus status = statuses.getOrDefault(node.id, SnapshotStatus.DEFAULT);
            String label = node.character == 0
                    ? "root"
                    : Character.toString(node.character) + (node.terminal ? "*" : "");
            nodes.add(new Node(node.id, label, status));
            if (parent != null) {
                edges.add(new Edge(
                        "edge-" + parent.id + "-" + node.id,
                        parent.id,
                        node.id,
                        status,
                        Character.toString(node.character)));
            }
            for (TreeNode child : node.children.values()) {
                collect(child, node, nodes, edges);
            }
        }

        private List<SimulationStep> steps() {
            return List.copyOf(steps);
        }
    }

    private record Request(List<String> words, String operation, String query) {
        private Request {
            words = List.copyOf(words);
        }

        private static Request parse(JsonNode input) {
            ObjectNode object = OptionalSmallHelper.requireObject(input, TYPE);
            OptionalSmallHelper.requireExactFields(object, Set.of("words", "operation", "word", "text"), TYPE + " input");
            List<String> words = OptionalSmallHelper.readLowercaseList(
                    object.get("words"), "words", TYPE, MIN_WORDS, MAX_WORDS, MAX_WORD_LENGTH,
                    MAX_WORDS * MAX_WORD_LENGTH);
            String operation = OptionalSmallHelper.readText(object.get("operation"), "operation", TYPE);
            if (operation.equals("has")) {
                operation = "search";
            }
            if (!Set.of("insert", "search", "prefix", "match").contains(operation)) {
                throw new IllegalArgumentException(TYPE + " operation must be insert, search, prefix, or match");
            }
            boolean hasWord = object.has("word");
            boolean hasText = object.has("text");
            if (operation.equals("match")) {
                if (!hasText || hasWord) {
                    throw new IllegalArgumentException(TYPE + " match requires exactly text");
                }
                String text = OptionalSmallHelper.readLowercase(
                        object.get("text"), "text", TYPE, 0, MAX_TEXT_LENGTH);
                checkNodeBound(words, null);
                return new Request(words, operation, text);
            }
            if (!hasWord || hasText) {
                throw new IllegalArgumentException(TYPE + " " + operation + " requires exactly word");
            }
            String word = OptionalSmallHelper.readLowercase(
                    object.get("word"), "word", TYPE, 1, MAX_WORD_LENGTH);
            checkNodeBound(words, word);
            return new Request(words, operation, word);
        }

        private String operationDescription() {
            return operation + "(" + query + ")";
        }

        private static void checkNodeBound(List<String> words, String extraWord) {
            Set<String> prefixes = new LinkedHashSet<>();
            for (String word : words) {
                addPrefixes(prefixes, word);
            }
            if (extraWord != null) {
                addPrefixes(prefixes, extraWord);
            }
            if (prefixes.size() + 1 > MAX_TRIE_NODES) {
                throw new IllegalArgumentException(TYPE + " trie node count must be at most " + MAX_TRIE_NODES);
            }
        }

        private static void addPrefixes(Set<String> prefixes, String word) {
            for (int length = 1; length <= word.length(); length++) {
                prefixes.add(word.substring(0, length));
            }
        }
    }
}
