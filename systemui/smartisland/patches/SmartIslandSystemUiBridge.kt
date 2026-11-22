package com.android.systemui.originpixel.smartisland

import android.app.Notification
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.media.controls.domain.pipeline.MediaDataManager
import com.android.systemui.media.controls.shared.model.MediaData
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
import javax.inject.Inject

/**
 * OriginPixel OS 17 — Hardened LineageOS 24.0 SystemUI Wiring Bridge.
 *
 * Verified upstream lineage-24.0 signatures (2026-11-22):
 *  - NotifPipeline.kt: addCollectionListener / removeCollectionListener
 *  - NotifCollectionListener.java: onEntryAdded / onEntryUpdated / onEntryRemoved
 *  - MediaDataManager.kt: addListener / removeListener / Listener.onMediaDataLoaded / onMediaDataRemoved
 *  - PrivacyItemController.kt: addCallback / removeCallback / Callback.onPrivacyItemsChanged(List<PrivacyItem>)
 *  - KeyguardStateController.java: addCallback / removeCallback / Callback.onKeyguardShowingChanged()
 *
 * Compiled ONLY inside a full LineageOS 24.0 Soong build (Gate G3 external build host).
 */
@SysUISingleton
class SmartIslandSystemUiBridge @Inject constructor(
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
    private var started = false

    private val notifListener = object : NotifCollectionListener {
        override fun onEntryAdded(entry: NotificationEntry) = postEntry(entry)

        override fun onEntryUpdated(entry: NotificationEntry) = postEntry(entry)

        override fun onEntryRemoved(entry: NotificationEntry, reason: Int) {
            coordinator.onNotificationRemoved(entry.key)
        }
    }

    private val mediaListener = object : MediaDataManager.Listener {
        override fun onMediaDataLoaded(
            key: String,
            oldKey: String?,
            data: MediaData,
            immediately: Boolean,
            receivedSmartspaceCardLatency: Int,
            isSsReactivated: Boolean,
        ) {
            val playing = data.isPlaying == true
            val title = data.song?.toString() ?: ""
            val artist = data.artist?.toString() ?: ""
            coordinator.onMediaSessionUpdated(playing, title, artist, System.currentTimeMillis())
        }

        override fun onMediaDataRemoved(key: String, userInitiated: Boolean) {
            coordinator.onMediaSessionUpdated(false, "", "", System.currentTimeMillis())
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

    private fun postEntry(entry: NotificationEntry) {
        val sbn = entry.sbn
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: sbn.packageName
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val vis = sbn.notification.visibility
        val sensitive = vis == Notification.VISIBILITY_PRIVATE || vis == Notification.VISIBILITY_SECRET
        coordinator.onNotificationPosted(entry.key, title, text, sensitive, sbn.postTime)
    }

    fun start() {
        if (started) return
        started = true
        notifPipeline.addCollectionListener(notifListener)
        mediaDataManager.addListener(mediaListener)
        privacyItemController.addCallback(privacyCallback)
        keyguardStateController.addCallback(keyguardCallback)
        coordinator.onKeyguardShowingChanged(keyguardStateController.isShowing)
    }

    fun stop() {
        if (!started) return
        started = false
        notifPipeline.removeCollectionListener(notifListener)
        mediaDataManager.removeListener(mediaListener)
        privacyItemController.removeCallback(privacyCallback)
        keyguardStateController.removeCallback(keyguardCallback)
    }

    fun onGoingToSleep() = coordinator.onGoingToSleep()

    fun onUserSwitched() = coordinator.onUserSwitched()
}
