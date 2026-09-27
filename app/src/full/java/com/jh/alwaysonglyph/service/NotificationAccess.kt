package com.jh.alwaysonglyph.service

import android.content.Context

object NotificationAccess {
    fun getUnreadCount(): Int = UnreadNotificationListenerService.getUnreadCount()

    fun isAccessGranted(context: Context): Boolean =
        UnreadNotificationListenerService.isNotificationAccessGranted(context)
}
