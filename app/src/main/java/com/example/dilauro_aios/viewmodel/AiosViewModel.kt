package com.example.dilauro_aios.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dilauro_aios.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

class AiosViewModel : ViewModel() {

    // --- D-Kernel & Scheduler State ---
    private val _processes = MutableStateFlow<List<Process>>(initialProcesses())
    val processes: StateFlow<List<Process>> = _processes.asStateFlow()

    private val _schedulerTelemetry = MutableStateFlow(SchedulerTelemetry())
    val schedulerTelemetry: StateFlow<SchedulerTelemetry> = _schedulerTelemetry.asStateFlow()

    // --- DAM-Engine Memory State ---
    private val _memoryTelemetry = MutableStateFlow(MemoryTelemetry())
    val memoryTelemetry: StateFlow<MemoryTelemetry> = _memoryTelemetry.asStateFlow()

    private val _evictionLogs = MutableStateFlow<List<PageEvictionEvent>>(initialEvictionLogs())
    val evictionLogs: StateFlow<List<PageEvictionEvent>> = _evictionLogs.asStateFlow()

    // --- SynapseFS Filesystem State ---
    private val _synapseFiles = MutableStateFlow<List<SynapseFile>>(initialSynapseFiles())
    val synapseFiles: StateFlow<List<SynapseFile>> = _synapseFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SemanticSearchResult>>(emptyList())
    val searchResults: StateFlow<List<SemanticSearchResult>> = _searchResults.asStateFlow()

    // --- Capsules State ---
    private val _capsules = MutableStateFlow<List<Capsule>>(initialCapsules())
    val capsules: StateFlow<List<Capsule>> = _capsules.asStateFlow()

    // --- Neural Shield State ---
    private val _securityThreats = MutableStateFlow<List<SecurityThreat>>(initialSecurityThreats())
    val securityThreats: StateFlow<List<SecurityThreat>> = _securityThreats.asStateFlow()

    private val _threatGauge = MutableStateFlow(12f) // Anomaly percentage (0-100%)
    val threatGauge: StateFlow<Float> = _threatGauge.asStateFlow()

    // --- HUT & Entanglement State ---
    private val _hutTelemetry = MutableStateFlow(HutTelemetry())
    val hutTelemetry: StateFlow<HutTelemetry> = _hutTelemetry.asStateFlow()

    // Simulation controls
    private val _simulationRunning = MutableStateFlow(true)
    val simulationRunning: StateFlow<Boolean> = _simulationRunning.asStateFlow()

    init {
        performSemanticSearch("")
        startKernelHeartbeat()
    }

    private fun startKernelHeartbeat() {
        viewModelScope.launch {
            while (true) {
                delay(1200)
                if (_simulationRunning.value) {
                    tickKernelCycle()
                }
            }
        }
    }

    private fun tickKernelCycle() {
        // 1. Tick Processes & Adaptive Scheduler
        _processes.update { currentList ->
            if (currentList.isEmpty()) return@update currentList

            // Pick next running task
            val runningIdx = currentList.indexOfFirst { it.state == ProcessState.RUNNING }
            val nextIdx = if (runningIdx == -1 || runningIdx >= currentList.size - 1) 0 else runningIdx + 1

            currentList.mapIndexed { index, p ->
                val isNowRunning = (index == nextIdx)
                val newBurst = Random.nextLong(8, 25)
                val updatedRing = (p.cpuBurstHistory.takeLast(4) + newBurst)
                val predicted = updatedRing.average().toLong()

                // ML dynamic scoring based on burst stability and latency sensitivity
                val mlScore = when (p.type) {
                    ProcessType.INTERACTIVE -> 0.92f + (Random.nextFloat() * 0.06f)
                    ProcessType.BATCH -> 0.65f + (Random.nextFloat() * 0.1f)
                    ProcessType.DAEMON -> 0.78f + (Random.nextFloat() * 0.08f)
                }

                p.copy(
                    state = if (isNowRunning) ProcessState.RUNNING else ProcessState.READY,
                    cpuTimeUsedMs = if (isNowRunning) p.cpuTimeUsedMs + newBurst else p.cpuTimeUsedMs,
                    cpuBurstHistory = updatedRing,
                    predictedBurstMs = predicted,
                    dynamicScore = mlScore,
                    contextSwitches = if (isNowRunning) p.contextSwitches + 1 else p.contextSwitches
                )
            }
        }

        // 2. Update Scheduler Telemetry
        _schedulerTelemetry.update { prev ->
            prev.copy(
                schedulingCycles = prev.schedulingCycles + 1,
                contextSwitchesPerSec = if (prev.isMlAugmented) Random.nextInt(110, 160) else Random.nextInt(220, 310),
                avgLatencyMs = if (prev.isMlAugmented) 1.8f + Random.nextFloat() * 0.8f else 4.5f + Random.nextFloat() * 1.5f,
                mlPredictionAccuracy = if (prev.isMlAugmented) 94.0f + Random.nextFloat() * 3.5f else 0.0f,
                activeTasksCount = _processes.value.size
            )
        }

        // 3. Gradual threat level cooldown
        if (_threatGauge.value > 15f) {
            _threatGauge.update { (it - 4f).coerceAtLeast(12f) }
        }

        // 4. Memory slight jitter
        _memoryTelemetry.update { mem ->
            val delta = Random.nextInt(-20, 25)
            val newAlloc = (mem.allocatedMb + delta).coerceIn(2800, mem.totalRamMb - 500)
            mem.copy(allocatedMb = newAlloc)
        }
    }

    // --- Scheduler Operations ---
    fun toggleMlScheduler() {
        _schedulerTelemetry.update { it.copy(isMlAugmented = !it.isMlAugmented) }
    }

    fun toggleSimulation() {
        _simulationRunning.update { !it }
    }

    fun spawnProcess(name: String, type: ProcessType, priority: Int) {
        val nextPid = (_processes.value.maxOfOrNull { it.pid } ?: 100) + Random.nextInt(1, 15)
        val newProc = Process(
            pid = nextPid,
            name = name.ifBlank { "capsule_task_$nextPid" },
            type = type,
            priority = priority.coerceIn(1, 10),
            state = ProcessState.READY
        )
        _processes.update { it + newProc }
    }

    fun terminateProcess(pid: Int) {
        _processes.update { it.filter { p -> p.pid != pid } }
    }

    fun boostPriority(pid: Int) {
        _processes.update { list ->
            list.map { if (it.pid == pid) it.copy(priority = (it.priority + 2).coerceAtMost(10)) else it }
        }
    }

    // --- DAM Memory Operations ---
    fun setMemoryPressure(pressure: MemoryPressure) {
        _memoryTelemetry.update { mem ->
            when (pressure) {
                MemoryPressure.NORMAL -> mem.copy(
                    pressure = pressure,
                    allocatedMb = 3450,
                    workingSetMb = 2100,
                    coldPagesMb = 1350
                )
                MemoryPressure.MODERATE -> {
                    // Trigger predictive pre-eviction
                    triggerPredictiveEviction("DAM Memory Pressure (Moderate): Pre-evicted inactive cold pages.")
                    mem.copy(
                        pressure = pressure,
                        allocatedMb = 4850,
                        workingSetMb = 3100,
                        coldPagesMb = 1750,
                        totalEvictions = mem.totalEvictions + 128
                    )
                }
                MemoryPressure.CRITICAL -> {
                    // High urgency eviction
                    triggerPredictiveEviction("CRITICAL MEMORY: Evicted 512 cold pages across background capsules.")
                    mem.copy(
                        pressure = pressure,
                        allocatedMb = 6900,
                        workingSetMb = 4600,
                        coldPagesMb = 2300,
                        totalEvictions = mem.totalEvictions + 512
                    )
                }
            }
        }
    }

    fun triggerCompaction() {
        _memoryTelemetry.update { mem ->
            val reclaimed = 380
            triggerPredictiveEviction("Manual DAM compaction: Compressed $reclaimed MB into zRAM pool.")
            mem.copy(
                allocatedMb = (mem.allocatedMb - reclaimed).coerceAtLeast(1500),
                compressedMb = mem.compressedMb + reclaimed,
                compressionRatio = 2.8f,
                totalEvictions = mem.totalEvictions + 240
            )
        }
    }

    private fun triggerPredictiveEviction(reason: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val event = PageEvictionEvent(
            id = UUID.randomUUID().toString(),
            timestamp = sdf.format(Date()),
            capsuleName = _capsules.value.randomOrNull()?.name ?: "d_kernel",
            pagesEvicted = Random.nextInt(64, 512),
            reason = reason
        )
        _evictionLogs.update { (listOf(event) + it).take(20) }
    }

    // --- SynapseFS Operations ---
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        performSemanticSearch(query)
    }

    fun performSemanticSearch(query: String) {
        val files = _synapseFiles.value
        if (query.isBlank()) {
            _searchResults.value = files.map { SemanticSearchResult(it, 1.0f) }
            return
        }

        val tokens = query.lowercase().split(" ", "_", "-", "/").filter { it.isNotBlank() }
        val scored = files.map { file ->
            var score = 0.0f
            val searchableText = "${file.filename} ${file.path} ${file.semanticDescription} ${file.tags.joinToString(" ")} ${file.contextAffinity}".lowercase()
            for (token in tokens) {
                if (searchableText.contains(token)) {
                    score += 0.35f
                }
            }
            // Add semantic pseudo-cosine similarity
            val pseudoCosine = 0.4f + (abs(file.filename.hashCode() % 50) / 100.0f)
            val finalScore = (score + pseudoCosine).coerceIn(0.1f, 0.99f)
            SemanticSearchResult(file, finalScore)
        }.sortedByDescending { it.similarityScore }

        _searchResults.value = scored
    }

    fun addSynapseFile(filename: String, path: String, category: FileCategory, tags: List<String>, description: String) {
        val newFile = SynapseFile(
            id = UUID.randomUUID().toString(),
            path = path.ifBlank { "/user/documents/" },
            filename = filename.ifBlank { "note_${System.currentTimeMillis()}.syn" },
            sizeKb = Random.nextInt(12, 1024),
            category = category,
            tags = if (tags.isEmpty()) listOf("custom", "user") else tags,
            semanticDescription = description.ifBlank { "User semantic note indexed by SynapseFS vector engine." },
            accessCount = 1,
            contextAffinity = "User Space, Interactive",
            embeddingPreview = listOf(0.24f, -0.19f, 0.77f, 0.42f)
        )
        _synapseFiles.update { listOf(newFile) + it }
        performSemanticSearch(_searchQuery.value)
    }

    fun deleteSynapseFile(id: String) {
        _synapseFiles.update { it.filter { f -> f.id != id } }
        performSemanticSearch(_searchQuery.value)
    }

    // --- Capsule WASI Operations ---
    fun toggleCapsuleState(id: String) {
        _capsules.update { list ->
            list.map { capsule ->
                if (capsule.id == id) {
                    val nextStatus = if (capsule.status == CapsuleStatus.RUNNING) CapsuleStatus.PAUSED else CapsuleStatus.RUNNING
                    capsule.copy(status = nextStatus)
                } else capsule
            }
        }
    }

    fun hotSwapCapsule(id: String) {
        _capsules.update { list ->
            list.map { capsule ->
                if (capsule.id == id) {
                    val currentVersionParts = capsule.version.split(".").mapNotNull { it.toIntOrNull() }
                    val newVersion = if (currentVersionParts.size == 3) {
                        "${currentVersionParts[0]}.${currentVersionParts[1]}.${currentVersionParts[2] + 1}"
                    } else "2.0.0"
                    capsule.copy(
                        version = newVersion,
                        wasiModuleSizeKb = capsule.wasiModuleSizeKb + Random.nextInt(10, 50)
                    )
                } else capsule
            }
        }
    }

    fun migrateCapsule(id: String) {
        _capsules.update { list ->
            list.map { capsule ->
                if (capsule.id == id) {
                    val newTarget = if (capsule.targetNode.contains("Local")) "Raspberry Pi 5 (Node-02)" else "Local Host (ARM64)"
                    capsule.copy(
                        status = CapsuleStatus.MIGRATING,
                        targetNode = newTarget
                    )
                } else capsule
            }
        }

        // Simulate migration completion after 2 seconds
        viewModelScope.launch {
            delay(2000)
            _capsules.update { list ->
                list.map { if (it.id == id) it.copy(status = CapsuleStatus.RUNNING) else it }
            }
        }
    }

    fun deployCapsule(name: String, capabilities: List<String>) {
        val nextId = UUID.randomUUID().toString().take(8)
        val newCapsule = Capsule(
            id = nextId,
            name = name.ifBlank { "capsule_$nextId" },
            version = "1.0.0",
            status = CapsuleStatus.RUNNING,
            memoryUsageMb = Random.nextInt(80, 240),
            cpuPercent = Random.nextFloat() * 12f,
            capabilities = if (capabilities.isEmpty()) listOf("capsule.fs:ro", "capsule.sys:telemetry") else capabilities
        )
        _capsules.update { it + newCapsule }
    }

    // --- Neural Shield Operations ---
    fun simulateThreatVector(vectorType: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val threat = when (vectorType) {
            "SYSCALL_ANOMALY" -> SecurityThreat(
                id = UUID.randomUUID().toString(),
                timestamp = time,
                sourceCapsule = "rogue_wasi_probe",
                syscallName = "sys_kernel_map_phys",
                anomalyScore = 0.94f,
                severity = ThreatSeverity.CRITICAL,
                actionTaken = ThreatAction.QUARANTINED,
                details = "Syscall frequency anomaly: Burst of 420 calls/sec exceeds behavioral baseline."
            )
            "MEMORY_SANDBOX_BREACH" -> SecurityThreat(
                id = UUID.randomUUID().toString(),
                timestamp = time,
                sourceCapsule = "untrusted_plugin",
                syscallName = "sys_shm_write",
                anomalyScore = 0.88f,
                severity = ThreatSeverity.HIGH,
                actionTaken = ThreatAction.THROTTLED,
                details = "Attempted out-of-bounds pointer write past WASI linear memory bounds."
            )
            else -> SecurityThreat(
                id = UUID.randomUUID().toString(),
                timestamp = time,
                sourceCapsule = "entanglement_sync",
                syscallName = "sys_ipc_send_token",
                anomalyScore = 0.74f,
                severity = ThreatSeverity.WARNING,
                actionTaken = ThreatAction.FLAGGED,
                details = "Zero-trust IPC capability verification failed: expired cryptographic token."
            )
        }

        _securityThreats.update { (listOf(threat) + it).take(20) }
        _threatGauge.update { (it + 45f).coerceAtMost(98f) }
    }

    fun clearThreatAudit() {
        _securityThreats.value = emptyList()
        _threatGauge.value = 10f
    }

    // --- HUT Operations ---
    fun flushJitCache() {
        _hutTelemetry.update {
            it.copy(
                translatedInstructionsTotal = it.translatedInstructionsTotal + 250000,
                jitCacheHitRate = 99.1f
            )
        }
    }

    // Initial Data Helpers
    private fun initialProcesses(): List<Process> = listOf(
        Process(
            pid = 1,
            name = "d_kernel_init",
            type = ProcessType.DAEMON,
            priority = 10,
            state = ProcessState.RUNNING,
            cpuBurstHistory = listOf(8, 7, 9, 8, 9),
            cpuTimeUsedMs = 1240,
            predictedBurstMs = 8,
            dynamicScore = 0.98f
        ),
        Process(
            pid = 24,
            name = "liquid_ui_compositor",
            type = ProcessType.INTERACTIVE,
            priority = 9,
            state = ProcessState.READY,
            cpuBurstHistory = listOf(16, 17, 16, 15, 16),
            cpuTimeUsedMs = 890,
            predictedBurstMs = 16,
            dynamicScore = 0.94f
        ),
        Process(
            pid = 82,
            name = "synapse_vector_indexer",
            type = ProcessType.BATCH,
            priority = 5,
            state = ProcessState.READY,
            cpuBurstHistory = listOf(45, 52, 48, 60, 50),
            cpuTimeUsedMs = 3120,
            predictedBurstMs = 51,
            dynamicScore = 0.62f
        ),
        Process(
            pid = 104,
            name = "neural_shield_sentinel",
            type = ProcessType.DAEMON,
            priority = 8,
            state = ProcessState.READY,
            cpuBurstHistory = listOf(12, 11, 14, 13, 12),
            cpuTimeUsedMs = 540,
            predictedBurstMs = 12,
            dynamicScore = 0.88f
        ),
        Process(
            pid = 142,
            name = "dam_memory_telemetry",
            type = ProcessType.DAEMON,
            priority = 7,
            state = ProcessState.READY,
            cpuBurstHistory = listOf(10, 9, 10, 11, 10),
            cpuTimeUsedMs = 380,
            predictedBurstMs = 10,
            dynamicScore = 0.82f
        )
    )

    private fun initialEvictionLogs(): List<PageEvictionEvent> = listOf(
        PageEvictionEvent("e1", "19:14:02", "synapse_vector_indexer", 128, "Predictive cold page reclaim prior to burst"),
        PageEvictionEvent("e2", "19:10:45", "liquid_ui_compositor", 64, "Inactive texture buffer eviction to zRAM"),
        PageEvictionEvent("e3", "19:05:12", "entangle_mesh_daemon", 256, "Background peer cache compaction")
    )

    private fun initialSynapseFiles(): List<SynapseFile> = listOf(
        SynapseFile(
            id = "f1",
            path = "/kernel/",
            filename = "d_kernel_core.rs",
            sizeKb = 245,
            category = FileCategory.KERNEL_CORE,
            tags = listOf("kernel", "microkernel", "rust", "sel4"),
            semanticDescription = "D-Kernel core microkernel entry point with capability token verification and IPC endpoints.",
            accessCount = 89,
            contextAffinity = "Boot, Core Execution"
        ),
        SynapseFile(
            id = "f2",
            path = "/sys/sched/",
            filename = "adaptive_weights.bin",
            sizeKb = 64,
            category = FileCategory.NEURAL_WEIGHTS,
            tags = listOf("scheduler", "ml", "weights", "burst"),
            semanticDescription = "Decision-tree ML weights for process classification and dynamic burst prediction.",
            accessCount = 312,
            contextAffinity = "Scheduler Telemetry"
        ),
        SynapseFile(
            id = "f3",
            path = "/capsules/liquid_ui/",
            filename = "app_shell.wasm",
            sizeKb = 820,
            category = FileCategory.WASI_BYTECODE,
            tags = listOf("wasi", "ui", "liquid", "impeller"),
            semanticDescription = "Liquid UI context-aware layout engine compiled to WebAssembly WASI micro-container.",
            accessCount = 145,
            contextAffinity = "Display Compositor"
        ),
        SynapseFile(
            id = "f4",
            path = "/sec/neural_shield/",
            filename = "anomaly_rules.json",
            sizeKb = 32,
            category = FileCategory.SECURITY_POLICY,
            tags = listOf("security", "ebpf", "rules", "anomaly"),
            semanticDescription = "Heuristic behavioral baseline rules for syscall anomaly scoring and zero-trust validation.",
            accessCount = 98,
            contextAffinity = "Security Sentry"
        ),
        SynapseFile(
            id = "f5",
            path = "/hut/",
            filename = "x86_to_arm64_jit.tbl",
            sizeKb = 512,
            category = FileCategory.KERNEL_CORE,
            tags = listOf("hut", "translator", "jit", "rosetta"),
            semanticDescription = "Universal Hardware Translator jump table for cross-architecture x86_64 to ARM64 execution.",
            accessCount = 67,
            contextAffinity = "Binary Translation"
        ),
        SynapseFile(
            id = "f6",
            path = "/synapse/embeddings/",
            filename = "contextual_user.vec",
            sizeKb = 1240,
            category = FileCategory.NEURAL_WEIGHTS,
            tags = listOf("synapse", "vector", "faiss", "semantic"),
            semanticDescription = "Dense semantic vector index mapping file contexts, temporal affinities, and user intent.",
            accessCount = 210,
            contextAffinity = "Filesystem Search"
        )
    )

    private fun initialCapsules(): List<Capsule> = listOf(
        Capsule(
            id = "cap_liquid",
            name = "Liquid Desk Shell",
            version = "1.2.0",
            status = CapsuleStatus.RUNNING,
            memoryUsageMb = 210,
            cpuPercent = 4.2f,
            capabilities = listOf("capsule.ui:compositor", "capsule.fs:ro", "capsule.sys:input")
        ),
        Capsule(
            id = "cap_sentry",
            name = "Neural Sentry",
            version = "1.0.4",
            status = CapsuleStatus.RUNNING,
            memoryUsageMb = 85,
            cpuPercent = 1.8f,
            capabilities = listOf("capsule.sec:ebpf", "capsule.ipc:audit", "capsule.sys:telemetry")
        ),
        Capsule(
            id = "cap_indexer",
            name = "Synapse Vector Indexer",
            version = "2.1.0",
            status = CapsuleStatus.RUNNING,
            memoryUsageMb = 340,
            cpuPercent = 6.5f,
            capabilities = listOf("capsule.fs:rw", "capsule.neural:faiss", "capsule.sys:timer")
        ),
        Capsule(
            id = "cap_entangle",
            name = "Entanglement Mesh Sync",
            version = "1.1.2",
            status = CapsuleStatus.IDLE,
            memoryUsageMb = 120,
            cpuPercent = 0.8f,
            capabilities = listOf("capsule.net:libp2p", "capsule.net:mdns", "capsule.ipc:bridge")
        )
    )

    private fun initialSecurityThreats(): List<SecurityThreat> = listOf(
        SecurityThreat(
            id = "t1",
            timestamp = "19:15:30",
            sourceCapsule = "unverified_wasm_guest",
            syscallName = "sys_mem_prot_exec",
            anomalyScore = 0.92f,
            severity = ThreatSeverity.HIGH,
            actionTaken = ThreatAction.QUARANTINED,
            details = "Attempted dynamic code allocation violating W^X security invariants."
        ),
        SecurityThreat(
            id = "t2",
            timestamp = "19:08:14",
            sourceCapsule = "mesh_discovery",
            syscallName = "sys_socket_bind_raw",
            anomalyScore = 0.68f,
            severity = ThreatSeverity.WARNING,
            actionTaken = ThreatAction.FLAGGED,
            details = "Raw socket bind attempt without capability token 'capsule.net:raw'."
        )
    )
}
