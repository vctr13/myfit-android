package com.example.myfit.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import android.widget.Toast
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfit.data.db.entity.ChatMessage
import com.example.myfit.data.model.ParsedFoodData
import com.example.myfit.data.model.ParsedFoodItem
import com.example.myfit.data.model.scaledTo
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToDiet: () -> Unit,
    vm: ChatViewModel = viewModel()
) {
    val messages by vm.messages.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) vm.onSpeechRecognized(text)
        }
    }

    fun startAndroidVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Назовите, что съели")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            speechLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Распознавание речи недоступно на этом устройстве", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startRecordingVoiceGemini()
        else Toast.makeText(context, "Нужно разрешение на микрофон", Toast.LENGTH_SHORT).show()
    }

    fun onMicClick() {
        when (vm.speechRecognitionMethod) {
            "gemini" -> when {
                vm.isRecordingVoice -> vm.stopRecordingVoiceGemini()
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> vm.startRecordingVoiceGemini()
                else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> startAndroidVoiceInput()
        }
    }

    LaunchedEffect(Unit) { vm.refreshDate() }

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
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.Filled.Home, contentDescription = "Главная")
                    }
                    IconButton(onClick = onNavigateToDiet) {
                        Icon(Icons.Filled.Restaurant, contentDescription = "Диета")
                    }
                    IconButton(onClick = { vm.openFactsDialog() }) {
                        Icon(Icons.Filled.Psychology, contentDescription = "Память бота")
                    }
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
                            MessageBubble(msg, onSaveToMemory = { vm.openSaveFact(msg) })
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
                    onConfirm = { mealType, editedItems -> vm.confirmFood(mealType, editedItems) },
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
                IconButton(
                    onClick = { onMicClick() },
                    enabled = !vm.isLoading && !vm.isTranscribing,
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = if (vm.isRecordingVoice) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    if (vm.isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = if (vm.isRecordingVoice) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (vm.isRecordingVoice) "Остановить запись" else "Надиктовать",
                            tint = if (vm.isRecordingVoice) MaterialTheme.colorScheme.onError
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

    if (vm.showSaveFactDialog) SaveFactDialog(vm)
    if (vm.showFactsDialog)    FactsDialog(vm)
}

// ── Память бота: сохранение факта ───────────────────────────

@Composable
private fun SaveFactDialog(vm: ChatViewModel) {
    AlertDialog(
        onDismissRequest = { vm.dismissSaveFact() },
        title = { Text("Добавить в память бота") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "ИИ будет использовать это в других ответах",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = vm.factKeyInput,
                    onValueChange = { vm.factKeyInput = it },
                    label = { Text("Название (например: Аллергия)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vm.factValueInput,
                    onValueChange = { vm.factValueInput = it },
                    label = { Text("Текст") },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                vm.factError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { vm.confirmSaveFact() }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = { vm.dismissSaveFact() }) { Text("Отмена") }
        }
    )
}

// ── Память бота: просмотр и удаление фактов ─────────────────

@Composable
private fun FactsDialog(vm: ChatViewModel) {
    val facts by vm.facts.collectAsState()

    AlertDialog(
        onDismissRequest = { vm.dismissFactsDialog() },
        title = { Text("Память бота") },
        text = {
            if (facts.isEmpty()) {
                Text(
                    "Пока ничего не сохранено. Нажмите значок закладки у любого сообщения в чате, чтобы бот запомнил его.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(facts, key = { it.id }) { fact ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(fact.key, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    fact.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { vm.deleteFact(fact.key) }) {
                                Icon(
                                    Icons.Filled.Delete, null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { vm.dismissFactsDialog() }) { Text("Закрыть") }
        }
    )
}

// ── Food confirm card ──────────────────────────────────────

@Composable
private fun FoodConfirmCard(
    foodData: ParsedFoodData,
    onConfirm: (mealType: String, editedItems: List<ParsedFoodItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val mealTypes = listOf(
        "завтрак" to "Завтрак",
        "обед"    to "Обед",
        "ужин"    to "Ужин",
        "перекус" to "Перекус"
    )

    // Граммы редактируемы: AI иногда верно считает КБЖУ по весу, но не возвращает сам amount_g
    // в JSON (особенно если в одном сообщении сразу несколько продуктов) — без этого поля
    // граммы не показывались бы ни в журнале, ни в архиве, и пользователь узнавал бы об ошибке
    // только постфактум. Редактируем здесь же, до того как запись уйдёт в БД.
    val gramsText = remember(foodData) {
        foodData.items.map { item ->
            mutableStateOf(if (item.amountG > 0f) item.amountG.roundToInt().toString() else "")
        }
    }
    var selectedMeal by remember {
        val h = LocalTime.now().hour
        val m = LocalTime.now().minute
        val auto = when {
            h in 6..10               -> "завтрак"
            h in 11..14 || (h == 15 && m == 0) -> "обед"
            h in 18..20              -> "ужин"
            else                     -> "перекус"
        }
        mutableStateOf(auto)
    }

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
            foodData.items.forEachIndexed { index, item ->
                if (item.name.lowercase() in waterNames) return@forEachIndexed
                val editedGrams = gramsText[index].value.toFloatOrNull() ?: 0f
                val isGramsError = gramsText[index].value.toIntOrNull().let { it == null || it == 0 }
                // КБЖУ пересчитывается пропорционально при каждом изменении граммов —
                // читает то же состояние, что и поле ввода, поэтому обновляется само
                val displayItem = item.scaledTo(editedGrams)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        val focusRequester = remember { FocusRequester() }
                        // Видимая рамка компактная, но зона нажатия вокруг неё расширена
                        // до рекомендованного минимума 48dp — иначе в маленькую рамку тяжело
                        // попасть пальцем (что и произошло: без этого поле выглядело нерабочим).
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { focusRequester.requestFocus() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (isGramsError) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicTextField(
                                    value = gramsText[index].value,
                                    onValueChange = { new ->
                                        if (new.length <= 5 && new.all { it.isDigit() }) gramsText[index].value = new
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(32.dp).focusRequester(focusRequester)
                                )
                                Text(
                                    "г",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Text(
                            "${displayItem.kcal.roundToInt()} ккал",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        "Б:${"%.1f".format(displayItem.protein)}г  Ж:${"%.1f".format(displayItem.fat)}г  У:${"%.1f".format(displayItem.carbs)}г",
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
                    onClick = {
                        val editedItems = foodData.items.mapIndexed { index, item ->
                            item.scaledTo(gramsText[index].value.toFloatOrNull() ?: 0f)
                        }
                        onConfirm(selectedMeal, editedItems)
                    },
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
private fun MessageBubble(message: ChatMessage, onSaveToMemory: () -> Unit) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.tertiaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onTertiaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleShape = if (isUser)
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

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
            Column(modifier = Modifier.widthIn(max = 320.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onSaveToMemory,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkAdd,
                            contentDescription = "Добавить в память бота",
                            tint = contentColor
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Скопировать текст",
                            tint = contentColor
                        )
                    }
                }
                if (isUser) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                } else {
                    SelectionContainer {
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

