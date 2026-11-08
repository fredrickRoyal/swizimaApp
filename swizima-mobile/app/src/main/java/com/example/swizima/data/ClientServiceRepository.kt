package com.example.swizima.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ApiClient(
    val id: String,
    val clientName: String,
    val clientCode: String
)

data class ApiServiceCatalogItem(
    val id: String,
    val clientId: String,
    val clientName: String,
    val serviceName: String,
    val category: String,
    val price: Double,
    val currency: String,
    val description: String?
)

data class ApiClientStaff(
    val id: String,
    val clientId: String,
    val fullName: String,
    val position: String?,
    val phoneNumber: String?
)

object ClientServiceRepository {
    var apiErrorMessage by mutableStateOf<String?>(null)
    var activeClientId: String = "219e77a7-b398-4829-b06f-5978193fe9c5"

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

    suspend fun fetchClients(): List<ApiClient> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/clients?page=0&size=50&sort=createdAt,desc")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                val json = extractContentJson(rawJson)
                val clients = mutableListOf<ApiClient>()
                val idMatches = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val nameMatches = Regex("\"clientName\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val codeMatches = Regex("\"clientCode\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val ids = idMatches.map { it.groupValues[1] }
                val names = nameMatches.map { it.groupValues[1] }
                val codes = codeMatches.map { it.groupValues[1] }

                for (i in names.indices) {
                    clients.add(
                        ApiClient(
                            id = ids.getOrElse(i) { "id_$i" },
                            clientName = names[i],
                            clientCode = codes.getOrElse(i) { "SWZ-0$i" }
                        )
                    )
                }
                if (clients.isNotEmpty()) {
                    activeClientId = clients.first().id
                }
                apiErrorMessage = null
                clients
            } else {
                apiErrorMessage = "Failed to fetch clients (Server response: ${connection.responseCode})"
                emptyList()
            }
        } catch (e: Exception) {
            apiErrorMessage = "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/clients"}"
            emptyList()
        }
    }

    suspend fun fetchServiceCatalogs(clientId: String?): List<ApiServiceCatalogItem> = withContext(Dispatchers.IO) {
        try {
            val urlStr = buildString {
                append("${ApiGatewayConfig.gatewayBaseUrl}/service-catalogs?")
                val params = mutableListOf<String>("page=0", "size=50", "sort=createdAt,desc")
                val targetClientId = if (!clientId.isNullOrBlank()) clientId else activeClientId
                if (targetClientId.isNotBlank()) params.add("clientId=$targetClientId")
                append(params.joinToString("&"))
            }
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                val json = extractContentJson(rawJson)
                val items = mutableListOf<ApiServiceCatalogItem>()
                val idMatches = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val nameMatches = Regex("\"serviceName\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val catMatches = Regex("\"category\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val priceMatches = Regex("\"price\"\\s*:\\s*([0-9.]+)").findAll(json).toList()
                val currMatches = Regex("\"currency\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val ids = idMatches.map { it.groupValues[1] }
                val names = nameMatches.map { it.groupValues[1] }
                val cats = catMatches.map { it.groupValues[1] }
                val prices = priceMatches.map { it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                val currs = currMatches.map { it.groupValues[1] }

                for (i in names.indices) {
                    items.add(
                        ApiServiceCatalogItem(
                            id = ids.getOrElse(i) { "s_$i" },
                            clientId = clientId ?: activeClientId,
                            clientName = "Swizima Motors",
                            serviceName = names[i],
                            category = cats.getOrElse(i) { "Parking" },
                            price = prices.getOrElse(i) { 10000.0 },
                            currency = currs.getOrElse(i) { "UGX" },
                            description = null
                        )
                    )
                }
                apiErrorMessage = null
                items
            } else {
                apiErrorMessage = "Failed to fetch service catalogs (Server response: ${connection.responseCode})"
                emptyList()
            }
        } catch (e: Exception) {
            apiErrorMessage = "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/service-catalogs"}"
            emptyList()
        }
    }

    suspend fun fetchClientStaff(clientId: String): List<ApiClientStaff> = withContext(Dispatchers.IO) {
        val targetId = if (clientId.isNotBlank()) clientId else activeClientId
        if (targetId.isBlank()) return@withContext emptyList()
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/clients/$targetId/staff?page=0&size=50&sort=createdAt,desc")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                val json = extractContentJson(rawJson)
                val staffList = mutableListOf<ApiClientStaff>()
                val idMatches = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val nameMatches = Regex("\"fullName\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val posMatches = Regex("\"position\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val phoneMatches = Regex("\"phoneNumber\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val ids = idMatches.map { it.groupValues[1] }
                val names = nameMatches.map { it.groupValues[1] }
                val positions = posMatches.map { it.groupValues[1] }
                val phones = phoneMatches.map { it.groupValues[1] }

                for (i in names.indices) {
                    staffList.add(
                        ApiClientStaff(
                            id = ids.getOrElse(i) { "st_$i" },
                            clientId = targetId,
                            fullName = names[i],
                            position = positions.getOrElse(i) { "Staff" },
                            phoneNumber = phones.getOrElse(i) { "" }
                        )
                    )
                }
                apiErrorMessage = null
                staffList
            } else {
                apiErrorMessage = "Failed to fetch client staff (Server response: ${connection.responseCode})"
                emptyList()
            }
        } catch (e: Exception) {
            apiErrorMessage = "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/clients/$targetId/staff"}"
            emptyList()
        }
    }
}
