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
    val paymentStatus: String = "PENDING",
    val clearanceStatus: String = "IN_SERVICE",
    val status: String = "In Service"
)

object VehicleRecordRepository {
    val records = mutableStateListOf<VehicleRecord>()
    var apiErrorMessage by mutableStateOf<String?>(null)

    private val currentDateFormatted: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun extractContentJson(json: String): String {
        val contentIndex = json.indexOf("\"content\"")
        if (contentIndex != -1) {
            val bracketIndex = json.indexOf('[', contentIndex)
            val closingBracketIndex = json.lastIndexOf(']')
            if (bracketIndex != -1 && closingBracketIndex != -1 && closingBracketIndex > bracketIndex) {
                return json.substring(bracketIndex, closingBracketIndex + 1)
            }
        }
        return json
    }

    suspend fun fetchRecordsFromApi(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/client-transactions/vehicle-services?page=0&size=50&sort=createdAt,desc")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                val json = extractContentJson(rawJson)
                val fetchedRecords = mutableListOf<VehicleRecord>()

                val idMatches = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val plateMatches = Regex("\"vehicleNumberPlate\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val serviceMatches = Regex("\"serviceType\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val clientMatches = Regex("\"customerName\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val phoneMatches = Regex("\"customerPhoneNumber\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val amountMatches = Regex("\"amountCharged\"\\s*:\\s*([0-9.]+)").findAll(json).toList()
                val assignedMatches = Regex("\"assignedTo\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val commentMatches = Regex("\"generalComment\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val paymentStatusMatches = Regex("\"paymentStatus\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val clearanceMatches = Regex("\"vehicleClearanceStatus\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val entryDateMatches = Regex("\"entryDate\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val entryTimeMatches = Regex("\"entryTime\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val entryDateTimeMatches = Regex("\"entryDateTime\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val expectedExitMatches = Regex("\"expectedExitTime\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val actualExitMatches = Regex("\"actualExitDateTime\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val ids = idMatches.map { it.groupValues[1] }
                val plates = plateMatches.map { it.groupValues[1] }
                val services = serviceMatches.map { it.groupValues[1] }
                val clients = clientMatches.map { it.groupValues[1] }
                val phones = phoneMatches.map { it.groupValues[1] }
                val amounts = amountMatches.map { it.groupValues[1] }
                val assigned = assignedMatches.map { it.groupValues[1] }
                val comments = commentMatches.map { it.groupValues[1] }
                val paymentStatuses = paymentStatusMatches.map { it.groupValues[1] }
                val clearances = clearanceMatches.map { it.groupValues[1] }

                val entryDates = entryDateMatches.map { it.groupValues[1] }
                val entryTimes = entryTimeMatches.map { it.groupValues[1] }
                val entryDateTimes = entryDateTimeMatches.map { it.groupValues[1] }
                val expectedExits = expectedExitMatches.map { it.groupValues[1] }
                val actualExits = actualExitMatches.map { it.groupValues[1] }

                for (i in plates.indices) {
                    val rawAmount = amounts.getOrElse(i) { "0" }.toDoubleOrNull() ?: 0.0
                    val formattedAmount = java.text.NumberFormat.getNumberInstance(Locale.US).format(rawAmount.toLong())
                    val payStatus = paymentStatuses.getOrElse(i) { "PENDING" }
                    val clearStatus = clearances.getOrElse(i) { "IN_SERVICE" }
                    val displayStatus = clearStatus.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

                    val rawEntryDateTime = entryDateTimes.getOrNull(i) ?: ""
                    val parsedDate = if (i < entryDates.size) {
                        entryDates[i]
                    } else if (rawEntryDateTime.length >= 10) {
                        rawEntryDateTime.substring(0, 10)
                    } else {
                        currentDateFormatted
                    }

                    val parsedTime = if (i < entryTimes.size) {
                        entryTimes[i].take(5)
                    } else if (rawEntryDateTime.length >= 16) {
                        rawEntryDateTime.substring(11, 16)
                    } else {
                        "08:30"
                    }

                    val rawExit = actualExits.getOrNull(i) ?: expectedExits.getOrNull(i) ?: ""
                    val parsedExit = if (rawExit.length >= 16) rawExit.substring(11, 16) else if (rawExit.length >= 5) rawExit.take(5) else ""

                    fetchedRecords.add(
                        VehicleRecord(
                            id = ids.getOrElse(i) { java.util.UUID.randomUUID().toString() },
                            numberPlate = plates[i],
                            serviceType = services.getOrElse(i) { "Parking" },
                            date = parsedDate,
                            entryTime = parsedTime,
                            expectedExitTime = parsedExit,
                            clientName = clients.getOrElse(i) { "CLIENT" },
                            clientPhoneNumbers = listOf(phones.getOrElse(i) { "+256 700 000 000" }),
                            isLeavingKey = true,
                            assignedToPerson = assigned.getOrElse(i) { "ATTENDANT" },
                            generalComment = comments.getOrElse(i) { "" },
                            amountCharged = formattedAmount,
                            paymentStatus = payStatus,
                            clearanceStatus = clearStatus,
                            status = displayStatus
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
            val primaryPhone = record.clientPhoneNumbers.firstOrNull() ?: ""
            val altPhone = record.clientPhoneNumbers.getOrNull(1) ?: ""
            val cleanAmount = record.amountCharged.replace(",", "").toDoubleOrNull() ?: 0.0
            val clientRefId = if (ClientServiceRepository.activeClientId.isNotBlank()) ClientServiceRepository.activeClientId else "219e77a7-b398-4829-b06f-5978193fe9c5"

            val expectedExitField = if (record.expectedExitTime.isNotBlank()) {
                val timeStr = if (record.expectedExitTime.length == 5) "${record.expectedExitTime}:00" else record.expectedExitTime
                "\"expectedExitTime\": \"${record.date}T$timeStr\","
            } else {
                ""
            }

            val entryTimeStr = if (record.entryTime.length == 5) "${record.entryTime}:00" else record.entryTime

            val payload = """
                {
                  "clientRef": "$clientRefId",
                  "customerPhoneNumber": "$primaryPhone",
                  "customerAlternativePhone": "$altPhone",
                  "vehicleNumberPlate": "${record.numberPlate}",
                  "serviceType": "${record.serviceType}",
                  "amountCharged": $cleanAmount,
                  "assignedTo": "${record.assignedToPerson}",
                  "vehicleKeyShared": ${record.isLeavingKey},
                  "entryDateTime": "${record.date}T$entryTimeStr",
                  $expectedExitField
                  "customerName": "${record.clientName}",
                  "customerNationality": "${record.nationality}",
                  "customerIdNumber": "${record.idPassportNumber}",
                  "generalComment": "${record.generalComment}",
                  "paymentStatus": "${record.paymentStatus}",
                  "vehicleClearanceStatus": "${record.clearanceStatus}"
                }
            """.trimIndent()

            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                records.add(0, record)
                apiErrorMessage = null
                true
            } else {
                val errorMsg = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) {
                    ""
                }
                apiErrorMessage = "Failed to save record (Server $responseCode: $errorMsg)"
                false
            }
        } catch (e: Exception) {
            apiErrorMessage = "API Connection Failed: ${e.localizedMessage ?: "Unable to connect to ${ApiGatewayConfig.gatewayBaseUrl}"}"
            false
        }
    }

    suspend fun addPayment(transactionId: String, amount: Double, method: String, reference: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/client-transactions/vehicle-services/$transactionId/payments")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val payload = """
                {
                  "amount": $amount,
                  "paymentMethod": "$method",
                  "referenceNumber": "${reference ?: ""}",
                  "paymentDateTime": "${currentDateFormatted}T00:00:00"
                }
            """.trimIndent()
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            val success = responseCode in 200..299
            if (success) {
                try {
                    clearVehicle(transactionId, "Auto System", "Auto-cleared upon payment")
                } catch (_: Exception) {}
            }
            success
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearVehicle(transactionId: String, by: String?, comment: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/client-transactions/vehicle-services/$transactionId/clear")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val payload = """
                {
                  "by": "${by ?: "Attendant"}",
                  "comment": "${comment ?: "Cleared successfully"}"
                }
            """.trimIndent()
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exitVehicle(transactionId: String, by: String?, comment: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/client-transactions/vehicle-services/$transactionId/exit")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val payload = """
                {
                  "by": "${by ?: "Attendant"}",
                  "comment": "${comment ?: "Exited successfully"}"
                }
            """.trimIndent()
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (_: Exception) {
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
