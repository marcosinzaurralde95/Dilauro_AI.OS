package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Compress
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
import com.example.dilauro_aios.model.MemoryPressure
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: AiosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mem by viewModel.memoryTelemetry.collectAsState()
    val evictionLogs by viewModel.evictionLogs.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DAM-Engine Memory", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("Dynamic Adaptive Memory & Predictive Eviction", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("memory_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.triggerCompaction() },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = ElectricViolet.copy(alpha = 0.2f), contentColor = NeonCyan),
                        modifier = Modifier.padding(end = 8.dp).testTag("trigger_compaction_button")
                    ) {
                        Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compact", fontSize = 12.sp)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RAM Distribution Card
            item {
                CyberCard(
                    borderColor = when (mem.pressure) {
                        MemoryPressure.NORMAL -> BorderCyan
                        MemoryPressure.MODERATE -> WarningAmber
                        MemoryPressure.CRITICAL -> ThreatCrimson
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RAM ALLOCATION BREAKDOWN", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        MetricBadge(
                            text = "PRESSURE: ${mem.pressure.name}",
                            textColor = when (mem.pressure) {
                                MemoryPressure.NORMAL -> EmeraldSecurity
                                MemoryPressure.MODERATE -> WarningAmber
                                MemoryPressure.CRITICAL -> ThreatCrimson
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${mem.allocatedMb} MB / ${mem.totalRamMb} MB",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-segmented bar
                    val allocatedFraction = (mem.allocatedMb.toFloat() / mem.totalRamMb).coerceIn(0f, 1f)
                    val wssFraction = (mem.workingSetMb.toFloat() / mem.totalRamMb).coerceIn(0f, 1f)
                    val coldFraction = (mem.coldPagesMb.toFloat() / mem.totalRamMb).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(CardSurfaceHighlight, RoundedCornerShape(7.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(wssFraction.coerceAtLeast(0.01f))
                                .background(NeonCyan, RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(coldFraction.coerceAtLeast(0.01f))
                                .background(ElectricViolet)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((1f - (wssFraction + coldFraction)).coerceAtLeast(0.01f))
                                .background(Color.Transparent)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem(color = NeonCyan, label = "Working Set", value = "${mem.workingSetMb} MB")
                        LegendItem(color = ElectricViolet, label = "Cold Pages", value = "${mem.coldPagesMb} MB")
                        LegendItem(color = EmeraldSecurity, label = "zRAM Compressed", value = "${mem.compressedMb} MB (${mem.compressionRatio}x)")
                    }
                }
            }

            // Memory Pressure Injection Card
            item {
                CyberCard {
                    Text("SIMULATE MEMORY PRESSURE SIGNAL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DAM-Engine continuously watches memory thresholds and sends asynchronous signals to capsules to release cold buffers before Linux OOM killer can trigger.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemoryPressure.values().forEach { pressure ->
                            val isSelected = mem.pressure == pressure
                            Button(
                                onClick = { viewModel.setMemoryPressure(pressure) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pressure_btn_${pressure.name.lowercase()}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) {
                                        when (pressure) {
                                            MemoryPressure.NORMAL -> EmeraldSecurity
                                            MemoryPressure.MODERATE -> WarningAmber
                                            MemoryPressure.CRITICAL -> ThreatCrimson
                                        }
                                    } else CardSurfaceHighlight
                                )
                            ) {
                                Text(
                                    text = pressure.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) DeepBackground else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Eviction & Optimization Logs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Predictive Pre-Eviction Audit Trail", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Predictive Acc: ${mem.predictiveAccuracy}%", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }

            items(evictionLogs, key = { it.id }) { log ->
                CyberCard(
                    modifier = Modifier.testTag("eviction_log_${log.id}"),
                    backgroundColor = DarkSurface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.capsuleName,
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan,
                            fontSize = 14.sp
                        )
                        Text(
                            text = log.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "-${log.pagesEvicted} pages reclaimed",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSecurity,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
        }
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
