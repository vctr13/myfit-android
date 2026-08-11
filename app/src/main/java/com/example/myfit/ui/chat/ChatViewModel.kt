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
import com.example.myfit.data.db.entity.UserFacts
import com.example.myfit.data.model.ParsedFoodData
import com.example.myfit.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = MyFitApp.from(application)
    private val userFactsDao = app.database.userFactsDao()

    private val repository = ChatRepository(
        chatMessageDao = app.database.chatMessageDao(),
        userProfileDao = app.database.userProfileDao(),
        productDao     = app.database.productDao(),
        foodEntryDao   = app.database.foodEntryDao(),
        dailyLogDao    = app.database.dailyLogDao(),
        userFactsDao   = app.database.userFactsDao(),
        apiKeyProvider = { app.securePrefs.apiKey },
        modelProvider  = { app.securePrefs.apiModel }
    )

    // Отображаем только сообщения текущего дня — вся история остаётся в БД для контекста ИИ
    private val _dateFlow = MutableStateFlow(LocalDate.now().toString())
    fun refreshDate() { _dateFlow.value = LocalDate.now().toString() }

    val messages: StateFlow<List<ChatMessage>> = _dateFlow
        .flatMapLatest { dateStr ->
            val startOfDay = LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            repository.messagesFrom(startOfDay)
        }
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
                        water_ml = item.waterMl,
                        grams_g = item.amountG,
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

    // ── Память чат-бота (факты о пользователе) ──────────────────
    val facts: StateFlow<List<UserFacts>> = userFactsDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var showFactsDialog by mutableStateOf(false)
        private set
    fun openFactsDialog() { showFactsDialog = true }
    fun dismissFactsDialog() { showFactsDialog = false }
    fun deleteFact(key: String) {
        viewModelScope.launch { userFactsDao.deleteByKey(key) }
    }

    var showSaveFactDialog by mutableStateOf(false)
        private set
    var factKeyInput by mutableStateOf("")
    var factValueInput by mutableStateOf("")
    var factError by mutableStateOf<String?>(null)
        private set

    fun openSaveFact(message: ChatMessage) {
        factKeyInput = ""
        factValueInput = message.content
        factError = null
        showSaveFactDialog = true
    }

    fun dismissSaveFact() { showSaveFactDialog = false }

    fun confirmSaveFact() {
        val key = factKeyInput.trim()
        val value = factValueInput.trim()
        if (key.isBlank())   { factError = "Введите короткое название (например: «Аллергия»)"; return }
        if (value.isBlank()) { factError = "Текст заметки не может быть пустым"; return }
        factError = null
        viewModelScope.launch {
            userFactsDao.upsert(UserFacts(key = key, value = value, source = "chat_manual"))
            showSaveFactDialog = false
        }
    }
}

