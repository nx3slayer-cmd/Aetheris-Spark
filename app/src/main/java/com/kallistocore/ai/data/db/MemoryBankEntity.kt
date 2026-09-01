package com.kallistocore.ai.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks individual chat threads / conversation sessions.
 */
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

/**
 * Stores chat messages, tool outputs (search), and multimodal attachments (Kokoro audio, Img2Img).
 */
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
    val role: String, // "user", "assistant", "system", "search_tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String? = null, // Synthesized Kokoro TTS audio or recorded mic clip
    val imageFilePath: String? = null, // Generated or Img2Img edited artwork path
    val toolDataJson: String? = null   // Structured JSON payload for web search or system hooks
)

/**
 * Dedicated Long-Term Memory Bank.
 * Retains permanent facts, user preferences, and memories across multiple sessions.
 */
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
    val key: String,                  // Fact category or memory key (e.g., "user_name", "coding_preferences")
    val content: String,              // Actual fact / memory detail
    val importance: Float = 1.0f,     // Priority weight (0.0 to 1.0)
    val timestamp: Long = System.currentTimeMillis(),
    val sourceSessionId: String? = null,
    val embeddingVector: String? = null // Comma-separated float values for local semantic vector search
)

/**
 * SQLite Full-Text Search (FTS4) index for instantaneous querying over the Memory Bank.
 */
@Entity(tableName = "memory_entries_fts")
@Fts4(contentEntity = MemoryEntryEntity::class)
data class MemoryFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    val key: String,
    val content: String
)
