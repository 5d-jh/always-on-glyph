package com.jh.alwaysonglyph.service

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jh.alwaysonglyph.renderer.StatusWidget
import com.jh.alwaysonglyph.renderer.WidgetPriority
import java.util.concurrent.atomic.AtomicInteger

class UnreadNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateUnreadCount()
        syncNotificationPriority()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateUnreadCount()
        if (getUnreadCount() > 0) {
            WidgetPriority.push(StatusWidget.NOTIFICATION)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateUnreadCount()
        if (getUnreadCount() == 0) {
            WidgetPriority.remove(StatusWidget.NOTIFICATION)
        }
    }

    private fun syncNotificationPriority() {
        if (getUnreadCount() > 0) {
            WidgetPriority.push(StatusWidget.NOTIFICATION)
        } else {
            WidgetPriority.remove(StatusWidget.NOTIFICATION)
        }
    }

    private fun updateUnreadCount() {
        try {
            val activeNotifs = activeNotifications ?: emptyArray()
            val count = activeNotifs.count { sbn ->
                val flags = sbn.notification?.flags ?: 0
                val isOngoing = (flags and Notification.FLAG_ONGOING_EVENT) != 0
                !sbn.isClearable.not() && !isOngoing
            }
            unreadCount.set(count)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val unreadCount = AtomicInteger(0)

        fun getUnreadCount(): Int = unreadCount.get()

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            val packageName = context.packageName
            return enabledListeners.contains(packageName)
        }
    }
}
