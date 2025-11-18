package com.example.wellsync.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

class StepTracker(private val context: Context, private val onStepDetected: (steps: Int, calories: Float, distance: Float, rawSensorTotal: Int?) -> Unit) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelSensor: Sensor? = null

    private var stepCount = 0
    private var initialStepCount = -1
    private var isTracking = false

    // Keep last raw sensor total for step counter sensors
    private var lastRawSensorTotal: Int = -1

    // Accelerometer-based step detection variables
    private var lastAccel = SensorManager.GRAVITY_EARTH
    private var currentAccel = SensorManager.GRAVITY_EARTH
    private var acceleration = 0f
    private val stepThreshold = 2.0f
    private var lastStepTime = 0L
    private val minStepInterval = 300L // Minimum 300ms between steps

    // Improved accelerometer-based step detection variables
    private val accelValues = mutableListOf<Float>()
    private val accelWindowSize = 50 // Window size for smoothing
    private val peakThreshold = 1.2f // Threshold for peak detection
    private val minPeakInterval = 400L // Minimum time between peaks

    // Advanced accelerometer-based step detection variables
    private val accelBuffer = mutableListOf<Float>()
    private val filteredAccelBuffer = mutableListOf<Float>()
    private val bufferSize = 100
    private var dynamicThreshold = 1.0f
    private var lastStepTimeAccel = 0L
    private val minStepIntervalAccel = 250L // Minimum 250ms between steps

    // Filter coefficients
    private val alpha = 0.8f // Low-pass filter coefficient
    private var filteredAccel = 0f
    private var prevFilteredAccel = 0f

    // Peak detection
    private var peakValue = 0f
    private var valleyValue = Float.MAX_VALUE
    private var isLookingForPeak = true
    private var lastPeakTime = 0L
    private val peakInterval = 300L

    // Device-specific adjustments
    private val isXiaomiDevice = Build.MANUFACTURER.lowercase().contains("xiaomi") ||
                                 Build.BRAND.lowercase().contains("xiaomi") ||
                                 Build.MODEL.lowercase().contains("redmi")

    // Adjusted thresholds for Xiaomi devices
    private val xiaomiStepThreshold = 1.5f // Lower threshold for Xiaomi
    private val xiaomiPeakThreshold = 1.1f // Lower peak threshold for Xiaomi

    init {
        initializeSensors()
    }

    private fun initializeSensors() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Try to get step counter sensor (most accurate)
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Try to get step detector sensor (alternative)
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // Get accelerometer as fallback
        accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun startTracking() {
        if (isTracking) return

        // Check for runtime permissions on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, use accelerometer fallback
                startAccelerometerTracking()
                return
            }
        }

        var sensorRegistered = false

        // Try step counter first (most accurate)
        stepCounterSensor?.let { sensor ->
            val registered = sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI) ?: false
            if (registered) {
                sensorRegistered = true
                initialStepCount = -1 // Reset for new session
            }
        }

        // If step counter failed, try step detector
        if (!sensorRegistered) {
            stepDetectorSensor?.let { sensor ->
                sensorRegistered = sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI) ?: false
            }
        }

        // If both step sensors failed, use accelerometer
        if (!sensorRegistered) {
            startAccelerometerTracking()
        }

        isTracking = sensorRegistered || accelSensor != null
    }

    private fun startAccelerometerTracking() {
        accelSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            resetAccelerometerValues()
        }
    }

    private fun resetAccelerometerValues() {
        lastAccel = SensorManager.GRAVITY_EARTH
        currentAccel = SensorManager.GRAVITY_EARTH
        acceleration = 0f
        lastStepTime = 0L
        accelValues.clear()
        accelBuffer.clear()
        filteredAccelBuffer.clear()
        filteredAccel = 0f
        prevFilteredAccel = 0f
        peakValue = 0f
        valleyValue = Float.MAX_VALUE
        isLookingForPeak = true
        lastPeakTime = 0L
        lastStepTimeAccel = 0L
    }

    fun stopTracking() {
        if (!isTracking) return

        sensorManager?.unregisterListener(this)
        isTracking = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking) return

        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    handleStepCounterEvent(sensorEvent)
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    handleStepDetectorEvent()
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    handleAccelerometerEvent(sensorEvent)
                }
            }
        }
    }

    private fun handleStepCounterEvent(event: SensorEvent) {
        val totalSteps = event.values[0].toInt()

        // Store last raw sensor total for consumers who need it
        lastRawSensorTotal = totalSteps

        if (initialStepCount == -1) {
            // First reading - set as baseline
            initialStepCount = totalSteps
            stepCount = 0
        } else {
            // Calculate steps since tracking started
            stepCount = totalSteps - initialStepCount
            if (stepCount < 0) stepCount = 0
        }

        // Pass both the delta (stepCount) and the raw sensor total
        updateStats(rawSensorTotal = totalSteps)
    }

    private fun handleStepDetectorEvent() {
        stepCount++
        updateStats(rawSensorTotal = null)
    }

    private fun handleAccelerometerEvent(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate current acceleration magnitude
        val magnitude = sqrt(x * x + y * y + z * z)

        // Add to rolling window for analysis
        accelValues.add(magnitude)
        if (accelValues.size > accelWindowSize) {
            accelValues.removeAt(0)
        }

        // Only start detecting after we have enough data
        if (accelValues.size < accelWindowSize / 2) return

        // Detect steps using multiple methods for better accuracy
        val currentTime = System.currentTimeMillis()

        // Method 1: Peak detection
        if (isPeakDetected(magnitude, accelValues)) {
            if (lastPeakTime == 0L || currentTime - lastPeakTime > minPeakInterval) {
                stepCount++
                lastPeakTime = currentTime
                updateStats(rawSensorTotal = null)
            }
        }

        // Method 2: Threshold-based detection (backup)
        val currentThreshold = if (isXiaomiDevice) xiaomiStepThreshold else stepThreshold
        val filteredAccel = magnitude - SensorManager.GRAVITY_EARTH // Remove gravity
        if (filteredAccel > currentThreshold &&
            (lastStepTime == 0L || currentTime - lastStepTime > minStepInterval)) {
            // Only use threshold if peak detection hasn't triggered recently
            if (lastPeakTime == 0L || currentTime - lastPeakTime > minPeakInterval / 2) {
                stepCount++
                lastStepTime = currentTime
                updateStats(rawSensorTotal = null)
            }
        }

        // Advanced detection using dynamic thresholding and filtering
        if (isAdvancedDetectionEnabled()) {
            // Update accelerometer buffers
            updateAccelBuffers(magnitude)

            // Dynamic threshold adjustment based on recent activity
            adjustDynamicThreshold()

            // Step detection based on zero-crossing of filtered signal
            detectStepsFromFilteredSignal(currentTime)
        }
    }

    private fun isPeakDetected(currentValue: Float, window: List<Float>): Boolean {
        if (window.size < accelWindowSize) return false

        val currentPeakThreshold = if (isXiaomiDevice) xiaomiPeakThreshold else peakThreshold

        // Simple peak detection: check if current value is greater than previous values in the window
        val isPeak = window.takeLast(accelWindowSize).all { it < currentValue } &&
                     window.takeLast(accelWindowSize).first() < currentValue * currentPeakThreshold

        return isPeak
    }

    private fun updateStats(rawSensorTotal: Int? = null) {
        val distance = (stepCount * 0.8f) / 1000f // km (assuming 0.8m per step)
        val calories = stepCount * 0.04f // kcal (rough estimate: 0.04 calories per step)
        onStepDetected(stepCount, calories, distance, rawSensorTotal)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    fun resetDailySteps() {
        stepCount = 0
        initialStepCount = -1
        resetAccelerometerValues()
    }

    fun getCurrentSteps(): Int = stepCount

    fun isStepCounterAvailable(): Boolean = stepCounterSensor != null

    fun isStepDetectorAvailable(): Boolean = stepDetectorSensor != null

    fun isAccelerometerAvailable(): Boolean = accelSensor != null

    fun getSensorInfo(): String {
        return when {
            stepCounterSensor != null -> "Using Step Counter Sensor"
            stepDetectorSensor != null -> "Using Step Detector Sensor"
            accelSensor != null -> "Using Accelerometer Fallback"
            else -> "No sensors available"
        }
    }

    // Enhanced sensor availability checks
    fun hasStepCounterSensor(): Boolean {
        return stepCounterSensor != null
    }

    fun hasStepDetectorSensor(): Boolean {
        return stepDetectorSensor != null
    }

    fun hasAccelerometerSensor(): Boolean {
        return accelSensor != null
    }

    fun hasAnySensor(): Boolean {
        return stepCounterSensor != null || stepDetectorSensor != null || accelSensor != null
    }

    fun getDetailedSensorInfo(): String {
        val sb = StringBuilder()
        sb.append("Sensor Availability Report:\n")

        stepCounterSensor?.let { sensor ->
            sb.append("✓ Step Counter: ${sensor.name} (Vendor: ${sensor.vendor})\n")
            sb.append("  Max Range: ${sensor.maximumRange}, Power: ${sensor.power}mA\n")
        } ?: sb.append("✗ Step Counter: Not available\n")

        stepDetectorSensor?.let { sensor ->
            sb.append("✓ Step Detector: ${sensor.name} (Vendor: ${sensor.vendor})\n")
            sb.append("  Max Range: ${sensor.maximumRange}, Power: ${sensor.power}mA\n")
        } ?: sb.append("✗ Step Detector: Not available\n")

        accelSensor?.let { sensor ->
            sb.append("✓ Accelerometer: ${sensor.name} (Vendor: ${sensor.vendor})\n")
            sb.append("  Max Range: ${sensor.maximumRange}m/s², Power: ${sensor.power}mA\n")
        } ?: sb.append("✗ Accelerometer: Not available\n")

        sb.append("\nPermission Status:\n")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            // Proper interpolation: escape inner quotes
            sb.append("Activity Recognition: ${if (hasPermission) "✓ Granted" else "✗ Not granted"}\n")
        } else {
            sb.append("Activity Recognition: ✓ Not required (Android < 10)\n")
        }

        return sb.toString()
    }

    fun validateSensorFunctionality(): ValidationResult {
        val result = ValidationResult()

        // Check sensor availability
        if (!hasAnySensor()) {
            result.isValid = false
            result.issues.add("No step sensors or accelerometer available on this device")
            return result
        }

        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission && !hasAccelerometerSensor()) {
                result.isValid = false
                result.issues.add("Activity Recognition permission required for step sensors")
            }
        }

        // Prefer step counter over step detector over accelerometer
        when {
            hasStepCounterSensor() -> {
                result.recommendedSensor = "Step Counter (Most Accurate)"
                result.accuracy = "High"
            }
            hasStepDetectorSensor() -> {
                result.recommendedSensor = "Step Detector (Good)"
                result.accuracy = "Medium"
            }
            hasAccelerometerSensor() -> {
                result.recommendedSensor = "Accelerometer Fallback (Basic)"
                result.accuracy = "Low"
                result.warnings.add("Using accelerometer fallback - accuracy may be limited")
            }
        }

        return result
    }

    private fun isAdvancedDetectionEnabled(): Boolean {
        // Enable advanced detection if we have an accelerometer and no step counter/detector available
        return accelSensor != null && stepCounterSensor == null && stepDetectorSensor == null
    }

    private fun updateAccelBuffers(currentValue: Float) {
        // Add new value to buffer
        accelBuffer.add(currentValue)
        if (accelBuffer.size > bufferSize) {
            accelBuffer.removeAt(0)
        }

        // Apply low-pass filter
        filteredAccel = alpha * filteredAccel + (1 - alpha) * currentValue

        // Add filtered value to buffer
        filteredAccelBuffer.add(filteredAccel)
        if (filteredAccelBuffer.size > bufferSize) {
            filteredAccelBuffer.removeAt(0)
        }
    }

    private fun adjustDynamicThreshold() {
        // Adjust threshold based on recent step activity
        val recentSteps = accelBuffer.takeLast(10)
        val averageStep = recentSteps.average().toFloat()

        dynamicThreshold = if (averageStep > 0) {
            averageStep * 0.7f // Set threshold to 70% of average step size
        } else {
            1.0f // Default threshold
        }
    }

    private fun detectStepsFromFilteredSignal(currentTime: Long) {
        // Proper step detection using peak/valley detection on filtered signal
        val currentFiltered = filteredAccelBuffer.lastOrNull() ?: return
        val prevFiltered = if (filteredAccelBuffer.size > 1) filteredAccelBuffer[filteredAccelBuffer.size - 2] else currentFiltered

        // Update peak/valley tracking
        if (isLookingForPeak) {
            if (currentFiltered > peakValue) {
                peakValue = currentFiltered
            } else if (currentFiltered < peakValue - dynamicThreshold) {
                // Found a peak, now look for valley
                isLookingForPeak = false
                valleyValue = currentFiltered
            }
        } else {
            if (currentFiltered < valleyValue) {
                valleyValue = currentFiltered
            } else if (currentFiltered > valleyValue + dynamicThreshold) {
                // Found a valley, now look for peak
                // Check if this constitutes a step
                val stepAmplitude = peakValue - valleyValue
                if (stepAmplitude > dynamicThreshold &&
                    currentTime - lastStepTimeAccel > minStepIntervalAccel) {
                    stepCount++
                    lastStepTimeAccel = currentTime
                    updateStats(rawSensorTotal = null)
                }

                isLookingForPeak = true
                peakValue = currentFiltered
            }
        }
    }

    data class ValidationResult(
        var isValid: Boolean = true,
        var recommendedSensor: String = "",
        var accuracy: String = "",
        val issues: MutableList<String> = mutableListOf(),
        val warnings: MutableList<String> = mutableListOf()
    )
}
