package com.originpixel.systemui.core;

import java.util.Objects;

/** Pure state contracts for native shade, Control Center and lock-screen integration. */
public final class SurfaceStateContract {
    public enum Surface { SHADE, CONTROL_CENTER, LOCK_SCREEN }
    public enum Gesture { PULL_DOWN, PULL_UP, HORIZONTAL }
    public enum State { HIDDEN, PEEK, EXPANDED }

    public record Snapshot(Surface surface, State state, boolean locked, boolean sensitiveContentVisible) {}

    private Surface activeSurface = Surface.SHADE;
    private State state = State.HIDDEN;
    private boolean locked;

    public synchronized Snapshot onGesture(Gesture gesture) {
        Objects.requireNonNull(gesture);
        if (gesture == Gesture.HORIZONTAL) return snapshot();
        if (gesture == Gesture.PULL_DOWN) {
            if (state == State.HIDDEN) state = State.PEEK;
            else if (state == State.PEEK) {
                state = State.EXPANDED;
                activeSurface = activeSurface == Surface.SHADE ? Surface.CONTROL_CENTER : Surface.SHADE;
            }
        } else if (state == State.EXPANDED) state = State.PEEK;
        else if (state == State.PEEK) state = State.HIDDEN;
        return snapshot();
    }

    public synchronized Snapshot onKeyguardChanged(boolean showing) {
        locked = showing;
        if (locked && state == State.EXPANDED) state = State.PEEK;
        if (locked) activeSurface = Surface.LOCK_SCREEN;
        else if (activeSurface == Surface.LOCK_SCREEN) activeSurface = Surface.SHADE;
        return snapshot();
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(activeSurface, state, locked, !locked && state == State.EXPANDED);
    }
}
