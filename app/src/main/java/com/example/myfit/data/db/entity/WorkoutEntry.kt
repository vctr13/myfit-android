package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_entry",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDay::class,
            parentColumns = ["id"],
            childColumns = ["workout_day_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("workout_day_id"),
        Index("exercise_id")
    ]
)
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workout_day_id: Int,
    val exercise_id: Int,
    val difficulty_level: Int,
    val sets: Int,
    val reps: Int? = null,
    val duration_sec: Int? = null,
    val weight_kg: Float? = null,
    val sort_order: Int = 0
)
