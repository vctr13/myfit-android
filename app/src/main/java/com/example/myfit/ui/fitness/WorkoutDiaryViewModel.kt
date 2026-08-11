package com.example.myfit.ui.fitness

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.WorkoutDay
import com.example.myfit.data.db.model.WorkoutEntryWithExercise
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutDiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MyFitApp.from(application).database

    val allWorkouts: StateFlow<List<WorkoutDay>> = db.workoutDayDao().getAll()
        .map { list -> list.filter { it.is_completed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allEntries: StateFlow<List<WorkoutEntryWithExercise>> = db.workoutEntryDao().getAllWithExercise()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun entriesForDay(dayId: Int, allEntries: List<WorkoutEntryWithExercise>): List<WorkoutEntryWithExercise> =
        allEntries.filter { it.workoutEntry.workout_day_id == dayId }

    fun deleteDay(day: WorkoutDay) {
        viewModelScope.launch { db.workoutDayDao().delete(day) }
    }
}
