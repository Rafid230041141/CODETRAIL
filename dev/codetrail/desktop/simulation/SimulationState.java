package dev.codetrail.desktop.simulation;

/** Typed immutable state consumed by exactly one renderer family. */
public sealed interface SimulationState
        permits ArrayState, LinkedState, TreeState, GraphState, TableState, StackState {
    RendererFamily rendererFamily();

    SimulationState copy();
}
