package android.service.notification

import android.app.Notification

class StatusBarNotification(
    val packageName: String,
    val notification: Notification,
    val postTime: Long,
)
