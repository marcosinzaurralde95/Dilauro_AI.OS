package com.example.dilauro_aios.model

enum class ProcessType {
    INTERACTIVE,
    AI_INFERENCE,
    BATCH,
    DAEMON
}

enum class ProcessState {
    READY,
    RUNNING,
    BLOCKED
}

enum class SchedulerPolicyMode {
    BALANCED_ADAPTIVE,
    AI_TURBO_INFERENCE,
    ULTRA_LOW_LATENCY,
    EFFICIENCY_SAVER
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
    val capsuleId: String? = null,
    val assignedCoreId: Int = 0,
    val aiWorkloadWeight: Float = 0.2f, // 0.0 to 1.0 (tensor / neural ratio)
    val quantumTimeSliceMs: Int = 16,
    val preemptionDeadlineMs: Long = 24,
    val tensorFlopsEst: String = "N/A"
)

data class CpuCoreState(
    val coreId: Int,
    val name: String,
    val type: String,
    val loadPercent: Float,
    val tempCelsius: Float,
    val frequencyGhz: Float,
    val activeProcessName: String?,
    val isTensorCore: Boolean = false
)

data class AiWorkloadTelemetry(
    val tensorTflops: Float = 3.84f,
    val npuUtilizationPercent: Float = 76.5f,
    val activeAiPipelinesCount: Int = 3,
    val neuralCacheHitRate: Float = 96.8f,
    val currentPolicy: SchedulerPolicyMode = SchedulerPolicyMode.BALANCED_ADAPTIVE,
    val totalAiInferenceBursts: Long = 184520,
    val latencyP99Ms: Float = 3.2f
)

data class SchedulerTelemetry(
    val isMlAugmented: Boolean = true,
    val schedulingCycles: Long = 0,
    val contextSwitchesPerSec: Int = 142,
    val avgLatencyMs: Float = 2.4f,
    val mlPredictionAccuracy: Float = 94.2f,
    val activeTasksCount: Int = 6,
    val timeSliceMs: Int = 16,
    val policyMode: SchedulerPolicyMode = SchedulerPolicyMode.BALANCED_ADAPTIVE
)
