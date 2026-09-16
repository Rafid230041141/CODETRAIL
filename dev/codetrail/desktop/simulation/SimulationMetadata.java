package dev.codetrail.desktop.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;

/** Immutable metadata shown beside a simulation and its pseudocode. */
public final class SimulationMetadata {
    private final String type;
    private final String title;
    private final String timeComplexity;
    private final String spaceComplexity;
    private final RendererFamily rendererFamily;
    private final JsonNode defaultInput;
    private final String inputHelp;
    private final List<String> pseudocode;

    public SimulationMetadata(
            String type,
            String title,
            String timeComplexity,
            String spaceComplexity,
            RendererFamily rendererFamily,
            JsonNode defaultInput,
            String inputHelp,
            List<String> pseudocode) {
        this.type = requireText(type, "type");
        this.title = requireText(title, "title");
        this.timeComplexity = requireText(timeComplexity, "timeComplexity");
        this.spaceComplexity = requireText(spaceComplexity, "spaceComplexity");
        this.rendererFamily = Objects.requireNonNull(rendererFamily, "rendererFamily");
        this.defaultInput = Objects.requireNonNull(defaultInput, "defaultInput").deepCopy();
        this.inputHelp = requireText(inputHelp, "inputHelp");
        Objects.requireNonNull(pseudocode, "pseudocode");
        if (pseudocode.isEmpty() || pseudocode.stream().anyMatch(line -> line == null || line.isBlank())) {
            throw new IllegalArgumentException("pseudocode must contain nonblank lines");
        }
        this.pseudocode = List.copyOf(pseudocode);
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String timeComplexity() {
        return timeComplexity;
    }

    public String spaceComplexity() {
        return spaceComplexity;
    }

    public RendererFamily rendererFamily() {
        return rendererFamily;
    }

    /** Returns a deep copy because Jackson nodes are mutable. */
    public JsonNode defaultInput() {
        return defaultInput.deepCopy();
    }

    public String inputHelp() {
        return inputHelp;
    }

    public List<String> pseudocode() {
        return pseudocode;
    }

    // Bean-style aliases keep the boundary convenient for JavaFX property code.
    public String getType() { return type(); }
    public String getTitle() { return title(); }
    public String getTimeComplexity() { return timeComplexity(); }
    public String getSpaceComplexity() { return spaceComplexity(); }
    public RendererFamily getRendererFamily() { return rendererFamily(); }
    public JsonNode getDefaultInput() { return defaultInput(); }
    public String getInputHelp() { return inputHelp(); }
    public List<String> getPseudocode() { return pseudocode(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SimulationMetadata metadata)) return false;
        return type.equals(metadata.type)
                && title.equals(metadata.title)
                && timeComplexity.equals(metadata.timeComplexity)
                && spaceComplexity.equals(metadata.spaceComplexity)
                && rendererFamily == metadata.rendererFamily
                && defaultInput.equals(metadata.defaultInput)
                && inputHelp.equals(metadata.inputHelp)
                && pseudocode.equals(metadata.pseudocode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, timeComplexity, spaceComplexity, rendererFamily,
                defaultInput, inputHelp, pseudocode);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
        return value;
    }
}
