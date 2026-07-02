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
    @SerializedName("amount_g") val amountG: Float
)

data class ChatResult(
    val text: String,
    val foodData: ParsedFoodData?
)
