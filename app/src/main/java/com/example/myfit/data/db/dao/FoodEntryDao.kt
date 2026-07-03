package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myfit.data.db.entity.FoodEntry
import com.example.myfit.data.db.model.DailyNutrition
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {

    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY created_at ASC")
    fun getByDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY created_at ASC")
    suspend fun getByDateOnce(date: String): List<FoodEntry>

    @Query("SELECT * FROM food_entry WHERE date = :date AND meal_type = :mealType ORDER BY created_at ASC")
    fun getByDateAndMeal(date: String, mealType: String): Flow<List<FoodEntry>>

    @Query("""
        SELECT
            COALESCE(SUM(calories), 0) as calories,
            COALESCE(SUM(protein), 0) as protein,
            COALESCE(SUM(fat), 0) as fat,
            COALESCE(SUM(carbs), 0) as carbs,
            COALESCE(SUM(fiber), 0) as fiber,
            COALESCE(SUM(water_ml), 0) as water_ml
        FROM food_entry
        WHERE date = :date
    """)
    suspend fun getDailyTotals(date: String): DailyNutrition

    @Query("""
        SELECT
            COALESCE(SUM(calories), 0) as calories,
            COALESCE(SUM(protein), 0) as protein,
            COALESCE(SUM(fat), 0) as fat,
            COALESCE(SUM(carbs), 0) as carbs,
            COALESCE(SUM(fiber), 0) as fiber,
            COALESCE(SUM(water_ml), 0) as water_ml
        FROM food_entry
        WHERE date = :date
    """)
    fun getDailyTotalsFlow(date: String): Flow<DailyNutrition>

    @Query("SELECT DISTINCT date FROM food_entry ORDER BY date DESC")
    fun getDistinctDatesFlow(): Flow<List<String>>

    @Query("DELETE FROM food_entry WHERE date < :cutoff")
    suspend fun deleteOlderThan(cutoff: String)

    @Query("DELETE FROM food_entry")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FoodEntry): Long

    @Update
    suspend fun update(entry: FoodEntry)

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Query("DELETE FROM food_entry WHERE id = :id")
    suspend fun deleteById(id: Int)

}
