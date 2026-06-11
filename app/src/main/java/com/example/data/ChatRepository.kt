package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessionsFlow()
    val allSkills: Flow<List<AiSkill>> = chatDao.getAllSkillsFlow()
    val allMemories: Flow<List<MemoryEntity>> = chatDao.getAllMemoriesFlow()

    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSessionFlow(sessionId)
    }

    suspend fun getSessionById(id: Int): ChatSession? {
        return chatDao.getSessionById(id)
    }

    suspend fun createNewSession(title: String): Int {
        return chatDao.insertSession(ChatSession(title = title)).toInt()
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun deleteSession(session: ChatSession) {
        chatDao.deleteMessagesForSession(session.id)
        chatDao.deleteSession(session)
    }

    suspend fun updateSessionTitle(sessionId: Int, title: String) {
        chatDao.updateSessionTitle(sessionId, title)
    }

    // Skills API
    suspend fun insertSkill(skill: AiSkill): Long {
        return chatDao.insertSkill(skill)
    }

    suspend fun deleteSkill(skill: AiSkill) {
        chatDao.deleteSkill(skill)
    }

    suspend fun updateSkillEnabled(id: Int, isEnabled: Boolean) {
        chatDao.updateSkillEnabled(id, isEnabled)
    }

    suspend fun getEnabledSkills(): List<AiSkill> {
        return chatDao.getEnabledSkills()
    }

    // RAG Memories API
    suspend fun insertMemory(memory: MemoryEntity): Long {
        return chatDao.insertMemory(memory)
    }

    suspend fun deleteMemory(memory: MemoryEntity) {
        chatDao.deleteMemory(memory)
    }

    suspend fun clearAllMemories() {
        chatDao.clearAllMemories()
    }

    suspend fun searchMemories(query: String): List<MemoryEntity> {
        return chatDao.searchMemories("%$query%")
    }
}
