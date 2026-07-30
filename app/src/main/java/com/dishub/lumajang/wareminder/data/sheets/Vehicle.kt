package com.dishub.lumajang.wareminder.data.sheets

import com.google.gson.annotations.SerializedName

data class Vehicle(
    val rowIndex: Int = 0,
    @SerializedName("NOMOR KENDARAAN") val nomorKendaraan: String = "",
    @SerializedName("NAMA PEMILIK") val namaPemilik: String = "",
    @SerializedName("ALAMAT") val alamat: String = "",
    @SerializedName("NOMOR UJI") val nomorUji: String = "",
    @SerializedName("NOMOR RANGKA") val nomorRangka: String = "",
    @SerializedName("NOMOR MESIN") val nomorMesin: String = "",
    @SerializedName("MASA BERLAKU") val masaBerlaku: String = "",
    @SerializedName("NOMOR HP") val nomorHP: String = "",
    @SerializedName("HARI") val hari: String = "",
    @SerializedName("TANGGAL") val tanggal: String = "",
    @SerializedName("BULAN") val bulan: String = "",
    @SerializedName("TAHUN") val tahun: String = "",
    @SerializedName("done") val done: String = ""
) {
    val nomorHPClean: String
        get() = nomorHP.replace(Regex("[^0-9]"), "").let {
            when {
                it.startsWith("62") -> it
                it.startsWith("0") -> "62${it.substring(1)}"
                it.startsWith("+62") -> it.substring(1)
                else -> "62$it"
            }
        }

    val masaBerlakuFormatted: String
        get() = "$hari, $tanggal $bulan $tahun"

    fun isExpired(): Boolean = done.equals("done", ignoreCase = true)

    fun toMap(): Map<String, Any> = mapOf(
        "NOMOR KENDARAAN" to nomorKendaraan,
        "NAMA PEMILIK" to namaPemilik,
        "ALAMAT" to alamat,
        "NOMOR UJI" to nomorUji,
        "NOMOR RANGKA" to nomorRangka,
        "NOMOR MESIN" to nomorMesin,
        "MASA BERLAKU" to masaBerlaku,
        "NOMOR HP" to nomorHP,
        "HARI" to hari,
        "TANGGAL" to tanggal,
        "BULAN" to bulan,
        "TAHUN" to tahun,
        "done" to done
    )

    companion object {
        val HEADERS = listOf(
            "NOMOR KENDARAAN", "NAMA PEMILIK", "ALAMAT", "NOMOR UJI",
            "NOMOR RANGKA", "NOMOR MESIN", "MASA BERLAKU", "NOMOR HP",
            "HARI", "TANGGAL", "BULAN", "TAHUN", "done"
        )

        fun fromRow(row: List<Any?>, rowIndex: Int): Vehicle? {
            if (row.size < 13) return null
            val get = { i: Int -> row.getOrElse(i) { "" }.toString().trim() }
            val nomorKendaraan = get(0)
            if (nomorKendaraan.isBlank()) return null
            return Vehicle(
                rowIndex = rowIndex,
                nomorKendaraan = nomorKendaraan,
                namaPemilik = get(1),
                alamat = get(2),
                nomorUji = get(3),
                nomorRangka = get(4),
                nomorMesin = get(5),
                masaBerlaku = get(6),
                nomorHP = get(7),
                hari = get(8),
                tanggal = get(9),
                bulan = get(10),
                tahun = get(11),
                done = get(12)
            )
        }
    }
}
