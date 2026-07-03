package com.example.myfit.ui.onboarding

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.UserProfile
import com.example.myfit.data.network.GeminiService
import com.example.myfit.data.prefs.SecurePrefs
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MyFitApp.from(application).database
    private val securePrefs = MyFitApp.from(application).securePrefs

    var birthDate by mutableStateOf("")
    var weight by mutableStateOf("")
    var height by mutableStateOf("")
    var gender by mutableStateOf("male")
    var goal by mutableStateOf("loss")
    var activityLevel by mutableStateOf(1.2f)
    var apiKey by mutableStateOf("")
    var selectedModel by mutableStateOf(SecurePrefs.DEFAULT_MODEL)

    var isCheckingKey by mutableStateOf(false)
        private set
    var keyStatus by mutableStateOf<KeyStatus>(KeyStatus.Idle)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var saveComplete by mutableStateOf(false)
        private set
    var formError by mutableStateOf<String?>(null)
        private set

    fun resetKeyStatus() { keyStatus = KeyStatus.Idle }

    fun verifyApiKey() {
        val key = apiKey.trim()
        if (key.isBlank()) { keyStatus = KeyStatus.Error("Введите API-ключ"); return }
        isCheckingKey = true
        keyStatus = KeyStatus.Idle
        viewModelScope.launch {
            keyStatus = try {
                GeminiService(key, selectedModel).chat("You are a helpful assistant.", emptyList(), "Hi")
                KeyStatus.Valid
            } catch (e: Exception) {
                KeyStatus.Error(e.message?.take(200) ?: "Неизвестная ошибка")
            }
            isCheckingKey = false
        }
    }

    fun save() {
        val weightFloat = weight.trim().replace(',', '.').toFloatOrNull()
        val heightFloat = height.trim().replace(',', '.').toFloatOrNull()
        val birthLocalDate = runCatching { LocalDate.parse(birthDate) }.getOrNull()
        val ageInt = birthLocalDate?.let { Period.between(it, LocalDate.now()).years }

        when {
            birthLocalDate == null        -> { formError = "Укажите дату рождения"; return }
            ageInt !in 10..120            -> { formError = "Возраст должен быть от 10 до 120 лет"; return }
            weightFloat == null || weightFloat !in 30f..300f -> { formError = "Введите корректный вес (30–300 кг)"; return }
            heightFloat == null || heightFloat !in 100f..250f -> { formError = "Введите корректный рост (100–250 см)"; return }
            keyStatus !is KeyStatus.Valid -> { formError = "Сначала проверьте API-ключ"; return }
        }
        formError = null
        isSaving = true
        viewModelScope.launch {
            db.userProfileDao().upsert(UserProfile(
                age = ageInt!!,
                birth_date = birthDate,
                gender = gender,
                height_cm = heightFloat!!,
                weight_kg = weightFloat!!,
                goal = goal,
                activity_level = activityLevel,
                api_key_set = true
            ))
            securePrefs.apiKey = apiKey.trim()
            securePrefs.apiModel = selectedModel
            isSaving = false
            saveComplete = true
        }
    }

    sealed interface KeyStatus {
        data object Idle : KeyStatus
        data object Valid : KeyStatus
        data class Error(val message: String) : KeyStatus
    }
}