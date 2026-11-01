package com.example.swizima.data

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

    suspend fun fetchClients(): List<ApiClient> = withContext(Dispatchers.IO) {
        if (ApiGatewayConfig.isSimulationMode) {
            return@withContext listOf(
                ApiClient("org_swizima_01", "Swizima Motors & Parking Ltd", "SWZ-001"),
                ApiClient("org_swizima_02", "Kampala City Parking Operations", "KCP-002")
            )
        }
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/clients")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                listOf(ApiClient("org_swizima_01", "Swizima Motors & Parking Ltd", "SWZ-001"))
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            listOf(ApiClient("org_swizima_01", "Swizima Motors & Parking Ltd", "SWZ-001"))
        }
    }

    suspend fun fetchServiceCatalogs(category: String?, clientId: String?): List<ApiServiceCatalogItem> = withContext(Dispatchers.IO) {
        if (ApiGatewayConfig.isSimulationMode) {
            val all = listOf(
                ApiServiceCatalogItem("s1", "org_swizima_01", "Swizima Motors", "Standard Parking (Daily)", "Parking", 5000.0, "UGX", "Standard secure parking slot"),
                ApiServiceCatalogItem("s2", "org_swizima_01", "Swizima Motors", "VIP Covered Parking", "Parking", 10000.0, "UGX", "Covered secure parking spot"),
                ApiServiceCatalogItem("s3", "org_swizima_01", "Swizima Motors", "Full Body Foam Wash", "General washing", 25000.0, "UGX", "Exterior foam wash & tire shine"),
                ApiServiceCatalogItem("s4", "org_swizima_01", "Swizima Motors", "Express Underwash & Interior Vacuum", "General washing", 30000.0, "UGX", "Complete wash + interior cleaning"),
                ApiServiceCatalogItem("s5", "org_swizima_01", "Swizima Motors", "Brake Pad Replacement & Diagnostics", "Repair", 150000.0, "UGX", "Labor and testing"),
                ApiServiceCatalogItem("s6", "org_swizima_01", "Swizima Motors", "15-Point Pre-Trip Inspection", "Review", 40000.0, "UGX", "General safety check"),
                ApiServiceCatalogItem("s7", "org_swizima_01", "Swizima Motors", "Full Engine Service & Oil Change", "General Service", 120000.0, "UGX", "Oil filter, air filter, engine tune-up")
            )
            return@withContext if (category.isNullOrBlank()) {
                all
            } else {
                all.filter { it.category.equals(category, ignoreCase = true) || category.equals(it.category, ignoreCase = true) || it.category.contains(category, ignoreCase = true) }
            }
        }
        try {
            val urlStr = buildString {
                append("${ApiGatewayConfig.gatewayBaseUrl}/service-catalogs")
                if (!category.isNullOrBlank() || !clientId.isNullOrBlank()) {
                    append("?")
                    val params = mutableListOf<String>()
                    if (!category.isNullOrBlank()) params.add("category=$category")
                    if (!clientId.isNullOrBlank()) params.add("clientId=$clientId")
                    append(params.joinToString("&"))
                }
            }
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                emptyList()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchClientStaff(clientId: String): List<ApiClientStaff> = withContext(Dispatchers.IO) {
        if (ApiGatewayConfig.isSimulationMode) {
            return@withContext listOf(
                ApiClientStaff("st1", clientId, "James Mukasa (Valet)", "Valet Attendant", "+256 712 345 678"),
                ApiClientStaff("st2", clientId, "Mark Ssemwanga", "Parking Warden", "+256 733 987 654"),
                ApiClientStaff("st3", clientId, "Peter Wandera", "Senior Washer", "+254 700 123 999"),
                ApiClientStaff("st4", clientId, "Alex Omondi", "Lead Mechanic", "+254 722 444 888"),
                ApiClientStaff("st5", clientId, "Daniel Inspector", "Safety Reviewer", "+254 788 555 222"),
                ApiClientStaff("st6", clientId, "John TeamLead", "Operations Manager", "+256 755 333 111")
            )
        }
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/clients/$clientId/staff")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 20000
            }
            if (connection.responseCode in 200..299) {
                emptyList()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            listOf(
                ApiClientStaff("st1", clientId, "James Mukasa", "Valet Attendant", "+256 712 345 678"),
                ApiClientStaff("st2", clientId, "Mark Ssemwanga", "Parking Warden", "+256 733 987 654")
            )
        }
    }
}
