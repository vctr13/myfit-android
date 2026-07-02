package com.example.myfit.ui.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.ChatMessage
import com.example.myfit.data.db.entity.FoodEntry
import com.example.myfit.data.model.ParsedFoodData
import com.example.myfit.data.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = MyFitApp.from(application)

    private val repository = ChatRepository(
        chatMessageDao = app.database.chatMessageDao(),
        userProfileDao = app.database.userProfileDao(),
        productDao     = app.database.productDao(),
        foodEntryDao   = app.database.foodEntryDao(),
        apiKeyProvider = { app.securePrefs.apiKey },
        modelProvider  = { app.securePrefs.apiModel }
    )

    val messages: StateFlow<List<ChatMessage>> = repository.messages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var inputText by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var pendingFoodData by mutableStateOf<ParsedFoodData?>(null)
        private set

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || isLoading) return

        inputText = ""
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val result = repository.send(text)
                pendingFoodData = result.foodData
            } catch (e: Exception) {
                errorMessage = e.message ?: "Неизвестная ошибка"
            } finally {
                isLoading = false
            }
        }
    }

    fun confirmFood(mealType: String) {
        val data = pendingFoodData ?: return
        pendingFoodData = null
        val today = LocalDate.now().toString()
        val entryDate = data.date?.takeIf { it.isNotBlank() && it < today } ?: today
        val timeStr = LocalTime.now().toString().take(5)

        viewModelScope.launch {
            val dao = app.database.foodEntryDao()
            data.items.forEach { item ->
                dao.insert(
                    FoodEntry(
                        date = entryDate,
                        time = timeStr,
                        meal_type = mealType,
                        name = item.name,
                        calories = item.kcal,
                        protein = item.protein,
                        fat = item.fat,
                        carbs = item.carbs,
                        fiber = item.fiber.takeIf { it > 0 },
                        water_ml = 0f,
                        source = "ai_chat",
                        confidence = 0.85f
                    )
                )
            }
            if (data.waterMl > 0) {
                dao.insert(
                    FoodEntry(
                        date = entryDate,
                        time = timeStr,
                        meal_type = mealType,
                        name = "Вода/напитки",
                        calories = 0f,
                        protein = 0f,
                        fat = 0f,
                        carbs = 0f,
                        water_ml = data.waterMl,
                        source = "ai_chat",
                        confidence = 0.95f
                    )
                )
            }
        }
    }

    fun dismissFood() {
        pendingFoodData = null
    }

    fun dismissError() {
        errorMessage = null
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }
}
