package com.example.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Lightweight accelerometer shake detector for shake-to-skip.
 * Automatically registered only during active playback when enabled to save battery and memory.
 */
class ShakeDetector(
    private val context: Context,
    private val onShakeListener: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var lastShakeTimestamp: Long = 0L

    companion object {
        // G-force threshold: ~2.5 - 2.8 times gravity for intentional hand shake
        private const val SHAKE_THRESHOLD_GRAVITY = 2.6f
        private const val SHAKE_COOLDOWN_MS = 1200L
    }

    fun start() {
        if (isListening || accelerometer == null || sensorManager == null) return
        try {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        } catch (e: Exception) {
            Log.w("ShakeDetector", "Failed to register sensor listener: ${e.message}")
        }
    }

    fun stop() {
        if (!isListening || sensorManager == null) return
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTimestamp > SHAKE_COOLDOWN_MS) {
                lastShakeTimestamp = now
                vibrateBriefly()
                onShakeListener()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun vibrateBriefly() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(40L)
                }
            }
        } catch (_: Exception) {}
    }
}
