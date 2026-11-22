package com.originpixel.motion;

/** Deterministic, frame-rate-independent damped spring step for offline motion tests. */
public final class SpringIntegrator {
    public record State(double position, double velocity) {}
    private SpringIntegrator() {}

    public static State step(State state, double target, double stiffness, double dampingRatio, double dtSeconds) {
        if (state == null || !Double.isFinite(target) || !Double.isFinite(stiffness)
                || stiffness <= 0 || !Double.isFinite(dampingRatio) || dampingRatio < 0
                || !Double.isFinite(dtSeconds) || dtSeconds <= 0 || dtSeconds > 0.05) {
            throw new IllegalArgumentException("Invalid spring input or timestep");
        }
        double acceleration = stiffness * (target - state.position())
                - 2.0 * dampingRatio * Math.sqrt(stiffness) * state.velocity();
        double velocity = state.velocity() + acceleration * dtSeconds;
        double position = state.position() + velocity * dtSeconds;
        return new State(position, velocity);
    }
}
