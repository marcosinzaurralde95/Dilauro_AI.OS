package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.ui.components.CapsuleSandboxDashboard
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Capsule Sandbox Runtime", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            MetricBadge(
                                text = "RING-3 WASI",
                                textColor = NeonPurple,
                                backgroundColor = DeepBackground
                            )
                        }
                        Text(
                            "Software Fault Isolation (SFI) & Isolated Container Processes",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("capsules_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
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
            // WASI Sandbox Overview Architecture Card
            item {
                CyberCard(borderColor = NeonPurple) {
                    Text("CAPSULE RUNTIME SPECIFICATION // SFI RFC-0004", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Capability-Based Micro-Containers",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Each Dilauro application executes within a WebAssembly micro-container isolated by software fault isolation (SFI). Syscalls are granted only through non-forgeable capability tokens with zero ambient authority. Terminating a container immediately purges linear memory and revokes all capability handles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // The 'Capsule Sandbox' UI component
            item {
                CapsuleSandboxDashboard(
                    viewModel = viewModel,
                    isCompact = false
                )
            }
        }
    }
}
