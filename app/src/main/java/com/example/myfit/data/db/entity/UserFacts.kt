package com.example.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_facts",
    indices = [Index(value = ["key"], unique = true)]
)
data class UserFacts(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,
    val value: String,
    val source: String,
    val updated_at: Long = System.currentTimeMillis()
)
