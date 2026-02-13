package com.example.medreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.medreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot detectado — reagendando alarmes e sonecas...")

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)

                    // 1. Reagendar alarmes normais
                    val enabledAlarms = db.alarmScheduleDao().getAllEnabled()
                    for (alarm in enabledAlarms) {
                        val medication = db.medicationDao().getById(alarm.medicationId)
                        val name = medication?.name ?: "Medicamento"
                        AlarmScheduler.schedule(context, alarm, name)
                    }
                    Log.d(TAG, "${enabledAlarms.size} alarmes reagendados após boot")

                    // 2. Reagendar sonecas pendentes
                    val now = System.currentTimeMillis()
                    val pendingSnoozes = db.doseHistoryDao().getPendingSnoozesOneShot(now)
                    for (snooze in pendingSnoozes) {
                        val alarms = db.alarmScheduleDao().getByMedicationOneShot(snooze.medicationId)
                        val matchingAlarm = alarms.firstOrNull {
                            it.hour == snooze.scheduledHour && it.minute == snooze.scheduledMinute
                        }
                        val alarmId = matchingAlarm?.id ?: 0L

                        val remainingMs = snooze.snoozedTo!! - now
                        val remainingMinutes = (remainingMs / 60000).toInt().coerceAtLeast(1)

                        AlarmScheduler.scheduleSnooze(
                            context = context,
                            alarmId = alarmId,
                            medicationId = snooze.medicationId,
                            medicationName = snooze.medicationName,
                            originalHour = snooze.scheduledHour,
                            originalMinute = snooze.scheduledMinute,
                            minutes = remainingMinutes
                        )
                        Log.d(TAG, "Soneca reagendada: ${snooze.medicationName} em $remainingMinutes min")
                    }
                    Log.d(TAG, "${pendingSnoozes.size} sonecas reagendadas após boot")

                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao reagendar após boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}