package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myfit.data.db.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercise ORDER BY name ASC")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise ORDER BY name ASC")
    suspend fun getAllOnce(): List<Exercise>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Int): Exercise?

    @Query("SELECT * FROM exercise WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<Exercise>

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("DELETE FROM exercise WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM exercise WHERE id = :id AND is_custom = 1")
    suspend fun deleteCustomById(id: Int)

    @Query("SELECT * FROM exercise WHERE training_mode = 'both' OR training_mode = :mode ORDER BY muscle_groups ASC, name ASC")
    fun getByMode(mode: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE training_mode = 'both' OR training_mode = :mode ORDER BY muscle_groups ASC, name ASC")
    suspend fun getByModeOnce(mode: String): List<Exercise>
}
