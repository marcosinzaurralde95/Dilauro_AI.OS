package com.example.dilauro_aios.model

enum class FileCategory {
    KERNEL_CORE,
    NEURAL_WEIGHTS,
    WASI_BYTECODE,
    SECURITY_POLICY,
    TELEMETRY_LOGS,
    USER_DATA
}

data class SynapseFile(
    val id: String,
    val path: String,
    val filename: String,
    val sizeKb: Int,
    val category: FileCategory,
    val tags: List<String>,
    val semanticDescription: String,
    val accessCount: Int,
    val contextAffinity: String, // e.g. "Scheduler, Nightly, Batch"
    val embeddingPreview: List<Float> = listOf(0.12f, -0.45f, 0.88f, 0.31f)
)

data class SemanticSearchResult(
    val file: SynapseFile,
    val similarityScore: Float // 0.0 to 1.0 (Cosine similarity)
)
