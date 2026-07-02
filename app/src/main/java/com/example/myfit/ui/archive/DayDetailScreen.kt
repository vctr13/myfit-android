package com.example.myfit.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.db.entity.FoodEntry
import com.example.myfit.data.db.model.DailyNutrition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: String,
    onBack: () -> Unit,
    vm: ArchiveViewModel = viewModel()
) {
    val entries by vm.loadEntries(date).collectAsState()
    val totals by vm.loadTotals(date).collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formatDate(date)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Итого за день
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Итого за день", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("${totals.calories.roundToInt()} ккал",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Б: ${"%.1f".format(totals.protein)}г  " +
                            "Ж: ${"%.1f".format(totals.fat)}г  " +
                            "У: ${"%.1f".format(totals.carbs)}г  " +
                            "Вода: ${totals.water_ml.roundToInt()} мл",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Журнал по приёмам пищи
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
                                        Text(entry.time, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { vm.deleteEntry(entry) }) {
                                        Icon(Icons.Filled.Delete, null,
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun normalizeMealType(raw: String): String = when (raw.trim().lowercase()) {
    "breakfast", "завтрак" -> "завтрак"
    "lunch", "обед"        -> "обед"
    "dinner", "ужин"       -> "ужин"
    "snack", "перекус"     -> "перекус"
    "напитки", "drinks"    -> "напитки"
    else -> raw.trim().lowercase()
}

private fun formatDate(iso: String): String = try {
    val d = LocalDate.parse(iso)
    val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    d.format(fmt)
} catch (e: Exception) { iso }
