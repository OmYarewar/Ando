package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Int): ChatSession?

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Int)

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Int, title: String)

    // AI Skills CRUD
    @Query("SELECT * FROM ai_skills ORDER BY id ASC")
    fun getAllSkillsFlow(): Flow<List<AiSkill>>

    @Query("SELECT * FROM ai_skills WHERE isEnabled = 1")
    suspend fun getEnabledSkills(): List<AiSkill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: AiSkill): Long

    @Delete
    suspend fun deleteSkill(skill: AiSkill)

    @Query("UPDATE ai_skills SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateSkillEnabled(id: Int, isEnabled: Boolean)

    // RAG Memories CRUD
    @Query("SELECT * FROM rag_memories ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM rag_memories WHERE keyFact LIKE :query OR associatedTopic LIKE :query")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM rag_memories")
    suspend fun clearAllMemories()
}
