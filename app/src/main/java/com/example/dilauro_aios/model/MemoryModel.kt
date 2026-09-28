package com.example.dilauro_aios.model

enum class MemoryPressure {
    NORMAL,
    MODERATE,
    CRITICAL
}

data class MemoryTelemetry(
    val totalRamMb: Int = 8192,
    val allocatedMb: Int = 3450,
    val workingSetMb: Int = 2100,
    val coldPagesMb: Int = 1350,
    val compressedMb: Int = 620,
    val compressionRatio: Float = 2.4f,
    val pressure: MemoryPressure = MemoryPressure.NORMAL,
    val totalEvictions: Long = 1840,
    val predictiveAccuracy: Float = 91.8f
)

data class PageEvictionEvent(
    val id: String,
    val timestamp: String,
    val capsuleName: String,
    val pagesEvicted: Int,
    val reason: String
)
