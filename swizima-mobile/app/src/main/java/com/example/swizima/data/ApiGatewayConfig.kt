package com.example.swizima.data

import android.content.Context

object ApiGatewayConfig {
    var gatewayBaseUrl: String = "http://192.168.100.6:9080/api/v1"

    private const val PREFS_NAME = "swizima_settings"
    private const val KEY_GATEWAY_URL = "gateway_base_url"

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedUrl = prefs.getString(KEY_GATEWAY_URL, null)
            if (!savedUrl.isNullOrBlank()) {
                gatewayBaseUrl = savedUrl
            }
        } catch (_: Exception) {}
    }

    fun saveGatewayUrl(context: Context, url: String) {
        try {
            val cleanUrl = url.trim().removeSuffix("/")
            gatewayBaseUrl = cleanUrl
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_GATEWAY_URL, cleanUrl).apply()
        } catch (_: Exception) {}
    }
}
