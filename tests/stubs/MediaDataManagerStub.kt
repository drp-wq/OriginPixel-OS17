package com.android.systemui.media.controls.domain.pipeline

import com.android.systemui.media.controls.shared.model.MediaData

interface MediaDataManager {
    fun addListener(listener: Listener) {}
    fun removeListener(listener: Listener) {}
    interface Listener {
        fun onMediaDataLoaded(
            key: String,
            oldKey: String?,
            data: MediaData,
            immediately: Boolean,
            receivedSmartspaceCardLatency: Int,
            isSsReactivated: Boolean,
        ) {}
        fun onMediaDataRemoved(key: String, userInitiated: Boolean) {}
    }
}
