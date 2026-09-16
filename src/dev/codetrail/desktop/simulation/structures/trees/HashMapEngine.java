package dev.codetrail.desktop.simulation.structures.trees;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Bounded integer hash map traced with explicit linear-probing collisions. */
public final class HashMapEngine implements SimulationEngine {
    public static final String TYPE = "HASH_MAP";
    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 15;
    public static final int MAX_ENTRIES = 15;
    public static final int MAX_ABS_VALUE = 999;
    public static final int MAX_TRACE_STEPS = 2048;

    private static final int LINE_HASH = 1;
    private static final int LINE_PROBE_LOOP = 2;
    private static final int LINE_PROBE_SLOT = 3;
    private static final int LINE_FOUND = 4;
    private static final int LINE_TOMBSTONE = 5;
    private static final int LINE_EMPTY = 6;
    private static final int LINE_PUT = 7;
    private static final int LINE_GET = 8;
    private static final int LINE_REMOVE = 9;
    private static final int LINE_FULL = 10;
    private static final int LINE_UPDATE = 11;
    private static final int LINE_RETURN = 12;

    private static final List<String> PSEUDOCODE = List.of(
            "start = floorMod(key, capacity)",
            "for step = 0 .. capacity - 1:",
            "    slot = (start + step) mod capacity",
            "    if slot holds key: use that slot",
            "    remember the first TOMBSTONE",
            "    if slot is EMPTY: stop probing",
            "put: write key and value into the chosen slot",
            "get: return the value or NOT_FOUND",
            "remove: mark the matching slot TOMBSTONE",
            "if every slot is occupied: return FULL",
            "update an existing key in place",
            "return the table and operation result");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        Model model = new Model(request.capacity());
        Trace trace = new Trace(model);
        trace.add(
                0,
                "Initialize an empty table with capacity " + request.capacity(),
                StepEventType.INITIALIZE,
                facts(model, "initialize", "-", "none", "initialize", List.of(), 0, SnapshotStatus.ACTIVE));

        for (Entry entry : request.entries()) {
            trace.beginOperation();
            executePut(model, trace, entry.key(), entry.value(), "build", false);
            trace.finishOperation();
        }

        trace.beginOperation();
        switch (request.operation()) {
            case "put" -> executePut(model, trace, request.key(), request.value(), "put", true);
            case "get" -> executeGet(model, trace, request.key());
            case "remove" -> executeRemove(model, trace, request.key());
            default -> throw new IllegalStateException("unsupported validated operation: " + request.operation());
        }
        trace.finishOperation();

        trace.markAllDone();
        trace.add(
                LINE_RETURN,
                "Return the table with " + model.size + " live entr" + (model.size == 1 ? "y" : "ies"),
                StepEventType.EXECUTE_LINE,
                facts(model, request.operation(), request.operationKeyText(), trace.result,
                        "return", trace.probePath, trace.probeStart, SnapshotStatus.DONE));
        trace.add(
                0,
                "Complete: hash map operation result is " + trace.result,
                StepEventType.COMPLETE,
                facts(model, "complete", request.operationKeyText(), trace.result,
                        "complete", trace.probePath, trace.probeStart, SnapshotStatus.DONE));
        return trace.steps();
    }

    private static void executePut(
            Model model,
            Trace trace,
            int key,
            int value,
            String phase,
            boolean requested) {
        Probe probe = model.probe(key);
        trace.probeStart = probe.start();
        trace.probePath = new ArrayList<>(probe.path());
        String operation = requested ? "put(" + key + "," + value + ")" : "build(" + key + "," + value + ")";
        trace.activate(probe.start());
        trace.add(
                LINE_HASH,
                "Hash key " + key + " to start slot " + probe.start() + " using floorMod",
                StepEventType.EXECUTE_LINE,
                facts(model, operation, Integer.toString(key), "pending", phase,
                        probe.path(), probe.start(), SnapshotStatus.ACTIVE));

        for (int step = 0; step < probe.path().size(); step++) {
            int slot = probe.path().get(step);
            trace.activate(slot);
            trace.add(
                    LINE_PROBE_SLOT,
                    "Probe slot " + slot + " (step " + step + ")",
                    StepEventType.EXECUTE_LINE,
                    facts(model, operation, Integer.toString(key), "pending", phase + "; probing",
                            probe.path(), probe.start(), SnapshotStatus.ACTIVE));
            if (model.state[slot] == OCCUPIED && model.keys[slot] != key) {
                trace.add(
                        LINE_PROBE_LOOP,
                        "Collision at slot " + slot + ": key " + model.keys[slot]
                                + " is different, continue linearly",
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), "pending", phase + "; collision",
                                probe.path(), probe.start(), SnapshotStatus.ACTIVE));
                continue;
            }
            if (model.state[slot] == OCCUPIED) {
                int oldValue = model.values[slot];
                model.values[slot] = value;
                trace.result = "UPDATED";
                trace.rewrite = "slot[" + slot + "].value: " + oldValue + " -> " + value;
                model.rewriteText = trace.rewrite;
                trace.add(
                        LINE_UPDATE,
                        "Update existing key " + key + " in slot " + slot,
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), trace.result, phase + "; update",
                                probe.path(), probe.start(), SnapshotStatus.DONE));
                return;
            }
            int target = probe.insertionSlot();
            if (target < 0) {
                trace.result = "FULL";
                trace.rewrite = "none";
                trace.add(
                        LINE_FULL,
                        "All " + model.capacity + " slots are occupied; return FULL",
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), trace.result, phase + "; full",
                                probe.path(), probe.start(), SnapshotStatus.REJECTED));
                return;
            }
            String oldState = stateName(model.state[target]);
            model.state[target] = OCCUPIED;
            model.keys[target] = key;
            model.values[target] = value;
            model.size++;
            trace.activate(target);
            trace.result = "INSERTED";
            trace.rewrite = "slot[" + target + "]: " + oldState + " -> " + key + "=" + value;
            model.rewriteText = trace.rewrite;
            trace.add(
                    LINE_PUT,
                    "Write key " + key + " and value " + value + " into slot " + target,
                    StepEventType.EXECUTE_LINE,
                    facts(model, operation, Integer.toString(key), trace.result, phase + "; insert",
                            probe.path(), probe.start(), SnapshotStatus.ACTIVE));
            return;
        }
        if (probe.insertionSlot() < 0) {
            trace.result = "FULL";
            trace.rewrite = "none";
            trace.add(
                    LINE_FULL,
                    "The complete probe cycle found no reusable slot; return FULL",
                    StepEventType.EXECUTE_LINE,
                    facts(model, operation, Integer.toString(key), trace.result, phase + "; full",
                            probe.path(), probe.start(), SnapshotStatus.REJECTED));
        }
    }

    private static void executeGet(Model model, Trace trace, int key) {
        Probe probe = model.probe(key);
        trace.probeStart = probe.start();
        trace.probePath = new ArrayList<>(probe.path());
        String operation = "get(" + key + ")";
        trace.activate(probe.start());
        trace.add(
                LINE_HASH,
                "Hash key " + key + " to start slot " + probe.start() + " using floorMod",
                StepEventType.EXECUTE_LINE,
                facts(model, operation, Integer.toString(key), "pending", "get", probe.path(), probe.start(),
                        SnapshotStatus.ACTIVE));
        for (int step = 0; step < probe.path().size(); step++) {
            int slot = probe.path().get(step);
            trace.activate(slot);
            trace.add(
                    LINE_PROBE_SLOT,
                    "Inspect slot " + slot + " while searching for key " + key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, operation, Integer.toString(key), "pending", "get; probing",
                            probe.path(), probe.start(), SnapshotStatus.ACTIVE));
            if (model.state[slot] == OCCUPIED && model.keys[slot] == key) {
                trace.result = Integer.toString(model.values[slot]);
                trace.add(
                        LINE_GET,
                        "Get found key " + key + " in slot " + slot + " with value " + trace.result,
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), trace.result, "get; found",
                                probe.path(), probe.start(), SnapshotStatus.DONE));
                return;
            }
            if (model.state[slot] == OCCUPIED) {
                trace.add(
                        LINE_PROBE_LOOP,
                        "Slot " + slot + " holds another key; continue probing",
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), "pending", "get; collision",
                                probe.path(), probe.start(), SnapshotStatus.ACTIVE));
            } else if (model.state[slot] == TOMBSTONE) {
                trace.add(
                        LINE_TOMBSTONE,
                        "Pass through TOMBSTONE at slot " + slot,
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), "pending", "get; tombstone",
                                probe.path(), probe.start(), SnapshotStatus.ACTIVE));
            } else {
                trace.result = "NOT_FOUND";
                trace.add(
                        LINE_GET,
                        "Reach EMPTY slot " + slot + "; key " + key + " is NOT_FOUND",
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), trace.result, "get; miss",
                                probe.path(), probe.start(), SnapshotStatus.REJECTED));
                return;
            }
        }
        trace.result = "NOT_FOUND";
        trace.add(
                LINE_GET,
                "Complete the probe cycle; key " + key + " is NOT_FOUND",
                StepEventType.EXECUTE_LINE,
                facts(model, operation, Integer.toString(key), trace.result, "get; miss",
                        probe.path(), probe.start(), SnapshotStatus.REJECTED));
    }

    private static void executeRemove(Model model, Trace trace, int key) {
        Probe probe = model.probe(key);
        trace.probeStart = probe.start();
        trace.probePath = new ArrayList<>(probe.path());
        String operation = "remove(" + key + ")";
        trace.activate(probe.start());
        trace.add(
                LINE_HASH,
                "Hash key " + key + " to start slot " + probe.start() + " using floorMod",
                StepEventType.EXECUTE_LINE,
                facts(model, operation, Integer.toString(key), "pending", "remove", probe.path(), probe.start(),
                        SnapshotStatus.ACTIVE));
        for (int slot : probe.path()) {
            trace.activate(slot);
            trace.add(
                    LINE_PROBE_SLOT,
                    "Inspect slot " + slot + " while removing key " + key,
                    StepEventType.EXECUTE_LINE,
                    facts(model, operation, Integer.toString(key), "pending", "remove; probing",
                            probe.path(), probe.start(), SnapshotStatus.ACTIVE));
            if (model.state[slot] == OCCUPIED && model.keys[slot] == key) {
                model.state[slot] = TOMBSTONE;
                model.size--;
                trace.result = "REMOVED";
                trace.rewrite = "slot[" + slot + "]: OCCUPIED -> TOMBSTONE";
                model.rewriteText = trace.rewrite;
                trace.add(
                        LINE_REMOVE,
                        "Mark slot " + slot + " TOMBSTONE so the probe chain remains searchable",
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), trace.result, "remove; tombstone",
                                probe.path(), probe.start(), SnapshotStatus.DONE));
                return;
            }
            if (model.state[slot] == EMPTY) {
                trace.result = "NOT_FOUND";
                trace.add(
                        LINE_REMOVE,
                        "Reach EMPTY slot " + slot + "; key " + key + " is NOT_FOUND",
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), trace.result, "remove; miss",
                                probe.path(), probe.start(), SnapshotStatus.REJECTED));
                return;
            }
            if (model.state[slot] == TOMBSTONE) {
                trace.add(
                        LINE_TOMBSTONE,
                        "Pass through TOMBSTONE at slot " + slot,
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), "pending", "remove; tombstone",
                                probe.path(), probe.start(), SnapshotStatus.ACTIVE));
            } else {
                trace.add(
                        LINE_PROBE_LOOP,
                        "Slot " + slot + " holds another key; continue probing",
                        StepEventType.EXECUTE_LINE,
                        facts(model, operation, Integer.toString(key), "pending", "remove; collision",
                                probe.path(), probe.start(), SnapshotStatus.ACTIVE));
            }
        }
        trace.result = "NOT_FOUND";
        trace.add(
                LINE_REMOVE,
                "Complete the probe cycle; key " + key + " is NOT_FOUND",
                StepEventType.EXECUTE_LINE,
                facts(model, operation, Integer.toString(key), trace.result, "remove; miss",
                        probe.path(), probe.start(), SnapshotStatus.REJECTED));
    }

    private static List<Fact> facts(
            Model model,
            String operation,
            String key,
            String result,
            String phase,
            List<Integer> path,
            int start,
            SnapshotStatus status) {
        String table = model.formatSlots();
        String entries = model.formatEntries();
        String pathText = path.toString();
        int offset = model.currentSlot < 0 ? -1 : Math.floorMod(model.currentSlot - start, model.capacity);
        String equation = model.currentSlot < 0 ? "none"
                : "start = floorMod(" + key + ", " + model.capacity + ") = " + start
                        + "; slot = (" + start + " + " + offset + ") mod " + model.capacity + " = " + model.currentSlot;
        String detail = phase.contains("tombstone")
                ? "A deleted slot keeps the search chain open; continue probing."
                : phase.contains("collision") ? "This slot holds a different key; advance to the next slot and wrap at capacity."
                : phase.contains("insert") ? "Write into the first reusable slot after checking for an existing key."
                : "Linear probing checks consecutive slots; an unused slot ends an unsuccessful search.";
        return List.of(
                new Fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                new Fact("teaching-equation", equation, status),
                new Fact("teaching-detail", detail, status),
                new Fact("probe-slot", model.currentSlot < 0 ? "none" : Integer.toString(model.currentSlot), status),
                new Fact("probe-offset", offset < 0 ? "none" : Integer.toString(offset), status),
                new Fact("operation", operation, SnapshotStatus.DEFAULT),
                new Fact("key", key, SnapshotStatus.DEFAULT),
                new Fact("capacity", Integer.toString(model.capacity), status),
                new Fact("size", Integer.toString(model.size), status),
                new Fact("table", table, status),
                new Fact("slots", table, status),
                new Fact("entries", entries, status),
                new Fact("collision", "linear-probing", SnapshotStatus.DEFAULT),
                new Fact("probe-start", Integer.toString(start), status),
                new Fact("probe-path", pathText, status),
                new Fact("result", result, status),
                new Fact("outcome", result, status),
                new Fact("rewrite", model.rewriteText, status),
                new Fact("phase", phase, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("capacity", 7);
        ArrayNode entries = defaultInput.putArray("entries");
        addEntry(entries, 12, 12);
        addEntry(entries, 19, 19);
        addEntry(entries, 26, 26);
        addEntry(entries, 5, 5);
        defaultInput.put("operation", "put");
        defaultInput.put("key", 33);
        defaultInput.put("value", 90);
        defaultInput.put("collision", "linear-probing");
        return new SimulationMetadata(
                TYPE,
                "Hash Map",
                "O(1) average, O(n) worst per operation",
                "O(capacity)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"capacity\":7,\"entries\":[[12,12],[19,19]],\"operation\":\"put\",\"key\":33,\"value\":90,\"collision\":\"linear-probing\"}; capacity and entries are bounded, and operation is put, get, or remove.",
                PSEUDOCODE);
    }

    private static void addEntry(ArrayNode entries, int key, int value) {
        ArrayNode pair = entries.addArray();
        pair.add(key);
        pair.add(value);
    }

    private static final int EMPTY = 0;
    private static final int OCCUPIED = 1;
    private static final int TOMBSTONE = 2;

    private static String stateName(int state) {
        return switch (state) {
            case EMPTY -> "EMPTY";
            case OCCUPIED -> "OCCUPIED";
            case TOMBSTONE -> "TOMBSTONE";
            default -> throw new IllegalStateException("unknown table state: " + state);
        };
    }

    private static final class Model {
        private final int capacity;
        private final int[] state;
        private final int[] keys;
        private final int[] values;
        private int size;
        private int currentSlot = -1;
        private String rewriteText = "none";

        private Model(int capacity) {
            this.capacity = capacity;
            state = new int[capacity];
            keys = new int[capacity];
            values = new int[capacity];
        }

        private Probe probe(int key) {
            int start = Math.floorMod(key, capacity);
            List<Integer> path = new ArrayList<>();
            int firstTombstone = -1;
            int found = -1;
            for (int step = 0; step < capacity; step++) {
                int slot = (start + step) % capacity;
                path.add(slot);
                if (state[slot] == OCCUPIED && keys[slot] == key) {
                    found = slot;
                    break;
                }
                if (state[slot] == TOMBSTONE && firstTombstone < 0) {
                    firstTombstone = slot;
                }
                if (state[slot] == EMPTY) {
                    break;
                }
            }
            int insertion = found >= 0 ? found : firstEmpty(path, firstTombstone);
            return new Probe(start, List.copyOf(path), found, insertion);
        }

        private int firstEmpty(List<Integer> path, int firstTombstone) {
            if (firstTombstone >= 0) {
                return firstTombstone;
            }
            for (int slot : path) {
                if (state[slot] == EMPTY) {
                    return slot;
                }
            }
            return -1;
        }

        private String formatSlots() {
            StringBuilder formatted = new StringBuilder("[");
            for (int slot = 0; slot < capacity; slot++) {
                if (slot > 0) {
                    formatted.append(", ");
                }
                formatted.append(slot).append(':');
                if (state[slot] == OCCUPIED) {
                    formatted.append(keys[slot]).append('=').append(values[slot]);
                } else {
                    formatted.append(stateName(state[slot]));
                }
            }
            return formatted.append(']').toString();
        }

        private String formatEntries() {
            StringBuilder formatted = new StringBuilder("{");
            boolean first = true;
            for (int slot = 0; slot < capacity; slot++) {
                if (state[slot] != OCCUPIED) {
                    continue;
                }
                if (!first) {
                    formatted.append(", ");
                }
                formatted.append(keys[slot]).append('=').append(values[slot]);
                first = false;
            }
            return formatted.append('}').toString();
        }
    }

    private record Probe(int start, List<Integer> path, int foundSlot, int insertionSlot) {
    }

    private static final class Trace {
        private final Model model;
        private final SnapshotStatus[] statuses;
        private final Set<Integer> activeSlots = new LinkedHashSet<>();
        private final List<SimulationStep> steps = new ArrayList<>();
        private String result = "none";
        private String rewrite = "none";
        private List<Integer> probePath = List.of();
        private int probeStart = 0;

        private Trace(Model model) {
            this.model = Objects.requireNonNull(model, "model");
            statuses = new SnapshotStatus[model.capacity];
            Arrays.fill(statuses, SnapshotStatus.DEFAULT);
        }

        private void beginOperation() {
            for (int slot : activeSlots) {
                statuses[slot] = SnapshotStatus.DEFAULT;
            }
            activeSlots.clear();
            model.rewriteText = "none";
            model.currentSlot = -1;
            rewrite = "none";
            result = "none";
        }

        private void activate(int slot) {
            if (slot < 0 || slot >= model.capacity) {
                throw new IllegalArgumentException("hash map trace slot is outside capacity: " + slot);
            }
            model.currentSlot = slot;
            statuses[slot] = SnapshotStatus.ACTIVE;
            activeSlots.add(slot);
        }

        private void finishOperation() {
            model.rewriteText = rewrite;
            for (int slot : List.copyOf(activeSlots)) {
                statuses[slot] = SnapshotStatus.DONE;
            }
            activeSlots.clear();
        }

        private void markAllDone() {
            Arrays.fill(statuses, SnapshotStatus.DONE);
            activeSlots.clear();
        }

        private void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("hash map trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0 || highlightedLine > PSEUDOCODE.size()) {
                throw new IllegalArgumentException("hash map highlighted line is outside pseudocode");
            }
            if (narration == null || narration.isBlank()) {
                throw new IllegalArgumentException("hash map narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(facts, "facts");
            List<List<TypedCell>> rows = new ArrayList<>(model.capacity);
            for (int slot = 0; slot < model.capacity; slot++) {
                SnapshotStatus status = statuses[slot];
                String key = model.state[slot] == OCCUPIED ? Integer.toString(model.keys[slot]) : "-";
                String value = model.state[slot] == OCCUPIED ? Integer.toString(model.values[slot]) : "-";
                rows.add(List.of(
                        OptionalSmallHelper.cell("slot", Integer.toString(slot), status),
                        OptionalSmallHelper.cell("key", key, status),
                        OptionalSmallHelper.cell("value", value, status),
                        OptionalSmallHelper.cell("state", stateName(model.state[slot]), status)));
            }
            TableState state = new TableState(List.of("slot", "key", "value", "state"), rows, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.of(), Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private List<SimulationStep> steps() {
            return List.copyOf(steps);
        }
    }

    private record Entry(int key, int value) {
    }

    private record Request(
            int capacity,
            List<Entry> entries,
            String operation,
            int key,
            int value) {
        private Request {
            entries = List.copyOf(entries);
        }

        private static Request parse(JsonNode input) {
            ObjectNode object = OptionalSmallHelper.requireObject(input, TYPE);
            OptionalSmallHelper.requireExactFields(
                    object,
                    Set.of("capacity", "entries", "operation", "key", "value", "collision"),
                    TYPE + " input");
            int capacity = OptionalSmallHelper.readInt(object.get("capacity"), "capacity", TYPE);
            if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
                throw new IllegalArgumentException(TYPE + " capacity must be in the inclusive range "
                        + MIN_CAPACITY + ".." + MAX_CAPACITY);
            }
            JsonNode collisionNode = object.get("collision");
            if (collisionNode == null || !collisionNode.isTextual()
                    || !collisionNode.textValue().equals("linear-probing")) {
                throw new IllegalArgumentException(TYPE + " collision must be exactly linear-probing");
            }
            JsonNode entriesNode = object.get("entries");
            if (entriesNode == null || !entriesNode.isArray()) {
                throw new IllegalArgumentException(TYPE + " entries must be a JSON array");
            }
            if (entriesNode.size() > MAX_ENTRIES || entriesNode.size() > capacity) {
                throw new IllegalArgumentException(TYPE + " entries must fit within capacity and contain at most "
                        + MAX_ENTRIES + " pairs");
            }
            List<Entry> entries = new ArrayList<>(entriesNode.size());
            Set<Integer> seenKeys = new HashSet<>();
            for (int index = 0; index < entriesNode.size(); index++) {
                JsonNode pair = entriesNode.get(index);
                if (pair == null || !pair.isArray() || pair.size() != 2) {
                    throw new IllegalArgumentException(TYPE + " entries[" + index + "] must be [key,value]");
                }
                int key = OptionalSmallHelper.readInt(pair.get(0), "entries[" + index + "][0]", TYPE);
                int value = OptionalSmallHelper.readInt(pair.get(1), "entries[" + index + "][1]", TYPE);
                checkAbs(key, "entries[" + index + "][0]");
                checkAbs(value, "entries[" + index + "][1]");
                if (!seenKeys.add(key)) {
                    throw new IllegalArgumentException(TYPE + " entries cannot repeat key " + key);
                }
                entries.add(new Entry(key, value));
            }
            String operation = OptionalSmallHelper.readText(object.get("operation"), "operation", TYPE);
            int key = 0;
            int value = 0;
            if (operation.equals("put")) {
                requireFields(object, Set.of("capacity", "entries", "operation", "key", "value", "collision"));
                key = readOperationInt(object, "key");
                value = readOperationInt(object, "value");
                checkAbs(key, "key");
                checkAbs(value, "value");
            } else if (operation.equals("get") || operation.equals("remove")) {
                requireFields(object, Set.of("capacity", "entries", "operation", "key", "collision"));
                key = readOperationInt(object, "key");
                checkAbs(key, "key");
            } else {
                throw new IllegalArgumentException(TYPE + " operation must be put, get, or remove");
            }
            return new Request(capacity, entries, operation, key, value);
        }

        private String operationKeyText() {
            return Integer.toString(key);
        }

        private static int readOperationInt(ObjectNode object, String field) {
            return OptionalSmallHelper.readInt(object.get(field), field, TYPE);
        }

        private static void requireFields(ObjectNode object, Set<String> expected) {
            Set<String> actual = new HashSet<>();
            object.fieldNames().forEachRemaining(name -> actual.add(name));
            if (!actual.equals(expected)) {
                throw new IllegalArgumentException(TYPE + " operation fields must be exactly " + expected);
            }
        }

        private static void checkAbs(int value, String field) {
            if (Math.abs((long) value) > MAX_ABS_VALUE) {
                throw new IllegalArgumentException(TYPE + " " + field
                        + " must have absolute value at most " + MAX_ABS_VALUE);
            }
        }
    }
}
