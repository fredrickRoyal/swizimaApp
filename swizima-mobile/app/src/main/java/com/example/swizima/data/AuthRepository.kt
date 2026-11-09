package com.example.swizima.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
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
    private var lastOtpReference: String = ""

    private const val PREFS_NAME = "swizima_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_CONTACT = "user_contact"
    private const val KEY_IS_LINKED = "is_linked"
    private const val KEY_ORG_ID = "org_id"

    fun saveSession(context: Context, profile: UserProfile) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_CONTACT, profile.contact)
                .putBoolean(KEY_IS_LINKED, profile.isLinked)
                .putString(KEY_ORG_ID, profile.organizationId)
                .apply()
        } catch (_: Exception) {}
    }

    fun clearSession(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }

    fun getSavedSession(context: Context): UserProfile? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            if (!isLoggedIn) return null
            val contact = prefs.getString(KEY_USER_CONTACT, "") ?: ""
            if (contact.isBlank()) return null
            val isLinked = prefs.getBoolean(KEY_IS_LINKED, true)
            val orgId = prefs.getString(KEY_ORG_ID, null)
            UserProfile(
                userId = "usr_saved_session",
                contact = contact,
                name = "Verified User",
                organizationId = orgId,
                organizationName = "Swizima Motors",
                isLinked = isLinked
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun requestOtp(contactInput: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/auth/login/otp")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val payload = "{\"username\":\"$contactInput\"}"
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val refMatch = Regex("\"otpReference\"\\s*:\\s*\"([^\"]+)\"").find(responseBody)
                if (refMatch != null) {
                    lastOtpReference = refMatch.groupValues[1]
                }
                AuthResult(true, "OTP sent successfully")
            } else {
                val errorMsg = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) {
                    ""
                }
                AuthResult(false, "Failed to send OTP (Server error $responseCode: $errorMsg)")
            }
        } catch (e: Exception) {
            AuthResult(false, "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/auth/login/otp"}")
        }
    }

    suspend fun verifyOtpAndFetchProfile(contactInput: String, otp: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/auth/verify-otp")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val refToSend = if (lastOtpReference.isNotBlank()) lastOtpReference else "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            val payload = "{\"otpReference\":\"$refToSend\",\"code\":\"$otp\"}"
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }

                var userId = "usr_api_1"
                val userMatch = Regex("\"userId\"\\s*:\\s*\"([^\"]+)\"").find(responseBody)
                if (userMatch != null) {
                    userId = userMatch.groupValues[1]
                }

                val isUnlinked = contactInput.contains("000") || contactInput.endsWith("0")
                val profile = UserProfile(
                    userId = userId,
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
                } catch (_: Exception) {
                    ""
                }
                AuthResult(false, "Invalid OTP verification code (Server response $responseCode: $errorMsg)")
            }
        } catch (e: Exception) {
            AuthResult(false, "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/auth/verify-otp"}")
        }
    }

    suspend fun requestOrganizationLinking(contact: String, orgCodeOrDetails: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiGatewayConfig.gatewayBaseUrl}/users/request-linking")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }
            val payload = "{\"contact\":\"$contact\",\"details\":\"$orgCodeOrDetails\"}"
            connection.outputStream.write(payload.toByteArray())
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                AuthResult(true, "Linking request submitted successfully")
            } else {
                val errorMsg = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) {
                    ""
                }
                AuthResult(false, "Failed to submit linking request (Server response $responseCode: $errorMsg)")
            }
        } catch (e: Exception) {
            AuthResult(false, "API Connection Failed: ${e.localizedMessage ?: "Unable to reach ${ApiGatewayConfig.gatewayBaseUrl}/users/request-linking"}")
        }
    }
}
