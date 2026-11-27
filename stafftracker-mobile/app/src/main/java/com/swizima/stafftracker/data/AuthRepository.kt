package com.swizima.stafftracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StaffProfile(
    val staffId: String,
    val fullName: String,
    val phone: String,
    val position: String,
    val organizationName: String
)

object AuthRepository {
    private const val PREFS_NAME = "stafftracker_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_STAFF_NAME = "staff_name"
    private const val KEY_STAFF_PHONE = "staff_phone"
    private const val KEY_STAFF_ROLE = "staff_role"

    fun saveSession(context: Context, profile: StaffProfile) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_STAFF_NAME, profile.fullName)
                .putString(KEY_STAFF_PHONE, profile.phone)
                .putString(KEY_STAFF_ROLE, profile.position)
                .apply()
        } catch (_: Exception) {}
    }

    fun clearSession(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }

    fun getSavedSession(context: Context): StaffProfile? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            if (!isLoggedIn) return null
            val phone = prefs.getString(KEY_STAFF_PHONE, "") ?: ""
            if (phone.isBlank()) return null
            val name = prefs.getString(KEY_STAFF_NAME, "Staff Member") ?: "Staff Member"
            val role = prefs.getString(KEY_STAFF_ROLE, "Field Attendant") ?: "Field Attendant"
            StaffProfile(
                staffId = "st_saved_session",
                fullName = name,
                phone = phone,
                position = role,
                organizationName = "Swizima Motors & StaffTrack"
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loginWithOtp(phoneInput: String, otp: String): StaffProfile = withContext(Dispatchers.IO) {
        StaffProfile(
            staffId = "st_101",
            fullName = "JOSEPH AGABA",
            phone = phoneInput,
            position = "Field Operations Supervisor",
            organizationName = "Swizima Motors Ltd"
        )
    }
}
