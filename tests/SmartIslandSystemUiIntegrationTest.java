package com.originpixel.systemui.smartisland;

import com.originpixel.systemui.smartisland.SmartIslandContract.*;
import com.originpixel.systemui.smartisland.SmartIslandMotionSpec.*;
import com.originpixel.systemui.smartisland.SmartIslandUiController.*;
import com.originpixel.systemui.smartisland.SystemUiIntegrationBoundaries.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Regression & Integration Test Suite for SmartIsland SystemUI Modules (6 test suites).
 */
public final class SmartIslandSystemUiIntegrationTest {

    public static void main(String[] args) {
        testCollapsedExpandedToggleAndGeometry();
        testSpringMotionVsReducedMotionAndAnimatorScale();
        testReducedTransparencySurfaceFallback();
        testVerifiedBridgesEndToEndFlow();
        testSwipeDismissProtectsPrivacyIndicator();
        testFailClosedNotificationBridgeGate();
        testUnlockDoesNotAutoExposeSensitiveCollapsedNotification();
        System.out.println("ALL SmartIslandSystemUiIntegrationTest CASES PASSED (7/7)");
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

    private static void assertEq(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " | expected=" + expected + " actual=" + actual);
        }
    }
}
