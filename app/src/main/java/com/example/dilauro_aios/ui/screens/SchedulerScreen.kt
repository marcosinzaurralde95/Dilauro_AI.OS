package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.ProcessType
import com.example.dilauro_aios.ui.components.MlSchedulerDashboard
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    viewModel: AiosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpawnDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "D-Kernel ML Scheduler",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "RFC-0001 CPU Prioritization & AI Workload Balancing",
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
                            contentDescription = "Spawn Task",
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
            item {
                MlSchedulerDashboard(
                    viewModel = viewModel,
                    isCompact = false
                )
            }
        }
    }

    // Spawn Process Dialog
    if (showSpawnDialog) {
        var procName by remember { mutableStateOf("") }
        var procType by remember { mutableStateOf(ProcessType.AI_INFERENCE) }
        var procPrio by remember { mutableFloatStateOf(8f) }

        AlertDialog(
            onDismissRequest = { showSpawnDialog = false },
            title = { Text("Spawn D-Kernel Task", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = procName,
                        onValueChange = { procName = it },
                        label = { Text("Process / Task Name") },
                        placeholder = { Text("e.g. tensor_attention_pass") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("spawn_name_input")
                    )

                    Text("Process Classification:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProcessType.values().forEach { t ->
                            FilterChip(
                                selected = procType == t,
                                onClick = { procType = t },
                                label = { Text(t.name.take(4), fontSize = 10.sp) }
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
                    Text("Spawn Task")
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
