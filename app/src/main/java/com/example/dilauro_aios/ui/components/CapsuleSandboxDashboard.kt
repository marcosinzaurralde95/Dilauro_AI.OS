package com.example.dilauro_aios.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.Capsule
import com.example.dilauro_aios.model.CapsuleStatus
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel
import java.util.Locale

enum class CapsuleFilter(val label: String) {
    ALL("All Instances"),
    RUNNING_ONLY("Running Only"),
    PAUSED("Paused"),
    TERMINATED("Terminated"),
    HIGH_RESOURCE("High Memory (>200MB)")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CapsuleSandboxDashboard(
    viewModel: AiosViewModel,
    isCompact: Boolean = false,
    onViewFullSandbox: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val capsules by viewModel.capsules.collectAsState()
    var selectedFilter by remember { mutableStateOf(CapsuleFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeployDialog by remember { mutableStateOf(false) }
    var terminateCandidate by remember { mutableStateOf<Capsule?>(null) }
    var toastNotice by remember { mutableStateOf<String?>(null) }

    val runningCount = capsules.count { it.status == CapsuleStatus.RUNNING }
    val totalRamUsedMb = capsules.sumOf { it.memoryUsageMb }
    val totalCpuPercent = capsules.sumOf { it.cpuPercent.toDouble() }.toFloat()
    val maxSandboxRamMb = 2048

    val filteredCapsules = remember(capsules, selectedFilter, searchQuery) {
        capsules.filter { cap ->
            val matchesFilter = when (selectedFilter) {
                CapsuleFilter.ALL -> true
                CapsuleFilter.RUNNING_ONLY -> cap.status == CapsuleStatus.RUNNING
                CapsuleFilter.PAUSED -> cap.status == CapsuleStatus.PAUSED
                CapsuleFilter.TERMINATED -> cap.status == CapsuleStatus.TERMINATED
                CapsuleFilter.HIGH_RESOURCE -> cap.memoryUsageMb > 200
            }
            val matchesSearch = searchQuery.isBlank() ||
                    cap.name.contains(searchQuery, ignoreCase = true) ||
                    cap.id.contains(searchQuery, ignoreCase = true) ||
                    cap.capabilities.any { it.contains(searchQuery, ignoreCase = true) }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. Global Sandbox Telemetry & Header Deck ---
        CyberCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("capsule_sandbox_overview_card"),
            borderColor = NeonPurple
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (runningCount > 0) EmeraldSecurity else WarningAmber, CircleShape)
                            )
                            Text(
                                text = "CAPSULE SANDBOX RUNTIME",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonPurple,
                                fontWeight = FontWeight.Bold
                            )
                            MetricBadge(
                                text = "SFI ISOLATION",
                                textColor = NeonCyan,
                                backgroundColor = DeepBackground
                            )
                        }
                        Text(
                            text = "$runningCount Running Isolated Containers",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isCompact) {
                        TextButton(
                            onClick = onViewFullSandbox,
                            modifier = Modifier.testTag("view_full_sandbox_btn")
                        ) {
                            Text("Manage All", color = NeonCyan, fontSize = 12.sp)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { showDeployDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("spawn_sandbox_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DeepBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Spawn Sandbox", color = DeepBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resource Consumption Meters Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // RAM Meter
                    CyberCard(
                        modifier = Modifier.weight(1f),
                        borderColor = BorderSubtle,
                        backgroundColor = DeepBackground.copy(alpha = 0.6f)
                    ) {
                        Text("SANDBOX RAM ALLOCATION", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalRamUsedMb MB",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (totalRamUsedMb > 1500) WarningAmber else NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val ramFraction = (totalRamUsedMb.toFloat() / maxSandboxRamMb).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { ramFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (ramFraction > 0.8f) ThreatCrimson else NeonCyan,
                            trackColor = BorderSubtle
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Cap: $maxSandboxRamMb MB pool", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
                    }

                    // CPU Meter
                    CyberCard(
                        modifier = Modifier.weight(1f),
                        borderColor = BorderSubtle,
                        backgroundColor = DeepBackground.copy(alpha = 0.6f)
                    ) {
                        Text("AGGREGATE CPU QUOTA", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", totalCpuPercent),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (totalCpuPercent > 70f) WarningAmber else EmeraldSecurity,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val cpuFraction = (totalCpuPercent / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { cpuFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (cpuFraction > 0.7f) WarningAmber else EmeraldSecurity,
                            trackColor = BorderSubtle
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Across 8 Neural Cores", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
                    }
                }
            }
        }

        // Feedback Toast
        AnimatedVisibility(visible = toastNotice != null) {
            toastNotice?.let { notice ->
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = EmeraldSecurity
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(18.dp))
                        Text(notice, color = EmeraldSecurity, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // --- 2. Filter & Search Controls (Only shown in full mode) ---
        if (!isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search isolated containers by name, ID or capability...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandbox_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CapsuleFilter.entries) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) NeonPurple.copy(alpha = 0.2f) else DeepBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonPurple else BorderSubtle
                            ),
                            modifier = Modifier
                                .clickable { selectedFilter = filter }
                                .testTag("filter_chip_${filter.name.lowercase()}")
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) NeonPurple else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Container Process Cards List ---
        val displayList = if (isCompact) capsules.take(3) else filteredCapsules

        if (displayList.isEmpty()) {
            CyberCard(modifier = Modifier.fillMaxWidth(), borderColor = BorderSubtle) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LayersClear, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No sandbox containers match the filter", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            displayList.forEach { cap ->
                IsolatedContainerCard(
                    capsule = cap,
                    onTerminate = { terminateCandidate = cap },
                    onRestart = {
                        viewModel.restartCapsule(cap.id)
                        toastNotice = "Restarted sandbox container '${cap.name}'"
                    },
                    onToggle = { viewModel.toggleCapsuleState(cap.id) },
                    onHotSwap = { viewModel.hotSwapCapsule(cap.id) }
                )
            }
        }

        // Clean-up terminated instances button if any are terminated
        val hasTerminated = capsules.any { it.status == CapsuleStatus.TERMINATED }
        if (!isCompact && hasTerminated) {
            OutlinedButton(
                onClick = {
                    viewModel.purgeTerminatedCapsules()
                    toastNotice = "Purged all terminated container sandboxes from VFS registry"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("purge_terminated_sandboxes_btn"),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Purge Terminated Sandboxes from Registry", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }

    // --- Terminate Confirmation Dialog ---
    terminateCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { terminateCandidate = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ThreatCrimson)
                    Text("Terminate Sandbox Instance?", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to terminate '${target.name}' (${target.id})?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "This will immediately revoke all capabilities, kill the isolated WASI container, and reclaim ${target.memoryUsageMb} MB RAM and ${String.format(Locale.getDefault(), "%.1f%%", target.cpuPercent)} CPU.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.terminateCapsule(target.id)
                        toastNotice = "Terminated sandbox instance '${target.name}' (PID: ${target.id})"
                        terminateCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson),
                    modifier = Modifier.testTag("confirm_terminate_capsule_btn")
                ) {
                    Text("Terminate Container", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { terminateCandidate = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }

    // --- Deploy New Sandbox Dialog ---
    if (showDeployDialog) {
        var nameInput by remember { mutableStateOf("") }
        var memoryLimitInput by remember { mutableStateOf("256") }
        var cpuQuotaInput by remember { mutableStateOf("25.0") }
        var selectedPerms by remember { mutableStateOf(setOf("capsule.fs:ro", "capsule.sys:telemetry")) }

        val availablePerms = listOf(
            "capsule.fs:ro", "capsule.fs:rw",
            "capsule.net:libp2p", "capsule.neural:faiss",
            "capsule.sys:telemetry", "capsule.ui:compositor",
            "capsule.sec:ebpf", "capsule.audio:out"
        )

        AlertDialog(
            onDismissRequest = { showDeployDialog = false },
            title = {
                Text("Spawn Isolated WASI Sandbox", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Container Name") },
                        placeholder = { Text("e.g. secure_crypto_worker") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spawn_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = memoryLimitInput,
                            onValueChange = { memoryLimitInput = it },
                            label = { Text("RAM Cap (MB)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cpuQuotaInput,
                            onValueChange = { cpuQuotaInput = it },
                            label = { Text("CPU Quota (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Attach Capability Tokens:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availablePerms.forEach { perm ->
                            val isChecked = selectedPerms.contains(perm)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    selectedPerms = if (isChecked) selectedPerms - perm else selectedPerms + perm
                                },
                                label = { Text(perm, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurple.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonPurple
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ram = memoryLimitInput.toIntOrNull() ?: 256
                        val cpu = cpuQuotaInput.toFloatOrNull() ?: 25f
                        viewModel.deployCapsule(
                            name = nameInput,
                            capabilities = selectedPerms.toList(),
                            memoryLimitMb = ram,
                            cpuQuotaPercent = cpu
                        )
                        toastNotice = "Deployed new sandbox container '$nameInput'"
                        showDeployDialog = false
                    },
                    modifier = Modifier.testTag("confirm_spawn_sandbox_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Spawn Container", color = DeepBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeployDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
fun IsolatedContainerCard(
    capsule: Capsule,
    onTerminate: () -> Unit,
    onRestart: () -> Unit,
    onToggle: () -> Unit,
    onHotSwap: () -> Unit
) {
    val isRunning = capsule.status == CapsuleStatus.RUNNING
    val isTerminated = capsule.status == CapsuleStatus.TERMINATED

    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("capsule_sandbox_card_${capsule.id}"),
        borderColor = when {
            isTerminated -> ThreatCrimson.copy(alpha = 0.4f)
            isRunning -> BorderCyan.copy(alpha = 0.5f)
            else -> BorderSubtle
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header info & status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = capsule.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isTerminated) TextSecondary else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        MetricBadge(
                            text = "v${capsule.version}",
                            textColor = TextCyan,
                            backgroundColor = DarkSurface
                        )
                    }
                    Text(
                        text = "PID: ${capsule.id} · Node: ${capsule.targetNode} · ${capsule.isolationTier}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                CapsuleStatusBadge(status = capsule.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Resource Consumption Section (CPU & RAM)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // RAM usage meter
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RAM Usage", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                        Text(
                            text = "${capsule.memoryUsageMb} / ${capsule.memoryLimitMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (capsule.memoryUsageMb > capsule.memoryLimitMb * 0.8f) WarningAmber else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    val memPercent = (capsule.memoryUsageMb.toFloat() / capsule.memoryLimitMb.coerceAtLeast(1)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { memPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isTerminated) TextTertiary else if (memPercent > 0.8f) WarningAmber else NeonCyan,
                        trackColor = BorderSubtle
                    )
                }

                // CPU usage meter
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("CPU Consumption", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%% / %.0f%%", capsule.cpuPercent, capsule.cpuQuotaPercent),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (capsule.cpuPercent > capsule.cpuQuotaPercent * 0.8f) WarningAmber else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    val cpuFraction = (capsule.cpuPercent / capsule.cpuQuotaPercent.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { cpuFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isTerminated) TextTertiary else if (cpuFraction > 0.8f) WarningAmber else EmeraldSecurity,
                        trackColor = BorderSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-telemetry strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Threads: ${capsule.threadCount} · Net: ${capsule.networkIoKbps} KB/s",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
                Text(
                    text = "Syscalls: ${capsule.syscallCount} · Uptime: ${capsule.uptimeSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attached Capability Tokens
            Text("Capability Tokens:", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                capsule.capabilities.forEach { cap ->
                    MetricBadge(
                        text = cap,
                        textColor = if (isTerminated) TextSecondary else EmeraldSecurity,
                        backgroundColor = if (isTerminated) CardSurfaceHighlight else Color(0xFF042F2E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Actions Row (TERMINATE sandbox instance option!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isTerminated) {
                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("toggle_btn_${capsule.id}")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Resume",
                                tint = if (isRunning) WarningAmber else EmeraldSecurity,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onHotSwap,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("hotswap_btn_${capsule.id}")
                        ) {
                            Icon(Icons.Default.Autorenew, contentDescription = "Hot-Swap", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Primary Termination / Restart Action
                if (isTerminated) {
                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSecurity.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSecurity),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("restart_capsule_${capsule.id}")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Respawn Sandbox", color = EmeraldSecurity, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onTerminate,
                        colors = ButtonDefaults.buttonColors(containerColor = ThreatCrimson.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ThreatCrimson),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("terminate_capsule_${capsule.id}")
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null, tint = ThreatCrimson, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terminate Instance", color = ThreatCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
