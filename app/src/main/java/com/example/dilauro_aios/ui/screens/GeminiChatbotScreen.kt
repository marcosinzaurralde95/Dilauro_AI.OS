package com.example.dilauro_aios.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.ChatMessage
import com.example.dilauro_aios.model.ChatRole
import com.example.dilauro_aios.model.ChatbotPersona
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.GenerativeStudioViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatbotScreen(
    viewModel: GenerativeStudioViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.chatMessages.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    val isKeySet = viewModel.isKeyConfigured

    var toastNotice by remember { mutableStateOf<String?>(null) }

    val quickPrompts = listOf(
        "Analyze D-Kernel ML scheduling convergence across cores",
        "Audit WASI capability token permissions for isolation breaches",
        "Explain SynapseFS vector indexing and retrieval patterns",
        "Diagnose DAM working set pressure & trigger zRAM compaction"
    )

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Gemini Neural Copilot", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            MetricBadge(
                                text = selectedPersona.model.take(14),
                                textColor = NeonCyan,
                                backgroundColor = DeepBackground
                            )
                        }
                        Text(
                            selectedPersona.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chatbot_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Conversation", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBackground)
            )
        },
        bottomBar = {
            // Chat Input Bar
            Surface(
                color = DeepBackground,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Quick Prompts Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        items(quickPrompts) { prompt ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CardSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable {
                                    viewModel.onChatInputChanged(prompt)
                                }
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextCyan,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Input Text Field & Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { viewModel.onChatInputChanged(it) },
                            placeholder = { Text("Ask Dilauro Copilot...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        IconButton(
                            onClick = {
                                viewModel.sendChatMessage()
                                coroutineScope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size)
                                    }
                                }
                            },
                            enabled = !isLoading && chatInput.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (chatInput.isNotBlank() && !isLoading) NeonCyan else BorderSubtle)
                                .testTag("send_chat_message_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DeepBackground, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = DeepBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = DeepBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Persona Selector Row (gemini-3.1-pro-preview, gemini-3.5-flash, gemini-3.1-flash-lite)
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderCyan
                ) {
                    Text(
                        text = "SELECT COPILOT ROLE & MODEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChatbotPersona.entries.forEach { persona ->
                            val isSelected = selectedPersona == persona
                            OutlinedButton(
                                onClick = { viewModel.selectPersona(persona) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DeepBackground
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else BorderSubtle
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = persona.roleBadge,
                                        fontSize = 9.sp,
                                        color = if (isSelected) NeonCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = persona.model.replace("-preview", ""),
                                        fontSize = 8.sp,
                                        color = TextTertiary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedPersona.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Toast Notice
            item {
                AnimatedVisibility(visible = toastNotice != null) {
                    toastNotice?.let { notice ->
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = EmeraldSecurity
                        ) {
                            Text(notice, color = EmeraldSecurity, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Message Thread
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    message = msg,
                    onCopyText = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Copilot Message", msg.content))
                        toastNotice = "Message copied to clipboard!"
                    }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                        Text(
                            text = "${selectedPersona.title} is reasoning...",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCopyText: () -> Unit
) {
    val isUser = message.role == ChatRole.USER
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DeepBackground)
                    .border(1.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            color = if (isUser) NeonCyan.copy(alpha = 0.18f) else DarkSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) NeonCyan.copy(alpha = 0.5f) else BorderSubtle
            ),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clickable { onCopyText() }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender label & time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "OPERATOR" else (message.modelUsed ?: "GEMINI"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) NeonCyan else NeonPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sdf.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.2f))
                    .border(1.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}
