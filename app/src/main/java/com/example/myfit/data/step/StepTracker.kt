package com.example.myfit.data.step

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class StepTracker(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = appContext.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)

    val isSensorAvailable = stepSensor != null

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private var registered = false

    init {
        // Сразу загружаем последнее сохранённое значение
        val today = LocalDate.now().toString()
        val base = prefs.getInt("base_$today", -1)
        if (base != -1) {
            val lastKnown = prefs.getInt("last_steps_$today", base)
            _todaySteps.value = (lastKnown - base).coerceAtLeast(0)
        }
    }

    // Вызывается из Application.onCreate() и из PedometerScreen после получения разрешения
    fun start(context: Context = appContext) {
        if (registered || stepSensor == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) return

        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        registered = true
    }

    override fun onSensorChanged(event: SensorEvent) {
        val stepsFromBoot = event.values[0].toInt()
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val baseKey = "base_$today"
        val base = prefs.getInt(baseKey, -1)

        when {
            base == -1 -> {
                val yesterdayLast = prefs.getInt("last_steps_$yesterday", -1)
                val baseline = if (yesterdayLast in 1..stepsFromBoot) yesterdayLast else stepsFromBoot
                prefs.edit().putInt(baseKey, baseline).apply()
                _todaySteps.value = (stepsFromBoot - baseline).coerceAtLeast(0)
            }
            stepsFromBoot < base -> {
                // Телефон перезагружали — счётчик сбросился
                prefs.edit().putInt(baseKey, 0).apply()
                _todaySteps.value = stepsFromBoot
            }
            else -> {
                _todaySteps.value = stepsFromBoot - base
            }
        }
        prefs.edit().putInt("last_steps_$today", stepsFromBoot).apply()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
