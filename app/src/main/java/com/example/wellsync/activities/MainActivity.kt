package com.example.wellsync.activities

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.wellsync.R
import com.example.wellsync.databinding.ActivityMainBinding
import com.example.wellsync.utils.DataManager
import com.example.wellsync.utils.DataRecovery
import com.example.wellsync.utils.NotificationUtils

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var dataManager: DataManager? = null
    private var notificationUtils: NotificationUtils? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyUserTheme()

        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            binding?.let {
                setContentView(it.root)
            } ?: run {
                Log.e("MainActivity", "Failed to inflate binding")
                showErrorAndFinish("Failed to initialize app interface")
                return
            }

            if (DataRecovery.isDataCorrupted(this)) {
                Log.w("MainActivity", "Corrupted data detected, clearing...")
                DataRecovery.clearAllAppData(this)
            }

            dataManager = DataManager(this)
            notificationUtils = NotificationUtils(this)

            val currentUser = dataManager?.getCurrentUser()
            if (currentUser.isNullOrEmpty()) {
                Log.w("MainActivity", "No current user found, redirecting to login")
                navigateToLogin()
                return
            }

            if (!setupBottomNavigation()) {
                Log.e("MainActivity", "Failed to setup navigation")
                showErrorAndFinish("Failed to setup navigation")
                return
            }

            requestPermissions()
            setupNotifications()

        } catch (e: Exception) {
            Log.e("MainActivity", "Critical error in onCreate: ${e.message}", e)
            showErrorAndFinish("Failed to initialize the app")
        }
    }

    private fun applyUserTheme() {
        try {
            val sharedPrefs = getSharedPreferences("wellsync_prefs", MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", null)
            val isDarkMode = sharedPrefs.getBoolean("theme_mode_$currentUser", false)
            val themeColor = sharedPrefs.getString("theme_color_$currentUser", "green") ?: "green"

            AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )

            val themeResId = getThemeResId(themeColor)
            setTheme(themeResId)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error applying user theme: ${e.message}", e)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            setTheme(R.style.Theme_WellSync)
        }
    }

    private fun getThemeResId(color: String): Int {
        return when (color.lowercase()) {
            "emerald green", "#43a047", "green" -> R.style.Theme_WellSync_Emerald
            "ocean blue", "#2196f3", "blue" -> R.style.Theme_WellSync_Ocean
            "sunset orange", "#ff9800", "orange" -> R.style.Theme_WellSync_Sunset
            "royal purple", "#9c27b0", "purple" -> R.style.Theme_WellSync_Purple
            "rose pink", "#e91e63", "pink" -> R.style.Theme_WellSync_Pink
            else -> R.style.Theme_WellSync_Emerald
        }
    }

    private fun setupBottomNavigation(): Boolean {
        return try {
            binding?.let { binding ->
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

                if (navHostFragment == null) {
                    Log.e("MainActivity", "NavHostFragment not found")
                    return false
                }

                val navController = navHostFragment.navController
                binding.bottomNavigation.setupWithNavController(navController)

                binding.bottomNavigation.setOnItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.nav_home -> {
                            if (navController.currentDestination?.id != R.id.nav_home) {
                                navController.navigate(R.id.nav_home)
                            }
                            true
                        }
                        R.id.nav_habits -> {
                            if (navController.currentDestination?.id != R.id.nav_habits) {
                                navController.navigate(R.id.nav_habits)
                            }
                            true
                        }
                        R.id.nav_steps -> {
                            if (navController.currentDestination?.id != R.id.nav_steps) {
                                navController.navigate(R.id.nav_steps)
                            }
                            true
                        }
                        R.id.nav_mood -> {
                            if (navController.currentDestination?.id != R.id.nav_mood) {
                                navController.navigate(R.id.nav_mood)
                            }
                            true
                        }
                        R.id.nav_profile -> {
                            if (navController.currentDestination?.id != R.id.nav_profile) {
                                navController.navigate(R.id.nav_profile)
                            }
                            true
                        }
                        else -> false
                    }
                }

                navController.addOnDestinationChangedListener { _, destination, _ ->
                    when (destination.id) {
                        R.id.nav_water_limit -> {
                            binding.bottomNavigation.menu.setGroupCheckable(0, false, true)
                            for (i in 0 until binding.bottomNavigation.menu.size()) {
                                binding.bottomNavigation.menu.getItem(i).isChecked = false
                            }
                        }
                        else -> {
                            binding.bottomNavigation.menu.setGroupCheckable(0, true, true)
                            val menuItem = when (destination.id) {
                                R.id.nav_home -> binding.bottomNavigation.menu.findItem(R.id.nav_home)
                                R.id.nav_habits -> binding.bottomNavigation.menu.findItem(R.id.nav_habits)
                                R.id.nav_steps -> binding.bottomNavigation.menu.findItem(R.id.nav_steps)
                                R.id.nav_mood -> binding.bottomNavigation.menu.findItem(R.id.nav_mood)
                                R.id.nav_profile -> binding.bottomNavigation.menu.findItem(R.id.nav_profile)
                                else -> null
                            }
                            menuItem?.isChecked = true
                        }
                    }
                }

                Log.d("MainActivity", "Navigation setup successful")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up navigation: ${e.message}", e)
            false
        }
    }

    private fun requestPermissions() {
        try {
            val permissions = arrayOf(
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.POST_NOTIFICATIONS
            )

            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            }

            requestExactAlarmPermission()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error requesting permissions: ${e.message}", e)
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun setupNotifications() {
        try {
            val currentUser = dataManager?.getCurrentUser()
            if (currentUser.isNullOrEmpty()) return

            val morningTime = dataManager?.getMorningTime(currentUser)?.split(":") ?: listOf("08", "00")
            val nightTime = dataManager?.getNightTime(currentUser)?.split(":") ?: listOf("22", "00")

            if (morningTime.size >= 2 && nightTime.size >= 2) {
                notificationUtils?.scheduleMorningNotification(
                    morningTime[0].toIntOrNull() ?: 8,
                    morningTime[1].toIntOrNull() ?: 0
                )

                notificationUtils?.scheduleNightNotification(
                    nightTime[0].toIntOrNull() ?: 22,
                    nightTime[1].toIntOrNull() ?: 0
                )
            }

            notificationUtils?.scheduleHydrationReminder(1)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up notifications: ${e.message}", e)
        }
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error navigating to login: ${e.message}", e)
            finishAffinity()
        }
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            Log.d("MainActivity", "Permissions result received")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        dataManager = null
        notificationUtils = null
    }
}
