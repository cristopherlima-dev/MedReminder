package com.example.medreminder.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.medreminder.data.entity.AlarmSchedule
import com.example.medreminder.data.entity.Medication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScheduleScreen(
    medication: Medication?,
    alarms: List<AlarmSchedule>,
    onNavigateBack: () -> Unit,
    onAddAlarm: (Int, Int) -> Unit,
    onDeleteAlarm: (AlarmSchedule) -> Unit,
    onToggleAlarm: (AlarmSchedule) -> Unit,
    onEditMedication: (Long) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<AlarmSchedule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(medication?.name ?: "Carregando...")
                        if (medication?.dosage?.isNotBlank() == true) {
                            Text(
                                text = medication.dosage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (medication != null) {
                        TextButton(onClick = { onEditMedication(medication.id) }) {
                            Text("Editar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar alarme")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Notas do medicamento
            if (medication?.notes?.isNotBlank() == true) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "📝 ${medication.notes}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Título da seção
            Text(
                text = "Alarmes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhum alarme configurado.\nToque no + para adicionar um horário.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { onToggleAlarm(alarm) },
                            onDelete = { showDeleteDialog = alarm }
                        )
                    }
                }
            }
        }

        // TimePicker Dialog
        if (showTimePicker) {
            var showDuplicateError by remember { mutableStateOf(false) }

            TimePickerDialog(
                onDismiss = {
                    showTimePicker = false
                    showDuplicateError = false
                },
                onConfirm = { hour, minute ->
                    val isDuplicate = alarms.any { it.hour == hour && it.minute == minute }
                    if (isDuplicate) {
                        showDuplicateError = true
                    } else {
                        onAddAlarm(hour, minute)
                        showTimePicker = false
                    }
                },
                errorMessage = if (showDuplicateError) "Já existe um alarme neste horário!" else null
            )
        }

        // Delete Dialog
        showDeleteDialog?.let { alarm ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Excluir alarme") },
                text = { Text("Excluir o alarme das %02d:%02d?".format(alarm.hour, alarm.minute)) },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteAlarm(alarm)
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
fun AlarmCard(
    alarm: AlarmSchedule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "%02d:%02d".format(alarm.hour, alarm.minute),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = if (alarm.isEnabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Text(
                text = "Diário",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() }
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    errorMessage: String? = null
) {
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecione o horário") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ==================== PREVIEWS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "Com Alarmes")
@Composable
fun AlarmScheduleScreenPreview() {
    AlarmScheduleScreen(
        medication = Medication(id = 1, name = "Losartana", dosage = "50mg", notes = "Tomar em jejum"),
        alarms = listOf(
            AlarmSchedule(id = 1, medicationId = 1, hour = 8, minute = 0, isEnabled = true),
            AlarmSchedule(id = 2, medicationId = 1, hour = 14, minute = 30, isEnabled = true),
            AlarmSchedule(id = 3, medicationId = 1, hour = 20, minute = 0, isEnabled = false)
        ),
        onNavigateBack = {},
        onAddAlarm = { _, _ -> },
        onDeleteAlarm = {},
        onToggleAlarm = {},
        onEditMedication = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "Sem Alarmes")
@Composable
fun AlarmScheduleScreenEmptyPreview() {
    AlarmScheduleScreen(
        medication = Medication(id = 2, name = "Vitamina D", dosage = "1 cápsula"),
        alarms = emptyList(),
        onNavigateBack = {},
        onAddAlarm = { _, _ -> },
        onDeleteAlarm = {},
        onToggleAlarm = {},
        onEditMedication = {}
    )
}