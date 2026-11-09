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
    val clientCode: String,
    val address: String,
    val phone: String
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

data class ApiBillingWallet(
    val balance: Double = 8500.0,
    val totalCredited: Double = 10000.0,
    val totalCharged: Double = 1500.0,
    val percentageRate: Double = 5.0,
    val currency: String = "UGX",
    val lowBalanceThreshold: Double = 1000.0,
    val planStatus: String = "ACTIVE"
)

data class ApiCreditRequest(
    val id: String = "",
    val requestedAmount: Double = 0.0,
    val paymentMethod: String = "MOBILE_MONEY",
    val paymentReference: String? = null,
    val payerPhone: String? = null,
    val status: String = "PENDING",
    val createdAt: String = "",
    val notes: String? = null
)

data class ApiCreditLoad(
    val id: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "MOBILE_MONEY",
    val paymentReference: String? = null,
    val loadedAt: String = "",
    val notes: String? = null
)

object ClientServiceRepository {
    var apiErrorMessage by mutableStateOf<String?>(null)
    var activeClientId: String = "219e77a7-b398-4829-b06f-5978193fe9c5"
    var activeClientName: String = "Swizima Motors"
    var activeClientAddress: String = "Kampala, Uganda"
    var activeClientPhone: String = "+256 700 123 456"

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
                val addressMatches = Regex("\"address\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val cityMatches = Regex("\"city\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val phoneMatches = Regex("\"phoneNumber\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val ids = idMatches.map { it.groupValues[1] }
                val names = nameMatches.map { it.groupValues[1] }
                val codes = codeMatches.map { it.groupValues[1] }
                val addresses = addressMatches.map { it.groupValues[1] }
                val cities = cityMatches.map { it.groupValues[1] }
                val phones = phoneMatches.map { it.groupValues[1] }

                for (i in names.indices) {
                    val addr = addresses.getOrNull(i) ?: cities.getOrNull(i) ?: "Kampala, Uganda"
                    clients.add(
                        ApiClient(
                            id = ids.getOrElse(i) { "id_$i" },
                            clientName = names[i],
                            clientCode = codes.getOrElse(i) { "SWZ-0$i" },
                            address = addr,
                            phone = phones.getOrNull(i) ?: ""
                        )
                    )
                }
                if (clients.isNotEmpty()) {
                    val first = clients.first()
                    activeClientId = first.id
                    activeClientName = first.clientName
                    activeClientAddress = first.address.ifBlank { "Kampala, Uganda" }
                    activeClientPhone = if (first.phone.isNotBlank()) first.phone else "+256 700 123 456"
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

    suspend fun fetchBillingWallet(clientId: String?): ApiBillingWallet = withContext(Dispatchers.IO) {
        val targetId = if (!clientId.isNullOrBlank()) clientId else activeClientId
        val defaultWallet = ApiBillingWallet()
        if (targetId.isBlank()) return@withContext defaultWallet
        try {
            val url = URL("${ApiGatewayConfig.paymentGatewayBaseUrl}/billing/wallets/client/$targetId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }

                val balanceMatch = Regex("\"balance\"\\s*:\\s*([0-9.]+)").find(json)
                val creditedMatch = Regex("\"totalCredited\"\\s*:\\s*([0-9.]+)").find(json)
                val chargedMatch = Regex("\"totalCharged\"\\s*:\\s*([0-9.]+)").find(json)
                val rateMatch = Regex("\"percentageRate\"\\s*:\\s*([0-9.]+)").find(json)
                val currencyMatch = Regex("\"currency\"\\s*:\\s*\"([^\"]+)\"").find(json)
                val thresholdMatch = Regex("\"lowBalanceThreshold\"\\s*:\\s*([0-9.]+)").find(json)
                val planMatch = Regex("\"planStatus\"\\s*:\\s*\"([^\"]+)\"").find(json)

                val balance = balanceMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 8500.0
                val credited = creditedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 10000.0
                val charged = chargedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 1500.0
                val rate = rateMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 5.0
                val currency = currencyMatch?.groupValues?.get(1) ?: "UGX"
                val threshold = thresholdMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 1000.0
                val status = planMatch?.groupValues?.get(1) ?: "ACTIVE"

                ApiBillingWallet(
                    balance = balance,
                    totalCredited = credited,
                    totalCharged = charged,
                    percentageRate = rate,
                    currency = currency,
                    lowBalanceThreshold = threshold,
                    planStatus = status
                )
            } else {
                defaultWallet
            }
        } catch (_: Exception) {
            defaultWallet
        }
    }

    suspend fun submitCreditRequest(
        clientId: String?,
        amount: Double,
        paymentMethod: String,
        paymentReference: String?,
        phone: String?,
        notes: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val targetId = if (!clientId.isNullOrBlank()) clientId else activeClientId
        if (targetId.isBlank()) return@withContext false
        try {
            val url = URL("${ApiGatewayConfig.paymentGatewayBaseUrl}/billing/wallets/client/$targetId/credit-requests")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val refVal = if (paymentReference.isNullOrBlank()) "null" else "\"$paymentReference\""
            val nameVal = if (activeClientName.isBlank()) "null" else "\"$activeClientName\""
            val phoneVal = if (phone.isNullOrBlank()) "null" else "\"$phone\""
            val notesVal = if (notes.isNullOrBlank()) "null" else "\"$notes\""

            val payload = """
                {
                  "requestedAmount": $amount,
                  "paymentMethod": "$paymentMethod",
                  "paymentReference": $refVal,
                  "payerName": $nameVal,
                  "payerPhone": $phoneVal,
                  "notes": $notesVal
                }
            """.trimIndent()
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                apiErrorMessage = null
                true
            } else {
                val errorMsg = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) { "" }
                apiErrorMessage = "Credit request failed (Server $responseCode: $errorMsg)"
                false
            }
        } catch (e: Exception) {
            apiErrorMessage = "API Connection Failed: ${e.localizedMessage}"
            false
        }
    }

    suspend fun fetchCreditRequests(clientId: String?): List<ApiCreditRequest> = withContext(Dispatchers.IO) {
        val targetId = if (!clientId.isNullOrBlank()) clientId else activeClientId
        if (targetId.isBlank()) return@withContext emptyList()
        try {
            val url = URL("${ApiGatewayConfig.paymentGatewayBaseUrl}/billing/credit-requests?clientRef=$targetId&page=0&size=50&sort=createdAt,desc")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                val json = extractContentJson(rawJson)
                val items = mutableListOf<ApiCreditRequest>()

                val idMatches = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val amountMatches = Regex("\"requestedAmount\"\\s*:\\s*([0-9.]+)").findAll(json).toList()
                val methodMatches = Regex("\"paymentMethod\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val statusMatches = Regex("\"status\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val dateMatches = Regex("\"createdAt\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val refMatches = Regex("\"paymentReference\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val ids = idMatches.map { it.groupValues[1] }
                val amounts = amountMatches.map { it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                val methods = methodMatches.map { it.groupValues[1] }
                val statuses = statusMatches.map { it.groupValues[1] }
                val dates = dateMatches.map { it.groupValues[1] }
                val refs = refMatches.map { it.groupValues[1] }

                for (i in amounts.indices) {
                    items.add(
                        ApiCreditRequest(
                            id = ids.getOrElse(i) { "cr_$i" },
                            requestedAmount = amounts[i],
                            paymentMethod = methods.getOrElse(i) { "MOBILE_MONEY" },
                            status = statuses.getOrElse(i) { "PENDING" },
                            createdAt = dates.getOrElse(i) { "" }.take(10),
                            paymentReference = refs.getOrNull(i)
                        )
                    )
                }
                items
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchCreditLoads(clientId: String?): List<ApiCreditLoad> = withContext(Dispatchers.IO) {
        val targetId = if (!clientId.isNullOrBlank()) clientId else activeClientId
        if (targetId.isBlank()) return@withContext emptyList()
        try {
            val url = URL("${ApiGatewayConfig.paymentGatewayBaseUrl}/billing/credit-loads?clientRef=$targetId&page=0&size=50&sort=loadedAt,desc")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                val json = extractContentJson(rawJson)
                val items = mutableListOf<ApiCreditLoad>()

                val idMatches = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val amountMatches = Regex("\"amount\"\\s*:\\s*([0-9.]+)").findAll(json).toList()
                val methodMatches = Regex("\"paymentMethod\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val dateMatches = Regex("\"loadedAt\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()
                val refMatches = Regex("\"paymentReference\"\\s*:\\s*\"([^\"]+)\"").findAll(json).toList()

                val ids = idMatches.map { it.groupValues[1] }
                val amounts = amountMatches.map { it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                val methods = methodMatches.map { it.groupValues[1] }
                val dates = dateMatches.map { it.groupValues[1] }
                val refs = refMatches.map { it.groupValues[1] }

                for (i in amounts.indices) {
                    items.add(
                        ApiCreditLoad(
                            id = ids.getOrElse(i) { "cl_$i" },
                            amount = amounts[i],
                            paymentMethod = methods.getOrElse(i) { "MOBILE_MONEY" },
                            loadedAt = dates.getOrElse(i) { "" }.take(10),
                            paymentReference = refs.getOrNull(i)
                        )
                    )
                }
                items
            } else {
                emptyList()
            }
        } catch (_: Exception) {
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

    suspend fun addServiceCatalog(
        clientId: String?,
        serviceName: String,
        category: String,
        price: Double,
        currency: String = "UGX"
    ): Boolean = withContext(Dispatchers.IO) {
        val targetId = if (!clientId.isNullOrBlank()) clientId else activeClientId
        if (targetId.isBlank()) return@withContext false
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/service-catalogs")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val catCode = when {
                category.contains("park", ignoreCase = true) -> "CAR_PARK"
                category.contains("wash", ignoreCase = true) -> "CAR_WASH"
                category.contains("repair", ignoreCase = true) -> "REPAIR"
                else -> "MAINTENANCE"
            }
            val payload = """
                {
                  "clientId": "$targetId",
                  "serviceName": "${serviceName.trim()}",
                  "category": "$catCode",
                  "price": $price,
                  "currency": "$currency"
                }
            """.trimIndent()
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    suspend fun addClientStaff(
        clientId: String?,
        fullName: String,
        position: String?,
        phoneNumber: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val targetId = if (!clientId.isNullOrBlank()) clientId else activeClientId
        if (targetId.isBlank()) return@withContext false
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/clients/$targetId/staff")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val posVal = if (position.isNullOrBlank()) "null" else "\"${position.trim().uppercase()}\""
            val phoneVal = if (phoneNumber.isNullOrBlank()) "null" else "\"${phoneNumber.trim()}\""

            val payload = """
                {
                  "fullName": "${fullName.trim().uppercase()}",
                  "position": $posVal,
                  "phoneNumber": $phoneVal
                }
            """.trimIndent()
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }
}
