package com.originpixel.systemui.smartisland;

import com.originpixel.systemui.smartisland.SmartIslandContract.*;
import com.originpixel.systemui.smartisland.SmartIslandMotionSpec.*;
import com.originpixel.systemui.smartisland.SmartIslandUiController.*;
import com.originpixel.systemui.smartisland.SystemUiIntegrationBoundaries.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Regression & Integration Test Suite for SmartIsland SystemUI Modules (12 cases).
 */
public final class SmartIslandSystemUiIntegrationTest {

    public static void main(String[] args) {
        testCollapsedExpandedToggleAndGeometry();
        testSpringMotionVsReducedMotionAndAnimatorScale();
        testMotionPlanForSameStateContentUpdates();
        testReducedTransparencySurfaceFallback();
        testVerifiedBridgesEndToEndFlow();
        testSwipeDismissProtectsPrivacyIndicator();
        testFailClosedNotificationBridgeGate();
        testUnlockDoesNotAutoExposeSensitiveCollapsedNotification();
        testPriorityInterruptionAndRestorationDispatch();
        testEnvironmentScaleSanitization();
        testCallEventSinkInterruptionAndCancellation();
        testCallSourceGateFailsClosedWithoutProvenance();
        System.out.println("ALL SmartIslandSystemUiIntegrationTest CASES PASSED (12/12)");
    }

    private static void testCollapsedExpandedToggleAndGeometry() {
        SmartIslandContract contract = new SmartIslandContract();
        SmartIslandUiController controller = new SmartIslandUiController(contract);
        List<UiRenderModel> events = new ArrayList<>();
        controller.addListener(events::add);

        SystemUiBridgeCoordinator bridge = new SystemUiBridgeCoordinator(
                new SimulatedSystemUiAdapter(contract), controller);

        bridge.onMediaSessionUpdated(true, "Resonance", "HOME", 100L);
        UiRenderModel collapsed = events.get(events.size() - 1);
        assertEq(IslandState.COLLAPSED, collapsed.snapshot().state(), "Media starts collapsed");
        assertEq(168, collapsed.geometry().widthDp(), "Single collapsed pill width 168dp");
        assertEq(999, collapsed.geometry().cornerRadiusDp(), "Collapsed pill radius 999dp");

        controller.onPillTapped();
        UiRenderModel expanded = events.get(events.size() - 1);
        assertEq(IslandState.EXPANDED, expanded.snapshot().state(), "Tap expands pill");
        assertEq(348, expanded.geometry().widthDp(), "Expanded width 348dp");
        assertEq(24, expanded.geometry().cornerRadiusDp(), "Expanded card radius 24dp");
        assertEq(HapticCue.EXPAND_CONFIRM, expanded.motionPlan().hapticCue(), "Expand haptic cue");

        controller.onPillTapped();
        UiRenderModel back = events.get(events.size() - 1);
        assertEq(IslandState.COLLAPSED, back.snapshot().state(), "Second tap collapses pill");
        assertEq(HapticCue.COLLAPSE_LIGHT, back.motionPlan().hapticCue(), "Collapse haptic cue");
    }

    private static void testSpringMotionVsReducedMotionAndAnimatorScale() {
        MotionPlan normal = SmartIslandMotionSpec.computeTransition(
                IslandState.COLLAPSED, IslandState.EXPANDED, false, 1.0f, true);
        assertEq(true, normal.useSpring(), "Normal expand uses spring");
        assertEq(SpringPreset.FLUID, normal.springPreset(), "Expand uses FLUID spring");
        assertEq(true, normal.interruptible(), "Spring must be interruptible");

        MotionPlan reduced = SmartIslandMotionSpec.computeTransition(
                IslandState.COLLAPSED, IslandState.EXPANDED, true, 1.0f, false);
        assertEq(false, reduced.useSpring(), "Reduced motion disables spring");
        assertEq(120, reduced.fallbackDurationMs(), "Reduced motion uses 120ms crossfade");
        assertEq(HapticCue.NONE, reduced.hapticCue(), "Disabled haptics emits NONE");

        MotionPlan zeroScale = SmartIslandMotionSpec.computeTransition(
                IslandState.COLLAPSED, IslandState.EXPANDED, false, 0.0f, true);
        assertEq(false, zeroScale.useSpring(), "Zero animator scale disables spring");
        assertEq(0, zeroScale.fallbackDurationMs(), "Zero animator scale disables all transition duration");
        MotionPlan reducedZeroScale = SmartIslandMotionSpec.computeTransition(
                IslandState.COLLAPSED, IslandState.EXPANDED, true, 0.0f, true);
        assertEq(0, reducedZeroScale.fallbackDurationMs(), "Zero animator scale overrides reduced-motion fade");
        MotionPlan invalidScale = SmartIslandMotionSpec.computeTransition(
                IslandState.COLLAPSED, IslandState.EXPANDED, false, Float.NaN, true);
        assertEq(false, invalidScale.useSpring(), "Invalid scale fails closed without a spring");
        assertEq(0, invalidScale.fallbackDurationMs(), "Invalid scale fails closed with no motion");

        MotionPlan collapse = SmartIslandMotionSpec.computeTransition(
                IslandState.EXPANDED, IslandState.COLLAPSED, false, 1.0f, true);
        assertEq(SpringPreset.SNAPPY, collapse.springPreset(), "Collapse uses SNAPPY spring");

        MotionPlan dismiss = SmartIslandMotionSpec.computeTransition(
                IslandState.COLLAPSED, IslandState.HIDDEN, false, 1.0f, true);
        assertEq(SpringPreset.GENTLE, dismiss.springPreset(), "Dismiss uses GENTLE spring");
        assertEq(HapticCue.DISMISS_TICK, dismiss.hapticCue(), "Dismiss emits one tick when enabled");
    }

    private static void testMotionPlanForSameStateContentUpdates() {
        MotionPlan unchanged = SmartIslandMotionSpec.computeTransition(
                IslandState.COLLAPSED, IslandState.COLLAPSED, false, 1.0f, true);
        assertEq(false, unchanged.useSpring(), "Same-state content update must not animate");
        assertEq(null, unchanged.springPreset(), "Same-state update has no spring preset");
        assertEq(0, unchanged.fallbackDurationMs(), "Same-state update has zero duration");
        assertEq(HapticCue.NONE, unchanged.hapticCue(), "Same-state update must not emit a haptic");

        MotionPlan reduced = SmartIslandMotionSpec.computeTransition(
                IslandState.EXPANDED, IslandState.EXPANDED, true, 1.0f, true);
        assertEq(0, reduced.fallbackDurationMs(), "Same-state no-op takes precedence over reduced-motion fade");
    }

    private static void testReducedTransparencySurfaceFallback() {
        SmartIslandContract contract = new SmartIslandContract();
        SmartIslandUiController controller = new SmartIslandUiController(contract);
        SystemUiBridgeCoordinator bridge = new SystemUiBridgeCoordinator(
                new SimulatedSystemUiAdapter(contract), controller);

        bridge.onMediaSessionUpdated(true, "Track", "Artist", 100L);
        controller.updateEnvironment(false, true, true, 1.0f);
        UiRenderModel model = controller.dispatchCurrent();
        assertEq(0, model.geometry().blurRadiusDp(), "Reduced transparency sets blur to 0dp");
        assertEq(1.0f, model.geometry().alpha(), "Reduced transparency forces opaque alpha 1.0");
    }

    private static void testVerifiedBridgesEndToEndFlow() {
        SmartIslandContract contract = new SmartIslandContract();
        SmartIslandUiController controller = new SmartIslandUiController(contract);
        SystemUiBridgeCoordinator bridge = new SystemUiBridgeCoordinator(
                new SimulatedSystemUiAdapter(contract), controller);

        bridge.onNotificationPosted("otp", "Bank", "Code 8842", true, 100L);
        controller.onPillTapped(); // expand while unlocked
        assertEq("Code 8842", controller.dispatchCurrent().snapshot().displaySubtitle(), "Unlocked expanded shows OTP");

        bridge.onKeyguardShowingChanged(true);
        UiRenderModel locked = controller.dispatchCurrent();
        assertEq(IslandState.COLLAPSED, locked.snapshot().state(), "Keyguard lock collapses sensitive");
        assertEq("Sensitive content hidden", locked.snapshot().displaySubtitle(), "Keyguard lock redacts OTP");

        bridge.onUserSwitched();
        assertEq(IslandState.HIDDEN, controller.dispatchCurrent().snapshot().state(), "User switch clears island");
    }

    private static void testSwipeDismissProtectsPrivacyIndicator() {
        SmartIslandContract contract = new SmartIslandContract();
        SmartIslandUiController controller = new SmartIslandUiController(contract);
        SystemUiBridgeCoordinator bridge = new SystemUiBridgeCoordinator(
                new SimulatedSystemUiAdapter(contract), controller);

        bridge.onPrivacyItemsChanged(true, "Microphone", 100L);
        controller.onSwipeDismissPrimary();
        UiRenderModel afterSwipe = controller.dispatchCurrent();
        assertEq("privacy:Microphone", afterSwipe.snapshot().primaryActivity().id(),
                "Swipe dismiss must NOT dismiss active privacy indicator");
        assertEq(IslandState.EXPANDED, afterSwipe.snapshot().state(),
                "Active privacy indicator remains visible after swipe attempt");
    }

    private static void testFailClosedNotificationBridgeGate() {
        NotificationBridgeGate gate = NotificationBridgeGate.unverifiedDefault();
        assertEq(false, gate.isLiveBindingPermitted(), "Default gate must be fail-closed");
        boolean threw = false;
        try {
            gate.requireLiveBindingPermitted();
        } catch (IllegalStateException expected) {
            threw = expected.getMessage().contains("FAIL-CLOSED");
        }
        assertEq(true, threw, "requireLiveBindingPermitted must throw FAIL-CLOSED exception");
    }

    private static void testUnlockDoesNotAutoExposeSensitiveCollapsedNotification() {
        SmartIslandContract contract = new SmartIslandContract();
        SmartIslandUiController controller = new SmartIslandUiController(contract);
        SystemUiBridgeCoordinator bridge = new SystemUiBridgeCoordinator(
                new SimulatedSystemUiAdapter(contract), controller);

        bridge.onKeyguardShowingChanged(true);
        bridge.onNotificationPosted("sec1", "Vault", "Seed phrase", true, 100L);
        assertEq("Sensitive content hidden", controller.dispatchCurrent().snapshot().displaySubtitle(),
                "Redacted while locked");

        bridge.onKeyguardShowingChanged(false);
        assertEq(IslandState.COLLAPSED, controller.dispatchCurrent().snapshot().state(),
                "Unlocking must NOT auto-expand sensitive notification");
        assertEq("Sensitive content hidden", controller.dispatchCurrent().snapshot().displaySubtitle(),
                "Unlocking while collapsed keeps sensitive subtitle redacted");

        controller.onPillTapped();
        assertEq("Seed phrase", controller.dispatchCurrent().snapshot().displaySubtitle(),
                "Explicit tap when unlocked reveals sensitive subtitle");
    }

    private static void testPriorityInterruptionAndRestorationDispatch() {
        SmartIslandContract contract = new SmartIslandContract();
        SmartIslandUiController controller = new SmartIslandUiController(contract);
        SystemUiBridgeCoordinator bridge = new SystemUiBridgeCoordinator(
                new SimulatedSystemUiAdapter(contract), controller);

        bridge.onMediaSessionUpdated(true, "Track", "Artist", 100L);
        bridge.onNotificationPosted("note", "Chat", "Hello", false, 200L);
        contract.post(new IslandActivity("call", Priority.CALL, "Phone", "Incoming", false, 300L));
        UiRenderModel interrupted = controller.dispatchCurrent();
        assertEq("call", interrupted.snapshot().primaryActivity().id(), "Call wins over concurrent events");
        assertEq(IslandState.EXPANDED, interrupted.snapshot().state(), "High priority call interrupts");

        contract.dismiss("call");
        UiRenderModel restored = controller.dispatchCurrent();
        assertEq("media:active", restored.snapshot().primaryActivity().id(), "Media restores after call ends");
        assertEq(IslandState.COLLAPSED, restored.snapshot().state(), "Restored media returns to collapsed");
        assertEq(HapticCue.COLLAPSE_LIGHT, restored.motionPlan().hapticCue(), "Restoration uses collapse cue");
    }

    private static void testEnvironmentScaleSanitization() {
        SmartIslandContract contract = new SmartIslandContract();
        SmartIslandUiController controller = new SmartIslandUiController(contract);
        SystemUiBridgeCoordinator bridge = new SystemUiBridgeCoordinator(
                new SimulatedSystemUiAdapter(contract), controller);
        controller.updateEnvironment(false, false, true, Float.NaN);
        bridge.onMediaSessionUpdated(true, "Track", "Artist", 100L);
        MotionPlan motion = controller.dispatchCurrent().motionPlan();
        assertEq(0, motion.fallbackDurationMs(), "NaN animator scale is treated as animations disabled");
        assertEq(false, motion.useSpring(), "NaN animator scale cannot enable spring motion");
    }

    private static void testCallEventSinkInterruptionAndCancellation() {
        SmartIslandContract contract = new SmartIslandContract();
        SmartIslandUiController controller = new SmartIslandUiController(contract);
        SystemUiBridgeCoordinator bridge = new SystemUiBridgeCoordinator(
                new SimulatedSystemUiAdapter(contract), controller);
        bridge.onMediaSessionUpdated(true, "Track", "Artist", 100L);

        bridge.onCallEvent(new SimulatedSystemUiAdapter.CallEvent(
                SimulatedSystemUiAdapter.CallState.ACTIVE, 200L));
        UiRenderModel duringCall = controller.dispatchCurrent();
        assertEq("Call", duringCall.snapshot().displayTitle(), "Call sink exposes generic label");
        assertEq("Sensitive content hidden", duringCall.snapshot().displaySubtitle(),
                "Call sink does not expose caller information");
        assertEq("call:active", duringCall.snapshot().primaryActivity().id(), "Call sink takes priority");

        bridge.onCallEvent(new SimulatedSystemUiAdapter.CallEvent(
                SimulatedSystemUiAdapter.CallState.ENDED, 300L));
        UiRenderModel restored = controller.dispatchCurrent();
        assertEq("media:active", restored.snapshot().primaryActivity().id(), "Ended event restores media");
        assertEq(IslandState.COLLAPSED, restored.snapshot().state(), "Ended event collapses restored media");
    }

    private static void testCallSourceGateFailsClosedWithoutProvenance() {
        SystemUiIntegrationBoundaries.CallSourceGate gate =
                SystemUiIntegrationBoundaries.CallSourceGate.unverifiedDefault();
        assertEq(false, gate.isLiveBindingPermitted(), "Unverified call source must remain disabled");
        boolean rejected = false;
        try {
            gate.requireLiveBindingPermitted();
        } catch (IllegalStateException expected) {
            rejected = expected.getMessage().contains("source/signature provenance is missing");
        }
        assertEq(true, rejected, "Missing call API provenance must fail closed");
        assertEq(true, new SystemUiIntegrationBoundaries.CallSourceGate(true, "commit:path:signature")
                        .isLiveBindingPermitted(), "Recorded target provenance permits the adapter boundary");
    }

    private static void assertEq(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " | expected=" + expected + " actual=" + actual);
        }
    }
}
