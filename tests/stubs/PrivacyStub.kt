package com.android.systemui.privacy

enum class PrivacyType { TYPE_CAMERA, TYPE_MICROPHONE, TYPE_LOCATION }

data class PrivacyItem(val privacyType: PrivacyType)

interface PrivacyItemController {
    fun addCallback(callback: Callback)
    fun removeCallback(callback: Callback)
    interface Callback {
        fun onPrivacyItemsChanged(privacyItems: List<PrivacyItem>)
    }
}
