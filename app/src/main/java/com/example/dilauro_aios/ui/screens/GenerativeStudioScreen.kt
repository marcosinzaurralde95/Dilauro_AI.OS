package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.ui.theme.DeepBackground
import com.example.dilauro_aios.ui.theme.NeonCyan
import com.example.dilauro_aios.ui.theme.TextSecondary
import com.example.dilauro_aios.viewmodel.GenerativeStudioViewModel

enum class StudioTab(val title: String, val icon: ImageVector, val tag: String) {
    IMAGES("Images (Gemini 3)", Icons.Default.Image, "tab_images"),
    TRANSCRIBE("Transcribe Audio", Icons.Default.Mic, "tab_transcribe"),
    MUSIC("Music (Lyria 3)", Icons.Default.MusicNote, "tab_music"),
    CHATBOT("Gemini Copilot", Icons.Default.Chat, "tab_chatbot"),
    VIDEO("Veo 3 Video", Icons.Default.Videocam, "tab_video")
}

@Composable
fun GenerativeStudioScreen(
    viewModel: GenerativeStudioViewModel,
    onBack: () -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        // Scrollable Tab Row for switching between all generative neural features
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DeepBackground,
            contentColor = NeonCyan,
            edgePadding = 12.dp,
            modifier = Modifier.testTag("generative_studio_tabs")
        ) {
            StudioTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp)) },
                    text = { Text(tab.title, fontSize = 11.sp, maxLines = 1) },
                    selectedContentColor = NeonCyan,
                    unselectedContentColor = TextSecondary,
                    modifier = Modifier.testTag(tab.tag)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> ImageStudioScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
                1 -> AudioTranscriberScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
                2 -> MusicGeneratorScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
                3 -> GeminiChatbotScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
                4 -> VeoVideoGeneratorScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
        }
    }
}
