package com.example.medreminder.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medreminder.data.entity.DoseHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyList: List<DoseHistory>,
    onNavigateBack: () -> Unit,
    onDateSelected: ((Int, Int, Int) -> Unit)? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateLabel by remember { mutableStateOf("Todas as doses") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 Histórico de Doses") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Filtrar por data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Label do filtro
            Text(
                text = selectedDateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📋",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhum registro encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "O histórico será preenchido\nconforme você confirmar os alarmes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Agrupar por dia
                val grouped = historyList.groupBy { dose ->
                    val cal = Calendar.getInstance().apply { timeInMillis = dose.takenAt }
                    val sdf = SimpleDateFormat("dd/MM/yyyy (EEEE)", Locale("pt", "BR"))
                    sdf.format(cal.time)
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (dateLabel, doses) ->
                        item {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(doses, key = { it.id }) { dose ->
                            DoseHistoryCard(dose)
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // DatePicker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance().apply { timeInMillis = millis }
                            val year = cal.get(Calendar.YEAR)
                            val month = cal.get(Calendar.MONTH)
                            val day = cal.get(Calendar.DAY_OF_MONTH)

                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                            selectedDateLabel = "Filtrado: ${sdf.format(cal.time)}"

                            onDateSelected?.invoke(year, month, day)
                        }
                        showDatePicker = false
                    }) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun DoseHistoryCard(dose: DoseHistory) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val takenTime = timeFormat.format(Date(dose.takenAt))

    val statusEmoji = when (dose.status) {
        "TAKEN" -> "✅"
        "MISSED" -> "❌"
        "SNOOZED" -> "⏰"
        "DISMISSED" -> "🚫"
        else -> "❓"
    }

    val statusText = when (dose.status) {
        "TAKEN" -> "Tomou"
        "MISSED" -> "Perdeu"
        "SNOOZED" -> {
            if (dose.snoozedTo != null) {
                val snoozedTime = timeFormat.format(Date(dose.snoozedTo))
                "Adiou para $snoozedTime"
            } else {
                "Adiou"
            }
        }
        "DISMISSED" -> "Optou por não tomar"
        else -> dose.status
    }

    val statusColor = when (dose.status) {
        "TAKEN" -> MaterialTheme.colorScheme.primary
        "SNOOZED" -> MaterialTheme.colorScheme.tertiary
        "MISSED" -> MaterialTheme.colorScheme.error
        "DISMISSED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusEmoji,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dose.medicationName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Agendado: %02d:%02d".format(dose.scheduledHour, dose.scheduledMinute),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = takenTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "Com Histórico")
@Composable
fun HistoryScreenPreview() {
    val now = System.currentTimeMillis()
    HistoryScreen(
        historyList = listOf(
            DoseHistory(id = 1, medicationId = 1, medicationName = "Losartana", scheduledHour = 8, scheduledMinute = 0, takenAt = now - 3600000, status = "TAKEN"),
            DoseHistory(id = 2, medicationId = 2, medicationName = "Vitamina D", scheduledHour = 12, scheduledMinute = 0, takenAt = now - 1800000, status = "SNOOZED", snoozedTo = now - 1500000),
            DoseHistory(id = 3, medicationId = 1, medicationName = "Losartana", scheduledHour = 20, scheduledMinute = 0, takenAt = now - 86400000, status = "TAKEN"),
            DoseHistory(id = 4, medicationId = 3, medicationName = "Omega 3", scheduledHour = 14, scheduledMinute = 30, takenAt = now - 86400000, status = "MISSED"),
            DoseHistory(id = 5, medicationId = 3, medicationName = "Omega 3", scheduledHour = 23, scheduledMinute = 0, takenAt = now - 80000000, status = "DISMISSED")
        ),
        onNavigateBack = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "Sem Histórico")
@Composable
fun HistoryScreenEmptyPreview() {
    HistoryScreen(
        historyList = emptyList(),
        onNavigateBack = {}
    )
}