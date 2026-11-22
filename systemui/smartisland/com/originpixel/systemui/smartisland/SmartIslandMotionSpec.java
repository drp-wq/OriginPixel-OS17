package com.originpixel.systemui.smartisland;

import com.originpixel.systemui.smartisland.SmartIslandContract.IslandState;
import java.util.Objects;

/**
 * OriginPixel OS 17 — SmartIsland Spring Motion, Reduced-Motion & Haptics Specification.
 *
 * Aligns with L1 design tokens (overlays/design-tokens/originpixel-tokens.json). Explicit
 * reduced motion uses a 120ms crossfade; Android's zero animator scale disables motion.
 */
public final class SmartIslandMotionSpec {

    public static final int REDUCED_MOTION_CROSSFADE_MS = 120;
    public static final int NO_OP_DURATION_MS = 0;

    public enum SpringPreset {
        SNAPPY(500.0f, 0.82f),
        FLUID(320.0f, 0.85f),
        GENTLE(200.0f, 0.90f);

        public final float stiffness;
        public final float dampingRatio;

        SpringPreset(float stiffness, float dampingRatio) {
            this.stiffness = stiffness;
            this.dampingRatio = dampingRatio;
        }
    }

    public enum HapticCue {
        NONE,
        EXPAND_CONFIRM,
        COLLAPSE_LIGHT,
        DISMISS_TICK
    }

    public record MotionPlan(
            boolean useSpring,
            SpringPreset springPreset,
            int fallbackDurationMs,
            boolean interruptible,
            HapticCue hapticCue
    ) {}

    private SmartIslandMotionSpec() {}

    public static MotionPlan computeTransition(
            IslandState from,
            IslandState to,
            boolean reducedMotion,
            float animatorDurationScale,
            boolean hapticsEnabled
    ) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        // Content can change while the visible island state stays the same. Do not
        // replay motion or haptics for those data-only updates.
        if (from == to) {
            return new MotionPlan(false, null, NO_OP_DURATION_MS, true, HapticCue.NONE);
        }

        HapticCue cue = HapticCue.NONE;
        if (hapticsEnabled && from != to) {
            if (to == IslandState.EXPANDED) {
                cue = HapticCue.EXPAND_CONFIRM;
            } else if (to == IslandState.COLLAPSED) {
                cue = HapticCue.COLLAPSE_LIGHT;
            } else if (to == IslandState.HIDDEN) {
                cue = HapticCue.DISMISS_TICK;
            }
        }

        boolean animationsDisabled = !Float.isFinite(animatorDurationScale)
                || animatorDurationScale <= 0.0f;
        if (reducedMotion || animationsDisabled) {
            int duration = reducedMotion && !animationsDisabled
                    ? REDUCED_MOTION_CROSSFADE_MS
                    : NO_OP_DURATION_MS;
            return new MotionPlan(false, null, duration, true, cue);
        }

        SpringPreset preset = (to == IslandState.EXPANDED)
                ? SpringPreset.FLUID
                : (to == IslandState.HIDDEN ? SpringPreset.GENTLE : SpringPreset.SNAPPY);

        return new MotionPlan(true, preset, 0, true, cue);
    }
}
