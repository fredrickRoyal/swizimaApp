package com.example.swizima.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
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
    val records = mutableStateListOf<VehicleRecord>()
    var apiErrorMessage by mutableStateOf<String?>(null)

    private val currentDateFormatted: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    suspend fun fetchRecordsFromApi(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/client-transactions/vehicle-services")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val fetchedRecords = mutableListOf<VehicleRecord>()

                val idMatches = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val plateMatches = Regex("\"vehicleNumberPlate\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val serviceMatches = Regex("\"serviceType\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val clientMatches = Regex("\"customerName\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val phoneMatches = Regex("\"customerPhoneNumber\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val amountMatches = Regex("\"amountCharged\"\\s*:\\s*([0-9.]+)").findAll(json).toList()
                val assignedMatches = Regex("\"assignedTo\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val commentMatches = Regex("\"generalComment\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val ids = idMatches.map { it.groupValues[1] }
                val plates = plateMatches.map { it.groupValues[1] }
                val services = serviceMatches.map { it.groupValues[1] }
                val clients = clientMatches.map { it.groupValues[1] }
                val phones = phoneMatches.map { it.groupValues[1] }
                val amounts = amountMatches.map { it.groupValues[1] }
                val assigned = assignedMatches.map { it.groupValues[1] }
                val comments = commentMatches.map { it.groupValues[1] }

                for (i in plates.indices) {
                    fetchedRecords.add(
                        VehicleRecord(
                            id = ids.getOrElse(i) { java.util.UUID.randomUUID().toString() },
                            numberPlate = plates[i],
                            serviceType = services.getOrElse(i) { "Parking" },
                            date = currentDateFormatted,
                            entryTime = "08:30",
                            clientName = clients.getOrElse(i) { "CLIENT" },
                            clientPhoneNumbers = listOf(phones.getOrElse(i) { "+256 700 000 000" }),
                            isLeavingKey = true,
                            assignedToPerson = assigned.getOrElse(i) { "ATTENDANT" },
                            generalComment = comments.getOrElse(i) { "" },
                            amountCharged = amounts.getOrElse(i) { "0" },
                            status = "Active"
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    records.clear()
                    records.addAll(fetchedRecords)
                }
                apiErrorMessage = null
                true
            } else {
                apiErrorMessage = "Failed to fetch vehicle records from API (Server response: $responseCode)"
                false
            }
        } catch (e: Exception) {
            apiErrorMessage = "API Connection Failed: ${e.localizedMessage ?: "Unable to connect to ${ApiGatewayConfig.gatewayBaseUrl}"}"
            false
        }
    }

    suspend fun addRecord(record: VehicleRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/client-transactions/vehicle-services")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                records.add(0, record)
                apiErrorMessage = null
                true
            } else {
                apiErrorMessage = "Failed to save record to API (Server response: $responseCode)"
                false
            }
        } catch (e: Exception) {
            apiErrorMessage = "API Connection Failed: ${e.localizedMessage ?: "Unable to connect to ${ApiGatewayConfig.gatewayBaseUrl}"}"
            false
        }
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
        val keyword = when {
            serviceType.contains("park", ignoreCase = true) -> "park"
            serviceType.contains("wash", ignoreCase = true) -> "wash"
            serviceType.contains("repair", ignoreCase = true) -> "repair"
            serviceType.contains("review", ignoreCase = true) || serviceType.contains("check", ignoreCase = true) -> "review"
            serviceType.contains("maintenance", ignoreCase = true) -> "maintenance"
            else -> serviceType
        }
        return records.filter { record ->
            record.serviceType.equals(serviceType, ignoreCase = true) ||
                    record.serviceType.contains(keyword, ignoreCase = true) ||
                    keyword.contains(record.serviceType, ignoreCase = true)
        }
    }
}
