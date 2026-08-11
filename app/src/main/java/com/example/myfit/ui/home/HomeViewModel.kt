package com.example.myfit.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.DailyLog
import com.example.myfit.data.db.entity.UserProfile
import com.example.myfit.data.db.entity.WeightEntry
import com.example.myfit.data.db.model.DailyNutrition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

enum class WeightPeriod(val label: String, val days: Int?) {
    M1("1м", 30), M3("3м", 90), M6("6м", 180), Y1("1г", 365), ALL("все", null)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = MyFitApp.from(application)
    private val dailyLogDao = app.database.dailyLogDao()
    private val _dateFlow = MutableStateFlow(LocalDate.now().toString())

    fun refreshDate() { _dateFlow.value = LocalDate.now().toString() }

    val profile: StateFlow<UserProfile?> = app.database.userProfileDao().getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val todayNutrition: StateFlow<DailyNutrition> = _dateFlow
        .flatMapLatest { date -> app.database.foodEntryDao().getDailyTotalsFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyNutrition())

    // ── Калории с тренировок ──────────────────────────────────────────────────
    val todayWorkoutCalories: StateFlow<Int> = _dateFlow
        .flatMapLatest { date -> app.database.workoutDayDao().getTotalCaloriesFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Шаги и их калории — из DailyLog ──────────────────────────────
    private val _todayLogForSteps: StateFlow<DailyLog?> = _dateFlow
        .flatMapLatest { date -> dailyLogDao.getByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stepCalories: StateFlow<Int> = _todayLogForSteps
        .map { it?.calories_burned_steps ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val todaySteps: StateFlow<Int> = _todayLogForSteps
        .map { it?.steps ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Тренировочный день ────────────────────────────────────────────────────
    val isTrainingDay: StateFlow<Boolean> = _dateFlow
        .flatMapLatest { date -> dailyLogDao.getByDate(date) }
        .map { it?.is_training_day ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleTrainingDay() {
        val date = LocalDate.now().toString()
        viewModelScope.launch {
            val log = dailyLogDao.getByDateOnce(date)
            if (log == null) {
                dailyLogDao.upsert(DailyLog(date = date, is_training_day = true))
            } else {
                dailyLogDao.updateTrainingDay(date, !log.is_training_day)
            }
        }
    }

    // ── Weight / Waist period filter ──────────────────────────────────────────
    private val _weightPeriod = MutableStateFlow(WeightPeriod.M1)
    val weightPeriod: StateFlow<WeightPeriod> = _weightPeriod.asStateFlow()

    fun setWeightPeriod(p: WeightPeriod) { _weightPeriod.value = p }

    private val _allWeightEntries = app.database.weightDao().getAllFlow()

    val weightHistory: StateFlow<List<WeightEntry>> =
        combine(_allWeightEntries, _weightPeriod) { entries, period ->
            val cutoff = period.days?.let { LocalDate.now().minusDays(it.toLong()).toString() }
            entries.filter { it.weight_kg > 0f && (cutoff == null || it.date >= cutoff) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val waistHistory: StateFlow<List<WeightEntry>> =
        combine(_allWeightEntries, _weightPeriod) { entries, period ->
            val cutoff = period.days?.let { LocalDate.now().minusDays(it.toLong()).toString() }
            entries.filter { it.waist_cm != null && (cutoff == null || it.date >= cutoff) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val greeting: String
        get() = when (LocalTime.now().hour) {
            in 5..11  -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..22 -> "Добрый вечер"
            else      -> "Доброй ночи"
        }

    // ── Добавление показателей ────────────────────────────────────────────────
    var showWeightDialog by mutableStateOf(false)
        private set
    var weightInput by mutableStateOf("")
    var waistInput  by mutableStateOf("")
    var weightError by mutableStateOf<String?>(null)
        private set

    fun openWeightDialog()    { weightInput = ""; waistInput = ""; weightError = null; showWeightDialog = true }
    fun dismissWeightDialog() { showWeightDialog = false }

    fun saveMetrics() {
        val kg = weightInput.trim().replace(',', '.').toFloatOrNull()
        val cm = waistInput.trim().replace(',', '.').toFloatOrNull()
        if (kg == null && cm == null) { weightError = "Введите хотя бы одно значение"; return }
        if (kg != null && (kg < 1f || kg > 300f)) { weightError = "Некорректный вес (1–300 кг)"; return }
        if (cm != null && (cm < 40f || cm > 200f)) { weightError = "Некорректный обхват (40–200 см)"; return }
        weightError = null
        viewModelScope.launch {
            app.database.weightDao().insert(
                WeightEntry(date = LocalDate.now().toString(), weight_kg = kg ?: 0f, waist_cm = cm)
            )
            if (kg != null) {
                app.database.userProfileDao().getProfileOnce()?.let { profile ->
                    app.database.userProfileDao().upsert(profile.copy(current_weight_kg = kg))
                }
            }
        }
        showWeightDialog = false
    }
}
