package com.dishub.lumajang.wareminder.data.sheets

data class Vehicle(
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
    val noHpClean: String
        get() = noHp.replace(Regex("[^0-9]"), "").let {
            when {
                it.startsWith("62") -> it
                it.startsWith("0") -> "62${it.substring(1)}"
                it.startsWith("+62") -> it.substring(1)
                else -> "62$it"
            }
        }

    val isExpired: Boolean
        get() = statusReminder.isNotBlank() && statusReminder != ""

    fun formatTanggalIndonesia(): String {
        val iso = tanggalExpiry
        if (iso.isBlank()) return tanggalExpiry
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id"))
            val date = sdf.parse(iso.substring(0, 10)) ?: return iso
            val hariIndo = arrayOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu")
            val bulanIndo = arrayOf(
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
            )
            val cal = java.util.Calendar.getInstance().apply { time = date }
            val namaHari = hariIndo[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            val tanggalNum = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val bulan = bulanIndo[cal.get(java.util.Calendar.MONTH)]
            val tahun = cal.get(java.util.Calendar.YEAR)
            "$namaHari, $tanggalNum $bulan $tahun"
        } catch (_: Exception) {
            tanggalExpiry
        }
    }

    fun countDaysUntilExpiry(): Int? {
        if (tanggalExpiry.isBlank()) return null
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id"))
            val expDate = sdf.parse(tanggalExpiry.substring(0, 10)) ?: return null
            val cal = java.util.Calendar.getInstance()
            val now = java.util.Calendar.getInstance()
            cal.time = expDate
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            now.set(java.util.Calendar.HOUR_OF_DAY, 0)
            now.set(java.util.Calendar.MINUTE, 0)
            now.set(java.util.Calendar.SECOND, 0)
            now.set(java.util.Calendar.MILLISECOND, 0)
            val diff = cal.timeInMillis - now.timeInMillis
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (_: Exception) {
            null
        }
    }
}
