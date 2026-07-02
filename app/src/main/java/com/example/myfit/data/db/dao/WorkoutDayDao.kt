package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myfit.data.db.entity.WorkoutDay
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDayDao {

    @Query("SELECT * FROM workout_day ORDER BY date DESC")
    fun getAll(): Flow<List<WorkoutDay>>

    @Query("SELECT * FROM workout_day WHERE date = :date ORDER BY created_at ASC")
    fun getByDateFlow(date: String): Flow<List<WorkoutDay>>

    @Query("SELECT * FROM workout_day WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): WorkoutDay?

    @Query("SELECT * FROM workout_day WHERE id = :id")
    suspend fun getById(id: Int): WorkoutDay?

    @Query("SELECT * FROM workout_day ORDER BY created_at ASC LIMIT 1")
    suspend fun getFirstWorkout(): WorkoutDay?

    @Query("SELECT * FROM workout_day ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestWorkout(): WorkoutDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(day: WorkoutDay): Long

    @Update
    suspend fun update(day: WorkoutDay)

    @Delete
    suspend fun delete(day: WorkoutDay)

    @Query("DELETE FROM workout_day WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM workout_day")
    suspend fun deleteAll()

    @Query("DELETE FROM workout_day WHERE date < :cutoff")
    suspend fun deleteOlderThan(cutoff: String)

    @Query("SELECT * FROM workout_day WHERE is_completed = 1 ORDER BY date DESC, created_at DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<WorkoutDay>>

    @Query("SELECT COALESCE(SUM(calories_burned), 0) FROM workout_day WHERE date = :date AND is_completed = 1")
    fun getTotalCaloriesFlow(date: String): Flow<Int>
}
