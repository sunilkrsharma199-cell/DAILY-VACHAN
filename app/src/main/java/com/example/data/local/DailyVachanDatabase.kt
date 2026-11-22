package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DevotionalEntity::class], version = 1, exportSchema = false)
abstract class DailyVachanDatabase : RoomDatabase() {
    abstract fun devotionalDao(): DevotionalDao

    companion object {
        @Volatile
        private var INSTANCE: DailyVachanDatabase? = null

        fun getInstance(context: Context): DailyVachanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DailyVachanDatabase::class.java,
                    "daily_vachan.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
