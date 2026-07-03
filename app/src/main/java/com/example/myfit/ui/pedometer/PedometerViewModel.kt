package com.example.myfit.ui.pedometer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.DailyLog
import kotlinx.coroutines.launch
import java.time.LocalDate

class PedometerViewModel(application: Application) : AndroidViewModel(application) {

    private val db         = MyFitApp.from(application).database
    private val logDao     = db.dailyLogDao()
    private val profileDao = db.userProfileDao()

    val stepGoal = 10_000

    var todaySteps   by mutableStateOf(0);     private set
    var stepsInput   by mutableStateOf("")
    var weightKg     by mutableStateOf(70f);   private set
    var isSaved      by mutableStateOf(false); private set

    private var inputPrefilled = false

    init {
        viewModelScope.launch {
            profileDao.getProfileOnce()?.let { weightKg = it.weight_kg }
        }
        viewModelScope.launch {
            logDao.getByDate(LocalDate.now().toString()).collect { log ->
                todaySteps = log?.steps ?: 0
                if (!inputPrefilled && (log?.steps ?: 0) > 0) {
                    stepsInput = (log?.steps ?: 0).toString()
                    inputPrefilled = true
                }
            }
        }
    }

    val distanceKm: Float       get() = todaySteps * 0.00075f
    val caloriesBurned: Int     get() = (todaySteps * 0.04f * (weightKg / 70f)).toInt()
    val progressFraction: Float get() = (todaySteps.toFloat() / stepGoal).coerceIn(0f, 1f)

    fun save() {
        val steps = stepsInput.trim().toIntOrNull()?.coerceAtLeast(0) ?: return
        val calories = (steps * 0.04f * (weightKg / 70f)).toInt()
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existing = logDao.getByDateOnce(today)
            if (existing == null) {
                logDao.upsert(DailyLog(date = today, steps = steps, calories_burned_steps = calories))
            } else {
                logDao.updateSteps(today, steps, calories)
            }
            isSaved = true
        }
    }

    fun onStepsInputChange(value: String) {
        stepsInput = value.filter { it.isDigit() }
        isSaved = false
    }
}