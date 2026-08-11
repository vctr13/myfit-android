# DEV_NOTES — Android + Gemini API

---

## ПРАВИЛА ДЛЯ AI — ЧИТАТЬ ПЕРЕД НАЧАЛОМ РАБОТЫ

### Честность

- **Не выдумывать причины ошибок.** Если причина неизвестна — сказать "не знаю" и проверить фактически (прочитать файл, запустить сборку, проверить байты).
- **Не утверждать что задача выполнена без проверки.** BUILD SUCCESSFUL должно быть в реальном выводе инструмента, не предполагаться.
- **Не придумывать объяснения задним числом.** Если сделал ошибку — признать, найти реальную причину через инструменты, исправить.
- **Не ссылаться на ограничения которые не подтверждены.** Прежде чем объяснять ошибку "визуальной схожестью символов" или "особенностью рендеринга" — проверить байтовые значения и убедиться что это действительно так.

### Верификация пути перед редактированием файла

AI не может визуально отличить похожие символы в именах пакетов (например, `t` vs `x` в конце слова). Это приводит к созданию файлов в неправильных папках и неправильным импортам.

**Корень проблемы:** AI копирует имена из вывода инструментов. Если в проекте существует файл или папка с неправильным именем — AI будет продолжать использовать это имя, потому что видит его в реальных путях. Это распространение ошибки, а не визуальная путаница.

**Дополнительный риск — Unicode-омоглифы:** кириллическая `а` (U+0430) и латинская `a` (U+0061) визуально неотличимы, но это разные символы. Класс с кириллической буквой в имени пакета не найдётся при импорте с латинской буквой.

**Обязательно перед любым созданием или редактированием файла:**

```powershell
# 1. Найти реальный путь через Glob — не угадывать из памяти или истории чата
# Glob: **/ИмяФайла.kt

# 2. Прочитать первую строку — убедиться в точном имени пакета
Get-Content "путь\Файл.kt" -Encoding UTF8 | Select-Object -First 1

# 3. Проверить что нет файлов-дублей в соседних папках
Get-ChildItem "родительская\папка\" -Directory | Select-Object Name

# 4. После записи — верифицировать:
(Get-Content $path -Encoding UTF8)[0]
```

**Что происходит при нарушении:** файл создаётся не там → компилятор находит дубли → все импорты unresolved → BUILD FAILED.

**Правило:** путь к файлу — всегда из `Glob`, никогда из памяти или предыдущего вывода инструментов.

---

### Кириллица в Kotlin-файлах

PowerShell 5.1 по умолчанию записывает файлы в Windows-1252. Кириллица превращается в `РєРєР°Р»` вместо `ккал`. Файл компилируется, но UI показывает иероглифы.

```powershell
# НЕЛЬЗЯ
Set-Content -Path $file -Value $content
Out-File -FilePath $file -InputObject $content

# МОЖНО — единственный безопасный способ через PowerShell
[System.IO.File]::WriteAllText($path, $content, [System.Text.Encoding]::UTF8)

# Проверка повреждения (ищем мусорные последовательности):
Get-Content $path -Encoding UTF8 | Select-String "РК|Рє|С‚|РЈ"
```

Инструменты `Write` и `Edit` (Claude Code) записывают корректно напрямую.

---

## Gemini API

### Модели

| Модель | Скорость | Качество | Free Tier |
|--------|----------|----------|-----------|
| gemini-2.5-flash | медленная | высокое (thinking) | есть |
| gemini-2.5-flash-lite | быстрая | хорошее | есть |
| gemini-2.0-flash | быстрая | хорошее | **нет** |
| gemini-3-flash | средняя | высокое | есть |

Всегда реализуй переключение модели в настройках — 503 ошибки неизбежны при пиковой нагрузке.

### Лимиты Free Tier

> Лимиты меняются: https://ai.google.dev/gemini-api/docs/rate-limits

| Модель | RPM | RPD | TPM |
|--------|-----|-----|-----|
| gemini-2.5-flash | 10 | 250 | 250 000 |
| gemini-2.5-flash-lite | 15 | 1 000 | 250 000 |

- 10 RPM = 6 с между запросами (безопасно — 7 с)
- 250 RPD для flash исчерпывается за ~25 минут активной разработки → используй flash-lite при разработке
- RPD сбрасывается в полночь UTC−8
- Google снижает лимиты без предупреждения

### Обработка ошибок

- **503** — высокая нагрузка, не баг. Решение: переключение модели в UI.
- **429** — лимит запросов. Показать сообщение, **не делать авто-retry**.
- **Таймаут** — длинный системный промпт. Оптимизируй промпт.
- Всегда показывай пользователю текст ошибки из API.

### Системный промпт

**Структурированные данные из ответа AI:**

```kotlin
// Теги для извлечения JSON:
// [DATA]{...json...}[/DATA]

// Regex — обязательно тройные кавычки:
val regex = Regex("""\[DATA]([\s\S]*?)\[/DATA]""")
```

**Критично:** без явной инструкции AI накапливает данные из всей истории:
```
ВАЖНО: включай в [DATA] ТОЛЬКО данные из ПОСЛЕДНЕГО сообщения пользователя.
```

**Разные типы данных в одном запросе** — явно разграничивай:
```
items[] — только еда. water_ml — только напитки в мл, не путать с граммами.
```

**Пользовательские данные** — передавай в системный промпт каждый раз с инструкцией "используй ТОЧНО эти значения".

**Размер промпта:** ~500 токенов = +100-200 мс к ответу. Включай пользовательские данные только если они есть.

### Где хранить API ключ

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
EncryptedSharedPreferences.create(context, "secure_prefs", masterKey, ...)
```

### Паттерн AI → подтверждение → БД

```
Пользователь пишет → AI отвечает текстом + JSON в тегах
→ Парсинг JSON → карточка подтверждения → пользователь OK → запись в БД
```

---

## Room Database

### Миграции

```kotlin
// AppDatabase.kt
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE t ADD COLUMN col TYPE DEFAULT val")
    }
}

// Application.kt — добавить ВСЕ миграции, не пропускать ни одну
Room.databaseBuilder(...).addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...).build()
```

Пропущенная миграция в цепочке = краш при обновлении.

### Дата в ViewModel

```kotlin
// НЕПРАВИЛЬНО — вычисляется один раз, не обновляется через полночь
val today = LocalDate.now().toString()

// ПРАВИЛЬНО
private val _dateFlow = MutableStateFlow(LocalDate.now().toString())
val entries = _dateFlow.flatMapLatest { date -> dao.getByDate(date) }.stateIn(...)
fun refreshDate() { _dateFlow.value = LocalDate.now().toString() }

// В Composable:
LaunchedEffect(Unit) { vm.refreshDate() }
```

При insert — всегда `LocalDate.now()` напрямую, не кэшировать.

---

## Навигация Jetpack Compose

### Drawer — единый паттерн

```kotlin
navController.navigate(route) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

Если часть экранов навигируется без `popUpTo` — `restoreState` восстанавливает чужой back stack.

### Аргументы

```kotlin
data object Detail : Screen("detail/{id}") {
    fun createRoute(id: String) = "detail/$id"
    const val ARG = "id"
}
composable(
    Screen.Detail.route,
    arguments = listOf(navArgument(Screen.Detail.ARG) { type = NavType.StringType })
) { DetailScreen(id = it.arguments?.getString(Screen.Detail.ARG) ?: return@composable) }
```

---

## Jetpack Compose — особенности

```kotlin
// ExposedDropdownMenuBox — обязательно:
modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)

// flatMapLatest — аннотация обязательна:
@OptIn(ExperimentalCoroutinesApi::class)

// LinearProgressIndicator — новый синтаксис Material3:
LinearProgressIndicator(progress = { value })  // правильно
LinearProgressIndicator(progress = value)       // устаревшее
```

### Клавиатура (windowSoftInputMode)

```xml
<!-- AndroidManifest.xml — TopAppBar остаётся на месте при открытии клавиатуры -->
<activity android:windowSoftInputMode="adjustResize">
```

Работает только с `verticalScroll` или `LazyColumn`.

---

## Шагомер — TYPE_STEP_COUNTER

`Sensor.TYPE_STEP_COUNTER` считает непрерывно с перезагрузки, возвращает абсолютное значение, сбрасывается при перезагрузке телефона.

```kotlin
// Правильный паттерн — база на начало дня:
val baseline = prefs.getInt("last_steps_$yesterday", stepsFromBoot)
todaySteps = stepsFromBoot - baseline
prefs.edit().putInt("last_steps_$today", stepsFromBoot).apply()

// Перезагрузка телефона:
if (stepsFromBoot < baseline) { baseline = 0; todaySteps = stepsFromBoot }

// init — показывать сохранённое значение до первого срабатывания сенсора:
init {
    val base = prefs.getInt("base_$today", -1)
    if (base != -1) todaySteps = (prefs.getInt("last_steps_$today", base) - base).coerceAtLeast(0)
}
```

---

## Сборка APK

```powershell
# Обязательно задать JAVA_HOME перед каждым вызовом Gradle:
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Проверка Java:
java -version
where java
gradlew --version

# Сборка релиза:
.\gradlew assembleRelease

# Если Studio открыта и блокирует lint:
.\gradlew assembleRelease -x lintVitalRelease

# APK:
app/build/outputs/apk/release/app-release.apk
```

**Важно:** кнопка Run в Studio добавляет `testOnly=true` — такой APK нельзя установить вручную. Всегда собирать через Gradle.

---

## Подписание APK и Play Protect

```bash
# Создать keystore:
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
& "$env:JAVA_HOME\bin\keytool" -genkeypair -v -keystore "$env:USERPROFILE\.android\app.jks" `
  -alias app -keyalg RSA -keysize 2048 -validity 10000
```

```properties
# local.properties (в .gitignore):
KEY_STORE=C\:\\Users\\User\\.android\\app.jks
KEY_ALIAS=app
STORE_PASS=пароль
KEY_PASS=пароль
```

```kotlin
// build.gradle.kts — подписывать ОБА варианта:
signingConfigs { create("release") { storeFile = ...; ... } }
buildTypes {
    release { signingConfig = signingConfigs.getByName("release") }
    debug   { signingConfig = signingConfigs.getByName("release") }
}
```

| Ситуация | Play Protect |
|----------|-------------|
| Первая установка (sideload) | Блокирует → отключить PP |
| Обновление (тот же пакет + подпись) | Пропускает |
| In-app обновление через FileProvider | Пропускает |

---

## FileProvider для установки APK

```xml
<!-- res/xml/file_paths.xml — path="." охватывает все пути: -->
<paths>
    <external-files-path name="external_files" path="." />
    <cache-path name="cache" path="." />
</paths>
```

```kotlin
// Проверка разрешения (Android 8+):
if (!ctx.packageManager.canRequestPackageInstalls()) {
    // открыть Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
}
```

---

## GitHub Releases — публикация через PowerShell

```powershell
# Кириллица в теле релиза — кодировать явно:
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json))
Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $bodyBytes

# Загрузка APK:
$apkBytes = [System.IO.File]::ReadAllBytes("app\build\outputs\apk\release\app-release.apk")
Invoke-RestMethod -Uri "$uploadUrl?name=app-1.0.apk" -Method Post `
    -Headers @{ Authorization="Bearer $token"; "Content-Type"="application/vnd.android.package-archive" } `
    -Body $apkBytes

# Git push через PAT:
git push "https://TOKEN@github.com/owner/repo.git" main
git remote set-url origin https://github.com/owner/repo.git  # убрать токен после пуша
```

---

---

## Оформление релизов GitHub — обязательный формат

> Нарушения: v1.10-v1.12 (нет заголовка), v1.14 (английский текст), v1.15 (префикс «MyFIT», кодировка). Исправлено.

### Имя релиза (name)

```
v{X.Y} — {Краткое название на русском}
```

**Правильно:** `v1.9 — Дата рождения, автоматический возраст`
**Неправильно:** `v1.14` (нет заголовка), `MyFIT v1.15` (лишний префикс), `v1.14: timer sound` (английский)

### Тело релиза (body)

```markdown
## Что нового в v{X.Y}

### {Раздел}
- пункт 1
- пункт 2
```

### Чеклист перед публикацией

- [ ] Имя: v{X.Y} — {Название на русском} (без «MyFIT», без английского)
- [ ] Тело: начинается с `## Что нового в v{X.Y}`
- [ ] Весь текст на русском
- [ ] Кодировка: `[System.Text.Encoding]::UTF8.GetBytes($json)` при вызове GitHub API
- [ ] Никогда не передавать кириллический текст через `-Body $string` — только через байты

### PowerShell — безопасная отправка

```powershell
$json = @{ name = $name; body = $body } | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
Invoke-RestMethod -Method Patch -Uri $url -Headers $headers -Body $bytes -ContentType 'application/json'
```

---

## Чеклист — новый проект с Gemini

- [ ] Переключение модели в UI
- [ ] Обработка 503 и 429 с сообщением пользователю
- [ ] API ключ в EncryptedSharedPreferences
- [ ] Regex для парсинга JSON использует тройные кавычки `"""`
- [ ] Системный промпт: "только из текущего сообщения"
- [ ] Room миграции добавлены в цепочку в Application
- [ ] Дата в ViewModel через MutableStateFlow
- [ ] Вся навигация drawer использует единый popUpTo паттерн
- [ ] `LaunchedEffect(Unit) { vm.refreshDate() }` на экранах с фильтром по дате

## Чеклист — перед релизом

**Читать этот чеклист перед КАЖДЫМ релизом, не полагаться на память.**

- [ ] Kotlin-файлы с кириллицей: нет иероглифов в UI
- [ ] Нет файлов-дублей в неправильных пакетных папках
- [ ] Release keystore задан в `local.properties`, файл в `.gitignore`
- [ ] Debug и Release buildTypes — одинаковый signingConfig
- [ ] `versionCode` увеличен, **`versionName` обновлён и совпадает и с `versionCode`, и с тегом релиза на GitHub** (например `versionCode = 18` ⇔ `versionName = "1.18"` ⇔ тег `v1.18`). Несовпадение `versionName` ломает проверку обновлений — приложение будет вечно считать себя устаревшим (баг в v1.18: `versionCode` подняли, `versionName` забыли).
- [ ] Сборка через `.\gradlew assembleRelease` (не через Studio)
- [ ] `git add` + `git commit` + `git push` — часть релиза, а не отдельный шаг. Публикация APK на GitHub Releases без пуша соответствующих коммитов оставляет репозиторий в состоянии, не совпадающем с выпущенным релизом.
- [ ] APK загружен на GitHub Releases
- [ ] Первая установка на устройство — Play Protect отключить

### Обновление уже опубликованного релиза (без бампа версии)

Если правки вносятся в уже вышедшую версию (например, багфиксы сразу после релиза, до того как пользователи массово обновились) — версию не бампать, `versionCode`/`versionName` оставить как есть, пересобрать APK и заменить файл в существующем GitHub Release (и при необходимости обновить текст релиза), а не создавать новый тег.