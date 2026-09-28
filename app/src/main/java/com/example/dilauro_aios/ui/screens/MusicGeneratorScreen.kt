package com.example.dilauro_aios.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.GeneratedMusicTrack
import com.example.dilauro_aios.model.LyriaMusicDuration
import com.example.dilauro_aios.model.MusicGenrePreset
import com.example.dilauro_aios.ui.components.AudioWaveformVisualizer
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.GenerativeStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicGeneratorScreen(
    viewModel: GenerativeStudioViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val musicPrompt by viewModel.musicPrompt.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val durationConfig by viewModel.musicDurationConfig.collectAsState()
    val bpm by viewModel.musicBpm.collectAsState()
    val isGenerating by viewModel.isGeneratingMusic.collectAsState()
    val tracks by viewModel.musicTracks.collectAsState()
    val playingTrackId by viewModel.playingTrackId.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val isKeySet = viewModel.isKeyConfigured

    var notificationMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Neural Music Synthesizer", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            MetricBadge(
                                text = "LYRIA-3",
                                textColor = NeonCyan,
                                backgroundColor = DeepBackground
                            )
                        }
                        Text(
                            "AI Music Composition via Lyria-3 Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("music_gen_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBackground)
            )
        },
        containerColor = DeepBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Engine Status Notice
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isKeySet) BorderCyan.copy(alpha = 0.5f) else BorderSubtle
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isKeySet) EmeraldSecurity else WarningAmber, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKeySet) "GOOGLE LYRIA-3 MUSIC ENGINE CONNECTED" else "LYRIA-3 READY (PROCEDURAL AUDIO ACTIVE)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isKeySet) EmeraldSecurity else WarningAmber
                            )
                            Text(
                                text = "Supports lyria-3-clip-preview (up to 30s) and lyria-3-pro-preview (full tracks) with real-time waveform synthesis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Notification Banner
            item {
                AnimatedVisibility(visible = notificationMessage != null) {
                    notificationMessage?.let { msg ->
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = EmeraldSecurity
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = EmeraldSecurity)
                                Text(msg, color = EmeraldSecurity, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Lyria-3 Composer Deck
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderCyan
                ) {
                    Text(
                        text = "COMPOSE NEURAL SOUNDTRACK",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Model & Duration Mode Selector
                    Text("Lyria Model & Track Duration:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LyriaMusicDuration.entries.forEach { duration ->
                            val isSelected = durationConfig == duration
                            OutlinedButton(
                                onClick = { viewModel.onMusicDurationConfigSelected(duration) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DeepBackground
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else BorderSubtle
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = duration.label,
                                        fontSize = 11.sp,
                                        color = if (isSelected) NeonCyan else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = duration.model,
                                        fontSize = 9.sp,
                                        color = TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Genre Presets
                    Text("Genre Preset:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MusicGenrePreset.entries) { genre ->
                            val isSelected = selectedGenre == genre
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DeepBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else BorderSubtle
                                ),
                                modifier = Modifier.clickable { viewModel.onGenreSelected(genre) }
                            ) {
                                Text(
                                    text = "${genre.genreName} (${genre.bpm} BPM)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Music Prompt
                    OutlinedTextField(
                        value = musicPrompt,
                        onValueChange = { viewModel.onMusicPromptChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_prompt_input"),
                        label = { Text("Soundtrack Acoustic Prompt", color = NeonCyan) },
                        placeholder = { Text("Instrumentation, mood, rhythms, synths...") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // BPM Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tempo (BPM):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("$bpm BPM", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { viewModel.onMusicBpmChanged(it.toInt()) },
                        valueRange = 60f..180f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Generate Music Button
                    Button(
                        onClick = { viewModel.generateMusic() },
                        enabled = !isGenerating && musicPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_music_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepBackground, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synthesizing Audio via Lyria 3...", color = DeepBackground)
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = DeepBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Track (${durationConfig.model})", color = DeepBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Generated Tracks Library
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Synthesized Tracks Library (${tracks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }

            items(tracks, key = { it.id }) { track ->
                val isCurrentPlaying = playingTrackId == track.id
                TrackPlayerCard(
                    track = track,
                    isPlaying = isCurrentPlaying,
                    progress = if (isCurrentPlaying) playbackProgress else 0f,
                    onPlayToggle = { viewModel.playTrack(track) },
                    onExportToSynapse = {
                        notificationMessage = "Archived audio to SynapseFS: /vfs/audio_synthesizer/track_${track.id.take(6)}.wav"
                    }
                )
            }
        }
    }
}

@Composable
fun TrackPlayerCard(
    track: GeneratedMusicTrack,
    isPlaying: Boolean,
    progress: Float,
    onPlayToggle: () -> Unit,
    onExportToSynapse: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isPlaying) NeonCyan else BorderSubtle
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricBadge(
                        text = track.modelUsed,
                        textColor = NeonCyan,
                        backgroundColor = DeepBackground
                    )
                    MetricBadge(
                        text = "${track.bpm} BPM",
                        textColor = NeonPurple,
                        backgroundColor = DeepBackground
                    )
                }
                Text(
                    text = "${track.durationSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = track.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Waveform Spectrum Visualizer
            AudioWaveformVisualizer(
                isPlaying = isPlaying,
                progress = progress,
                activeColor = NeonCyan,
                seed = track.waveformSeed
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Playback controls & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayToggle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) ThreatCrimson else NeonCyan
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play Track",
                        tint = DeepBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "Pause" else "Play Music",
                        color = DeepBackground,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onExportToSynapse,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSecurity)
                ) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to SynapseFS", color = EmeraldSecurity, fontSize = 12.sp)
                }
            }
        }
    }
}
