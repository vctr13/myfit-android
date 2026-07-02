package com.example.myfit.ui.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.db.entity.Exercise
import com.example.myfit.data.db.entity.WorkoutDay
import com.example.myfit.data.db.entity.WorkoutTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    onOpenDrawer: () -> Unit,
    vm: FitnessViewModel = viewModel()
) {
    LaunchedEffect(Unit) { vm.refreshDate() }

    val exercises      by vm.exercises.collectAsState()
    val recentWorkouts by vm.recentWorkouts.collectAsState()
    val todayWorkouts  by vm.todayWorkouts.collectAsState()
    val showReminder   by vm.showReminder.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Силовые тренировки") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Меню")
                    }
                },
                actions = {
                    if (!vm.isWorkoutActive) {
                        IconButton(onClick = { vm.openExerciseCatalog() }) {
                            Icon(Icons.Outlined.FitnessCenter, contentDescription = "Каталог упражнений")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showReminder) {
                MonthlyReminderBanner(onDismiss = { vm.dismissReminder() })
            }

            ModeToggleRow(
                mode     = vm.trainingMode,
                onChange = { vm.setMode(it) },
                enabled  = !vm.isWorkoutActive
            )

            HorizontalDivider()

            if (vm.isWorkoutActive) {
                ActiveWorkoutContent(vm = vm)
            } else {
                IdleContent(vm = vm, todayWorkouts = todayWorkouts, recentWorkouts = recentWorkouts)
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (vm.showTemplateSheet)    TemplatePickerDialog(vm = vm)
    if (vm.showExercisePicker)   ExercisePickerDialog(vm = vm, exercises = exercises)
    if (vm.showAddSetDialog)     AddSetDialog(vm = vm)
    if (vm.showWeightPickerDialog) WeightPickerDialog(vm = vm)
    if (vm.showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { vm.dismissCancelConfirm() },
            title  = { Text("Отменить тренировку?") },
            text   = { Text("Все подходы будут удалены и тренировка не сохранится.") },
            confirmButton = {
                Button(
                    onClick = { vm.cancelWorkout() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Отменить") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissCancelConfirm() }) { Text("Продолжить") }
            }
        )
    }
    if (vm.showInfoDialog)       InfoDialog(vm = vm)
    if (vm.showExerciseCatalog)  ExerciseCatalogDialog(vm = vm, exercises = exercises)
    if (vm.showExerciseEditor)   ExerciseEditorDialog(vm = vm)
    if (vm.showTemplateEditor)   TemplateEditorDialog(vm = vm, exercises = exercises)
}

// ── Mode toggle ───────────────────────────────────────────────────────────────

@Composable
private fun ModeToggleRow(mode: TrainingMode, onChange: (TrainingMode) -> Unit, enabled: Boolean) {
    val modes = TrainingMode.entries
    TabRow(selectedTabIndex = modes.indexOf(mode)) {
        modes.forEach { entry ->
            Tab(
                selected = mode == entry,
                onClick  = { if (enabled) onChange(entry) },
                text     = { Text(entry.label) }
            )
        }
    }
}

// ── Monthly reminder banner ───────────────────────────────────────────────────

@Composable
private fun MonthlyReminderBanner(onDismiss: () -> Unit) {
    Surface(
        color    = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text     = "💪 Новый месяц — самое время повысить нагрузку!",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Закрыть",
                    modifier = Modifier.size(18.dp),
                    tint     = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// ── Idle content ──────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    vm: FitnessViewModel,
    todayWorkouts: List<WorkoutDay>,
    recentWorkouts: List<WorkoutDay>
) {
    val today     = LocalDate.now().toString()
    val todayDone = todayWorkouts.filter { it.is_completed }
    val history   = recentWorkouts.filter { it.date != today }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (todayDone.isNotEmpty()) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Сегодня выполнено:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        todayDone.forEach { wd ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    wd.label ?: "Тренировка",
                                    style    = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { vm.deleteWorkoutDay(wd) }) {
                                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.FitnessCenter, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Text("Сегодня тренировок нет", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Button(
                onClick  = { vm.openTemplateSheet() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Начать тренировку")
            }
        }

        if (history.isNotEmpty()) {
            item {
                Text(
                    "Последние тренировки",
                    style    = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(history, key = { it.id }) { wd ->
                HistoryCard(wd = wd, onDelete = { vm.deleteWorkoutDay(wd) })
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun HistoryCard(wd: WorkoutDay, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(wd.label ?: "Тренировка", style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatWorkoutDate(wd.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}

// ── Active workout content ────────────────────────────────────────────────────

@Composable
private fun ActiveWorkoutContent(vm: FitnessViewModel) {
    Column(Modifier.fillMaxSize()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (vm.trainingMode == TrainingMode.HOME) "Домашняя тренировка" else "Тренировка в зале",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val done = vm.activeExercises.count { it.sets.isNotEmpty() }
                    Text(
                        "$done / ${vm.activeExercises.size} упражнений",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(onClick = { vm.finishWorkout() }) { Text("Завершить") }
                    TextButton(onClick = { vm.requestCancelWorkout() }) {
                        Text("Отмена", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        LazyColumn(
            modifier            = Modifier.weight(1f),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(vm.activeExercises, key = { _, ae -> ae.exercise.id }) { index, ae ->
                ExerciseLogCard(ae = ae, index = index, vm = vm)
            }

            item {
                OutlinedButton(
                    onClick  = { vm.openExercisePicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Добавить упражнение")
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Exercise log card ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseLogCard(ae: ActiveExercise, index: Int, vm: FitnessViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: name + muscles | [i] [×]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ae.exercise.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        ae.exercise.muscle_groups,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick  = { vm.openInfoDialog(ae.exercise) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Описание",
                        modifier = Modifier.size(20.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick  = { vm.removeActiveExercise(index) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Close, null,
                        modifier = Modifier.size(20.dp),
                        tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            // Chip row: [Вес] [Режим] [п1] [п2] ... [+] ([×] если есть подходы)
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Weight chip — clickable on inner Row (not Surface) to avoid minimumInteractiveComponentSize inflation
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = MaterialTheme.colorScheme.surface,
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { vm.openWeightPickerDialog(index) }
                            .padding(start = 8.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.FitnessCenter, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (ae.weightKg > 0f) "${"%.1f".format(ae.weightKg)} кг" else "0 кг",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Mode chip
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = MaterialTheme.colorScheme.surface,
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clickable { vm.toggleMode(index) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ae.isTimeBased) {
                            Icon(Icons.Filled.Timer, "Секунды", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text("П", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Set chips — one per logged set (compact custom)
                ae.sets.forEach { set ->
                    Surface(
                        shape  = RoundedCornerShape(10.dp),
                        color  = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (ae.isTimeBased) "${set.value}с" else "${set.value}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Add set button
                FilledTonalIconButton(
                    onClick  = { vm.openAddSetDialog(index) },
                    modifier = Modifier.size(32.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor   = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить подход", modifier = Modifier.size(18.dp))
                }

                // Remove last set button (only when sets exist)
                if (ae.sets.isNotEmpty()) {
                    IconButton(
                        onClick  = { vm.removeLastSet(index) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close, null,
                            modifier = Modifier.size(16.dp),
                            tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// ── Template picker dialog ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatePickerDialog(vm: FitnessViewModel) {
    val allTemplates by vm.templates.collectAsState()
    val modeKey      = vm.trainingMode.key
    val modeTemplates = allTemplates.filter { it.mode == modeKey || it.mode == "both" }

    AlertDialog(
        onDismissRequest = { vm.dismissTemplateSheet() },
        title   = {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Выбери тренировку")
                FilledTonalIconButton(
                    onClick  = { vm.openCreateTemplate() },
                    modifier = Modifier.size(36.dp),
                    colors   = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor   = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(Icons.Filled.Add, "Создать комплекс", Modifier.size(18.dp))
                }
            }
        },
        text    = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(modeTemplates, key = { it.id }) { template ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable { vm.startFromTemplate(template) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.titleSmall)
                                if (template.description.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val names = template.exerciseNameList()
                                if (names.isNotEmpty()) {
                                    Text(
                                        names.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (!template.is_builtin) {
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.MoreVert, null, Modifier.size(18.dp))
                                    }
                                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                        DropdownMenuItem(
                                            text        = { Text("Редактировать") },
                                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                            onClick     = { vm.openEditTemplate(template); showMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text        = { Text("Удалить") },
                                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                            onClick     = { vm.deleteTemplate(template); showMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick  = { vm.startCustomWorkout() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Своя тренировка") }
                }
            }
        },
        confirmButton  = {},
        dismissButton  = {
            TextButton(onClick = { vm.dismissTemplateSheet() }) { Text("Отмена") }
        }
    )
}

// ── Exercise picker dialog ────────────────────────────────────────────────────

@Composable
private fun ExercisePickerDialog(vm: FitnessViewModel, exercises: List<Exercise>) {
    var query by remember { mutableStateOf("") }
    val modeKey = vm.trainingMode.key

    val grouped = remember(exercises, query, modeKey) {
        exercises
            .filter { it.training_mode == modeKey || it.training_mode == "both" }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.muscle_groups.contains(query, ignoreCase = true) }
            .groupBy { normalizeMuscle(it.muscle_groups) }
            .toSortedMap()
    }

    AlertDialog(
        onDismissRequest = { vm.dismissExercisePicker() },
        title   = { Text("Выбери упражнение") },
        text    = {
            Column {
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("Поиск…") },
                    leadingIcon   = { Icon(Icons.Filled.Search, null) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    grouped.forEach { (muscle, exList) ->
                        item(key = "header_$muscle") {
                            Text(
                                muscle,
                                style    = MaterialTheme.typography.labelLarge,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(exList, key = { it.id }) { ex ->
                            val alreadyAdded = vm.activeExercises.any { it.exercise.id == ex.id }
                            ListItem(
                                headlineContent   = { Text(ex.name, style = MaterialTheme.typography.bodyMedium) },
                                supportingContent = {
                                    Text(ex.muscle_groups, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                trailingContent = if (alreadyAdded) {
                                    { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                modifier = Modifier.clickable { vm.pickExercise(ex) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton  = {},
        dismissButton  = {
            TextButton(onClick = { vm.dismissExercisePicker() }) { Text("Готово") }
        }
    )
}

// ── Add-set dialog (только счётчик повторений или секунд) ──────────────────────

@Composable
private fun AddSetDialog(vm: FitnessViewModel) {
    val ae      = vm.activeExercises.getOrNull(vm.addSetExerciseIndex)
    val exName  = ae?.exercise?.name ?: ""
    val setNum  = (ae?.sets?.size ?: 0) + 1
    val timeBased = ae?.isTimeBased == true

    AlertDialog(
        onDismissRequest = { vm.dismissAddSetDialog() },
        title   = { Text("Подход $setNum · $exName") },
        text    = {
            CounterRow(
                label       = if (timeBased) "Секунды" else "Повторения",
                value       = if (timeBased) "${vm.addSetValue} с" else "${vm.addSetValue}",
                onDecrement = { vm.decrementSetValue() },
                onIncrement = { vm.incrementSetValue() }
            )
        },
        confirmButton  = {
            Button(onClick = { vm.confirmAddSet() }) { Text("Записать") }
        },
        dismissButton  = {
            TextButton(onClick = { vm.dismissAddSetDialog() }) { Text("Отмена") }
        }
    )
}

// ── Weight picker dialog ──────────────────────────────────────────────────────

@Composable
private fun WeightPickerDialog(vm: FitnessViewModel) {
    val ae     = vm.activeExercises.getOrNull(vm.weightPickerIndex)
    val exName = ae?.exercise?.name ?: ""

    AlertDialog(
        onDismissRequest = { vm.dismissWeightPickerDialog() },
        title   = { Text("Вес · $exName") },
        text    = {
            CounterRow(
                label       = "Кг (шаг 2.5)",
                value       = if (vm.weightPickerValue > 0f) "${"%.1f".format(vm.weightPickerValue)}" else "0",
                onDecrement = { vm.decrementWeightPicker() },
                onIncrement = { vm.incrementWeightPicker() }
            )
        },
        confirmButton  = {
            Button(onClick = { vm.confirmWeightPicker() }) { Text("Применить") }
        },
        dismissButton  = {
            TextButton(onClick = { vm.dismissWeightPickerDialog() }) { Text("Отмена") }
        }
    )
}

// ── Counter row ───────────────────────────────────────────────────────────────

@Composable
private fun CounterRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalIconButton(onClick = onDecrement, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Уменьшить")
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.width(100.dp))
            FilledTonalIconButton(onClick = onIncrement, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Увеличить")
            }
        }
    }
}

// ── Info dialog ───────────────────────────────────────────────────────────────

@Composable
private fun InfoDialog(vm: FitnessViewModel) {
    val ex = vm.infoExercise ?: return

    AlertDialog(
        onDismissRequest = { vm.dismissInfoDialog() },
        title   = { Text(ex.name) },
        text    = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier            = Modifier.verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.FitnessCenter, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("Иллюстрация", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }

                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(
                        ex.muscle_groups,
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (ex.description.isNotBlank()) {
                    Text(ex.description, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        "Описание отсутствует. Откройте каталог (иконка в шапке), чтобы добавить.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton  = {},
        dismissButton  = {
            TextButton(onClick = { vm.dismissInfoDialog() }) { Text("Закрыть") }
        }
    )
}

// ── Exercise catalog dialog ───────────────────────────────────────────────────

@Composable
private fun ExerciseCatalogDialog(vm: FitnessViewModel, exercises: List<Exercise>) {
    var query by remember { mutableStateOf("") }

    val grouped = remember(exercises, query) {
        exercises
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.muscle_groups.contains(query, ignoreCase = true) }
            .groupBy { normalizeMuscle(it.muscle_groups) }
            .toSortedMap()
    }

    AlertDialog(
        onDismissRequest = { vm.dismissExerciseCatalog() },
        title   = {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Каталог упражнений")
                FilledTonalIconButton(
                    onClick = { vm.openAddExercise() },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor   = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить упражнение", modifier = Modifier.size(20.dp))
                }
            }
        },
        text    = {
            Column {
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("Поиск…") },
                    leadingIcon   = { Icon(Icons.Filled.Search, null) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    grouped.forEach { (muscle, exList) ->
                        item(key = "hdr_$muscle") {
                            Text(
                                muscle,
                                style    = MaterialTheme.typography.labelLarge,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(exList, key = { it.id }) { ex ->
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(ex.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(ex.muscle_groups, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick  = { vm.openInfoDialog(ex) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Outlined.Info, "Описание", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.MoreVert, null, Modifier.size(18.dp))
                                    }
                                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                        DropdownMenuItem(
                                            text        = { Text("Редактировать") },
                                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                            onClick     = { vm.openEditExercise(ex); showMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text        = { Text("Копировать") },
                                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                            onClick     = { vm.openCopyExercise(ex); showMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text        = { Text("Удалить") },
                                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                            onClick     = { vm.deleteExercise(ex); showMenu = false }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        },
        confirmButton  = {},
        dismissButton  = {
            TextButton(onClick = { vm.dismissExerciseCatalog() }) { Text("Закрыть") }
        }
    )
}

// ── Exercise editor dialog ────────────────────────────────────────────────────

@Composable
private fun ExerciseEditorDialog(vm: FitnessViewModel) {
    val title = if (vm.editorIsEdit) "Редактировать" else "Новое упражнение"

    AlertDialog(
        onDismissRequest = { vm.dismissExerciseEditor() },
        title   = { Text(title) },
        text    = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value         = vm.editorName,
                    onValueChange = { vm.editorName = it },
                    label         = { Text("Название*") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = vm.editorMuscles,
                    onValueChange = { vm.editorMuscles = it },
                    label         = { Text("Группы мышц*") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = vm.editorDescription,
                    onValueChange = { vm.editorDescription = it },
                    label         = { Text("Описание / техника") },
                    minLines      = 3,
                    maxLines      = 6,
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("Режим тренировки:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    "both" to "Домашние и зальные",
                    "home" to "Только домашние",
                    "gym"  to "Только зальные"
                ).forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth().clickable { vm.editorMode = key }
                    ) {
                        RadioButton(selected = vm.editorMode == key, onClick = { vm.editorMode = key })
                        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                vm.editorError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton  = {
            Button(onClick = { vm.saveExercise() }) { Text("Сохранить") }
        },
        dismissButton  = {
            TextButton(onClick = { vm.dismissExerciseEditor() }) { Text("Отмена") }
        }
    )
}

// ── Template editor dialog ────────────────────────────────────────────────────

@Composable
private fun TemplateEditorDialog(vm: FitnessViewModel, exercises: List<Exercise>) {
    val isEdit = vm.templateEditorId != null
    var exQuery by remember { mutableStateOf("") }

    val modeKey = when (vm.templateEditorMode) {
        "home" -> "home"
        "gym"  -> "gym"
        else   -> null
    }
    val selectedNames = vm.templateEditorExercises.toSet()
    val filteredEx = remember(exercises, exQuery, modeKey, selectedNames) {
        exercises
            .filter { modeKey == null || it.training_mode == modeKey || it.training_mode == "both" }
            .filter { exQuery.isBlank() || it.name.contains(exQuery, ignoreCase = true) }
            .filter { it.name !in selectedNames }
    }

    AlertDialog(
        onDismissRequest = { vm.dismissTemplateEditor() },
        title   = { Text(if (isEdit) "Редактировать комплекс" else "Новый комплекс") },
        text    = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value         = vm.templateEditorName,
                    onValueChange = { vm.templateEditorName = it },
                    label         = { Text("Название*") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = vm.templateEditorDescription,
                    onValueChange = { vm.templateEditorDescription = it },
                    label         = { Text("Описание") },
                    minLines      = 2,
                    maxLines      = 4,
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("Режим:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    "both" to "Дом и зал",
                    "home" to "Только дом",
                    "gym"  to "Только зал"
                ).forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth().clickable { vm.templateEditorMode = key }
                    ) {
                        RadioButton(selected = vm.templateEditorMode == key, onClick = { vm.templateEditorMode = key })
                        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                HorizontalDivider()

                // Selected exercises
                if (vm.templateEditorExercises.isNotEmpty()) {
                    Text("Упражнения (${vm.templateEditorExercises.size}):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    vm.templateEditorExercises.forEachIndexed { idx, name ->
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${idx + 1}. $name",
                                style    = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick  = { vm.removeExerciseFromTemplate(name) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Search to add exercise
                Text("Добавить упражнение:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value         = exQuery,
                    onValueChange = { exQuery = it },
                    placeholder   = { Text("Поиск…") },
                    leadingIcon   = { Icon(Icons.Filled.Search, null) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    filteredEx.take(8).forEach { ex ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable { vm.addExerciseToTemplate(ex.name); exQuery = "" }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(ex.name, style = MaterialTheme.typography.bodySmall)
                                Text(ex.muscle_groups, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }

                vm.templateEditorError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton  = {
            Button(onClick = { vm.saveTemplate() }) { Text("Сохранить") }
        },
        dismissButton  = {
            TextButton(onClick = { vm.dismissTemplateEditor() }) { Text("Отмена") }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun normalizeMuscle(raw: String): String = when {
    raw.contains("спина") || raw.contains("широчайш") -> "Спина"
    raw.contains("грудь")                              -> "Грудь"
    raw.contains("ноги") || raw.contains("ягодиц")    -> "Ноги"
    raw.contains("плеч")                               -> "Плечи"
    raw.contains("бицепс")                             -> "Бицепс"
    raw.contains("трицепс")                            -> "Трицепс"
    raw.contains("пресс") || raw.contains("кор") ||
        raw.contains("TVA") || raw.contains("поясн")  -> "Пресс / Кор"
    else                                               -> "Другое"
}

private fun formatWorkoutDate(iso: String): String = try {
    val d   = LocalDate.parse(iso)
    val fmt = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))
    d.format(fmt)
} catch (e: Exception) { iso }
