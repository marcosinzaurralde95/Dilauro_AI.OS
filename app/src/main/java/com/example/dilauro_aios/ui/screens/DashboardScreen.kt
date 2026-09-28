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
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
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
