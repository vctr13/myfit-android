# Информация о графиках веса и талии

## 1. Где находится код графиков
Код отрисовки графиков находится в файле `app/src/main/java/com/example/myfit/ui/home/HomeScreen.kt`.

*   **Функция отрисовки талии**: `WaistGraph(entries: List<WeightEntry>)` (использует `Canvas` для рисования).
*   **Компонент интерфейса**: Секция "Динамика талии" внутри `WeightCard`.
*   **Цвет линии талии**: `MaterialTheme.colorScheme.secondary`.

## 2. Где и как хранятся данные
Данные для обоих графиков (вес и талия) хранятся в **одной таблице** базы данных SQLite (через Room).

*   **Таблица**: `weight_entry`
*   **Класс сущности**: `WeightEntry` (находится в `app/src/main/java/com/example/myfit/data/db/entity/WeightEntry.kt`).
*   **Структура записи**:
    *   `date`: дата замера (уникальный индекс).
    *   `weight_kg`: значение веса.
    *   `waist_cm`: значение обхвата талии (может быть `null`).

### Логика обработки данных (HomeViewModel)
Данные запрашиваются общим списком, а затем фильтруются:
*   **Для веса**: `entries.filter { it.weight_kg > 0f ... }`
*   **Для талии**: `entries.filter { it.waist_cm != null ... }`
