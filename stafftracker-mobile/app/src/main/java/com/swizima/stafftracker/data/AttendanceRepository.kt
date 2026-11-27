package com.swizima.stafftracker.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AttendanceRecord(
    val id: String,
    val date: String,
    val checkInTime: String,
    val checkOutTime: String? = null,
    val locationName: String,
    val totalHours: String = "Active Shift",
    val status: String = "CHECKED_IN"
)

object AttendanceRepository {
    var isCheckedIn by mutableStateOf(false)
    var currentShiftStartTime by mutableStateOf<String?>(null)
    var activeLocationName by mutableStateOf("Plot 12, Industrial Area, Kampala")

    val attendanceHistory = mutableStateListOf<AttendanceRecord>(
        AttendanceRecord(
            id = "att_01",
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            checkInTime = "08:00 AM",
            checkOutTime = "05:00 PM",
            locationName = "Plot 12, Industrial Area, Kampala",
            totalHours = "9.0 hrs",
            status = "COMPLETED"
        )
    )

    suspend fun clockIn(location: String): Boolean = withContext(Dispatchers.IO) {
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val nowDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        activeLocationName = location
        currentShiftStartTime = nowTime
        isCheckedIn = true

        val newRecord = AttendanceRecord(
            id = "att_${System.currentTimeMillis()}",
            date = nowDate,
            checkInTime = nowTime,
            checkOutTime = null,
            locationName = location,
            totalHours = "Active Shift",
            status = "CHECKED_IN"
        )
        withContext(Dispatchers.Main) {
            attendanceHistory.add(0, newRecord)
        }
        true
    }

    suspend fun clockOut(): Boolean = withContext(Dispatchers.IO) {
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        isCheckedIn = false

        if (attendanceHistory.isNotEmpty()) {
            val first = attendanceHistory.first()
            if (first.status == "CHECKED_IN") {
                val updated = first.copy(
                    checkOutTime = nowTime,
                    totalHours = "8.5 hrs",
                    status = "COMPLETED"
                )
                withContext(Dispatchers.Main) {
                    attendanceHistory[0] = updated
                }
            }
        }
        true
    }
}
