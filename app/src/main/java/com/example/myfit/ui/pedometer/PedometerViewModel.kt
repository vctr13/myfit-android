package com.example.myfit.ui.pedometer

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import kotlinx.coroutines.launch
import java.time.LocalDate

class PedometerViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = application.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
    private val profileDao = MyFitApp.from(application).database.userProfileDao()

    val isSensorAvailable = stepSensor != null

    var todaySteps by mutableStateOf(0)
        private set
    var weightKg by mutableStateOf(70f)
        private set
    val stepGoal = 10_000

    init {
        viewModelScope.launch {
            profileDao.getProfileOnce()?.let { weightKg = it.weight_kg }
        }
        // Сразу показываем последнее сохранённое значение — не ждём срабатывания сенсора
        val today = LocalDate.now().toString()
        val base = prefs.getInt("base_$today", -1)
        if (base != -1) {
            val lastKnown = prefs.getInt("last_steps_$today", base)
            todaySteps = (lastKnown - base).coerceAtLeast(0)
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val stepsFromBoot = event.values[0].toInt()
            val today = LocalDate.now().toString()
            val yesterday = LocalDate.now().minusDays(1).toString()
            val baseKey = "base_$today"
            val base = prefs.getInt(baseKey, -1)

            when {
                base == -1 -> {
                    // Первое чтение сегодня.
                    // Берём вчерашний последний счётчик как базу «начало дня»
                    val yesterdayLast = prefs.getInt("last_steps_$yesterday", -1)
                    val baseline = if (yesterdayLast in 1..stepsFromBoot) yesterdayLast
                                   else stepsFromBoot
                    prefs.edit().putInt(baseKey, baseline).apply()
                    todaySteps = (stepsFromBoot - baseline).coerceAtLeast(0)
                }
                stepsFromBoot < base -> {
                    // Телефон перезагружали — счётчик сбросился
                    prefs.edit().putInt(baseKey, 0).apply()
                    todaySteps = stepsFromBoot
                }
                else -> {
                    todaySteps = stepsFromBoot - base
                }
            }
            // Сохраняем текущее значение, чтобы завтра использовать как базу
            prefs.edit().putInt("last_steps_$today", stepsFromBoot).apply()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(sensorListener)
    }

    override fun onCleared() {
        stopListening()
    }

    val distanceKm: Float get() = todaySteps * 0.00075f
    val caloriesBurned: Int get() = (todaySteps * 0.04f * (weightKg / 70f)).toInt()
    val progressFraction: Float get() = (todaySteps.toFloat() / stepGoal).coerceIn(0f, 1f)
}
