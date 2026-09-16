package dev.codetrail.desktop.simulation.paradigms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Full 0/1 knapsack table trace with visible previous-row dependencies. */
public final class DynamicProgrammingEngine implements SimulationEngine {
    public static final String TYPE = "DYNAMIC_PROGRAMMING";
    public static final String ALGORITHM = "KNAPSACK_01";
    public static final int MAX_ITEMS = 10;
    public static final int MAX_CAPACITY = 20;
    public static final int MAX_ABS_VALUE = 999;
    public static final int MAX_ITEM_WEIGHT = 999;
    public static final int MAX_TRACE_STEPS = 4096;

    private static final int LINE_METHOD = 1;
    private static final int LINE_INITIALIZE = 2;
    private static final int LINE_OUTER = 3;
    private static final int LINE_INNER = 4;
    private static final int LINE_SKIP = 5;
    private static final int LINE_TAKE = 6;
    private static final int LINE_FILL = 7;
    private static final int LINE_BACKTRACK = 8;
    private static final int LINE_RETURN = 9;

    private static final int DEFAULT_CAPACITY = 7;
    private static final List<Item> DEFAULT_ITEMS = List.of(
            new Item(0, 1, 1),
            new Item(1, 3, 4),
            new Item(2, 4, 5),
            new Item(3, 5, 7));
    private static final List<String> PSEUDOCODE = List.of(
            "knapsack01(items, capacity):",
            "    dp[0][c] = 0 for c = 0 .. capacity",
            "    for i = 1 .. n:",
            "        for c = 0 .. capacity:",
            "            skip = dp[i - 1][c]",
            "            take = value[i - 1] + dp[i - 1][c - weight[i - 1]] if weight fits; otherwise -infinity",
            "            dp[i][c] = max(skip, take)",
            "    backtrack from dp[n][capacity] to chosen items",
            "    return chosen items and dp[n][capacity]");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        ParadigmSupport.requireObject(input, TYPE);
        ParadigmSupport.requireAlgorithm(input, TYPE, ALGORITHM);
        int capacity = ParadigmSupport.boundedInt(
                ParadigmSupport.requireField(input, TYPE, "capacity"),
                TYPE + " capacity",
                0,
                MAX_CAPACITY);
        List<Item> items = readItems(input);
        Model model = new Model(items, capacity);
        List<SimulationStep> steps = new ArrayList<>();

        model.phase = "initialize";
        for (int current = 0; current <= capacity; current++) {
            model.dp[0][current] = 0;
            model.computed[0][current] = true;
        }
        add(
                steps,
                model,
                0,
                "Initialize the 0/1 knapsack table for capacity " + capacity,
                StepEventType.INITIALIZE);
        add(steps, model, LINE_METHOD, "Compute the best value for each item prefix and capacity", StepEventType.EXECUTE_LINE);
        add(steps, model, LINE_INITIALIZE, "Fill the empty-item row with zero values", StepEventType.EXECUTE_LINE);

        for (int itemRow = 1; itemRow <= items.size(); itemRow++) {
            Item item = items.get(itemRow - 1);
            model.phase = "row";
            model.activeRow = itemRow;
            model.activeCapacity = -1;
            model.skip = null;
            model.take = null;
            model.dependency = "none";
            model.choice = "pending";
            add(
                    steps,
                    model,
                    LINE_OUTER,
                    "Process item " + item.index + " with weight " + item.weight + " and value " + item.value,
                    StepEventType.EXECUTE_LINE);
            for (int current = 0; current <= capacity; current++) {
                model.activeRow = itemRow;
                model.activeCapacity = current;
                model.skip = null;
                model.take = null;
                model.dependency = "skip=dp[" + (itemRow - 1) + "][" + current + "]";
                model.choice = "pending";
                model.phase = "cell";
                add(
                        steps,
                        model,
                        LINE_INNER,
                        "Evaluate dp[" + itemRow + "][" + current + "]",
                        StepEventType.EXECUTE_LINE);

                int skip = model.dp[itemRow - 1][current];
                model.skip = skip;
                model.phase = "skip";
                add(
                        steps,
                        model,
                        LINE_SKIP,
                        "Dependency skip = dp[" + (itemRow - 1) + "][" + current + "] = " + skip,
                        StepEventType.EXECUTE_LINE);

                int take = Integer.MIN_VALUE;
                model.phase = "take";
                if (item.weight <= current) {
                    take = item.value + model.dp[itemRow - 1][current - item.weight];
                    model.dependency = "skip=dp[" + (itemRow - 1) + "][" + current + "]; take=dp["
                            + (itemRow - 1) + "][" + (current - item.weight) + "]";
                    model.take = take;
                    add(
                            steps,
                            model,
                            LINE_TAKE,
                            "Dependency take = value " + item.value + " + dp[" + (itemRow - 1) + "]["
                                    + (current - item.weight) + "] = " + item.value + " + "
                                    + model.dp[itemRow - 1][current - item.weight] + " = " + take,
                            StepEventType.EXECUTE_LINE);
                } else {
                    model.take = null;
                    model.dependency = "skip=dp[" + (itemRow - 1) + "][" + current + "]; take=not-fit";
                    add(
                            steps,
                            model,
                            LINE_TAKE,
                            "Weight " + item.weight + " > capacity " + current + ": taking this item is unavailable",
                            StepEventType.EXECUTE_LINE);
                }
                model.dp[itemRow][current] = Math.max(skip, take);
                model.choice = take > skip ? "take" : "skip";
                model.computed[itemRow][current] = true;
                model.phase = "fill";
                add(
                        steps,
                        model,
                        LINE_FILL,
                        "dp[" + itemRow + "][" + current + "] = max(" + skip + ", "
                                + (model.take == null ? "unavailable" : take) + ") = "
                                + model.dp[itemRow][current] + ": " + model.choice,
                        StepEventType.EXECUTE_LINE);
            }
        }

        model.activeRow = -1;
        model.activeCapacity = -1;
        model.phase = "backtrack";
        model.skip = null;
        model.take = null;
        model.dependency = "none";
        model.choice = "pending";
        int currentCapacity = capacity;
        for (int itemRow = items.size(); itemRow >= 1; itemRow--) {
            Item item = items.get(itemRow - 1);
            model.activeRow = itemRow;
            model.activeCapacity = currentCapacity;
            boolean take = item.weight <= currentCapacity
                    && model.dp[itemRow][currentCapacity] != model.dp[itemRow - 1][currentCapacity]
                    && model.dp[itemRow][currentCapacity]
                    == item.value + model.dp[itemRow - 1][currentCapacity - item.weight];
            if (take) {
                model.chosen[item.index] = true;
                model.selectedWeight += item.weight;
                model.selectedValue += item.value;
                model.backtrackChoice = "take item " + item.index;
                currentCapacity -= item.weight;
            } else {
                model.backtrackChoice = "skip item " + item.index;
            }
            model.phase = "backtrack";
            add(
                    steps,
                    model,
                    LINE_BACKTRACK,
                    "Backtrack at item " + item.index + ": " + model.backtrackChoice,
                    StepEventType.EXECUTE_LINE);
        }
        model.activeRow = -1;
        model.activeCapacity = -1;
        model.phase = "return";
        add(
                steps,
                model,
                LINE_RETURN,
                "Return value " + model.dp[items.size()][capacity] + " with selected weight " + model.selectedWeight,
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: best value = " + model.dp[items.size()][capacity]
                        + ", selected weight = " + model.selectedWeight,
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private static List<Item> readItems(JsonNode input) {
        JsonNode items = ParadigmSupport.requireField(input, TYPE, "items");
        if (!items.isArray() || items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException(
                    TYPE + " items length must be in the inclusive range 0.." + MAX_ITEMS);
        }
        List<Item> result = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            JsonNode item = items.get(index);
            if (item == null || !item.isObject()) {
                throw new IllegalArgumentException(TYPE + " items entries must be objects");
            }
            int weight = ParadigmSupport.boundedInt(
                    ParadigmSupport.requireField(item, TYPE, "weight"),
                    TYPE + " item weight",
                    1,
                    MAX_ITEM_WEIGHT);
            int value = ParadigmSupport.boundedInt(
                    ParadigmSupport.requireField(item, TYPE, "value"),
                    TYPE + " item value",
                    -MAX_ABS_VALUE,
                    MAX_ABS_VALUE);
            result.add(new Item(index, weight, value));
        }
        return List.copyOf(result);
    }

    private static void add(
            List<SimulationStep> steps,
            Model model,
            int line,
            String narration,
            StepEventType eventType) {
        ParadigmSupport.add(
                steps,
                model.state(),
                Set.of(),
                Set.of(),
                line,
                narration,
                eventType,
                TYPE,
                MAX_TRACE_STEPS);
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("algorithm", ALGORITHM);
        defaultInput.put("capacity", DEFAULT_CAPACITY);
        ArrayNode items = defaultInput.putArray("items");
        for (Item item : DEFAULT_ITEMS) {
            ObjectNode value = items.addObject();
            value.put("weight", item.weight);
            value.put("value", item.value);
        }
        return new SimulationMetadata(
                TYPE,
                "Dynamic Programming: 0/1 Knapsack",
                "O(nC)",
                "O(nC)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"algorithm\":\"KNAPSACK_01\",\"capacity\":7,\"items\":[{\"weight\":1,\"value\":1}]}; capacity must be 0.."
                        + MAX_CAPACITY + ", items must be 0.." + MAX_ITEMS + ", weights must be positive, and values must have absolute value at most "
                        + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }

    private record Item(int index, int weight, int value) {
    }

    private static final class Model {
        private final List<Item> items;
        private final int capacity;
        private final int[][] dp;
        private final boolean[][] computed;
        private final boolean[] chosen;
        private int activeRow = -1;
        private int activeCapacity = -1;
        private int selectedWeight;
        private int selectedValue;
        private Integer skip;
        private Integer take;
        private String dependency = "none";
        private String choice = "pending";
        private String backtrackChoice = "pending";
        private String phase = "pending";

        private Model(List<Item> items, int capacity) {
            this.items = List.copyOf(items);
            this.capacity = capacity;
            this.dp = new int[items.size() + 1][capacity + 1];
            this.computed = new boolean[items.size() + 1][capacity + 1];
            this.chosen = new boolean[items.size()];
        }

        private TableState state() {
            List<String> columns = new ArrayList<>(capacity + 2);
            columns.add("item");
            for (int current = 0; current <= capacity; current++) {
                columns.add("c" + current);
            }
            List<List<TypedCell>> rows = new ArrayList<>(items.size() + 1);
            rows.add(rowFor(0, null));
            for (int itemRow = 1; itemRow <= items.size(); itemRow++) {
                rows.add(rowFor(itemRow, items.get(itemRow - 1)));
            }
            SnapshotStatus resultStatus = phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
            List<Integer> selected = new ArrayList<>();
            for (int index = 0; index < chosen.length; index++) {
                if (chosen[index]) {
                    selected.add(index);
                }
            }
            List<Fact> facts = List.of(
                    ParadigmSupport.fact("algorithm", "DP_KNAPSACK", SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("teaching-equation", teachingEquation(), SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("teaching-detail", teachingDetail(), SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("target-row", Integer.toString(activeRow), SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("target-column", Integer.toString(activeCapacity < 0 ? -1 : activeCapacity + 1), SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("capacity", Integer.toString(capacity), SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("phase", phase, phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("target", activeRow >= 1 && activeCapacity >= 0
                            ? "dp[" + activeRow + "][" + activeCapacity + "]" : "none", SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("target-cell", activeRow >= 1 && activeCapacity >= 0
                            ? "dp-" + activeRow + "-" + activeCapacity : "none", SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("skip", skipExpression(), skip == null ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("take", takeExpression(), take == null ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("dependency", dependency, activeRow >= 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("choice", choice, activeRow >= 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("backtrack-choice", backtrackChoice, phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("best-value", Integer.toString(dp[items.size()][capacity]), resultStatus),
                    ParadigmSupport.fact("selected-items", selected.toString(), resultStatus),
                    ParadigmSupport.fact("selected-weight", Integer.toString(selectedWeight), resultStatus),
                    ParadigmSupport.fact("selected-value", Integer.toString(selectedValue), resultStatus));
            return new TableState(columns, rows, facts);
        }

        private String teachingEquation() {
            if (activeRow < 1 || activeCapacity < 0) {
                return phase.equals("complete") || phase.equals("return")
                        ? "Best value = " + dp[items.size()][capacity] + "; selected weight = " + selectedWeight
                        : "dp[i][c]: best value using the first i items within capacity c";
            }
            String target = "dp[" + activeRow + "][" + activeCapacity + "]";
            return switch (phase) {
                case "skip" -> "Skip: " + skipExpression();
                case "take" -> "Take: " + takeExpression();
                case "fill" -> target + " = max(" + skip + ", "
                        + (take == null ? "unavailable" : take) + ") = " + dp[activeRow][activeCapacity];
                case "backtrack" -> target + " = " + dp[activeRow][activeCapacity] + ": " + backtrackChoice;
                default -> "Evaluate " + target;
            };
        }

        private String teachingDetail() {
            if (activeRow < 1 || activeCapacity < 0) {
                return "Each item can be chosen once; both choices read the previous row.";
            }
            Item item = items.get(activeRow - 1);
            if (phase.equals("fill")) {
                return "Skip: " + skipExpression() + "; take: " + takeExpression();
            }
            return "Item " + item.index + ": weight " + item.weight + ", value " + item.value
                    + "; capacity " + activeCapacity + ". Read row " + (activeRow - 1) + ".";
        }

        private String skipExpression() {
            if (skip == null || activeRow < 1 || activeCapacity < 0) {
                return "not evaluated";
            }
            return "dp[" + (activeRow - 1) + "][" + activeCapacity + "] = " + skip;
        }

        private String takeExpression() {
            if (activeRow < 1 || activeCapacity < 0 || !(phase.equals("take") || phase.equals("fill"))) {
                return "not evaluated";
            }
            Item item = items.get(activeRow - 1);
            if (take == null) {
                return "unavailable: weight " + item.weight + " > capacity " + activeCapacity;
            }
            int remaining = activeCapacity - item.weight;
            return item.value + " + dp[" + (activeRow - 1) + "][" + remaining + "] = "
                    + item.value + " + " + dp[activeRow - 1][remaining] + " = " + take;
        }

        private List<TypedCell> rowFor(int itemRow, Item item) {
            SnapshotStatus itemStatus = SnapshotStatus.DEFAULT;
            if (item != null && chosen[item.index]) {
                itemStatus = SnapshotStatus.DONE;
            }
            if (activeRow == itemRow) {
                itemStatus = SnapshotStatus.ACTIVE;
            }
            String itemLabel = item == null
                    ? "base"
                    : "item " + item.index + " (w=" + item.weight + ",v=" + item.value + ")";
            List<TypedCell> cells = new ArrayList<>(capacity + 2);
            cells.add(ParadigmSupport.cell("item-" + itemRow, itemLabel, itemStatus));
            Item activeItem = activeRow >= 1 && activeRow <= items.size()
                    ? items.get(activeRow - 1)
                    : null;
            for (int current = 0; current <= capacity; current++) {
                SnapshotStatus status = computed[itemRow][current]
                        ? SnapshotStatus.DONE
                        : SnapshotStatus.DEFAULT;
                if (activeRow == itemRow && activeCapacity == current) {
                    status = SnapshotStatus.ACTIVE;
                } else if (activeItem != null && activeRow == itemRow + 1
                        && ((skip != null && activeCapacity == current)
                        || (take != null && activeCapacity - activeItem.weight == current))) {
                    status = SnapshotStatus.ACTIVE;
                }
                String value = computed[itemRow][current] ? Integer.toString(dp[itemRow][current]) : "?";
                cells.add(ParadigmSupport.cell("dp-" + itemRow + "-" + current, value, status));
            }
            return List.copyOf(cells);
        }
    }

}
