package com.example.medreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.medreminder.data.dao.AlarmScheduleDao
import com.example.medreminder.data.dao.DoseHistoryDao
import com.example.medreminder.data.dao.MedicationDao
import com.example.medreminder.data.entity.AlarmSchedule
import com.example.medreminder.data.entity.DoseHistory
import com.example.medreminder.data.entity.Medication

@Database(
    entities = [Medication::class, AlarmSchedule::class, DoseHistory::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun alarmScheduleDao(): AlarmScheduleDao
    abstract fun doseHistoryDao(): DoseHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "med_reminder_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}