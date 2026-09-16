package dev.codetrail.desktop.simulation.strings.remaining;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Suffix automaton extension with real suffix links, transitions, and clones. */
public final class SuffixAutomatonEngine implements SimulationEngine {
    public static final String TYPE = "SUFFIX_AUTOMATON";
    public static final int MIN_TEXT_LENGTH = 0;
    public static final int MAX_TEXT_LENGTH = RemainingStringSupport.MAX_SUFFIX_TEXT_LENGTH;
    public static final int MAX_STATES = Math.max(1, 2 * MAX_TEXT_LENGTH);
    public static final int MAX_TRACE_STEPS = RemainingStringSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_ROOT = 2;
    private static final int LINE_EXTEND = 3;
    private static final int LINE_NEW_STATE = 4;
    private static final int LINE_WALK = 7;
    private static final int LINE_ADD_TRANSITION = 7;
    private static final int LINE_NO_PARENT = 8;
    private static final int LINE_EXISTING = 9;
    private static final int LINE_DIRECT_LINK = 10;
    private static final int LINE_CLONE = 11;
    private static final int LINE_REDIRECT = 12;
    private static final int LINE_CLONE_LINKS = 13;
    private static final int LINE_LAST = 14;
    private static final int LINE_RETURN = 15;

    private static final List<String> PSEUDOCODE = List.of(
            "buildSuffixAutomaton(text):",
            "    create root state 0 with length 0 and link -1",
            "    for each character c in text: extend(c)",
            "        cur = new state with length len[last] + 1",
            "        p = last",
            "        while p != -1 and transition[p][c] is absent:",
            "            transition[p][c] = cur; p = link[p]",
            "        if p == -1: link[cur] = 0",
            "        else q = transition[p][c]",
            "            if len[p] + 1 == len[q]: link[cur] = q",
            "            else clone q with length len[p] + 1",
            "                redirect matching transitions to clone",
            "                link[q] = link[cur] = clone",
            "        last = cur",
            "return states, transitions, and suffix links");

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
                "Initialize suffix automaton construction for text "
                        + RemainingStringSupport.boundedText(text),
                StepEventType.INITIALIZE);
        add(
                steps,
                model,
                LINE_METHOD,
                "Extend one state machine character at a time",
                StepEventType.EXECUTE_LINE);
        model.phase = "root";
        add(
                steps,
                model,
                LINE_ROOT,
                "Create root q0 with length 0 and suffix link -1",
                StepEventType.EXECUTE_LINE);

        if (text.isEmpty()) {
            model.phase = "return";
            model.operation = "empty text leaves only q0";
            add(
                    steps,
                    model,
                    LINE_RETURN,
                    "Return the root-only suffix automaton",
                    StepEventType.EXECUTE_LINE);
            model.phase = "complete";
            add(
                    steps,
                    model,
                    0,
                    "Complete: root-only suffix automaton",
                    StepEventType.COMPLETE);
            return List.copyOf(steps);
        }

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            model.phase = "extend";
            model.extensionIndex = index;
            model.currentCharacter = character;
            model.currentState = -1;
            model.parentState = model.last;
            model.existingState = -1;
            model.cloneState = -1;
            model.activeFrom = -1;
            model.activeTo = -1;
            model.activeCharacter = '\0';
            model.operation = "extend(" + character + ")";
            model.lengthCondition = "";
            model.cloneSource = -1;
            model.cloneLinks = "";
            model.linkBoth = false;
            add(
                    steps,
                    model,
                    LINE_EXTEND,
                    "Extend the automaton with text[" + index + "] = '" + character + "'",
                    StepEventType.EXECUTE_LINE);

            int current = model.addState(new State(model.states.size(), model.states.get(model.last).length + 1));
            model.currentState = current;
            model.operation = "create q" + current + " with length " + model.states.get(current).length;
            add(
                    steps,
                    model,
                    LINE_NEW_STATE,
                    "Create current state q" + current + " with length " + model.states.get(current).length,
                    StepEventType.EXECUTE_LINE);

            int parent = model.last;
            model.parentState = parent;
            while (parent != -1 && !model.states.get(parent).transitions.containsKey(character)) {
                model.phase = "walk";
                model.activeFrom = parent;
                model.activeTo = current;
                model.activeCharacter = character;
                model.states.get(parent).transitions.put(character, current);
                model.operation = "transition q" + parent + " -" + character + "-> q" + current;
                add(
                        steps,
                        model,
                        LINE_ADD_TRANSITION,
                        "Add transition q" + parent + " -" + character + "-> q" + current,
                        StepEventType.EXECUTE_LINE);
                int previousParent = parent;
                parent = model.states.get(parent).link;
                model.parentState = parent;
                model.activeFrom = previousParent;
                model.activeTo = parent;
                model.activeCharacter = '\0';
                if (parent != -1) {
                    model.operation = "follow suffix link to q" + parent;
                    add(
                            steps,
                            model,
                            LINE_WALK,
                            "Follow the suffix link to q" + parent + " and check for another missing transition",
                            StepEventType.EXECUTE_LINE);
                }
            }

            if (parent == -1) {
                model.states.get(current).link = 0;
                model.parentState = -1;
                model.activeFrom = current;
                model.activeTo = 0;
                model.activeCharacter = '\0';
                model.operation = "link[q" + current + "] = q0";
                add(
                        steps,
                        model,
                        LINE_NO_PARENT,
                        "No suffix-link parent remains; set link[q" + current + "] = q0",
                        StepEventType.EXECUTE_LINE);
            } else {
                int existing = model.states.get(parent).transitions.get(character);
                model.existingState = existing;
                int expectedLength = model.states.get(parent).length + 1;
                int existingLength = model.states.get(existing).length;
                model.lengthCondition = "len[q" + parent + "] + 1 = " + expectedLength
                        + (expectedLength == existingLength ? " = " : " < ")
                        + "len[q" + existing + "] = " + existingLength;
                model.activeFrom = parent;
                model.activeTo = existing;
                model.activeCharacter = character;
                model.operation = "existing transition q" + parent + " -" + character + "-> q" + existing;
                add(
                        steps,
                        model,
                        LINE_EXISTING,
                        "Find existing transition q" + parent + " -" + character + "-> q" + existing,
                        StepEventType.EXECUTE_LINE);
                if (model.states.get(parent).length + 1 == model.states.get(existing).length) {
                    model.states.get(current).link = existing;
                    model.activeFrom = current;
                    model.activeTo = existing;
                    model.activeCharacter = '\0';
                    model.operation = "link[q" + current + "] = q" + existing;
                    add(
                            steps,
                            model,
                            LINE_DIRECT_LINK,
                            "The existing state has the expected length; set link[q" + current + "] = q" + existing,
                            StepEventType.EXECUTE_LINE);
                } else {
                    State source = model.states.get(existing);
                    int clone = model.states.size();
                    State cloneState = new State(clone, model.states.get(parent).length + 1);
                    cloneState.link = source.link;
                    cloneState.transitions.putAll(source.transitions);
                    model.addState(cloneState);
                    model.cloneState = clone;
                    model.cloneSource = existing;
                    model.operation = "clone q" + existing + " as q" + clone
                            + " with length " + cloneState.length;
                    model.cloneCount++;
                    add(
                            steps,
                            model,
                            LINE_CLONE,
                            model.lengthCondition + "; clone q" + existing + " as q" + clone
                                    + " at length " + cloneState.length,
                            StepEventType.EXECUTE_LINE);

                    int redirectParent = parent;
                    while (redirectParent != -1) {
                        Integer redirectedTarget = model.states.get(redirectParent).transitions.get(character);
                        if (redirectedTarget == null || redirectedTarget != existing) {
                            break;
                        }
                        model.states.get(redirectParent).transitions.put(character, clone);
                        model.parentState = redirectParent;
                        model.activeFrom = redirectParent;
                        model.activeTo = clone;
                        model.activeCharacter = character;
                        model.operation = "redirect q" + redirectParent + " -" + character + "-> q" + clone;
                        add(
                                steps,
                                model,
                                LINE_REDIRECT,
                                "Redirect q" + redirectParent + " -" + character + "-> q" + clone,
                                StepEventType.EXECUTE_LINE);
                        redirectParent = model.states.get(redirectParent).link;
                    }
                    model.states.get(existing).link = clone;
                    model.states.get(current).link = clone;
                    model.existingState = existing;
                    model.parentState = redirectParent;
                    model.activeFrom = existing;
                    model.activeTo = clone;
                    model.activeCharacter = '\0';
                    model.cloneLinks = "link[q" + existing + "] = link[q" + current + "] = q" + clone;
                    model.operation = model.cloneLinks;
                    model.linkBoth = true;
                    add(
                            steps,
                            model,
                            LINE_CLONE_LINKS,
                            "Point both q" + existing + " and q" + current + " to clone q" + clone,
                            StepEventType.EXECUTE_LINE);
                }
            }

            model.last = current;
            model.linkBoth = false;
            model.cloneSource = -1;
            model.lengthCondition = "";
            model.cloneLinks = "";
            model.phase = "extend";
            model.parentState = -1;
            model.existingState = -1;
            model.cloneState = -1;
            model.activeFrom = -1;
            model.activeTo = -1;
            model.activeCharacter = '\0';
            model.operation = "last = q" + current;
            add(
                    steps,
                    model,
                    LINE_LAST,
                    "Set last = q" + current + " for the next extension",
                    StepEventType.EXECUTE_LINE);
        }

        model.phase = "return";
        model.currentState = -1;
        model.currentCharacter = '\0';
        model.operation = "all transitions and suffix links are built";
        add(
                steps,
                model,
                LINE_RETURN,
                "Return all suffix-automaton states, character transitions, and suffix links",
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: suffix automaton has " + model.states.size() + " state(s) and " + model.cloneCount + " clone(s)",
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
        List<Node> nodes = new ArrayList<>(model.states.size());
        for (State state : model.states) {
            SnapshotStatus status = model.phase.equals("complete")
                    ? SnapshotStatus.DONE
                    : state.id == model.currentState
                            || state.id == model.parentState
                            || state.id == model.existingState
                            || state.id == model.cloneState
                                    ? SnapshotStatus.ACTIVE
                                    : SnapshotStatus.DONE;
            nodes.add(new Node(
                    RemainingStringSupport.nodeId(state.id),
                    "q" + state.id + " len=" + state.length,
                    status));
        }
        List<Edge> edges = new ArrayList<>();
        Set<String> activeEdgeIds = new java.util.HashSet<>();
        for (State state : model.states) {
            if (state.link >= 0) {
                String edgeId = suffixLinkEdgeId(state.id);
                SnapshotStatus status = model.phase.equals("complete")
                        ? SnapshotStatus.DONE
                        : (state.id == model.activeFrom || model.linkBoth && state.id == model.currentState)
                                && model.activeCharacter == '\0'
                                ? SnapshotStatus.ACTIVE
                                : SnapshotStatus.DONE;
                edges.add(new Edge(
                        edgeId,
                        RemainingStringSupport.nodeId(state.id),
                        RemainingStringSupport.nodeId(state.link),
                        status,
                        "link"));
                if (status == SnapshotStatus.ACTIVE) {
                    activeEdgeIds.add(edgeId);
                }
            }
            for (Map.Entry<Character, Integer> transition : state.transitions.entrySet()) {
                String edgeId = transitionEdgeId(state.id, transition.getKey());
                SnapshotStatus status = model.phase.equals("complete")
                        ? SnapshotStatus.DONE
                        : state.id == model.activeFrom
                                && transition.getKey() == model.activeCharacter
                                && transition.getValue() == model.activeTo
                                        ? SnapshotStatus.ACTIVE
                                        : SnapshotStatus.DONE;
                edges.add(new Edge(
                        edgeId,
                        RemainingStringSupport.nodeId(state.id),
                        RemainingStringSupport.nodeId(transition.getValue()),
                        status,
                        Character.toString(transition.getKey())));
                if (status == SnapshotStatus.ACTIVE) {
                    activeEdgeIds.add(edgeId);
                }
            }
        }
        Set<String> activeNodeIds = new java.util.HashSet<>();
        addActiveNode(activeNodeIds, model.currentState);
        addActiveNode(activeNodeIds, model.parentState);
        addActiveNode(activeNodeIds, model.existingState);
        addActiveNode(activeNodeIds, model.cloneState);
        steps.add(RemainingStringSupport.graphStep(
                nodes,
                edges,
                facts(model),
                activeNodeIds,
                activeEdgeIds,
                line,
                narration,
                eventType));
    }

    private static void addActiveNode(Set<String> activeNodeIds, int stateId) {
        if (stateId >= 0) {
            activeNodeIds.add(RemainingStringSupport.nodeId(stateId));
        }
    }

    private static String transitionEdgeId(int from, char character) {
        return "t-q" + from + "-" + character;
    }

    private static String suffixLinkEdgeId(int from) {
        return "s-q" + from;
    }

    private static List<Fact> facts(Model model) {
        SnapshotStatus status = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        List<Fact> facts = new ArrayList<>(List.of(
                new Fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                new Fact("current-state", model.currentState < 0 ? "none" : "q" + model.currentState, status),
                new Fact("parent-state", model.parentState < 0 ? "none" : "q" + model.parentState, status),
                new Fact("existing-state", model.existingState < 0 ? "none" : "q" + model.existingState, status),
                new Fact("clone-state", model.cloneState < 0 ? "none" : "q" + model.cloneState, status),
                new Fact("clone-source", model.cloneSource < 0 ? "none" : "q" + model.cloneSource, status),
                new Fact("length-condition", model.lengthCondition, status),
                new Fact("clone-links", model.cloneLinks, status),
                new Fact("phase", model.phase, SnapshotStatus.DEFAULT),
                new Fact("text", RemainingStringSupport.boundedText(model.text), SnapshotStatus.DEFAULT),
                new Fact("state-count", Integer.toString(model.states.size()), status),
                new Fact("extension-index", Integer.toString(model.extensionIndex), SnapshotStatus.ACTIVE),
                new Fact("current-character", model.currentCharacter == '\0'
                        ? "-" : Character.toString(model.currentCharacter), SnapshotStatus.ACTIVE),
                new Fact("last", RemainingStringSupport.nodeId(model.last), status),
                new Fact("links", formatLinks(model), status),
                new Fact("suffix-links", formatLinks(model), status),
                new Fact("lengths", formatLengths(model), status),
                new Fact("transitions", formatTransitions(model), status),
                new Fact("clones", Integer.toString(model.cloneCount), status),
                new Fact("operation", model.operation, status)));
        if (model.cloneState >= 0) {
            addCloneWitnesses(facts, model, status);
        }
        return List.copyOf(facts);
    }

    /** A clone groups suffixes with equal end positions in the prefix being extended. */
    private static void addCloneWitnesses(List<Fact> facts, Model model, SnapshotStatus status) {
        String prefix = model.text.substring(0, model.extensionIndex + 1);
        State clone = model.states.get(model.cloneState);
        int shortestLength = model.states.get(clone.link).length + 1;
        String shortest = prefix.substring(prefix.length() - shortestLength);
        String longest = prefix.substring(prefix.length() - clone.length);
        String source = longestStateWitness(model, model.cloneSource, prefix);
        facts.add(new Fact("processed-prefix", prefix, status));
        facts.add(new Fact("clone-shortest", shortest, status));
        facts.add(new Fact("clone-longest", longest, status));
        facts.add(new Fact("clone-end-positions", endPositions(prefix, longest).toString(), status));
        facts.add(new Fact("clone-source-substring", source, status));
        facts.add(new Fact("clone-source-end-positions", endPositions(prefix, source).toString(), status));
    }

    private static String longestStateWitness(Model model, int stateId, String prefix) {
        int length = model.states.get(stateId).length;
        for (int start = 0; start + length <= prefix.length(); start++) {
            int reached = 0;
            for (int offset = 0; offset < length && reached >= 0; offset++) {
                reached = model.states.get(reached).transitions.getOrDefault(prefix.charAt(start + offset), -1);
            }
            if (reached == stateId) {
                return prefix.substring(start, start + length);
            }
        }
        throw new IllegalStateException("Suffix-automaton clone source has no prefix witness");
    }

    private static List<Integer> endPositions(String prefix, String substring) {
        List<Integer> ends = new ArrayList<>();
        for (int start = 0; start + substring.length() <= prefix.length(); start++) {
            if (prefix.startsWith(substring, start)) {
                ends.add(start + substring.length() - 1);
            }
        }
        return List.copyOf(ends);
    }

    private static String formatLinks(Model model) {
        StringBuilder result = new StringBuilder("{");
        for (int index = 0; index < model.states.size(); index++) {
            if (index > 0) {
                result.append(", ");
            }
            int link = model.states.get(index).link;
            result.append(RemainingStringSupport.nodeId(index)).append('=').append(link < 0 ? "-1" : RemainingStringSupport.nodeId(link));
        }
        return result.append('}').toString();
    }

    private static String formatLengths(Model model) {
        StringBuilder result = new StringBuilder("{");
        for (int index = 0; index < model.states.size(); index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(RemainingStringSupport.nodeId(index)).append('=').append(model.states.get(index).length);
        }
        return result.append('}').toString();
    }

    private static String formatTransitions(Model model) {
        StringBuilder result = new StringBuilder("{");
        boolean first = true;
        for (State state : model.states) {
            for (Map.Entry<Character, Integer> transition : state.transitions.entrySet()) {
                if (!first) {
                    result.append(", ");
                }
                first = false;
                result.append(RemainingStringSupport.nodeId(state.id))
                        .append('-').append(transition.getKey()).append("->")
                        .append(RemainingStringSupport.nodeId(transition.getValue()));
            }
        }
        return result.append('}').toString();
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("text", "abcbc");
        return new SimulationMetadata(
                TYPE,
                "Suffix Automaton",
                "O(n)",
                "O(n)",
                RendererFamily.GRAPH,
                defaultInput,
                "Enter exactly {\"text\":\"abcbc\"}; text must be lowercase ASCII and have length "
                        + MIN_TEXT_LENGTH + ".." + MAX_TEXT_LENGTH
                        + ". Empty text leaves root q0. Nodes use short q IDs and length labels; character edges"
                        + " show transitions and edges labelled link show suffix links. Clones are created when an"
                        + " existing transition has a length gap.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final String text;
        private final int maxStates;
        private final List<State> states = new ArrayList<>();
        private int last;
        private int extensionIndex = -1;
        private int currentState = -1;
        private int parentState = -1;
        private int existingState = -1;
        private int cloneState = -1;
        private int activeFrom = -1;
        private int activeTo = -1;
        private char currentCharacter;
        private char activeCharacter;
        private int cloneCount;
        private int cloneSource = -1;
        private boolean linkBoth;
        private String lengthCondition = "";
        private String cloneLinks = "";
        private String phase = "initialize";
        private String operation = "pending";

        private Model(String text) {
            this.text = text;
            this.maxStates = Math.max(1, 2 * text.length());
            this.last = addState(new State(0, 0));
            this.states.get(0).link = -1;
        }

        private int addState(State state) {
            if (states.size() >= maxStates) {
                throw new IllegalStateException(TYPE + " state count exceeded bounded state limit");
            }
            if (state.id != states.size()) {
                throw new IllegalArgumentException("suffix-automaton state IDs must be contiguous");
            }
            states.add(state);
            return state.id;
        }
    }

    private static final class State {
        private final int id;
        private final int length;
        private int link = -1;
        private final Map<Character, Integer> transitions = new TreeMap<>();

        private State(int id, int length) {
            this.id = id;
            this.length = length;
        }
    }
}
