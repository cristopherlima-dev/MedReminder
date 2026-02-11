package com.example.medreminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.alarm.AlarmScheduler
import com.example.medreminder.data.AppDatabase
import com.example.medreminder.data.entity.AlarmSchedule
import com.example.medreminder.data.entity.Medication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val alarmDao = db.alarmScheduleDao()
    private val medicationDao = db.medicationDao()

    private val _medication = MutableStateFlow<Medication?>(null)
    val medication: StateFlow<Medication?> = _medication.asStateFlow()

    private val _alarms = MutableStateFlow<List<AlarmSchedule>>(emptyList())
    val alarms: StateFlow<List<AlarmSchedule>> = _alarms.asStateFlow()

    fun loadMedication(medicationId: Long) {
        viewModelScope.launch {
            _medication.value = medicationDao.getById(medicationId)
        }
        viewModelScope.launch {
            alarmDao.getByMedication(medicationId).collect { list ->
                _alarms.value = list
            }
        }
    }

    fun addAlarm(medicationId: Long, hour: Int, minute: Int) {
        viewModelScope.launch {
            val alarm = AlarmSchedule(
                medicationId = medicationId,
                hour = hour,
                minute = minute
            )
            val id = alarmDao.insert(alarm)
            val savedAlarm = alarm.copy(id = id)

            // Agendar o alarme no sistema
            val medName = _medication.value?.name ?: "Medicamento"
            AlarmScheduler.schedule(
                getApplication(),
                savedAlarm,
                medName
            )
        }
    }

    fun deleteAlarm(alarm: AlarmSchedule) {
        viewModelScope.launch {
            alarmDao.delete(alarm)
            AlarmScheduler.cancel(getApplication(), alarm.id)
        }
    }

    fun toggleAlarm(alarm: AlarmSchedule) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmDao.update(updated)

            if (updated.isEnabled) {
                val medName = _medication.value?.name ?: "Medicamento"
                AlarmScheduler.schedule(getApplication(), updated, medName)
            } else {
                AlarmScheduler.cancel(getApplication(), alarm.id)
            }
        }
    }
}