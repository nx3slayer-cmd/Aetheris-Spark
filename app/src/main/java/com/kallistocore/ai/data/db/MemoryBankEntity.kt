package com.kallistocore.ai.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_sessions",
    indices = [Index(value = ["updatedAt"])]
)
data class ConversationSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelUsed: String = "Llama 3.2 3B"
)

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String? = null,
    val imageFilePath: String? = null,
    val toolDataJson: String? = null
)

@Entity(
    tableName = "memory_entries",
    indices = [
        Index(value = ["key"]),
        Index(value = ["timestamp"])
    ]
)
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val content: String,
    val importance: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceSessionId: String? = null,
    val embeddingVector: String? = null
)
