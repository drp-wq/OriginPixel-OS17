package com.originpixel.systemui.smartisland;

import com.originpixel.systemui.smartisland.SmartIslandContract.*;

/**
 * OriginPixel OS 17 — Simulated SystemUI Event Adapter (Framework-Agnostic).
 *
 * Translates simulated Android SystemUI callbacks into SmartIslandContract state
 * transitions. Does NOT import or patch unverified Android 17 SystemUI classes.
 */
public final class SimulatedSystemUiAdapter {

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

    public void onCallStateChanged(boolean active, String caller, long nowMs) {
        if (active) {
            contract.post(new IslandActivity("call:active", Priority.CALL, "Active Call", caller, false, nowMs));
        } else {
            contract.dismiss("call:active");
        }
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
