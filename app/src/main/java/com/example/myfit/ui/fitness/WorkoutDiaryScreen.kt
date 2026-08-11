package com.example.myfit.ui.fitness

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.db.entity.WorkoutDay
import com.example.myfit.data.db.entity.WorkoutEntry
import com.example.myfit.data.db.model.WorkoutEntryWithExercise
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDiaryScreen(
    onBack: () -> Unit,
    vm: WorkoutDiaryViewModel = viewModel()
) {
    val workouts   by vm.allWorkouts.collectAsState()
    val allEntries by vm.allEntries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дневник тренировок") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (workouts.isEmpty()) {
            Column(
                modifier              = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.FitnessCenter, null,
                    modifier = Modifier.size(56.dp),
                    tint     = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Тренировок пока нет",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Начни тренировку в разделе «Фитнес»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(workouts, key = { it.id }) { day ->
                    val entries = vm.entriesForDay(day.id, allEntries)
                    WorkoutDayCard(
                        day      = day,
                        entries  = entries,
                        onDelete = { vm.deleteDay(day) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun WorkoutDayCard(
    day: WorkoutDay,
    entries: List<WorkoutEntryWithExercise>,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        diaryDateFormat(day.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        day.label ?: "Тренировка",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (day.duration_minutes != null && day.duration_minutes > 0) {
                            InfoChip("${day.duration_minutes} мин")
                        }
                        if (day.calories_burned > 0) {
                            InfoChip("${day.calories_burned} ккал")
                        }
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Delete, null,
                        modifier = Modifier.size(18.dp),
                        tint     = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }

            // Exercise list
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entries.forEach { ew ->
                        ExerciseResultRow(ew)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseResultRow(ew: WorkoutEntryWithExercise) {
    val e = ew.workoutEntry
    val resultText = buildResultText(e)

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(ew.exercise.name, style = MaterialTheme.typography.bodySmall)
            Text(
                ew.exercise.muscle_groups,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            resultText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun buildResultText(e: WorkoutEntry): String {
    val setVals = e.set_values?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
    val weight = if ((e.weight_kg ?: 0f) > 0f) "${"%.1f".format(e.weight_kg)} кг" else ""
    val sep = if (weight.isNotEmpty()) " - " else ""
    return if (!setVals.isNullOrEmpty()) {
        val timeBased = e.duration_sec != null
        val valStr = setVals.joinToString(", ") { if (timeBased) "${it}с" else "$it" }
        "$weight$sep$valStr"
    } else {
        val valStr = when {
            e.duration_sec != null -> "${e.sets}×${e.duration_sec}с"
            e.reps != null         -> "${e.sets}×${e.reps}"
            else                   -> "${e.sets} подх."
        }
        "$weight$sep$valStr"
    }
}

fun formatPrevResult(e: WorkoutEntry): String = "Пред.: ${buildResultText(e)}"

private fun diaryDateFormat(iso: String): String = try {
    val d   = LocalDate.parse(iso)
    val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale.forLanguageTag("ru"))
    d.format(fmt).replaceFirstChar { it.uppercase() }
} catch (e: Exception) { iso }
