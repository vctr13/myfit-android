package com.example.myfit.ui.chat

import android.app.Application
import android.media.MediaRecorder
import android.util.Base64
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
import com.example.myfit.data.model.ParsedFoodItem
import com.example.myfit.data.network.GeminiService
import com.example.myfit.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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

    // Сейчас есть только "android" (системный RecognizerIntent). Экран читает это значение,
    // чтобы в будущем при добавлении распознавания через Gemini переключение происходило
    // только через Настройки, без изменений в UI чата.
    val speechRecognitionMethod: String get() = app.securePrefs.speechRecognitionMethod

    fun onSpeechRecognized(text: String) {
        inputText = if (inputText.isBlank()) text else "$inputText $text"
    }

    // ── Распознавание речи через Gemini (альтернатива системному Android) ──────
    // Отдельная "лёгкая" модель независимо от той, что выбрана для самого чата —
    // у неё отдельная (намного большая) дневная квота запросов, транскрипция
    // не задевает лимит основной модели.
    private val transcriptionModel = "gemini-2.5-flash-lite"

    var isRecordingVoice by mutableStateOf(false)
        private set
    var isTranscribing by mutableStateOf(false)
        private set

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var autoStopJob: Job? = null

    fun startRecordingVoiceGemini() {
        if (isRecordingVoice) return
        if (app.securePrefs.apiKey.isBlank()) {
            errorMessage = "Сначала укажите API-ключ Gemini в настройках"
            return
        }

        val file = File(app.cacheDir, "voice_input_${System.currentTimeMillis()}.aac")
        @Suppress("DEPRECATION") // MediaRecorder(Context) требует API 31+, minSdk проекта — 26
        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
        }
        try {
            recorder.prepare()
            recorder.start()
        } catch (e: Exception) {
            recorder.release()
            errorMessage = "Не удалось начать запись: ${e.message}"
            return
        }

        mediaRecorder = recorder
        recordingFile = file
        isRecordingVoice = true

        autoStopJob = viewModelScope.launch {
            delay(60_000)
            stopRecordingVoiceGemini()
        }
    }

    fun stopRecordingVoiceGemini() {
        if (!isRecordingVoice) return
        isRecordingVoice = false
        autoStopJob?.cancel()
        autoStopJob = null

        val recorder = mediaRecorder
        mediaRecorder = null
        val audioFile = recordingFile
        recordingFile = null

        try {
            recorder?.stop()
        } catch (_: Exception) {
            // Запись короче долей секунды (например, случайный тап) — stop() может упасть,
            // файл в этом случае всё равно будет удалён ниже.
        } finally {
            recorder?.release()
        }

        if (audioFile == null) return
        isTranscribing = true
        viewModelScope.launch {
            try {
                val base64 = Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
                val service = GeminiService(app.securePrefs.apiKey, transcriptionModel)
                val text = service.transcribe(base64, "audio/aac")
                if (text.isNotBlank()) onSpeechRecognized(text)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Не удалось распознать речь"
            } finally {
                isTranscribing = false
                audioFile.delete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoStopJob?.cancel()
        mediaRecorder?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        recordingFile?.delete()
    }

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

    fun confirmFood(mealType: String, editedItems: List<ParsedFoodItem>) {
        val data = pendingFoodData ?: return
        pendingFoodData = null
        val today = LocalDate.now().toString()
        val entryDate = data.date?.takeIf { it.isNotBlank() && it < today } ?: today
        val timeStr = LocalTime.now().toString().take(5)

        viewModelScope.launch {
            val dao = app.database.foodEntryDao()
            editedItems.forEach { item ->
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

