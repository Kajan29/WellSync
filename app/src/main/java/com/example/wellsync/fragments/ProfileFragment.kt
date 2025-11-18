package com.example.wellsync.fragments

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.wellsync.activities.LoginActivity
import com.example.wellsync.databinding.FragmentProfileBinding
import com.example.wellsync.utils.DataManager
import com.example.wellsync.utils.NotificationUtils
import com.example.wellsync.widgets.WidgetUpdateManager
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: DataManager
    private lateinit var notificationUtils: NotificationUtils
    private var currentUser: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        notificationUtils = NotificationUtils(requireContext())
        currentUser = dataManager.getCurrentUser()

        setupUI()
        loadUserData()
    }

    private fun setupUI() {
        binding.apply {

            btnEditProfile.setOnClickListener { showEditProfileDialog() }
            btnLogout.setOnClickListener { showLogoutDialog() }


            cardMorningNotification.setOnClickListener { setMorningNotification() }
            cardNightNotification.setOnClickListener { setNightNotification() }
            cardHydrationSettings.setOnClickListener { setHydrationInterval() }

            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                currentUser?.let { user ->
                    dataManager.setThemeMode(user, isChecked)
                    AppCompatDelegate.setDefaultNightMode(
                        if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                        else AppCompatDelegate.MODE_NIGHT_NO
                    )
                    WidgetUpdateManager.updateWidgets(requireContext())
                }
            }

            btnThemeColor.setOnClickListener { showThemeColorDialog() }

            btnShareStats.setOnClickListener { shareWeeklyStats() }
        }
    }

    private fun loadUserData() {
        currentUser?.let { username ->
            val users = dataManager.getUsers()
            val user = users.find { it.username == username }

            binding.apply {
                tvUsername.text = "@$username"
                tvUserEmail.text = user?.email ?: "No email set"
                tvUserName.text = user?.name?.ifEmpty { "Add your name" } ?: "Add your name"

                val morningTime = dataManager.getMorningTime(username)
                val nightTime = dataManager.getNightTime(username)
                tvMorningTime.text = "Morning reminder: $morningTime"
                tvNightTime.text = "Evening reminder: $nightTime"

                val hydrationInterval = dataManager.getHydrationInterval(username)
                tvHydrationInterval.text = "Hydration reminder: Every $hydrationInterval hours"

                val isDarkMode = dataManager.getThemeMode(username)
                switchDarkMode.isChecked = isDarkMode

                val themeColor = dataManager.getThemeColor(username)
                tvThemeColor.text = "Theme color: $themeColor"

                loadUserStats(username)
            }
        }
    }

    private fun loadUserStats(username: String) {
        val habits = dataManager.getHabits(username)
        val moods = dataManager.getMoods(username)
        val stepsToday = dataManager.getStepsToday(username)
        val caloriesToday = dataManager.getCaloriesToday(username)

        binding.apply {
            tvTotalHabits.text = "Active habits: ${habits.size}"
            tvMoodEntries.text = "Mood entries: ${moods.size}"
            tvStepsToday.text = "Steps today: $stepsToday"
            tvCaloriesToday.text = "Calories today: ${caloriesToday.toInt()} kcal"

            val completedHabits = habits.count { it.done }
            val completionRate = if (habits.isNotEmpty()) {
                (completedHabits * 100) / habits.size
            } else 0
            tvHabitCompletion.text = "Today's completion: $completionRate%"
        }
    }

    private fun showEditProfileDialog() {
        val etName = EditText(requireContext()).apply {
            hint = "Enter your name"
            setText(binding.tvUserName.text.toString().replace("Add your name", ""))
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile ✏️")
            .setView(etName)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    currentUser?.let { username ->
                        val users = dataManager.getUsers().toMutableList()
                        val idx = users.indexOfFirst { it.username == username }
                        if (idx >= 0) {
                            val oldUser = users[idx]
                            users[idx] = oldUser.copy(name = newName)
                            dataManager.saveUsers(users)
                            WidgetUpdateManager.updateWidgets(requireContext())
                        }
                    }

                    binding.tvUserName.text = newName
                    Toast.makeText(requireContext(), "Profile updated ✅", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setMorningNotification() {
        val currentTimeStr = dataManager.getMorningTime(currentUser ?: "")
        val parts = currentTimeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            currentUser?.let { user ->
                val nightTime = dataManager.getNightTime(user)
                dataManager.setNotificationTimes(user, timeString, nightTime)
                notificationUtils.scheduleMorningNotification(selectedHour, selectedMinute)
                binding.tvMorningTime.text = "Morning reminder: $timeString"
                WidgetUpdateManager.updateWidgets(requireContext())
                Toast.makeText(requireContext(), "Morning notification updated 🌅", Toast.LENGTH_SHORT).show()
            }
        }, hour, minute, true).show()
    }

    private fun setNightNotification() {
        val currentTimeStr = dataManager.getNightTime(currentUser ?: "")
        val parts = currentTimeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 22
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            currentUser?.let { user ->
                val morningTime = dataManager.getMorningTime(user)
                dataManager.setNotificationTimes(user, morningTime, timeString)
                notificationUtils.scheduleNightNotification(selectedHour, selectedMinute)
                binding.tvNightTime.text = "Evening reminder: $timeString"
                WidgetUpdateManager.updateWidgets(requireContext())
                Toast.makeText(requireContext(), "Evening notification updated 🌙", Toast.LENGTH_SHORT).show()
            }
        }, hour, minute, true).show()
    }

    private fun setHydrationInterval() {
        val intervals = arrayOf("1 hour", "2 hours", "3 hours", "4 hours", "6 hours", "8 hours")
        val intervalValues = arrayOf(1, 2, 3, 4, 6, 8)

        AlertDialog.Builder(requireContext())
            .setTitle("Hydration Reminder Interval")
            .setItems(intervals) { _, which ->
                val selectedInterval = intervalValues[which]
                currentUser?.let { user ->
                    dataManager.setHydrationInterval(user, selectedInterval)
                    notificationUtils.scheduleHydrationReminder(selectedInterval * 60)
                    binding.tvHydrationInterval.text = "Hydration reminder: Every $selectedInterval hours"
                    WidgetUpdateManager.updateWidgets(requireContext())
                    Toast.makeText(requireContext(), "Hydration reminder updated 💧", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showThemeColorDialog() {
        val colors = arrayOf("Emerald Green", "Ocean Blue", "Sunset Orange", "Royal Purple", "Rose Pink")
        val colorValues = arrayOf("#43A047", "#2196F3", "#FF9800", "#9C27B0", "#E91E63")

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Theme Color")
            .setItems(colors) { _, which ->
                val selectedColor = colorValues[which]
                currentUser?.let { user ->
                    dataManager.setThemeColor(user, selectedColor)
                    binding.tvThemeColor.text = "Theme color: $selectedColor"
                    Toast.makeText(requireContext(), "Theme color updated", Toast.LENGTH_SHORT).show()
                    WidgetUpdateManager.updateWidgets(requireContext())
                    requireActivity().recreate()
                }
            }
            .show()
    }

    private fun shareWeeklyStats() {
        currentUser?.let { user ->
            val habits = dataManager.getHabits(user)
            val moods = dataManager.getMoods(user).takeLast(7)
            val stepsToday = dataManager.getStepsToday(user)
            val caloriesToday = dataManager.getCaloriesToday(user)

            val completedHabits = habits.count { it.done }
            val completionRate = if (habits.isNotEmpty()) {
                (completedHabits * 100) / habits.size
            } else 0

            val shareText = """
                📊 My WellSync Weekly Summary
                
                🎯 Habits: $completedHabits/${habits.size} completed ($completionRate%)
                😊 Mood Entries: ${moods.size} this week
                👣 Steps Today: $stepsToday
                🔥 Calories Today: ${caloriesToday.toInt()} kcal
                
                Staying committed to my wellness journey! 💚
                
                #WellSync #WellnessJourney #HealthyHabits
            """.trimIndent()

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Weekly Stats"))
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                dataManager.logout()
                WidgetUpdateManager.updateWidgets(requireContext())
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
