package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.R
import com.example.dilauro_aios.ui.components.CapsuleSandboxDashboard
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.components.MlSchedulerDashboard
import com.example.dilauro_aios.ui.components.NeuralShieldStatusDashboard
import com.example.dilauro_aios.ui.components.SynapseFsTreeMapDashboard
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel

@Composable
fun DashboardScreen(
    viewModel: AiosViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val processes by viewModel.processes.collectAsState()
    val schedTele by viewModel.schedulerTelemetry.collectAsState()
    val memTele by viewModel.memoryTelemetry.collectAsState()
    val threatGauge by viewModel.threatGauge.collectAsState()
    val capsules by viewModel.capsules.collectAsState()
    val aiTele by viewModel.aiWorkloadTelemetry.collectAsState()
    val isRunning by viewModel.simulationRunning.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner HUD
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderCyan, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dilauro_banner),
                    contentDescription = "Dilauro AI-OS HUD Visualization",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, DeepBackground.copy(alpha = 0.85f)),
                                startY = 40f
                            )
                        )
                )
                // HUD Title & Status
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isRunning) EmeraldSecurity else WarningAmber, CircleShape)
                        )
                        Text(
                            text = if (isRunning) "D-KERNEL: ACTIVE" else "D-KERNEL: PAUSED",
                            color = if (isRunning) EmeraldSecurity else WarningAmber,
                            style = MaterialTheme.typography.labelSmall
                        )
                        MetricBadge(
                            text = "NO_STD MICROKERNEL",
                            textColor = NeonCyan,
                            backgroundColor = DeepBackground.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = "Dilauro AI-OS",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Adaptive AI-native OS · Rust Kernel · Liquid UI",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Control Simulation Button
                IconButton(
                    onClick = { viewModel.toggleSimulation() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .testTag("toggle_simulation_button")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Kernel Clock",
                        tint = NeonCyan
                    )
                }
            }
        }

        // Live HUD Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scheduler Metric
                CyberCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate("scheduler") },
                    borderColor = if (schedTele.isMlAugmented) NeonCyan.copy(alpha = 0.5f) else BorderSubtle
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ML SCHEDULER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (schedTele.isMlAugmented) NeonCyan else TextTertiary, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${schedTele.mlPredictionAccuracy.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = "${schedTele.contextSwitchesPerSec} sw/s · ${schedTele.avgLatencyMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }

                // DAM Memory Metric
                CyberCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate("memory") },
                    borderColor = when (memTele.pressure) {
                        com.example.dilauro_aios.model.MemoryPressure.NORMAL -> BorderSubtle
                        com.example.dilauro_aios.model.MemoryPressure.MODERATE -> WarningAmber.copy(alpha = 0.5f)
                        com.example.dilauro_aios.model.MemoryPressure.CRITICAL -> ThreatCrimson.copy(alpha = 0.5f)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DAM MEMORY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            memTele.pressure.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (memTele.pressure) {
                                com.example.dilauro_aios.model.MemoryPressure.NORMAL -> EmeraldSecurity
                                com.example.dilauro_aios.model.MemoryPressure.MODERATE -> WarningAmber
                                com.example.dilauro_aios.model.MemoryPressure.CRITICAL -> ThreatCrimson
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${memTele.allocatedMb} MB",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "WSS: ${memTele.workingSetMb}M · Cold: ${memTele.coldPagesMb}M",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }

        // Second Row: Neural Shield & Capsules
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Neural Shield Card
                CyberCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate("security") },
                    borderColor = if (threatGauge > 35f) ThreatCrimson.copy(alpha = 0.6f) else BorderSubtle
                ) {
                    Text("NEURAL SHIELD", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${threatGauge.toInt()}% Anomaly",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (threatGauge > 35f) ThreatCrimson else EmeraldSecurity
                    )
                    Text(
                        text = if (threatGauge > 35f) "Threat Intercepted" else "Zero-Trust IPC OK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }

                // WASI Capsules
                CyberCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate("capsules") }
                ) {
                    Text("WASI CAPSULES", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${capsules.count { it.status == com.example.dilauro_aios.model.CapsuleStatus.RUNNING }} Active",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonPurple
                    )
                    Text(
                        text = "${capsules.size} Deployed Micro-VMs",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }

        // Live D-Kernel ML Scheduler Dashboard (CPU Prioritization & AI Workload Balancing)
        item {
            MlSchedulerDashboard(
                viewModel = viewModel,
                isCompact = true,
                onViewFullScheduler = { onNavigate("scheduler") }
            )
        }

        // Live Neural Shield Status Dashboard (Active Protocols & Real-Time Privacy Alerts)
        item {
            NeuralShieldStatusDashboard(
                viewModel = viewModel,
                isCompact = true,
                onViewAllAlerts = { onNavigate("security") }
            )
        }

        // Live SynapseFS TreeMap Dashboard (Semantic File Indexing & Retrieval Patterns)
        item {
            SynapseFsTreeMapDashboard(
                viewModel = viewModel,
                isCompact = true,
                onViewFullFs = { onNavigate("synapse") }
            )
        }

        // Live Capsule Sandbox Dashboard (Running Isolated Containers & Termination)
        item {
            CapsuleSandboxDashboard(
                viewModel = viewModel,
                isCompact = true,
                onViewFullSandbox = { onNavigate("capsules") }
            )
        }

        // Neural Intelligence & Generative Studio Section
        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Neural Creative & Intelligence Lab",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Gemini 3 Vision · Audio Transcriber · Lyria 3 Music",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    TextButton(onClick = { onNavigate("generative_studio") }) {
                        Text("Open Studio", color = NeonCyan, fontSize = 12.sp)
                    }
                }
            }
        }

        // 1. Create & Edit Images Card
        item {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("image_studio") }
                    .testTag("nav_card_image_studio"),
                borderColor = BorderCyan
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Create & Edit Images", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Create & Edit Images", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            MetricBadge(text = "GEMINI-3", textColor = NeonCyan, backgroundColor = DeepBackground)
                        }
                        Text(
                            "Text-to-image prompts & interactive image-to-image refinement with aspect ratios and resolutions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        // 2. Transcribe Audio Card
        item {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("audio_transcribe") }
                    .testTag("nav_card_audio_transcribe"),
                borderColor = NeonPurple.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(NeonPurple.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, NeonPurple, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Transcribe Audio", tint = NeonPurple, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Transcribe Audio", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            MetricBadge(text = "GEMINI-3.5", textColor = NeonPurple, backgroundColor = DeepBackground)
                        }
                        Text(
                            "Real-time voice memo recording, acoustic telemetry decoding, summaries, and action item extraction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        // 3. Generate Music Card
        item {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("music_generator") }
                    .testTag("nav_card_music_gen"),
                borderColor = EmeraldSecurity.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(EmeraldSecurity.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, EmeraldSecurity, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Generate Music", tint = EmeraldSecurity, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Generate Music", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            MetricBadge(text = "LYRIA-3", textColor = EmeraldSecurity, backgroundColor = DeepBackground)
                        }
                        Text(
                            "Synthesize clips and tracks using lyria-3-clip-preview and lyria-3-pro-preview with live waveform equalizer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        // 4. Gemini Chatbot Card
        item {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("gemini_chat") }
                    .testTag("nav_card_gemini_chat"),
                borderColor = NeonCyan.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "Gemini Copilot", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Gemini Neural Copilot", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            MetricBadge(text = "MULTI-TURN", textColor = NeonCyan, backgroundColor = DeepBackground)
                        }
                        Text(
                            "Multi-turn conversational assistant with role-based system instructions (gemini-3.1-pro, 3.5-flash, 3.1-lite).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        // 5. Veo 3 Video Generation & Photo Animation Card
        item {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("veo_video") }
                    .testTag("nav_card_veo_video"),
                borderColor = ElectricViolet.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(ElectricViolet.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, ElectricViolet, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Veo Video", tint = ElectricViolet, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Veo 3 Video & Animation", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            MetricBadge(text = "VEO-3.1", textColor = ElectricViolet, backgroundColor = DeepBackground)
                        }
                        Text(
                            "Text-to-video & photo animation in 16:9 and 9:16 using veo-3.1-fast-generate-preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        // Subsystems Section
        item {
            Text(
                text = "OS Subsystems & Architecture",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            SubsystemNavCard(
                title = "D-Kernel Adaptive Scheduler",
                tag = "RFC-0001",
                description = "ML-driven preemptive process scheduler. Telemetry ring buffers, historical CPU burst prediction, dynamic priority rebalancing.",
                icon = Icons.Default.Tune,
                badgeColor = NeonCyan,
                onClick = { onNavigate("scheduler") }
            )
        }

        item {
            SubsystemNavCard(
                title = "DAM-Engine (Dynamic Adaptive Memory)",
                tag = "MEMORY OPTIMIZER",
                description = "Real-time working set estimation, predictive cold page pre-eviction, zRAM compaction, and memory pressure signaling.",
                icon = Icons.Default.Memory,
                badgeColor = ElectricViolet,
                onClick = { onNavigate("memory") }
            )
        }

        item {
            SubsystemNavCard(
                title = "SynapseFS Semantic Filesystem",
                tag = "VECTOR FS",
                description = "Context-aware filesystem with FAISS dense vector search, rich contextual metadata, temporal affinity, and prefetching.",
                icon = Icons.Default.FolderSpecial,
                badgeColor = EmeraldSecurity,
                onClick = { onNavigate("synapse") }
            )
        }

        item {
            SubsystemNavCard(
                title = "Capsules (WASI Runtime)",
                tag = "MICRO-CONTAINERS",
                description = "WebAssembly container runtime with capability-based security tokens, live hot-swapping, and zero ambient authority.",
                icon = Icons.Default.Apps,
                badgeColor = NeonPurple,
                onClick = { onNavigate("capsules") }
            )
        }

        item {
            SubsystemNavCard(
                title = "Neural Shield Heuristic Security",
                tag = "ML SENTRY",
                description = "eBPF syscall anomaly detection, behavioral baseline learning, zero-trust IPC audits, and real-time quarantine.",
                icon = Icons.Default.Security,
                badgeColor = ThreatCrimson,
                onClick = { onNavigate("security") }
            )
        }

        item {
            SubsystemNavCard(
                title = "HUT & Entanglement Protocol",
                tag = "BINARY TRANSLATOR & MESH",
                description = "Hardware Universal Translator (x86_64 to ARM64 JIT) and distributed multi-device compute mesh hand-off.",
                icon = Icons.Default.Hub,
                badgeColor = TextCyan,
                onClick = { onNavigate("entanglement") }
            )
        }

        item {
            SubsystemNavCard(
                title = "Neural Vision & Image Editor",
                tag = "GEMINI-3 MULTIMODAL",
                description = "Prompt-based visual asset synthesis and interactive image-to-image refinement with aspect ratio scaling.",
                icon = Icons.Default.AutoFixHigh,
                badgeColor = NeonCyan,
                onClick = { onNavigate("image_studio") }
            )
        }

        item {
            SubsystemNavCard(
                title = "Acoustic Intel & Transcriber",
                tag = "GEMINI-3.5 ACOUSTIC",
                description = "Microphone voice memos, audio telemetry decoding, automatic executive summaries, and action item extraction.",
                icon = Icons.Default.Mic,
                badgeColor = NeonPurple,
                onClick = { onNavigate("audio_transcribe") }
            )
        }

        item {
            SubsystemNavCard(
                title = "Neural Music Synthesizer",
                tag = "LYRIA-3 ENGINE",
                description = "AI soundtrack synthesis using lyria-3-clip-preview and lyria-3-pro-preview with live waveform equalizer.",
                icon = Icons.Default.MusicNote,
                badgeColor = EmeraldSecurity,
                onClick = { onNavigate("music_generator") }
            )
        }
    }
}

@Composable
fun SubsystemNavCard(
    title: String,
    tag: String,
    description: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    CyberCard(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("nav_${title.take(10).lowercase().replace(" ", "_")}"),
        borderColor = BorderSubtle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    MetricBadge(
                        text = tag,
                        textColor = badgeColor,
                        backgroundColor = badgeColor.copy(alpha = 0.12f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
