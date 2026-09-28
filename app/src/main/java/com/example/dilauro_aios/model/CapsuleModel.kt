package com.example.dilauro_aios.model

enum class CapsuleStatus {
    RUNNING,
    IDLE,
    PAUSED,
    MIGRATING,
    TERMINATED
}

data class Capsule(
    val id: String,
    val name: String,
    val version: String,
    val status: CapsuleStatus,
    val memoryUsageMb: Int,
    val cpuPercent: Float,
    val capabilities: List<String>,
    val targetNode: String = "Local Host (ARM64)",
    val wasiModuleSizeKb: Int = 1420,
    val memoryLimitMb: Int = 256,
    val cpuQuotaPercent: Float = 25.0f,
    val uptimeSeconds: Long = 1840L,
    val isolationTier: String = "WASI SFI (Ring 3)",
    val threadCount: Int = 4,
    val networkIoKbps: Float = 36.4f,
    val syscallCount: Long = 14200L,
    val exitCode: Int? = null
)
