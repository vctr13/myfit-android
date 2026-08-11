package com.example.myfit.ui.fitness

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.MyFitApp
import com.example.myfit.data.db.entity.DailyLog
import com.example.myfit.data.db.entity.Exercise
import com.example.myfit.data.db.entity.WorkoutDay
import com.example.myfit.data.db.entity.WorkoutEntry
import com.example.myfit.data.db.entity.WorkoutTemplate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt
import java.time.YearMonth

// ── Domain types ──────────────────────────────────────────────────────────────

enum class TrainingMode(val label: String, val key: String) {
    HOME("Дом", "home"), GYM("Зал", "gym")
}

data class SetLog(val value: Int)

data class WorkoutSummary(val durationMin: Int?, val calories: Int)

data class ActiveExercise(
    val exercise: Exercise,
    val weightKg: Float = 0f,
    val isTimeBased: Boolean = false,
    val sets: List<SetLog> = emptyList(),
    val prevResult: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class FitnessViewModel(application: Application) : AndroidViewModel(application) {

    private val db              = MyFitApp.from(application).database
    private val securePrefs     = MyFitApp.from(application).securePrefs
    private val exerciseDao     = db.exerciseDao()
    private val workoutDao      = db.workoutDayDao()
    private val entryDao        = db.workoutEntryDao()
    private val profileDao      = db.userProfileDao()
    private val templateDao     = db.workoutTemplateDao()
    private val logDao          = db.dailyLogDao()
    private val prefs           = application.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)

    // ── Mode ──────────────────────────────────────────────────────────────────
    var trainingMode by mutableStateOf(TrainingMode.HOME)
        private set

    fun setMode(mode: TrainingMode) {
        if (!isWorkoutActive) trainingMode = mode
    }

    // ── Date refresh ──────────────────────────────────────────────────────────
    private val _dateFlow = MutableStateFlow(LocalDate.now().toString())
    fun refreshDate() { _dateFlow.value = LocalDate.now().toString() }

    // ── Exercise catalog ──────────────────────────────────────────────────────
    val exercises: StateFlow<List<Exercise>> = exerciseDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Workout templates (from DB) ───────────────────────────────────────────
    val templates: StateFlow<List<WorkoutTemplate>> = templateDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Today's completed workouts ────────────────────────────────────────────
    val todayWorkouts: StateFlow<List<WorkoutDay>> = _dateFlow
        .flatMapLatest { date -> workoutDao.getByDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Monthly reminder ──────────────────────────────────────────────────────
    private val _showReminder = MutableStateFlow(false)
    val showReminder: StateFlow<Boolean> = _showReminder.asStateFlow()

    // ── Active workout state ──────────────────────────────────────────────────
    var isWorkoutActive by mutableStateOf(false)
        private set
    var workoutStarted by mutableStateOf(false)
        private set
    var editingWorkoutDayId by mutableStateOf<Int?>(null)
        private set
    val activeExercises = mutableStateListOf<ActiveExercise>()

    // ── Template / Exercise picker dialogs ────────────────────────────────────
    var showTemplateSheet by mutableStateOf(false)
        private set
    var showExercisePicker by mutableStateOf(false)
        private set

    // ── Weight picker dialog ─────────────────────────────────────────────────
    var showWeightPickerDialog by mutableStateOf(false)
        private set
    var weightPickerIndex by mutableStateOf(0)
        private set
    var weightPickerValue by mutableStateOf(0f)

    // ── Add-set dialog ───────────────────────────────────────────────────────
    var showAddSetDialog by mutableStateOf(false)
        private set
    var addSetExerciseIndex by mutableStateOf(0)
        private set
    var addSetValue by mutableStateOf(10)

    // ── Confirm-cancel dialog ─────────────────────────────────────────────────
    var showCancelConfirm by mutableStateOf(false)
        private set

    // ── Итог завершённой тренировки ───────────────────────────────────────────
    var finishedSummary by mutableStateOf<WorkoutSummary?>(null)
        private set

    // ── Rest timer ────────────────────────────────────────────────────────────
    var restTimerSec   by mutableStateOf(120)
    var restTimerLeft  by mutableStateOf(0)
    var restTimerRunning by mutableStateOf(false)
        private set
    private var timerJob: Job? = null
    private var workoutStartMillis: Long? = null

    // ── Exercise catalog dialog ───────────────────────────────────────────────
    var showExerciseCatalog by mutableStateOf(false)
        private set

    // ── Template catalog dialog ───────────────────────────────────────────────
    var showTemplateCatalog by mutableStateOf(false)
        private set

    // ── Template editor ───────────────────────────────────────────────────────
    var showTemplateEditor by mutableStateOf(false)
        private set
    var templateEditorId by mutableStateOf<Int?>(null)
        private set
    var templateEditorName by mutableStateOf("")
    var templateEditorDescription by mutableStateOf("")
    var templateEditorMode by mutableStateOf("home")
    val templateEditorExercises = mutableStateListOf<String>()
    var templateEditorError by mutableStateOf<String?>(null)
        private set

    // ── Exercise editor dialog ────────────────────────────────────────────────
    var showExerciseEditor by mutableStateOf(false)
        private set
    var editorIsEdit by mutableStateOf(false)
        private set
    var editorSourceId by mutableStateOf<Int?>(null)
        private set
    var editorName by mutableStateOf("")
    var editorMuscles by mutableStateOf("")
    var editorDescription by mutableStateOf("")
    var editorMode by mutableStateOf("both")
    var editorIsTimeBased by mutableStateOf(false)
    var editorError by mutableStateOf<String?>(null)
        private set

    // ── Info dialog ───────────────────────────────────────────────────────────
    var showInfoDialog by mutableStateOf(false)
        private set
    var infoExercise by mutableStateOf<Exercise?>(null)
        private set

    // ── Steps input ───────────────────────────────────────────────────────────
    var stepsInput by mutableStateOf("")
    var stepsSaved by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            profileDao.getProfileOnce()
            checkMonthlyReminder()
            val log = logDao.getByDateOnce(LocalDate.now().toString())
            if (log != null && log.steps > 0) stepsInput = log.steps.toString()
        }
    }

    // ── Monthly reminder ──────────────────────────────────────────────────────
    private fun checkMonthlyReminder() {
        val day = LocalDate.now().dayOfMonth
        if (day > 5) { _showReminder.value = false; return }
        val lastShown = prefs.getString("reminder_month", "") ?: ""
        _showReminder.value = (lastShown != YearMonth.now().toString())
    }

    fun dismissReminder() {
        prefs.edit().putString("reminder_month", YearMonth.now().toString()).apply()
        _showReminder.value = false
    }

    // ── Start workout ─────────────────────────────────────────────────────────
    fun openTemplateSheet() {
        checkMonthlyReminder()
        showTemplateSheet = true
    }
    fun dismissTemplateSheet() { showTemplateSheet = false }

    fun startFromTemplate(template: WorkoutTemplate) {
        showTemplateSheet = false
        viewModelScope.launch {
            val allEx = exerciseDao.getAllOnce()
            activeExercises.clear()
            template.exerciseNameList().forEach { name ->
                val ex = allEx.firstOrNull { it.name == name } ?: return@forEach
                if (activeExercises.none { it.exercise.id == ex.id }) {
                    val last = entryDao.getRecentEntriesForExercise(ex.id, 1).firstOrNull()
                    val prev = last?.let { formatPrevResult(it) }
                    activeExercises.add(ActiveExercise(ex, isTimeBased = ex.is_time_based, prevResult = prev))
                }
            }
            editingWorkoutDayId = null
            workoutStarted = false
            workoutStartMillis = null
            isWorkoutActive = true
        }
    }

    fun startCustomWorkout() {
        showTemplateSheet = false
        activeExercises.clear()
        editingWorkoutDayId = null
        workoutStarted = false
        workoutStartMillis = null
        isWorkoutActive = true
        showExercisePicker = true
    }

    fun startTiming() {
        workoutStartMillis = System.currentTimeMillis()
        workoutStarted = true
    }

    // ── Resume (edit) completed workout ───────────────────────────────────────
    fun resumeWorkout(day: WorkoutDay) {
        viewModelScope.launch {
            val entries = entryDao.getWithExerciseByWorkoutDayIdOnce(day.id)
            activeExercises.clear()
            entries.forEach { ew ->
                val sets = ew.workoutEntry.set_values
                    ?.split(",")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?.map { SetLog(it) }
                    ?: run {
                        val repsOrSec = if (ew.workoutEntry.duration_sec != null)
                            ew.workoutEntry.duration_sec
                        else
                            ew.workoutEntry.reps ?: 0
                        List(ew.workoutEntry.sets) { SetLog(repsOrSec) }
                    }
                val isTimeBased = ew.workoutEntry.duration_sec != null
                val last = entryDao.getRecentEntriesForExercise(ew.exercise.id, 2)
                    .firstOrNull { it.workout_day_id != day.id }
                activeExercises.add(
                    ActiveExercise(
                        exercise    = ew.exercise,
                        weightKg    = ew.workoutEntry.weight_kg ?: 0f,
                        isTimeBased = isTimeBased,
                        sets        = sets,
                        prevResult  = last?.let { formatPrevResult(it) }
                    )
                )
            }
            editingWorkoutDayId = day.id
            workoutStarted = true
            workoutStartMillis = null
            isWorkoutActive = true
        }
    }

    // ── Exercise picker ───────────────────────────────────────────────────────
    fun openExercisePicker() { showExercisePicker = true }
    fun dismissExercisePicker() { showExercisePicker = false }

    fun pickExercise(exercise: Exercise) {
        if (activeExercises.none { it.exercise.id == exercise.id }) {
            viewModelScope.launch {
                val last = entryDao.getRecentEntriesForExercise(exercise.id, 1).firstOrNull()
                val prev = last?.let { formatPrevResult(it) }
                activeExercises.add(ActiveExercise(exercise, isTimeBased = exercise.is_time_based, prevResult = prev))
            }
        }
    }

    // ── Weight picker ─────────────────────────────────────────────────────────
    fun openWeightPickerDialog(index: Int) {
        weightPickerIndex = index
        weightPickerValue = activeExercises.getOrNull(index)?.weightKg ?: 0f
        showWeightPickerDialog = true
    }
    fun dismissWeightPickerDialog() { showWeightPickerDialog = false }
    fun incrementWeightPicker() { weightPickerValue += 2.5f }
    fun decrementWeightPicker() { weightPickerValue = (weightPickerValue - 2.5f).coerceAtLeast(0f) }
    fun confirmWeightPicker() {
        val idx = weightPickerIndex
        val old = activeExercises.getOrNull(idx) ?: run { showWeightPickerDialog = false; return }
        activeExercises[idx] = old.copy(weightKg = weightPickerValue)
        showWeightPickerDialog = false
    }

    // ── Set logging ───────────────────────────────────────────────────────────
    fun openAddSetDialog(index: Int) {
        addSetExerciseIndex = index
        val ae = activeExercises.getOrNull(index)
        addSetValue = if (ae?.isTimeBased == true) 30 else 10
        showAddSetDialog = true
    }
    fun dismissAddSetDialog() { showAddSetDialog = false }

    fun incrementSetValue() { addSetValue++ }
    fun decrementSetValue() { if (addSetValue > 1) addSetValue-- }

    fun confirmAddSet() {
        val idx = addSetExerciseIndex
        val old = activeExercises.getOrNull(idx) ?: return
        activeExercises[idx] = old.copy(sets = old.sets + SetLog(addSetValue))
        showAddSetDialog = false
    }

    fun removeLastSet(index: Int) {
        val old = activeExercises.getOrNull(index) ?: return
        if (old.sets.isNotEmpty()) activeExercises[index] = old.copy(sets = old.sets.dropLast(1))
    }

    fun removeActiveExercise(index: Int) {
        if (index in activeExercises.indices) activeExercises.removeAt(index)
    }

    // ── Finish / Cancel ───────────────────────────────────────────────────────
    fun finishWorkout() {
        val logged = activeExercises.filter { it.sets.isNotEmpty() }
        if (logged.isEmpty()) { cancelWorkout(); return }

        viewModelScope.launch {
            val bodyWeight = profileDao.getProfileOnce()?.current_weight_kg ?: 70f

            val totalCalories = logged.sumOf { ae ->
                val externalWeight = ae.weightKg
                if (ae.isTimeBased) {
                    val totalSeconds = ae.sets.sumOf { it.value }
                    val effectiveWeight = bodyWeight + externalWeight * 0.3f
                    (totalSeconds * effectiveWeight * 0.0014).toDouble()
                } else {
                    val totalReps = ae.sets.sumOf { it.value }
                    val effectiveWeight = externalWeight + bodyWeight * 0.4f
                    (totalReps * effectiveWeight * 0.025).toDouble()
                }
            }.roundToInt().coerceAtLeast(0)

            val durationMin = workoutStartMillis?.let {
                ((System.currentTimeMillis() - it) / 60_000L).toInt().coerceAtLeast(1)
            }

            val editId = editingWorkoutDayId
            var finalDurationMin = durationMin
            if (editId != null) {
                // Обновляем существующую тренировку
                val existing = workoutDao.getById(editId)
                if (existing != null) {
                    finalDurationMin = existing.duration_minutes ?: durationMin
                    workoutDao.update(existing.copy(
                        calories_burned  = totalCalories,
                        duration_minutes = finalDurationMin
                    ))
                    entryDao.deleteByWorkoutDayId(editId)
                    logged.forEachIndexed { sort, ae ->
                        val avgValue = ae.sets.map { it.value }.average().toInt().takeIf { ae.sets.isNotEmpty() }
                        entryDao.insert(
                            WorkoutEntry(
                                workout_day_id   = editId,
                                exercise_id      = ae.exercise.id,
                                difficulty_level = 0,
                                sets             = ae.sets.size,
                                reps             = if (!ae.isTimeBased) avgValue else null,
                                duration_sec     = if (ae.isTimeBased) avgValue else null,
                                weight_kg        = ae.weightKg.takeIf { it > 0f },
                                sort_order       = sort,
                                set_values       = ae.sets.joinToString(",") { it.value.toString() }
                            )
                        )
                    }
                }
            } else {
                // Создаём новую тренировку
                val dayId = workoutDao.insert(
                    WorkoutDay(
                        date             = LocalDate.now().toString(),
                        label            = if (trainingMode == TrainingMode.HOME) "Домашняя тренировка" else "Тренировка в зале",
                        is_completed     = true,
                        calories_burned  = totalCalories,
                        duration_minutes = durationMin
                    )
                ).toInt()

                logged.forEachIndexed { sort, ae ->
                    val avgValue = ae.sets.map { it.value }.average().toInt().takeIf { ae.sets.isNotEmpty() }
                    entryDao.insert(
                        WorkoutEntry(
                            workout_day_id   = dayId,
                            exercise_id      = ae.exercise.id,
                            difficulty_level = 0,
                            sets             = ae.sets.size,
                            reps             = if (!ae.isTimeBased) avgValue else null,
                            duration_sec     = if (ae.isTimeBased) avgValue else null,
                            weight_kg        = ae.weightKg.takeIf { it > 0f },
                            sort_order       = sort,
                            set_values       = ae.sets.joinToString(",") { it.value.toString() }
                        )
                    )
                }
            }

            finishedSummary = WorkoutSummary(durationMin = finalDurationMin, calories = totalCalories)
            resetActiveState()
        }
    }

    fun dismissFinishedSummary() { finishedSummary = null }

    fun requestCancelWorkout() { showCancelConfirm = true }
    fun dismissCancelConfirm() { showCancelConfirm = false }

    fun cancelWorkout() {
        showCancelConfirm = false
        resetActiveState()
    }

    private fun resetActiveState() {
        activeExercises.clear()
        isWorkoutActive = false
        workoutStarted = false
        editingWorkoutDayId = null
        workoutStartMillis = null
        stopRestTimer()
    }

    // ── Rest timer ────────────────────────────────────────────────────────────
    fun toggleRestTimer() {
        if (restTimerRunning) {
            stopRestTimer()
        } else {
            restTimerLeft = restTimerSec
            restTimerRunning = true
            timerJob = viewModelScope.launch {
                while (restTimerLeft > 0) {
                    delay(1000L)
                    restTimerLeft--
                }
                restTimerRunning = false
                playTimerSound()
            }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        timerJob = null
        restTimerRunning = false
        restTimerLeft = 0
    }

    private fun playTimerSound() {
        try {
            when (securePrefs.timerSound) {
                "none" -> {}
                "notification" -> {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    RingtoneManager.getRingtone(getApplication(), uri)?.play()
                }
                else -> {
                    val uri = Uri.parse("android.resource://" + getApplication<Application>().packageName + "/raw/ring")
                    val mp = MediaPlayer.create(getApplication(), uri)
                    mp?.setOnCompletionListener { it.release() }
                    mp?.start()
                }
            }
        } catch (_: Exception) { }
    }

    // ── Workout history ───────────────────────────────────────────────────────
    fun deleteWorkoutDay(day: WorkoutDay) {
        viewModelScope.launch { workoutDao.delete(day) }
    }

    // ── Exercise catalog CRUD ─────────────────────────────────────────────────
    fun openExerciseCatalog() { showExerciseCatalog = true }
    fun dismissExerciseCatalog() { showExerciseCatalog = false }

    // ── Template catalog ──────────────────────────────────────────────────────
    fun openTemplateCatalog() { showTemplateCatalog = true }
    fun dismissTemplateCatalog() { showTemplateCatalog = false }

    fun openAddExercise() {
        editorIsEdit  = false
        editorSourceId = null
        editorName    = ""
        editorMuscles = ""
        editorDescription = ""
        editorMode    = "both"
        editorIsTimeBased = false
        editorError   = null
        showExerciseEditor = true
    }

    fun openEditExercise(e: Exercise) {
        editorIsEdit  = true
        editorSourceId = e.id
        editorName    = e.name
        editorMuscles = e.muscle_groups
        editorDescription = e.description
        editorMode    = e.training_mode
        editorIsTimeBased = e.is_time_based
        editorError   = null
        showExerciseEditor = true
    }

    fun openCopyExercise(e: Exercise) {
        editorIsEdit  = false
        editorSourceId = null
        editorName    = "${e.name} (копия)"
        editorMuscles = e.muscle_groups
        editorDescription = e.description
        editorMode    = e.training_mode
        editorIsTimeBased = e.is_time_based
        editorError   = null
        showExerciseEditor = true
    }

    fun dismissExerciseEditor() { showExerciseEditor = false }

    fun saveExercise() {
        val name    = editorName.trim()
        val muscles = editorMuscles.trim()
        if (name.isBlank())    { editorError = "Введите название упражнения"; return }
        if (muscles.isBlank()) { editorError = "Введите группы мышц";         return }
        editorError = null

        viewModelScope.launch {
            if (editorIsEdit) {
                val id       = editorSourceId ?: return@launch
                val existing = exerciseDao.getById(id) ?: return@launch
                exerciseDao.update(
                    existing.copy(
                        name          = name,
                        muscle_groups = muscles,
                        description   = editorDescription.trim(),
                        training_mode = editorMode,
                        is_time_based = editorIsTimeBased
                    )
                )
            } else {
                exerciseDao.insert(
                    Exercise(
                        name          = name,
                        muscle_groups = muscles,
                        description   = editorDescription.trim(),
                        training_mode = editorMode,
                        is_custom     = true,
                        is_time_based = editorIsTimeBased
                    )
                )
            }
            showExerciseEditor = false
        }
    }

    fun deleteExercise(e: Exercise) {
        viewModelScope.launch { exerciseDao.deleteById(e.id) }
    }

    // ── Info dialog ───────────────────────────────────────────────────────────
    fun openInfoDialog(e: Exercise) {
        infoExercise = e
        showInfoDialog = true
    }

    fun dismissInfoDialog() { showInfoDialog = false }

    // ── Template editor ───────────────────────────────────────────────────────
    fun openCreateTemplate() {
        templateEditorId = null
        templateEditorName = ""
        templateEditorDescription = ""
        templateEditorMode = trainingMode.key
        templateEditorExercises.clear()
        templateEditorError = null
        showTemplateEditor = true
    }

    fun openEditTemplate(template: WorkoutTemplate) {
        templateEditorId = template.id
        templateEditorName = template.name
        templateEditorDescription = template.description
        templateEditorMode = template.mode
        templateEditorExercises.clear()
        templateEditorExercises.addAll(template.exerciseNameList())
        templateEditorError = null
        showTemplateEditor = true
    }

    fun dismissTemplateEditor() { showTemplateEditor = false }

    fun addExerciseToTemplate(name: String) {
        if (name !in templateEditorExercises) templateEditorExercises.add(name)
    }

    fun removeExerciseFromTemplate(name: String) {
        templateEditorExercises.remove(name)
    }

    fun saveTemplate() {
        val name = templateEditorName.trim()
        if (name.isBlank()) { templateEditorError = "Введите название комплекса"; return }
        if (templateEditorExercises.isEmpty()) { templateEditorError = "Добавьте хотя бы одно упражнение"; return }
        templateEditorError = null
        viewModelScope.launch {
            val id = templateEditorId
            if (id != null) {
                val existing = templateDao.getById(id) ?: return@launch
                templateDao.update(existing.copy(
                    name = name,
                    description = templateEditorDescription.trim(),
                    mode = templateEditorMode,
                    exercise_names = templateEditorExercises.joinToString(",")
                ))
            } else {
                templateDao.insert(WorkoutTemplate(
                    name = name,
                    description = templateEditorDescription.trim(),
                    mode = templateEditorMode,
                    exercise_names = templateEditorExercises.joinToString(","),
                    is_builtin = false
                ))
            }
            showTemplateEditor = false
        }
    }

    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch { templateDao.delete(template) }
    }

    // ── Steps ─────────────────────────────────────────────────────────────────
    fun onStepsInputChange(value: String) {
        stepsInput = value.filter { it.isDigit() }
        stepsSaved = false
    }

    fun saveSteps() {
        val steps = stepsInput.trim().toIntOrNull() ?: 0
        val date = LocalDate.now().toString()
        viewModelScope.launch {
            val weightKg = profileDao.getProfileOnce()?.current_weight_kg ?: 70f
            val calories = (steps * 0.04f * (weightKg / 70f)).toInt()
            val existing = logDao.getByDateOnce(date)
            if (existing == null) {
                logDao.upsert(DailyLog(date = date, steps = steps, calories_burned_steps = calories))
            } else {
                logDao.updateSteps(date, steps, calories)
            }
            stepsSaved = true
        }
    }
}