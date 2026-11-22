package com.android.systemui.statusbar.notification.collection.notifcollection

import com.android.systemui.statusbar.notification.collection.NotificationEntry

interface NotifCollectionListener {
    fun onEntryAdded(entry: NotificationEntry) {}
    fun onEntryUpdated(entry: NotificationEntry) {}
    fun onEntryRemoved(entry: NotificationEntry, reason: Int) {}
}
