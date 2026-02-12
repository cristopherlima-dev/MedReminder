package com.example.medreminder.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medreminder.data.entity.AlarmSchedule
import com.example.medreminder.data.entity.DoseHistory
import com.example.medreminder.data.entity.Medication
import com.example.medreminder.data.relation.MedicationWithAlarms
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListScreen(
    medicationsWithAlarms: List<MedicationWithAlarms>,
    pendingSnoozes: List<DoseHistory> = emptyList(),
    onAddClick: () -> Unit,
    onMedicationClick: (Long) -> Unit,
    onDeleteClick: (MedicationWithAlarms) -> Unit,
    onSettingsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onConfirmSnooze: (DoseHistory) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf<MedicationWithAlarms?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💊 MedReminder") },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Histórico"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar medicamento")
            }
        }
    ) { padding ->
        if (medicationsWithAlarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "💊",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum medicamento cadastrado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toque no + para adicionar\nseu primeiro medicamento",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cards de soneca pendente
                if (pendingSnoozes.isNotEmpty()) {
                    items(pendingSnoozes, key = { "snooze_${it.id}" }) { snooze ->
                        PendingSnoozeCard(
                            dose = snooze,
                            onConfirm = { onConfirmSnooze(snooze) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Card de próximo alarme
                val nextAlarm = findNextAlarm(medicationsWithAlarms)
                if (nextAlarm != null) {
                    item {
                        NextAlarmCard(
                            medicationName = nextAlarm.first,
                            hour = nextAlarm.second,
                            minute = nextAlarm.third
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(medicationsWithAlarms, key = { it.medication.id }) { item ->
                    MedicationCard(
                        item = item,
                        onClick = { onMedicationClick(item.medication.id) },
                        onDeleteClick = { showDeleteDialog = item }
                    )
                }
            }
        }

        // Diálogo de confirmação de exclusão
        showDeleteDialog?.let { item ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Excluir medicamento") },
                text = { Text("Deseja excluir \"${item.medication.name}\" e todos os seus alarmes?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteClick(item)
                        showDeleteDialog = null
                    }) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun PendingSnoozeCard(
    dose: DoseHistory,
    onConfirm: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val snoozeTime = if (dose.snoozedTo != null) {
        timeFormat.format(Date(dose.snoozedTo))
    } else "?"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⏰ Soneca pendente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dose.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Alarme às $snoozeTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("✅ Já Tomei!")
            }
        }
    }
}

@Composable
fun NextAlarmCard(medicationName: String, hour: Int, minute: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⏰ Próximo alarme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = "%02d:%02d".format(hour, minute),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MedicationCard(
    item: MedicationWithAlarms,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (item.medication.dosage.isNotBlank()) {
                    Text(
                        text = item.medication.dosage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.alarms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val enabledCount = item.alarms.count { it.isEnabled }
                    Text(
                        text = "⏰ $enabledCount alarme(s) ativo(s): " +
                                item.alarms.filter { it.isEnabled }
                                    .joinToString(", ") {
                                        "%02d:%02d".format(it.hour, it.minute)
                                    },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (enabledCount < item.alarms.size) {
                        Text(
                            text = "⏸️ ${item.alarms.size - enabledCount} alarme(s) pausado(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sem alarmes configurados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun findNextAlarm(medicationsWithAlarms: List<MedicationWithAlarms>): Triple<String, Int, Int>? {
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)

    var bestName: String? = null
    var bestHour = -1
    var bestMinute = -1
    var bestDiff = Int.MAX_VALUE

    for (item in medicationsWithAlarms) {
        for (alarm in item.alarms) {
            if (!alarm.isEnabled) continue

            var diffMinutes = (alarm.hour * 60 + alarm.minute) - (currentHour * 60 + currentMinute)
            if (diffMinutes <= 0) {
                diffMinutes += 24 * 60
            }

            if (diffMinutes < bestDiff) {
                bestDiff = diffMinutes
                bestName = item.medication.name
                bestHour = alarm.hour
                bestMinute = alarm.minute
            }
        }
    }

    return if (bestName != null) Triple(bestName, bestHour, bestMinute) else null
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MedicationListScreenPreview() {
    MedicationListScreen(
        medicationsWithAlarms = listOf(
            MedicationWithAlarms(
                medication = Medication(id = 1, name = "Losartana", dosage = "50mg"),
                alarms = listOf(
                    AlarmSchedule(id = 1, medicationId = 1, hour = 8, minute = 0),
                    AlarmSchedule(id = 2, medicationId = 1, hour = 20, minute = 0)
                )
            ),
            MedicationWithAlarms(
                medication = Medication(id = 2, name = "Vitamina D", dosage = "1 cápsula"),
                alarms = listOf(
                    AlarmSchedule(id = 3, medicationId = 2, hour = 12, minute = 0, isEnabled = false)
                )
            ),
            MedicationWithAlarms(
                medication = Medication(id = 3, name = "Omega 3", dosage = "1g"),
                alarms = emptyList()
            )
        ),
        pendingSnoozes = listOf(
            DoseHistory(
                id = 10,
                medicationId = 1,
                medicationName = "Losartana",
                scheduledHour = 8,
                scheduledMinute = 0,
                status = "SNOOZED",
                snoozedTo = System.currentTimeMillis() + 180000
            )
        ),
        onAddClick = {},
        onMedicationClick = {},
        onDeleteClick = {}
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "Lista Vazia")
@Composable
fun MedicationListScreenEmptyPreview() {
    MedicationListScreen(
        medicationsWithAlarms = emptyList(),
        onAddClick = {},
        onMedicationClick = {},
        onDeleteClick = {}
    )
}