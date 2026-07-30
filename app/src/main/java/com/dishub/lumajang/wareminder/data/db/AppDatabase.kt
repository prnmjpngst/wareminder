package com.dishub.lumajang.wareminder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SendLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
}
