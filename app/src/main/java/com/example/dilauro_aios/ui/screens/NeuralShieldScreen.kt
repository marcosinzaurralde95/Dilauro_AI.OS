package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.SecurityThreat
import com.example.dilauro_aios.model.ThreatAction
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.components.NeuralShieldStatusDashboard
import com.example.dilauro_aios.ui.components.ThreatSeverityBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralShieldScreen(
    viewModel: AiosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val threats by viewModel.securityThreats.collectAsState()
    val threatGauge by viewModel.threatGauge.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Neural Shield Sentinel",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Active Security Protocols & Real-Time Privacy Monitor",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("shield_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearThreatAudit() }, modifier = Modifier.testTag("clear_threats_btn")) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Clear Audit", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Sub-Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepBackground,
                contentColor = NeonCyan,
                divider = { HorizontalDivider(color = BorderSubtle) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Status & Privacy Alerts",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_shield_status")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Syscall Firewall & eBPF",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                            if (threats.isNotEmpty()) {
                                MetricBadge(text = "${threats.size}", textColor = ThreatCrimson, backgroundColor = Color(0xFF450A0A))
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_shield_syscalls")
                )
            }

            when (selectedTab) {
                0 -> {
                    // MAIN STATUS DASHBOARD TAB: Monitors active security protocols & real-time privacy alerts
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            NeuralShieldStatusDashboard(
                                viewModel = viewModel,
                                isCompact = false
                            )
                        }
                    }
                }
                1 -> {
                    // SYSCALL FIREWALL & BENCHMARK TAB
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Heuristic Threat Level
                        item {
                            val isElevated = threatGauge > 35f
                            CyberCard(
                                borderColor = if (isElevated) ThreatCrimson else EmeraldSecurity,
                                backgroundColor = DarkSurface
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("HEURISTIC SYSCALL ANOMALY DETECTOR", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    MetricBadge(
                                        text = if (isElevated) "STATE: ANOMALY MITIGATED" else "STATE: NOMINAL",
                                        textColor = if (isElevated) ThreatCrimson else EmeraldSecurity,
                                        backgroundColor = if (isElevated) Color(0xFF450A0A) else Color(0xFF042F2E)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "${threatGauge.toInt()}% Anomaly Probability",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (isElevated) ThreatCrimson else EmeraldSecurity,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (threatGauge / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = if (isElevated) ThreatCrimson else EmeraldSecurity,
                                    trackColor = CardSurfaceHighlight
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "eBPF in-kernel telemetry monitors all sys_write, sys_mprotect, and socket invocations with under 15μs decision overhead.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Benchmark Injection Card
                        item {
                            CyberCard {
                                Text("TRIGGER THREAT SIMULATION BENCHMARK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Inject malicious anomaly vectors to verify eBPF automated quarantine & SFI sandbox containment.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.simulateThreatVector("SYSCALL_ANOMALY") },
                                        modifier = Modifier.weight(1f).testTag("sim_threat_syscall"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson.copy(alpha = 0.8f))
                                    ) {
                                        Text("Syscall Spike", fontSize = 11.sp, color = TextPrimary)
                                    }

                                    Button(
                                        onClick = { viewModel.simulateThreatVector("MEMORY_SANDBOX_BREACH") },
                                        modifier = Modifier.weight(1f).testTag("sim_threat_mem"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
                                    ) {
                                        Text("Bounds Breach", fontSize = 11.sp, color = TextPrimary)
                                    }

                                    Button(
                                        onClick = { viewModel.simulateThreatVector("IPC_SPOOFING") },
                                        modifier = Modifier.weight(1f).testTag("sim_threat_ipc"),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber.copy(alpha = 0.8f))
                                    ) {
                                        Text("Token Spoof", fontSize = 11.sp, color = DeepBackground)
                                    }
                                }
                            }
                        }

                        // Threat Audit Trail List
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Intercepted Syscall Attacks (${threats.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Zero Ambient Authority", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                        }

                        items(threats, key = { it.id }) { threat ->
                            ThreatEventCard(threat = threat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatEventCard(threat: SecurityThreat) {
    CyberCard(
        modifier = Modifier.testTag("threat_card_${threat.id}"),
        borderColor = when (threat.actionTaken) {
            ThreatAction.QUARANTINED -> ThreatCrimson
            ThreatAction.THROTTLED -> WarningAmber
            ThreatAction.FLAGGED -> ElectricViolet
            ThreatAction.ALLOWED -> BorderSubtle
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = threat.sourceCapsule,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = threat.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Syscall: ${threat.syscallName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace
                )
            }
            ThreatSeverityBadge(severity = threat.severity)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = threat.details,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Anomaly Score: ${String.format("%.2f", threat.anomalyScore)}",
                style = MaterialTheme.typography.labelSmall,
                color = ThreatCrimson,
                fontWeight = FontWeight.Bold
            )

            MetricBadge(
                text = "ACTION: ${threat.actionTaken.name}",
                textColor = when (threat.actionTaken) {
                    ThreatAction.QUARANTINED -> ThreatCrimson
                    ThreatAction.THROTTLED -> WarningAmber
                    ThreatAction.FLAGGED -> TextCyan
                    ThreatAction.ALLOWED -> EmeraldSecurity
                },
                backgroundColor = CardSurfaceHighlight
            )
        }
    }
}
