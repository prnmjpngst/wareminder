package com.dishub.lumajang.wareminder.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dishub.lumajang.wareminder.App
import com.dishub.lumajang.wareminder.MainActivity
import com.dishub.lumajang.wareminder.R
import com.dishub.lumajang.wareminder.data.repository.ReminderRepository
import com.dishub.lumajang.wareminder.server.WebServer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ReminderForegroundService : LifecycleService() {

    @Inject lateinit var repository: ReminderRepository
    @Inject lateinit var webServer: WebServer

    private val NOTIFICATION_ID = 1001

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> start()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        repository.setServiceRunning(true)

        startForeground(NOTIFICATION_ID, buildNotification("Memulai layanan..."))

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                webServer.start()
            }

            // Sync data on start
            withContext(Dispatchers.IO) {
                repository.syncFromSheets()
            }

            updateNotification("Layanan berjalan")

            // Schedule periodic work
            scheduleHourlyCheck()
        }
    }

    private fun scheduleHourlyCheck() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, ReminderForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, App.CHANNEL_SERVICE)
            .setContentTitle("WA Reminder Dishub")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        repository.setServiceRunning(false)
        webServer.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        const val ACTION_STOP = "com.dishub.lumajang.wareminder.STOP"
    }
}
