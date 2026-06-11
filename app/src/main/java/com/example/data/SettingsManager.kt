package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nim_chat_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NVIDIA_API_KEY = "nvidia_api_key"
        private const val KEY_NVIDIA_MODEL_ID = "nvidia_model_id"
        private const val KEY_SEARCH_ENABLED = "search_enabled"
        private const val KEY_SEARCH_PROVIDER = "search_provider"
        private const val KEY_TAVILY_API_KEY = "tavily_api_key"

        const val PROVIDER_DDG = "DuckDuckGo (Free, No Key)"
        const val PROVIDER_TAVILY = "Tavily Search (Requires Key)"

        const val DEFAULT_MODEL = "nvidia/llama-3.1-nemotron-70b-instruct"
    }

    var nvidiaApiKey: String
        get() = prefs.getString(KEY_NVIDIA_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NVIDIA_API_KEY, value.trim()).apply()

    var nvidiaModelId: String
        get() = prefs.getString(KEY_NVIDIA_MODEL_ID, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_NVIDIA_MODEL_ID, value.trim()).apply()

    var searchEnabled: Boolean
        get() = prefs.getBoolean(KEY_SEARCH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEARCH_ENABLED, value).apply()

    var searchProvider: String
        get() = prefs.getString(KEY_SEARCH_PROVIDER, PROVIDER_DDG) ?: PROVIDER_DDG
        set(value) = prefs.edit().putString(KEY_SEARCH_PROVIDER, value).apply()

    var tavilyApiKey: String
        get() = prefs.getString(KEY_TAVILY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TAVILY_API_KEY, value.trim()).apply()
}
