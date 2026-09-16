package dev.codetrail.desktop.simulation;

import java.util.List;
import java.util.Objects;

/** Immutable table/grid state. Each row has one typed cell per column. */
public final class TableState implements SimulationState {
    private final List<String> columns;
    private final List<List<TypedCell>> rows;
    private final List<Fact> facts;

    public TableState(List<String> columns, List<List<TypedCell>> rows) {
        this(columns, rows, List.of());
    }

    public TableState(List<String> columns, List<List<TypedCell>> rows, List<Fact> facts) {
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(rows, "rows");
        this.columns = columns.stream().map(column -> {
            if (column == null || column.isBlank()) {
                throw new IllegalArgumentException("column names must be nonblank");
            }
            return column;
        }).toList();
        this.rows = rows.stream().map(row -> {
            Objects.requireNonNull(row, "row");
            if (row.size() != this.columns.size()) {
                throw new IllegalArgumentException("every table row must match the column count");
            }
            return row.stream()
                    .map(cell -> Objects.requireNonNull(cell, "cell").copy())
                    .toList();
        }).toList();
        this.facts = TreeState.copyFacts(facts);
    }

    @Override
    public RendererFamily rendererFamily() { return RendererFamily.TABLE; }

    public List<String> columns() { return columns; }
    public List<List<TypedCell>> rows() { return rows; }
    public List<Fact> facts() { return facts; }

    @Override
    public TableState copy() { return new TableState(columns, rows, facts); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TableState state)) return false;
        return columns.equals(state.columns) && rows.equals(state.rows) && facts.equals(state.facts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, rows, facts);
    }
}
