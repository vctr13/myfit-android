package com.example.myfit.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.db.entity.ChatMessage
import com.example.myfit.data.model.ParsedFoodData
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDrawer: () -> Unit,
    vm: ChatViewModel = viewModel()
) {
    val messages by vm.messages.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, vm.isLoading) {
        val total = messages.size + if (vm.isLoading) 1 else 0
        if (total > 0) scope.launch { listState.animateScrollToItem(total - 1) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чат с AI") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Меню")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { vm.clearHistory() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Очистить историю")
                        }
                    }
                }
            )
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            // ── Error banner ──────────────────────────────────
            vm.errorMessage?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = err,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { vm.dismissError() }) {
                            Text("OK", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // ── Message list ──────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty() && !vm.isLoading) {
                    EmptyState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(msg)
                        }
                        if (vm.isLoading) {
                            item(key = "typing") { TypingIndicator() }
                        }
                    }
                }
            }

            // ── Food confirm card ─────────────────────────────
            vm.pendingFoodData?.let { foodData ->
                HorizontalDivider()
                FoodConfirmCard(
                    foodData = foodData,
                    onConfirm = { mealType -> vm.confirmFood(mealType) },
                    onDismiss = { vm.dismissFood() }
                )
            }

            // ── Input bar ─────────────────────────────────────
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = vm.inputText,
                    onValueChange = { vm.inputText = it },
                    placeholder = { Text("Спросите о питании или тренировках…") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onSend = { vm.sendMessage() })
                )
                val canSend = vm.inputText.isNotBlank() && !vm.isLoading
                IconButton(
                    onClick = { vm.sendMessage() },
                    enabled = canSend,
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = if (canSend) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Food confirm card ──────────────────────────────────────

@Composable
private fun FoodConfirmCard(
    foodData: ParsedFoodData,
    onConfirm: (mealType: String) -> Unit,
    onDismiss: () -> Unit
) {
    val mealTypes = listOf(
        "завтрак" to "Завтрак",
        "обед"    to "Обед",
        "ужин"    to "Ужин",
        "перекус" to "Перекус"
    )
    var selectedMeal by remember { mutableStateOf("обед") }

    val today = LocalDate.now().toString()
    val entryDate = foodData.date?.takeIf { it.isNotBlank() && it != today }
    val dateFmt = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))
    val dateLabel = entryDate?.let {
        try { "Дата записи: ${LocalDate.parse(it).format(dateFmt)}" } catch (e: Exception) { "Дата: $it" }
    }

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "Добавить в дневник?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            if (dateLabel != null) {
                Text(
                    "⚠ $dateLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(6.dp))

            // Food items (фильтруем воду и напитки — они идут в water_ml)
            val waterNames = setOf("вода", "water", "чай", "кофе", "сок", "молоко", "напиток", "напитки")
            foodData.items.filter { it.name.lowercase() !in waterNames }.forEach { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${item.name} (${item.amountG.roundToInt()} г)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${item.kcal.roundToInt()} ккал",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        "Б:${"%.1f".format(item.protein)}г  Ж:${"%.1f".format(item.fat)}г  У:${"%.1f".format(item.carbs)}г",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            if (foodData.waterMl > 0) {
                Text(
                    "Вода/напитки: ${foodData.waterMl.roundToInt()} мл",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(Modifier.height(8.dp))

            // Meal type selector
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mealTypes.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedMeal == key,
                        onClick = { selectedMeal = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirm(selectedMeal) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Добавить")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text("Отмена")
                }
            }

            Text(
                "Данные неверны? Нажмите «Отмена» и уточните в чате.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Message bubble ─────────────────────────────────────────

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.tertiaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onTertiaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleShape = if (isUser)
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 48.dp
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(color = bubbleColor, shape = bubbleShape) {
            if (isUser) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            } else {
                MarkdownText(
                    markdown = message.content,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = contentColor
                )
            }
        }
    }
}

// ── Typing indicator ───────────────────────────────────────

@Composable
private fun TypingIndicator() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Gemini думает…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Empty state ────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Привет! Я ваш AI-нутрициолог.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Спросите про питание, калории, рецепты или тренировки — отвечу с учётом вашего профиля.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
