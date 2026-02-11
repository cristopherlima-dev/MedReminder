package com.example.medreminder.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.medreminder.data.entity.Medication
import com.example.medreminder.viewmodel.MedicationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationFormScreen(
    medicationId: Long?,
    viewModel: MedicationViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditing = medicationId != null && medicationId > 0

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(!isEditing) }

    LaunchedEffect(medicationId) {
        if (isEditing && medicationId != null) {
            val med = viewModel.getMedicationById(medicationId)
            if (med != null) {
                name = med.name
                dosage = med.dosage
                notes = med.notes
            }
            isLoaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Medicamento" else "Novo Medicamento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do medicamento *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosagem (ex: 500mg, 1 comprimido)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                if (isEditing && medicationId != null) {
                                    viewModel.updateMedication(
                                        Medication(
                                            id = medicationId,
                                            name = name.trim(),
                                            dosage = dosage.trim(),
                                            notes = notes.trim()
                                        )
                                    )
                                } else {
                                    viewModel.addMedication(
                                        name.trim(),
                                        dosage.trim(),
                                        notes.trim()
                                    )
                                }
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text(if (isEditing) "Salvar Alterações" else "Cadastrar Medicamento")
                }
            }
        }
    }
}

// ==================== PREVIEWS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationFormPreviewContent(
    title: String,
    buttonText: String,
    buttonEnabled: Boolean,
    initialName: String = "",
    initialDosage: String = "",
    initialNotes: String = ""
) {
    var name by remember { mutableStateOf(initialName) }
    var dosage by remember { mutableStateOf(initialDosage) }
    var notes by remember { mutableStateOf(initialNotes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do medicamento *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosagem (ex: 500mg, 1 comprimido)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = buttonEnabled
            ) {
                Text(buttonText)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Novo Medicamento")
@Composable
fun MedicationFormScreenNewPreview() {
    MedicationFormPreviewContent(
        title = "Novo Medicamento",
        buttonText = "Cadastrar Medicamento",
        buttonEnabled = false
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "Editar Medicamento")
@Composable
fun MedicationFormScreenEditPreview() {
    MedicationFormPreviewContent(
        title = "Editar Medicamento",
        buttonText = "Salvar Alterações",
        buttonEnabled = true,
        initialName = "Losartana",
        initialDosage = "50mg",
        initialNotes = "Tomar em jejum"
    )
}