package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val steps: Int = 0,
    val calories_burned_steps: Int = 0,
    val calories_burned_workout: Int = 0,
    val water_ml: Int = 0,
    val weight_kg: Float? = null,
    val is_training_day: Boolean = false,
    val updated_at: Long = System.currentTimeMillis()
)
