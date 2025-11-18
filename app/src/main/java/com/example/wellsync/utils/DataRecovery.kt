package com.example.wellsync.utils

import android.content.Context
import android.util.Log


object DataRecovery {

    fun clearAllAppData(context: Context): Boolean {
        return try {
            // Clear SharedPreferences
            val sharedPrefs = context.getSharedPreferences("wellsync_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()

            // Clear any cached files
            context.cacheDir.deleteRecursively()

            Log.i("DataRecovery", "Successfully cleared all app data")
            true
        } catch (e: Exception) {
            Log.e("DataRecovery", "Error clearing app data: ${e.message}", e)
            false
        }
    }

    fun isDataCorrupted(context: Context): Boolean {
        return try {
            // Try to access SharedPreferences safely
            val sharedPrefs = context.getSharedPreferences("wellsync_prefs", Context.MODE_PRIVATE)
            val allEntries = sharedPrefs.all

            // Check if any entries look corrupted
            for ((key, value) in allEntries) {
                if (key.startsWith("users") || key.startsWith("habits_") || key.startsWith("moods_")) {
                    val jsonString = value as? String
                    if (jsonString != null && !isValidJson(jsonString)) {
                        Log.w("DataRecovery", "Corrupted data detected for key: $key")
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e("DataRecovery", "Error checking data corruption: ${e.message}", e)
            true
        }
    }

    private fun isValidJson(json: String): Boolean {
        return try {
            json.trim().let {
                it.startsWith("[") && it.endsWith("]") ||
                it.startsWith("{") && it.endsWith("}")
            }
        } catch (e: Exception) {
            false
        }
    }
}
