package com.dishub.lumajang.wareminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    companion object {
        const val CHANNEL_SERVICE = "service_channel"
        const val CHANNEL_LOG = "log_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Layanan Background",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi layanan background reminder"
            }

            val logChannel = NotificationChannel(
                CHANNEL_LOG,
                "Log Pengiriman",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi hasil pengiriman pesan"
            }

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(logChannel)
        }
    }
}
