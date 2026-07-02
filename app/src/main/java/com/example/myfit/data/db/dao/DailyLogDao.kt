package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myfit.data.db.entity.DailyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_log WHERE date = :date")
    fun getByDate(date: String): Flow<DailyLog?>

    @Query("SELECT * FROM daily_log WHERE date = :date")
    suspend fun getByDateOnce(date: String): DailyLog?

    @Query("SELECT * FROM daily_log ORDER BY date DESC")
    fun getAll(): Flow<List<DailyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyLog)

    @Update
    suspend fun update(log: DailyLog)

    @Query("UPDATE daily_log SET steps = :steps, calories_burned_steps = :caloriesBurned, updated_at = :now WHERE date = :date")
    suspend fun updateSteps(date: String, steps: Int, caloriesBurned: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_log SET water_ml = :waterMl, updated_at = :now WHERE date = :date")
    suspend fun updateWater(date: String, waterMl: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_log SET calories_burned_workout = :calories, updated_at = :now WHERE date = :date")
    suspend fun updateWorkoutCalories(date: String, calories: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_log SET weight_kg = :weightKg, updated_at = :now WHERE date = :date")
    suspend fun updateWeight(date: String, weightKg: Float, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_log SET is_training_day = :isTraining, updated_at = :now WHERE date = :date")
    suspend fun updateTrainingDay(date: String, isTraining: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM daily_log")
    suspend fun deleteAll()

    @Query("DELETE FROM daily_log WHERE date < :cutoff")
    suspend fun deleteOlderThan(cutoff: String)
}
