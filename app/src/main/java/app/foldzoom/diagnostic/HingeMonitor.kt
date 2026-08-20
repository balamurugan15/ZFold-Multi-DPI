package app.foldzoom.diagnostic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

enum class FoldPosture { FOLDED, FLEX, UNFOLDED, UNSUPPORTED }

class HingeMonitor(context: Context, private val onPosture: (Float, FoldPosture) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val hingeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
    private var lastPosture: FoldPosture? = null

    fun start(): Boolean {
        val sensor = hingeSensor ?: return false
        return sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val angle = event.values.firstOrNull() ?: return
        val posture = when {
            angle <= 25f -> FoldPosture.FOLDED
            angle >= 165f -> FoldPosture.UNFOLDED
            else -> FoldPosture.FLEX
        }
        if (posture != lastPosture) {
            lastPosture = posture
            onPosture(angle, posture)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
