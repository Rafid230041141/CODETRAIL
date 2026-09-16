package dev.codetrail.desktop.simulation;

/** Semantic event attached to a precomputed trace step. */
public enum StepEventType {
    INITIALIZE,
    EXECUTE_LINE,
    PUSH_FRAME,
    RETURN_FRAME,
    COMPLETE
}
