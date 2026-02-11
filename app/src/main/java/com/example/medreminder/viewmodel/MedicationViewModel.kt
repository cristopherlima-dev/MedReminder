package com.example.medreminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.data.AppDatabase
import com.example.medreminder.data.entity.Medication
import com.example.medreminder.data.relation.MedicationWithAlarms
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val medicationDao = db.medicationDao()

    val medicationsWithAlarms: StateFlow<List<MedicationWithAlarms>> =
        medicationDao.getAllWithAlarms()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMedication(name: String, dosage: String, notes: String) {
        viewModelScope.launch {
            medicationDao.insert(
                Medication(name = name, dosage = dosage, notes = notes)
            )
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            medicationDao.update(medication)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            medicationDao.delete(medication)
        }
    }

    suspend fun getMedicationById(id: Long): Medication? {
        return medicationDao.getById(id)
    }
}