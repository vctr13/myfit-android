package com.example.myfit.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myfit.data.db.dao.ChatMessageDao
import com.example.myfit.data.db.dao.DailyLogDao
import com.example.myfit.data.db.dao.ExerciseDao
import com.example.myfit.data.db.dao.FoodEntryDao
import com.example.myfit.data.db.dao.ProductDao
import com.example.myfit.data.db.dao.UserFactsDao
import com.example.myfit.data.db.dao.UserProfileDao
import com.example.myfit.data.db.dao.WeightDao
import com.example.myfit.data.db.dao.WorkoutDayDao
import com.example.myfit.data.db.dao.WorkoutEntryDao
import com.example.myfit.data.db.dao.WorkoutTemplateDao
import com.example.myfit.data.db.entity.ChatMessage
import com.example.myfit.data.db.entity.DailyLog
import com.example.myfit.data.db.entity.Exercise
import com.example.myfit.data.db.entity.FoodEntry
import com.example.myfit.data.db.entity.Product
import com.example.myfit.data.db.entity.UserFacts
import com.example.myfit.data.db.entity.UserProfile
import com.example.myfit.data.db.entity.WeightEntry
import com.example.myfit.data.db.entity.WorkoutDay
import com.example.myfit.data.db.entity.WorkoutEntry
import com.example.myfit.data.db.entity.WorkoutTemplate

@Database(
    entities = [
        UserProfile::class,
        UserFacts::class,
        DailyLog::class,
        FoodEntry::class,
        Exercise::class,
        WorkoutDay::class,
        WorkoutEntry::class,
        WorkoutTemplate::class,
        Product::class,
        ChatMessage::class,
        WeightEntry::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userFactsDao(): UserFactsDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun workoutEntryDao(): WorkoutEntryDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun productDao(): ProductDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun weightDao(): WeightDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_entry ADD COLUMN water_ml REAL NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS weight_entry " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "weight_kg REAL NOT NULL, " +
                    "created_at INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_weight_entry_date ON weight_entry(date)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN training_mode TEXT NOT NULL DEFAULT 'both'")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN current_weight_kg REAL NOT NULL DEFAULT 0")
                db.execSQL("UPDATE user_profile SET current_weight_kg = weight_kg WHERE current_weight_kg = 0")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_log ADD COLUMN is_training_day INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS workout_template (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        mode TEXT NOT NULL DEFAULT 'both',
                        exercise_names TEXT NOT NULL DEFAULT '',
                        is_builtin INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = OFF")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exercise_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        muscle_groups TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        image_url TEXT,
                        training_mode TEXT NOT NULL DEFAULT 'both',
                        is_custom INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO exercise_new (id, name, muscle_groups, description, image_url, training_mode, is_custom)
                    SELECT id, name, muscle_groups, COALESCE(description_level1, ''), NULL, training_mode, is_custom
                    FROM exercise
                """.trimIndent())
                db.execSQL("DROP TABLE exercise")
                db.execSQL("ALTER TABLE exercise_new RENAME TO exercise")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_exercise_name ON exercise(name)")
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }
    }
}
