package com.example.medreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.medreminder.ui.navigation.NavGraph
import com.example.medreminder.ui.theme.MedReminderTheme
import com.example.medreminder.viewmodel.AlarmViewModel
import com.example.medreminder.viewmodel.MedicationViewModel

class MainActivity : ComponentActivity() {

    private val medicationViewModel: MedicationViewModel by viewModels()
    private val alarmViewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        medicationViewModel = medicationViewModel,
                        alarmViewModel = alarmViewModel
                    )
                }
            }
        }
    }
}