package dev.codetrail.desktop.simulation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** A view of recorded trace data, not a source-level debugger or inferred variables. */
public record TraceCaption(String activeLine, List<String> changes, String changeHeading,
                           String narration, String recordedValues) {
    public TraceCaption {
        changes = List.copyOf(changes);
    }

    public static TraceCaption at(SimulationTrace trace, int index) {
        SimulationStep step = trace.stepAt(index);
        int line = step.highlightedPseudocodeLine();
        String activeLine = line == 0 ? "No active pseudocode line"
                : "Line " + line + " · " + trace.metadata().pseudocode().get(line - 1).strip();
        List<Fact> facts = facts(step.stateSnapshot());
        Map<String, String> previous = new LinkedHashMap<>();
        if (index > 0) facts(trace.stepAt(index - 1).stateSnapshot())
                .forEach(fact -> previous.put(fact.key(), fact.value()));
        // Compare against the trace's preceding step, not the last screen visited.
        // This keeps Back, Forward, and Restart independent of navigation history.
        List<String> changes = facts.stream()
                .filter(fact -> scalar(fact.key(), fact.value()))
                .filter(fact -> index == 0 || !Objects.equals(previous.get(fact.key()), fact.value()))
                .sorted(Comparator.comparingInt(TraceCaption::priority))
                .limit(2)
                .map(fact -> fact.key() + " = " + fact.value())
                .toList();
        String recorded = facts.stream().map(fact -> fact.key() + " = " + fact.value())
                .collect(Collectors.joining("\n"));
        if (recorded.isEmpty()) recorded = "See the visualisation for this step's recorded structure.";
        return new TraceCaption(activeLine, changes, index == 0 ? "Recorded: " : "Changed: ",
                step.narration(), recorded);
    }

    private static int priority(Fact fact) {
        if (fact.key().equalsIgnoreCase("phase") || fact.key().equalsIgnoreCase("algorithm")) return 2;
        return fact.status() == SnapshotStatus.ACTIVE ? 0 : 1;
    }

    private static boolean scalar(String key, String value) {
        return key.length() <= 24 && value.length() <= 32 && !value.isBlank()
                && value.chars().noneMatch(character -> "\n\r[]{};,".indexOf(character) >= 0);
    }

    private static List<Fact> facts(SimulationState state) {
        if (state instanceof ArrayState array) return array.facts();
        if (state instanceof TreeState tree) return tree.facts();
        if (state instanceof GraphState graph) return graph.facts();
        if (state instanceof TableState table) return table.facts();
        if (state instanceof StackState stack) return stack.facts();
        if (state instanceof LinkedState linked) {
            if (!linked.facts().isEmpty()) return linked.facts();
            String head = linked.headId() == null ? "none" : linked.nodes().stream()
                    .filter(node -> node.id().equals(linked.headId())).map(Node::label)
                    .findFirst().orElse(linked.headId());
            return List.of(new Fact("Head", head, SnapshotStatus.ACTIVE),
                    new Fact("Nodes", Integer.toString(linked.nodes().size()), SnapshotStatus.DEFAULT),
                    new Fact("Links", Integer.toString(linked.edges().size()), SnapshotStatus.DEFAULT));
        }
        return List.of();
    }
}
