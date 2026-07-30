package com.dishub.lumajang.wareminder.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dishub.lumajang.wareminder.data.sheets.Vehicle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaIntentSender @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun sendOpenChat(v: Vehicle) {
        val uri = "https://wa.me/${v.noHpClean}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }
}
