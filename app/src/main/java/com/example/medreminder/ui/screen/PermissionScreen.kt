package com.example.medreminder.ui.screen

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current

    var notificationGranted by remember { mutableStateOf(checkNotificationPermission(context)) }
    var exactAlarmGranted by remember { mutableStateOf(checkExactAlarmPermission(context)) }
    var batteryOptimizationIgnored by remember { mutableStateOf(checkBatteryOptimization(context)) }

    // Verificar periodicamente
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            notificationGranted = checkNotificationPermission(context)
            exactAlarmGranted = checkExactAlarmPermission(context)
            batteryOptimizationIgnored = checkBatteryOptimization(context)

            if (notificationGranted && exactAlarmGranted && batteryOptimizationIgnored) {
                onAllGranted()
                break
            }
        }
    }

    // Launcher para permissão de notificação
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚙️ Configurações Necessárias",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Para que os alarmes funcionem corretamente no seu Samsung, precisamos de algumas permissões:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Notificações
            PermissionCard(
                title = "Notificações",
                description = "Necessário para mostrar os alarmes",
                isGranted = notificationGranted,
                onRequestClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            // 2. Alarmes Exatos
            PermissionCard(
                title = "Alarmes Exatos",
                description = "Necessário para disparar os alarmes no horário correto",
                isGranted = exactAlarmGranted,
                onRequestClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                }
            )

            // 3. Otimização de Bateria
            PermissionCard(
                title = "Ignorar Otimização de Bateria",
                description = "ESSENCIAL para Samsung! Impede que o sistema mate o app em segundo plano",
                isGranted = batteryOptimizationIgnored,
                onRequestClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card com dica Samsung
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚠️ Dica para Samsung",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Além das permissões acima, recomendamos:\n\n" +
                                "1. Configurações > Cuidados com o dispositivo > Bateria > MedReminder > Sem restrições\n\n" +
                                "2. Configurações > Apps > MedReminder > Bateria > Sem restrições\n\n" +
                                "3. Mantenha o app na lista de apps recentes (não remova com swipe)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão para continuar mesmo sem todas as permissões
            OutlinedButton(
                onClick = onAllGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar mesmo assim")
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isGranted) "✅" else "❌",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isGranted) {
                Button(
                    onClick = onRequestClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Permitir")
                }
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}