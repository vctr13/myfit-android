package com.example.myfit.ui.pedometer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import kotlinx.coroutines.launch

class PedometerViewModel(application: Application) : AndroidViewModel(application) {

    private val stepTracker = MyFitApp.from(application).stepTracker
    private val profileDao = MyFitApp.from(application).database.userProfileDao()

    val isSensorAvailable = stepTracker.isSensorAvailable
    val stepGoal = 10_000

    var todaySteps by mutableStateOf(stepTracker.todaySteps.value)
        private set

    var weightKg by mutableStateOf(70f)
        private set

    init {
        viewModelScope.launch {
            profileDao.getProfileOnce()?.let { weightKg = it.weight_kg }
        }
        viewModelScope.launch {
            stepTracker.todaySteps.collect { todaySteps = it }
        }
    }

    val distanceKm: Float get() = todaySteps * 0.00075f
    val caloriesBurned: Int get() = (todaySteps * 0.04f * (weightKg / 70f)).toInt()
    val progressFraction: Float get() = (todaySteps.toFloat() / stepGoal).coerceIn(0f, 1f)
}
