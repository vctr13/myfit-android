package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfit.data.db.entity.WeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntry): Long

    @Query("SELECT * FROM weight_entry ORDER BY date ASC")
    fun getAllFlow(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entry ORDER BY date DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 30): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entry WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): WeightEntry?

    @Query("DELETE FROM weight_entry WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM weight_entry WHERE date < :cutoff")
    suspend fun deleteOlderThan(cutoff: String)

    @Query("DELETE FROM weight_entry")
    suspend fun deleteAll()
}
