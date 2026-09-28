package com.example.dilauro_aios.ui.screens

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
import com.example.dilauro_aios.model.Capsule
import com.example.dilauro_aios.model.CapsuleStatus
import com.example.dilauro_aios.ui.components.CapsuleStatusBadge
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsulesScreen(
    viewModel: AiosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val capsules by viewModel.capsules.collectAsState()
    var showDeployDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Capsules (WASI Runtime)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("WASI Micro-Containers & Zero Ambient Authority", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("capsules_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeployDialog = true }, modifier = Modifier.testTag("deploy_capsule_btn")) {
                        Icon(Icons.Default.AddBox, contentDescription = "Deploy Capsule", tint = NeonCyan)
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
            // WASI Sandbox Overview Card
            item {
                CyberCard(borderColor = NeonPurple) {
                    Text("CAPSULE RUNTIME SPECIFICATION", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Capability-Based Micro-Containers",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Each Dilauro application executes within a WebAssembly micro-container isolated by software fault isolation (SFI). Syscalls are granted only through non-forgeable capability tokens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Capsules List
            item {
                Text(
                    text = "Active WASI Containers (${capsules.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            items(capsules, key = { it.id }) { cap ->
                CapsuleItemCard(
                    capsule = cap,
                    onToggle = { viewModel.toggleCapsuleState(cap.id) },
                    onHotSwap = { viewModel.hotSwapCapsule(cap.id) },
                    onMigrate = { viewModel.migrateCapsule(cap.id) }
                )
            }
        }
    }

    if (showDeployDialog) {
        var capName by remember { mutableStateOf("") }
        var selectedCapPerms by remember { mutableStateOf(setOf("capsule.fs:ro", "capsule.sys:telemetry")) }

        AlertDialog(
            onDismissRequest = { showDeployDialog = false },
            title = { Text("Deploy WASI Capsule", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = capName,
                        onValueChange = { capName = it },
                        label = { Text("Capsule Name") },
                        placeholder = { Text("e.g. quantum_crypto_agent") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("deploy_name_input")
                    )

                    Text("Attach Capability Tokens:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    listOf("capsule.fs:ro", "capsule.fs:rw", "capsule.net:libp2p", "capsule.neural:faiss", "capsule.sys:telemetry").forEach { perm ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedCapPerms.contains(perm),
                                onCheckedChange = { checked ->
                                    selectedCapPerms = if (checked) selectedCapPerms + perm else selectedCapPerms - perm
                                }
                            )
                            Text(perm, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deployCapsule(capName, selectedCapPerms.toList())
                        showDeployDialog = false
                    },
                    modifier = Modifier.testTag("confirm_deploy_button")
                ) {
                    Text("Deploy Module")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeployDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
fun CapsuleItemCard(
    capsule: Capsule,
    onToggle: () -> Unit,
    onHotSwap: () -> Unit,
    onMigrate: () -> Unit
) {
    CyberCard(
        modifier = Modifier.testTag("capsule_card_${capsule.id}"),
        borderColor = if (capsule.status == CapsuleStatus.RUNNING) BorderSubtle else BorderCyan
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = capsule.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    MetricBadge(
                        text = "v${capsule.version}",
                        textColor = TextCyan,
                        backgroundColor = DarkSurface
                    )
                }
                Text(
                    text = "Node: ${capsule.targetNode} · Bytecode: ${capsule.wasiModuleSizeKb} KB",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace
                )
            }
            CapsuleStatusBadge(status = capsule.status)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Resource usage row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RAM: ${capsule.memoryUsageMb} MB · CPU: ${String.format("%.1f%%", capsule.cpuPercent)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Capability badges
        Text("Active Capabilities:", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            capsule.capabilities.forEach { cap ->
                MetricBadge(
                    text = cap,
                    textColor = EmeraldSecurity,
                    backgroundColor = Color(0xFF042F2E),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = BorderSubtle)
        Spacer(modifier = Modifier.height(8.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onHotSwap,
                modifier = Modifier.testTag("hotswap_${capsule.id}")
            ) {
                Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hot-Swap", color = NeonCyan, fontSize = 12.sp)
            }

            TextButton(
                onClick = onMigrate,
                modifier = Modifier.testTag("migrate_${capsule.id}")
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricViolet)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Migrate Mesh", color = ElectricViolet, fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = onToggle,
                modifier = Modifier.testTag("toggle_${capsule.id}"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (capsule.status == CapsuleStatus.RUNNING) WarningAmber.copy(alpha = 0.2f) else EmeraldSecurity.copy(alpha = 0.2f),
                    contentColor = if (capsule.status == CapsuleStatus.RUNNING) WarningAmber else EmeraldSecurity
                )
            ) {
                Text(if (capsule.status == CapsuleStatus.RUNNING) "Pause" else "Resume", fontSize = 12.sp)
            }
        }
    }
}
