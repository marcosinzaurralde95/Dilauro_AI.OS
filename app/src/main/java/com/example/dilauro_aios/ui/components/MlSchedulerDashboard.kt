package com.example.dilauro_aios.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.*
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * High-Performance D-Kernel ML Scheduler UI Component.
 * Visualizes real-time CPU task prioritization and AI workload balancing
 * according to D-Kernel RFC-0001 (Adaptive Scheduler Design).
 */
@Composable
fun MlSchedulerDashboard(
    viewModel: AiosViewModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onViewFullScheduler: (() -> Unit)? = null
) {
    val processes by viewModel.processes.collectAsState()
    val tele by viewModel.schedulerTelemetry.collectAsState()
    val cpuCores by viewModel.cpuCores.collectAsState()
    val aiTele by viewModel.aiWorkloadTelemetry.collectAsState()

    var showCoreAssignDialog by remember { mutableStateOf<Process?>(null) }
    var inspectedPcb by remember { mutableStateOf<Process?>(null) }
    var showAiInjectMenu by remember { mutableStateOf(false) }
    var selectedFilterType by remember { mutableStateOf<ProcessType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isRebalancing by remember { mutableStateOf(false) }
    var rebalanceMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ml_scheduler_dashboard_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. SCHEDULER POLICY & MASTER HUD
        // ==========================================
        CyberCard(
            borderColor = if (tele.isMlAugmented) NeonCyan.copy(alpha = 0.7f) else BorderSubtle,
            backgroundColor = DarkSurface,
            modifier = Modifier.testTag("ml_scheduler_hud_card")
        ) {
            // Header Row: Title & Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "beacon_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (tele.isMlAugmented) NeonCyan.copy(alpha = pulseAlpha)
                                else WarningAmber.copy(alpha = pulseAlpha)
                            )
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "D-KERNEL ML SCHEDULER",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            MetricBadge(
                                text = "RFC-0001",
                                textColor = NeonCyan,
                                backgroundColor = CardSurfaceHighlight
                            )
                        }
                        Text(
                            text = if (tele.isMlAugmented) "Reinforcement Learning Active · Latency Prediction"
                            else "Standard Linux-Style CFS Round-Robin Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tele.isMlAugmented) NeonCyan.copy(alpha = 0.8f) else TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Toggle Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (tele.isMlAugmented) "ML ACTIVE" else "CFS MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tele.isMlAugmented) NeonCyan else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = tele.isMlAugmented,
                        onCheckedChange = { viewModel.toggleMlScheduler() },
                        modifier = Modifier.testTag("toggle_ml_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepBackground,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = CardSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Workload Policy Modes Strip
            Text(
                text = "SCHEDULING PROFILE & AI BALANCING GOAL",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    SchedulerPolicyMode.BALANCED_ADAPTIVE to "Balanced Adaptive",
                    SchedulerPolicyMode.AI_TURBO_INFERENCE to "AI Turbo (NPU Max)",
                    SchedulerPolicyMode.ULTRA_LOW_LATENCY to "Low Latency (Sub-ms)",
                    SchedulerPolicyMode.EFFICIENCY_SAVER to "Efficiency Thermal"
                ).forEach { (policy, label) ->
                    val isSelected = tele.policyMode == policy
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSchedulerPolicy(policy) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("policy_chip_${policy.name.lowercase()}"),
                        leadingIcon = {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = NeonCyan)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (policy == SchedulerPolicyMode.AI_TURBO_INFERENCE) Color(0xFF581C87) else NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = if (policy == SchedulerPolicyMode.AI_TURBO_INFERENCE) Color(0xFFE9D5FF) else NeonCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            // Key Telemetry KPI Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    label = "PRED ACCURACY",
                    value = if (tele.isMlAugmented) String.format("%.1f%%", tele.mlPredictionAccuracy) else "CFS (0%)",
                    color = if (tele.isMlAugmented) NeonCyan else TextTertiary
                )
                TelemetryMetricItem(
                    label = "AI WORKLOAD",
                    value = String.format("%.1f TFLOPS", aiTele.tensorTflops),
                    color = Color(0xFFC084FC)
                )
                TelemetryMetricItem(
                    label = "AVG DISPATCH",
                    value = String.format("%.2f ms", tele.avgLatencyMs),
                    color = EmeraldSecurity
                )
                TelemetryMetricItem(
                    label = "NPU LOAD",
                    value = "${aiTele.npuUtilizationPercent.toInt()}%",
                    color = WarningAmber
                )
                TelemetryMetricItem(
                    label = "CYCLES",
                    value = "#${tele.schedulingCycles}",
                    color = TextPrimary
                )
            }
        }

        // ==========================================
        // 2. REAL-TIME DISPATCH TIMELINE PIPELINE
        // ==========================================
        CyberCard(
            borderColor = BorderCyan.copy(alpha = 0.5f),
            modifier = Modifier.testTag("dispatch_timeline_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Real-Time CPU Dispatch Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Multi-core quantum time slices & preemption points",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CardSurfaceHighlight)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "16ms QUANTA",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Dispatch Timeline Canvas
            CpuDispatchTimelineCanvas(
                cores = cpuCores,
                processes = processes,
                cycleCount = tele.schedulingCycles
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timeline Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = Color(0xFFC084FC), label = "AI Inference")
                LegendDot(color = NeonCyan, label = "Interactive")
                LegendDot(color = EmeraldSecurity, label = "Daemon")
                LegendDot(color = WarningAmber, label = "Batch")
                LegendDot(color = ThreatCrimson, label = "Preempted")
            }
        }

        // ==========================================
        // 3. AI WORKLOAD BALANCING & CLUSTER TOPOLOGY
        // ==========================================
        CyberCard(modifier = Modifier.testTag("ai_workload_balancer_card")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Heterogeneous Cluster & AI Balancer",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        MetricBadge(
                            text = "${aiTele.activeAiPipelinesCount} Pipelines",
                            textColor = Color(0xFFC084FC),
                            backgroundColor = Color(0xFF3B0764)
                        )
                    }
                    Text(
                        text = "Dynamic load steering between Cortex-X4 and NPU Tensor Engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Rebalance Action Button
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                isRebalancing = true
                                viewModel.rebalanceAiWorkloads()
                                delay(600)
                                isRebalancing = false
                                rebalanceMessage = "Rebalanced: Tensors pinned to NPU, IPC to Cortex-X4"
                                delay(3000)
                                rebalanceMessage = null
                            }
                        },
                        enabled = !isRebalancing,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ElectricViolet.copy(alpha = 0.25f),
                            contentColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("rebalance_cluster_btn")
                    ) {
                        if (isRebalancing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = NeonCyan
                            )
                        } else {
                            Icon(Icons.Default.Balance, contentDescription = "Rebalance", modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRebalancing) "Balancing..." else "Rebalance", fontSize = 11.sp)
                    }

                    // Inject AI Task Button
                    Box {
                        FilledTonalButton(
                            onClick = { showAiInjectMenu = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF4C1D95).copy(alpha = 0.6f),
                                contentColor = Color(0xFFE9D5FF)
                            ),
                            modifier = Modifier.testTag("inject_ai_task_btn")
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "Inject AI", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inject AI", fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = showAiInjectMenu,
                            onDismissRequest = { showAiInjectMenu = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("LLM KV-Cache Prefill (6.4 TFLOPS)", color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.injectAiWorkload("LLM_KV_PREFILL")
                                    showAiInjectMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Diffusion UNet Denoise (7.1 TFLOPS)", color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.injectAiWorkload("DIFFUSION_DENOISE")
                                    showAiInjectMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = ThreatCrimson, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("FAISS Vector MatMul (4.8 TFLOPS)", color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.injectAiWorkload("FAISS_MATMUL")
                                    showAiInjectMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Vision Impeller Tokenizer (5.2 TFLOPS)", color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.injectAiWorkload("VISION_TOKENIZER")
                                    showAiInjectMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = rebalanceMessage != null) {
                Surface(
                    color = EmeraldSecurity.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSecurity.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(14.dp))
                        Text(rebalanceMessage ?: "", color = EmeraldSecurity, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Tensor vs Vector Workload Ratio Bar
            AiWorkloadDistributionBar(
                npuPercent = aiTele.npuUtilizationPercent,
                activeTflops = aiTele.tensorTflops
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cores Grid (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cpuCores.take(2).forEach { core ->
                    CpuCoreCard(core = core, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cpuCores.drop(2).take(2).forEach { core ->
                    CpuCoreCard(core = core, modifier = Modifier.weight(1f))
                }
            }
        }

        // ==========================================
        // 4. REAL-TIME TASK PRIORITIZATION QUEUE
        // ==========================================
        CyberCard(modifier = Modifier.testTag("task_prioritization_queue_card")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Real-Time Task Prioritization Queue",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        MetricBadge(
                            text = "${processes.size} PCBs",
                            textColor = NeonCyan,
                            backgroundColor = CardSurfaceHighlight
                        )
                    }
                    Text(
                        text = "ML burst predictor dynamically reorders dispatch deadlines",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                if (isCompact && onViewFullScheduler != null) {
                    TextButton(
                        onClick = onViewFullScheduler,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.testTag("view_full_scheduler_btn")
                    ) {
                        Text("All Tasks", color = NeonCyan, fontSize = 11.sp)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs & Search
            if (!isCompact) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterType == null,
                        onClick = { selectedFilterType = null },
                        label = { Text("All (${processes.size})", fontSize = 10.sp) },
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                    ProcessType.values().forEach { type ->
                        val count = processes.count { it.type == type }
                        FilterChip(
                            selected = selectedFilterType == type,
                            onClick = { selectedFilterType = if (selectedFilterType == type) null else type },
                            label = { Text("${type.name.take(4)} ($count)", fontSize = 10.sp) },
                            modifier = Modifier.testTag("filter_chip_${type.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search task name or PID...", fontSize = 12.sp, color = TextTertiary) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scheduler_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Filtered & Sorted Tasks by dynamicScore
            val filteredTasks = processes
                .filter { p -> selectedFilterType == null || p.type == selectedFilterType }
                .filter { p -> searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true) || p.pid.toString().contains(searchQuery) }
                .sortedByDescending { it.dynamicScore }

            val tasksToDisplay = if (isCompact) filteredTasks.take(4) else filteredTasks

            if (tasksToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks match current filter.", color = TextTertiary, fontSize = 12.sp)
                }
            } else {
                tasksToDisplay.forEach { proc ->
                    PrioritizedTaskItem(
                        process = proc,
                        isMlOn = tele.isMlAugmented,
                        onAssignCore = { showCoreAssignDialog = proc },
                        onBoost = { viewModel.boostPriority(proc.pid) },
                        onPreempt = { viewModel.preemptTaskNow(proc.pid) },
                        onInspectPcb = { inspectedPcb = proc },
                        onPriorityChange = { newPrio -> viewModel.updateTaskPriority(proc.pid, newPrio) },
                        onKill = { viewModel.terminateProcess(proc.pid) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // ==========================================
    // 5. DIALOG: Assign Core Affinity
    // ==========================================
    if (showCoreAssignDialog != null) {
        val p = showCoreAssignDialog!!
        AlertDialog(
            onDismissRequest = { showCoreAssignDialog = null },
            confirmButton = {
                TextButton(
                    onClick = { showCoreAssignDialog = null },
                    modifier = Modifier.testTag("core_assign_done_btn")
                ) {
                    Text("Done", color = NeonCyan)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AltRoute, contentDescription = null, tint = NeonCyan)
                    Text("Core Affinity: ${p.name}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Migrate PID ${p.pid} to target heterogeneous compute core:", color = TextSecondary, fontSize = 12.sp)
                    cpuCores.forEach { core ->
                        val isSelected = p.assignedCoreId == core.coreId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.18f) else CardSurfaceHighlight)
                                .border(1.dp, if (isSelected) NeonCyan else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.assignProcessToCore(p.pid, core.coreId)
                                    showCoreAssignDialog = null
                                }
                                .padding(12.dp)
                                .testTag("select_core_${core.coreId}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        core.name,
                                        color = if (isSelected) NeonCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (core.isTensorCore) {
                                        MetricBadge("NPU", textColor = Color(0xFFC084FC), backgroundColor = Color(0xFF3B0764))
                                    }
                                }
                                Text("${core.type} · ${core.frequencyGhz} GHz · ${core.loadPercent.toInt()}% Load", color = TextTertiary, fontSize = 10.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Assigned", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            containerColor = DarkSurface
        )
    }

    // ==========================================
    // 6. DIALOG: Deep PCB Inspector (RFC-0001)
    // ==========================================
    if (inspectedPcb != null) {
        val p = inspectedPcb!!
        AlertDialog(
            onDismissRequest = { inspectedPcb = null },
            confirmButton = {
                Button(
                    onClick = { inspectedPcb = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.testTag("close_pcb_dialog_btn")
                ) {
                    Text("Close Inspector", color = DeepBackground, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = NeonCyan)
                    Column {
                        Text("PCB Inspector: ${p.name}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Process Control Block (RFC-0001 Descriptor)", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PcbField(label = "PID", value = p.pid.toString(), color = NeonCyan)
                        PcbField(label = "STATE", value = p.state.name, color = if (p.state == ProcessState.RUNNING) EmeraldSecurity else TextPrimary)
                        PcbField(label = "TYPE", value = p.type.name, color = Color(0xFFC084FC))
                        PcbField(label = "AFFINITY", value = "Core ${p.assignedCoreId}", color = WarningAmber)
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // ML Scheduling Telemetry
                    Text("D-KERNEL ML SCHEDULER ATTRIBUTES", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PcbField(label = "DYNAMIC SCORE", value = String.format("%.3f", p.dynamicScore), color = NeonCyan)
                        PcbField(label = "PRED BURST", value = "${p.predictedBurstMs} ms", color = TextPrimary)
                        PcbField(label = "USED CPU", value = "${p.cpuTimeUsedMs} ms", color = TextPrimary)
                        PcbField(label = "CTX SWITCHES", value = p.contextSwitches.toString(), color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PcbField(label = "QUANTUM TIME SLICE", value = "${p.quantumTimeSliceMs} ms", color = TextSecondary)
                        PcbField(label = "PREEMPTION DEADLINE", value = "${p.preemptionDeadlineMs} ms", color = WarningAmber)
                        PcbField(label = "TENSOR WORKLOAD", value = "${(p.aiWorkloadWeight * 100).toInt()}%", color = Color(0xFFC084FC))
                        PcbField(label = "EST TFLOPS", value = p.tensorFlopsEst, color = Color(0xFFC084FC))
                    }

                    // Burst History Buffer
                    Text("BURST RING BUFFER (LAST 5 CYCLES)", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
                    BurstRingBufferGraph(
                        bursts = p.cpuBurstHistory,
                        activeColor = if (p.type == ProcessType.AI_INFERENCE) Color(0xFFC084FC) else NeonCyan,
                        baseColor = ElectricViolet
                    )

                    // Fast Action Buttons inside PCB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.preemptTaskNow(p.pid)
                                inspectedPcb = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pcb_preempt_now_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSecurity)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preempt CPU", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.boostPriority(p.pid)
                                inspectedPcb = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pcb_boost_priority_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.KeyboardDoubleArrowUp, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Boost +2", fontSize = 11.sp)
                        }
                    }
                }
            },
            containerColor = DarkSurface
        )
    }
}

/**
 * Custom Canvas drawing multi-core real-time dispatch timeline pipeline.
 */
@Composable
fun CpuDispatchTimelineCanvas(
    cores: List<CpuCoreState>,
    processes: List<Process>,
    cycleCount: Long,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timeline_sweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DeepBackground)
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val rowHeight = canvasHeight / 4f

        // Draw Core Tracks
        cores.take(4).forEachIndexed { coreIdx, core ->
            val rowTop = coreIdx * rowHeight

            // Track background grid line
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, rowTop + rowHeight),
                end = Offset(canvasWidth, rowTop + rowHeight),
                strokeWidth = 1f
            )

            // Simulated quantum blocks on this core
            val blocksCount = 6
            val blockWidth = canvasWidth / blocksCount
            for (b in 0 until blocksCount) {
                val blockLeft = b * blockWidth + 2f
                val blockRight = (b + 1) * blockWidth - 2f
                val isRunningBlock = (b == ((cycleCount + coreIdx) % blocksCount).toInt())

                val blockColor = if (core.isTensorCore) {
                    if (isRunningBlock) Color(0xFFC084FC) else Color(0xFF581C87).copy(alpha = 0.5f)
                } else if (coreIdx == 0) {
                    if (isRunningBlock) NeonCyan else Color(0xFF0E7490).copy(alpha = 0.4f)
                } else if (coreIdx == 2) {
                    if (isRunningBlock) EmeraldSecurity else Color(0xFF047857).copy(alpha = 0.4f)
                } else {
                    if (isRunningBlock) WarningAmber else Color(0xFFB45309).copy(alpha = 0.4f)
                }

                drawRoundRect(
                    color = blockColor,
                    topLeft = Offset(blockLeft, rowTop + 2f),
                    size = Size(blockRight - blockLeft, rowHeight - 4f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }

        // Draw animated sweep playhead
        val playheadX = canvasWidth * sweepProgress
        drawLine(
            color = NeonCyan,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, canvasHeight),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )
    }
}

/**
 * Distribution bar showing NPU Tensor Matrix vs CPU Vector AVX vs IO wait.
 */
@Composable
fun AiWorkloadDistributionBar(
    npuPercent: Float,
    activeTflops: Float,
    modifier: Modifier = Modifier
) {
    val tensorRatio = (npuPercent / 100f).coerceIn(0.2f, 0.95f)
    val cpuVectorRatio = ((1f - tensorRatio) * 0.7f).coerceIn(0.05f, 0.5f)
    val ioWaitRatio = (1f - tensorRatio - cpuVectorRatio).coerceIn(0.02f, 0.3f)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TENSOR WORKLOAD DISTRIBUTION",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "${(tensorRatio * 100).toInt()}% NPU Dedicated",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC084FC),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Segmented multi-color bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(DeepBackground)
        ) {
            // NPU Tensor segment
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(tensorRatio)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF7E22CE), Color(0xFFC084FC)))
                    )
            )
            // CPU Vector SIMD segment
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(cpuVectorRatio)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF0284C7), NeonCyan))
                    )
            )
            // IO / DMA wait segment
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(ioWaitRatio)
                    .background(WarningAmber.copy(alpha = 0.7f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("■ NPU Tensor Matrix (${(tensorRatio * 100).toInt()}%)", color = Color(0xFFC084FC), fontSize = 9.sp)
            Text("■ CPU Vector NEON (${(cpuVectorRatio * 100).toInt()}%)", color = NeonCyan, fontSize = 9.sp)
            Text("■ DMA Wait (${(ioWaitRatio * 100).toInt()}%)", color = WarningAmber, fontSize = 9.sp)
        }
    }
}

/**
 * Task row item with dynamic prioritization scoring, live priority tuning slider,
 * burst telemetry, and deep inspection.
 */
@Composable
fun PrioritizedTaskItem(
    process: Process,
    isMlOn: Boolean,
    onAssignCore: () -> Unit,
    onBoost: () -> Unit,
    onPreempt: () -> Unit,
    onInspectPcb: () -> Unit,
    onPriorityChange: (Int) -> Unit,
    onKill: () -> Unit
) {
    val isRunning = process.state == ProcessState.RUNNING
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isRunning) CardSurfaceHighlight else DarkSurface)
            .border(
                1.dp,
                if (isRunning) NeonCyan.copy(alpha = 0.85f) else BorderSubtle,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
            .testTag("priority_task_${process.pid}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header Row: Status, Name, PID, Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) NeonCyan else TextTertiary)
                    )
                    Text(
                        text = process.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "PID ${process.pid}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Core affinity button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, if (process.assignedCoreId == 1) Color(0xFF7E22CE) else BorderSubtle, RoundedCornerShape(4.dp))
                            .clickable(onClick = onAssignCore)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("assign_core_${process.pid}")
                    ) {
                        Text(
                            text = if (process.assignedCoreId == 1) "NPU" else "Core ${process.assignedCoreId}",
                            color = if (process.assignedCoreId == 1) Color(0xFFC084FC) else NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    ProcessTypeBadge(type = process.type)
                }
            }

            // Burst ring buffer graph
            BurstRingBufferGraph(
                bursts = process.cpuBurstHistory,
                activeColor = if (process.type == ProcessType.AI_INFERENCE) Color(0xFFC084FC) else NeonCyan,
                baseColor = ElectricViolet
            )

            // Dynamic Score & Telemetry Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Dynamic ML Score:", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
                        Text(
                            text = String.format("%.3f", process.dynamicScore),
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (process.type == ProcessType.AI_INFERENCE) {
                            Text(
                                text = "· ${process.tensorFlopsEst}",
                                color = Color(0xFFC084FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "Pred: ${process.predictedBurstMs}ms · Actual: ${process.cpuBurstHistory.lastOrNull() ?: 15}ms · Prio: ${process.priority}/10",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 9.sp
                    )
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Preempt Button
                    IconButton(
                        onClick = onPreempt,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("preempt_btn_${process.pid}")
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = "Preempt to CPU",
                            tint = if (isRunning) EmeraldSecurity else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Boost +2 Button
                    IconButton(
                        onClick = onBoost,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("boost_btn_${process.pid}")
                    ) {
                        Icon(
                            Icons.Default.KeyboardDoubleArrowUp,
                            contentDescription = "Boost Priority",
                            tint = EmeraldSecurity,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Expand / Priority Slider Button
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("expand_task_btn_${process.pid}")
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = "Tune Priority",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // PCB Inspector Button
                    IconButton(
                        onClick = onInspectPcb,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("inspect_pcb_btn_${process.pid}")
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Inspect PCB",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Kill Task Button
                    IconButton(
                        onClick = onKill,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("kill_btn_${process.pid}")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kill Task",
                            tint = ThreatCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expandable Priority Tuning Slider
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DeepBackground)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME TASK PRIORITY TUNER",
                            color = TextTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Level ${process.priority} of 10",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = process.priority.toFloat(),
                        onValueChange = { onPriorityChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("priority_slider_${process.pid}"),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = CardSurfaceHighlight
                        )
                    )
                }
            }
        }
    }
}

/**
 * Individual CPU Core Status Tile.
 */
@Composable
fun CpuCoreCard(
    core: CpuCoreState,
    modifier: Modifier = Modifier
) {
    val barColor = if (core.isTensorCore) Color(0xFFC084FC)
    else if (core.loadPercent > 80f) ThreatCrimson
    else if (core.loadPercent > 50f) NeonCyan
    else EmeraldSecurity

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardSurfaceHighlight.copy(alpha = 0.5f))
            .border(
                1.dp,
                if (core.isTensorCore) Color(0xFF7E22CE).copy(alpha = 0.7f) else BorderSubtle,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
            .testTag("core_card_${core.coreId}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = core.name.take(13),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (core.isTensorCore) Color(0xFFD8B4FE) else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${core.loadPercent.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Progress load bar
            LinearProgressIndicator(
                progress = { (core.loadPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = DeepBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${core.frequencyGhz}GHz · ${core.tempCelsius.toInt()}°C",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (core.tempCelsius > 65f) ThreatCrimson else TextTertiary,
                    fontSize = 9.sp
                )
                Text(
                    text = core.activeProcessName?.take(11) ?: "idle",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (core.isTensorCore) Color(0xFFC084FC) else NeonCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TelemetryMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun PcbField(
    label: String,
    value: String,
    color: Color = TextPrimary
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 8.sp)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(label, color = TextTertiary, fontSize = 9.sp)
    }
}
