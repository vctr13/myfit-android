package com.example.myfit.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
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
                        ProfileRow("Возраст", "${p.age} лет")
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
                    vm.models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = { vm.selectModel(model); modelExpanded = false }
                        )
                    }
                }
            }

            Text(
                "Смена модели вступает в силу немедленно для новых сообщений.",
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

            Spacer(Modifier.height(8.dp))
        }
    }

    if (vm.showEditProfile) {
        EditProfileDialog(vm = vm)
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
                OutlinedTextField(
                    value = vm.editAge,
                    onValueChange = { vm.editAge = it },
                    label = { Text("Возраст") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
