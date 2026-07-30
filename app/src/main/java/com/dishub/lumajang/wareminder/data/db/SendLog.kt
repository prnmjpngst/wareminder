package com.dishub.lumajang.wareminder.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "send_logs")
data class SendLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nomorKendaraan: String,
    val namaPemilik: String,
    val nomorHP: String,
    val status: String,       // SENT, FAILED, SKIPPED
    val error: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
