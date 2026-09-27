package com.jh.alwaysonglyph.service

import android.content.Context

object NotificationAccess {
    fun getUnreadCount(): Int = 0

    fun isAccessGranted(context: Context): Boolean = false
}
