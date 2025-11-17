package com.example.wellsync.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.wellsync.R
import com.example.wellsync.databinding.FragmentHomeBinding
import com.example.wellsync.utils.DataManager
import com.example.wellsync.utils.NotificationUtils
import com.example.wellsync.utils.StepTracker
import com.example.wellsync.widgets.WellSyncWidgetProvider
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: DataManager
    private lateinit var stepTracker: StepTracker
    private var currentUser: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error inflating layout: ${e.message}", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            initializeComponents()
            loadDashboardData()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onViewCreated: ${e.message}", e)
            showErrorMessage("Failed to load home page")
        }
    }

    private fun initializeComponents() {
        try {
            dataManager = DataManager(requireContext())
            currentUser = dataManager.getCurrentUser()

            if (currentUser.isNullOrEmpty()) {
                Log.w("HomeFragment", "No current user found")
                showErrorMessage("User not logged in")
                return
            }

            setupStepTracker()
            setupUI()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error initializing components: ${e.message}", e)
            throw e
        }
    }

    private fun setupStepTracker() {
        try {
            stepTracker = StepTracker(requireContext()) { steps, calories, distance, rawSensorTotal ->
                currentUser?.let { user ->
                    try {
                        if (rawSensorTotal != null) {
                            val updatedTotal = dataManager.updateStepsFromSensor(user, rawSensorTotal)

                            val updatedCalories = updatedTotal * 0.04f
                            dataManager.saveCaloriesToday(user, updatedCalories)

                            updateStepDisplay(updatedTotal, updatedCalories, distance)
                            updateDailyStepChart(user)

                        } else {
                            dataManager.saveStepsToday(user, steps)
                            dataManager.saveCaloriesToday(user, calories)

                            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            dataManager.saveHourlySteps(user, currentHour, steps)

                            updateStepDisplay(steps, calories, distance)
                            updateDailyStepChart(user)
                        }

                        val intent = Intent(WellSyncWidgetProvider.UPDATE_WIDGET_ACTION)
                        requireContext().sendBroadcast(intent)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error saving step data: ${e.message}", e)
                    }
                }
            }

            requestPermissions()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up step tracker: ${e.message}", e)
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED -> {
                    checkBodySensorsPermission()
                }
                else -> {
                    activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            stepTracker.startTracking()
        }
    }

    private fun checkBodySensorsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED -> {
                    stepTracker.startTracking()
                }
                else -> {
                    bodySensorsLauncher.launch(Manifest.permission.BODY_SENSORS)
                }
            }
        } else {
            stepTracker.startTracking()
        }
    }

    private val activityRecognitionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            checkBodySensorsPermission()
        } else {
            Toast.makeText(requireContext(), "Using accelerometer for step tracking. May be less accurate.", Toast.LENGTH_LONG).show()
            stepTracker.startTracking()
        }
    }

    private val bodySensorsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            stepTracker.startTracking()
        } else {
            Toast.makeText(requireContext(), "Step tracking may be limited without sensor permissions", Toast.LENGTH_LONG).show()
            stepTracker.startTracking()
        }
    }

    private fun setupUI() {
        try {
            setupWelcomeBoard()
            setupWaterIntakeButton()
            setupNavigationClickListeners()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up UI: ${e.message}", e)
        }
    }

    private fun setupNavigationClickListeners() {
        try {
            binding.cardWaterManagement.setOnClickListener {
                try {
                    findNavController().navigate(R.id.nav_water_limit)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error navigating to water limit: ${e.message}", e)
                    Toast.makeText(requireContext(), "Unable to open water management", Toast.LENGTH_SHORT).show()
                }
            }

            binding.cardMoodToday.setOnClickListener {
                try {
                    findNavController().navigate(R.id.nav_mood)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error navigating to mood: ${e.message}", e)
                    Toast.makeText(requireContext(), "Unable to open mood tracker", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up navigation click listeners: ${e.message}", e)
        }
    }

    private fun setupWelcomeBoard() {
        try {
            currentUser?.let { username ->
                val user = dataManager.getUserByUsername(username)
                val displayName = if (user?.name?.isNotEmpty() == true) user.name else username

                val greeting = getTimeBasedGreeting()
                val motivationalMessage = getMotivationalMessage()

                binding.apply {
                    tvWelcomeGreeting.text = greeting
                    tvWelcomeName.text = "Hello, $displayName! "
                    tvWelcomeMessage.text = motivationalMessage
                    tvCurrentDate.text = getCurrentDateString()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up welcome board: ${e.message}", e)
            binding.apply {
                tvWelcomeGreeting.text = "Welcome! "
                tvWelcomeName.text = "Hello! "
                tvWelcomeMessage.text = "Ready to start your wellness journey!"
                tvCurrentDate.text = getCurrentDateString()
            }
        }
    }

    private fun getTimeBasedGreeting(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning! "
            in 12..16 -> "Good Afternoon! "
            in 17..20 -> "Good Evening! "
            else -> "Good Night! "
        }
    }

    private fun getMotivationalMessage(): String {
        val calendar = Calendar.getInstance()
        val messages = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> listOf(
                "Ready to start your day strong? ",
                "A new day, a fresh start! ",
                "Let's make today amazing! "
            )
            in 12..16 -> listOf(
                "How's your day going so far? ",
                "Keep up the great momentum! ",
                "Stay focused on your goals! "
            )
            in 17..20 -> listOf(
                "Time to wind down and reflect! ",
                "Evening wellness time! ",
                "Let's check your progress! "
            )
            else -> listOf(
                "Time to rest and recharge! ",
                "Sweet dreams and wellness! ",
                "Rest well for tomorrow! "
            )
        }
        return messages.random()
    }

    private fun getCurrentDateString(): String {
        return try {
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            dateFormat.format(Date())
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error formatting date: ${e.message}", e)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }

    private fun setupWaterIntakeButton() {
        try {
            binding.btnAddWater.setOnClickListener {
                currentUser?.let { user ->
                    try {
                        val currentIntake = dataManager.getWaterIntakeToday(user)
                        val newIntake = currentIntake + 250 // Add 250ml
                        dataManager.saveWaterIntakeToday(user, newIntake)
                        updateWaterIntakeChart(user)

                        Toast.makeText(requireContext(), "Added 250ml water!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(WellSyncWidgetProvider.UPDATE_WIDGET_ACTION)
                        requireContext().sendBroadcast(intent)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error adding water: ${e.message}", e)
                        Toast.makeText(requireContext(), "Failed to add water", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up water intake button: ${e.message}", e)
        }
    }

    private fun loadDashboardData() {
        currentUser?.let { user ->
            try {
                lifecycleScope.launch {
                    try {
                        updateHabitsProgress(user)
                        updateTodaysMood(user)
                        updateStepsFromStorage(user)
                        updateWaterIntakeChart(user)
                        updateDailyStepChart(user)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error loading dashboard data: ${e.message}", e)
                        showErrorMessage("Some data may not be available")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error launching coroutine: ${e.message}", e)
            }
        }
    }

    private fun updateHabitsProgress(user: String) {
        try {
            val habits = dataManager.getHabits(user)
            val completedHabits = habits.count { it.done }
            val totalHabits = habits.size

            binding.apply {
                tvHabitsProgress.text = "$completedHabits / $totalHabits habits completed"
                progressBarHabits.max = if (totalHabits > 0) totalHabits else 1
                progressBarHabits.progress = completedHabits

                val percentage = if (totalHabits > 0) (completedHabits * 100) / totalHabits else 0
                tvHabitsPercentage.text = "$percentage%"
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating habits progress: ${e.message}", e)
            binding.apply {
                tvHabitsProgress.text = "0 / 0 habits completed"
                tvHabitsPercentage.text = "0%"
            }
        }
    }

    private fun updateTodaysMood(user: String) {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val moods = dataManager.getMoods(user)
            val todayMood = moods.find { it.date == today }

            binding.apply {
                if (todayMood != null) {
                    tvMoodToday.text = "Today's mood: ${todayMood.emoji}"
                    tvMoodStatus.text = "Mood logged for today"
                } else {
                    tvMoodToday.text = "No mood logged yet"
                    tvMoodStatus.text = "Tap to log your mood"
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating mood: ${e.message}", e)
            binding.apply {
                tvMoodToday.text = "Mood data unavailable"
                tvMoodStatus.text = "Tap to log your mood"
            }
        }
    }

    private fun updateStepsFromStorage(user: String) {
        try {
            val steps = dataManager.getStepsToday(user)
            val calories = dataManager.getCaloriesToday(user)
            val distance = (steps * 0.8f) / 1000f
            updateStepDisplay(steps, calories, distance)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating steps: ${e.message}", e)
            updateStepDisplay(0, 0f, 0f)
        }
    }

    private fun updateStepDisplay(steps: Int, calories: Float, distance: Float) {
        try {
            currentUser?.let { user ->
                val stepGoal = dataManager.getStepGoal(user)
                val percentage = if (stepGoal > 0) (steps * 100) / stepGoal else 0

                binding.apply {
                    tvStepPercentage.text = "$percentage%"
                    tvStepsProgress.text = "$steps / $stepGoal steps"
                    tvDistance.text = "${"%.2f".format(distance)} km"
                    tvCalories.text = "${calories.toInt()} kcal"
                    tvStepGoal.text = "Goal: ${String.format("%,d", stepGoal)} steps daily"
                }

                setupStepProgressChart(steps, stepGoal)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating step display: ${e.message}", e)
            binding.apply {
                tvStepPercentage.text = "0%"
                tvStepsProgress.text = "0 / 10000 steps"
                tvDistance.text = "0.00 km"
                tvCalories.text = "0 kcal"
                tvStepGoal.text = "Goal: 10,000 steps daily"
            }
        }
    }

    private fun setupStepProgressChart(current: Int, goal: Int) {
        try {
            val entries = mutableListOf<PieEntry>()

            if (current > 0) {
                val completed = current.coerceAtMost(goal)
                entries.add(PieEntry(completed.toFloat(), "Completed"))
            }

            val remaining = (goal - current).coerceAtLeast(0)
            if (remaining > 0) {
                entries.add(PieEntry(remaining.toFloat(), "Remaining"))
            }

            if (entries.isNotEmpty()) {
                val dataSet = PieDataSet(entries, "")
                dataSet.apply {
                    colors = if (current >= goal) {
                        listOf(
                            requireContext().getColor(android.R.color.holo_green_light),
                            requireContext().getColor(android.R.color.holo_green_dark)
                        )
                    } else {
                        listOf(
                            requireContext().getColor(android.R.color.holo_blue_bright),
                            requireContext().getColor(android.R.color.holo_blue_light)
                        )
                    }
                    setDrawValues(false)
                    sliceSpace = 2f
                }

                val pieData = PieData(dataSet)
                binding.chartStepProgress.apply {
                    data = pieData
                    description.isEnabled = false
                    legend.isEnabled = false
                    setDrawEntryLabels(false)
                    setDrawCenterText(true)
                    centerText = "${current}\nsteps"
                    setCenterTextSize(16f)
                    setHoleRadius(65f)
                    animateY(1000)
                    invalidate()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up step chart: ${e.message}", e)
        }
    }

    private fun updateWaterIntakeChart(user: String) {
        try {
            val currentIntake = dataManager.getWaterIntakeToday(user)
            val dailyGoalGlasses = dataManager.getWaterGoal(user)
            val dailyGoalMl = dailyGoalGlasses * 250

            val currentIntakeGlasses = currentIntake / 250f

            binding.apply {
                tvWaterProgress.text = "${String.format("%.1f", currentIntakeGlasses)} / ${dailyGoalGlasses} glasses"
                val percentage = if (dailyGoalMl > 0) (currentIntake * 100f) / dailyGoalMl else 0f
                tvWaterPercentage.text = "${percentage.toInt()}%"
                progressBarWater.max = dailyGoalMl
                progressBarWater.progress = currentIntake
                tvWaterGoal.text = "Goal: ${String.format("%.1f", dailyGoalGlasses * 0.25)}L daily"
            }

            setupWaterIntakeChart(currentIntake, dailyGoalMl)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating water chart: ${e.message}", e)
            binding.apply {
                tvWaterProgress.text = "0 / 8 glasses"
                tvWaterPercentage.text = "0%"
                progressBarWater.max = 2000
                progressBarWater.progress = 0
                tvWaterGoal.text = "Goal: 2.0L daily"
            }
        }
    }

    private fun setupWaterIntakeChart(currentIntake: Int, dailyGoalMl: Int) {
        try {
            val entries = mutableListOf<PieEntry>()

            if (currentIntake > 0) {
                val consumed = currentIntake.coerceAtMost(dailyGoalMl)
                entries.add(PieEntry(consumed.toFloat(), "Consumed"))
            }

            val remaining = (dailyGoalMl - currentIntake).coerceAtLeast(0)
            if (remaining > 0) {
                entries.add(PieEntry(remaining.toFloat(), "Remaining"))
            }

            if (entries.isNotEmpty()) {
                val dataSet = PieDataSet(entries, "")
                dataSet.apply {
                    colors = if (currentIntake >= dailyGoalMl) {
                        listOf(
                            requireContext().getColor(android.R.color.holo_green_light),
                            requireContext().getColor(android.R.color.holo_green_dark)
                        )
                    } else {
                        listOf(
                            requireContext().getColor(android.R.color.holo_blue_bright),
                            requireContext().getColor(android.R.color.holo_blue_light)
                        )
                    }
                    setDrawValues(false)
                    sliceSpace = 2f
                }

                val pieData = PieData(dataSet)
                binding.chartWaterIntake.apply {
                    data = pieData
                    description.isEnabled = false
                    legend.isEnabled = false
                    setDrawEntryLabels(false)
                    setDrawCenterText(true)
                    centerText = "${currentIntake / 250} / ${dailyGoalMl / 250} glasses"
                    setCenterTextSize(16f)
                    setHoleRadius(65f)
                    animateY(1000)
                    invalidate()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up water intake chart: ${e.message}", e)
        }
    }

    private fun updateDailyStepChart(user: String) {
        try {
            val hourlySteps = dataManager.getTodayHourlySteps(user)
            val entries = mutableListOf<Entry>()
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            for (hour in 6..minOf(currentHour + 1, 23)) {
                val steps = hourlySteps[hour] ?: 0
                entries.add(Entry(hour.toFloat(), steps.toFloat()))
            }

            if (entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, "Steps Throughout Day")
                dataSet.apply {
                    color = requireContext().getColor(android.R.color.holo_green_dark)
                    lineWidth = 3f
                    circleRadius = 5f
                    setDrawValues(false)
                }

                val lineData = LineData(dataSet)
                binding.chartDailySteps.apply {
                    data = lineData
                    description.isEnabled = false
                    legend.isEnabled = false
                    invalidate()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating daily step chart: ${e.message}", e)
        }
    }

    private fun showErrorMessage(message: String) {
        try {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error showing toast: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            loadDashboardData()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onResume: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (::stepTracker.isInitialized) {
                stepTracker.stopTracking()
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error stopping step tracker: ${e.message}", e)
        }
        _binding = null
    }
}
