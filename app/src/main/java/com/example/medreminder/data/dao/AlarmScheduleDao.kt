package com.example.medreminder.data.dao

import androidx.room.*
import com.example.medreminder.data.entity.AlarmSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmSchedule): Long

    @Update
    suspend fun update(alarm: AlarmSchedule)

    @Delete
    suspend fun delete(alarm: AlarmSchedule)

    @Query("SELECT * FROM alarm_schedules WHERE medicationId = :medicationId ORDER BY hour, minute")
    fun getByMedication(medicationId: Long): Flow<List<AlarmSchedule>>

    @Query("SELECT * FROM alarm_schedules WHERE id = :id")
    suspend fun getById(id: Long): AlarmSchedule?

    @Query("SELECT * FROM alarm_schedules WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<AlarmSchedule>

    @Query("DELETE FROM alarm_schedules WHERE medicationId = :medicationId")
    suspend fun deleteAllByMedication(medicationId: Long)
}