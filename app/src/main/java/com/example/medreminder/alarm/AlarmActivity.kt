package com.example.medreminder.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medreminder.data.AppDatabase
import com.example.medreminder.data.entity.DoseHistory
import com.example.medreminder.ui.theme.MedReminderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AlarmActivity"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar para aparecer sobre lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getLongExtra("ALARM_ID", -1)
        val medicationId = intent.getLongExtra("MEDICATION_ID", -1)
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: "Medicamento"
        val hour = intent.getIntExtra("HOUR", 0)
        val minute = intent.getIntExtra("MINUTE", 0)

        Log.d(TAG, "=== AlarmActivity aberta ===")
        Log.d(TAG, "alarmId=$alarmId, medicationId=$medicationId, name=$medicationName, hour=$hour, minute=$minute")

        // Iniciar som e vibração IMEDIATAMENTE (antes do Compose inflar)
        startAlarmSound()
        startVibration()

        setContent {
            MedReminderTheme {
                AlarmScreen(
                    medicationName = medicationName,
                    hour = hour,
                    minute = minute,
                    onDismiss = {
                        // Parar som e vibração imediatamente
                        stopAlarmSoundAndVibration()

                        // Parar o Service também (caso esteja rodando)
                        val serviceIntent = Intent(this@AlarmActivity, AlarmService::class.java)
                        stopService(serviceIntent)

                        // Gravar no banco e depois fechar
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                Log.d(TAG, "Gravando dose: medicationId=$medicationId, name=$medicationName")

                                val db = AppDatabase.getInstance(applicationContext)

                                // Buscar o medicationId real pelo alarmId caso venha -1
                                var finalMedicationId = medicationId
                                if (finalMedicationId <= 0 && alarmId > 0) {
                                    Log.d(TAG, "medicationId inválido, buscando pelo alarmId=$alarmId")
                                    val alarm = db.alarmScheduleDao().getById(alarmId)
                                    if (alarm != null) {
                                        finalMedicationId = alarm.medicationId
                                        Log.d(TAG, "medicationId encontrado via alarm: $finalMedicationId")
                                    }
                                }

                                if (finalMedicationId > 0) {
                                    val id = db.doseHistoryDao().insert(
                                        DoseHistory(
                                            medicationId = finalMedicationId,
                                            medicationName = medicationName,
                                            scheduledHour = hour,
                                            scheduledMinute = minute,
                                            status = "TAKEN"
                                        )
                                    )
                                    Log.d(TAG, "Dose gravada com sucesso! id=$id")
                                } else {
                                    Log.e(TAG, "ERRO: medicationId inválido mesmo após busca: $finalMedicationId")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "ERRO ao gravar dose: ${e.message}")
                                e.printStackTrace()
                            } finally {
                                runOnUiThread { finish() }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Som do alarme iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao tocar som: ${e.message}")
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
        Log.d(TAG, "Vibração iniciada")
    }

    private fun stopAlarmSoundAndVibration() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        Log.d(TAG, "Som e vibração parados")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSoundAndVibration()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        stopAlarmSoundAndVibration()
        val serviceIntent = Intent(this, AlarmService::class.java)
        stopService(serviceIntent)
        finish()
    }
}

@Composable
fun AlarmScreen(
    medicationName: String,
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "💊",
                fontSize = 80.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Hora do Medicamento!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = medicationName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "%02d:%02d".format(hour, minute),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "✅ Já Tomei!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}