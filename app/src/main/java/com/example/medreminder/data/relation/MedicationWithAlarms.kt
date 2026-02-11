package com.example.medreminder.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.medreminder.data.entity.AlarmSchedule
import com.example.medreminder.data.entity.Medication

data class MedicationWithAlarms(
    @Embedded val medication: Medication,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicationId"
    )
    val alarms: List<AlarmSchedule>
)