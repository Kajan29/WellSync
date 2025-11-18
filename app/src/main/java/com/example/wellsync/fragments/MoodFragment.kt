package com.example.wellsync.fragments

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wellsync.adapters.MoodHistoryAdapter
import com.example.wellsync.databinding.FragmentMoodBinding
import com.example.wellsync.models.MoodEntry
import com.example.wellsync.utils.DataManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class MoodFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentMoodBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: DataManager
    private lateinit var moodHistoryAdapter: MoodHistoryAdapter
    private var currentUser: String? = null
    private val moodHistory = mutableListOf<MoodEntry>()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val shakeThreshold = 800

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        currentUser = dataManager.getCurrentUser()

        setupSensorManager()
        setupUI()
        setupRecyclerView()
        loadMoodHistory()
        checkTodaysMood()
    }

    private fun setupSensorManager() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun setupUI() {
        binding.apply {
            btnHappy.setOnClickListener { logMood("😊", "Happy") }
            btnNeutral.setOnClickListener { logMood("😐", "Neutral") }
            btnSad.setOnClickListener { logMood("😔", "Sad") }
            btnExcited.setOnClickListener { logMood("😍", "Excited") }
            btnAngry.setOnClickListener { logMood("😠", "Angry") }

            tvShakeInstruction.text = "💡 Tip: Shake your phone to quickly add a mood entry!"
        }
    }

    private fun setupRecyclerView() {
        moodHistoryAdapter = MoodHistoryAdapter(moodHistory)
        binding.recyclerViewMoodHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moodHistoryAdapter
        }
    }

    private fun loadMoodHistory() {
        currentUser?.let { user ->
            val moods = dataManager.getMoods(user).sortedByDescending { it.timestamp }
            moodHistory.clear()
            moodHistory.addAll(moods)
            moodHistoryAdapter.notifyDataSetChanged()
        }
    }

    private fun checkTodaysMood() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayMood = moodHistory.find { it.date == today }

        binding.apply {
            if (todayMood != null) {
                tvTodayMood.text = "Today's mood: ${todayMood.emoji}"
                tvMoodStatus.text = "You've logged your mood for today!"
                cardTodayMood.alpha = 0.8f
            } else {
                tvTodayMood.text = "No mood logged today"
                tvMoodStatus.text = "How are you feeling today?"
                cardTodayMood.alpha = 1.0f
            }
        }
    }

    private fun logMood(emoji: String, description: String) {
        currentUser?.let { user ->
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val moodEntry = MoodEntry(today, emoji)

            dataManager.addMoodEntry(user, moodEntry)
            loadMoodHistory()
            checkTodaysMood()

            Toast.makeText(requireContext(), "Mood logged: $description $emoji", Toast.LENGTH_SHORT).show()

            animateButton(when (emoji) {
                "😊" -> binding.btnHappy
                "😐" -> binding.btnNeutral
                "😔" -> binding.btnSad
                "😍" -> binding.btnExcited
                "😠" -> binding.btnAngry
                else -> binding.btnHappy
            })
        }
    }

    private fun animateButton(button: View) {
        button.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                button.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun showQuickMoodDialog() {
        val moods = arrayOf("😊 Happy", "😐 Neutral", "😔 Sad", "😍 Excited", "😠 Angry")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Quick Mood Entry")
            .setItems(moods) { _, which ->
                val (emoji, description) = when (which) {
                    0 -> "😊" to "Happy"
                    1 -> "😐" to "Neutral"
                    2 -> "😔" to "Sad"
                    3 -> "😍" to "Excited"
                    4 -> "😠" to "Angry"
                    else -> "😊" to "Happy"
                }
                logMood(emoji, description)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastUpdate > 100) {
                val timeDiff = currentTime - lastUpdate
                lastUpdate = currentTime

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val speed = sqrt(((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ)).toDouble()) / timeDiff * 10000

                if (speed > shakeThreshold) {
                    showQuickMoodDialog()
                }

                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
