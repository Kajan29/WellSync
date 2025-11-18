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
import kotlinx.coroutines.*

class SensorDiagnostics(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    data class DiagnosticReport(
        val deviceSupportsStepCounter: Boolean,
        val hasActivityRecognitionPermission: Boolean,
        val availableSensors: List<SensorInfo>,
        val recommendations: List<String>,
        val testResults: MutableList<TestResult> = mutableListOf()
    )

    data class SensorInfo(
        val name: String,
        val type: Int,
        val vendor: String,
        val version: Int,
        val maxRange: Float,
        val resolution: Float,
        val power: Float,
        val isWakeUpSensor: Boolean
    )

    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val message: String,
        val details: String = ""
    )

    fun runComprehensiveDiagnostics(): DiagnosticReport {
        val availableSensors = getAllRelevantSensors()
        val hasPermission = checkActivityRecognitionPermission()
        val deviceSupports = availableSensors.any { it.type == Sensor.TYPE_STEP_COUNTER || it.type == Sensor.TYPE_STEP_DETECTOR }

        val recommendations = generateRecommendations(availableSensors, hasPermission, deviceSupports)

        return DiagnosticReport(
            deviceSupportsStepCounter = deviceSupports,
            hasActivityRecognitionPermission = hasPermission,
            availableSensors = availableSensors,
            recommendations = recommendations
        )
    }

    suspend fun runLiveSensorTest(durationSeconds: Int = 10): List<TestResult> = withContext(Dispatchers.IO) {
        val testResults = mutableListOf<TestResult>()

        testStepCounterSensor(testResults, durationSeconds)

        testStepDetectorSensor(testResults, durationSeconds)

        testAccelerometerSensor(testResults, durationSeconds)

        return@withContext testResults
    }

    private suspend fun testStepCounterSensor(results: MutableList<TestResult>, duration: Int) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (sensor == null) {
            results.add(TestResult(
                "Step Counter Sensor",
                false,
                "Step Counter sensor not available on this device",
                "This device does not have a dedicated step counting sensor"
            ))
            return
        }

        var stepCountReceived = false
        var initialValue = -1f
        var finalValue = -1f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (initialValue == -1f) {
                        initialValue = it.values[0]
                    }
                    finalValue = it.values[0]
                    stepCountReceived = true
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        if (!registered) {
            results.add(TestResult(
                "Step Counter Sensor",
                false,
                "Failed to register step counter sensor listener",
                "The sensor exists but couldn't be accessed. Check permissions."
            ))
            return
        }

        delay(duration * 1000L)
        sensorManager.unregisterListener(listener)

        results.add(TestResult(
            "Step Counter Sensor",
            stepCountReceived,
            if (stepCountReceived) "Step Counter sensor is working (Initial: ${initialValue.toInt()}, Final: ${finalValue.toInt()})"
            else "Step Counter sensor registered but no data received",
            "Sensor: ${sensor.name} by ${sensor.vendor}"
        ))
    }

    private suspend fun testStepDetectorSensor(results: MutableList<TestResult>, duration: Int) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (sensor == null) {
            results.add(TestResult(
                "Step Detector Sensor",
                false,
                "Step Detector sensor not available on this device"
            ))
            return
        }

        var stepsDetected = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    stepsDetected++
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        if (!registered) {
            results.add(TestResult(
                "Step Detector Sensor",
                false,
                "Failed to register step detector sensor listener"
            ))
            return
        }

        delay(duration * 1000L)
        sensorManager.unregisterListener(listener)

        results.add(TestResult(
            "Step Detector Sensor",
            true,
            "Step Detector sensor test completed - detected $stepsDetected steps during ${duration}s test",
            "Sensor: ${sensor.name} by ${sensor.vendor}"
        ))
    }

    private suspend fun testAccelerometerSensor(results: MutableList<TestResult>, duration: Int) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensor == null) {
            results.add(TestResult(
                "Accelerometer Sensor",
                false,
                "Accelerometer sensor not available (this is very unusual)"
            ))
            return
        }

        var dataPointsReceived = 0
        var maxAcceleration = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    dataPointsReceived++
                    val magnitude = kotlin.math.sqrt(
                        it.values[0] * it.values[0] +
                        it.values[1] * it.values[1] +
                        it.values[2] * it.values[2]
                    )
                    if (magnitude > maxAcceleration) {
                        maxAcceleration = magnitude
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        if (!registered) {
            results.add(TestResult(
                "Accelerometer Sensor",
                false,
                "Failed to register accelerometer sensor listener"
            ))
            return
        }

        delay(duration * 1000L)
        sensorManager.unregisterListener(listener)

        val dataRate = dataPointsReceived.toFloat() / duration

        results.add(TestResult(
            "Accelerometer Sensor",
            dataPointsReceived > 0,
            "Accelerometer test: $dataPointsReceived data points in ${duration}s (${dataRate.toInt()} Hz), max acceleration: ${"%.2f".format(maxAcceleration)} m/s²",
            "Sensor: ${sensor.name} by ${sensor.vendor}"
        ))
    }

    private fun getAllRelevantSensors(): List<SensorInfo> {
        val relevantSensorTypes = listOf(
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD
        )

        return sensorManager.getSensorList(Sensor.TYPE_ALL)
            .filter { it.type in relevantSensorTypes }
            .map { sensor ->
                SensorInfo(
                    name = sensor.name,
                    type = sensor.type,
                    vendor = sensor.vendor,
                    version = sensor.version,
                    maxRange = sensor.maximumRange,
                    resolution = sensor.resolution,
                    power = sensor.power,
                    isWakeUpSensor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        sensor.isWakeUpSensor
                    } else false
                )
            }
    }

    private fun checkActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun generateRecommendations(
        sensors: List<SensorInfo>,
        hasPermission: Boolean,
        deviceSupports: Boolean
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (!deviceSupports) {
            recommendations.add("Your device doesn't have dedicated step counting sensors. The app will use accelerometer-based step detection.")
            recommendations.add("For better accuracy, consider upgrading to a device with built-in step sensors.")
        }

        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recommendations.add("Grant 'Activity Recognition' permission for more accurate step counting.")
        }

        val hasAccelerometer = sensors.any { it.type == Sensor.TYPE_ACCELEROMETER }
        if (!hasAccelerometer) {
            recommendations.add("No accelerometer detected - this is unusual and may indicate a hardware issue.")
        }

        val hasGyroscope = sensors.any { it.type == Sensor.TYPE_GYROSCOPE }
        if (!hasGyroscope) {
            recommendations.add("No gyroscope sensor detected - step detection accuracy may be reduced.")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Your device has excellent sensor support for step counting!")
        }

        return recommendations
    }

    fun getSensorTypeString(type: Int): String {
        return when (type) {
            Sensor.TYPE_STEP_COUNTER -> "Step Counter"
            Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
            else -> "Unknown Sensor (Type: $type)"
        }
    }
}
