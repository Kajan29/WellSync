package com.example.wellsync.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.wellsync.models.Habit
import com.example.wellsync.models.MoodEntry
import com.example.wellsync.models.User
import com.example.wellsync.widgets.WidgetUpdateManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class DataManager(private val context: Context) {
    private val sharedPrefs: SharedPreferences by lazy {
        try {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e("DataManager", "Error accessing SharedPreferences: ${e.message}", e)
            throw RuntimeException("Failed to initialize data storage", e)
        }
    }
    private val gson = Gson()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val PREF_NAME = "wellsync_prefs"
    }

    private fun todayDate(): String = dateFormat.format(Date())

    fun saveUsers(users: List<User>) {
        try {
            val json = gson.toJson(users)
            sharedPrefs.edit { putString("users", json) }
        } catch (e: Exception) {
            Log.e("DataManager", "Error saving users: ${e.message}", e)
        }
    }

    fun getUsers(): List<User> {
        return try {
            val json = sharedPrefs.getString("users", "[]") ?: "[]"
            if (json.isBlank() || json == "[]") return emptyList()

            val type = object : TypeToken<List<User>>() {}.type
            val users = gson.fromJson<List<User>>(json, type)
            users ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e("DataManager", "Error parsing users JSON, resetting: ${e.message}", e)
            sharedPrefs.edit { remove("users") }
            emptyList()
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting users: ${e.message}", e)
            emptyList()
        }
    }

    fun addUser(user: User): Boolean {
        return try {
            val users = getUsers().toMutableList()
            if (users.any { it.username == user.username }) return false
            val hashedPassword = hashPassword(user.password)
            if (hashedPassword != null) {
                users.add(user.copy(password = hashedPassword))
                saveUsers(users)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DataManager", "Error adding user: ${e.message}", e)
            false
        }
    }

    fun validateUser(username: String, password: String): Boolean {
        return try {
            val users = getUsers()
            val hashedPassword = hashPassword(password)
            hashedPassword != null && users.any {
                it.username == username && it.password == hashedPassword
            }
        } catch (e: Exception) {
            Log.e("DataManager", "Error validating user: ${e.message}", e)
            false
        }
    }

    private fun hashPassword(password: String): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("DataManager", "Error hashing password: ${e.message}", e)
            null
        }
    }

    fun setCurrentUser(username: String) {
        try {
            sharedPrefs.edit { putString("current_user", username) }
            WidgetUpdateManager.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("DataManager", "Error setting current user: ${e.message}", e)
        }
    }

    fun getCurrentUser(): String? {
        return try {
            sharedPrefs.getString("current_user", null)
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting current user: ${e.message}", e)
            null
        }
    }

    fun logout() {
        try {
            sharedPrefs.edit { remove("current_user") }
            WidgetUpdateManager.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("DataManager", "Error during logout: ${e.message}", e)
        }
    }

    fun saveHabits(username: String, habits: List<Habit>) {
        try {
            val json = gson.toJson(habits)
            sharedPrefs.edit { putString("habits_$username", json) }
            WidgetUpdateManager.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("DataManager", "Error saving habits: ${e.message}", e)
        }
    }

    fun getHabits(username: String): List<Habit> {
        return try {
            val json = sharedPrefs.getString("habits_$username", "[]") ?: "[]"
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e("DataManager", "Error parsing habits JSON: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting habits: ${e.message}", e)
            emptyList()
        }
    }

    fun saveMoods(username: String, moods: List<MoodEntry>) {
        try {
            val json = gson.toJson(moods)
            sharedPrefs.edit { putString("moods_$username", json) }
        } catch (e: Exception) {
            Log.e("DataManager", "Error saving moods: ${e.message}", e)
        }
    }

    fun getMoods(username: String): List<MoodEntry> {
        return try {
            val json = sharedPrefs.getString("moods_$username", "[]") ?: "[]"
            val type = object : TypeToken<List<MoodEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e("DataManager", "Error parsing moods JSON: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting moods: ${e.message}", e)
            emptyList()
        }
    }

    fun addMoodEntry(username: String, mood: MoodEntry) {
        try {
            val moods = getMoods(username).toMutableList()
            val today = todayDate()
            moods.removeAll { it.date == today }
            moods.add(mood)
            saveMoods(username, moods)
        } catch (e: Exception) {
            Log.e("DataManager", "Error adding mood entry: ${e.message}", e)
        }
    }

    fun setHydrationInterval(username: String, hours: Int) {
        try {
            sharedPrefs.edit { putInt("hydration_interval_$username", hours) }
        } catch (e: Exception) {
            Log.e("DataManager", "Error setting hydration interval: ${e.message}", e)
        }
    }

    fun getHydrationInterval(username: String): Int {
        return try {
            sharedPrefs.getInt("hydration_interval_$username", 3)
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting hydration interval: ${e.message}", e)
            3
        }
    }

    fun getMorningTime(username: String): String {
        return try {
            sharedPrefs.getString("morning_time_$username", "08:00") ?: "08:00"
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting morning time: ${e.message}", e)
            "08:00"
        }
    }

    fun getNightTime(username: String): String {
        return try {
            sharedPrefs.getString("night_time_$username", "22:00") ?: "22:00"
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting night time: ${e.message}", e)
            "22:00"
        }
    }

    fun setThemeMode(username: String, isDark: Boolean) {
        sharedPrefs.edit { putBoolean("theme_mode_$username", isDark) }
    }

    fun getThemeMode(username: String): Boolean {
        return sharedPrefs.getBoolean("theme_mode_$username", false)
    }

    fun setThemeColor(username: String, color: String) {
        sharedPrefs.edit { putString("theme_color_$username", color) }
    }

    fun getThemeColor(username: String): String {
        return sharedPrefs.getString("theme_color_$username", "green") ?: "green"
    }

    fun getPendingHabitsCount(): Int {
        val currentUser = getCurrentUser() ?: return 0
        val habits = getHabits(currentUser)
        return habits.count { !it.done }
    }

    private fun waterIntakeKey(username: String, date: String): String = "water_intake_ml_${username}_${date}"

    fun getWaterIntakeProgress(): Int {
        val currentUser = getCurrentUser() ?: return 0
        return try {
            sharedPrefs.getInt(waterIntakeKey(currentUser, todayDate()), 0)
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting water intake progress: ${e.message}", e)
            0
        }
    }

    fun getWaterIntakeToday(username: String): Int {
        return try {
            sharedPrefs.getInt(waterIntakeKey(username, todayDate()), 0)
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting water intake today: ${e.message}", e)
            0
        }
    }

    fun addWaterIntake(amountMl: Int) {
        val currentUser = getCurrentUser() ?: return
        val today = todayDate()
        val currentIntake = getWaterIntakeProgress()
        try {
            sharedPrefs.edit {
                putInt(waterIntakeKey(currentUser, today), currentIntake + amountMl)
                putLong("last_water_intake_${currentUser}", System.currentTimeMillis())
            }
            WidgetUpdateManager.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("DataManager", "Error adding water intake: ${e.message}", e)
        }
    }

    fun setWaterGoal(goal: Int) {
        val currentUser = getCurrentUser() ?: return
        setWaterGoal(currentUser, goal)
    }

    fun setWaterGoal(username: String, goalMl: Int) {
        try {
            sharedPrefs.edit { putInt("water_goal_ml_${username}", goalMl) }
            WidgetUpdateManager.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("DataManager", "Error setting water goal: ${e.message}", e)
        }
    }

    fun getWaterGoal(): Int {
        val currentUser = getCurrentUser() ?: return 2000
        return getWaterGoal(currentUser)
    }

    fun getWaterGoal(username: String): Int {
        return try {
            sharedPrefs.getInt("water_goal_ml_${username}", 2000)
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting water goal: ${e.message}", e)
            2000
        }
    }

    fun saveCaloriesToday(username: String, calories: Float) {
        try {
            val today = todayDate()
            sharedPrefs.edit { putFloat("calories_${username}_$today", calories) }
            WidgetUpdateManager.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("DataManager", "Error saving calories today: ${e.message}", e)
        }
    }

    fun getCaloriesToday(username: String): Float {
        return try {
            sharedPrefs.getFloat("calories_${username}_" + todayDate(), 0f)
        } catch (e: Exception) {
            Log.e("DataManager", "Error getting calories today: ${e.message}", e)
            0f
        }
    }

    fun setNotificationTimes(username: String, morningTime: String, nightTime: String) {
        try {
            sharedPrefs.edit {
                putString("morning_time_${username}", morningTime)
                putString("night_time_${username}", nightTime)
            }
        } catch (e: Exception) {
            Log.e("DataManager", "Error setting notification times: ${e.message}", e)
        }
    }

    fun getLastWaterIntakeTime(username: String): Long {
        return sharedPrefs.getLong("last_water_intake_${username}", 0L)
    }

    fun hasRecentWaterIntake(username: String, withinMinutes: Int = 30): Boolean {
        val lastIntakeTime = getLastWaterIntakeTime(username)
        if (lastIntakeTime == 0L) return false

        val currentTime = System.currentTimeMillis()
        val timeDifferenceMinutes = (currentTime - lastIntakeTime) / (60 * 1000)
        return timeDifferenceMinutes < withinMinutes
    }

    fun getUserByUsername(username: String): User? {
        val users = getUsers()
        return users.find { it.username == username }
    }

    fun saveHourlySteps(username: String, hour: Int, steps: Int) {
        val today = todayDate()
        sharedPrefs.edit { putInt("hourly_steps_${username}_${today}_$hour", steps) }
    }

    fun getTodayHourlySteps(username: String): Map<Int, Int> {
        val today = todayDate()
        val hourlySteps = mutableMapOf<Int, Int>()
        for (hour in 0..23) {
            val steps = sharedPrefs.getInt("hourly_steps_${username}_${today}_$hour", 0)
            if (steps > 0) {
                hourlySteps[hour] = steps
            }
        }
        return hourlySteps
    }

    fun addWaterGlass(username: String) {
        val today = todayDate()
        val currentGlasses = getWaterIntakeToday(username) / 250
        try {
            sharedPrefs.edit { putInt("water_glasses_${username}_$today", currentGlasses + 1) }
            WidgetUpdateManager.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("DataManager", "Error adding water glass: ${e.message}", e)
        }
    }

    fun saveWaterIntakeToday(username: String, amountMl: Int) {
        try {
            val today = todayDate()
            sharedPrefs.edit { putInt(waterIntakeKey(username, today), amountMl) }
            WidgetUpdateManager.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("DataManager", "Error saving water intake today: ${e.message}", e)
        }
    }

    fun setStepGoal(username: String, goal: Int) {
        sharedPrefs.edit { putInt("step_goal_${username}", goal) }
    }

    fun getStepGoal(username: String): Int {
        return sharedPrefs.getInt("step_goal_${username}", 10000)
    }

    fun getStepGoal(): Int {
        val currentUser = getCurrentUser() ?: return 10000
        return getStepGoal(currentUser)
    }

    fun getStepsToday(username: String): Int {
        val today = todayDate()
        return sharedPrefs.getInt("steps_${username}_$today", 0)
    }

    fun saveStepsToday(username: String, steps: Int) {
        try {
            val today = todayDate()
            sharedPrefs.edit { putInt("steps_${username}_$today", steps) }
        } catch (e: Exception) {
            Log.e("DataManager", "Error saving steps today: ${e.message}", e)
        }
    }

    fun addSteps(username: String, additionalSteps: Int) {
        try {
            val today = todayDate()
            val current = getStepsToday(username)
            sharedPrefs.edit { putInt("steps_${username}_$today", current + additionalSteps) }
        } catch (e: Exception) {
            Log.e("DataManager", "Error adding steps: ${e.message}", e)
        }
    }

    private fun lastSensorTotalKey(username: String) = "last_sensor_total_${username}"
    private fun lastSensorDateKey(username: String) = "last_sensor_date_${username}"

    fun updateStepsFromSensor(username: String, currentSensorTotal: Int): Int {
        try {
            val today = todayDate()

            val lastDate = sharedPrefs.getString(lastSensorDateKey(username), null)
            val lastTotal = sharedPrefs.getInt(lastSensorTotalKey(username), -1)

            if (lastDate == null || lastDate != today) {
                sharedPrefs.edit {
                    putString(lastSensorDateKey(username), today)
                    putInt(lastSensorTotalKey(username), currentSensorTotal)
                    putInt("steps_${username}_$today", 0)
                }

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                saveHourlySteps(username, currentHour, 0)

                WidgetUpdateManager.updateWidgets(context)
                return 0
            }

            val delta = if (lastTotal < 0) {
                0
            } else {
                val rawDelta = currentSensorTotal - lastTotal
                if (rawDelta < 0) currentSensorTotal else rawDelta
            }

            sharedPrefs.edit {
                putInt(lastSensorTotalKey(username), currentSensorTotal)
                putString(lastSensorDateKey(username), today)
            }

            if (delta > 0) {
                addSteps(username, delta)

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                addHourlySteps(username, currentHour, delta)

                WidgetUpdateManager.updateWidgets(context)
            }

            return getStepsToday(username)
        } catch (e: Exception) {
            Log.e("DataManager", "Error updating steps from sensor: ${e.message}", e)
            return getStepsToday(username)
        }
    }

    fun addHourlySteps(username: String, hour: Int, delta: Int) {
        try {
            val today = todayDate()
            val key = "hourly_steps_${username}_${today}_$hour"
            val current = sharedPrefs.getInt(key, 0)
            sharedPrefs.edit { putInt(key, current + delta) }
        } catch (e: Exception) {
            Log.e("DataManager", "Error adding hourly steps: ${e.message}", e)
        }
    }

}
