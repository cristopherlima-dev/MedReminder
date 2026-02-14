package com.example.medreminder.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.medreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "med_reminder_alarm"
        const val NOTIFICATION_ID = 9999
        private const val TAG = "AlarmService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra("ALARM_ID", -1) ?: -1
        val medicationId = intent?.getLongExtra("MEDICATION_ID", -1) ?: -1
        val medicationName = intent?.getStringExtra("MEDICATION_NAME") ?: "Medicamento"
        val hour = intent?.getIntExtra("HOUR", 0) ?: 0
        val minute = intent?.getIntExtra("MINUTE", 0) ?: 0
        val snoozeCount = intent?.getIntExtra("SNOOZE_COUNT", 0) ?: 0

        Log.d(TAG, "Service iniciado (backup) para: $medicationName às %02d:%02d, medId=$medicationId, snoozeCount=$snoozeCount".format(hour, minute))

        val notification = createAlarmNotification(medicationName, hour, minute, alarmId, medicationId, snoozeCount)
        startForeground(NOTIFICATION_ID, notification)

        rescheduleForTomorrow(alarmId)

        serviceScope.launch {
            kotlinx.coroutines.delay(30_000)
            Log.d(TAG, "Service auto-stop após 30s")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarmes de Medicamento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de alarme de medicamentos"
                enableVibration(false)
                setSound(null, null)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createAlarmNotification(
        medicationName: String,
        hour: Int,
        minute: Int,
        alarmId: Long,
        medicationId: Long,
        snoozeCount: Int
    ): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("MEDICATION_ID", medicationId)
            putExtra("MEDICATION_NAME", medicationName)
            putExtra("HOUR", hour)
            putExtra("MINUTE", minute)
            putExtra("SNOOZE_COUNT", snoozeCount)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("💊 Hora do Medicamento!")
            .setContentText("$medicationName - %02d:%02d".format(hour, minute))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()
    }

    private fun rescheduleForTomorrow(alarmId: Long) {
        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val alarm = db.alarmScheduleDao().getById(alarmId)
                if (alarm != null && alarm.isEnabled) {
                    val medication = db.medicationDao().getById(alarm.medicationId)
                    val name = medication?.name ?: "Medicamento"
                    AlarmScheduler.scheduleNextDay(applicationContext, alarm, name)
                    Log.d(TAG, "Reagendado para amanhã: $alarmId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao reagendar: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Service destruído")
    }
}