package com.example.myfit.data.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.myfit.data.db.entity.Exercise
import com.example.myfit.data.db.entity.WorkoutEntry

data class WorkoutEntryWithExercise(
    @Embedded val workoutEntry: WorkoutEntry,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id"
    )
    val exercise: Exercise
)
