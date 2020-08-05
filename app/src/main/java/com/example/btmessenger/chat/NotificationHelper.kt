package com.example.btmessenger.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

fun createNotificationChannel(context: Context, importance: Int, showBadge: Boolean, name: String, description: String) {
    // 1
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        // 2
        val channelId = "${context.packageName}-$name"
        val channel = NotificationChannel(channelId, name, importance)
        channel.description = description
        channel.setShowBadge(showBadge)

        // 3
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}