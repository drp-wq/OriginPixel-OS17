package com.android.systemui.originpixel.smartisland

import com.android.systemui.media.controls.domain.pipeline.MediaDataManager
import com.android.systemui.privacy.PrivacyItem
import com.android.systemui.privacy.PrivacyItemController
import com.android.systemui.statusbar.notification.collection.NotifPipeline
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener
import com.android.systemui.statusbar.policy.KeyguardStateController
import com.originpixel.systemui.smartisland.SimulatedSystemUiAdapter
import com.originpixel.systemui.smartisland.SmartIslandContract
import com.originpixel.systemui.smartisland.SmartIslandUiController
import com.originpixel.systemui.smartisland.SystemUiIntegrationBoundaries.SystemUiBridgeCoordinator

/**
 * OriginPixel OS 17 — Minimal LineageOS 24.0 SystemUI Wiring Adapter.
 *
 * Wired strictly against verified LineageOS/android_frameworks_base@lineage-24.0 classes:
 *  - NotifPipeline.kt (com.android.systemui.statusbar.notification.collection.NotifPipeline)
 *  - MediaDataManager.kt (com.android.systemui.media.controls.domain.pipeline.MediaDataManager)
 *  - PrivacyItemController.kt (com.android.systemui.privacy.PrivacyItemController)
 *  - KeyguardStateController.java (com.android.systemui.statusbar.policy.KeyguardStateController)
 *
 * NOTE: Compiled ONLY inside a full LineageOS 24.0 Soong build (Gate G3 external build host).
 * L1 RRO overlayable.xml remains UNVERIFIED and fail-closed disabled.
 */
class SmartIslandSystemUiBridge(
    private val notifPipeline: NotifPipeline,
    private val mediaDataManager: MediaDataManager,
    private val privacyItemController: PrivacyItemController,
    private val keyguardStateController: KeyguardStateController,
) {
    val contract = SmartIslandContract()
    val uiController = SmartIslandUiController(contract)
    private val coordinator = SystemUiBridgeCoordinator(
        SimulatedSystemUiAdapter(contract),
        uiController,
    )

    private val notifListener = object : NotifCollectionListener {
        override fun onEntryAdded(entry: NotificationEntry) {
            val sbn = entry.sbn
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: sbn.packageName
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
            val sensitive = sbn.notification.visibility == android.app.Notification.VISIBILITY_PRIVATE
            coordinator.onNotificationPosted(entry.key, title, text, sensitive, sbn.postTime)
        }

        override fun onEntryRemoved(entry: NotificationEntry, reason: Int) {
            coordinator.onNotificationRemoved(entry.key)
        }
    }

    private val privacyCallback = object : PrivacyItemController.Callback {
        override fun onPrivacyItemsChanged(privacyItems: List<PrivacyItem>) {
            val now = System.currentTimeMillis()
            val activeLabels = privacyItems.map { it.privacyType.name }.toSet()
            listOf("TYPE_CAMERA", "TYPE_MICROPHONE", "TYPE_LOCATION").forEach { label ->
                coordinator.onPrivacyItemsChanged(activeLabels.contains(label), label, now)
            }
        }
    }

    private val keyguardCallback = object : KeyguardStateController.Callback {
        override fun onKeyguardShowingChanged() {
            coordinator.onKeyguardShowingChanged(keyguardStateController.isShowing)
        }
    }

    fun start() {
        notifPipeline.addCollectionListener(notifListener)
        privacyItemController.addCallback(privacyCallback)
        keyguardStateController.addCallback(keyguardCallback)
        coordinator.onKeyguardShowingChanged(keyguardStateController.isShowing)
    }

    fun stop() {
        privacyItemController.removeCallback(privacyCallback)
        keyguardStateController.removeCallback(keyguardCallback)
    }

    fun onGoingToSleep() = coordinator.onGoingToSleep()

    fun onUserSwitched() = coordinator.onUserSwitched()
}
