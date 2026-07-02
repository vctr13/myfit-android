package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_entry",
    indices = [Index("date", unique = true)]
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val weight_kg: Float,
    val created_at: Long = System.currentTimeMillis()
)
