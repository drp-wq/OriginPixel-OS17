package com.originpixel.systemui.smartisland;

import com.originpixel.systemui.smartisland.SmartIslandContract.*;
import com.originpixel.systemui.smartisland.SmartIslandMotionSpec.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OriginPixel OS 17 — Native SmartIsland Collapsed/Expanded State Controller.
 *
 * Bridges SmartIslandContract state arbitration with L1 surface/motion tokens and
 * notifies SystemUI view observers with immutable UiRenderModel snapshots.
 */
public final class SmartIslandUiController {

    public record SurfaceGeometry(
            int widthDp,
            int heightDp,
            int cornerRadiusDp,
            int blurRadiusDp,
            float alpha
    ) {}

    public record UiRenderModel(
            RenderSnapshot snapshot,
            MotionPlan motionPlan,
            SurfaceGeometry geometry
    ) {}

    public interface StateListener {
        void onRenderStateChanged(UiRenderModel model);
    }

    private final SmartIslandContract contract;
    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();

    private IslandState lastState = IslandState.HIDDEN;
    private boolean reducedMotion = false;
    private boolean reducedTransparency = false;
    private boolean hapticsEnabled = true;
    private float animatorDurationScale = 1.0f;

    public SmartIslandUiController(SmartIslandContract contract) {
        this.contract = Objects.requireNonNull(contract, "contract");
    }

    public void addListener(StateListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(StateListener listener) {
        listeners.remove(listener);
    }

    public synchronized void updateEnvironment(
            boolean reducedMotion,
            boolean reducedTransparency,
            boolean hapticsEnabled,
            float animatorDurationScale
    ) {
        this.reducedMotion = reducedMotion;
        this.reducedTransparency = reducedTransparency;
        this.hapticsEnabled = hapticsEnabled;
        this.animatorDurationScale = Math.max(0.0f, animatorDurationScale);
        dispatchCurrent();
    }

    public synchronized void onPillTapped() {
        RenderSnapshot current = contract.snapshot();
        if (current.state() == IslandState.COLLAPSED) {
            contract.setExpanded(true);
        } else if (current.state() == IslandState.EXPANDED) {
            contract.setExpanded(false);
        }
        dispatchCurrent();
    }

    public synchronized void onSwipeDismissPrimary() {
        RenderSnapshot current = contract.snapshot();
        if (current.primaryActivity() != null
                && current.primaryActivity().priority() != Priority.PRIVACY_INDICATOR) {
            contract.dismiss(current.primaryActivity().id());
        }
        dispatchCurrent();
    }

    public synchronized UiRenderModel dispatchCurrent() {
        RenderSnapshot snap = contract.snapshot();
        MotionPlan motion = SmartIslandMotionSpec.computeTransition(
                lastState, snap.state(), reducedMotion, animatorDurationScale, hapticsEnabled
        );
        SurfaceGeometry geom = computeGeometry(snap.state(), snap.activeCount());
        lastState = snap.state();

        UiRenderModel model = new UiRenderModel(snap, motion, geom);
        for (StateListener l : listeners) {
            l.onRenderStateChanged(model);
        }
        return model;
    }

    private SurfaceGeometry computeGeometry(IslandState state, int activeCount) {
        int blur = reducedTransparency ? 0 : (state == IslandState.EXPANDED ? 24 : 12);
        float alpha = reducedTransparency ? 1.0f : (state == IslandState.EXPANDED ? 0.68f : 0.78f);

        return switch (state) {
            case HIDDEN -> new SurfaceGeometry(0, 0, 999, 0, 0.0f);
            case COLLAPSED -> new SurfaceGeometry(activeCount > 1 ? 216 : 168, 36, 999, blur, alpha);
            case EXPANDED -> new SurfaceGeometry(348, 132, 24, blur, alpha);
        };
    }
}
