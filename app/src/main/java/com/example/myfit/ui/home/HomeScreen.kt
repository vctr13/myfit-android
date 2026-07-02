package com.example.myfit.ui.home

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import com.example.myfit.ui.theme.CarbsAccent
import com.example.myfit.ui.theme.FatAccent
import com.example.myfit.ui.theme.ProteinAccent
import com.example.myfit.ui.theme.WaterAccent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.db.entity.WeightEntry
import com.example.myfit.data.db.model.DailyNutrition
import com.example.myfit.data.db.entity.UserProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        vm.refreshDate()
        vm.refreshStepCalories()
    }

    val profile             by vm.profile.collectAsState()
    val today               by vm.todayNutrition.collectAsState()
    val weightHistory       by vm.weightHistory.collectAsState()
    val weightPeriod        by vm.weightPeriod.collectAsState()
    val isTrainingDay       by vm.isTrainingDay.collectAsState()
    val stepCalories        by vm.stepCalories.collectAsState()
    val workoutCalories     by vm.todayWorkoutCalories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Главная") },
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val p = profile

            // ── Greeting ──────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text  = "${vm.greeting}!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (p != null) {
                    Text(
                        text  = "Цель: ${goalLabel(p.goal)} · ${p.target_kcal.roundToInt()} ккал / день",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (p != null) {
                // Тренировочный день — переключатель
                FilterChip(
                    selected = isTrainingDay,
                    onClick  = { vm.toggleTrainingDay() },
                    label    = { Text(if (isTrainingDay) "Тренировочный день" else "Обычный день",
                        style = MaterialTheme.typography.labelMedium) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor     = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )

                val targetKcal  = if (isTrainingDay) p.target_kcal_training  else p.target_kcal
                val targetWater = if (isTrainingDay) p.target_water_ml_training.toFloat() else p.target_water_ml.toFloat()

                CaloriesCard(
                    eaten           = today.calories,
                    target          = targetKcal,
                    stepCalories    = stepCalories,
                    workoutCalories = workoutCalories,
                    accentColor     = MaterialTheme.colorScheme.primary
                )

                MacroCard(today = today, profile = p, isTrainingDay = isTrainingDay)

                NutritionSummaryCard(
                    title       = "Вода",
                    consumed    = today.water_ml,
                    target      = targetWater,
                    unit        = "мл",
                    accentColor = WaterAccent
                )
            }

            WeightCard(
                entries       = weightHistory,
                period        = weightPeriod,
                onPeriodChange = { vm.setWeightPeriod(it) },
                onAddClick    = { vm.openWeightDialog() }
            )
        }
    }

    if (vm.showWeightDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissWeightDialog() },
            title = { Text("Добавить вес") },
            text = {
                Column {
                    OutlinedTextField(
                        value         = vm.weightInput,
                        onValueChange = { vm.weightInput = it },
                        label         = { Text("кг (например 74.5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    vm.weightError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton  = { TextButton(onClick = { vm.saveWeight() }) { Text("Сохранить") } },
            dismissButton  = { TextButton(onClick = { vm.dismissWeightDialog() }) { Text("Отмена") } }
        )
    }
}

// ── Calories card ─────────────────────────────────────────────────────────────

@Composable
private fun CaloriesCard(
    eaten: Float,
    target: Float,
    stepCalories: Int,
    workoutCalories: Int,
    accentColor: Color
) {
    val burnedTotal = stepCalories + workoutCalories
    val net = (eaten - burnedTotal).coerceAtLeast(0f)
    val progress = if (target > 0) (net / target).coerceIn(0f, 1f) else 0f
    val remaining = (target - net).coerceAtLeast(0f)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Калории", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            // Съедено
            CalorieRow(
                label = "Съедено",
                value = "${eaten.roundToInt()} ккал",
                valueColor = accentColor,
                bold = false
            )
            Spacer(Modifier.height(4.dp))

            // Шаги
            CalorieRow(
                label = "− Шаги",
                value = "${stepCalories} ккал",
                valueColor = MaterialTheme.colorScheme.tertiary,
                bold = false
            )
            Spacer(Modifier.height(4.dp))

            // Тренировки
            CalorieRow(
                label = "− Тренировки",
                value = "${workoutCalories} ккал",
                valueColor = MaterialTheme.colorScheme.tertiary,
                bold = false
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            // Фактические (net)
            CalorieRow(
                label = "Фактические",
                value = "${net.roundToInt()} / ${target.roundToInt()} ккал",
                valueColor = accentColor,
                bold = true
            )

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth(),
                color      = accentColor,
                trackColor = accentColor.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = if (remaining > 0) "Осталось: ${remaining.roundToInt()} ккал" else "Цель достигнута!",
                style = MaterialTheme.typography.bodySmall,
                color = if (remaining > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CalorieRow(label: String, value: String, valueColor: Color, bold: Boolean) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Weight card ───────────────────────────────────────────────────────────────

@Composable
private fun WeightCard(
    entries: List<WeightEntry>,
    period: WeightPeriod,
    onPeriodChange: (WeightPeriod) -> Unit,
    onAddClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Динамика веса", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Внести вес", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Period selector
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WeightPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = p == period,
                        onClick  = { onPeriodChange(p) },
                        label    = { Text(p.label, style = MaterialTheme.typography.labelSmall) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor     = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }

            if (entries.isEmpty()) {
                Text(
                    "Нет данных за период. Нажмите + чтобы внести вес.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Current weight + delta
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text  = "Текущий",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = "${"%.1f".format(entries.last().weight_kg)} кг",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (entries.size >= 2) {
                        val diff = entries.last().weight_kg - entries.first().weight_kg
                        val sign = if (diff >= 0) "+" else ""
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text  = "За период",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text  = "$sign${"%.1f".format(diff)} кг",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (diff <= 0f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Graph
                if (entries.size >= 2) {
                    WeightGraph(entries = entries)
                }
            }
        }
    }
}

// ── Weight graph with axes ────────────────────────────────────────────────────

@Composable
private fun WeightGraph(entries: List<WeightEntry>) {
    if (entries.size < 2) return

    val lineColor  = MaterialTheme.colorScheme.primary
    val fillColor  = lineColor.copy(alpha = 0.15f)
    val gridColor  = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val labelColorArgb = labelColor.toArgb()
    val density        = LocalDensity.current
    val textSizePx     = with(density) { 10.sp.toPx() }

    val minW = entries.minOf { it.weight_kg }
    val maxW = entries.maxOf { it.weight_kg }
    val step     = niceStep((maxW - minW).toDouble()).toFloat()
    val gridMin  = (floor((minW / step).toDouble()) * step).toFloat()
    val gridMax  = (ceil((maxW / step).toDouble()) * step).toFloat()
    val gridCount = ((gridMax - gridMin) / step).toInt().coerceIn(1, 8)
    val yRange   = (gridMax - gridMin).coerceAtLeast(step)

    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val lp = 44.dp.toPx()
        val rp = 8.dp.toPx()
        val tp = 8.dp.toPx()
        val bp = 24.dp.toPx()
        val gw = size.width - lp - rp
        val gh = size.height - tp - bp
        val n  = entries.size

        fun xOf(i: Int) = lp + (i.toFloat() / (n - 1)) * gw
        fun yOf(kg: Float) = tp + (1f - (kg - gridMin) / yRange).coerceIn(0f, 1f) * gh

        // ── Grid lines ────────────────────────────────────────────────────
        for (i in 0..gridCount) {
            val y = tp + (1f - (i.toFloat() * step) / yRange) * gh
            drawLine(gridColor, Offset(lp, y), Offset(lp + gw, y), 0.8.dp.toPx())
        }

        // ── Gradient fill ─────────────────────────────────────────────────
        val pts = entries.mapIndexed { i, e -> Offset(xOf(i), yOf(e.weight_kg)) }
        val fillPath = Path().apply {
            moveTo(pts.first().x, tp + gh)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, tp + gh)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(
            listOf(fillColor, Color.Transparent), tp, tp + gh
        ))

        // ── Line ──────────────────────────────────────────────────────────
        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(linePath, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

        // ── Dots ──────────────────────────────────────────────────────────
        pts.forEachIndexed { idx, pt ->
            if (idx == pts.lastIndex) {
                drawCircle(Color.White, 5.dp.toPx(), pt)
                drawCircle(lineColor,   4.dp.toPx(), pt, style = Stroke(2.dp.toPx()))
            } else {
                drawCircle(lineColor, 2.5.dp.toPx(), pt)
            }
        }

        // ── Text labels (Y + X) ───────────────────────────────────────────
        val xIdxs = pickLabelIndices(n, maxLabels = 4)
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                isAntiAlias = true
                textSize    = textSizePx
                color       = labelColorArgb
            }
            paint.textAlign = Paint.Align.RIGHT
            for (i in 0..gridCount) {
                val kg = gridMin + i * step
                val y  = tp + (1f - (i.toFloat() * step) / yRange) * gh
                canvas.nativeCanvas.drawText(
                    "%.0f".format(kg), lp - 4.dp.toPx(), y + textSizePx * 0.38f, paint
                )
            }
            paint.textAlign = Paint.Align.CENTER
            xIdxs.forEach { idx ->
                canvas.nativeCanvas.drawText(
                    formatShortDate(entries[idx].date),
                    xOf(idx),
                    tp + gh + bp - 4.dp.toPx(),
                    paint
                )
            }
        }
    }
}

// ── Nutrition cards ───────────────────────────────────────────────────────────

@Composable
private fun NutritionSummaryCard(
    title: String,
    consumed: Float,
    target: Float,
    unit: String,
    accentColor: Color
) {
    val progress  = if (target > 0) (consumed / target).coerceIn(0f, 1f) else 0f
    val remaining = (target - consumed).coerceAtLeast(0f)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                Text(
                    "${consumed.roundToInt()}",
                    style = MaterialTheme.typography.displaySmall,
                    color = accentColor
                )
                Text(
                    "/ ${target.roundToInt()} $unit",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth(),
                color      = accentColor,
                trackColor = accentColor.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = if (remaining > 0) "Осталось: ${remaining.roundToInt()} $unit" else "Цель достигнута!",
                style = MaterialTheme.typography.bodySmall,
                color = if (remaining > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MacroCard(today: DailyNutrition, profile: UserProfile, isTrainingDay: Boolean) {
    val targetFat   = if (isTrainingDay) profile.target_fat_g_training   else profile.target_fat_g
    val targetCarbs = if (isTrainingDay) profile.target_carbs_g_training else profile.target_carbs_g
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("БЖУ сегодня", style = MaterialTheme.typography.titleMedium)
            MacroRow("Белки",    today.protein, profile.target_protein_g, "г", ProteinAccent)
            MacroRow("Жиры",     today.fat,     targetFat,               "г", FatAccent)
            MacroRow("Углеводы", today.carbs,   targetCarbs,             "г", CarbsAccent)
        }
    }
}

@Composable
private fun MacroRow(label: String, consumed: Float, target: Float, unit: String, color: Color) {
    val progress = if (target > 0) (consumed / target).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${"%.1f".format(consumed)} / ${"%.1f".format(target)} $unit",
                style = MaterialTheme.typography.bodySmall
            )
        }
        LinearProgressIndicator(
            progress   = { progress },
            modifier   = Modifier.fillMaxWidth(),
            color      = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun niceStep(range: Double): Double = when {
    range <= 0  -> 1.0
    range < 2   -> 0.5
    range < 5   -> 1.0
    range < 10  -> 2.0
    range < 20  -> 5.0
    range < 50  -> 10.0
    else        -> 20.0
}

private fun pickLabelIndices(size: Int, maxLabels: Int): List<Int> {
    if (size <= maxLabels) return (0 until size).toList()
    val step = (size - 1).toFloat() / (maxLabels - 1)
    return (0 until maxLabels).map { (it * step).roundToInt() }
}

private fun formatShortDate(iso: String): String = try {
    val d   = LocalDate.parse(iso)
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru"))
    d.format(fmt)
} catch (e: Exception) { iso.takeLast(5) }

private fun goalLabel(goal: String) = when (goal) {
    "loss"     -> "снижение веса"
    "gain"     -> "набор массы"
    "maintain" -> "поддержание"
    else       -> goal
}
