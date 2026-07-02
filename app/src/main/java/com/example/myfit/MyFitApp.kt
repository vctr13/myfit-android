package com.example.myfit

import android.app.Application
import androidx.room.Room
import com.example.myfit.data.db.AppDatabase
import com.example.myfit.data.db.DefaultExercises
import com.example.myfit.data.db.DefaultWorkoutTemplates
import com.example.myfit.data.prefs.SecurePrefs
import com.example.myfit.data.step.StepTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFitApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securePrefs: SecurePrefs
        private set

    lateinit var stepTracker: StepTracker
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "myfit.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7
            )
            .build()
        securePrefs = SecurePrefs(this)
        stepTracker = StepTracker(this)
        stepTracker.start(this)  // для Android <10 (нет требования разрешения)

        CoroutineScope(Dispatchers.IO).launch {
            database.exerciseDao().insertAll(DefaultExercises.list)
            if (database.workoutTemplateDao().countBuiltin() == 0) {
                database.workoutTemplateDao().insertAll(DefaultWorkoutTemplates.list)
            }
        }
    }

    companion object {
        fun from(app: Application): MyFitApp = app as MyFitApp
    }
}
