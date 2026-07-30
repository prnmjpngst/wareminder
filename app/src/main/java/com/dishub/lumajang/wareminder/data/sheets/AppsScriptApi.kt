package com.dishub.lumajang.wareminder.data.sheets

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsScriptApi @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private fun buildUrl(scriptUrl: String, path: String): String {
        val sep = if (scriptUrl.contains("?")) "&" else "?"
        return "$scriptUrl${sep}$path"
    }

    suspend fun fetchVehicles(scriptUrl: String): Result<List<Vehicle>> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(scriptUrl, "action=eligible")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val listType = object : TypeToken<List<VehicleRaw>>() {}.type
            val raw: List<VehicleRaw> = gson.fromJson(body, listType)
            val vehicles = raw.map { it.toVehicle() }
            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAll(scriptUrl: String): Result<List<Vehicle>> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(scriptUrl, "action=all")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val listType = object : TypeToken<List<VehicleRaw>>() {}.type
            val raw: List<VehicleRaw> = gson.fromJson(body, listType)
            val vehicles = raw.map { it.toVehicle() }
            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStats(scriptUrl: String): Result<ScriptStats> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(scriptUrl, "action=stats")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val stats: ScriptStats = gson.fromJson(body, ScriptStats::class.java)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markDone(scriptUrl: String, row: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(scriptUrl, "action=markDone&row=$row")
            val request = Request.Builder().url(url)
                .post(FormBody.Builder().add("_", "1").build())
                .build()
            client.newCall(request).execute().close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class VehicleRaw(
        val row: Int = 0,
        val noPolisi: String = "",
        val namaPemilik: String = "",
        val alamat: String = "",
        val noUji: String = "",
        val noRangka: String = "",
        val noMesin: String = "",
        val tanggalExpiry: String = "",
        val noHp: String = "",
        val statusReminder: String = ""
    ) {
        fun toVehicle() = Vehicle(
            row = row,
            noPolisi = noPolisi,
            namaPemilik = namaPemilik,
            alamat = alamat,
            noUji = noUji,
            noRangka = noRangka,
            noMesin = noMesin,
            tanggalExpiry = tanggalExpiry,
            noHp = noHp,
            statusReminder = statusReminder
        )
    }

    data class ScriptStats(
        val total: Int = 0,
        val sent: Int = 0,
        val pending: Int = 0,
        val eligible: Int = 0,
        val preExpiry: Int = 0,
        val postExpiry: Int = 0
    )
}
