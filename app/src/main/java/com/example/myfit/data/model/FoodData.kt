package com.example.myfit.data.model

import com.google.gson.annotations.SerializedName

data class ParsedFoodData(
    val items: List<ParsedFoodItem> = emptyList(),
    @SerializedName("water_ml") val waterMl: Float = 0f,
    val date: String? = null   // "YYYY-MM-DD" если не сегодня, null = сегодня
)

data class ParsedFoodItem(
    val name: String,
    val kcal: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val fiber: Float = 0f,
    @SerializedName("amount_g") val amountG: Float,
    @SerializedName("water_ml") val waterMl: Float = 0f
)

data class ChatResult(
    val text: String,
    val foodData: ParsedFoodData?
)

// Пересчитывает КБЖУ/клетчатку/воду продукта пропорционально исправленному весу —
// используется, когда пользователь правит граммы AI в карточке подтверждения.
// Без исходного веса (amountG <= 0) пропорцию составить не из чего — оставляем КБЖУ как есть.
fun ParsedFoodItem.scaledTo(editedGrams: Float): ParsedFoodItem {
    if (amountG <= 0f || editedGrams <= 0f) return this
    val ratio = editedGrams / amountG
    return copy(
        amountG = editedGrams,
        kcal = kcal * ratio,
        protein = protein * ratio,
        fat = fat * ratio,
        carbs = carbs * ratio,
        fiber = fiber * ratio,
        waterMl = waterMl * ratio
    )
}
