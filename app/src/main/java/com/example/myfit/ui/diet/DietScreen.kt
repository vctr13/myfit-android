package com.example.myfit.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import com.example.myfit.ui.theme.CarbsAccent
import com.example.myfit.ui.theme.CarbsBg
import com.example.myfit.ui.theme.FatAccent
import com.example.myfit.ui.theme.FatBg
import com.example.myfit.ui.theme.MacroText
import com.example.myfit.ui.theme.ProteinAccent
import com.example.myfit.ui.theme.ProteinBg
import com.example.myfit.ui.theme.WaterAccent
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.db.entity.FoodEntry
import com.example.myfit.data.db.entity.UserProfile
import com.example.myfit.data.db.model.DailyNutrition
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    onOpenDrawer: () -> Unit,
    vm: DietViewModel = viewModel()
) {
    LaunchedEffect(Unit) { vm.refreshDate() }

    val profile       by vm.profile.collectAsState()
    val today         by vm.todayNutrition.collectAsState()
    val entries       by vm.todayEntries.collectAsState()
    val isTrainingDay by vm.isTrainingDay.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Диета") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Меню")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { vm.openAddFoodDialog() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить еду")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Сегодня") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Цель") })
            }

            when (selectedTab) {
                0 -> TodayTab(
                    profile = profile,
                    today = today,
                    entries = entries,
                    isTrainingDay = isTrainingDay,
                    onToggleTrainingDay = { vm.toggleTrainingDay() },
                    onAddWater = { vm.addWater(it) },
                    onOpenWaterDialog = { vm.openWaterDialog() },
                    onDeleteEntry = { vm.deleteEntry(it) }
                )
                1 -> GoalTab(profile = profile)
            }
        }
    }

    if (vm.showWaterDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissWaterDialog() },
            title = { Text("Своё количество") },
            text = {
                OutlinedTextField(
                    value = vm.waterInput,
                    onValueChange = { vm.waterInput = it },
                    label = { Text("мл") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmWaterDialog() }) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissWaterDialog() }) { Text("Отмена") }
            }
        )
    }

    if (vm.showAddFoodDialog) {
        AddFoodDialog(vm = vm)
    }
}

// ── Вкладка "Сегодня" ────────────────────────────────────────

@Composable
private fun TodayTab(
    profile: UserProfile?,
    today: DailyNutrition,
    entries: List<FoodEntry>,
    isTrainingDay: Boolean,
    onToggleTrainingDay: () -> Unit,
    onAddWater: (Int) -> Unit,
    onOpenWaterDialog: () -> Unit,
    onDeleteEntry: (FoodEntry) -> Unit
) {
    val p = profile ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Тренировочный день — переключатель
        FilterChip(
            selected = isTrainingDay,
            onClick  = onToggleTrainingDay,
            label    = { Text(if (isTrainingDay) "Тренировочный день" else "Обычный день",
                style = MaterialTheme.typography.labelMedium) },
            colors   = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedLabelColor     = MaterialTheme.colorScheme.onTertiaryContainer
            )
        )

        val targetKcal  = if (isTrainingDay) p.target_kcal_training  else p.target_kcal
        val targetWater = if (isTrainingDay) p.target_water_ml_training.toFloat() else p.target_water_ml.toFloat()
        val targetFat   = if (isTrainingDay) p.target_fat_g_training   else p.target_fat_g
        val targetCarbs = if (isTrainingDay) p.target_carbs_g_training else p.target_carbs_g

        // Прогресс
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NutritionRow("Калории", today.calories, targetKcal, "ккал",
                    MaterialTheme.colorScheme.primary)
                NutritionRow("Белки", today.protein, p.target_protein_g, "г",
                    ProteinAccent)
                NutritionRow("Жиры", today.fat, targetFat, "г",
                    FatAccent)
                NutritionRow("Углеводы", today.carbs, targetCarbs, "г",
                    CarbsAccent)
                NutritionRow("Вода", today.water_ml, targetWater, "мл",
                    WaterAccent)
            }
        }

        // Быстрая вода
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WaterDrop, null, tint = WaterAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить воду", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAddWater(200) }, Modifier.weight(1f)) { Text("+200 мл") }
                    OutlinedButton(onClick = { onAddWater(300) }, Modifier.weight(1f)) { Text("+300 мл") }
                    OutlinedButton(onClick = onOpenWaterDialog, Modifier.weight(1f)) { Text("Другое") }
                }
            }
        }

        // Журнал питания
        if (entries.isNotEmpty()) {
            Text("Журнал питания", style = MaterialTheme.typography.titleMedium)
            val grouped = entries.groupBy { normalizeMealType(it.meal_type) }
            val mealOrder = listOf("завтрак", "обед", "ужин", "перекус", "напитки")
            val allKeys = (mealOrder + grouped.keys.filter { it !in mealOrder })
                .distinct().filter { it in grouped }

            allKeys.forEach { mealKey ->
                val mealEntries = grouped[mealKey] ?: return@forEach
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(mealKey.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        mealEntries.forEach { entry ->
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.name, style = MaterialTheme.typography.bodyMedium)
                                    val parts = buildList {
                                        if (entry.calories > 0) add("${entry.calories.roundToInt()} ккал")
                                        if (entry.protein > 0) add("Б:${"%.1f".format(entry.protein)}г")
                                        if (entry.fat > 0) add("Ж:${"%.1f".format(entry.fat)}г")
                                        if (entry.carbs > 0) add("У:${"%.1f".format(entry.carbs)}г")
                                        if (entry.water_ml > 0) add("${entry.water_ml.roundToInt()} мл")
                                    }
                                    if (parts.isNotEmpty()) {
                                        Text(parts.joinToString("  "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { onDeleteEntry(entry) }) {
                                    Icon(Icons.Filled.Delete, null,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(72.dp))
    }
}

// ── Вкладка "Цель" ───────────────────────────────────────────

@Composable
private fun GoalTab(profile: UserProfile?) {
    val p = profile ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Калории", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("${p.target_kcal.roundToInt()} ккал",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SubValue("TDEE", "${p.tdee.roundToInt()} ккал")
                    SubValue("BMR", "${p.bmr.roundToInt()} ккал")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MacroCard("Белки", "${"%.1f".format(p.target_protein_g)} г",
                ProteinBg, MacroText, Modifier.weight(1f))
            MacroCard("Жиры", "${"%.1f".format(p.target_fat_g)} г",
                FatBg, MacroText, Modifier.weight(1f))
            MacroCard("Углеводы", "${"%.1f".format(p.target_carbs_g)} г",
                CarbsBg, MacroText, Modifier.weight(1f))
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, null, tint = WaterAccent,
                    modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text("Вода", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${p.target_water_ml} мл / день",
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ── Диалог добавления еды вручную ────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodDialog(vm: DietViewModel) {
    var mealExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { vm.dismissAddFoodDialog() },
        title = { Text("Добавить вручную") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = vm.addFoodName, onValueChange = { vm.addFoodName = it },
                    label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vm.addFoodKcal, onValueChange = { vm.addFoodKcal = it },
                        label = { Text("Ккал *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = vm.addFoodProtein, onValueChange = { vm.addFoodProtein = it },
                        label = { Text("Белки г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vm.addFoodFat, onValueChange = { vm.addFoodFat = it },
                        label = { Text("Жиры г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = vm.addFoodCarbs, onValueChange = { vm.addFoodCarbs = it },
                        label = { Text("Углев г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                ExposedDropdownMenuBox(expanded = mealExpanded, onExpandedChange = { mealExpanded = !mealExpanded }) {
                    OutlinedTextField(
                        value = vm.addFoodMeal.replaceFirstChar { it.uppercase() },
                        onValueChange = {}, readOnly = true,
                        label = { Text("Приём пищи") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = mealExpanded, onDismissRequest = { mealExpanded = false }) {
                        vm.mealTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.replaceFirstChar { it.uppercase() }) },
                                onClick = { vm.addFoodMeal = type; mealExpanded = false })
                        }
                    }
                }
                vm.addFoodError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.saveManualFood() }) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = { vm.dismissAddFoodDialog() }) { Text("Отмена") } }
    )
}

// ── Вспомогательные composable ────────────────────────────────

private fun normalizeMealType(raw: String): String = when (raw.trim().lowercase()) {
    "breakfast", "завтрак" -> "завтрак"
    "lunch", "обед"        -> "обед"
    "dinner", "ужин"       -> "ужин"
    "snack", "перекус"     -> "перекус"
    "напитки", "drinks"    -> "напитки"
    else -> raw.trim().lowercase()
}

@Composable
private fun NutritionRow(label: String, consumed: Float, target: Float, unit: String, color: Color) {
    val progress = if (target > 0) (consumed / target).coerceIn(0f, 1f) else 0f
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            val consumedStr = if (unit == "ккал" || unit == "мл") "${consumed.roundToInt()}"
                              else "%.1f".format(consumed)
            val targetStr = if (unit == "ккал" || unit == "мл") "${target.roundToInt()}"
                            else "%.1f".format(target)
            Text("$consumedStr / $targetStr $unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(),
            color = color, trackColor = color.copy(alpha = 0.2f))
    }
}

@Composable
private fun MacroCard(label: String, value: String, containerColor: Color,
                      contentColor: Color, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = contentColor)
        }
    }
}

@Composable
private fun SubValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
