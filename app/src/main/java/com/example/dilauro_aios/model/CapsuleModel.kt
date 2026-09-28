package com.example.dilauro_aios.model

enum class CapsuleStatus {
    RUNNING,
    IDLE,
    PAUSED,
    MIGRATING
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
    val wasiModuleSizeKb: Int = 1420
)
