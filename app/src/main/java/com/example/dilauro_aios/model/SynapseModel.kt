package com.example.dilauro_aios.model

enum class FileCategory {
    KERNEL_CORE,
    NEURAL_WEIGHTS,
    WASI_BYTECODE,
    SECURITY_POLICY,
    TELEMETRY_LOGS,
    USER_DATA
}

enum class IndexingStatus {
    INDEXED,
    OPTIMIZING,
    QUANTIZED,
    PENDING
}

enum class TreeMapSizing(val label: String) {
    STORAGE_SIZE("Storage Size"),
    RETRIEVAL_HEAT("Retrieval Heat"),
    SEMANTIC_SIMILARITY("Query Similarity")
}

enum class TreeMapColoring(val label: String) {
    BY_CLUSTER("Semantic Cluster"),
    BY_RETRIEVAL_HEAT("Access Frequency Heat"),
    BY_SIMILARITY("Cosine Match Glow")
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
    val embeddingPreview: List<Float> = listOf(0.12f, -0.45f, 0.88f, 0.31f),
    val cluster: String = "Core & System VFS",
    val indexingStatus: IndexingStatus = IndexingStatus.INDEXED,
    val recentRetrievalFrequency: Float = 0.65f, // 0.0 to 1.0 (Heat)
    val retrievalLatencyMs: Float = 1.4f,
    val cacheHitRate: Float = 96.2f,
    val tokenCount: Int = 1240,
    val vectorDimension: Int = 768,
    val prefetchAffinity: String = "Prefetched with /sys/sched/adaptive_weights.bin",
    val lastRetrievedTimestamp: String = "19:42:15"
)

data class SemanticSearchResult(
    val file: SynapseFile,
    val similarityScore: Float // 0.0 to 1.0 (Cosine similarity)
)

data class SynapseFsTelemetry(
    val totalFilesIndexed: Int = 14,
    val totalIndexSizeKb: Long = 18420,
    val activeQuery: String = "",
    val cacheHitRatio: Float = 94.8f,
    val avgRetrievalLatencyMs: Float = 1.35f,
    val vectorEmbeddingDimension: Int = 768,
    val hnswGraphNodes: Int = 18450,
    val hnswLayers: Int = 16,
    val quantizationMode: String = "PQ-8 (Product Quantization)",
    val prefetchAccuracyPercent: Float = 92.4f,
    val isReindexing: Boolean = false,
    val reindexingProgress: Float = 0f,
    val totalRetrievalsHandled: Long = 48291L,
    val hotInodesCount: Int = 5,
    val coldInodesCount: Int = 3
)

data class TreemapRect(
    val id: String,
    val file: SynapseFile,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val similarityScore: Float = 1.0f
)
