package com.example.dilauro_aios.model

enum class ProcessType {
    INTERACTIVE,
    BATCH,
    DAEMON
}

enum class ProcessState {
    READY,
    RUNNING,
    BLOCKED
}

/**
 * Process Control Block (PCB) representing an active task in the D-Kernel.
 * Preserves the architecture described in RFC-0001 (Adaptive Scheduler Design).
 */
data class Process(
    val pid: Int,
    val name: String,
    val type: ProcessType,
    val priority: Int, // 1 (lowest) to 10 (highest)
    val state: ProcessState = ProcessState.READY,
    val cpuBurstHistory: List<Long> = listOf(12, 14, 18, 15, 20), // Telemetry ring buffer
    val cpuTimeUsedMs: Long = 0,
    val predictedBurstMs: Long = 15,
    val dynamicScore: Float = 0.85f,
    val contextSwitches: Long = 0,
    val capsuleId: String? = null
)

data class SchedulerTelemetry(
    val isMlAugmented: Boolean = true,
    val schedulingCycles: Long = 0,
    val contextSwitchesPerSec: Int = 142,
    val avgLatencyMs: Float = 2.4f,
    val mlPredictionAccuracy: Float = 94.2f,
    val activeTasksCount: Int = 5,
    val timeSliceMs: Int = 20
)
