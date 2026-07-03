package com.example.myfit.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import com.example.myfit.data.prefs.SecurePrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    vm: OnboardingViewModel = viewModel()
) {
    LaunchedEffect(vm.saveComplete) {
        if (vm.saveComplete) onFinished()
    }

    val scrollState = rememberScrollState()
    var showApiKey by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    val activityOptions = listOf(
        1.2f to "Сидячий (нет нагрузки)",
        1.375f to "Лёгкая (1–3 дня/нед.)",
        1.55f to "Умеренная (3–5 дней/нед.)",
        1.725f to "Высокая (6–7 дней/нед.)"
    )
    val selectedActivityLabel = activityOptions.find { it.first == vm.activityLevel }?.second ?: ""

    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройка профиля MyFIT") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Личные данные ──────────────────────────────────
            Text("Личные данные", style = MaterialTheme.typography.titleMedium)

            BirthDatePicker(
                birthDate = vm.birthDate,
                onDateSelected = { vm.birthDate = it },
                modifier = Modifier.fillMaxWidth()
            )
            if (vm.birthDate.isNotEmpty()) {
                val age = runCatching {
                    Period.between(LocalDate.parse(vm.birthDate), LocalDate.now()).years
                }.getOrNull()
                if (age != null) {
                    Text(
                        "Возраст: $age лет",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = vm.weight,
                    onValueChange = { vm.weight = it },
                    label = { Text("Вес") },
                    suffix = { Text("кг") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vm.height,
                    onValueChange = { vm.height = it },
                    label = { Text("Рост") },
                    suffix = { Text("см") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Пол", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = vm.gender == "male",
                    onClick = { vm.gender = "male" },
                    label = { Text("Мужской") }
                )
                FilterChip(
                    selected = vm.gender == "female",
                    onClick = { vm.gender = "female" },
                    label = { Text("Женский") }
                )
            }

            HorizontalDivider()

            // ── Цели ───────────────────────────────────────────
            Text("Цели", style = MaterialTheme.typography.titleMedium)

            Text("Задача", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.goal == "loss",
                    onClick = { vm.goal = "loss" },
                    label = { Text("Похудение") }
                )
                FilterChip(
                    selected = vm.goal == "gain",
                    onClick = { vm.goal = "gain" },
                    label = { Text("Набор массы") }
                )
                FilterChip(
                    selected = vm.goal == "maintain",
                    onClick = { vm.goal = "maintain" },
                    label = { Text("Поддержание") }
                )
            }

            ExposedDropdownMenuBox(
                expanded = activityExpanded,
                onExpandedChange = { activityExpanded = !activityExpanded }
            ) {
                OutlinedTextField(
                    value = selectedActivityLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Уровень активности") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = activityExpanded,
                    onDismissRequest = { activityExpanded = false }
                ) {
                    activityOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                vm.activityLevel = value
                                activityExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Gemini API-ключ ────────────────────────────────
            Text("Gemini API-ключ", style = MaterialTheme.typography.titleMedium)
            Text(
                "Получите ключ на: aistudio.google.com/apikey",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Выбор модели
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = !modelExpanded }
            ) {
                OutlinedTextField(
                    value = vm.selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Модель Gemini") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    SecurePrefs.AVAILABLE_MODELS.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                vm.selectedModel = model
                                modelExpanded = false
                            }
                        )
                    }
                }
            }

            // API-ключ
            OutlinedTextField(
                value = vm.apiKey,
                onValueChange = {
                    vm.apiKey = it
                    vm.resetKeyStatus()
                },
                label = { Text("API-ключ") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showApiKey = !showApiKey }) {
                        Text(if (showApiKey) "Скрыть" else "Показать")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { vm.verifyApiKey() },
                    enabled = !vm.isCheckingKey
                ) {
                    if (vm.isCheckingKey) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Проверяем…")
                    } else {
                        Text("Проверить ключ")
                    }
                }

                when (val status = vm.keyStatus) {
                    is OnboardingViewModel.KeyStatus.Valid ->
                        Text("✓ Ключ действителен", color = MaterialTheme.colorScheme.primary)
                    is OnboardingViewModel.KeyStatus.Error ->
                        Text(
                            status.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    else -> {}
                }
            }

            // ── Ошибка формы ──────────────────────────────────
            vm.formError?.let { err ->
                Text(
                    err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Кнопка сохранения ─────────────────────────────
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { vm.save() },
                enabled = !vm.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (vm.isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Начать")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
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
