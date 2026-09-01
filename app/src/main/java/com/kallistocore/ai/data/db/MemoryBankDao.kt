package com.kallistocore.ai.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryBankDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSessionEntity)

    @Query("SELECT * FROM conversation_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ConversationSessionEntity>>

    @Query("UPDATE conversation_sessions SET updatedAt = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateSessionTimestamp(sessionId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversation_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(sessionId: String, limit: Int): List<MessageEntity>

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: String)

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getTotalMessageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntryEntity): Long

    @Query("SELECT * FROM memory_entries ORDER BY importance DESC, timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryEntryEntity?

    @Query("""
        SELECT * FROM memory_entries 
        WHERE content LIKE '%' || :query || '%' OR `key` LIKE '%' || :query || '%'
        ORDER BY importance DESC 
        LIMIT :limit
    """)
    suspend fun searchMemoriesFts(query: String, limit: Int = 10): List<MemoryEntryEntity>

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memory_entries")
    suspend fun clearAllMemories()

    @Query("SELECT SUM(LENGTH(content)) FROM memory_entries")
    suspend fun getApproximateMemoryBankSizeBytes(): Long?

    @Query("""
        DELETE FROM memory_entries 
        WHERE id IN (
            SELECT id FROM memory_entries 
            ORDER BY importance ASC, timestamp ASC 
            LIMIT :count
        )
    """)
    suspend fun pruneOldestLowPriorityMemories(count: Int)
}
