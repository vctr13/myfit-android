package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfit.data.db.entity.UserFacts
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFactsDao {

    @Query("SELECT * FROM user_facts ORDER BY updated_at DESC")
    fun getAll(): Flow<List<UserFacts>>

    @Query("SELECT * FROM user_facts ORDER BY updated_at DESC")
    suspend fun getAllOnce(): List<UserFacts>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fact: UserFacts)

    @Query("DELETE FROM user_facts WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM user_facts")
    suspend fun deleteAll()
}
