package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_day",
    indices = [Index("date")]
)
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val label: String? = null,
    val is_completed: Boolean = false,
    val calories_burned: Int = 0,
    val week_number: Int = 1,
    val created_at: Long = System.currentTimeMillis()
)
