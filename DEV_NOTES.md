# Заметки по разработке Android + Gemini API

Документ содержит общие уроки, применимые к любому Android-проекту с Gemini API.

---

## Gemini API — модели

| Модель | Скорость | Качество | Бесплатный лимит |
|--------|----------|----------|-----------------|
| gemini-2.5-flash | медленная | высокое (thinking) | есть |
| gemini-2.5-flash-lite | быстрая | хорошее | есть |
| gemini-2.0-flash | быстрая | хорошее | **нет** — не использовать |
| gemini-3-flash | средняя | высокое | есть |

**Вывод:** всегда реализуй переключение модели в настройках — 503 ошибки неизбежны при пиковой нагрузке на серверы.

---

## Gemini API — лимиты бесплатного уровня (Free Tier)

> Лимиты меняются. Актуальные значения: https://ai.google.dev/gemini-api/docs/rate-limits

| Модель | RPM (запросов/мин) | RPD (запросов/день) | TPM (токенов/мин) |
|--------|-------------------|--------------------|--------------------|
| gemini-2.5-flash | **10** | **250** | 250 000 |
| gemini-2.5-flash-lite | **15** | **1 000** | 250 000 |

### Что это означает на практике

- **10 RPM** = минимум **6 секунд** между запросами для flash (безопасный интервал — 7 с)
- **15 RPM** = минимум **4 секунды** между запросами для flash-lite
- **250 RPD** для flash — очень мало. При активном тестировании лимит исчерпывается за час (~4 запроса в минуту в течение часа). Переключайся на flash-lite при разработке.
- Лимит RPD сбрасывается в полночь по тихоокеанскому времени (UTC−8).
- Google периодически **снижает** лимиты без предупреждения (flash RPD был снижен с 1500 до 250).

### Рекомендации

```kotlin
// Не делай запросы чаще чем нужно:
// 1. Отправляй запрос только по явному действию пользователя (не автоматически)
// 2. Не повторяй запрос автоматически при ошибке — пусть пользователь решает
// 3. Кэшируй ответы где возможно (например, системный промпт)

// Обработка 429 (rate limit exceeded):
if (error.code == 429) {
    // Показать: "Превышен лимит запросов. Попробуйте через минуту."
    // НЕ делать автоматический retry — это заблокирует аккаунт
}
```

### Часы (RPH) — неофициально

Google не публикует лимит в час явно, но на практике при 10 RPM:
- Теоретически 600 запросов/час, но RPD = 250 является жёстким ограничением
- Реально: ~10 запросов/мин в течение 25 минут — и дневной лимит исчерпан

---

## Gemini API — обработка ошибок

- **503 (Service Unavailable)** — частая ошибка при высокой нагрузке. Не баг приложения.
  - Решение: переключение модели в UI без перезапуска приложения.
- **Таймауты** — длинный системный промпт увеличивает время ответа. Оптимизируй промпт.
- Всегда показывай пользователю текст ошибки из API (errorMessage в ViewModel).

---

## Системный промпт — ключевые правила

### Структурированные данные (JSON из ответа AI)

Если AI должен возвращать структурированные данные внутри текста:

1. **Явно указывай теги** для извлечения:
   ```
   [DATA]{...json...}[/DATA]
   ```
2. **Используй тройные кавычки** в Kotlin для regex — иначе экранирование ломается:
   ```kotlin
   // ПРАВИЛЬНО
   val regex = Regex("""\[DATA]([\s\S]*?)\[/DATA]""")
   // НЕПРАВИЛЬНО — скобки не экранированы корректно
   val regex = Regex("\[DATA]([\s\S]*?)\[/DATA]")
   ```
3. **КРИТИЧНО:** без явной инструкции AI накапливает данные из всей истории диалога, а не только из текущего сообщения:
   ```
   ВАЖНО: включай в блок [DATA] ТОЛЬКО данные из ПОСЛЕДНЕГО сообщения пользователя.
   НЕ включай данные из предыдущих сообщений или из истории разговора.
   ```

### Разделение типов данных

Если в одном запросе есть разные типы (например, еда и вода):
- Явно объясни какой тип куда идёт:
  ```
  items[] — только твёрдая еда. НЕ добавляй воду/напитки в items[].
  water_ml — только напитки в миллилитрах. НЕ путать с граммами.
  ```
- AI путает единицы измерения (мл vs г vs мг) без явного указания.

### Пользовательские данные в промпте

Если нужно использовать точные значения из БД пользователя (свои продукты, нормы):
- Передавай их в системный промпт при каждом запросе
- Добавляй явную инструкцию: "используй ТОЧНО эти значения, умножай на количество"
- Без этого AI использует усреднённые данные из USDA

---

## Room Database — паттерны

### Миграции

```kotlin
// AppDatabase.kt
@Database(version = N, ...)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_X_Y = object : Migration(X, Y) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE table_name ADD COLUMN new_col TYPE NOT NULL DEFAULT value")
            }
        }
    }
}

// MyFitApp.kt — обязательно добавить ВСЕ миграции
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)  // не пропускать
    .build()
```

Если пропустить хотя бы одну миграцию в цепочке — краш при обновлении.

### Flow с датой в ViewModel

**Проблема:** `val today = LocalDate.now().toString()` вычисляется один раз при создании ViewModel. Если ViewModel живёт через полночь — показывает вчерашние данные.

**Решение:**
```kotlin
private val _dateFlow = MutableStateFlow(LocalDate.now().toString())

val todayEntries = _dateFlow
    .flatMapLatest { date -> dao.getByDate(date) }
    .stateIn(...)

fun refreshDate() { _dateFlow.value = LocalDate.now().toString() }
```
```kotlin
// В Composable:
LaunchedEffect(Unit) { vm.refreshDate() }
```

Также при вставке данных (insert) всегда используй `LocalDate.now()` напрямую, не кэшируй.

---

## Навигация Jetpack Compose

### Единый паттерн для всех экранов в drawer

Все пункты навигации (включая Настройки) должны использовать одинаковый блок:
```kotlin
navController.navigate(route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

**Проблема:** если часть экранов навигируется без `popUpTo`, `restoreState = true` восстанавливает старый back stack включая «чужие» экраны — пользователь видит не тот экран.

### Навигация с аргументами

```kotlin
// Screen.kt
data object Detail : Screen("detail/{id}") {
    fun createRoute(id: String) = "detail/$id"
    const val ARG = "id"
}

// NavHost
composable(
    route = Screen.Detail.route,
    arguments = listOf(navArgument(Screen.Detail.ARG) { type = NavType.StringType })
) { backStack ->
    val id = backStack.arguments?.getString(Screen.Detail.ARG) ?: return@composable
    DetailScreen(id = id)
}
```

---

## Jetpack Compose — особенности

### ExposedDropdownMenuBox

```kotlin
// Обязательно:
modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
// Если не добавить — дропдаун не привязывается к полю
```

### flatMapLatest требует аннотации

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModel : ViewModel() {
    // ...
}
```

### PrimaryTabRow — экспериментальный API

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
```

### LinearProgressIndicator — новый синтаксис в Material3

```kotlin
// ПРАВИЛЬНО (Compose 1.6+)
LinearProgressIndicator(progress = { value })
// УСТАРЕВШЕЕ
LinearProgressIndicator(progress = value)
```

---

## PowerShell + UTF-8 (при работе с русскими символами)

```powershell
# НЕПРАВИЛЬНО — ломает кириллицу
Set-Content -Path $file -Value $content

# ПРАВИЛЬНО
[System.IO.File]::WriteAllText($path, $content, [System.Text.Encoding]::UTF8)

# Чтение
[System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
```

---

## Архитектура AI-функционала

### Паттерн: AI → структурированные данные → подтверждение пользователя

```
Пользователь пишет → AI отвечает текстом + JSON в тегах
→ Парсинг JSON из ответа
→ Показать карточку подтверждения пользователю  
→ Пользователь подтверждает → запись в БД
```

Преимущества:
- Пользователь контролирует что попадает в БД
- Можно корректировать данные перед записью
- AI ошибается — подтверждение как фильтр

### Где хранить API ключ

Никогда не хранить в `SharedPreferences` напрямую. Использовать `EncryptedSharedPreferences` (Android Keystore):
```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
EncryptedSharedPreferences.create(context, "secure_prefs", masterKey, ...)
```

### Размер системного промпта

- Каждые ~500 токенов промпта = +100-200мс к ответу
- Включай пользовательские данные (продукты, профиль) только если они есть
- Не дублируй инструкции

---

## Чеклист перед запуском нового проекта с Gemini

- [ ] Реализовано переключение модели в UI
- [ ] Обработка 503 с понятным сообщением пользователю  
- [ ] API ключ в EncryptedSharedPreferences
- [ ] Regex для парсинга данных использует тройные кавычки `"""`
- [ ] Системный промпт содержит "только из текущего сообщения"
- [ ] Room миграции добавлены в цепочку в Application
- [ ] Дата в ViewModel через MutableStateFlow, не как val
- [ ] Вся навигация drawer использует единый popUpTo паттерн
- [ ] `LaunchedEffect(Unit) { vm.refreshDate() }` на экранах с фильтрацией по дате
