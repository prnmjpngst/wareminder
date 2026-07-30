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

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ReminderRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReminderWorker"
    }

    override suspend fun doWork(): Result {
        if (!repository.isServiceRunning()) return Result.success()

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        if (currentHour < repository.scheduleStartHour || currentHour > repository.scheduleEndHour) {
            return Result.success()
        }

        Log.d(TAG, "Starting reminder check at ${now.time}")

        return try {
            val syncResult = repository.syncFromAppScript()
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

            val intentSender = WaIntentSender(applicationContext)

            for ((index, vehicle) in batch.withIndex()) {
                if (!repository.isServiceRunning()) break

                try {
                    val message = repository.buildMessage(vehicle)
                    intentSender.sendOpenChat(vehicle)
                    Thread.sleep(3000)

                    if (WaAutoSendService.hasInstance()) {
                        WaAutoSendService.sendWithTyping(message)
                    }

                    Thread.sleep((message.length * 150L).coerceAtLeast(5000))

                    repository.markSent(vehicle)
                    Log.d(TAG, "Sent reminder to ${vehicle.noPolisi}")

                    if (index < batch.size - 1) {
                        Thread.sleep(30_000L)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send to ${vehicle.noPolisi}", e)
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
