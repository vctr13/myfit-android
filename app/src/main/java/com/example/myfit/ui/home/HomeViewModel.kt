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

    // ── Weight period filter ──────────────────────────────────────────────────
    private val _weightPeriod = MutableStateFlow(WeightPeriod.M1)
    val weightPeriod: StateFlow<WeightPeriod> = _weightPeriod.asStateFlow()

    fun setWeightPeriod(p: WeightPeriod) { _weightPeriod.value = p }

    val weightHistory: StateFlow<List<WeightEntry>> =
        combine(app.database.weightDao().getAllFlow(), _weightPeriod) { entries, period ->
            if (period.days == null) entries
            else {
                val cutoff = LocalDate.now().minusDays(period.days.toLong()).toString()
                entries.filter { it.date >= cutoff }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val greeting: String
        get() = when (LocalTime.now().hour) {
            in 5..11  -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..22 -> "Добрый вечер"
            else      -> "Доброй ночи"
        }

    // ── Добавление веса ───────────────────────────────────────────────────────
    var showWeightDialog by mutableStateOf(false)
        private set
    var weightInput by mutableStateOf("")
    var weightError by mutableStateOf<String?>(null)
        private set

    fun openWeightDialog()    { weightInput = ""; weightError = null; showWeightDialog = true }
    fun dismissWeightDialog() { showWeightDialog = false }

    fun saveWeight() {
        val kg = weightInput.replace(',', '.').toFloatOrNull()
        if (kg == null || kg < 20f || kg > 300f) {
            weightError = "Введите корректный вес (20–300 кг)"; return
        }
        viewModelScope.launch {
            app.database.weightDao().insert(
                WeightEntry(date = LocalDate.now().toString(), weight_kg = kg)
            )
            // Обновляем текущий вес в профиле → пересчитываются все цели (КЖБУ, вода)
            app.database.userProfileDao().getProfileOnce()?.let { profile ->
                app.database.userProfileDao().upsert(profile.copy(current_weight_kg = kg))
            }
        }
        showWeightDialog = false
    }
}
