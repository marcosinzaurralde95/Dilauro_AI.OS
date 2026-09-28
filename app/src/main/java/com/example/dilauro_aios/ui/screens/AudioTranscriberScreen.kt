package com.example.dilauro_aios.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.AudioSamplePreset
import com.example.dilauro_aios.model.TranscriptionResult
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.GenerativeStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTranscriberScreen(
    viewModel: GenerativeStudioViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val transcriptions by viewModel.transcriptions.collectAsState()
    val selectedResult by viewModel.selectedTranscription.collectAsState()
    val samplePresets = viewModel.audioSamplePresets
    val isKeySet = viewModel.isKeyConfigured

    var permissionGranted by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            viewModel.startMicrophoneRecording()
        } else {
            notificationMessage = "Microphone permission required for voice recording."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Audio Transcriber", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            MetricBadge(
                                text = "GEMINI-3.5",
                                textColor = NeonCyan,
                                backgroundColor = DeepBackground
                            )
                        }
                        Text(
                            "Voice Intel & Acoustic Telemetry Transcription",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("audio_transcriber_back_button")) {
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
                                Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldSecurity)
                                Text(msg, color = EmeraldSecurity, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Real-Time Audio Recorder Control Deck
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isRecording) ThreatCrimson else BorderCyan
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRecording) "LIVE ACOUSTIC STREAMING..." else "MICROPHONE VOICE INTEL",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isRecording) ThreatCrimson else NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Duration Display
                        val minutes = recordingDuration / 60
                        val seconds = recordingDuration % 60
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (isRecording) ThreatCrimson else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big Mic Action Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) ThreatCrimson else NeonCyan)
                                .border(
                                    3.dp,
                                    if (isRecording) ThreatCrimson.copy(alpha = 0.6f) else BorderCyan,
                                    CircleShape
                                )
                                .clickable {
                                    if (isRecording) {
                                        viewModel.stopMicrophoneRecordingAndTranscribe()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                                .testTag("record_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTranscribing) {
                                CircularProgressIndicator(color = DeepBackground, modifier = Modifier.size(32.dp))
                            } else {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                    tint = DeepBackground,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isRecording) "Tap to Stop & Transcribe via Gemini" else "Tap Mic to Record Live Voice Memo",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Quick Audio Dispatch Presets
            item {
                Text(
                    text = "System Audio Dispatch Memos",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Tap any telemetry recording to transcribe immediately",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            items(samplePresets, key = { it.id }) { preset ->
                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.transcribeSamplePreset(preset) },
                    borderColor = BorderSubtle
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(DeepBackground, CircleShape)
                                .border(1.dp, BorderCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                "${preset.category} · ${preset.durationText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = { viewModel.transcribeSamplePreset(preset) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Transcribe", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Transcriptions History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Decoded Intel Transcripts (${transcriptions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }

            items(transcriptions, key = { it.id }) { item ->
                TranscriptionCard(
                    item = item,
                    onCopyToClipboard = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Voice Transcript", item.rawTranscription)
                        clipboard.setPrimaryClip(clip)
                        notificationMessage = "Transcript copied to clipboard!"
                    },
                    onSaveToSynapseFs = {
                        notificationMessage = "Archived transcript to SynapseFS: /vfs/voice_intel/transcript_${item.id.take(6)}.md"
                    }
                )
            }
        }
    }
}

@Composable
fun TranscriptionCard(
    item: TranscriptionResult,
    onCopyToClipboard: () -> Unit,
    onSaveToSynapseFs: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when (item.urgencyLevel) {
            "KERNEL CRITICAL", "HIGH" -> ThreatCrimson.copy(alpha = 0.6f)
            else -> BorderCyan.copy(alpha = 0.5f)
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricBadge(
                        text = item.modelUsed,
                        textColor = NeonCyan,
                        backgroundColor = DeepBackground
                    )
                    MetricBadge(
                        text = item.sentiment,
                        textColor = if (item.sentiment.contains("CRITICAL")) ThreatCrimson else EmeraldSecurity,
                        backgroundColor = DeepBackground
                    )
                }
                Text(
                    text = sdf.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Raw Transcription Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DeepBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.rawTranscription,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextCyan,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Executive Summary
            Text("Executive Summary:", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(item.executiveSummary, style = MaterialTheme.typography.bodySmall, color = TextPrimary)

            Spacer(modifier = Modifier.height(8.dp))

            // Action Items
            if (item.actionItems.isNotEmpty()) {
                Text("Action Directives:", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                item.actionItems.forEach { action ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", color = NeonCyan, fontWeight = FontWeight.Bold)
                        Text(action, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyToClipboard,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", color = TextSecondary, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onSaveToSynapseFs,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSecurity)
                ) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to SynapseFS", color = EmeraldSecurity, fontSize = 12.sp)
                }
            }
        }
    }
}
