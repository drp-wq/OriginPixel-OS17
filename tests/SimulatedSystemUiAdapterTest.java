package com.originpixel.systemui.smartisland;

import com.originpixel.systemui.smartisland.SmartIslandContract.*;

/**
 * Unit tests for SimulatedSystemUiAdapter (5 test cases).
 * Validates simulated SystemUI event translation without AOSP SystemUI patches.
 */
public final class SimulatedSystemUiAdapterTest {

    public static void main(String[] args) {
        testMediaAndCallInterruptionFlow();
        testNotificationPostAndRemoveFlow();
        testKeyguardLockRedactsSensitiveNotification();
        testPrivacyIndicatorSurvivesDisabledIsland();
        testScreenOffAndUserSwitchReset();
        System.out.println("ALL SimulatedSystemUiAdapterTest CASES PASSED (5/5)");
    }

    private static void testMediaAndCallInterruptionFlow() {
        SmartIslandContract sm = new SmartIslandContract();
        SimulatedSystemUiAdapter adapter = new SimulatedSystemUiAdapter(sm);

        adapter.onMediaStateChanged(true, "Nightcall", "Kavinsky", 100L);
        assertEq("media:active", adapter.snapshot().primaryActivity().id(), "Media active");

        adapter.onCallStateChanged(true, "Mom", 200L);
        assertEq("call:active", adapter.snapshot().primaryActivity().id(), "Call interrupts media");
        assertEq(IslandState.EXPANDED, adapter.snapshot().state(), "Call expands island");

        adapter.onCallStateChanged(false, "", 300L);
        assertEq("media:active", adapter.snapshot().primaryActivity().id(), "Media restored after call");
        assertEq(IslandState.COLLAPSED, adapter.snapshot().state(), "Media collapsed after call");

        adapter.onMediaStateChanged(false, "", "", 400L);
        assertEq(IslandState.HIDDEN, adapter.snapshot().state(), "Media stop hides island");
    }

    private static void testNotificationPostAndRemoveFlow() {
        SmartIslandContract sm = new SmartIslandContract();
        SimulatedSystemUiAdapter adapter = new SimulatedSystemUiAdapter(sm);

        adapter.onNotificationPosted("k1", "Signal", "Meet at 5", false, 100L);
        assertEq("notif:k1", adapter.snapshot().primaryActivity().id(), "Notification posted");
        adapter.onNotificationRemoved("k1");
        assertEq(IslandState.HIDDEN, adapter.snapshot().state(), "Notification removal hides island");
    }

    private static void testKeyguardLockRedactsSensitiveNotification() {
        SmartIslandContract sm = new SmartIslandContract();
        SimulatedSystemUiAdapter adapter = new SimulatedSystemUiAdapter(sm);

        adapter.onNotificationPosted("otp", "Auth", "123-456", true, 100L);
        sm.setExpanded(true);
        assertEq("123-456", adapter.snapshot().displaySubtitle(), "Unlocked expanded shows OTP");

        adapter.onKeyguardLockedChanged(true);
        assertEq(IslandState.COLLAPSED, adapter.snapshot().state(), "Keyguard lock collapses sensitive");
        assertEq("Sensitive content hidden", adapter.snapshot().displaySubtitle(), "Keyguard lock redacts OTP");
    }

    private static void testPrivacyIndicatorSurvivesDisabledIsland() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.updatePolicy(new IslandPolicy(false, false, true, true));
        SimulatedSystemUiAdapter adapter = new SimulatedSystemUiAdapter(sm);

        adapter.onPrivacyIndicatorChanged(true, "Camera", 100L);
        assertEq(IslandState.COLLAPSED, adapter.snapshot().state(), "Camera indicator visible when disabled");
        adapter.onPrivacyIndicatorChanged(false, "Camera", 200L);
        assertEq(IslandState.HIDDEN, adapter.snapshot().state(), "Camera indicator cleared");
    }

    private static void testScreenOffAndUserSwitchReset() {
        SmartIslandContract sm = new SmartIslandContract();
        SimulatedSystemUiAdapter adapter = new SimulatedSystemUiAdapter(sm);

        adapter.onCallStateChanged(true, "Work", 100L);
        adapter.onScreenOff();
        assertEq(IslandState.COLLAPSED, adapter.snapshot().state(), "Screen off collapses for AOD");
        adapter.onUserSwitched();
        assertEq(IslandState.HIDDEN, adapter.snapshot().state(), "User switch clears state");
    }

    private static void assertEq(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " | expected=" + expected + " actual=" + actual);
        }
    }
}
