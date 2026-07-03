package com.example.myfit.ui.settings

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app     = MyFitApp.from(application)
    private val prefs   = app.securePrefs
    private val db      = app.database

    val profile = db.userProfileDao().getProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Модели Gemini ─────────────────────────────────────────
    var selectedModel by mutableStateOf(prefs.apiModel)
        private set
    var savedModels by mutableStateOf(prefs.savedModels)
        private set

    // Список моделей из API (для выбора чекбоксами)
    var apiModelsList by mutableStateOf<List<String>>(emptyList())
        private set
    var isLoadingModels by mutableStateOf(false)
        private set
    var modelsError by mutableStateOf<String?>(null)
        private set
    var checkedModels by mutableStateOf<Set<String>>(emptySet())
        private set
    var showModelsDialog by mutableStateOf(false)
        private set
    var manualModelInput by mutableStateOf("")

    fun selectModel(model: String) {
        prefs.apiModel = model
        selectedModel = model
    }

    fun fetchModelsFromApi() {
        isLoadingModels = true
        modelsError = null
        viewModelScope.launch {
            try {
                val models = GeminiService(prefs.apiKey, selectedModel)
                    .fetchModels()
                    .map { it.shortName }
                apiModelsList = models
                checkedModels = emptySet()
                showModelsDialog = true
            } catch (e: Exception) {
                modelsError = "Ошибка загрузки: ${e.message?.take(120)}"
            } finally {
                isLoadingModels = false
            }
        }
    }

    fun toggleModelCheck(model: String) {
        checkedModels = if (model in checkedModels) checkedModels - model else checkedModels + model
    }

    fun confirmAddModels() {
        val current = savedModels.toMutableList()
        checkedModels.forEach { if (it !in current) current.add(it) }
        prefs.savedModels = current
        savedModels = current
        showModelsDialog = false
    }

    fun dismissModelsDialog() { showModelsDialog = false }

    fun addManualModel() {
        val model = manualModelInput.trim()
        if (model.isBlank() || model in savedModels) { manualModelInput = ""; return }
        val updated = savedModels + model
        prefs.savedModels = updated
        savedModels = updated
        manualModelInput = ""
    }

    fun removeModel(model: String) {
        val updated = savedModels.filter { it != model }
        prefs.savedModels = updated
        savedModels = updated
        if (selectedModel == model) {
            val fallback = updated.firstOrNull() ?: SecurePrefs.DEFAULT_MODEL
            selectModel(fallback)
        }
    }

    // ── Редактирование профиля ────────────────────────────────
    var showEditProfile by mutableStateOf(false)
        private set
    var editBirthDate by mutableStateOf("")
    var editGender by mutableStateOf("male")
    var editHeight by mutableStateOf("")
    var editWeight by mutableStateOf("")
    var editGoal by mutableStateOf("maintain")
    var editActivity by mutableStateOf(1.55f)
    var editError by mutableStateOf<String?>(null)

    fun openEditProfile() {
        val p = profile.value ?: return
        editBirthDate = p.birth_date ?: ""
        editGender = p.gender
        editHeight = p.height_cm.toInt().toString()
        editWeight = "%.1f".format(p.weight_kg)
        editGoal = p.goal
        editActivity = p.activity_level
        editError = null
        showEditProfile = true
    }

    fun dismissEditProfile() { showEditProfile = false }

    fun saveProfile() {
        val height = editHeight.replace(",", ".").toFloatOrNull()
        val weight = editWeight.replace(",", ".").toFloatOrNull()
        val birthLocalDate = runCatching { LocalDate.parse(editBirthDate) }.getOrNull()
        val ageInt = birthLocalDate?.let { Period.between(it, LocalDate.now()).years }

        when {
            birthLocalDate == null               -> { editError = "Укажите дату рождения"; return }
            ageInt == null || ageInt !in 10..120 -> { editError = "Возраст должен быть 10–120 лет"; return }
            height == null || height !in 100f..250f -> { editError = "Рост: 100–250 см"; return }
            weight == null || weight !in 20f..300f  -> { editError = "Вес: 20–300 кг"; return }
        }

        viewModelScope.launch {
            val current = profile.value
            db.userProfileDao().upsert(UserProfile(
                id = 1,
                age = ageInt!!,
                birth_date = editBirthDate,
                gender = editGender,
                height_cm = height!!,
                weight_kg = weight!!,
                current_weight_kg = current?.current_weight_kg ?: weight!!,
                goal = editGoal,
                activity_level = editActivity,
                api_key_set = current?.api_key_set ?: false,
                created_at = current?.created_at ?: System.currentTimeMillis()
            ))
            showEditProfile = false
        }
    }

    // ── Очистка базы данных ───────────────────────────────────
    var showClearBeforeDialog by mutableStateOf(false)
    var showClearAllDialog    by mutableStateOf(false)
    var clearBeforeDate       by mutableStateOf("")   // "YYYY-MM-DD"
    var isClearingData        by mutableStateOf(false)
        private set

    fun clearDataBefore(onDone: () -> Unit) {
        val cutoff = clearBeforeDate.ifBlank { return }
        isClearingData = true
        viewModelScope.launch {
            try {
                val cutoffMs = LocalDate.parse(cutoff)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                db.foodEntryDao().deleteOlderThan(cutoff)
                db.dailyLogDao().deleteOlderThan(cutoff)
                db.weightDao().deleteOlderThan(cutoff)
                db.workoutDayDao().deleteOlderThan(cutoff)
                db.workoutEntryDao().deleteOrphans()
                db.chatMessageDao().deleteOlderThan(cutoffMs)
            } finally {
                isClearingData = false
                showClearBeforeDialog = false
                onDone()
            }
        }
    }

    fun clearAllData(onDone: () -> Unit) {
        isClearingData = true
        viewModelScope.launch {
            try {
                db.foodEntryDao().deleteAll()
                db.dailyLogDao().deleteAll()
                db.weightDao().deleteAll()
                db.workoutDayDao().deleteAll()
                db.workoutEntryDao().deleteAll()
                db.chatMessageDao().deleteAll()
                db.userProfileDao().deleteAll()
                prefs.clearAll()
            } finally {
                isClearingData = false
                showClearAllDialog = false
                onDone()
            }
        }
    }
}