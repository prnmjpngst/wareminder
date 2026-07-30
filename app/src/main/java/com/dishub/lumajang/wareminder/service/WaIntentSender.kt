package com.dishub.lumajang.wareminder.service

import android.content.Context
import android.content.Intent
import com.dishub.lumajang.wareminder.data.sheets.Vehicle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaIntentSender @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun send(v: Vehicle) {
        val intent = buildWaIntent(v)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }

    private fun buildWaIntent(v: Vehicle): Intent {
        val message = buildMessage(v)
        val uri = "https://wa.me/${v.nomorHPClean}?text=${android.net.Uri.encode(message)}"
        return Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
    }

    private fun buildMessage(v: Vehicle): String = buildString {
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
}
