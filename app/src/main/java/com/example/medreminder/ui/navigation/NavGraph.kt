package com.example.medreminder.ui.navigation

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.medreminder.ui.screen.AlarmScheduleScreen
import com.example.medreminder.ui.screen.HistoryScreen
import com.example.medreminder.ui.screen.MedicationFormScreen
import com.example.medreminder.ui.screen.MedicationListScreen
import com.example.medreminder.ui.screen.PermissionScreen
import com.example.medreminder.viewmodel.AlarmViewModel
import com.example.medreminder.viewmodel.HistoryViewModel
import com.example.medreminder.viewmodel.MedicationViewModel
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    navController: NavHostController,
    medicationViewModel: MedicationViewModel,
    alarmViewModel: AlarmViewModel,
    historyViewModel: HistoryViewModel
) {
    val context = LocalContext.current
    val medicationsWithAlarms by medicationViewModel.medicationsWithAlarms.collectAsState()

    val allPermissionsGranted = remember { mutableStateOf(checkAllPermissions(context)) }

    val startDestination = if (allPermissionsGranted.value) "medication_list" else "permissions"

    NavHost(navController = navController, startDestination = startDestination) {

        // Tela 0: Permissões
        composable("permissions") {
            PermissionScreen(
                onAllGranted = {
                    allPermissionsGranted.value = true
                    navController.navigate("medication_list") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }

        // Tela 1: Lista de medicamentos
        composable("medication_list") {
            MedicationListScreen(
                medicationsWithAlarms = medicationsWithAlarms,
                onAddClick = { navController.navigate("medication_form/0") },
                onMedicationClick = { id -> navController.navigate("alarm_schedule/$id") },
                onDeleteClick = { item -> medicationViewModel.deleteMedication(item.medication) },
                onSettingsClick = { navController.navigate("permissions") },
                onHistoryClick = { navController.navigate("history") }
            )
        }

        // Tela 2: Formulário de cadastro/edição
        composable(
            route = "medication_form/{medicationId}",
            arguments = listOf(navArgument("medicationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getLong("medicationId")
            MedicationFormScreen(
                medicationId = medicationId,
                viewModel = medicationViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Tela 3: Alarmes do medicamento
        composable(
            route = "alarm_schedule/{medicationId}",
            arguments = listOf(navArgument("medicationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getLong("medicationId") ?: return@composable

            val medication by alarmViewModel.medication.collectAsState()
            val alarms by alarmViewModel.alarms.collectAsState()

            LaunchedEffect(medicationId) {
                alarmViewModel.loadMedication(medicationId)
            }

            AlarmScheduleScreen(
                medication = medication,
                alarms = alarms,
                onNavigateBack = { navController.popBackStack() },
                onAddAlarm = { hour, minute ->
                    alarmViewModel.addAlarm(medicationId, hour, minute)
                },
                onDeleteAlarm = { alarm -> alarmViewModel.deleteAlarm(alarm) },
                onToggleAlarm = { alarm -> alarmViewModel.toggleAlarm(alarm) },
                onEditMedication = { id -> navController.navigate("medication_form/$id") }
            )
        }

        // Tela 4: Histórico de doses
        composable("history") {
            val historyList by historyViewModel.allHistory.collectAsState(initial = emptyList())
            var filteredList by remember { mutableStateOf<List<com.example.medreminder.data.entity.DoseHistory>?>(null) }

            HistoryScreen(
                historyList = filteredList ?: historyList,
                onNavigateBack = { navController.popBackStack() },
                onDateSelected = { year, month, day ->
                    filteredList = null // Reset para mostrar loading
                    // Coletar dados filtrados
                    kotlinx.coroutines.MainScope().launch {
                        historyViewModel.getHistoryByDate(year, month, day).collect { list ->
                            filteredList = list
                        }
                    }
                }
            )
        }
    }
}

private fun checkAllPermissions(context: Context): Boolean {
    val notificationOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true

    val exactAlarmOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else true

    val batteryOk = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .isIgnoringBatteryOptimizations(context.packageName)

    return notificationOk && exactAlarmOk && batteryOk
}