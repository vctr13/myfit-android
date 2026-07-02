package com.example.myfit.data.db

import com.example.myfit.data.db.entity.WorkoutTemplate

object DefaultWorkoutTemplates {
    val list = listOf(
        // ── Домашние ──────────────────────────────────────────────────────────
        WorkoutTemplate(name = "Полное тело", description = "Пн / Ср / Пт · 45–60 мин", mode = "home",
            exercise_names = "Подтягивания,Отжимания,Приседания,Обратные отжимания от стула,Планка прямая",
            is_builtin = true),
        WorkoutTemplate(name = "Пресс + Кор", description = "Ежедневно · 15–20 мин", mode = "home",
            exercise_names = "Вакуум,Планка прямая,Боковая планка,Подъём коленей в висе",
            is_builtin = true),
        WorkoutTemplate(name = "Верх тела", description = "Грудь, спина, руки · 30–40 мин", mode = "home",
            exercise_names = "Австралийские подтягивания,Отжимания,Обратные отжимания от стула",
            is_builtin = true),
        WorkoutTemplate(name = "Ноги + Ягодицы", description = "Нижняя часть · 30 мин", mode = "home",
            exercise_names = "Приседания,Выпады,Ягодичный мост",
            is_builtin = true),
        WorkoutTemplate(name = "Сессия A", description = "Ноги, грудь, спина, пресс · 50–60 мин", mode = "home",
            exercise_names = "Болгарский сплит-присед,Отжимания,Подтягивания,Ягодичный мост на одной ноге,Подъём ног в висе,Планка прямая",
            is_builtin = true),
        WorkoutTemplate(name = "Сессия B", description = "Ноги, грудь, спина, икры · 50–60 мин", mode = "home",
            exercise_names = "Воздушный присед,Отжимания,Подтягивания обратным хватом,Нордические сгибания (негатив),Лодочка,Подъём на носки",
            is_builtin = true),
        // ── Зальные ───────────────────────────────────────────────────────────
        WorkoutTemplate(name = "Фулл боди", description = "Пн / Ср / Пт · 60–75 мин", mode = "gym",
            exercise_names = "Жим штанги лёжа,Тяга верхнего блока,Приседания со штангой,Жим гантелей сидя,Подъём гантелей на бицепс",
            is_builtin = true),
        WorkoutTemplate(name = "Грудь + Трицепс", description = "Вт · 45 мин", mode = "gym",
            exercise_names = "Жим гантелей лёжа,Жим штанги лёжа,Французский жим",
            is_builtin = true),
        WorkoutTemplate(name = "Спина + Бицепс", description = "Чт · 45–55 мин", mode = "gym",
            exercise_names = "Тяга верхнего блока,Тяга гантели в наклоне,Становая тяга,Подъём гантелей на бицепс",
            is_builtin = true),
        WorkoutTemplate(name = "Ноги + Поясница", description = "Сб · 60 мин", mode = "gym",
            exercise_names = "Приседания со штангой,Жим ногами,Становая тяга,Гиперэкстензия",
            is_builtin = true),
    )
}
