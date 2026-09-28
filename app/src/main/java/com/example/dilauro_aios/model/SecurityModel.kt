package com.example.dilauro_aios.model

enum class ThreatSeverity {
    INFO,
    WARNING,
    HIGH,
    CRITICAL
}

enum class ThreatAction {
    ALLOWED,
    FLAGGED,
    THROTTLED,
    QUARANTINED
}

data class SecurityThreat(
    val id: String,
    val timestamp: String,
    val sourceCapsule: String,
    val syscallName: String,
    val anomalyScore: Float, // 0.0 to 1.0
    val severity: ThreatSeverity,
    val actionTaken: ThreatAction,
    val details: String
)

data class HutTelemetry(
    val translatedInstructionsTotal: Long = 14285900,
    val jitCacheHitRate: Float = 98.4f,
    val sourceArch: String = "x86_64 ELF",
    val destArch: String = "AArch64 (ARMv9)",
    val translationLatencyUs: Float = 14.2f,
    val connectedMeshPeers: Int = 3
)
