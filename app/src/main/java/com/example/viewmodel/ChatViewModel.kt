package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(database.chatDao)
    val settings = SettingsManager(application)

    // Setup network clients
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val nvidiaApi: NvidiaNimApi = Retrofit.Builder()
        .baseUrl("https://integrate.api.nvidia.com/v1/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(NvidiaNimApi::class.java)

    private val searchService = WebSearchService(okHttpClient)

    // State flows
    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val skills: StateFlow<List<AiSkill>> = repository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForSession(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchStatus = MutableStateFlow<String?>(null)
    val searchStatus: StateFlow<String?> = _searchStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Auto-select the most recent session or create one on launch
        viewModelScope.launch {
            sessions.collectFirst { list ->
                if (list.isNotEmpty() && _currentSessionId.value == null) {
                    _currentSessionId.value = list.first().id
                }
            }
        }

        // Initialize default AI Skills and memories if SQLite lists are blank
        viewModelScope.launch {
            skills.collectFirst { list ->
                if (list.isEmpty()) {
                    val defaults = listOf(
                        AiSkill(
                            uuid = "general-assist",
                            title = "💡 General Assistant",
                            description = "Standard helpful general AI chatbot",
                            systemPrompt = "You are an intelligent, friendly general-purpose chat companion powered by NVIDIA NIM.",
                            isEnabled = true,
                            isSystem = true
                        ),
                        AiSkill(
                            uuid = "web-agent",
                            title = "🔍 Web Deep-Search",
                            description = "Synthesizes web search results with citation layouts",
                            systemPrompt = "When search results are provided in the context, analyze them carefully. Synthesize factual claims with bullet points and clear, visible Markdown citation links.",
                            isEnabled = true,
                            isSystem = true
                        ),
                        AiSkill(
                            uuid = "rag-memory",
                            title = "🧠 RAG Context Sync",
                            description = "Reads, recalls and aligns with matching SQLite memories",
                            systemPrompt = "You have integrated long-term Memory capabilities. Align your response style with matching memories retrieve from previous chats.",
                            isEnabled = true,
                            isSystem = true
                        ),
                        AiSkill(
                            uuid = "turbo-speed",
                            title = "🚀 Turbo Mode",
                            description = "Compresses output layout for lightning-fast replies",
                            systemPrompt = "Reply incredibly concisely! Bullet points only, strictly under 3 sentences. No welcome messages, no introductory talk, no chit-chat.",
                            isEnabled = false,
                            isSystem = true
                        ),
                        AiSkill(
                            uuid = "game-master",
                            title = "🎮 Sci-Fi Game Master",
                            description = "Turns chats into interactive survival RPG game modules",
                            systemPrompt = "Act as the Game Master for a deep-space adventure RPG. Present a thrilling narrative situation, then provide the user with three clean choices: [A], [B], or [C]. Maintain this interactive text format.",
                            isEnabled = false,
                            isSystem = true
                        )
                    )
                    defaults.forEach { repository.insertSkill(it) }
                }
            }

            memories.collectFirst { list ->
                if (list.isEmpty()) {
                    repository.insertMemory(
                        MemoryEntity(
                            keyFact = "Highly powered by NVIDIA NIM and SQLite dynamic context syncing.",
                            associatedTopic = "system-info"
                        )
                    )
                    repository.insertMemory(
                        MemoryEntity(
                            keyFact = "Active user yarewarom@gmail.com is testing the ultimate Ando client.",
                            associatedTopic = "userinfo"
                        )
                    )
                }
            }
        }
    }

    private suspend fun <T> Flow<T>.collectFirst(action: suspend (T) -> Unit) {
        take(1).collect(action)
    }

    fun selectSession(id: Int) {
        _currentSessionId.value = id
        _errorMessage.value = null
    }

    fun createNewSession(initialTitle: String = "Blank Chat") {
        viewModelScope.launch {
            val newId = repository.createNewSession(initialTitle)
            _currentSessionId.value = newId
            _errorMessage.value = null
        }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_currentSessionId.value == session.id) {
                // select another or clear
                val currentList = sessions.value.filter { it.id != session.id }
                if (currentList.isNotEmpty()) {
                    _currentSessionId.value = currentList.first().id
                } else {
                    _currentSessionId.value = null
                }
            }
        }
    }

    fun renameSession(id: Int, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(id, newTitle)
        }
    }

    // Skills custom modifiers
    fun toggleSkill(id: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateSkillEnabled(id, isEnabled)
        }
    }

    fun deleteSkill(skill: AiSkill) {
        viewModelScope.launch {
            repository.deleteSkill(skill)
        }
    }

    fun insertCustomSkill(title: String, description: String, prompt: String) {
        viewModelScope.launch {
            repository.insertSkill(
                AiSkill(
                    uuid = "custom-${System.currentTimeMillis()}",
                    title = title,
                    description = description,
                    systemPrompt = prompt,
                    isEnabled = true,
                    isSystem = false
                )
            )
        }
    }

    // Manual RAG memories
    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
        }
    }

    fun addManualMemory(keyFact: String, topic: String) {
        viewModelScope.launch {
            repository.insertMemory(
                MemoryEntity(keyFact = keyFact, associatedTopic = topic)
            )
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val key = settings.nvidiaApiKey
        if (key.isBlank()) {
            _errorMessage.value = "NVIDIA NIM API Key is missing. Tap the Config section to set your credentials!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _searchStatus.value = null

            // 1. Ensure active session exists
            var activeSessionId = _currentSessionId.value
            if (activeSessionId == null) {
                activeSessionId = repository.createNewSession("Blank Chat")
                _currentSessionId.value = activeSessionId
            }

            // 2. Fetch match index RAG memories dynamically
            val matchedMemories = mutableListOf<MemoryEntity>()
            val searchWords = content.split(Regex("\\s+")).filter { it.length > 3 }
            searchWords.take(4).forEach { word ->
                val matches = repository.searchMemories(word)
                matchedMemories.addAll(matches)
            }
            val uniqueMemories = matchedMemories.distinctBy { it.id }

            val memoriesContext = if (uniqueMemories.isNotEmpty()) {
                """
                === SQLite Retrieved User Memories ===
                Prior historical facts matches retrieved regarding topics:
                ${uniqueMemories.joinToString("\n") { "- Memory: ${it.keyFact} (Topic: ${it.associatedTopic})" }}
                ======================================
                """.trimIndent()
            } else ""

            // 3. Perform Web Search if toggled
            var finalPrompt = content
            var searchQueryString: String? = null
            var searchResultsString: String? = null

            if (settings.searchEnabled) {
                _searchStatus.value = "Querying live web matches..."
                try {
                    val results = searchService.search(content, settings)
                    if (results.isNotEmpty()) {
                        searchQueryString = content
                        searchResultsString = results.joinToString("\n---\n") { 
                            "Title: ${it.title}\nSource: ${it.url}\nSummary: ${it.snippet}"
                        }
                        _searchStatus.value = "Retrieved ${results.size} snippets! Analyzing..."
                        
                        finalPrompt = """
                            Below is search context retrieved from the web matching the user's prompt. 
                            Use the search results to inform your response.

                            === Web Search Context ===
                            ${searchResultsString}
                            ===========================

                            User's Prompt: $content
                        """.trimIndent()
                    } else {
                        _searchStatus.value = "No direct web matches found. Synthesizing..."
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Web search failed", e)
                    _searchStatus.value = "Search error. Synthesizing standard context..."
                }
            }

            // Combine RAG dynamic context inside final prompting
            if (memoriesContext.isNotBlank()) {
                finalPrompt = "$memoriesContext\n\n$finalPrompt"
            }

            // 4. Save User Message
            val userMsgId = repository.insertMessage(
                ChatMessage(
                    sessionId = activeSessionId!!,
                    role = "user",
                    content = content,
                    searchQuery = searchQueryString,
                    searchResults = searchResultsString
                )
            )

            // 5. Update blank session titles dynamically
            val currentSession = repository.getSessionById(activeSessionId)
            if (currentSession != null && (currentSession.title == "Blank Chat" || currentSession.title == "New Chat")) {
                val words = content.split(" ")
                val cleanTitle = if (words.size > 5) words.take(5).joinToString(" ") + "..." else content
                repository.updateSessionTitle(activeSessionId, cleanTitle)
            }

            // 6. Gather conversation history for NVIDIA NIM
            val enabledSkills = repository.getEnabledSkills()
            val systemThemePrompt = enabledSkills.joinToString("\n") { it.systemPrompt }

            val history = mutableListOf<NvidiaMessage>()
            if (systemThemePrompt.isNotBlank()) {
                history.add(NvidiaMessage(role = "system", content = systemThemePrompt))
            }

            // Grab previous 12 messages in this session
            val sessionHist = messages.value.takeLast(12).map { msg ->
                val msgContent = if (msg.id.toLong() == userMsgId) {
                    finalPrompt
                } else {
                    msg.content
                }
                NvidiaMessage(role = msg.role, content = msgContent)
            }
            history.addAll(sessionHist)

            if (history.isEmpty() || history.none { it.role == "user" }) {
                history.add(NvidiaMessage(role = "user", content = finalPrompt))
            }

            // 7. Request NIM response
            try {
                val response = nvidiaApi.getChatCompletion(
                    authorization = "Bearer $key",
                    request = NvidiaChatRequest(
                        model = settings.nvidiaModelId,
                        messages = history
                    )
                )

                val choice = response.choices?.firstOrNull()
                if (choice != null) {
                    val aiReply = choice.message.content
                    repository.insertMessage(
                        ChatMessage(
                            sessionId = activeSessionId,
                            role = "assistant",
                            content = aiReply
                        )
                    )

                    // Auto extraction triggered after response completes
                    autoExtractMemory(content, aiReply)

                } else {
                    _errorMessage.value = "Empty completes choices block. Please check that selected Model supports free Chat completions."
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "NVIDIA NIM API request error", e)
                _errorMessage.value = "NVIDIA NIM API Error: ${e.localizedMessage ?: "Failed connection"}. Ensure your NIM API Key is valid."
            } finally {
                _isLoading.value = false
                _searchStatus.value = null
            }
        }
    }

    // Auto RAG memory builder loop
    private fun autoExtractMemory(userMsg: String, aiReply: String) {
        viewModelScope.launch {
            // Local fast regex extraction
            val expressions = listOf(
                Regex("my\\s+(\\w+)\\s+is\\s+([\\w\\s\\d]+)", RegexOption.IGNORE_CASE),
                Regex("i\\s+(like|love|know|work as|live in)\\s+([\\w\\s\\d]+)", RegexOption.IGNORE_CASE)
            )
            for (regex in expressions) {
                val match = regex.find(userMsg)
                if (match != null) {
                    repository.insertMemory(
                        MemoryEntity(
                            keyFact = "User states: ${match.value.trim()}",
                            associatedTopic = "regex-auto"
                        )
                    )
                }
            }

            // Background Deep NIM factual extractor
            val key = settings.nvidiaApiKey
            if (key.isNotBlank()) {
                try {
                    val response = nvidiaApi.getChatCompletion(
                        authorization = "Bearer $key",
                        request = NvidiaChatRequest(
                            model = settings.nvidiaModelId,
                            maxTokens = 120,
                            messages = listOf(
                                NvidiaMessage(
                                    role = "user",
                                    content = """
                                        Analyze this exchange. Extract at most one short simple personal trait/fact about the user (e.g. name, location, hobby, stack) in under 8 words. 
                                        If nothing is clearly stated, output literally "NONE".
                                        
                                        User: "$userMsg"
                                        AI: "$aiReply"
                                    """.trimIndent()
                                )
                            )
                        )
                    )
                    val fact = response.choices?.firstOrNull()?.message?.content?.trim()
                    if (!fact.isNullOrBlank() && !fact.contains("none", ignoreCase = true) && fact.length > 5) {
                        repository.insertMemory(
                            MemoryEntity(
                                keyFact = fact.replace("\"", ""),
                                associatedTopic = "nim-auto"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Background fact extraction skipped", e)
                }
            }
        }
    }

    // Auto creation from context
    fun autoGenerateSkillFromSession() {
        val key = settings.nvidiaApiKey
        if (key.isBlank()) return
        
        val currentActiveId = _currentSessionId.value ?: return
        val sessionMessages = messages.value
        if (sessionMessages.size < 2) {
            _errorMessage.value = "Type some prompt queries before auto-generating a skill so NIM has dialog lines to parse!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _searchStatus.value = "Auto-Generating customized skill theme using NIM..."
            try {
                val sampleConversation = sessionMessages.takeLast(6).joinToString("\n") { 
                    "${it.role.uppercase()}: ${it.content}"
                }

                val response = nvidiaApi.getChatCompletion(
                    authorization = "Bearer $key",
                    request = NvidiaChatRequest(
                        model = settings.nvidiaModelId,
                        maxTokens = 400,
                        messages = listOf(
                            NvidiaMessage(
                                role = "user",
                                content = """
                                    Analyze the conversation theme below. Formulate a specialized agent skill.
                                    You MUST output exactly three parts, separated by double dashes "===" with no preamble:
                                    [One-word Icon] [Short Title]
                                    ===
                                    [Short 10-word description of target specialization]
                                    ===
                                    [Detailed system prompt defining style, context, rules, and guidelines]

                                    === Conversation ===
                                    $sampleConversation
                                """.trimIndent()
                            )
                        )
                    )
                )

                val contentResp = response.choices?.firstOrNull()?.message?.content
                if (!contentResp.isNullOrBlank()) {
                    val parts = contentResp.split("===")
                    if (parts.size >= 3) {
                        val title = parts[0].replace("[", "").replace("]", "").trim()
                        val desc = parts[1].replace("[", "").replace("]", "").trim()
                        val systemPrompt = parts[2].replace("[", "").replace("]", "").trim()

                        repository.insertSkill(
                            AiSkill(
                                uuid = "auto-gen-${System.currentTimeMillis()}",
                                title = title,
                                description = desc,
                                systemPrompt = systemPrompt,
                                isEnabled = true,
                                isSystem = false
                            )
                        )
                        _searchStatus.value = "Generated Skill: $title!"
                        kotlinx.coroutines.delay(1500)
                    } else {
                        // fallback
                        val rID = (1000..9999).random()
                        repository.insertSkill(
                            AiSkill(
                                uuid = "auto-gen-$rID",
                                title = "✨ Theme Customizer $rID",
                                description = "Synthesized automatically matching session context",
                                systemPrompt = "Align closely with historical topic patterns: $sampleConversation",
                                isEnabled = true,
                                isSystem = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Skill auto creation failed", e)
                _errorMessage.value = "Failed to auto-generate skill: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _searchStatus.value = null
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
