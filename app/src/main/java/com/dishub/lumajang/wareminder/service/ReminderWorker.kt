package com.dishub.lumajang.wareminder.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dishub.lumajang.wareminder.data.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import kotlin.random.Random

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ReminderRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReminderWorker"
        private const val JEDA_MIN_MS = 10_000L
        private const val JEDA_MAKS_MS = 40_000L
    }

    override suspend fun doWork(): Result {
        if (!repository.isServiceRunning()) return Result.success()

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        // Only run during configured hours
        if (currentHour < repository.scheduleStartHour || currentHour > repository.scheduleEndHour) {
            return Result.success()
        }

        Log.d(TAG, "Starting reminder check at ${now.time}")

        return try {
            // Sync fresh data from Google Sheets
            val syncResult = repository.syncFromSheets()
            if (syncResult.isFailure) {
                Log.e(TAG, "Sync failed: ${syncResult.exceptionOrNull()?.message}")
            }

            val eligible = repository.getEligibleVehicles()
            Log.d(TAG, "Eligible vehicles: ${eligible.size}")

            val batch = eligible.take(repository.maxPerRun)
            if (batch.isEmpty()) {
                repository.lastCheckTime = System.currentTimeMillis()
                return Result.success()
            }

            for ((index, vehicle) in batch.withIndex()) {
                if (!repository.isServiceRunning()) break

                try {
                    // Launch WA intent
                    WaIntentSender(applicationContext).send(vehicle)
                    Thread.sleep(2000) // Wait for WA to open

                    // Try accessibility auto-send
                    if (WaAutoSendService.hasInstance()) {
                        WaAutoSendService.sendWithAutoClick {}
                    }

                    repository.markSent(vehicle)
                    Log.d(TAG, "Sent reminder to ${vehicle.nomorKendaraan} (${vehicle.nomorHP})")

                    // Random delay between messages
                    if (index < batch.size - 1) {
                        val delay = JEDA_MIN_MS + Random.nextLong(JEDA_MAKS_MS - JEDA_MIN_MS)
                        Thread.sleep(delay)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send to ${vehicle.nomorKendaraan}", e)
                    repository.markFailed(vehicle, e.message ?: "Unknown error")
                }
            }

            repository.lastCheckTime = System.currentTimeMillis()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.retry()
        }
    }
}
