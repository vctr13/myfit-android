package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myfit.data.db.entity.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Query("SELECT * FROM workout_template ORDER BY is_builtin DESC, id ASC")
    fun getAll(): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_template WHERE id = :id")
    suspend fun getById(id: Int): WorkoutTemplate?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(templates: List<WorkoutTemplate>)

    @Insert
    suspend fun insert(template: WorkoutTemplate): Long

    @Update
    suspend fun update(template: WorkoutTemplate)

    @Delete
    suspend fun delete(template: WorkoutTemplate)

    @Query("SELECT COUNT(*) FROM workout_template WHERE is_builtin = 1")
    suspend fun countBuiltin(): Int
}
