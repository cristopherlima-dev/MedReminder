package com.example.medreminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.medreminder.data.AppDatabase
import com.example.medreminder.data.entity.DoseHistory
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val doseHistoryDao = db.doseHistoryDao()

    val allHistory: Flow<List<DoseHistory>> = doseHistoryDao.getAll()

    fun getHistoryByDate(year: Int, month: Int, dayOfMonth: Int): Flow<List<DoseHistory>> {
        val startCal = Calendar.getInstance().apply {
            set(year, month, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(year, month, dayOfMonth, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return doseHistoryDao.getByDate(startCal.timeInMillis, endCal.timeInMillis)
    }

    /**
     * Retorna sonecas pendentes (status SNOOZED com snoozedTo no futuro).
     */
    fun getPendingSnoozes(): Flow<List<DoseHistory>> {
        return doseHistoryDao.getPendingSnoozes(System.currentTimeMillis())
    }

    /**
     * Marca uma soneca pendente como TAKEN e cancela o alarme de snooze.
     */
    suspend fun confirmSnoozedDose(dose: DoseHistory) {
        doseHistoryDao.updateStatus(dose.id, "TAKEN", System.currentTimeMillis())
    }
}