package com.dishub.lumajang.wareminder.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {

    @Insert
    suspend fun insert(log: SendLog)

    @Query("SELECT * FROM send_logs ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getLogs(limit: Int = 50, offset: Int = 0): List<SendLog>

    @Query("SELECT * FROM send_logs WHERE nomorKendaraan LIKE '%' || :plate || '%' ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun searchByPlate(plate: String, limit: Int = 50, offset: Int = 0): List<SendLog>

    @Query("SELECT COUNT(*) FROM send_logs")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM send_logs WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM send_logs WHERE timestamp >= :since AND status = 'SENT'")
    suspend fun countSentSince(since: Long): Int

    @Query("DELETE FROM send_logs WHERE timestamp < :before")
    suspend fun deleteOldLogs(before: Long)
}
