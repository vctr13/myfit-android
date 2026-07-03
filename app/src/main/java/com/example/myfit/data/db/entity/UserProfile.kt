package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.Period

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val age: Int,                                 // legacy: хранится при сохранении для совместимости
    val gender: String,
    val height_cm: Float,
    val weight_kg: Float,
    val current_weight_kg: Float = weight_kg,
    val goal: String,
    val activity_level: Float,
    val api_key_set: Boolean = false,
    val created_at: Long = System.currentTimeMillis(),
    val birth_date: String? = null                // "YYYY-MM-DD", null у старых пользователей
) {
    val computedAge: Int get() = birth_date?.let { bd ->
        runCatching { Period.between(LocalDate.parse(bd), LocalDate.now()).years }.getOrNull()
    } ?: age

    val bmr: Float get() = 10f * current_weight_kg + 6.25f * height_cm - 5f * computedAge +
            if (gender == "male") 5f else -161f

    val tdee: Float get() = bmr * activity_level

    val target_kcal: Float get() = tdee * 0.82f

    val target_protein_g: Float get() = current_weight_kg * 1.7f

    val target_water_ml: Int get() = (current_weight_kg * 30).toInt()

    val target_fat_g: Float get() = target_kcal * 0.25f / 9f

    val target_carbs_g: Float get() = (target_kcal - target_protein_g * 4f - target_fat_g * 9f) / 4f

    val target_kcal_training: Float get() = tdee * 0.95f

    val target_water_ml_training: Int get() = (current_weight_kg * 40).toInt()

    val target_fat_g_training: Float get() = target_kcal_training * 0.25f / 9f

    val target_carbs_g_training: Float get() =
        (target_kcal_training - target_protein_g * 4f - target_fat_g_training * 9f) / 4f
}