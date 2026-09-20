package com.wapo.flagship.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects phone shake gestures using the accelerometer.
 * Requires 5 back-and-forth shakes within a time window to trigger.
 */
class ShakeDetector(
    private val context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var isFirstReading = true

    // Shake counting
    private var shakeCount = 0
    private var firstShakeTime: Long = 0
    private var lastShakeTime: Long = 0

    companion object {
        private const val SHAKE_THRESHOLD = 12.0f // Acceleration threshold for shake
        private const val REQUIRED_SHAKES = 5 // Number of shakes required
        private const val SHAKE_WINDOW_MS = 1500L // Time window to complete all shakes
        private const val MIN_TIME_BETWEEN_SHAKES_MS = 100L // Minimum time between individual shakes
    }

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        resetShakeCount()
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        resetShakeCount()
    }

    private fun resetShakeCount() {
        shakeCount = 0
        firstShakeTime = 0
        lastShakeTime = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (isFirstReading) {
            lastX = x
            lastY = y
            lastZ = z
            isFirstReading = false
            return
        }

        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        lastX = x
        lastY = y
        lastZ = z

        val acceleration = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        if (acceleration > SHAKE_THRESHOLD) {
            val currentTime = System.currentTimeMillis()

            // Check if enough time has passed since last shake (debounce)
            if (currentTime - lastShakeTime < MIN_TIME_BETWEEN_SHAKES_MS) {
                return
            }

            // Check if shake window has expired, reset if so
            if (shakeCount > 0 && currentTime - firstShakeTime > SHAKE_WINDOW_MS) {
                resetShakeCount()
            }

            // Record this shake
            if (shakeCount == 0) {
                firstShakeTime = currentTime
            }
            shakeCount++
            lastShakeTime = currentTime

            // Check if we've reached required number of shakes
            if (shakeCount >= REQUIRED_SHAKES) {
                resetShakeCount()
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}

