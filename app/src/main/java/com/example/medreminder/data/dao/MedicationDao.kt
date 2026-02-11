package com.example.medreminder.data.dao

import androidx.room.*
import com.example.medreminder.data.entity.Medication
import com.example.medreminder.data.relation.MedicationWithAlarms
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAll(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Transaction
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllWithAlarms(): Flow<List<MedicationWithAlarms>>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getWithAlarms(id: Long): MedicationWithAlarms?
}