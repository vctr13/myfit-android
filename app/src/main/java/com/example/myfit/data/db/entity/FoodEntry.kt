package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_entry",
    indices = [Index("date")]
)
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val time: String,
    val meal_type: String,
    val name: String,
    val calories: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val fiber: Float? = null,
    val source: String,
    val confidence: Float,
    val food_quality: String? = null,
    val note: String? = null,
    val water_ml: Float = 0f,
    val created_at: Long = System.currentTimeMillis()
)
