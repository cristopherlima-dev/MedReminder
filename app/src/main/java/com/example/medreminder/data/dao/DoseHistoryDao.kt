package com.example.medreminder.data.dao

import androidx.room.*
import com.example.medreminder.data.entity.DoseHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseHistoryDao {

    @Insert
    suspend fun insert(dose: DoseHistory): Long

    @Query("SELECT * FROM dose_history ORDER BY takenAt DESC")
    fun getAll(): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history WHERE medicationId = :medicationId ORDER BY takenAt DESC")
    fun getByMedication(medicationId: Long): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history WHERE takenAt >= :startOfDay AND takenAt < :endOfDay ORDER BY takenAt DESC")
    fun getByDate(startOfDay: Long, endOfDay: Long): Flow<List<DoseHistory>>

    @Query("SELECT COUNT(*) FROM dose_history WHERE status = 'TAKEN' AND takenAt >= :since")
    suspend fun countTakenSince(since: Long): Int

    @Query("DELETE FROM dose_history")
    suspend fun deleteAll()
}