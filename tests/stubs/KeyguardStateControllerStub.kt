package com.android.systemui.statusbar.policy

interface KeyguardStateController {
    val isShowing: Boolean
    fun addCallback(callback: Callback)
    fun removeCallback(callback: Callback)
    interface Callback {
        fun onKeyguardShowingChanged() {}
    }
}
