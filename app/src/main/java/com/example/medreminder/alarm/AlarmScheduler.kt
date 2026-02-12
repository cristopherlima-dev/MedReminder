package com.example.medreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.medreminder.data.entity.AlarmSchedule
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    const val SNOOZE_REQUEST_CODE_BASE = 900000

    fun schedule(context: Context, alarm: AlarmSchedule, medicationName: String) {
        if (!alarm.isEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Sem permissão para alarmes exatos!")
                return
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("MEDICATION_ID", alarm.medicationId)
            putExtra("MEDICATION_NAME", medicationName)
            putExtra("HOUR", alarm.hour)
            putExtra("MINUTE", alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            calendar.timeInMillis,
            pendingIntent
        )

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        Log.d(TAG, "Alarme agendado: ${alarm.id} - $medicationName às %02d:%02d para ${calendar.time}".format(alarm.hour, alarm.minute))
    }

    fun cancel(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarme cancelado: $alarmId")
    }

    fun scheduleNextDay(context: Context, alarm: AlarmSchedule, medicationName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("MEDICATION_ID", alarm.medicationId)
            putExtra("MEDICATION_NAME", medicationName)
            putExtra("HOUR", alarm.hour)
            putExtra("MINUTE", alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            calendar.timeInMillis,
            pendingIntent
        )

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        Log.d(TAG, "Próximo alarme agendado: ${alarm.id} para ${calendar.time}")
    }

    /**
     * Agenda um alarme de soneca para daqui a [minutes] minutos.
     * Usa request code diferente (SNOOZE_REQUEST_CODE_BASE + alarmId) para não cancelar o alarme diário.
     */
    fun scheduleSnooze(
        context: Context,
        alarmId: Long,
        medicationId: Long,
        medicationName: String,
        originalHour: Int,
        originalMinute: Int,
        minutes: Int = 5
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Sem permissão para alarmes exatos (snooze)!")
                return
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("MEDICATION_ID", medicationId)
            putExtra("MEDICATION_NAME", medicationName)
            putExtra("HOUR", originalHour)
            putExtra("MINUTE", originalMinute)
            putExtra("IS_SNOOZE", true)
        }

        val snoozeRequestCode = SNOOZE_REQUEST_CODE_BASE + alarmId.toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            triggerTime,
            pendingIntent
        )

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        Log.d(TAG, "Soneca agendada: alarmId=$alarmId - $medicationName para daqui $minutes minutos")
    }

    /**
     * Cancela um alarme de soneca pendente.
     */
    fun cancelSnooze(context: Context, snoozeRequestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Soneca cancelada: requestCode=$snoozeRequestCode")
    }
}