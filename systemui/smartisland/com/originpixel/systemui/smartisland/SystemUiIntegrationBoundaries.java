package com.originpixel.systemui.smartisland;

import java.util.Objects;

/**
 * OriginPixel OS 17 — SystemUI Integration Boundaries (LineageOS 24.0).
 *
 * Verified upstream classes (HTTP 200 on LineageOS/android_frameworks_base@lineage-24.0):
 *  - MediaDataManager.kt
 *  - PrivacyItemController.kt
 *  - KeyguardStateController.java
 *  - CentralSurfacesImpl.java
 *
 * Fail-closed blocked upstream targets (HTTP 404 on lineage-24.0):
 *  - NotifPipeline.java (NotificationBridgeGate refuses live SystemUI binding until G3 tree verified)
 *  - SystemUI/res/values/overlayable.xml (RRO binding remains disabled)
 */
public final class SystemUiIntegrationBoundaries {

    public interface MediaDataBridge {
        void onMediaSessionUpdated(boolean playing, String title, String artist, long timestampMs);
    }

    public interface PrivacyIndicatorBridge {
        void onPrivacyItemsChanged(boolean active, String sensorLabel, long timestampMs);
    }

    public interface KeyguardStateBridge {
        void onKeyguardShowingChanged(boolean locked);
    }

    public interface CentralSurfacesLifecycleBridge {
        void onGoingToSleep();
        void onUserSwitched();
    }

    public interface NotificationEventSink {
        void onNotificationPosted(String key, String appName, String text, boolean sensitive, long timestampMs);
        void onNotificationRemoved(String key);
    }

    /**
     * Fail-closed gate for Android 17 notification pipeline binding.
     * Prevents wiring to unverified NotifPipeline signatures until G3 source sync passes.
     */
    public static final class NotificationBridgeGate {
        private final boolean verifiedInSyncedTree;

        public NotificationBridgeGate(boolean verifiedInSyncedTree) {
            this.verifiedInSyncedTree = verifiedInSyncedTree;
        }

        public static NotificationBridgeGate unverifiedDefault() {
            return new NotificationBridgeGate(false);
        }

        public boolean isLiveBindingPermitted() {
            return verifiedInSyncedTree;
        }

        public void requireLiveBindingPermitted() {
            if (!verifiedInSyncedTree) {
                throw new IllegalStateException(
                        "FAIL-CLOSED: NotifPipeline class path is UNVERIFIED on lineage-24.0 (HTTP 404). "
                                + "Sync full tree in Gate G3 before binding live SystemUI notifications."
                );
            }
        }
    }

    /**
     * Concrete coordinator wiring verified bridges into SimulatedSystemUiAdapter + SmartIslandUiController.
     */
    public static final class SystemUiBridgeCoordinator
            implements MediaDataBridge, PrivacyIndicatorBridge, KeyguardStateBridge,
            CentralSurfacesLifecycleBridge, NotificationEventSink {

        private final SimulatedSystemUiAdapter adapter;
        private final SmartIslandUiController controller;

        public SystemUiBridgeCoordinator(
                SimulatedSystemUiAdapter adapter,
                SmartIslandUiController controller
        ) {
            this.adapter = Objects.requireNonNull(adapter, "adapter");
            this.controller = Objects.requireNonNull(controller, "controller");
        }

        @Override
        public void onMediaSessionUpdated(boolean playing, String title, String artist, long timestampMs) {
            adapter.onMediaStateChanged(playing, title, artist, timestampMs);
            controller.dispatchCurrent();
        }

        @Override
        public void onPrivacyItemsChanged(boolean active, String sensorLabel, long timestampMs) {
            adapter.onPrivacyIndicatorChanged(active, sensorLabel, timestampMs);
            controller.dispatchCurrent();
        }

        @Override
        public void onKeyguardShowingChanged(boolean locked) {
            adapter.onKeyguardLockedChanged(locked);
            controller.dispatchCurrent();
        }

        @Override
        public void onGoingToSleep() {
            adapter.onScreenOff();
            controller.dispatchCurrent();
        }

        @Override
        public void onUserSwitched() {
            adapter.onUserSwitched();
            controller.dispatchCurrent();
        }

        @Override
        public void onNotificationPosted(String key, String appName, String text, boolean sensitive, long timestampMs) {
            adapter.onNotificationPosted(key, appName, text, sensitive, timestampMs);
            controller.dispatchCurrent();
        }

        @Override
        public void onNotificationRemoved(String key) {
            adapter.onNotificationRemoved(key);
            controller.dispatchCurrent();
        }
    }

    private SystemUiIntegrationBoundaries() {}
}
