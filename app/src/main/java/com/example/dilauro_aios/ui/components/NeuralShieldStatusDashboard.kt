package com.example.dilauro_aios.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.*
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dedicated Neural Shield Status Dashboard UI Component.
 * Monitors and displays active security protocols, cryptographic enforcement,
 * real-time privacy alerts, and zero-trust audit verification.
 */
@Composable
fun NeuralShieldStatusDashboard(
    viewModel: AiosViewModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onViewAllAlerts: (() -> Unit)? = null
) {
    val summary by viewModel.shieldSummary.collectAsState()
    val protocols by viewModel.protocols.collectAsState()
    val alerts by viewModel.privacyAlerts.collectAsState()
    val threatGauge by viewModel.threatGauge.collectAsState()

    var selectedPrivacyFilter by remember { mutableStateOf(PrivacyCategory.ALL) }
    var inspectingAlert by remember { mutableStateOf<PrivacyAlert?>(null) }
    var inspectingProtocol by remember { mutableStateOf<SecurityProtocol?>(null) }
    var showSimulateMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: SHIELD RADAR & INTEGRITY GAUGE HUD ---
        CyberCard(
            borderColor = if (threatGauge > 35f) ThreatCrimson.copy(alpha = 0.8f) else NeonCyan.copy(alpha = 0.5f),
            backgroundColor = DarkSurface,
            modifier = Modifier.testTag("neural_shield_radar_card")
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
                            .background(
                                if (threatGauge > 35f) ThreatCrimson else EmeraldSecurity,
                                CircleShape
                            )
                    )
                    Text(
                        text = "NEURAL SHIELD SENTINEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                MetricBadge(
                    text = if (summary.isScanning) "DEEP AUDIT SCANNING..."
                    else if (threatGauge > 35f) "ANOMALY MITIGATION ENGAGED"
                    else "ZERO-TRUST ENFORCING",
                    textColor = if (threatGauge > 35f) ThreatCrimson else EmeraldSecurity,
                    backgroundColor = if (threatGauge > 35f) Color(0xFF450A0A) else Color(0xFF042F2E)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Radar & Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Radar Canvas
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF071120)),
                    contentAlignment = Alignment.Center
                ) {
                    ShieldRadarCanvas(
                        isScanning = summary.isScanning,
                        hasThreat = threatGauge > 35f
                    )

                    // Center Shield Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (threatGauge > 35f) Icons.Default.GppMaybe else Icons.Default.Shield,
                            contentDescription = "Shield Status",
                            tint = if (threatGauge > 35f) ThreatCrimson else NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = String.format("%.1f%%", summary.overallIntegrityScore),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "INTEGRITY",
                            color = TextTertiary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp
                        )
                    }
                }

                // Stats Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ACTIVE PROTOCOLS", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
                            Text(
                                text = "${summary.activeProtocolsCount}/${summary.totalProtocolsCount} Engaged",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("PRIVACY SCORE", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
                            Text(
                                text = String.format("%.1f%%", summary.privacyScore),
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldSecurity,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("INTERCEPTIONS", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
                            Text(
                                text = "${summary.totalThreatsBlocked} Blocked",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarningAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column {
                            Text("INSPECTION LAG", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
                            Text(
                                text = "3.8 μs (eBPF)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Deep Audit Action Button
                    Button(
                        onClick = { viewModel.triggerDeepPrivacyAudit() },
                        enabled = !summary.isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("trigger_deep_privacy_audit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (summary.isScanning) CardSurfaceHighlight else ElectricViolet.copy(alpha = 0.85f),
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (summary.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auditing SFI Sandboxes...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.SecurityUpdateGood, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Deep Privacy Audit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Scanning progress bar
            AnimatedVisibility(visible = summary.isScanning) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    LinearProgressIndicator(
                        progress = { summary.scanProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonCyan,
                        trackColor = CardSurfaceHighlight
                    )
                }
            }
        }

        // --- SECTION 2: ACTIVE SECURITY PROTOCOLS MONITOR ---
        CyberCard(modifier = Modifier.testTag("security_protocols_monitor_card")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Security Protocols",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hardware NX, eBPF JIT & Zero-Trust Cryptographic Shims",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                MetricBadge(
                    text = "${protocols.count { it.isEnabled }} ENFORCING",
                    textColor = NeonCyan,
                    backgroundColor = CardSurfaceHighlight
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Protocol tiles list
            protocols.forEach { proto ->
                ProtocolRowItem(
                    protocol = proto,
                    onToggle = { viewModel.toggleProtocol(proto.id) },
                    onCycleStatus = { viewModel.cycleProtocolStatus(proto.id) },
                    onInspect = { inspectingProtocol = proto }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // --- SECTION 3: REAL-TIME PRIVACY ALERTS FEED ---
        CyberCard(modifier = Modifier.testTag("privacy_alerts_feed_card")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Real-Time Privacy Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (alerts.any { !it.isAcknowledged }) ThreatCrimson else EmeraldSecurity, CircleShape)
                        )
                    }
                    Text(
                        text = "${alerts.size} event records captured · Sensor & Data Leak Guard",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Action Menu: Simulate Probe
                Box {
                    IconButton(
                        onClick = { showSimulateMenu = true },
                        modifier = Modifier.testTag("simulate_privacy_alert_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddModerator,
                            contentDescription = "Simulate Privacy Alert",
                            tint = NeonCyan
                        )
                    }

                    DropdownMenu(
                        expanded = showSimulateMenu,
                        onDismissRequest = { showSimulateMenu = false },
                        modifier = Modifier.background(CardSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Probe: Sensor Audio Read Attempt", color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                viewModel.simulatePrivacyAlert(PrivacyCategory.SENSOR_ACCESS)
                                showSimulateMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = ThreatCrimson, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Probe: PII Query in SynapseFS", color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                viewModel.simulatePrivacyAlert(PrivacyCategory.DATA_LEAK_PREVENTION)
                                showSimulateMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Probe: Forged Ed25519 IPC Token", color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                viewModel.simulatePrivacyAlert(PrivacyCategory.CRYPTO_KEY_LEAK)
                                showSimulateMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Probe: Outbound Raw Socket Egress", color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                viewModel.simulatePrivacyAlert(PrivacyCategory.UNAUTHORIZED_EGRESS)
                                showSimulateMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    PrivacyCategory.ALL to "All",
                    PrivacyCategory.SENSOR_ACCESS to "Sensors",
                    PrivacyCategory.DATA_LEAK_PREVENTION to "Data Leaks",
                    PrivacyCategory.CRYPTO_KEY_LEAK to "Crypto / IPC",
                    PrivacyCategory.UNAUTHORIZED_EGRESS to "Egress"
                ).forEach { (cat, label) ->
                    val isSelected = selectedPrivacyFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPrivacyFilter = cat },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_privacy_${label.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filtered Alerts List
            val filteredAlerts = alerts.filter {
                selectedPrivacyFilter == PrivacyCategory.ALL || it.category == selectedPrivacyFilter
            }

            if (filteredAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Zero Privacy Violations Detected", color = TextSecondary, fontSize = 13.sp)
                        Text("All SFI sandboxes & differential privacy layers nominal", color = TextTertiary, fontSize = 11.sp)
                    }
                }
            } else {
                filteredAlerts.take(if (isCompact) 3 else 10).forEach { alert ->
                    PrivacyAlertItemCard(
                        alert = alert,
                        onInspect = { inspectingAlert = alert },
                        onQuarantine = { viewModel.quarantineAlertSource(alert.id) },
                        onAcknowledge = { viewModel.acknowledgeAlert(alert.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // --- DIALOG: Cryptographic Token Inspector ---
    if (inspectingAlert != null) {
        val a = inspectingAlert!!
        AlertDialog(
            onDismissRequest = { inspectingAlert = null },
            confirmButton = {
                TextButton(onClick = { inspectingAlert = null }) {
                    Text("Close", color = NeonCyan)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Token, contentDescription = null, tint = NeonCyan)
                    Text("Cryptographic Token Audit", color = TextPrimary, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Source Entity: ${a.sourceEntity}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("Timestamp: ${a.timestamp}", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                    Text("Event Type: ${a.title}", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cryptographic Signature / Fingerprint:", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(
                        text = a.tokenFingerprint,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepBackground, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Zero-Trust Verification Outcome:", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(a.details, color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Remediation / Quarantine Action:", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(a.remediationHint, color = EmeraldSecurity, fontSize = 12.sp)
                }
            },
            containerColor = CardSurface
        )
    }

    // --- DIALOG: Security Protocol Details ---
    if (inspectingProtocol != null) {
        val p = inspectingProtocol!!
        AlertDialog(
            onDismissRequest = { inspectingProtocol = null },
            confirmButton = {
                TextButton(onClick = { inspectingProtocol = null }) {
                    Text("Dismiss", color = NeonCyan)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge(text = p.code, textColor = NeonCyan, backgroundColor = Color(0xFF003B46))
                    Text(p.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category: ${p.category.name.replace("_", " ")}", color = TextSecondary, fontSize = 12.sp)
                    Text("Specification: ${p.description}", color = TextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Kernel Telemetry Metrics:", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Events Inspected: ${p.inspectedEventsCount}", color = TextSecondary, fontSize = 12.sp)
                        Text("Enforced Rules: ${p.enforcedRulesCount}", color = TextSecondary, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Violations Caught: ${p.violationsBlockedCount}", color = ThreatCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Overhead: ${p.latencyImpactUs} μs", color = EmeraldSecurity, fontSize = 12.sp)
                    }
                }
            },
            containerColor = CardSurface
        )
    }
}

/**
 * Animated High-Tech Radar Canvas with rotating sweep beam and concentric rings.
 */
@Composable
fun ShieldRadarCanvas(
    isScanning: Boolean,
    hasThreat: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isScanning) 1200 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // 1. Concentric circles
        drawCircle(
            color = BorderSubtle.copy(alpha = 0.4f),
            radius = radius * 0.35f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = BorderSubtle.copy(alpha = 0.6f),
            radius = radius * 0.70f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = if (hasThreat) ThreatCrimson.copy(alpha = 0.8f) else NeonCyan.copy(alpha = 0.8f),
            radius = radius * 0.98f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 2. Crosshair grid lines
        drawLine(
            color = BorderSubtle.copy(alpha = 0.35f),
            start = Offset(center.x, 0f),
            end = Offset(center.x, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = BorderSubtle.copy(alpha = 0.35f),
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 1.dp.toPx()
        )

        // 3. Rotating radar sweep line
        val angleRad = Math.toRadians(sweepAngle.toDouble())
        val endX = center.x + (radius * cos(angleRad)).toFloat()
        val endY = center.y + (radius * sin(angleRad)).toFloat()

        val beamColor = if (hasThreat) ThreatCrimson else NeonCyan

        drawLine(
            brush = Brush.radialGradient(
                colors = listOf(beamColor, beamColor.copy(alpha = 0.2f), Color.Transparent),
                center = center,
                radius = radius
            ),
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Protocol Row Component with status badge, toggle, and inspection.
 */
@Composable
fun ProtocolRowItem(
    protocol: SecurityProtocol,
    onToggle: () -> Unit,
    onCycleStatus: () -> Unit,
    onInspect: () -> Unit
) {
    val statusColor = when (protocol.status) {
        ProtocolStatus.ENFORCING -> NeonCyan
        ProtocolStatus.HARDENED -> EmeraldSecurity
        ProtocolStatus.MONITORING -> WarningAmber
        ProtocolStatus.BYPASSED -> TextTertiary
    }

    val statusBg = when (protocol.status) {
        ProtocolStatus.ENFORCING -> Color(0xFF003B46)
        ProtocolStatus.HARDENED -> Color(0xFF064E3B)
        ProtocolStatus.MONITORING -> Color(0xFF451A03)
        ProtocolStatus.BYPASSED -> CardSurfaceHighlight
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardSurfaceHighlight.copy(alpha = 0.5f))
            .border(1.dp, if (protocol.isEnabled) statusColor.copy(alpha = 0.3f) else BorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onInspect)
            .padding(10.dp)
            .testTag("protocol_item_${protocol.code.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricBadge(
                        text = protocol.code,
                        textColor = statusColor,
                        backgroundColor = statusBg
                    )
                    Text(
                        text = protocol.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (protocol.isEnabled) TextPrimary else TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${protocol.inspectedEventsCount / 1000}k inspected · ${protocol.violationsBlockedCount} blocked · ${protocol.latencyImpactUs}μs",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive Mode Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .clickable(onClick = onCycleStatus)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("cycle_status_${protocol.code.lowercase()}")
                ) {
                    Text(
                        text = protocol.status.name,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Switch Toggle
                Switch(
                    checked = protocol.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier
                        .scale(0.8f)
                        .testTag("toggle_proto_${protocol.code.lowercase()}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DeepBackground,
                        checkedTrackColor = statusColor,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = CardSurface
                    )
                )
            }
        }
    }
}

/**
 * Privacy Alert Item Card with action pills and token inspector.
 */
@Composable
fun PrivacyAlertItemCard(
    alert: PrivacyAlert,
    onInspect: () -> Unit,
    onQuarantine: () -> Unit,
    onAcknowledge: () -> Unit
) {
    val categoryIcon = when (alert.category) {
        PrivacyCategory.SENSOR_ACCESS -> Icons.Default.Mic
        PrivacyCategory.DATA_LEAK_PREVENTION -> Icons.Default.VisibilityOff
        PrivacyCategory.CRYPTO_KEY_LEAK -> Icons.Default.VpnKey
        PrivacyCategory.DIFFERENTIAL_PRIVACY -> Icons.Default.Grain
        PrivacyCategory.UNAUTHORIZED_EGRESS -> Icons.Default.Http
        PrivacyCategory.ALL -> Icons.Default.Shield
    }

    val actionColor = if (alert.isQuarantined) ThreatCrimson
    else when (alert.severity) {
        ThreatSeverity.CRITICAL -> ThreatCrimson
        ThreatSeverity.HIGH -> Color(0xFFFB923C)
        ThreatSeverity.WARNING -> WarningAmber
        ThreatSeverity.INFO -> NeonCyan
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (alert.isAcknowledged) CardSurface.copy(alpha = 0.6f) else DarkSurface)
            .border(1.dp, actionColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag("privacy_alert_${alert.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = alert.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            // Sub-details & entity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entity: ${alert.sourceEntity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace
                )

                MetricBadge(
                    text = alert.actionTaken,
                    textColor = actionColor,
                    backgroundColor = actionColor.copy(alpha = 0.15f)
                )
            }

            Text(
                text = alert.details,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onInspect,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("inspect_token_${alert.id}")
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect Token", color = NeonCyan, fontSize = 11.sp)
                }

                if (!alert.isQuarantined) {
                    TextButton(
                        onClick = onQuarantine,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("quarantine_source_${alert.id}")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThreatCrimson)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quarantine", color = ThreatCrimson, fontSize = 11.sp)
                    }
                }

                if (!alert.isAcknowledged) {
                    IconButton(
                        onClick = onAcknowledge,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("ack_alert_${alert.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Acknowledge", tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// Small helper extension for scale in Switch
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.padding(0.dp)
)
