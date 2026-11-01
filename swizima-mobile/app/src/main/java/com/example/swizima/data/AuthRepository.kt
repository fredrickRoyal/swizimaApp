package com.example.swizima.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class UserProfile(
    val userId: String,
    val contact: String,
    val name: String?,
    val organizationId: String?,
    val organizationName: String?,
    val isLinked: Boolean
)

data class AuthResult(
    val success: Boolean,
    val message: String,
    val profile: UserProfile? = null
)

object AuthRepository {
    private var lastOtpReference: String = "ref_default_001"

    suspend fun requestOtp(contactInput: String): AuthResult = withContext(Dispatchers.IO) {
        if (ApiGatewayConfig.isSimulationMode) {
            delay(800L)
            lastOtpReference = "ref_sim_${System.currentTimeMillis()}"
            return@withContext AuthResult(true, "OTP sent successfully to $contactInput")
        }

        try {
            // Correct backend endpoint matching swizima-web AuthService (/api/v1/auth/login/otp)
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/auth/login/otp")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }
            val payload = "{\"username\":\"$contactInput\"}"
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                lastOtpReference = "ref_${System.currentTimeMillis()}"
                AuthResult(true, "OTP sent successfully")
            } else {
                val errorMsg = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                AuthResult(false, "Failed to send OTP (Server error $responseCode: $errorMsg)")
            }
        } catch (e: Exception) {
            AuthResult(false, "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/auth/login/otp"}")
        }
    }

    suspend fun verifyOtpAndFetchProfile(contactInput: String, otp: String): AuthResult = withContext(Dispatchers.IO) {
        if (ApiGatewayConfig.isSimulationMode) {
            delay(1000L)
            if (otp == "123456" || otp.length == 6) {
                val isUnlinked = contactInput.contains("000") || contactInput.endsWith("0")
                val profile = if (isUnlinked) {
                    UserProfile(
                        userId = "usr_unlinked_123",
                        contact = contactInput,
                        name = "Guest User",
                        organizationId = null,
                        organizationName = null,
                        isLinked = false
                    )
                } else {
                    UserProfile(
                        userId = "usr_linked_789",
                        contact = contactInput,
                        name = "Authorized Inspector / Agent",
                        organizationId = "org_swizima_01",
                        organizationName = "Swizima Motors & Parking Ltd",
                        isLinked = true
                    )
                }
                return@withContext AuthResult(true, "Login successful", profile)
            } else {
                return@withContext AuthResult(false, "Invalid OTP code. Please enter 123456.")
            }
        }

        try {
            // Correct backend endpoint matching swizima-web AuthService (/api/v1/auth/verify-otp)
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/auth/verify-otp")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }
            val payload = "{\"otpReference\":\"$lastOtpReference\",\"code\":\"$otp\"}"
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val isUnlinked = contactInput.contains("000") || contactInput.endsWith("0")
                val profile = UserProfile(
                    userId = "usr_api_1",
                    contact = contactInput,
                    name = "Verified User",
                    organizationId = if (isUnlinked) null else "org_swizima_01",
                    organizationName = if (isUnlinked) null else "Swizima Motors & Parking Ltd",
                    isLinked = !isUnlinked
                )
                AuthResult(true, "Login successful", profile)
            } else {
                val errorMsg = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                AuthResult(false, "Invalid OTP verification code (Server response $responseCode: $errorMsg)")
            }
        } catch (e: Exception) {
            AuthResult(false, "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/auth/verify-otp"}")
        }
    }

    suspend fun requestOrganizationLinking(contact: String, orgCodeOrDetails: String): AuthResult = withContext(Dispatchers.IO) {
        if (ApiGatewayConfig.isSimulationMode) {
            delay(1000L)
            return@withContext AuthResult(true, "Linking request submitted successfully to organization administrators.")
        }

        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/users/request-linking")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
            }
            val payload = "{\"contact\":\"$contact\",\"details\":\"$orgCodeOrDetails\"}"
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                AuthResult(true, "Linking request submitted successfully")
            } else {
                val errorMsg = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                AuthResult(false, "Failed to submit linking request (Server response $responseCode: $errorMsg)")
            }
        } catch (e: Exception) {
            AuthResult(false, "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/users/request-linking"}")
        }
    }
}
