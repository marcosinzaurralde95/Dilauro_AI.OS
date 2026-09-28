package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.dilauro_aios.model.Process
import com.example.dilauro_aios.model.ProcessState
import com.example.dilauro_aios.model.ProcessType
import com.example.dilauro_aios.ui.components.BurstRingBufferGraph
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.components.ProcessTypeBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    viewModel: AiosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val processes by viewModel.processes.collectAsState()
    val tele by viewModel.schedulerTelemetry.collectAsState()

    var showSpawnDialog by remember { mutableStateOf(false) }
    var selectedPcb by remember { mutableStateOf<Process?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "D-Kernel Scheduler",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "RFC-0001 ML-Driven Process Orchestrator",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("scheduler_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Overview",
                            tint = NeonCyan
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSpawnDialog = true },
                        modifier = Modifier.testTag("spawn_process_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Spawn Process",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBackground
                )
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
            // ML vs Traditional Switch Card
            item {
                CyberCard(
                    borderColor = if (tele.isMlAugmented) NeonCyan else BorderSubtle,
                    backgroundColor = DarkSurface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (tele.isMlAugmented) "ML-Augmented Scheduling" else "Standard Round-Robin",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (tele.isMlAugmented) NeonCyan else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (tele.isMlAugmented) NeonCyan else WarningAmber, CircleShape)
                                )
                            }
                            Text(
                                text = if (tele.isMlAugmented)
                                    "Dynamic burst prediction & latency-sensitive preemption enabled"
                                else
                                    "Static time-slice CFS emulation without telemetry feedback",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = tele.isMlAugmented,
                            onCheckedChange = { viewModel.toggleMlScheduler() },
                            modifier = Modifier.testTag("toggle_ml_scheduler_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepBackground,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = CardSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live telemetry row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryColumn(label = "CYCLES", value = "${tele.schedulingCycles}")
                        TelemetryColumn(label = "CTX SWITCH", value = "${tele.contextSwitchesPerSec}/s")
                        TelemetryColumn(label = "AVG LATENCY", value = String.format("%.2f ms", tele.avgLatencyMs))
                        TelemetryColumn(
                            label = "ML ACCURACY",
                            value = if (tele.isMlAugmented) String.format("%.1f%%", tele.mlPredictionAccuracy) else "N/A"
                        )
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Process Queue (${processes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ring buffer length: 5 bursts",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            // Process Items List
            items(processes, key = { it.pid }) { proc ->
                ProcessCard(
                    process = proc,
                    isMlOn = tele.isMlAugmented,
                    onSelect = { selectedPcb = proc },
                    onBoost = { viewModel.boostPriority(proc.pid) },
                    onKill = { viewModel.terminateProcess(proc.pid) }
                )
            }
        }
    }

    // Process PCB Detail Sheet
    if (selectedPcb != null) {
        AlertDialog(
            onDismissRequest = { selectedPcb = null },
            confirmButton = {
                TextButton(onClick = { selectedPcb = null }) {
                    Text("Close", color = NeonCyan)
                }
            },
            title = {
                Text(
                    text = "PCB: ${selectedPcb?.name} (PID ${selectedPcb?.pid})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type: ${selectedPcb?.type?.name}", color = TextSecondary)
                    Text("Static Priority: ${selectedPcb?.priority}/10", color = TextSecondary)
                    Text("Dynamic ML Score: ${String.format("%.3f", selectedPcb?.dynamicScore ?: 0f)}", color = NeonCyan)
                    Text("Predicted Next Burst: ${selectedPcb?.predictedBurstMs} ms", color = TextSecondary)
                    Text("Total CPU Consumed: ${selectedPcb?.cpuTimeUsedMs} ms", color = TextSecondary)
                    Text("Context Switches: ${selectedPcb?.contextSwitches}", color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Telemetry Ring Buffer (ms):", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(
                        text = selectedPcb?.cpuBurstHistory?.joinToString(" → ") ?: "[]",
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            },
            containerColor = CardSurface
        )
    }

    // Spawn Process Dialog
    if (showSpawnDialog) {
        var procName by remember { mutableStateOf("") }
        var procType by remember { mutableStateOf(ProcessType.INTERACTIVE) }
        var procPrio by remember { mutableFloatStateOf(6f) }

        AlertDialog(
            onDismissRequest = { showSpawnDialog = false },
            title = { Text("Spawn Capsule Task", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = procName,
                        onValueChange = { procName = it },
                        label = { Text("Process / Task Name") },
                        placeholder = { Text("e.g. inference_worker") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("spawn_name_input")
                    )

                    Text("Process Classification:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProcessType.values().forEach { t ->
                            FilterChip(
                                selected = procType == t,
                                onClick = { procType = t },
                                label = { Text(t.name.take(4), fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Priority: ${procPrio.toInt()}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Slider(
                        value = procPrio,
                        onValueChange = { procPrio = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.spawnProcess(procName, procType, procPrio.toInt())
                        showSpawnDialog = false
                    },
                    modifier = Modifier.testTag("confirm_spawn_button")
                ) {
                    Text("Spawn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpawnDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
fun ProcessCard(
    process: Process,
    isMlOn: Boolean,
    onSelect: () -> Unit,
    onBoost: () -> Unit,
    onKill: () -> Unit
) {
    val isRunning = process.state == ProcessState.RUNNING

    CyberCard(
        modifier = Modifier
            .clickable(onClick = onSelect)
            .testTag("process_card_${process.pid}"),
        borderColor = if (isRunning) NeonCyan else BorderSubtle,
        backgroundColor = if (isRunning) CardSurfaceHighlight else CardSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (isRunning) NeonCyan else TextTertiary, CircleShape)
                )
                Text(
                    text = process.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "PID ${process.pid}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace
                )
            }
            ProcessTypeBadge(type = process.type)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ring buffer graph
        BurstRingBufferGraph(
            bursts = process.cpuBurstHistory,
            activeColor = if (isRunning) NeonCyan else ElectricViolet,
            baseColor = CardSurfaceHighlight
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Telemetry details & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Priority: ${process.priority}/10 ${if (isMlOn) "· Score: ${String.format("%.2f", process.dynamicScore)}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "Pred Burst: ${process.predictedBurstMs}ms · CPU: ${process.cpuTimeUsedMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onBoost,
                    modifier = Modifier.size(36.dp).testTag("boost_proc_${process.pid}")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowUp,
                        contentDescription = "Boost Priority",
                        tint = EmeraldSecurity,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onKill,
                    modifier = Modifier.size(36.dp).testTag("kill_proc_${process.pid}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Terminate Task",
                        tint = ThreatCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryColumn(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}
