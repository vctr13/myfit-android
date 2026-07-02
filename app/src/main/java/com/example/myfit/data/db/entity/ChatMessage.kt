package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_message",
    indices = [Index("chat_type")]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chat_type: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
