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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Earliest-finish activity selection with a bounded independent optimum check. */
public final class GreedyActivityEngine implements SimulationEngine {
    public static final String TYPE = "GREEDY_ACTIVITY_SELECTION";
    public static final int MAX_ACTIVITIES = 12;
    public static final int MAX_ABS_TIME = 999;
    public static final int MAX_SUBSETS = 4096;
    public static final int MAX_TRACE_STEPS = 4096;

    private static final int LINE_METHOD = 1;
    private static final int LINE_SORT = 2;
    private static final int LINE_INITIALIZE = 3;
    private static final int LINE_LOOP = 4;
    private static final int LINE_CHECK = 5;
    private static final int LINE_ACCEPT = 6;
    private static final int LINE_REJECT = 7;
    private static final int LINE_RETURN = 8;

    private static final List<Activity> DEFAULT_ACTIVITIES = List.of(
            new Activity(0, 5, 7),
            new Activity(1, 1, 4),
            new Activity(2, 8, 11),
            new Activity(3, 3, 5),
            new Activity(4, 12, 16),
            new Activity(5, 0, 6),
            new Activity(6, 3, 9),
            new Activity(7, 5, 9),
            new Activity(8, 6, 10),
            new Activity(9, 8, 12),
            new Activity(10, 2, 14));
    private static final List<String> PSEUDOCODE = List.of(
            "select(activities):",
            "    sort activities by nondecreasing finish time",
            "    lastFinish = -infinity; selected = []",
            "    for activity in activities:",
            "        if activity.start >= lastFinish:",
            "            select activity; lastFinish = activity.finish",
            "        else: skip activity because it overlaps",
            "    return selected");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        ParadigmSupport.requireObject(input, TYPE);
        List<Activity> original = readActivities(input);
        Oracle oracle = bruteForceOptimal(original);
        Model model = new Model(original, oracle);
        List<SimulationStep> steps = new ArrayList<>();

        model.phase = "initialize";
        add(
                steps,
                model,
                0,
                "Initialize " + original.size() + " activity row(s) for earliest-finish selection",
                StepEventType.INITIALIZE);
        model.phase = "sort";
        add(steps, model, LINE_METHOD, "Select a maximum compatible activity schedule", StepEventType.EXECUTE_LINE);
        model.sortByFinish();
        add(steps, model, LINE_SORT, "Process activities by earliest finish time", StepEventType.EXECUTE_LINE);
        model.lastFinish = null;
        model.phase = "scan";
        add(steps, model, LINE_INITIALIZE, "Set lastFinish before the first greedy choice", StepEventType.EXECUTE_LINE);

        for (int sortedIndex = 0; sortedIndex < model.activities.size(); sortedIndex++) {
            Activity activity = model.activities.get(sortedIndex);
            model.currentIndex = sortedIndex;
            model.statuses[sortedIndex] = SnapshotStatus.ACTIVE;
            model.phase = "consider";
            model.comparison = "not evaluated";
            add(
                    steps,
                    model,
                    LINE_LOOP,
                    "Consider activity A" + activity.originalIndex + " in finish order",
                    StepEventType.EXECUTE_LINE);
            boolean compatible = model.lastFinish == null || activity.start >= model.lastFinish;
            model.phase = compatible ? "compatible" : "incompatible";
            model.comparison = "start " + activity.start + " >= last finish "
                    + (model.lastFinish == null ? "-infinity" : model.lastFinish) + " → " + compatible;
            add(
                    steps,
                    model,
                    LINE_CHECK,
                    model.comparison,
                    StepEventType.EXECUTE_LINE);
            if (compatible) {
                model.statuses[sortedIndex] = SnapshotStatus.DONE;
                model.selected.add(activity.originalIndex);
                model.lastFinish = activity.finish;
                model.phase = "accept";
                add(
                        steps,
                        model,
                        LINE_ACCEPT,
                        "Accept A" + activity.originalIndex + " and advance last finish to " + activity.finish,
                        StepEventType.EXECUTE_LINE);
            } else {
                model.statuses[sortedIndex] = SnapshotStatus.REJECTED;
                model.phase = "skip";
                add(
                        steps,
                        model,
                        LINE_REJECT,
                        "Skip A" + activity.originalIndex + " because it overlaps the schedule",
                        StepEventType.EXECUTE_LINE);
            }
            model.currentIndex = -1;
        }

        model.phase = "return";
        add(
                steps,
                model,
                LINE_RETURN,
                "Return " + model.selected.size() + " greedy choice(s); bounded optimum is " + oracle.count,
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: greedy = " + model.selected.size() + ", exhaustive optimum = " + oracle.count,
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private static List<Activity> readActivities(JsonNode input) {
        JsonNode activities = ParadigmSupport.requireField(input, TYPE, "activities");
        if (!activities.isArray() || activities.size() > MAX_ACTIVITIES) {
            throw new IllegalArgumentException(
                    TYPE + " activities length must be in the inclusive range 0.." + MAX_ACTIVITIES);
        }
        List<Activity> result = new ArrayList<>(activities.size());
        for (int index = 0; index < activities.size(); index++) {
            JsonNode item = activities.get(index);
            if (item == null || !item.isObject()) {
                throw new IllegalArgumentException(TYPE + " activities entries must be objects");
            }
            int start = ParadigmSupport.boundedInt(
                    ParadigmSupport.requireField(item, TYPE, "start"),
                    TYPE + " activity start",
                    -MAX_ABS_TIME,
                    MAX_ABS_TIME);
            int finish = ParadigmSupport.boundedInt(
                    ParadigmSupport.requireField(item, TYPE, "finish"),
                    TYPE + " activity finish",
                    -MAX_ABS_TIME,
                    MAX_ABS_TIME);
            if (start >= finish) {
                throw new IllegalArgumentException(TYPE + " activities require start < finish");
            }
            result.add(new Activity(index, start, finish));
        }
        return List.copyOf(result);
    }

    private static Oracle bruteForceOptimal(List<Activity> activities) {
        int count = activities.size();
        if (count >= Integer.SIZE - 1) {
            throw new IllegalArgumentException(TYPE + " activity count is too large to enumerate");
        }
        int subsets = 1 << count;
        if (subsets > MAX_SUBSETS) {
            throw new IllegalArgumentException(TYPE + " exhaustive comparison exceeds the subset bound");
        }
        int bestCount = -1;
        int bestMask = 0;
        for (int mask = 0; mask < subsets; mask++) {
            List<Activity> candidate = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                if ((mask & (1 << index)) != 0) {
                    candidate.add(activities.get(index));
                }
            }
            candidate.sort(Activity.ORDER);
            Integer lastFinish = null;
            boolean compatible = true;
            for (Activity activity : candidate) {
                if (lastFinish != null && activity.start < lastFinish) {
                    compatible = false;
                    break;
                }
                lastFinish = activity.finish;
            }
            if (compatible && candidate.size() > bestCount) {
                bestCount = candidate.size();
                bestMask = mask;
            }
        }
        List<Integer> selected = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            if ((bestMask & (1 << index)) != 0) {
                selected.add(index);
            }
        }
        return new Oracle(bestCount, selected);
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
        ArrayNode activities = defaultInput.putArray("activities");
        for (Activity activity : DEFAULT_ACTIVITIES) {
            ObjectNode value = activities.addObject();
            value.put("start", activity.start);
            value.put("finish", activity.finish);
        }
        return new SimulationMetadata(
                TYPE,
                "Greedy: Activity Selection",
                "O(m log m)",
                "O(m)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"activities\":[{\"start\":1,\"finish\":4}]}; use 0.."
                        + MAX_ACTIVITIES + " activities, each with bounded integer start < finish. The trace compares the greedy count with an exhaustive optimum over at most "
                        + MAX_SUBSETS + " subsets.",
                PSEUDOCODE);
    }

    private record Activity(int originalIndex, int start, int finish) {
        private static final Comparator<Activity> ORDER = Comparator
                .comparingInt(Activity::finish)
                .thenComparingInt(Activity::start)
                .thenComparingInt(Activity::originalIndex);
    }

    private record Oracle(int count, List<Integer> selectedOriginalOrder) {
        private Oracle {
            selectedOriginalOrder = List.copyOf(selectedOriginalOrder);
        }
    }

    private static final class Model {
        private final List<Activity> activities;
        private final Oracle oracle;
        private final SnapshotStatus[] statuses;
        private final List<Integer> selected = new ArrayList<>();
        private Integer lastFinish;
        private int currentIndex = -1;
        private String phase = "pending";
        private String comparison = "not evaluated";

        private Model(List<Activity> activities, Oracle oracle) {
            this.activities = new ArrayList<>(activities);
            this.oracle = oracle;
            this.statuses = new SnapshotStatus[activities.size()];
            java.util.Arrays.fill(this.statuses, SnapshotStatus.DEFAULT);
        }

        private void sortByFinish() {
            activities.sort(Activity.ORDER);
        }

        private TableState state() {
            List<String> columns = List.of("activity", "start", "finish", "decision");
            List<List<TypedCell>> rows = new ArrayList<>(activities.size());
            for (int index = 0; index < activities.size(); index++) {
                Activity activity = activities.get(index);
                SnapshotStatus status = statuses[index];
                String decision;
                if (statuses[index] == SnapshotStatus.DONE) {
                    decision = "selected";
                } else if (statuses[index] == SnapshotStatus.REJECTED) {
                    decision = "skipped";
                } else if (currentIndex == index) {
                    decision = "consider";
                } else {
                    decision = "pending";
                }
                rows.add(List.of(
                        ParadigmSupport.cell("activity-" + activity.originalIndex, "A" + activity.originalIndex, status),
                        ParadigmSupport.cell("start-" + activity.originalIndex, Integer.toString(activity.start), status),
                        ParadigmSupport.cell("finish-" + activity.originalIndex, Integer.toString(activity.finish), status),
                        ParadigmSupport.cell("decision-" + activity.originalIndex, decision, status)));
            }
            SnapshotStatus finalStatus = phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
            List<Fact> facts = List.of(
                    ParadigmSupport.fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("teaching-equation", currentIndex >= 0
                            ? comparison.equals("not evaluated")
                                    ? "A" + activities.get(currentIndex).originalIndex + ": start " + activities.get(currentIndex).start
                                            + ", finish " + activities.get(currentIndex).finish
                                    : comparison
                            : phase.equals("complete") || phase.equals("return")
                                    ? "Selected " + selected.size() + " activities; maximum possible = " + oracle.count
                                    : "Sort by finish time, then accept each compatible activity", SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("teaching-detail", phase.equals("initialize")
                            ? "Activities begin in input order."
                            : phase.equals("sort") ? "Earlier finish leaves the most time for later activities."
                            : "Selected activities: " + ParadigmSupport.formatIntegerList(selected)
                                    + "; last finish = " + (lastFinish == null ? "-infinity" : lastFinish), SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("target-row", Integer.toString(currentIndex), SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("target-column", currentIndex < 0 ? "-1" : "1", SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("phase", phase, phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("last-finish", lastFinish == null ? "-infinity" : Integer.toString(lastFinish), SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("current-start", currentIndex < 0 ? "none"
                            : Integer.toString(activities.get(currentIndex).start), SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("comparison", comparison, phase.equals("incompatible") || phase.equals("skip")
                            ? SnapshotStatus.REJECTED : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("greedy-count", Integer.toString(selected.size()), finalStatus),
                    ParadigmSupport.fact("greedy-selection", ParadigmSupport.formatIntegerList(selected), finalStatus),
                    ParadigmSupport.fact("optimal-count", Integer.toString(oracle.count), finalStatus),
                    ParadigmSupport.fact("optimal-selection", ParadigmSupport.formatIntegerList(oracle.selectedOriginalOrder), finalStatus));
            return new TableState(columns, rows, facts);
        }
    }
}
