package com.example.myfit

import android.app.Application
import androidx.room.Room
import com.example.myfit.data.db.AppDatabase
import com.example.myfit.data.db.DefaultExercises
import com.example.myfit.data.db.DefaultWorkoutTemplates
import com.example.myfit.data.db.entity.WeightEntry
import com.example.myfit.data.prefs.SecurePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFitApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securePrefs: SecurePrefs
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "myfit.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12
            )
            // Откат на более старую версию приложения (кнопка «Откатить» в диалоге обновлений)
            // оставляет на диске базу данных более новой схемы, чем знает старый код.
            // Без этого Room аварийно завершает приложение при каждом запуске сразу
            // после отката. Явных миграций назад нет — при откате безопаснее пересоздать
            // локальные таблицы, чем крашиться в бесконечном цикле.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
        securePrefs = SecurePrefs(this)

        CoroutineScope(Dispatchers.IO).launch {
            database.exerciseDao().insertAll(DefaultExercises.list)
            if (database.workoutTemplateDao().countBuiltin() == 0) {
                database.workoutTemplateDao().insertAll(DefaultWorkoutTemplates.list)
            }
            ensureProfileAnchor()
        }
    }

    // Однократно создаёт точку отсчёта графика на дату первого релиза
    private suspend fun ensureProfileAnchor() {
        val anchorDate = "2026-07-02"
        if (database.weightDao().getByDate(anchorDate) != null) return
        val profile = database.userProfileDao().getProfileOnce() ?: return
        database.weightDao().insert(
            WeightEntry(date = anchorDate, weight_kg = profile.weight_kg, waist_cm = profile.waist_cm)
        )
    }

    companion object {
        fun from(app: Application): MyFitApp = app as MyFitApp
    }
}