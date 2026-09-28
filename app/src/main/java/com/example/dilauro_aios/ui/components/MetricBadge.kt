package com.example.dilauro_aios.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.CapsuleStatus
import com.example.dilauro_aios.model.ProcessType
import com.example.dilauro_aios.model.ThreatSeverity
import com.example.dilauro_aios.ui.theme.*

@Composable
fun MetricBadge(
    text: String,
    textColor: Color = NeonCyan,
    backgroundColor: Color = CardSurfaceHighlight,
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(6.dp))
                else Modifier
            )
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ProcessTypeBadge(type: ProcessType, modifier: Modifier = Modifier) {
    val (text, color, bg) = when (type) {
        ProcessType.INTERACTIVE -> Triple("INTERACTIVE", NeonCyan, Color(0xFF003B46))
        ProcessType.AI_INFERENCE -> Triple("AI INFERENCE", Color(0xFFC084FC), Color(0xFF3B0764))
        ProcessType.BATCH -> Triple("BATCH", WarningAmber, Color(0xFF3E2723))
        ProcessType.DAEMON -> Triple("DAEMON", ElectricViolet, Color(0xFF2C1654))
    }
    MetricBadge(text = text, textColor = color, backgroundColor = bg, modifier = modifier)
}

@Composable
fun CapsuleStatusBadge(status: CapsuleStatus, modifier: Modifier = Modifier) {
    val (text, color, bg) = when (status) {
        CapsuleStatus.RUNNING -> Triple("RUNNING", EmeraldSecurity, Color(0xFF064E3B))
        CapsuleStatus.IDLE -> Triple("IDLE", TextSecondary, CardSurfaceHighlight)
        CapsuleStatus.PAUSED -> Triple("PAUSED", WarningAmber, Color(0xFF451A03))
        CapsuleStatus.MIGRATING -> Triple("MIGRATING", NeonCyan, Color(0xFF083344))
    }
    MetricBadge(text = text, textColor = color, backgroundColor = bg, modifier = modifier)
}

@Composable
fun ThreatSeverityBadge(severity: ThreatSeverity, modifier: Modifier = Modifier) {
    val (text, color, bg) = when (severity) {
        ThreatSeverity.INFO -> Triple("INFO", TextCyan, Color(0xFF0C4A6E))
        ThreatSeverity.WARNING -> Triple("WARNING", WarningAmber, Color(0xFF451A03))
        ThreatSeverity.HIGH -> Triple("HIGH", Color(0xFFFB923C), Color(0xFF431407))
        ThreatSeverity.CRITICAL -> Triple("CRITICAL", ThreatCrimson, Color(0xFF450A0A))
    }
    MetricBadge(text = text, textColor = color, backgroundColor = bg, modifier = modifier)
}
