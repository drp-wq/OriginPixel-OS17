package com.android.systemui.statusbar.notification.collection

import android.service.notification.StatusBarNotification
import com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener

class NotificationEntry(val key: String, val sbn: StatusBarNotification)

interface NotifPipeline {
    fun addCollectionListener(listener: NotifCollectionListener)
    fun removeCollectionListener(listener: NotifCollectionListener)
}
