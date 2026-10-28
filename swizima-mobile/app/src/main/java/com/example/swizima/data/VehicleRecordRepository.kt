package com.example.swizima.data

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VehicleRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val numberPlate: String,
    val serviceType: String,
    val date: String,
    val entryTime: String,
    val expectedExitTime: String = "",
    val clientName: String,
    val nationality: String = "Ugandan",
    val idType: String = "National ID",
    val idPassportNumber: String = "",
    val clientPhoneNumbers: List<String>,
    val isLeavingKey: Boolean,
    val assignedToPerson: String = "",
    val generalComment: String = "",
    val amountCharged: String = "",
    val status: String = "Active"
)

object VehicleRecordRepository {
    private val currentDateFormatted: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val records = mutableStateListOf(
        VehicleRecord(
            numberPlate = "KCA 789B",
            serviceType = "Parking",
            date = currentDateFormatted,
            entryTime = "08:30",
            expectedExitTime = "17:00",
            clientName = "KASOMA, FREDRICK",
            nationality = "Ugandan",
            idType = "National ID",
            idPassportNumber = "CM90061102G2QC",
            clientPhoneNumbers = listOf("+256 712 345 678", "+256 799 112 233"),
            isLeavingKey = true,
            assignedToPerson = "JAMES (VALET)",
            generalComment = "Parked at Spot A-12",
            amountCharged = "20,000",
            status = "Active"
        ),
        VehicleRecord(
            numberPlate = "KDD 456Z",
            serviceType = "Parking",
            date = currentDateFormatted,
            entryTime = "09:15",
            expectedExitTime = "18:00",
            clientName = "MUKASA, DAVID",
            nationality = "Ugandan",
            idType = "Driving Licence",
            idPassportNumber = "DL987654",
            clientPhoneNumbers = listOf("+256 733 987 654"),
            isLeavingKey = false,
            assignedToPerson = "MARK (ATTENDANT)",
            generalComment = "Client keeps key",
            amountCharged = "15,000",
            status = "Active"
        ),
        VehicleRecord(
            numberPlate = "KCY 101X",
            serviceType = "General washing",
            date = currentDateFormatted,
            entryTime = "10:00",
            expectedExitTime = "11:30",
            clientName = "WANJIKU, ALICE",
            nationality = "Kenyan",
            idType = "National ID",
            idPassportNumber = "ID32145678",
            clientPhoneNumbers = listOf("+254 700 123 999"),
            isLeavingKey = true,
            assignedToPerson = "PETER (WASHER)",
            generalComment = "Full body foam wash & interior vacuum",
            amountCharged = "25,000",
            status = "In Progress"
        ),
        VehicleRecord(
            numberPlate = "KBZ 555M",
            serviceType = "Repair",
            date = currentDateFormatted,
            entryTime = "11:20",
            expectedExitTime = "16:30",
            clientName = "OMONDI, ROBERT",
            nationality = "Kenyan",
            idType = "Driving Licence",
            idPassportNumber = "DL554433",
            clientPhoneNumbers = listOf("+254 722 444 888", "+254 711 000 111"),
            isLeavingKey = true,
            assignedToPerson = "ALEX (MECHANIC)",
            generalComment = "Brake pad replacement and wheel alignment",
            amountCharged = "150,000",
            status = "In Progress"
        ),
        VehicleRecord(
            numberPlate = "KDA 321P",
            serviceType = "Review",
            date = currentDateFormatted,
            entryTime = "12:00",
            expectedExitTime = "13:30",
            clientName = "MUTHONI, GRACE",
            nationality = "Kenyan",
            idType = "National ID",
            idPassportNumber = "ID77889900",
            clientPhoneNumbers = listOf("+254 788 555 222"),
            isLeavingKey = true,
            assignedToPerson = "DANIEL (INSPECTOR)",
            generalComment = "15-Point pre-trip vehicle safety inspection",
            amountCharged = "40,000",
            status = "Active"
        ),
        VehicleRecord(
            numberPlate = "KCB 999K",
            serviceType = "General Service",
            date = currentDateFormatted,
            entryTime = "07:45",
            expectedExitTime = "14:00",
            clientName = "SCOTT, MICHAEL",
            nationality = "Ugandan",
            idType = "Passport",
            idPassportNumber = "A1234567",
            clientPhoneNumbers = listOf("+256 755 333 111"),
            isLeavingKey = true,
            assignedToPerson = "JOHN (TEAM LEAD)",
            generalComment = "Oil filter, air filter, and engine tune-up",
            amountCharged = "120,000",
            status = "Active"
        )
    )

    fun addRecord(record: VehicleRecord) {
        records.add(0, record) // Insert at top
    }

    fun getRecordById(id: String): VehicleRecord? {
        return records.find { it.id == id }
    }

    fun updateRecord(updatedRecord: VehicleRecord) {
        val index = records.indexOfFirst { it.id == updatedRecord.id }
        if (index != -1) {
            records[index] = updatedRecord
        }
    }

    fun getRecordsForService(serviceType: String): List<VehicleRecord> {
        return records.filter { it.serviceType.equals(serviceType, ignoreCase = true) }
    }
}
