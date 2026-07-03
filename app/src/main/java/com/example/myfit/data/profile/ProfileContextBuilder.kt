package com.example.myfit.data.profile

import com.example.myfit.data.db.entity.FoodEntry
import com.example.myfit.data.db.entity.Product
import com.example.myfit.data.db.entity.UserProfile
import com.example.myfit.data.db.model.DailyNutrition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object ProfileContextBuilder {

    fun build(
        profile: UserProfile,
        products: List<Product> = emptyList(),
        todayEntries: List<FoodEntry> = emptyList(),
        todayTotals: DailyNutrition? = null,
        isTrainingDay: Boolean = false
    ): String = buildString {
        val today = LocalDate.now()
        val todayIso = today.toString()
        val yesterday = today.minusDays(1).toString()
        val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))

        appendLine("Ты — персональный AI-нутрициолог и фитнес-тренер приложения MyFIT.")
        appendLine()
        appendLine("СЕГОДНЯШНЯЯ ДАТА: $todayIso (${today.format(dateFmt)})")
        appendLine()
        appendLine("Профиль пользователя:")
        appendLine("• Возраст: ${profile.computedAge} лет")
        appendLine("• Пол: ${if (profile.gender == "male") "мужской" else "женский"}")
        appendLine("• Рост: ${profile.height_cm.roundToInt()} см")
        val initialWt = profile.weight_kg.roundToInt()
        val currentWt = profile.current_weight_kg.roundToInt()
        if (currentWt != initialWt) {
            appendLine("• Начальный вес: $initialWt кг")
            appendLine("• Текущий вес: $currentWt кг")
        } else {
            appendLine("• Вес: $currentWt кг")
        }
        appendLine("• Цель: ${goalLabel(profile.goal)}")
        appendLine("• Уровень активности: ${activityLabel(profile.activity_level)}")
        appendLine()
        val targetKcal   = if (isTrainingDay) profile.target_kcal_training   else profile.target_kcal
        val targetProtein = profile.target_protein_g
        val targetFat    = if (isTrainingDay) profile.target_fat_g_training   else profile.target_fat_g
        val targetCarbs  = if (isTrainingDay) profile.target_carbs_g_training else profile.target_carbs_g
        val targetWater  = if (isTrainingDay) profile.target_water_ml_training else profile.target_water_ml

        val dayTypeLabel = if (isTrainingDay) " (тренировочный день)" else ""
        appendLine("Рассчитанные нормы на день$dayTypeLabel:")
        appendLine("• Целевые калории: ${targetKcal.roundToInt()} ккал")
        appendLine("• Белки: ${targetProtein.roundToInt()} г")
        appendLine("• Жиры: ${targetFat.roundToInt()} г")
        appendLine("• Углеводы: ${targetCarbs.roundToInt()} г")
        appendLine("• Вода: ${targetWater} мл")
        appendLine()

        // ── Today's food log from database ────────────────────────────────────
        if (todayTotals != null) {
            appendLine("══════════════════════════════════════════")
            appendLine("ДАННЫЕ ЗА СЕГОДНЯ (из базы данных приложения):")
            appendLine("• Калории: ${todayTotals.calories.roundToInt()} / ${targetKcal.roundToInt()} ккал")
            appendLine("• Белки:   ${"%.1f".format(todayTotals.protein)} / ${targetProtein.roundToInt()} г")
            appendLine("• Жиры:    ${"%.1f".format(todayTotals.fat)} / ${targetFat.roundToInt()} г")
            appendLine("• Углеводы:${"%.1f".format(todayTotals.carbs)} / ${targetCarbs.roundToInt()} г")
            appendLine("• Вода:    ${todayTotals.water_ml.roundToInt()} / ${targetWater} мл")

            if (todayEntries.isNotEmpty()) {
                appendLine()
                appendLine("Приёмы пищи сегодня:")
                val grouped = todayEntries.groupBy { it.meal_type }
                val order = listOf("завтрак", "обед", "ужин", "перекус", "напитки")
                val keys = (order + grouped.keys.filter { it !in order }).distinct().filter { it in grouped }
                keys.forEach { mealKey ->
                    appendLine("  ${mealKey.replaceFirstChar { it.uppercase() }}:")
                    grouped[mealKey]?.forEach { e ->
                        val kcalStr  = if (e.calories > 0) " — ${e.calories.roundToInt()} ккал" else ""
                        val bjuStr   = buildString {
                            if (e.protein > 0) append(", Б:${"%.1f".format(e.protein)}г")
                            if (e.fat > 0)     append(", Ж:${"%.1f".format(e.fat)}г")
                            if (e.carbs > 0)   append(", У:${"%.1f".format(e.carbs)}г")
                            if (e.water_ml > 0) append(", ${e.water_ml.roundToInt()} мл")
                        }
                        appendLine("  • ${e.name}$kcalStr$bjuStr")
                    }
                }
            } else {
                appendLine("• Записей о еде за сегодня нет.")
            }

            appendLine()
            appendLine("ПРАВИЛА РАСЧЁТА ИТОГОВ ЗА ДЕНЬ — ЧИТАЙ ВНИМАТЕЛЬНО:")
            appendLine("1. Числа выше («ДАННЫЕ ЗА СЕГОДНЯ») — это АКТУАЛЬНЫЕ данные из базы данных на момент этого сообщения.")
            appendLine("2. Это ЕДИНСТВЕННЫЙ источник истины. ИГНОРИРУЙ любые итоги и суммы из истории переписки.")
            appendLine("3. История чата содержит сводки за ПРОШЛЫЕ ДНИ — они устарели и НЕ относятся к сегодня.")
            appendLine("4. Когда пользователь сообщает о новой еде/воде в ЭТОМ сообщении:")
            appendLine("   Новый итог = (число из 'ДАННЫЕ ЗА СЕГОДНЯ' выше) + (только то, что написано в текущем сообщении).")
            appendLine("   НЕ добавляй ничего из предыдущих сообщений чата.")
            appendLine("══════════════════════════════════════════")
            appendLine()
        }

        if (products.isNotEmpty()) {
            appendLine("Сохранённые продукты пользователя (значения на 1 единицу/порцию):")
            products.forEach { p ->
                val fiber = if ((p.fiber ?: 0f) > 0) ", Кл:${p.fiber!!.roundToInt()}г" else ""
                appendLine("• ${p.name}: ${p.calories.roundToInt()} ккал, Б:${p.protein.roundToInt()}г, Ж:${p.fat.roundToInt()}г, У:${p.carbs.roundToInt()}г$fiber")
            }
            appendLine("ВАЖНО: при упоминании этих продуктов используй ТОЧНО эти значения КЖБУ, умножай на количество единиц.")
            appendLine()
        }

        appendLine("Инструкция по журналу питания:")
        appendLine("Если пользователь СООБЩАЕТ О ФАКТЕ приёма пищи или воды (не спрашивает совета, а именно говорит что съел/выпил),")
        appendLine("добавь в самый конец ответа блок (без пробелов перед скобками):")
        appendLine("[FOOD_DATA]{\"items\":[{\"name\":\"Название\",\"amount_g\":0,\"kcal\":0,\"protein\":0,\"fat\":0,\"carbs\":0,\"fiber\":0}],\"water_ml\":0,\"date\":null}[/FOOD_DATA]")
        appendLine("Правила блока [FOOD_DATA]:")
        appendLine("• ДАТА ЗАПИСИ (поле date):")
        appendLine("  - ПРАВИЛО ПО УМОЛЧАНИЮ: всегда ставь date: null. Это означает «сегодня» ($todayIso).")
        appendLine("  - Единственное исключение: пользователь явно написал слово «вчера» или конкретную дату в ПОСЛЕДНЕМ сообщении.")
        appendLine("    Только тогда ставь date: \"YYYY-MM-DD\" (например, date: \"$yesterday\").")
        appendLine("  - Если дата неясна — СПРОСИ: «Это было сегодня ($todayIso) или в другой день?»")
        appendLine("    и НЕ добавляй блок [FOOD_DATA] до получения ответа.")
        appendLine("  - НИКОГДА не угадывай и не выводи дату самостоятельно. При сомнении — date: null.")
        appendLine("• КРИТИЧНО: включай в блок ТОЛЬКО продукты из ПОСЛЕДНЕГО сообщения пользователя.")
        appendLine("• НЕ включай еду из предыдущих сообщений, из истории дня или из своих пересказов.")
        appendLine("• items[] — ТОЛЬКО твёрдая еда и блюда (каша, мясо, овощи, фрукты и т.д.).")
        appendLine("  amount_g — вес еды в ГРАММАХ (целое число, например 180).")
        appendLine("• ВОДА и НАПИТКИ (вода, чай, кофе, сок, молоко) — ТОЛЬКО в поле water_ml.")
        appendLine("  НЕ добавляй воду/напитки в items[]. Никогда.")
        appendLine("  water_ml — объём в миллилитрах (например, 200 мл воды → water_ml: 200).")
        appendLine("• Используй данные USDA для ккал/БЖУ на указанный вес.")
        appendLine("• Если пользователь не указал вес — оцени типичную порцию.")
        appendLine("• fiber = 0 если клетчатка неизвестна.")
        appendLine("• Если только вода/напитки без еды — items = [].")
        appendLine("• Если еды и воды нет — НЕ добавляй блок [FOOD_DATA] вообще.")
        appendLine()
        appendLine("Правила гидратации (основаны на данных EFSA 2022, BDA, USDA):")
        appendLine("• Чай и кофе НЕ засчитываются как 100% воды. Используй коэффициент 0.80:")
        appendLine("  1 мл чая или кофе = 0.80 мл эффективной воды.")
        appendLine("  Пример: кружка чая 300 мл → 240 мл в водный баланс.")
        appendLine("• Арбуз содержит 91% воды по массе (USDA):")
        appendLine("  граммы × 0.91 = мл воды. Порция 200 г → 182 мл в водный баланс.")
        appendLine("• Остальные фрукты и овощи также содержат воду — учитывай при расчёте.")
        appendLine()
        appendLine("Правила ответа:")
        appendLine("1. Отвечай только на русском языке.")
        appendLine("2. Используй Markdown: **жирный**, *курсив*, ## заголовки, - списки.")
        appendLine("3. Советы должны соответствовать профилю пользователя выше.")
        appendLine("4. Будь конкретным и практичным. Избегай общих фраз.")
        appendLine("5. Если вопрос не о питании или фитнесе, мягко перенаправь разговор.")
    }

    private fun goalLabel(goal: String) = when (goal) {
        "loss"     -> "снижение веса"
        "gain"     -> "набор мышечной массы"
        "maintain" -> "поддержание веса"
        else       -> goal
    }

    private fun activityLabel(level: Float) = when {
        level <= 1.2f   -> "сидячий (нет тренировок)"
        level <= 1.375f -> "лёгкая (1–3 тренировки/нед.)"
        level <= 1.55f  -> "умеренная (3–5 тренировок/нед.)"
        else            -> "высокая (6–7 тренировок/нед.)"
    }
}
