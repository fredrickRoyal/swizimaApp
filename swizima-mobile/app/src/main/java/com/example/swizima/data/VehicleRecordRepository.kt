package com.example.swizima.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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
    // Zero dummy data - records start empty and are populated from the API Gateway
    val records = mutableStateListOf<VehicleRecord>()
    var apiErrorMessage by mutableStateOf<String?>(null)

    suspend fun fetchRecordsFromApi(): Boolean = withContext(Dispatchers.IO) {
        if (ApiGatewayConfig.isSimulationMode) {
            return@withContext true
        }
        try {
            // Correct backend endpoint matching swizima-web SwizimaApi (/api/v1/client-transactions/vehicle-services)
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/client-transactions/vehicle-services")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
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
        if (ApiGatewayConfig.isSimulationMode) {
            records.add(0, record)
            return@withContext true
        }
        try {
            // Correct backend endpoint matching swizima-web SwizimaApi (/api/v1/client-transactions/vehicle-services)
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
        return records.filter { it.serviceType.equals(serviceType, ignoreCase = true) }
    }
}
