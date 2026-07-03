package com.example.myfit.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import kotlin.math.roundToInt

private val FREE_MODELS = setOf(
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.0-flash",
    "gemini-2.0-flash-lite",
    "gemini-1.5-flash",
    "gemini-1.5-flash-8b"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    onResetApp: () -> Unit = {},
    vm: SettingsViewModel = viewModel()
) {
    val profile by vm.profile.collectAsState()
    var modelExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Профиль ───────────────────────────────────────────
            profile?.let { p ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Профиль", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { vm.openEditProfile() }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val genderLabel = if (p.gender == "male") "Мужской" else "Женский"
                        val goalLabel = when (p.goal) {
                            "loss" -> "Похудение"
                            "gain" -> "Набор массы"
                            else  -> "Поддержание"
                        }
                        p.birth_date?.let { bd ->
                            runCatching {
                                val d = LocalDate.parse(bd)
                                val age = Period.between(d, LocalDate.now()).years
                                val fmt = "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
                                ProfileRow("Дата рождения", fmt)
                                ProfileRow("Возраст", "$age лет")
                            }.getOrNull()
                        } ?: ProfileRow("Возраст", "${p.age} лет")
                        ProfileRow("Пол", genderLabel)
                        ProfileRow("Рост", "${p.height_cm.toInt()} см")
                        ProfileRow("Начальный вес", "${"%.1f".format(p.weight_kg)} кг")
                        if (p.current_weight_kg != p.weight_kg) {
                            ProfileRow("Текущий вес", "${"%.1f".format(p.current_weight_kg)} кг")
                        }
                        ProfileRow("Цель", goalLabel)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        ProfileRow("Цель ккал", "${p.target_kcal.roundToInt()} ккал/день")
                        ProfileRow("Белки", "${"%.1f".format(p.target_protein_g)} г/день")
                        ProfileRow("Жиры", "${"%.1f".format(p.target_fat_g)} г/день")
                        ProfileRow("Углеводы", "${"%.1f".format(p.target_carbs_g)} г/день")
                        ProfileRow("Вода", "${p.target_water_ml} мл/день")
                    }
                }

                HorizontalDivider()
            }

            // ── Gemini модель ─────────────────────────────────────
            Text("Gemini модель", style = MaterialTheme.typography.titleMedium)

            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = !modelExpanded }
            ) {
                OutlinedTextField(
                    value = vm.selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Активная модель") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    vm.savedModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                val isFree = model in FREE_MODELS
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(model, modifier = Modifier.weight(1f))
                                    if (isFree) Text(
                                        "бесплатная",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = { vm.selectModel(model); modelExpanded = false },
                            trailingIcon = {
                                if (vm.savedModels.size > 1) {
                                    TextButton(
                                        onClick = { vm.removeModel(model) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("✕", color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Ручной ввод модели
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = vm.manualModelInput,
                    onValueChange = { vm.manualModelInput = it },
                    label = { Text("Добавить модель вручную") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { vm.addManualModel() },
                    enabled = vm.manualModelInput.isNotBlank()
                ) { Text("Добавить") }
            }

            // Загрузка списка из API
            OutlinedButton(
                onClick = { vm.fetchModelsFromApi() },
                enabled = !vm.isLoadingModels,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (vm.isLoadingModels) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Загрузить список из API")
            }

            vm.modelsError?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Text(
                "Смена модели вступает в силу для новых сообщений.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // ── Питание ───────────────────────────────────────────
            Text("Питание", style = MaterialTheme.typography.titleMedium)

            Card(
                onClick = onNavigateToMyProducts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Мои продукты", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Сохранённые единицы с точными КЖБУ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // ── Очистка данных ─────────────────────────────────────
            Text("Очистка данных", style = MaterialTheme.typography.titleMedium)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { vm.showClearBeforeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Удалить данные до даты…")
                    }
                    OutlinedButton(
                        onClick = { vm.showClearAllDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сбросить все данные и профиль",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (vm.showEditProfile) {
        EditProfileDialog(vm = vm)
    }
    if (vm.showClearBeforeDialog) {
        ClearBeforeDialog(vm = vm)
    }
    if (vm.showClearAllDialog) {
        ClearAllDialog(vm = vm, onConfirm = onResetApp)
    }
    if (vm.showModelsDialog) {
        ModelsPickerDialog(vm = vm)
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearBeforeDialog(vm: SettingsViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!vm.isClearingData) vm.showClearBeforeDialog = false },
        title = { Text("Удалить данные до даты") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Данные питания, тренировок, веса и чата старше выбранной даты будут удалены.")
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (vm.clearBeforeDate.isNotBlank()) {
                            runCatching {
                                val d = LocalDate.parse(vm.clearBeforeDate)
                                "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
                            }.getOrDefault(vm.clearBeforeDate)
                        } else "Выбрать дату"
                    )
                }
                if (vm.isClearingData) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Очистка…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.clearDataBefore {} },
                enabled = vm.clearBeforeDate.isNotBlank() && !vm.isClearingData
            ) { Text("Удалить") }
        },
        dismissButton = {
            TextButton(
                onClick = { vm.showClearBeforeDialog = false },
                enabled = !vm.isClearingData
            ) { Text("Отмена") }
        }
    )

    if (showDatePicker) {
        val initialMillis = remember {
            LocalDate.now().minusDays(30)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
                        vm.clearBeforeDate = date.toString()
                    }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ClearAllDialog(vm: SettingsViewModel, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!vm.isClearingData) vm.showClearAllDialog = false },
        title = { Text("Сбросить все данные?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Это необратимо удалит профиль, питание, тренировки, вес и историю чата. " +
                    "Приложение вернётся к экрану настройки профиля."
                )
                if (vm.isClearingData) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Удаляем данные…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.clearAllData(onConfirm) },
                enabled = !vm.isClearingData,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Удалить всё") }
        },
        dismissButton = {
            TextButton(
                onClick = { vm.showClearAllDialog = false },
                enabled = !vm.isClearingData
            ) { Text("Отмена") }
        }
    )
}

@Composable
private fun ModelsPickerDialog(vm: SettingsViewModel) {
    AlertDialog(
        onDismissRequest = { vm.dismissModelsDialog() },
        title = { Text("Модели из API") },
        text = {
            if (vm.apiModelsList.isEmpty()) {
                Text("Нет доступных моделей")
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    vm.apiModelsList.forEach { model ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { vm.toggleModelCheck(model) }
                        ) {
                            Checkbox(
                                checked = model in vm.checkedModels,
                                onCheckedChange = { vm.toggleModelCheck(model) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model, style = MaterialTheme.typography.bodyMedium)
                                if (model in FREE_MODELS) {
                                    Text(
                                        "бесплатная",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.confirmAddModels() },
                enabled = vm.checkedModels.isNotEmpty()
            ) { Text("Добавить выбранные") }
        },
        dismissButton = {
            TextButton(onClick = { vm.dismissModelsDialog() }) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(vm: SettingsViewModel) {
    val genderOptions = listOf("male" to "Мужской", "female" to "Женский")
    val goalOptions = listOf(
        "loss"     to "Похудение",
        "maintain" to "Поддержание",
        "gain"     to "Набор массы"
    )
    val activityOptions = listOf(
        1.2f   to "Сидячий (нет нагрузки)",
        1.375f to "Лёгкая (1–3 дня/нед.)",
        1.55f  to "Умеренная (3–5 дней/нед.)",
        1.725f to "Высокая (6–7 дней/нед.)"
    )

    var genderExpanded   by remember { mutableStateOf(false) }
    var goalExpanded     by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { vm.dismissEditProfile() },
        title = { Text("Редактировать профиль") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BirthDatePicker(
                    birthDate = vm.editBirthDate,
                    onDateSelected = { vm.editBirthDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
                if (vm.editBirthDate.isNotEmpty()) {
                    val age = runCatching {
                        Period.between(LocalDate.parse(vm.editBirthDate), LocalDate.now()).years
                    }.getOrNull()
                    if (age != null) {
                        Text(
                            "Возраст: $age лет",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = genderOptions.find { it.first == vm.editGender }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Пол") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        genderOptions.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) },
                                onClick = { vm.editGender = key; genderExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = vm.editHeight,
                    onValueChange = { vm.editHeight = it },
                    label = { Text("Рост (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vm.editWeight,
                    onValueChange = { vm.editWeight = it },
                    label = { Text("Вес (кг)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = goalExpanded,
                    onExpandedChange = { goalExpanded = !goalExpanded }
                ) {
                    OutlinedTextField(
                        value = goalOptions.find { it.first == vm.editGoal }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Цель") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = goalExpanded, onDismissRequest = { goalExpanded = false }) {
                        goalOptions.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) },
                                onClick = { vm.editGoal = key; goalExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = activityExpanded,
                    onExpandedChange = { activityExpanded = !activityExpanded }
                ) {
                    OutlinedTextField(
                        value = activityOptions.find { it.first == vm.editActivity }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Уровень активности") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                        activityOptions.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) },
                                onClick = { vm.editActivity = value; activityExpanded = false })
                        }
                    }
                }

                vm.editError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { vm.saveProfile() }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = { vm.dismissEditProfile() }) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePicker(
    birthDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    val displayText = remember(birthDate) {
        runCatching {
            val d = LocalDate.parse(birthDate)
            "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
        }.getOrDefault("")
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Дата рождения") },
            placeholder = { Text("выберите дату") },
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier
            .matchParentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showPicker = true }
        )
    }

    if (showPicker) {
        val initialMillis = remember(birthDate) {
            runCatching {
                LocalDate.parse(birthDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrElse {
                LocalDate.now().minusYears(30).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = IntRange(1924, LocalDate.now().year - 10)
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(date.toString())
                    }
                    showPicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
