package dev.codetrail.desktop.simulation;

import java.util.List;
import java.util.Objects;

/** Immutable array/bars state. */
public final class ArrayState implements SimulationState {
    private final List<TypedCell> cells;
    private final int focusIndex;
    private final List<Fact> facts;

    public ArrayState(List<TypedCell> cells, int focusIndex) {
        this(cells, focusIndex, List.of());
    }

    public ArrayState(List<TypedCell> cells, int focusIndex, List<Fact> facts) {
        Objects.requireNonNull(cells, "cells");
        if (focusIndex < -1 || focusIndex >= cells.size()) {
            throw new IllegalArgumentException("focusIndex must be -1 or an existing cell index");
        }
        this.cells = cells.stream().map(cell -> Objects.requireNonNull(cell, "cell").copy()).toList();
        this.focusIndex = focusIndex;
        this.facts = TreeState.copyFacts(facts);
    }

    public ArrayState(List<TypedCell> cells) {
        this(cells, -1);
    }

    @Override
    public RendererFamily rendererFamily() { return RendererFamily.ARRAY; }

    public List<TypedCell> cells() { return cells; }
    public int focusIndex() { return focusIndex; }
    public List<Fact> facts() { return facts; }

    @Override
    public ArrayState copy() { return new ArrayState(cells, focusIndex, facts); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ArrayState state)) return false;
        return focusIndex == state.focusIndex && cells.equals(state.cells) && facts.equals(state.facts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cells, focusIndex, facts);
    }
}
