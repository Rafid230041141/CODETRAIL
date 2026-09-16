package dev.codetrail.desktop.simulation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable runtime call-stack state. {@code frames} is the active stack from
 * bottom to top; a just-returned frame is exposed once as
 * {@code lastReturnedFrame} and its value is retained in {@code facts}.
 */
public final class StackState implements SimulationState {
    private final List<CallFrame> frames;
    private final List<Fact> facts;
    private final Set<String> activeFrameIds;
    private final CallFrame lastReturnedFrame;

    public StackState(List<CallFrame> frames, List<Fact> facts, Set<String> activeFrameIds) {
        this(frames, facts, activeFrameIds, null);
    }

    public StackState(
            List<CallFrame> frames,
            List<Fact> facts,
            Set<String> activeFrameIds,
            CallFrame lastReturnedFrame) {
        Objects.requireNonNull(frames, "frames");
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(activeFrameIds, "activeFrameIds");
        this.frames = frames.stream().map(frame -> Objects.requireNonNull(frame, "frame").copy()).toList();
        this.facts = facts.stream().map(fact -> Objects.requireNonNull(fact, "fact").copy()).toList();
        Set<String> frameIds = this.frames.stream().map(CallFrame::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!frameIds.containsAll(activeFrameIds)) {
            throw new IllegalArgumentException("activeFrameIds must refer to visible frames");
        }
        if (activeFrameIds.size() > 1) {
            throw new IllegalArgumentException("only the top frame can be ACTIVE");
        }
        if (!this.frames.isEmpty()) {
            CallFrame top = this.frames.get(this.frames.size() - 1);
            if (top.status() != SnapshotStatus.ACTIVE || !activeFrameIds.contains(top.id())) {
                throw new IllegalArgumentException("a nonempty runtime stack must have an active top frame");
            }
            for (int index = 0; index < this.frames.size() - 1; index++) {
                if (this.frames.get(index).status() == SnapshotStatus.ACTIVE
                        || activeFrameIds.contains(this.frames.get(index).id())) {
                    throw new IllegalArgumentException("only the top frame can be ACTIVE");
                }
            }
        } else if (!activeFrameIds.isEmpty()) {
            throw new IllegalArgumentException("an empty runtime stack cannot have active frames");
        }
        if (lastReturnedFrame != null && lastReturnedFrame.status() != SnapshotStatus.DONE) {
            throw new IllegalArgumentException("lastReturnedFrame must be DONE");
        }
        this.lastReturnedFrame = lastReturnedFrame == null ? null : lastReturnedFrame.copy();
        this.activeFrameIds = Set.copyOf(activeFrameIds);
    }

    public StackState(List<CallFrame> frames, List<Fact> facts) {
        this(frames, facts, frames.stream().map(CallFrame::id).collect(java.util.stream.Collectors.toSet()));
    }

    @Override
    public RendererFamily rendererFamily() { return RendererFamily.STACK; }

    public List<CallFrame> frames() { return frames; }
    public List<Fact> facts() { return facts; }
    public Set<String> activeFrameIds() { return activeFrameIds; }
    public Optional<CallFrame> lastReturnedFrame() {
        return Optional.ofNullable(lastReturnedFrame).map(CallFrame::copy);
    }
    public CallFrame returnedFrameOrNull() {
        return lastReturnedFrame == null ? null : lastReturnedFrame.copy();
    }

    @Override
    public StackState copy() { return new StackState(frames, facts, activeFrameIds, lastReturnedFrame); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StackState state)) return false;
        return frames.equals(state.frames)
                && facts.equals(state.facts)
                && activeFrameIds.equals(state.activeFrameIds)
                && Objects.equals(lastReturnedFrame, state.lastReturnedFrame);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frames, facts, activeFrameIds, lastReturnedFrame);
    }
}
