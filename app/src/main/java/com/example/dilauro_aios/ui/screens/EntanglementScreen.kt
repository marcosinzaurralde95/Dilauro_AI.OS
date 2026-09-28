package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.components.TelemetryColumn
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntanglementScreen(
    viewModel: AiosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hut by viewModel.hutTelemetry.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HUT & Entanglement", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("Hardware Universal Translator & Distributed Computing", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("entangle_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.flushJitCache() }, modifier = Modifier.testTag("flush_jit_btn")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recompile JIT", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HUT (Hardware Universal Translator) Card
            item {
                CyberCard(borderColor = NeonCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HUT BINARY TRANSLATION PIPELINE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        MetricBadge(
                            text = "JIT ACTIVE",
                            textColor = EmeraldSecurity,
                            backgroundColor = Color(0xFF042F2E)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${hut.sourceArch}  ➔  ${hut.destArch}",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cross-architecture binary translator inspired by Rosetta 2 and WASI. Fast dynamic recompilation for legacy x86_64 binaries onto ARM devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryColumn(label = "TRANSLATED OPS", value = "${hut.translatedInstructionsTotal / 1000000}M")
                        TelemetryColumn(label = "CACHE HIT", value = "${hut.jitCacheHitRate}%")
                        TelemetryColumn(label = "LATENCY", value = "${hut.translationLatencyUs} μs")
                    }
                }
            }

            // Entanglement Protocol Mesh Nodes
            item {
                Text(
                    text = "Entanglement Mesh Nodes (${hut.connectedMeshPeers} Connected)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            // Node 1: Local Device
            item {
                MeshNodeCard(
                    nodeName = "Local Device (Host Node)",
                    architecture = "AArch64 (ARMv9) · 8 Cores",
                    role = "Primary UI & Orchestrator",
                    status = "ONLINE (This Node)",
                    statusColor = EmeraldSecurity,
                    latency = "0 ms",
                    capsulesCount = 3
                )
            }

            // Node 2: Raspberry Pi 5
            item {
                MeshNodeCard(
                    nodeName = "Dilauro Node-02 (RPi 5)",
                    architecture = "ARM Cortex-A76 · 4 Cores",
                    role = "Sensory & Telemetry Ingestion",
                    status = "ONLINE",
                    statusColor = EmeraldSecurity,
                    latency = "4.2 ms",
                    capsulesCount = 1
                )
            }

            // Node 3: Titan Workstation
            item {
                MeshNodeCard(
                    nodeName = "Dilauro Titan-Compute",
                    architecture = "x86_64 AVX-512 · 32 Cores",
                    role = "Heavy ML Weight Training & FAISS Search",
                    status = "ONLINE (Entangled)",
                    statusColor = NeonCyan,
                    latency = "11.8 ms",
                    capsulesCount = 2
                )
            }
        }
    }
}

@Composable
fun MeshNodeCard(
    nodeName: String,
    architecture: String,
    role: String,
    status: String,
    statusColor: Color,
    latency: String,
    capsulesCount: Int
) {
    CyberCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(statusColor, CircleShape))
                    Text(text = nodeName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Text(text = architecture, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = FontFamily.Monospace)
            }
            MetricBadge(text = status, textColor = statusColor, backgroundColor = CardSurfaceHighlight)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Role: $role", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontSize = 12.sp)
            Text(text = "Ping: $latency · $capsulesCount Capsules", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}
