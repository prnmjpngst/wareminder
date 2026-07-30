package com.dishub.lumajang.wareminder.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import com.dishub.lumajang.wareminder.data.db.LogDao
import com.dishub.lumajang.wareminder.data.db.SendLog
import com.dishub.lumajang.wareminder.data.sheets.SheetsApi
import com.dishub.lumajang.wareminder.data.sheets.Vehicle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sheetsApi: SheetsApi,
    private val logDao: LogDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    var spreadsheetId: String
        get() = prefs.getString(SPREADSHEET_ID, "") ?: ""
        set(value) = prefs.edit().putString(SPREADSHEET_ID, value).apply()

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

    // Cached vehicles from last sync
    private var cachedVehicles: List<Vehicle> = emptyList()

    fun isServiceRunning(): Boolean = prefs.getBoolean(SERVICE_RUNNING, false)

    fun setServiceRunning(running: Boolean) {
        prefs.edit().putBoolean(SERVICE_RUNNING, running).apply()
    }

    suspend fun syncFromSheets(): Result<List<Vehicle>> {
        if (spreadsheetId.isBlank()) return Result.failure(Exception("Spreadsheet ID not configured"))
        if (!sheetsApi.isAvailable()) return Result.failure(Exception("Google Sheets not configured (missing service_account.json)"))
        return try {
            val vehicles = sheetsApi.fetchAllVehicles(spreadsheetId)
            cachedVehicles = vehicles
            lastSyncTime = System.currentTimeMillis()
            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedVehicles(): List<Vehicle> = cachedVehicles

    /**
     * Find vehicles within the expiry window (H-{windowStart} to H-{windowEnd}).
     * Parses TAHUN, BULAN, TANGGAL to compute days until expiry.
     */
    fun getEligibleVehicles(vehicles: List<Vehicle> = cachedVehicles): List<Vehicle> {
        val now = Calendar.getInstance()
        return vehicles.filter { v ->
            if (v.isExpired() || v.nomorHP.isBlank()) return@filter false
            val sisaHari = hitungSisaHari(v.tahun, v.bulan, v.tanggal)
            sisaHari != null && sisaHari <= windowStartDay && sisaHari >= windowEndDay
        }
    }

    private fun hitungSisaHari(tahun: String, bulan: String, tanggal: String): Int? {
        val monthMap = mapOf(
            "Januari" to 0, "Februari" to 1, "Maret" to 2, "April" to 3,
            "Mei" to 4, "Juni" to 5, "Juli" to 6, "Agustus" to 7,
            "September" to 8, "Oktober" to 9, "November" to 10, "Desember" to 11,
            "Jan" to 0, "Feb" to 1, "Mar" to 2, "Apr" to 3,
            "Mei" to 4, "Jun" to 5, "Jul" to 6, "Agu" to 7,
            "Sep" to 8, "Okt" to 9, "Nov" to 10, "Des" to 11
        )
        val year = tahun.toIntOrNull() ?: return null
        val month = monthMap[bulan.trim().lowercase().replaceFirstChar { it.uppercase() }] ?: return null
        val day = tanggal.toIntOrNull() ?: return null
        val cal = Calendar.getInstance()
        val now = Calendar.getInstance()
        cal.set(year, month, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        val diff = cal.timeInMillis - now.timeInMillis
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    fun buildMessage(v: Vehicle): String = buildString {
        appendLine("*DISHUB KAB. LUMAJANG*")
        appendLine()
        appendLine("*MASA BERLAKU (KIR) BERAKHIR ${v.hari}, ${v.tanggal} ${v.bulan} ${v.tahun}.*")
        appendLine("Identitas Kendaraan:")
        appendLine("No. Kendaraan : *${v.nomorKendaraan}*")
        appendLine("No. Uji       : *${v.nomorUji}*")
        appendLine("No. Rangka    : *${v.nomorRangka}*")
        appendLine("No. Mesin     : *${v.nomorMesin}*")
        appendLine("Identitas Pemilik:")
        appendLine("Atas Nama     : *${v.namaPemilik}*")
        appendLine("Alamat        : *${v.alamat}*")
        appendLine()
        appendLine("*MASA BERLAKU (KIR) BERAKHIR ${v.hari}, ${v.tanggal} ${v.bulan} ${v.tahun}.*")
        appendLine("Identitas Kendaraan:")
        appendLine("No. Kendaraan : *${v.nomorKendaraan}*")
        appendLine("No. Uji       : *${v.nomorUji}*")
        appendLine("No. Rangka    : *${v.nomorRangka}*")
        appendLine("No. Mesin     : *${v.nomorMesin}*")
        appendLine("Identitas Pemilik:")
        appendLine("Atas Nama     : *${v.namaPemilik}*")
        appendLine("Alamat        : *${v.alamat}*")
        appendLine()
        appendLine("*JANGAN LUPA UJIKAN KENDARAAN ANDA*")
        appendLine()
        appendLine("> _Sent via WA Reminder Dishub_")
    }

    fun buildWaIntent(v: Vehicle): Intent {
        val message = buildMessage(v)
        val uri = "https://wa.me/${v.nomorHPClean}?text=${Uri.encode(message)}"
        return Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    }

    suspend fun markSent(v: Vehicle) {
        try {
            sheetsApi.markDone(spreadsheetId, v.rowIndex)
        } catch (_: Exception) {}
        logDao.insert(
            SendLog(
                nomorKendaraan = v.nomorKendaraan,
                namaPemilik = v.namaPemilik,
                nomorHP = v.nomorHP,
                status = "SENT"
            )
        )
    }

    suspend fun markFailed(v: Vehicle, error: String) {
        logDao.insert(
            SendLog(
                nomorKendaraan = v.nomorKendaraan,
                namaPemilik = v.namaPemilik,
                nomorHP = v.nomorHP,
                status = "FAILED",
                error = error
            )
        )
    }

    suspend fun getLogs(limit: Int = 50, offset: Int = 0): List<SendLog> =
        logDao.getLogs(limit, offset)

    suspend fun getLogCount(): Int = logDao.count()
    suspend fun getSentCount(since: Long): Int = logDao.countSentSince(since)
    suspend fun getFailedCount(): Int = logDao.countByStatus("FAILED")

    fun getStats(): Map<String, Any> {
        val now = Calendar.getInstance()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val totalVehicles = cachedVehicles.size
        val expiredVehicles = cachedVehicles.count { it.isExpired() }
        val expiringSoon = getEligibleVehicles().size

        return mapOf(
            "totalVehicles" to totalVehicles,
            "expiredVehicles" to expiredVehicles,
            "expiringSoon" to expiringSoon,
            "activeVehicles" to (totalVehicles - expiredVehicles),
            "serviceRunning" to isServiceRunning(),
            "lastSync" to lastSyncTime,
            "lastCheck" to lastCheckTime,
            "spreadsheetConfigured" to spreadsheetId.isNotBlank(),
            "sheetsAvailable" to sheetsApi.isAvailable()
        )
    }

    companion object {
        private const val SPREADSHEET_ID = "spreadsheet_id"
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
