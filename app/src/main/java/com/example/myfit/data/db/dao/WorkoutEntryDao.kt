package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.myfit.data.db.entity.WorkoutEntry
import com.example.myfit.data.db.model.WorkoutEntryWithExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutEntryDao {

    @Query("SELECT * FROM workout_entry WHERE workout_day_id = :workoutDayId ORDER BY sort_order ASC")
    fun getByWorkoutDayId(workoutDayId: Int): Flow<List<WorkoutEntry>>

    @Transaction
    @Query("SELECT * FROM workout_entry WHERE workout_day_id = :workoutDayId ORDER BY sort_order ASC")
    fun getWithExerciseByWorkoutDayId(workoutDayId: Int): Flow<List<WorkoutEntryWithExercise>>

    @Transaction
    @Query("SELECT * FROM workout_entry WHERE workout_day_id = :workoutDayId ORDER BY sort_order ASC")
    suspend fun getWithExerciseByWorkoutDayIdOnce(workoutDayId: Int): List<WorkoutEntryWithExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkoutEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WorkoutEntry>)

    @Update
    suspend fun update(entry: WorkoutEntry)

    @Delete
    suspend fun delete(entry: WorkoutEntry)

    @Query("DELETE FROM workout_entry WHERE workout_day_id = :workoutDayId")
    suspend fun deleteByWorkoutDayId(workoutDayId: Int)

    @Query("DELETE FROM workout_entry")
    suspend fun deleteAll()

    @Query("SELECT difficulty_level FROM workout_entry WHERE exercise_id = :exerciseId ORDER BY id DESC LIMIT 1")
    suspend fun getLastDifficultyForExercise(exerciseId: Int): Int?

    @Query("SELECT * FROM workout_entry WHERE exercise_id = :exerciseId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentEntriesForExercise(exerciseId: Int, limit: Int): List<WorkoutEntry>

    @Query("DELETE FROM workout_entry WHERE workout_day_id NOT IN (SELECT id FROM workout_day)")
    suspend fun deleteOrphans()
}
