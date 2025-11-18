package com.example.wellsync.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Service to manage water reminder tracking and alerts
 */
class WaterReminderService(private val context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("water_reminders", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "WaterReminderService"
        private const val LAST_REMINDER_TIME = "last_reminder_time"
        private const val REMINDER_ACTIVE = "reminder_active"
    }

    fun startWaterReminder(username: String) {
        val currentTime = System.currentTimeMillis()
        sharedPrefs.edit()
            .putLong("${LAST_REMINDER_TIME}_$username", currentTime)
            .putBoolean("${REMINDER_ACTIVE}_$username", true)
            .apply()

        Log.d(TAG, "Water reminder started for user: $username")

        // Schedule the 30-minute alert
        val notificationUtils = NotificationUtils(context)
        notificationUtils.scheduleWaterReminderAlert(30)
    }

    fun stopWaterReminder(username: String) {
        sharedPrefs.edit()
            .putBoolean("${REMINDER_ACTIVE}_$username", false)
            .apply()

        Log.d(TAG, "Water reminder stopped for user: $username")

        // Cancel the 30-minute alert
        val notificationUtils = NotificationUtils(context)
        notificationUtils.cancelWaterReminderAlert()
    }

    fun isReminderActive(username: String): Boolean {
        return sharedPrefs.getBoolean("${REMINDER_ACTIVE}_$username", false)
    }

    fun getLastReminderTime(username: String): Long {
        return sharedPrefs.getLong("${LAST_REMINDER_TIME}_$username", 0L)
    }

    fun checkAndTriggerAlert(username: String) {
        val dataManager = DataManager(context)
        val lastIntakeTime = dataManager.getLastWaterIntakeTime(username)
        val currentTime = System.currentTimeMillis()

        // Check if 30 minutes have passed since last water intake
        val timeSinceLastIntake = (currentTime - lastIntakeTime) / (60 * 1000) // in minutes

        if (timeSinceLastIntake >= 30 && isReminderActive(username)) {
            val notificationUtils = NotificationUtils(context)
            notificationUtils.showWaterAlertNotification()
            Log.d(TAG, "Water alert triggered for user: $username (${timeSinceLastIntake} minutes since last intake)")
        }
    }
}
