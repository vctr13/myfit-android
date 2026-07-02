package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_template")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val mode: String = "both",           // "home", "gym", "both"
    val exercise_names: String = "",     // comma-separated exercise names
    val is_builtin: Boolean = false
) {
    fun exerciseNameList(): List<String> =
        exercise_names.split(",").map { it.trim() }.filter { it.isNotBlank() }
}
