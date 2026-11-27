package com.swizima.stafftracker.data

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StaffTask(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val assignedTo: String,
    val dueDate: String,
    val status: String // PENDING, IN_PROGRESS, COMPLETED
)

object TaskRepository {
    val tasks = mutableStateListOf(
        StaffTask(
            id = "tsk_1",
            title = "Car Wash & Polish Inspection",
            category = "Car wash",
            description = "Complete full body washing and interior vacuum for vehicle UBN778N.",
            assignedTo = "JOSEPH AGABA",
            dueDate = "Today @ 04:00 PM",
            status = "IN_PROGRESS"
        ),
        StaffTask(
            id = "tsk_2",
            title = "Parking Spot Reservation Check",
            category = "Car park",
            description = "Verify reserved parking bay 14 at Lugogo Bypass facility.",
            assignedTo = "JOSEPH AGABA",
            dueDate = "Today @ 06:00 PM",
            status = "PENDING"
        ),
        StaffTask(
            id = "tsk_3",
            title = "Engine Diagnostic Review",
            category = "Repair",
            description = "Perform obd-ii error code diagnostic scan for vehicle KDG667D.",
            assignedTo = "JOSEPH AGABA",
            dueDate = "Yesterday",
            status = "COMPLETED"
        )
    )

    suspend fun updateTaskStatus(taskId: String, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val updated = tasks[index].copy(status = newStatus)
            withContext(Dispatchers.Main) {
                tasks[index] = updated
            }
            true
        } else false
    }
}
