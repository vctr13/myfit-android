package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise",
    indices = [Index(value = ["name"], unique = true)]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val muscle_groups: String,
    val description: String = "",
    val image_url: String? = null,
    val training_mode: String = "both",
    val is_custom: Boolean = false
)
