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

---

## Подписание APK и Play Protect (Xiaomi HyperOS)

### Проблема

На Xiaomi с HyperOS (и некоторых других устройствах) при установке стороннего APK Google Play Protect блокирует установку даже после нажатия "Всё равно установить". Сообщение: **"Приложение не установлено"**.

### Как правильно организовать подписание

**1. Всегда создавай release keystore до первого публичного релиза:**

```bash
# Выполнять в PowerShell с JAVA_HOME → Android Studio JBR
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
& "$env:JAVA_HOME\bin\keytool" -genkeypair -v `
  -keystore "$env:USERPROFILE\.android\myapp-release.jks" `
  -alias myapp -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass "YourPassword" -keypass "YourPassword"
```

**2. Храни параметры в `local.properties` (он в .gitignore — никогда не попадёт в репо):**

```properties
VFIT_KEY_STORE=C\:\\Users\\User\\.android\\myapp-release.jks
VFIT_KEY_ALIAS=myapp
VFIT_STORE_PASS=YourPassword
VFIT_KEY_PASS=YourPassword
```

**3. В `build.gradle.kts` читай из `local.properties` и подписывай ОБА варианта сборки:**

```kotlin
import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    signingConfigs {
        create("release") {
            storeFile     = file(localProps.getProperty("VFIT_KEY_STORE", ""))
            storePassword = localProps.getProperty("VFIT_STORE_PASS", "")
            keyAlias      = localProps.getProperty("VFIT_KEY_ALIAS", "")
            keyPassword   = localProps.getProperty("VFIT_KEY_PASS", "")
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.getByName("release") }
        debug   { signingConfig = signingConfigs.getByName("release") }  // важно для тестирования
    }
}
```

Если debug подписан дефолтным ключом, а release — своим, обновление не установится (несовместимые подписи).

### Поведение Play Protect: первая установка vs обновление

| Ситуация | Play Protect | Что делать |
|----------|-------------|------------|
| Первая установка нового APK (sideload) | **Блокирует** даже с release-ключом | Отключить Play Protect на время установки |
| Обновление уже установленного приложения (тот же пакет + та же подпись) | **Пропускает** | Ничего делать не нужно |
| In-app обновление (скачали APK внутри приложения, установили через FileProvider) | **Пропускает** при совпадении подписи | Работает без отключения PP |

**Вывод:** для первого релиза пользователям Xiaomi нужно один раз отключить Play Protect. Все последующие обновления — через in-app механизм — проходят автоматически.

---

## FileProvider для установки APK

### Проблема

`FileProvider` бросает `IllegalArgumentException: Failed to find configured root` если путь в `file_paths.xml` не совпадает с реальным путём файла.

Путь `/storage/emulated/0/Android/data/com.example.myapp/files/Downloads/` на разных устройствах/версиях Android может быть:
- `Download/` (без 's') на некоторых эмуляторах
- `files/` (корень external-files-path) без подпапки

### Правильная конфигурация

```xml
<!-- res/xml/file_paths.xml -->
<!-- path="." охватывает ВСЕ пути внутри директории — безопасно и надёжно -->
<paths>
    <external-files-path name="external_files" path="." />
    <cache-path name="cache" path="." />
    <external-cache-path name="external_cache" path="." />
</paths>
```

### Разрешения в манифесте

```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### Проверка разрешения перед установкой (Android 8+)

```kotlin
fun downloadAndInstall(release: ReleaseInfo) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !ctx.packageManager.canRequestPackageInstalls()) {
        state = UpdateState.NeedsPermission  // показать диалог
        return
    }
    // продолжать скачивание
}

// Открыть настройки разрешения:
val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
    Uri.parse("package:${ctx.packageName}"))
ctx.startActivity(intent)
```

---

## Сборка APK через Gradle (не Android Studio)

### Проблема: флаг testOnly

Android Studio при нажатии кнопки Run добавляет флаг `testOnly=true` в манифест APK. Такой APK **нельзя установить вручную** — Android отклоняет его с ошибкой "Приложение не установлено".

Симптом: `.\gradlew assembleDebug` после запуска из Studio выдаёт "39 tasks up-to-date" и копирует тот же кэшированный APK с флагом testOnly.

### Решение

Всегда делать `clean` перед финальной сборкой APK для распространения:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew clean assembleRelease
```

Если Android Studio открыта и держит lock на папку `build`, `clean` упадёт. Два варианта:
1. Закрыть Studio → запустить `clean assembleRelease`
2. Не закрывать Studio → запустить `assembleRelease -x lintVitalRelease` (пропускает lint, который блокируется Studio)

```powershell
.\gradlew assembleRelease -x lintVitalRelease
```

### Где лежит готовый APK

```
app/build/outputs/apk/release/app-release.apk
```

---

## GitHub Releases — публикация APK через PowerShell

### Проблема с кириллицей в теле релиза

PowerShell 5.1 при передаче строк в `Invoke-WebRequest`/`Invoke-RestMethod` кодирует тело в Windows-1251. GitHub API видит `?????` вместо русских букв.

### Правильный способ

```powershell
$token = "ghp_ваш_токен"
$headers = @{
    "Authorization"       = "Bearer $token"
    "Accept"              = "application/vnd.github+json"
    "X-GitHub-Api-Version"= "2022-11-28"
    "Content-Type"        = "application/json; charset=utf-8"
}

# Принудительно кодировать тело в UTF-8:
$bodyObj = @{ tag_name = "v1.0"; name = "v1.0 — Первый релиз"; body = "Описание на русском" }
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes(($bodyObj | ConvertTo-Json))

$release = Invoke-RestMethod -Uri "https://api.github.com/repos/owner/repo/releases" `
    -Method Post -Headers $headers -Body $bodyBytes
```

### Загрузка APK как asset

```powershell
$apkPath  = "app\build\outputs\apk\release\app-release.apk"
$uploadUrl = $release.upload_url -replace '\{.*\}', ''  # убрать {?name,label}
$uploadUrl = "$uploadUrl?name=myapp-1.0.apk"

$apkHeaders = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/vnd.android.package-archive"
}
$apkBytes = [System.IO.File]::ReadAllBytes($apkPath)
$result = Invoke-RestMethod -Uri $uploadUrl -Method Post -Headers $apkHeaders -Body $apkBytes
Write-Host $result.browser_download_url
```

### Аутентификация Git через PAT

GitHub больше не принимает пароли. Используй PAT в URL (временно):

```powershell
$remote = "https://ТОКЕН@github.com/owner/repo.git"
git push $remote master
git push $remote v1.0
# После успешного пуша — не оставляй токен в remote URL:
git remote set-url origin https://github.com/owner/repo.git
```

---

## Клавиатура в Jetpack Compose (windowSoftInputMode)

### Проблема

По умолчанию `windowSoftInputMode = adjustPan` — при открытии клавиатуры весь экран сдвигается вверх вместе с TopAppBar. В Compose это выглядит как "экран уехал".

### Решение

В `AndroidManifest.xml` для MainActivity:

```xml
<activity
    android:name=".MainActivity"
    android:windowSoftInputMode="adjustResize">
```

`adjustResize` изменяет размер контента (Scaffold/Column), а не сдвигает весь экран. TopAppBar остаётся на месте, скроллится только список сообщений.

**Важно:** работает корректно только когда контент обёрнут в `verticalScroll` или `LazyColumn`. Если контент фиксированной высоты без скролла — клавиатура перекроет нижнюю часть.

---

## Шагомер — TYPE_STEP_COUNTER

### Как работает сенсор

`Sensor.TYPE_STEP_COUNTER` — аппаратный счётчик шагов. Он:
- Считает **непрерывно** с момента последней перезагрузки телефона (даже когда приложение не запущено)
- Возвращает **абсолютное** значение (например, 48 320 шагов с момента включения)
- **Сбрасывается в 0 при перезагрузке** телефона

Приложение не считает шаги само — оно только читает накопленное значение.

### Правильный паттерн подсчёта шагов за день

```kotlin
// SharedPreferences: "base_YYYY-MM-DD" — значение счётчика на начало дня
// SharedPreferences: "last_steps_YYYY-MM-DD" — последнее известное значение за день

// При первом чтении за сегодня:
val yesterdayLast = prefs.getInt("last_steps_$yesterday", -1)
val baseline = if (yesterdayLast in 1..stepsFromBoot) yesterdayLast else stepsFromBoot
prefs.edit().putInt("base_$today", baseline).apply()

// При каждом чтении:
todaySteps = stepsFromBoot - baseline

// Всегда сохранять последнее значение (для завтрашней базы):
prefs.edit().putInt("last_steps_$today", stepsFromBoot).apply()

// Обработка перезагрузки телефона:
if (stepsFromBoot < baseline) {
    prefs.edit().putInt("base_$today", 0).apply()
    todaySteps = stepsFromBoot
}
```

### Почему шагомер показывает 0 при первом открытии

Если не загружать сохранённое значение в `init` — до первого срабатывания сенсора показывается 0.

```kotlin
init {
    // Сразу показать сохранённое значение, не ждать сенсора
    val today = LocalDate.now().toString()
    val base = prefs.getInt("base_$today", -1)
    if (base != -1) {
        val lastKnown = prefs.getInt("last_steps_$today", base)
        todaySteps = (lastKnown - base).coerceAtLeast(0)
    }
}
```

### Ограничение первого дня установки

При первой установке в середине дня приложение не может восстановить шаги до установки (нет вчерашней базы). Это нормально — со второго дня работает корректно.

---

## Чеклист перед выпуском релиза Android

- [ ] Release keystore создан и задан в `local.properties`
- [ ] `local.properties` добавлен в `.gitignore`
- [ ] Debug и Release buildTypes используют одинаковый signingConfig
- [ ] `versionCode` увеличен, `versionName` обновлён в `build.gradle.kts`
- [ ] Сборка через `.\gradlew clean assembleRelease` (не через кнопку Run в Studio)
- [ ] APK загружен на GitHub Releases с именем `appname-X.Y.apk`
- [ ] URL в UpdateChecker указывает на `releases/latest` API (не на конкретный ID релиза)
- [ ] Первая установка на устройство — Play Protect временно отключить
- [ ] Все последующие обновления через in-app механизм работают без отключения PP
