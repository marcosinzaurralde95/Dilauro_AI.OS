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

enum class ProtocolStatus {
    ENFORCING,
    HARDENED,
    MONITORING,
    BYPASSED
}

enum class ProtocolCategory {
    ZERO_TRUST,
    MEMORY_HARDENING,
    DATA_PRIVACY,
    KERNEL_INTEGRITY,
    NETWORK_ISOLATION
}

data class SecurityProtocol(
    val id: String,
    val code: String,
    val name: String,
    val description: String,
    val category: ProtocolCategory,
    val status: ProtocolStatus,
    val isEnabled: Boolean = true,
    val inspectedEventsCount: Long = 142050,
    val violationsBlockedCount: Int = 14,
    val latencyImpactUs: Float = 4.2f,
    val enforcedRulesCount: Int = 38
)

enum class PrivacyCategory {
    ALL,
    SENSOR_ACCESS,
    DATA_LEAK_PREVENTION,
    CRYPTO_KEY_LEAK,
    DIFFERENTIAL_PRIVACY,
    UNAUTHORIZED_EGRESS
}

data class PrivacyAlert(
    val id: String,
    val timestamp: String,
    val title: String,
    val sourceEntity: String,
    val category: PrivacyCategory,
    val severity: ThreatSeverity,
    val actionTaken: String,
    val details: String,
    val tokenFingerprint: String,
    val remediationHint: String,
    val isQuarantined: Boolean = false,
    val isAcknowledged: Boolean = false
)

data class ShieldStatusSummary(
    val overallIntegrityScore: Float = 99.4f,
    val activeProtocolsCount: Int = 6,
    val totalProtocolsCount: Int = 6,
    val totalThreatsBlocked: Int = 42,
    val privacyScore: Float = 99.8f,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val lastAuditTimestamp: String = "Just now"
)

data class HutTelemetry(
    val translatedInstructionsTotal: Long = 14285900,
    val jitCacheHitRate: Float = 98.4f,
    val sourceArch: String = "x86_64 ELF",
    val destArch: String = "AArch64 (ARMv9)",
    val translationLatencyUs: Float = 14.2f,
    val connectedMeshPeers: Int = 3
)
