package com.originpixel.systemui.smartisland;

import com.originpixel.systemui.smartisland.SmartIslandContract.*;

/**
 * Zero-dependency unit & invariant test suite for SmartIslandContract (9 test suites).
 */
public final class SmartIslandContractTest {

    public static void main(String[] args) {
        testPriorityAndConcurrentSecondary();
        testInterruptionAndRestoration();
        testPrivacyRedactionAndLockScreen();
        testPrivacyIndicatorCannotBeDisabled();
        testAccessibilityEscalationAnnouncement();
        testReducedMotionAndDisablePolicy();
        testScreenOffAndUserSwitchLifecycle();
        testDismissAndClearLifecycle();
        testDeterministicEventSequenceWithInvariants();
        testSamePriorityTieBreakingAndUpdateInPlace();
        testPolicyTogglePreservesQueuedActivities();
        System.out.println("ALL SmartIslandContractTest CASES PASSED (11/11)");
    }

    private static void assertInvariants(RenderSnapshot snap, boolean deviceLocked) {
        if (snap.state() == IslandState.HIDDEN) {
            assertEq(null, snap.primaryActivity(), "INVARIANT: HIDDEN implies null primary");
            assertEq(null, snap.secondaryActivity(), "INVARIANT: HIDDEN implies null secondary");
            assertEq(0, snap.activeCount(), "INVARIANT: HIDDEN implies activeCount == 0");
        } else {
            if (snap.primaryActivity() == null) {
                throw new AssertionError("INVARIANT: visible state must have non-null primaryActivity");
            }
            if (snap.accessibilityDescription().isEmpty()) {
                throw new AssertionError("INVARIANT: visible state must have non-empty a11y description");
            }
        }
        if (snap.activeCount() >= 2) {
            if (snap.secondaryActivity() == null) {
                throw new AssertionError("INVARIANT: activeCount >= 2 must expose secondaryActivity");
            }
            if (snap.primaryActivity().priority().weight < snap.secondaryActivity().priority().weight) {
                throw new AssertionError("INVARIANT: primary weight must be >= secondary weight");
            }
        }
        if (deviceLocked && snap.primaryActivity() != null && snap.primaryActivity().sensitiveContent()) {
            if (snap.state() == IslandState.EXPANDED) {
                throw new AssertionError("INVARIANT: sensitive activity must never be EXPANDED while locked");
            }
            assertEq("Sensitive content hidden", snap.displaySubtitle(),
                    "INVARIANT: sensitive subtitle must be redacted while locked");
        }
    }

    private static void testDeterministicEventSequenceWithInvariants() {
        SmartIslandContract sm = new SmartIslandContract();
        boolean locked = false;

        // Step 1: Media playback starts
        sm.post(new IslandActivity("m1", Priority.MEDIA, "Synthwave", "Track 1", false, 1000L));
        RenderSnapshot s1 = sm.snapshot();
        assertInvariants(s1, locked);
        assertEq("m1", s1.primaryActivity().id(), "Seq 1: media primary");

        // Step 2: Sensitive notification arrives concurrently
        sm.post(new IslandActivity("n1", Priority.NOTIFICATION, "Messages", "Code 9921", true, 2000L));
        RenderSnapshot s2 = sm.snapshot();
        assertInvariants(s2, locked);
        assertEq("m1", s2.primaryActivity().id(), "Seq 2: media stays primary");
        assertEq("n1", s2.secondaryActivity().id(), "Seq 2: notification is secondary");

        // Step 3: Incoming call interrupts media
        sm.post(new IslandActivity("c1", Priority.CALL, "Incoming Call", "Bob", false, 3000L));
        RenderSnapshot s3 = sm.snapshot();
        assertInvariants(s3, locked);
        assertEq("c1", s3.primaryActivity().id(), "Seq 3: call interrupts");
        assertEq(IslandState.EXPANDED, s3.state(), "Seq 3: call expands");

        // Step 4: Notification removed while call active
        sm.dismiss("n1");
        RenderSnapshot s4 = sm.snapshot();
        assertInvariants(s4, locked);
        assertEq(2, s4.activeCount(), "Seq 4: call + media remain");

        // Step 5: Call ends -> media cleanly restored & collapsed
        sm.dismiss("c1");
        RenderSnapshot s5 = sm.snapshot();
        assertInvariants(s5, locked);
        assertEq("m1", s5.primaryActivity().id(), "Seq 5: media restored");
        assertEq(IslandState.COLLAPSED, s5.state(), "Seq 5: restored media collapsed");

        // Step 6: Device locks + sensitive notification posted while media dismissed
        sm.dismiss("m1");
        locked = true;
        sm.setDeviceLocked(true);
        sm.post(new IslandActivity("n2", Priority.NOTIFICATION, "Banking", "Transfer $500", true, 4000L));
        RenderSnapshot s6 = sm.snapshot();
        assertInvariants(s6, locked);
        assertEq(IslandState.COLLAPSED, s6.state(), "Seq 6: locked sensitive stays collapsed");

        // Step 7: Attempt to expand sensitive notification while locked -> blocked
        sm.setExpanded(true);
        RenderSnapshot s7 = sm.snapshot();
        assertInvariants(s7, locked);
        assertEq(IslandState.COLLAPSED, s7.state(), "Seq 7: locked expand blocked");

        // Step 8: Screen off -> remains collapsed
        sm.onScreenOff();
        assertInvariants(sm.snapshot(), locked);

        // Step 9: User switch -> all user-scoped activities cleared
        sm.onUserSwitched();
        RenderSnapshot s9 = sm.snapshot();
        assertInvariants(s9, locked);
        assertEq(IslandState.HIDDEN, s9.state(), "Seq 9: user switch clears island");
    }

    private static void testPriorityAndConcurrentSecondary() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.post(new IslandActivity("media-1", Priority.MEDIA, "Track A", "Artist", false, 100L));
        sm.post(new IslandActivity("notif-1", Priority.NOTIFICATION, "Chat", "Hello", true, 200L));
        RenderSnapshot snap = sm.snapshot();
        assertInvariants(snap, false);
        assertEq("media-1", snap.primaryActivity().id(), "MEDIA must beat NOTIFICATION");
        assertEq("notif-1", snap.secondaryActivity().id(), "NOTIFICATION must be secondary pill");
        assertEq(2, snap.activeCount(), "Active count must be 2");
    }

    private static void testInterruptionAndRestoration() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.post(new IslandActivity("media-1", Priority.MEDIA, "Podcast", "Ep 1", false, 100L));
        assertEq(IslandState.COLLAPSED, sm.snapshot().state(), "MEDIA starts COLLAPSED");

        sm.post(new IslandActivity("call-1", Priority.CALL, "Incoming Call", "Alice", false, 200L));
        RenderSnapshot duringCall = sm.snapshot();
        assertInvariants(duringCall, false);
        assertEq("call-1", duringCall.primaryActivity().id(), "CALL interrupts MEDIA");
        assertEq("media-1", duringCall.secondaryActivity().id(), "MEDIA becomes secondary");
        assertEq(IslandState.EXPANDED, duringCall.state(), "CALL auto-expands");

        sm.dismiss("call-1");
        RenderSnapshot afterCall = sm.snapshot();
        assertInvariants(afterCall, false);
        assertEq("media-1", afterCall.primaryActivity().id(), "MEDIA restored after CALL dismiss");
        assertEq(null, afterCall.secondaryActivity(), "No secondary activity left");
        assertEq(IslandState.COLLAPSED, afterCall.state(), "Restored MEDIA collapses cleanly");
    }

    private static void testPrivacyRedactionAndLockScreen() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.post(new IslandActivity("n1", Priority.NOTIFICATION, "Bank Alert", "OTP: 123456", true, 100L));
        assertEq("Sensitive content hidden", sm.snapshot().displaySubtitle(), "Redacted when collapsed");

        sm.setExpanded(true);
        assertEq("OTP: 123456", sm.snapshot().displaySubtitle(), "Visible when unlocked & expanded");

        sm.setDeviceLocked(true);
        RenderSnapshot locked = sm.snapshot();
        assertInvariants(locked, true);
        assertEq(IslandState.COLLAPSED, locked.state(), "Locking collapses sensitive expanded activity");
        assertEq("Sensitive content hidden", locked.displaySubtitle(), "Redacted when locked");

        sm.setExpanded(true);
        assertInvariants(sm.snapshot(), true);
        assertEq(IslandState.COLLAPSED, sm.snapshot().state(), "Cannot expand sensitive notification while locked");
    }

    private static void testPrivacyIndicatorCannotBeDisabled() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.updatePolicy(new IslandPolicy(false, false, true, true));
        sm.post(new IslandActivity("mic-1", Priority.PRIVACY_INDICATOR, "Microphone", "In use", false, 100L));
        RenderSnapshot snap = sm.snapshot();
        assertInvariants(snap, false);
        assertEq(IslandState.COLLAPSED, snap.state(), "Privacy indicator must remain visible even if island disabled");
        assertEq("mic-1", snap.primaryActivity().id(), "Privacy indicator must be primary");
    }

    private static void testAccessibilityEscalationAnnouncement() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.post(new IslandActivity("t1", Priority.TIMER, "Timer", "00:10", false, 100L));
        RenderSnapshot first = sm.snapshot();
        assertInvariants(first, false);
        assertEq(true, first.announceForAccessibility(), "High-priority escalation triggers a11y announcement");
        assertEq("Timer: 00:10", first.accessibilityDescription(), "A11y description formatted");
        assertEq(false, sm.snapshot().announceForAccessibility(), "Announcement consumed once");
    }

    private static void testReducedMotionAndDisablePolicy() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.updatePolicy(new IslandPolicy(true, true, true, false));
        sm.post(new IslandActivity("t1", Priority.TIMER, "Timer", "00:30", false, 100L));
        assertEq(TransitionStyle.REDUCED_CROSSFADE, sm.snapshot().transitionStyle(), "Reduced motion uses crossfade");
        assertEq(IslandState.COLLAPSED, sm.snapshot().state(), "autoExpandHighPriority=false keeps COLLAPSED");

        sm.updatePolicy(new IslandPolicy(false, true, true, false));
        assertInvariants(sm.snapshot(), false);
        assertEq(IslandState.HIDDEN, sm.snapshot().state(), "Disabled policy hides non-privacy island");
    }

    private static void testScreenOffAndUserSwitchLifecycle() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.post(new IslandActivity("t1", Priority.TIMER, "Timer", "01:00", false, 100L));
        assertEq(IslandState.EXPANDED, sm.snapshot().state(), "Timer auto-expands");
        sm.onScreenOff();
        assertEq(IslandState.COLLAPSED, sm.snapshot().state(), "Screen-off collapses expanded island for AOD");
        sm.onUserSwitched();
        assertInvariants(sm.snapshot(), false);
        assertEq(IslandState.HIDDEN, sm.snapshot().state(), "User switch clears user-scoped activities");
        assertEq(0, sm.snapshot().activeCount(), "Active count 0 after user switch");
    }

    private static void testDismissAndClearLifecycle() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.post(new IslandActivity("c1", Priority.CHARGING, "Charging", "85%", false, 100L));
        sm.dismiss("c1");
        assertInvariants(sm.snapshot(), false);
        assertEq(IslandState.HIDDEN, sm.snapshot().state(), "Dismissing last activity hides island");
        sm.post(new IslandActivity("c2", Priority.CHARGING, "Charging", "90%", false, 200L));
        sm.clearAll();
        assertInvariants(sm.snapshot(), false);
        assertEq(IslandState.HIDDEN, sm.snapshot().state(), "clearAll hides island");
    }

    private static void testSamePriorityTieBreakingAndUpdateInPlace() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.post(new IslandActivity("n1", Priority.NOTIFICATION, "App A", "First", false, 100L));
        sm.post(new IslandActivity("n2", Priority.NOTIFICATION, "App B", "Second", false, 200L));
        RenderSnapshot snap = sm.snapshot();
        assertInvariants(snap, false);
        assertEq("n2", snap.primaryActivity().id(), "Newer timestamp wins same-priority tie");
        assertEq("n1", snap.secondaryActivity().id(), "Older activity is secondary");

        // In-place update of n1 with newer timestamp promotes n1 without duplicating count
        sm.post(new IslandActivity("n1", Priority.NOTIFICATION, "App A", "Updated", false, 300L));
        RenderSnapshot updated = sm.snapshot();
        assertInvariants(updated, false);
        assertEq(2, updated.activeCount(), "In-place update must not duplicate activeCount");
        assertEq("n1", updated.primaryActivity().id(), "Updated n1 promoted to primary");
        assertEq("Updated", updated.displaySubtitle(), "Updated subtitle reflected");
    }

    private static void testPolicyTogglePreservesQueuedActivities() {
        SmartIslandContract sm = new SmartIslandContract();
        sm.post(new IslandActivity("m1", Priority.MEDIA, "Track", "Artist", false, 100L));
        sm.updatePolicy(new IslandPolicy(false, false, true, true));
        RenderSnapshot disabled = sm.snapshot();
        assertInvariants(disabled, false);
        assertEq(IslandState.HIDDEN, disabled.state(), "Disabled island is HIDDEN");

        sm.updatePolicy(new IslandPolicy(true, false, true, true));
        RenderSnapshot reenabled = sm.snapshot();
        assertInvariants(reenabled, false);
        assertEq(IslandState.COLLAPSED, reenabled.state(), "Re-enabling restores COLLAPSED state");
        assertEq("m1", reenabled.primaryActivity().id(), "Queued media activity preserved");
    }

    private static void assertEq(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " | expected=" + expected + " actual=" + actual);
        }
    }
}
