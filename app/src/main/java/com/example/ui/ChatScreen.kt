package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Data flow subscriptions
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchStatus by viewModel.searchStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val skillsList by viewModel.skills.collectAsStateWithLifecycle()
    val memoriesList by viewModel.memories.collectAsStateWithLifecycle()

    val latestRelease by viewModel.latestRelease.collectAsStateWithLifecycle()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsStateWithLifecycle()
    val updateCheckInProgress by viewModel.updateCheckInProgress.collectAsStateWithLifecycle()

    var showUpdateAlert by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Chat, 1 = RAG, 2 = Credentials/Skills
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto scroll down upon message list additions
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = Modifier.testTag("chat_drawer"),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Directory Branding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Ando OS",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ando Client",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "v${com.example.BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Action panel
                Text(
                    text = "CONVERSATIONS DIRECTORY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )

                Button(
                    onClick = {
                        viewModel.createNewSession("New Chat")
                        selectedTab = 0
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("btn_new_chat"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add session")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Dialogue", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sessions directory directory list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        val isSelected = session.id == currentSessionId
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = session.title,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                viewModel.selectSession(session.id)
                                selectedTab = 0
                                coroutineScope.launch { drawerState.close() }
                            },
                            badge = {
                                IconButton(
                                    onClick = { viewModel.deleteSession(session) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Session Log",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            modifier = Modifier.padding(vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer diagnostics metrics panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Logs",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SQLITE STORAGE COCKPIT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Local Chat Logs: ${sessions.size}\n• Learned Traits: ${memoriesList.size}\n• Configured Skills: ${skillsList.size}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "ANDO AI DIALOGUE"
                                    1 -> "RAG COGNITIVE SYNC"
                                    else -> "INTELLIGENCE WORKSPACE"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            )
                            val currentTitle = sessions.find { it.id == currentSessionId }?.title ?: "New Conversation"
                            Text(
                                text = if (selectedTab == 0) currentTitle else "Subsystem Connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu Drawer")
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            val searchEnabled = viewModel.settings.searchEnabled
                            IconButton(onClick = {
                                viewModel.settings.searchEnabled = !searchEnabled
                                viewModel.createNewSession(sessions.find { it.id == currentSessionId }?.title ?: "New Chat")
                                viewModel.selectSession(currentSessionId ?: 0)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Selector",
                                    tint = if (searchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Chat", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Face, contentDescription = "Active Dialogue") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("RAG Memory", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Memory Folders") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("Workspace", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Environment settings") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Subtle Ambient Glow background visual asset
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Active dialogue interface
                        Column(modifier = Modifier.fillMaxSize()) {
                            
                            // Active capabilities ribbon
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (viewModel.settings.nvidiaApiKey.isNotBlank()) Color.Green else Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (viewModel.settings.nvidiaApiKey.isNotBlank()) "NVIDIA KEY MOUNTED" else "NO API KEY FOUND",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Memory Indicator
                                    Text(
                                        text = "RAG SYNC: ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (viewModel.settings.searchEnabled) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "WEB SYNC",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (errorMessage != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Alert",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.clearError() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss error",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (messages.isEmpty()) {
                                EmptyChatState(
                                    onChipClick = { prompt ->
                                        inputText = prompt
                                        viewModel.sendMessage(prompt)
                                        inputText = ""
                                        keyboardController?.hide()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                                ) {
                                    items(messages) { message ->
                                        ChatMessageItem(message = message)
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                }
                            }

                            // Thinking status bar
                            AnimatedVisibility(
                                visible = isLoading || searchStatus != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = searchStatus ?: "Computing NIM inference payload...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = Color.Transparent
                                    )
                                }
                            }

                            // Dynamic input console
                            Surface(
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .navigationBarsPadding()
                                        .imePadding(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        placeholder = { Text("Ask anything... RAG context maps automatically") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("prompt_input"),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                        ),
                                        maxLines = 4,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(
                                            onSend = {
                                                if (inputText.isNotBlank() && !isLoading) {
                                                    viewModel.sendMessage(inputText)
                                                    inputText = ""
                                                    keyboardController?.hide()
                                                }
                                            }
                                        ),
                                        trailingIcon = {
                                            if (inputText.isNotEmpty()) {
                                                IconButton(onClick = { inputText = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear input",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (inputText.isNotBlank() && !isLoading)
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    )
                                                else
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                    )
                                            )
                                            .clickable(enabled = inputText.isNotBlank() && !isLoading) {
                                                viewModel.sendMessage(inputText)
                                                inputText = ""
                                                keyboardController?.hide()
                                            }
                                            .testTag("submit_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = if (inputText.isNotBlank() && !isLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // TAB 1: Local database memories controller
                        RAGMemoryManagerScreen(
                            viewModel = viewModel,
                            memoriesList = memoriesList
                        )
                    }
                    else -> {
                        // TAB 2: AI Skills Prompt card editor & credentials
                        SkillsWorkspaceScreen(
                            viewModel = viewModel,
                            skillsList = skillsList
                        )
                    }
                }
            }
        }

        // Auto release trigger alert
        if (isUpdateAvailable && latestRelease != null && showUpdateAlert) {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            AlertDialog(
                onDismissRequest = { showUpdateAlert = false },
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New APK Available",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "A new version (${latestRelease?.tagName}) is downloadable directly from Ando GitHub Releases repository page.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        latestRelease?.body?.let { notes ->
                            if (notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Release Notes Preview:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 4
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUpdateAlert = false
                            uriHandler.openUri(latestRelease?.htmlUrl ?: "https://github.com/${viewModel.settings.githubRepo}/releases")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Download Update", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateAlert = false }) {
                        Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

// ==========================================
// RAG Memory Panel Screen view
// ==========================================
@Composable
fun RAGMemoryManagerScreen(
    viewModel: ChatViewModel,
    memoriesList: List<MemoryEntity>
) {
    var searchQuery by remember { mutableStateOf("") }
    var newFactKey by remember { mutableStateOf("") }
    var newFactTopic by remember { mutableStateOf("") }

    val filteredMemories = remember(searchQuery, memoriesList) {
        if (searchQuery.isBlank()) {
            memoriesList
        } else {
            memoriesList.filter {
                it.keyFact.contains(searchQuery, ignoreCase = true) ||
                        it.associatedTopic.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🧠 Local Cognitive Memory Bank",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "User traits and factual highlights are summarized in real-time. SQLite queries align matching memory pieces matching your user prompts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Filter keywords or topics") },
                placeholder = { Text("e.g. Kotlin...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Add Manual Memory
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✍️ Record Custom Memory Point",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFactKey,
                        onValueChange = { newFactKey = it },
                        label = { Text("Fact Statement text") },
                        placeholder = { Text("e.g. Favorite development tool is Android Studio") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFactTopic,
                        onValueChange = { newFactTopic = it },
                        label = { Text("Topic Tag name") },
                        placeholder = { Text("e.g. editor-prefs") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (newFactKey.isNotBlank() && newFactTopic.isNotBlank()) {
                                viewModel.addManualMemory(newFactKey, newFactTopic)
                                newFactKey = ""
                                newFactTopic = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Database", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section directory details list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stored Factual Matrices (${filteredMemories.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                if (filteredMemories.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearAllMemories() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
            }
        }

        if (filteredMemories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded insights located in SQLite.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredMemories, key = { it.id }) { memory ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = memory.associatedTopic.lowercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dynamic Memory Sync",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = memory.keyFact,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.deleteMemory(memory) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Skills & AI Workspace (Configuration Screen)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsWorkspaceScreen(
    viewModel: ChatViewModel,
    skillsList: List<AiSkill>
) {
    var isCreatingCustom by remember { mutableStateOf(false) }
    var skillTitle by remember { mutableStateOf("") }
    var skillDesc by remember { mutableStateOf("") }
    var skillPrompt by remember { mutableStateOf("") }

    var apiKey by remember { mutableStateOf(viewModel.settings.nvidiaApiKey) }
    var modelId by remember { mutableStateOf(viewModel.settings.nvidiaModelId) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var githubRepo by remember { mutableStateOf(viewModel.settings.githubRepo) }

    val latestRelease by viewModel.latestRelease.collectAsStateWithLifecycle()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsStateWithLifecycle()
    val updateCheckInProgress by viewModel.updateCheckInProgress.collectAsStateWithLifecycle()

    val presetModels = listOf(
        "nvidia/llama-3.1-nemotron-70b-instruct",
        "meta/llama-3.1-8b-instruct",
        "mistralai/mixtral-8x7b-instruct",
        "google/gemma-2-27b-it"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "⚙️ Intelligence Card Workspace",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Toggled developer instruction cards are assembled in real-time as dynamic system guidelines to drive the context structure of the chatbot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // NIM Auto Prompt card builder
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✨ NVIDIA NIM Prompter Auto-Builder",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extracts general topics from your active chat log to generate customized instruction skill cards automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.autoGenerateSkillFromSession() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Synthesize Custom Skill Card", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Manual builder trigger
        item {
            if (!isCreatingCustom) {
                OutlinedButton(
                    onClick = { isCreatingCustom = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manually Forge Custom System Card", fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Forge Custom prompt card",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { isCreatingCustom = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = skillTitle,
                            onValueChange = { skillTitle = it },
                            label = { Text("Title tag / icon symbol") },
                            placeholder = { Text("e.g. 🕵️ Cyber investigator") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = skillDesc,
                            onValueChange = { skillDesc = it },
                            label = { Text("Brief description summary") },
                            placeholder = { Text("Provides security advice with rigorous checks") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = skillPrompt,
                            onValueChange = { skillPrompt = it },
                            label = { Text("Expanded instruction system prompt") },
                            placeholder = { Text("Act as a strict cybersecurity analyst...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 5
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (skillTitle.isNotBlank() && skillPrompt.isNotBlank()) {
                                    viewModel.insertCustomSkill(skillTitle, skillDesc, skillPrompt)
                                    skillTitle = ""
                                    skillDesc = ""
                                    skillPrompt = ""
                                    isCreatingCustom = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Enshrine System Skill", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active list header
        item {
            Text(
                text = "Active instruction cards (${skillsList.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Cards list
        items(skillsList, key = { it.id }) { skill ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (skill.isEnabled)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (skill.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = skill.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = skill.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = skill.isEnabled,
                            onCheckedChange = { viewModel.toggleSkill(skill.id, it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = skill.systemPrompt,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    if (!skill.isSystem) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { viewModel.deleteSkill(skill) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retire customized card", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Credentials Division
        item {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "🛡️ Secure System Credentials",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    viewModel.settings.nvidiaApiKey = it
                },
                label = { Text("NVIDIA NIM Auth Token Key") },
                placeholder = { Text("nvapi-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        text = if (apiKeyVisible) "Hide" else "Show",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { apiKeyVisible = !apiKeyVisible }
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )
            Text(
                text = "Saved locally on-device. Obtain a key token from build.nvidia.com.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        item {
            OutlinedTextField(
                value = modelId,
                onValueChange = {
                    modelId = it
                    viewModel.settings.nvidiaModelId = it
                },
                label = { Text("NVIDIA NIM Active model Selection ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Popular Preset Model Selections:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Model selector buttons
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetModels.forEach { pModel ->
                    val isSelected = pModel == modelId
                    SuggestionChip(
                        onClick = {
                            modelId = pModel
                            viewModel.settings.nvidiaModelId = pModel
                        },
                        label = { Text(pModel.substringAfter("/"), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            labelColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // GitHub release syncer
        item {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "🐙 GitHub Updates Sync Integration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Tracks the repository slug owner/name to inspect and synchronize dynamic Android APK updates from public GitHub releases.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = githubRepo,
                onValueChange = {
                    githubRepo = it
                    viewModel.settings.githubRepo = it
                },
                label = { Text("GitHub Repo Slug (owner/repository)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Checking action updates card
        item {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUpdateAvailable)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isUpdateAvailable) 1.dp else 0.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ando Version System",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Installed Version Code: ${com.example.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isUpdateAvailable && latestRelease != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "GitHub Release Found: ${latestRelease?.tagName}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (updateCheckInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Button(
                                onClick = { viewModel.checkForUpdates(silent = false) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Check", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isUpdateAvailable && latestRelease != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "A modern updated build is uploaded on GitHub. Download and overwrite installer APK dynamically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        latestRelease?.body?.let { notes ->
                            if (notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = Color.Black.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Release Logs:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = notes,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                uriHandler.openUri(latestRelease?.htmlUrl ?: "https://github.com/${viewModel.settings.githubRepo}/releases")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download APK Package", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Conversation Empty Cards State Helper
// ==========================================
@Composable
fun EmptyChatState(
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val starters = listOf(
        "Explain quantum physics in 2 simple sentences.",
        "What are some popular NVIDIA NIM microservices used today?",
        "Help me write a professional mail requesting a week off work."
    )

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High fidelity cosmic glowing sphere represent AI face
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "Robot logo",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Welcome to Ando Intelligence",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "An advanced dialogue platform syncing local memory fact models and active web research powered by high-speed NVIDIA NIM instances.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "SELECT AN ACTIVE EXPLORE FOCUS CHIP:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        starters.forEach { prompt ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { onChipClick(prompt) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Prompt Starter",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ==========================================
// Conversation Chat Bubble Row Renderer
// ==========================================
@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    var showReferences by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "AI face icon",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(0.85f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Main bubble card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser)
                            Color.White
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Web Search expandable details ribbon
            if (message.searchResults != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        .clickable { showReferences = !showReferences }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search details",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showReferences) "Hide dynamic references" else "View deep-search matrix results",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showReferences) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                if (showReferences) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "LIVE WEB RESULTS TRANSCRIPT:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = message.searchResults,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User profile face",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
