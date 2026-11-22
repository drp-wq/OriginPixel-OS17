package com.originpixel.systemui.smartisland;

import com.originpixel.systemui.smartisland.SmartIslandContract.*;

/**
 * OriginPixel OS 17 — Simulated SystemUI Event Adapter (Framework-Agnostic).
 *
 * Translates simulated Android SystemUI callbacks into SmartIslandContract state
 * transitions. Does NOT import or patch unverified Android 17 SystemUI classes.
 */
public final class SimulatedSystemUiAdapter {

    /** Call state exposed to the SmartIsland contract; deliberately contains no caller data. */
    public enum CallState {
        RINGING,
        ACTIVE,
        HELD,
        ENDED,
        CANCELED,
        DISCONNECTED
    }

    /** Framework-neutral call event suitable for tests until target telephony APIs are verified. */
    public record CallEvent(CallState state, long timestampMs) {
        public CallEvent {
            java.util.Objects.requireNonNull(state, "state");
        }
    }

    private final SmartIslandContract contract;

    public SimulatedSystemUiAdapter(SmartIslandContract contract) {
        this.contract = contract;
    }

    public void onNotificationPosted(String key, String appName, String text, boolean sensitive, long nowMs) {
        contract.post(new IslandActivity("notif:" + key, Priority.NOTIFICATION, appName, text, sensitive, nowMs));
    }

    public void onNotificationRemoved(String key) {
        contract.dismiss("notif:" + key);
    }

    public void onMediaStateChanged(boolean playing, String track, String artist, long nowMs) {
        if (playing) {
            contract.post(new IslandActivity("media:active", Priority.MEDIA, track, artist, false, nowMs));
        } else {
            contract.dismiss("media:active");
        }
    }

    /**
     * Calls outrank all other activities. RINGING, ACTIVE, and HELD remain represented
     * by one generic activity. Every terminal state dismisses it, restoring the next
     * ranked activity. No number, contact name, or call content enters the contract.
     */
    public void onCallEvent(CallEvent event) {
        java.util.Objects.requireNonNull(event, "event");
        switch (event.state()) {
            case RINGING, ACTIVE, HELD -> {
                contract.post(new IslandActivity(
                        "call:active", Priority.CALL, "Call", "Call in progress", true,
                        event.timestampMs()));
                // Keep the unverified call source at a privacy-safe collapsed surface.
                contract.setExpanded(false);
            }
            case ENDED, CANCELED, DISCONNECTED -> contract.dismiss("call:active");
        }
    }

    /** Compatibility helper for callers that only model a boolean active state. */
    public void onCallStateChanged(boolean active, String ignoredCaller, long nowMs) {
        onCallEvent(new CallEvent(active ? CallState.ACTIVE : CallState.ENDED, nowMs));
    }

    public void onPrivacyIndicatorChanged(boolean active, String sensorLabel, long nowMs) {
        if (active) {
            contract.post(new IslandActivity("privacy:" + sensorLabel, Priority.PRIVACY_INDICATOR, sensorLabel, "In use", false, nowMs));
        } else {
            contract.dismiss("privacy:" + sensorLabel);
        }
    }

    public void onKeyguardLockedChanged(boolean locked) {
        contract.setDeviceLocked(locked);
    }

    public void onScreenOff() {
        contract.onScreenOff();
    }

    public void onUserSwitched() {
        contract.onUserSwitched();
    }

    public RenderSnapshot snapshot() {
        return contract.snapshot();
    }
}
