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

    /**
     * Busca sonecas pendentes (Flow): status SNOOZED com snoozedTo no futuro.
     * Usado para mostrar na lista de medicamentos o botão "Já Tomei" antecipado.
     */
    @Query("SELECT * FROM dose_history WHERE status = 'SNOOZED' AND snoozedTo > :now ORDER BY snoozedTo ASC")
    fun getPendingSnoozes(now: Long): Flow<List<DoseHistory>>

    /**
     * Busca sonecas pendentes (suspend): usado pelo BootReceiver para reagendar após reinicialização.
     */
    @Query("SELECT * FROM dose_history WHERE status = 'SNOOZED' AND snoozedTo > :now ORDER BY snoozedTo ASC")
    suspend fun getPendingSnoozesOneShot(now: Long): List<DoseHistory>

    /**
     * Atualiza o status de uma dose (ex: SNOOZED → TAKEN quando o usuário toma antes da soneca).
     */
    @Query("UPDATE dose_history SET status = :newStatus, takenAt = :takenAt WHERE id = :doseId")
    suspend fun updateStatus(doseId: Long, newStatus: String, takenAt: Long = System.currentTimeMillis())
}