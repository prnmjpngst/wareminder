package com.dishub.lumajang.wareminder.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.dishub.lumajang.wareminder.data.db.LogDao
import com.dishub.lumajang.wareminder.data.db.SendLog
import com.dishub.lumajang.wareminder.data.sheets.AppsScriptApi
import com.dishub.lumajang.wareminder.data.sheets.Vehicle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appsScriptApi: AppsScriptApi,
    private val logDao: LogDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    var appsScriptUrl: String
        get() = prefs.getString(SCRIPT_URL, DEFAULT_GAS_URL) ?: DEFAULT_GAS_URL
        set(value) = prefs.edit().putString(SCRIPT_URL, value).apply()

    var windowStartDay: Int
        get() = prefs.getInt(WINDOW_START, 3)
        set(value) = prefs.edit().putInt(WINDOW_START, value).apply()

    var windowEndDay: Int
        get() = prefs.getInt(WINDOW_END, 0)
        set(value) = prefs.edit().putInt(WINDOW_END, value).apply()

    var scheduleStartHour: Int
        get() = prefs.getInt(SCHEDULE_START_HOUR, 8)
        set(value) = prefs.edit().putInt(SCHEDULE_START_HOUR, value).apply()

    var scheduleEndHour: Int
        get() = prefs.getInt(SCHEDULE_END_HOUR, 20)
        set(value) = prefs.edit().putInt(SCHEDULE_END_HOUR, value).apply()

    var maxPerRun: Int
        get() = prefs.getInt(MAX_PER_RUN, 5)
        set(value) = prefs.edit().putInt(MAX_PER_RUN, value).apply()

    var lastCheckTime: Long
        get() = prefs.getLong(LAST_CHECK, 0)
        set(value) = prefs.edit().putLong(LAST_CHECK, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(LAST_SYNC, 0)
        set(value) = prefs.edit().putLong(LAST_SYNC, value).apply()

    private var cachedVehicles: List<Vehicle> = emptyList()
    private var cachedStats: AppsScriptApi.ScriptStats? = null

    fun isServiceRunning(): Boolean = prefs.getBoolean(SERVICE_RUNNING, false)
    fun setServiceRunning(running: Boolean) {
        prefs.edit().putBoolean(SERVICE_RUNNING, running).apply()
    }

    suspend fun syncFromAppScript(): Result<List<Vehicle>> {
        if (appsScriptUrl.isBlank()) return Result.failure(Exception("Apps Script URL not configured"))
        val result = appsScriptApi.fetchAll(appsScriptUrl)
        if (result.isSuccess) {
            cachedVehicles = result.getOrDefault(emptyList())
            lastSyncTime = System.currentTimeMillis()
        }
        return result
    }

    fun getCachedVehicles(): List<Vehicle> = cachedVehicles

    fun getEligibleVehicles(vehicles: List<Vehicle> = cachedVehicles): List<Vehicle> {
        return vehicles.filter { v ->
            if (v.isExpired || v.noHp.isBlank()) return@filter false
            val sisaHari = v.countDaysUntilExpiry() ?: return@filter false
            sisaHari <= windowStartDay && sisaHari >= windowEndDay
        }
    }

    fun buildMessage(v: Vehicle): String = buildString {
        val formatted = v.formatTanggalIndonesia()
        appendLine("*DISHUB KAB. LUMAJANG*")
        appendLine()
        appendLine("*MASA BERLAKU (KIR) BERAKHIR $formatted.*")
        appendLine("Identitas Kendaraan:")
        appendLine("No. Kendaraan : *${v.noPolisi}*")
        appendLine("No. Uji       : *${v.noUji}*")
        appendLine("No. Rangka    : *${v.noRangka}*")
        appendLine("No. Mesin     : *${v.noMesin}*")
        appendLine("Identitas Pemilik:")
        appendLine("Atas Nama     : *${v.namaPemilik}*")
        appendLine("Alamat        : *${v.alamat}*")
        appendLine()
        appendLine("*JANGAN LUPA UJIKAN KENDARAAN ANDA*")
    }

    fun buildWaIntentWithoutText(v: Vehicle): Intent {
        val uri = "https://wa.me/${v.noHpClean}"
        return Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    suspend fun markSent(v: Vehicle) {
        try {
            appsScriptApi.markDone(appsScriptUrl, v.row)
        } catch (_: Exception) {}
        logDao.insert(
            SendLog(
                nomorKendaraan = v.noPolisi,
                namaPemilik = v.namaPemilik,
                nomorHP = v.noHp,
                status = "SENT"
            )
        )
    }

    suspend fun markFailed(v: Vehicle, error: String) {
        logDao.insert(
            SendLog(
                nomorKendaraan = v.noPolisi,
                namaPemilik = v.namaPemilik,
                nomorHP = v.noHp,
                status = "FAILED",
                error = error
            )
        )
    }

    suspend fun getLogs(limit: Int = 50, offset: Int = 0): List<SendLog> =
        logDao.getLogs(limit, offset)

    suspend fun getLogCount(): Int = logDao.count()
    suspend fun getSentCount(since: Long): Int = logDao.countSentSince(since)

    suspend fun getStats(): Map<String, Any> {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val totalVehicles = cachedVehicles.size
        val eligible = getEligibleVehicles().size
        val sentToday = getSentCount(startOfDay.timeInMillis)

        return mapOf(
            "totalVehicles" to totalVehicles,
            "expiringSoon" to eligible,
            "sentToday" to sentToday,
            "serviceRunning" to isServiceRunning(),
            "lastSync" to lastSyncTime,
            "lastCheck" to lastCheckTime,
            "appsScriptConfigured" to appsScriptUrl.isNotBlank()
        )
    }

    companion object {
        private const val SCRIPT_URL = "apps_script_url"
        private const val DEFAULT_GAS_URL = "https://script.google.com/macros/s/AKfycbx0hpENWDjBnbzkSfab5M4ZkqA9Kop6AnfZVMhKI0OeDofCuVbfBRkGUbhuRCna4urWvw/exec"
        private const val WINDOW_START = "window_start"
        private const val WINDOW_END = "window_end"
        private const val SCHEDULE_START_HOUR = "schedule_start_hour"
        private const val SCHEDULE_END_HOUR = "schedule_end_hour"
        private const val MAX_PER_RUN = "max_per_run"
        private const val LAST_CHECK = "last_check"
        private const val LAST_SYNC = "last_sync"
        private const val SERVICE_RUNNING = "service_running"
    }
}
