package com.example.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myfit.data.db.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_message WHERE chat_type = :chatType ORDER BY timestamp ASC")
    fun getMessages(chatType: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_message WHERE chat_type = :chatType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(chatType: String, limit: Int = 10): List<ChatMessage>

    @Query("SELECT * FROM chat_message WHERE chat_type = :chatType AND timestamp >= :sinceMs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesSince(chatType: String, sinceMs: Long, limit: Int = 20): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage)

    @Query("DELETE FROM chat_message WHERE chat_type = :chatType")
    suspend fun deleteByType(chatType: String)

    @Query("DELETE FROM chat_message")
    suspend fun deleteAll()

    @Query("""
        DELETE FROM chat_message WHERE id IN (
            SELECT id FROM chat_message
            WHERE chat_type = :chatType
            ORDER BY timestamp ASC
            LIMIT MAX(0, (SELECT COUNT(*) FROM chat_message WHERE chat_type = :chatType) - :keepLast)
        )
    """)
    suspend fun trimOldMessages(chatType: String, keepLast: Int = 50)
}
