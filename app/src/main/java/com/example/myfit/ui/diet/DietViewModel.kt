package com.example.myfit.ui.diet

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.DailyLog
import com.example.myfit.data.db.entity.FoodEntry
import com.example.myfit.data.db.entity.UserProfile
import com.example.myfit.data.db.model.DailyNutrition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DietViewModel(application: Application) : AndroidViewModel(application) {

    private val app = MyFitApp.from(application)
    private val dao = app.database.foodEntryDao()
    private val dailyLogDao = app.database.dailyLogDao()

    private val _dateFlow = MutableStateFlow(LocalDate.now().toString())

    fun refreshDate() {
        _dateFlow.value = LocalDate.now().toString()
    }

    val profile: StateFlow<UserProfile?> = app.database.userProfileDao().getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val todayNutrition: StateFlow<DailyNutrition> = _dateFlow
        .flatMapLatest { date -> dao.getDailyTotalsFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyNutrition())

    val todayEntries: StateFlow<List<FoodEntry>> = _dateFlow
        .flatMapLatest { date -> dao.getByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    // ── Быстрое добавление воды ───────────────────────────
    var showWaterDialog by mutableStateOf(false)
        private set
    var waterInput by mutableStateOf("")

    fun addWater(ml: Int) {
        val timeStr = LocalTime.now().toString().take(5)
        viewModelScope.launch {
            dao.insert(FoodEntry(
                date = LocalDate.now().toString(), time = timeStr, meal_type = "напитки",
                name = "Вода", calories = 0f, protein = 0f, fat = 0f, carbs = 0f,
                water_ml = ml.toFloat(), source = "manual", confidence = 1f
            ))
        }
    }

    fun openWaterDialog() { waterInput = ""; showWaterDialog = true }
    fun dismissWaterDialog() { showWaterDialog = false }
    fun confirmWaterDialog() {
        val ml = waterInput.toIntOrNull() ?: return
        if (ml > 0) addWater(ml)
        showWaterDialog = false
    }

    // ── Ручное добавление еды ─────────────────────────────
    var showAddFoodDialog by mutableStateOf(false)
        private set
    var addFoodName by mutableStateOf("")
    var addFoodKcal by mutableStateOf("")
    var addFoodProtein by mutableStateOf("")
    var addFoodFat by mutableStateOf("")
    var addFoodCarbs by mutableStateOf("")
    var addFoodMeal by mutableStateOf("завтрак")
    var addFoodError by mutableStateOf<String?>(null)

    val mealTypes = listOf("завтрак", "обед", "ужин", "перекус")

    fun openAddFoodDialog() {
        addFoodName = ""; addFoodKcal = ""; addFoodProtein = ""
        addFoodFat = ""; addFoodCarbs = ""; addFoodError = null
        showAddFoodDialog = true
    }

    fun dismissAddFoodDialog() { showAddFoodDialog = false }

    fun saveManualFood() {
        val name = addFoodName.trim()
        val kcal = addFoodKcal.toFloatOrNull()
        if (name.isBlank()) { addFoodError = "Введите название"; return }
        if (kcal == null) { addFoodError = "Введите калории"; return }
        val protein = addFoodProtein.toFloatOrNull() ?: 0f
        val fat = addFoodFat.toFloatOrNull() ?: 0f
        val carbs = addFoodCarbs.toFloatOrNull() ?: 0f
        val timeStr = LocalTime.now().toString().take(5)
        viewModelScope.launch {
            dao.insert(FoodEntry(
                date = LocalDate.now().toString(), time = timeStr, meal_type = addFoodMeal,
                name = name, calories = kcal, protein = protein, fat = fat, carbs = carbs,
                source = "manual", confidence = 1f
            ))
        }
        showAddFoodDialog = false
    }

    // ── Удаление записи ───────────────────────────────────
    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch { dao.delete(entry) }
    }
}
