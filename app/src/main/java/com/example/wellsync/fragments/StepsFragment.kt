package com.example.wellsync.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wellsync.databinding.FragmentStepsBinding
import com.example.wellsync.utils.DataManager
import com.example.wellsync.utils.StepTracker

class StepsFragment : Fragment() {

    private var _binding: FragmentStepsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: DataManager
    private lateinit var stepTracker: StepTracker
    private var currentUser: String? = null
    private var stepGoal = 10000

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStepsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        currentUser = dataManager.getCurrentUser()

        setupStepTracker()
        setupUI()
        loadTodaysData()
    }

    private fun setupStepTracker() {
        stepTracker = StepTracker(requireContext()) { steps, calories, distance, rawSensorTotal ->
            currentUser?.let { user ->
                if (rawSensorTotal != null) {
                    val updatedTotal = dataManager.updateStepsFromSensor(user, rawSensorTotal)
                    val updatedCalories = updatedTotal * 0.04f
                    dataManager.saveCaloriesToday(user, updatedCalories)
                    updateUI(updatedTotal, updatedCalories, distance)
                } else {
                    dataManager.saveStepsToday(user, steps)
                    dataManager.saveCaloriesToday(user, calories)
                    updateUI(steps, calories, distance)
                }
            }
        }
    }

    private fun setupUI() {
        loadStepGoal()
        updateGoalDisplay()

        binding.btnResetSteps.setOnClickListener {
            resetDailySteps()
        }
    }

    private fun loadStepGoal() {
        currentUser?.let { user ->
            stepGoal = dataManager.getStepGoal(user)
        }
    }

    private fun updateGoalDisplay() {
        binding.tvStepGoal.text = "Goal: $stepGoal steps"
    }

    private fun loadTodaysData() {
        currentUser?.let { user ->
            val steps = dataManager.getStepsToday(user)
            val calories = dataManager.getCaloriesToday(user)
            val distance = (steps * 0.8f) / 1000f
            updateUI(steps, calories, distance)
        }
    }

    private fun updateUI(steps: Int, calories: Float, distance: Float) {
        binding.apply {
            tvStepsCount.text = steps.toString()
            tvStepsLabel.text = "Steps Today"

            progressBarSteps.max = stepGoal
            progressBarSteps.progress = steps.coerceAtMost(stepGoal)

            val progressPercentage = ((steps.toFloat() / stepGoal) * 100).toInt().coerceAtMost(100)
            tvProgressPercentage.text = "$progressPercentage%"

            tvDistance.text = "${"%.2f".format(distance)} km"
            tvDistanceLabel.text = "Distance Covered"

            tvCalories.text = "${calories.toInt()} kcal"
            tvCaloriesLabel.text = "Calories Burned"

            if (steps >= stepGoal) {
                tvGoalStatus.text = "🎉 Goal Achieved!"
                tvGoalStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                cardGoalStatus.setCardBackgroundColor(requireContext().getColor(android.R.color.holo_green_light))
            } else {
                val remaining = stepGoal - steps
                tvGoalStatus.text = "$remaining steps to go!"
                tvGoalStatus.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
                cardGoalStatus.setCardBackgroundColor(requireContext().getColor(android.R.color.holo_orange_light))
            }

            val motivationMessage = when {
                steps == 0 -> "Let's get moving! Every step counts 👣"
                steps < (stepGoal * 0.25).toInt() -> "Great start! Keep it up 🚶‍♀️"
                steps < (stepGoal * 0.50).toInt() -> "You're on your way! 💪"
                steps < (stepGoal * 0.75).toInt() -> "Fantastic progress! Almost there 🔥"
                steps < stepGoal -> "So close to your goal! 🎯"
                else -> "Amazing! You've exceeded your goal! 🌟"
            }
            tvMotivation.text = motivationMessage
        }
    }

    private fun resetDailySteps() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Daily Steps")
            .setMessage("This will reset your step count for today. Are you sure?")
            .setPositiveButton("Reset") { _, _ ->
                stepTracker.resetDailySteps()
                currentUser?.let { user ->
                    dataManager.saveStepsToday(user, 0)
                    dataManager.saveCaloriesToday(user, 0f)
                }
                updateUI(0, 0f, 0f)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        stepTracker.startTracking()
    }

    override fun onPause() {
        super.onPause()
        stepTracker.stopTracking()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
