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

    private val _cpuCores = MutableStateFlow<List<CpuCoreState>>(initialCpuCores())
    val cpuCores: StateFlow<List<CpuCoreState>> = _cpuCores.asStateFlow()

    private val _aiWorkloadTelemetry = MutableStateFlow(AiWorkloadTelemetry())
    val aiWorkloadTelemetry: StateFlow<AiWorkloadTelemetry> = _aiWorkloadTelemetry.asStateFlow()

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

    private val _synapseFsTelemetry = MutableStateFlow(SynapseFsTelemetry())
    val synapseFsTelemetry: StateFlow<SynapseFsTelemetry> = _synapseFsTelemetry.asStateFlow()

    // --- Capsules State ---
    private val _capsules = MutableStateFlow<List<Capsule>>(initialCapsules())
    val capsules: StateFlow<List<Capsule>> = _capsules.asStateFlow()

    // --- Neural Shield & Privacy State ---
    private val _securityThreats = MutableStateFlow<List<SecurityThreat>>(initialSecurityThreats())
    val securityThreats: StateFlow<List<SecurityThreat>> = _securityThreats.asStateFlow()

    private val _threatGauge = MutableStateFlow(12f) // Anomaly percentage (0-100%)
    val threatGauge: StateFlow<Float> = _threatGauge.asStateFlow()

    private val _protocols = MutableStateFlow<List<SecurityProtocol>>(initialSecurityProtocols())
    val protocols: StateFlow<List<SecurityProtocol>> = _protocols.asStateFlow()

    private val _privacyAlerts = MutableStateFlow<List<PrivacyAlert>>(initialPrivacyAlerts())
    val privacyAlerts: StateFlow<List<PrivacyAlert>> = _privacyAlerts.asStateFlow()

    private val _shieldSummary = MutableStateFlow(ShieldStatusSummary())
    val shieldSummary: StateFlow<ShieldStatusSummary> = _shieldSummary.asStateFlow()

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
                val newBurst = Random.nextLong(8, 28)
                val updatedRing = (p.cpuBurstHistory.takeLast(4) + newBurst)
                val predicted = updatedRing.average().toLong()

                // ML dynamic scoring based on burst stability and latency sensitivity
                val mlScore = when (p.type) {
                    ProcessType.INTERACTIVE -> 0.92f + (Random.nextFloat() * 0.06f)
                    ProcessType.AI_INFERENCE -> 0.96f + (Random.nextFloat() * 0.03f)
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

        // 2. Update Multi-Core Workload States
        val currentProcesses = _processes.value
        _cpuCores.update { cores ->
            cores.map { core ->
                val activeOnCore = currentProcesses.firstOrNull { it.assignedCoreId == core.coreId && it.state == ProcessState.RUNNING }
                    ?: currentProcesses.firstOrNull { it.assignedCoreId == core.coreId }
                val targetLoad = if (core.isTensorCore) {
                    val aiCount = currentProcesses.count { it.type == ProcessType.AI_INFERENCE }
                    (52f + aiCount * 14f).coerceIn(40f, 96f)
                } else if (activeOnCore?.state == ProcessState.RUNNING) {
                    (activeOnCore.dynamicScore * 85f).coerceIn(45f, 95f)
                } else {
                    Random.nextFloat() * 15f + 18f
                }
                core.copy(
                    loadPercent = targetLoad,
                    activeProcessName = activeOnCore?.name ?: "idle"
                )
            }
        }

        // 3. Update AI Workload Telemetry
        _aiWorkloadTelemetry.update { prev ->
            val aiTasks = currentProcesses.filter { it.type == ProcessType.AI_INFERENCE }
            val tflopsSum = aiTasks.sumOf { (it.aiWorkloadWeight * 4.8).toDouble() }.toFloat().coerceAtLeast(1.2f)
            prev.copy(
                tensorTflops = tflopsSum,
                npuUtilizationPercent = _cpuCores.value.find { it.isTensorCore }?.loadPercent ?: 78f,
                activeAiPipelinesCount = aiTasks.size,
                totalAiInferenceBursts = prev.totalAiInferenceBursts + aiTasks.size * 3L
            )
        }

        // 4. Update Scheduler Telemetry
        _schedulerTelemetry.update { prev ->
            prev.copy(
                schedulingCycles = prev.schedulingCycles + 1,
                contextSwitchesPerSec = if (prev.isMlAugmented) Random.nextInt(110, 160) else Random.nextInt(220, 310),
                avgLatencyMs = if (prev.isMlAugmented) 1.8f + Random.nextFloat() * 0.8f else 4.5f + Random.nextFloat() * 1.5f,
                mlPredictionAccuracy = if (prev.isMlAugmented) 94.0f + Random.nextFloat() * 3.5f else 0.0f,
                activeTasksCount = _processes.value.size
            )
        }

        // 5. Gradual threat level cooldown
        if (_threatGauge.value > 15f) {
            _threatGauge.update { (it - 4f).coerceAtLeast(12f) }
        }

        // 6. Memory slight jitter
        _memoryTelemetry.update { mem ->
            val delta = Random.nextInt(-20, 25)
            val newAlloc = (mem.allocatedMb + delta).coerceIn(2800, mem.totalRamMb - 500)
            mem.copy(allocatedMb = newAlloc)
        }
    }

    // --- Scheduler Operations & AI Workload Balancing ---
    fun setSchedulerPolicy(policy: SchedulerPolicyMode) {
        _schedulerTelemetry.update { it.copy(policyMode = policy) }
        _aiWorkloadTelemetry.update { it.copy(currentPolicy = policy) }
        rebalanceAiWorkloads()
    }

    fun rebalanceAiWorkloads() {
        val policy = _schedulerTelemetry.value.policyMode
        _processes.update { list ->
            list.map { p ->
                val targetCore = when {
                    p.type == ProcessType.AI_INFERENCE -> 1 // NPU / Tensor Core
                    p.type == ProcessType.INTERACTIVE -> if (policy == SchedulerPolicyMode.ULTRA_LOW_LATENCY) 0 else 0
                    p.type == ProcessType.BATCH -> 2 // Efficiency A720
                    else -> 3 // Efficiency A520 Daemon
                }
                val boostScore = if (p.type == ProcessType.AI_INFERENCE && policy == SchedulerPolicyMode.AI_TURBO_INFERENCE) 0.98f else p.dynamicScore
                p.copy(assignedCoreId = targetCore, dynamicScore = boostScore)
            }
        }
    }

    fun assignProcessToCore(pid: Int, coreId: Int) {
        _processes.update { list ->
            list.map { if (it.pid == pid) it.copy(assignedCoreId = coreId.coerceIn(0, 3)) else it }
        }
    }

    fun injectAiWorkload(taskType: String) {
        val nextPid = (_processes.value.maxOfOrNull { it.pid } ?: 100) + Random.nextInt(1, 10)
        val (name, flops, weight, prio) = when (taskType) {
            "LLM_KV_PREFILL" -> Quadruple("gemini_kv_cache_prefill", "6.4 TFLOPS", 0.95f, 9)
            "FAISS_MATMUL" -> Quadruple("faiss_dense_embedding_batch", "4.8 TFLOPS", 0.88f, 8)
            "VISION_TOKENIZER" -> Quadruple("impeller_vision_tokenizer", "5.2 TFLOPS", 0.90f, 8)
            "DIFFUSION_DENOISE" -> Quadruple("diffusion_unet_denoise", "7.1 TFLOPS", 0.98f, 10)
            else -> Quadruple("neural_anomaly_scoring", "3.1 TFLOPS", 0.75f, 7)
        }
        val newProc = Process(
            pid = nextPid,
            name = name,
            type = ProcessType.AI_INFERENCE,
            priority = prio,
            state = ProcessState.READY,
            assignedCoreId = 1, // Auto-assign to Tensor / NPU Core
            aiWorkloadWeight = weight,
            tensorFlopsEst = flops,
            dynamicScore = 0.96f
        )
        _processes.update { it + newProc }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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

    fun updateTaskPriority(pid: Int, newPriority: Int) {
        _processes.update { list ->
            list.map { p ->
                if (p.pid == pid) {
                    val clamped = newPriority.coerceIn(1, 10)
                    val newScore = (clamped / 10f) * 0.95f + 0.05f
                    p.copy(priority = clamped, dynamicScore = newScore)
                } else p
            }
        }
    }

    fun preemptTaskNow(pid: Int) {
        _processes.update { list ->
            list.map { p ->
                if (p.pid == pid) {
                    p.copy(
                        state = ProcessState.RUNNING,
                        priority = 10,
                        dynamicScore = 0.99f,
                        contextSwitches = p.contextSwitches + 1
                    )
                } else if (p.state == ProcessState.RUNNING) {
                    p.copy(state = ProcessState.READY)
                } else p
            }
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
        _synapseFsTelemetry.update { prev ->
            prev.copy(
                activeQuery = query,
                totalRetrievalsHandled = prev.totalRetrievalsHandled + 1
            )
        }
        if (query.isBlank()) {
            _searchResults.value = files.map { SemanticSearchResult(it, 1.0f) }
            return
        }

        val tokens = query.lowercase().split(" ", "_", "-", "/").filter { it.isNotBlank() }
        val scored = files.map { file ->
            var score = 0.0f
            val searchableText = "${file.filename} ${file.path} ${file.semanticDescription} ${file.tags.joinToString(" ")} ${file.contextAffinity} ${file.cluster}".lowercase()
            for (token in tokens) {
                if (searchableText.contains(token)) {
                    score += 0.35f
                }
            }
            // Add semantic pseudo-cosine similarity
            val pseudoCosine = 0.35f + (abs((file.filename + query).hashCode() % 55) / 100.0f)
            val finalScore = (score + pseudoCosine).coerceIn(0.12f, 0.99f)
            SemanticSearchResult(file, finalScore)
        }.sortedByDescending { it.similarityScore }

        _searchResults.value = scored
    }

    fun triggerSynapseReindex() {
        viewModelScope.launch {
            _synapseFsTelemetry.update { it.copy(isReindexing = true, reindexingProgress = 0.1f) }
            _synapseFiles.update { list ->
                list.map { it.copy(indexingStatus = IndexingStatus.OPTIMIZING) }
            }
            delay(300)
            _synapseFsTelemetry.update { it.copy(reindexingProgress = 0.45f) }
            delay(350)
            _synapseFsTelemetry.update { it.copy(reindexingProgress = 0.8f) }
            delay(300)
            _synapseFiles.update { list ->
                list.map {
                    it.copy(
                        indexingStatus = IndexingStatus.INDEXED,
                        lastRetrievedTimestamp = "Just indexed"
                    )
                }
            }
            _synapseFsTelemetry.update {
                it.copy(
                    isReindexing = false,
                    reindexingProgress = 1.0f,
                    cacheHitRatio = 98.6f,
                    avgRetrievalLatencyMs = 1.1f,
                    hnswGraphNodes = it.hnswGraphNodes + 120
                )
            }
            performSemanticSearch(_searchQuery.value)
        }
    }

    fun simulateSynapseRetrieval(fileId: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeNow = sdf.format(Date())
        _synapseFiles.update { list ->
            list.map { f ->
                if (f.id == fileId) {
                    val newCount = f.accessCount + 1
                    val newHeat = (f.recentRetrievalFrequency + 0.18f).coerceAtMost(1.0f)
                    f.copy(
                        accessCount = newCount,
                        recentRetrievalFrequency = newHeat,
                        lastRetrievedTimestamp = timeNow
                    )
                } else {
                    f.copy(recentRetrievalFrequency = (f.recentRetrievalFrequency * 0.98f).coerceAtLeast(0.1f))
                }
            }
        }
        _synapseFsTelemetry.update { prev ->
            prev.copy(
                totalRetrievalsHandled = prev.totalRetrievalsHandled + 1,
                avgRetrievalLatencyMs = (Random.nextFloat() * 0.4f + 0.9f).coerceIn(0.8f, 2.5f)
            )
        }
        performSemanticSearch(_searchQuery.value)
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

    // --- Neural Shield Protocols & Privacy Operations ---
    fun toggleProtocol(id: String) {
        _protocols.update { list ->
            list.map {
                if (it.id == id) {
                    val nextEnabled = !it.isEnabled
                    it.copy(
                        isEnabled = nextEnabled,
                        status = if (nextEnabled) ProtocolStatus.ENFORCING else ProtocolStatus.BYPASSED
                    )
                } else it
            }
        }
        recalculateShieldSummary()
    }

    fun cycleProtocolStatus(id: String) {
        _protocols.update { list ->
            list.map {
                if (it.id == id) {
                    val nextStatus = when (it.status) {
                        ProtocolStatus.MONITORING -> ProtocolStatus.ENFORCING
                        ProtocolStatus.ENFORCING -> ProtocolStatus.HARDENED
                        ProtocolStatus.HARDENED -> ProtocolStatus.BYPASSED
                        ProtocolStatus.BYPASSED -> ProtocolStatus.ENFORCING
                    }
                    it.copy(status = nextStatus, isEnabled = nextStatus != ProtocolStatus.BYPASSED)
                } else it
            }
        }
        recalculateShieldSummary()
    }

    fun triggerDeepPrivacyAudit() {
        viewModelScope.launch {
            _shieldSummary.update { it.copy(isScanning = true, scanProgress = 0.15f) }
            delay(400)
            _shieldSummary.update { it.copy(scanProgress = 0.5f) }
            delay(500)
            _shieldSummary.update { it.copy(scanProgress = 0.85f) }
            delay(400)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            _shieldSummary.update {
                it.copy(
                    isScanning = false,
                    scanProgress = 1f,
                    overallIntegrityScore = 99.8f,
                    privacyScore = 99.9f,
                    lastAuditTimestamp = sdf.format(Date())
                )
            }
        }
    }

    fun quarantineAlertSource(alertId: String) {
        val alert = _privacyAlerts.value.find { it.id == alertId }
        _privacyAlerts.update { list ->
            list.map { if (it.id == alertId) it.copy(isQuarantined = true, actionTaken = "SOURCE QUARANTINED") else it }
        }
        if (alert != null) {
            _capsules.update { list ->
                list.map {
                    if (it.name.contains(alert.sourceEntity, ignoreCase = true) || alert.sourceEntity.contains(it.name, ignoreCase = true)) {
                        it.copy(status = CapsuleStatus.PAUSED)
                    } else it
                }
            }
        }
        _shieldSummary.update { it.copy(totalThreatsBlocked = it.totalThreatsBlocked + 1) }
    }

    fun acknowledgeAlert(alertId: String) {
        _privacyAlerts.update { list ->
            list.map { if (it.id == alertId) it.copy(isAcknowledged = true) else it }
        }
    }

    fun clearPrivacyAlerts() {
        _privacyAlerts.value = emptyList()
    }

    fun simulatePrivacyAlert(category: PrivacyCategory = PrivacyCategory.SENSOR_ACCESS) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val alertId = UUID.randomUUID().toString().take(8)

        val alert = when (category) {
            PrivacyCategory.SENSOR_ACCESS -> PrivacyAlert(
                id = alertId,
                timestamp = time,
                title = "Hardware Sensor Access Intercepted",
                sourceEntity = "untrusted_audio_capsule",
                category = PrivacyCategory.SENSOR_ACCESS,
                severity = ThreatSeverity.HIGH,
                actionTaken = "PROBE BLOCKED",
                details = "Microphone PCM buffer stream read attempted without valid 'capsule.sensor:audio' capability token.",
                tokenFingerprint = "SHA256:4f8a...9c12 (EXPIRED)",
                remediationHint = "Assign explicit user consent token or execute in dummy audio mode."
            )
            PrivacyCategory.DATA_LEAK_PREVENTION -> PrivacyAlert(
                id = alertId,
                timestamp = time,
                title = "SynapseFS Embedding Query Scrubbed",
                sourceEntity = "synapse_vector_indexer",
                category = PrivacyCategory.DATA_LEAK_PREVENTION,
                severity = ThreatSeverity.WARNING,
                actionTaken = "PII MASKED",
                details = "User context vector search query contained 2 sensitive identity tokens (SSN/Phone). Differential privacy noise injected.",
                tokenFingerprint = "DP-EPSILON: 0.5 (LAPLACE)",
                remediationHint = "Semantic vector sanitization protocol successfully anonymized query before embedding calculation."
            )
            PrivacyCategory.CRYPTO_KEY_LEAK -> PrivacyAlert(
                id = alertId,
                timestamp = time,
                title = "IPC Capability Token Forgery Attempt",
                sourceEntity = "guest_worker_wasi",
                category = PrivacyCategory.CRYPTO_KEY_LEAK,
                severity = ThreatSeverity.CRITICAL,
                actionTaken = "TOKEN REVOKED",
                details = "Forged Ed25519 signature detected during zero-trust inter-capsule IPC handshake with D-Kernel.",
                tokenFingerprint = "SIG_ERR: BAD_ED25519_KEY",
                remediationHint = "Attacker was isolated to sandbox linear memory without kernel privilege escalation."
            )
            PrivacyCategory.DIFFERENTIAL_PRIVACY -> PrivacyAlert(
                id = alertId,
                timestamp = time,
                title = "Telemetry Jitter Differential Guard",
                sourceEntity = "metrics_exporter",
                category = PrivacyCategory.DIFFERENTIAL_PRIVACY,
                severity = ThreatSeverity.INFO,
                actionTaken = "NOISE ADDED",
                details = "Process scheduling jitter metrics anonymized with randomized Gaussian perturbation to prevent side-channel timing analysis.",
                tokenFingerprint = "DP-GAUSSIAN: σ=1.2",
                remediationHint = "Timing side-channel mitigation active."
            )
            PrivacyCategory.UNAUTHORIZED_EGRESS -> PrivacyAlert(
                id = alertId,
                timestamp = time,
                title = "Outbound Subnet Connection Terminated",
                sourceEntity = "third_party_addon",
                category = PrivacyCategory.UNAUTHORIZED_EGRESS,
                severity = ThreatSeverity.HIGH,
                actionTaken = "SOCKET CLOSED",
                details = "Raw TCP egress attempted to 198.51.100.44:8080 without outbound net proxy token.",
                tokenFingerprint = "DENY_EGRESS_RULE#14",
                remediationHint = "WASI socket shim dropped packets immediately."
            )
            PrivacyCategory.ALL -> PrivacyAlert(
                id = alertId,
                timestamp = time,
                title = "Cross-Capsule Shared Memory Access Denied",
                sourceEntity = "rogue_wasi_probe",
                category = PrivacyCategory.DATA_LEAK_PREVENTION,
                severity = ThreatSeverity.HIGH,
                actionTaken = "SEGFAULT TRIGGERED",
                details = "Attempted out-of-bounds read into adjacent capsule's memory space.",
                tokenFingerprint = "SFI_CANARY_TRIP",
                remediationHint = "Software Fault Isolation guarded address boundaries."
            )
        }

        _privacyAlerts.update { (listOf(alert) + it).take(25) }
        _threatGauge.update { (it + 25f).coerceAtMost(95f) }
        _shieldSummary.update { it.copy(totalThreatsBlocked = it.totalThreatsBlocked + 1) }
    }

    private fun recalculateShieldSummary() {
        val total = _protocols.value.size
        val active = _protocols.value.count { it.isEnabled }
        val score = if (total > 0) (active.toFloat() / total) * 99.4f else 50f
        _shieldSummary.update {
            it.copy(
                activeProtocolsCount = active,
                totalProtocolsCount = total,
                overallIntegrityScore = score
            )
        }
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
            assignedCoreId = 0,
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
            assignedCoreId = 0,
            aiWorkloadWeight = 0.35f,
            cpuBurstHistory = listOf(16, 17, 16, 15, 16),
            cpuTimeUsedMs = 890,
            predictedBurstMs = 16,
            dynamicScore = 0.94f,
            tensorFlopsEst = "0.8 TFLOPS"
        ),
        Process(
            pid = 56,
            name = "gemini_kv_cache_prefill",
            type = ProcessType.AI_INFERENCE,
            priority = 9,
            state = ProcessState.READY,
            assignedCoreId = 1, // Neural / NPU Core
            aiWorkloadWeight = 0.95f,
            cpuBurstHistory = listOf(26, 28, 24, 30, 28),
            cpuTimeUsedMs = 2140,
            predictedBurstMs = 27,
            dynamicScore = 0.97f,
            tensorFlopsEst = "6.4 TFLOPS"
        ),
        Process(
            pid = 72,
            name = "faiss_dense_indexer",
            type = ProcessType.AI_INFERENCE,
            priority = 8,
            state = ProcessState.READY,
            assignedCoreId = 1, // Neural / NPU Core
            aiWorkloadWeight = 0.88f,
            cpuBurstHistory = listOf(32, 34, 30, 36, 33),
            cpuTimeUsedMs = 1850,
            predictedBurstMs = 33,
            dynamicScore = 0.92f,
            tensorFlopsEst = "4.8 TFLOPS"
        ),
        Process(
            pid = 82,
            name = "synapse_vector_indexer",
            type = ProcessType.BATCH,
            priority = 5,
            state = ProcessState.READY,
            assignedCoreId = 2,
            aiWorkloadWeight = 0.50f,
            cpuBurstHistory = listOf(45, 52, 48, 60, 50),
            cpuTimeUsedMs = 3120,
            predictedBurstMs = 51,
            dynamicScore = 0.62f,
            tensorFlopsEst = "1.4 TFLOPS"
        ),
        Process(
            pid = 104,
            name = "neural_shield_sentinel",
            type = ProcessType.DAEMON,
            priority = 8,
            state = ProcessState.READY,
            assignedCoreId = 0,
            aiWorkloadWeight = 0.65f,
            cpuBurstHistory = listOf(12, 11, 14, 13, 12),
            cpuTimeUsedMs = 540,
            predictedBurstMs = 12,
            dynamicScore = 0.88f,
            tensorFlopsEst = "2.1 TFLOPS"
        ),
        Process(
            pid = 142,
            name = "dam_memory_telemetry",
            type = ProcessType.DAEMON,
            priority = 7,
            state = ProcessState.READY,
            assignedCoreId = 3,
            cpuBurstHistory = listOf(10, 9, 10, 11, 10),
            cpuTimeUsedMs = 380,
            predictedBurstMs = 10,
            dynamicScore = 0.82f
        )
    )

    private fun initialCpuCores(): List<CpuCoreState> = listOf(
        CpuCoreState(
            coreId = 0,
            name = "Core 0 · Cortex-X4",
            type = "Performance",
            loadPercent = 64.2f,
            tempCelsius = 48.5f,
            frequencyGhz = 3.30f,
            activeProcessName = "liquid_ui_compositor",
            isTensorCore = false
        ),
        CpuCoreState(
            coreId = 1,
            name = "Core 1 · Neural Engine NPU",
            type = "AI Tensor Matrix",
            loadPercent = 88.4f,
            tempCelsius = 54.1f,
            frequencyGhz = 1.95f,
            activeProcessName = "gemini_kv_cache_prefill",
            isTensorCore = true
        ),
        CpuCoreState(
            coreId = 2,
            name = "Core 2 · Cortex-A720",
            type = "Balanced Compute",
            loadPercent = 44.8f,
            tempCelsius = 43.2f,
            frequencyGhz = 2.60f,
            activeProcessName = "synapse_vector_indexer",
            isTensorCore = false
        ),
        CpuCoreState(
            coreId = 3,
            name = "Core 3 · Cortex-A520",
            type = "High Efficiency",
            loadPercent = 29.5f,
            tempCelsius = 39.8f,
            frequencyGhz = 1.90f,
            activeProcessName = "dam_memory_telemetry",
            isTensorCore = false
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
            cluster = "Microkernel IPC & Core",
            tags = listOf("kernel", "microkernel", "rust", "sel4"),
            semanticDescription = "D-Kernel core microkernel entry point with capability token verification and IPC endpoints.",
            accessCount = 189,
            recentRetrievalFrequency = 0.78f,
            retrievalLatencyMs = 1.1f,
            cacheHitRate = 98.2f,
            tokenCount = 1840,
            prefetchAffinity = "Prefetched with sel4_capability_manifest.cap",
            contextAffinity = "Boot, Core Execution",
            lastRetrievedTimestamp = "19:44:12"
        ),
        SynapseFile(
            id = "f2",
            path = "/sys/sched/",
            filename = "adaptive_weights.bin",
            sizeKb = 64,
            category = FileCategory.NEURAL_WEIGHTS,
            cluster = "ML Burst Prediction",
            tags = listOf("scheduler", "ml", "weights", "burst"),
            semanticDescription = "Decision-tree ML weights for process classification and dynamic burst prediction.",
            accessCount = 412,
            recentRetrievalFrequency = 0.94f,
            retrievalLatencyMs = 0.9f,
            cacheHitRate = 99.4f,
            tokenCount = 512,
            prefetchAffinity = "Real-time D-Kernel scheduler quantum loop",
            contextAffinity = "Scheduler Telemetry",
            lastRetrievedTimestamp = "19:45:01"
        ),
        SynapseFile(
            id = "f3",
            path = "/capsules/liquid_ui/",
            filename = "app_shell.wasm",
            sizeKb = 820,
            category = FileCategory.WASI_BYTECODE,
            cluster = "Sandboxed UI Compositor",
            tags = listOf("wasi", "ui", "liquid", "impeller"),
            semanticDescription = "Liquid UI context-aware layout engine compiled to WebAssembly WASI micro-container.",
            accessCount = 245,
            recentRetrievalFrequency = 0.72f,
            retrievalLatencyMs = 1.8f,
            cacheHitRate = 95.1f,
            tokenCount = 3450,
            prefetchAffinity = "Prefetched with liquid_impeller_shaders.spv",
            contextAffinity = "Display Compositor",
            lastRetrievedTimestamp = "19:43:28"
        ),
        SynapseFile(
            id = "f4",
            path = "/sec/neural_shield/",
            filename = "anomaly_rules.json",
            sizeKb = 32,
            category = FileCategory.SECURITY_POLICY,
            cluster = "Neural Sentry Zero-Trust",
            tags = listOf("security", "ebpf", "rules", "anomaly"),
            semanticDescription = "Heuristic behavioral baseline rules for syscall anomaly scoring and zero-trust validation.",
            accessCount = 198,
            recentRetrievalFrequency = 0.85f,
            retrievalLatencyMs = 1.2f,
            cacheHitRate = 97.8f,
            tokenCount = 680,
            prefetchAffinity = "Prefetched with ebpf_syscall_filter.o",
            contextAffinity = "Security Sentry",
            lastRetrievedTimestamp = "19:44:50"
        ),
        SynapseFile(
            id = "f5",
            path = "/hut/",
            filename = "x86_to_arm64_jit.tbl",
            sizeKb = 512,
            category = FileCategory.KERNEL_CORE,
            cluster = "HUT Binary Translation",
            tags = listOf("hut", "translator", "jit", "rosetta"),
            semanticDescription = "Universal Hardware Translator jump table for cross-architecture x86_64 to ARM64 execution.",
            accessCount = 87,
            recentRetrievalFrequency = 0.42f,
            retrievalLatencyMs = 2.3f,
            cacheHitRate = 91.0f,
            tokenCount = 2100,
            prefetchAffinity = "Invoked upon foreign binary loader invocation",
            contextAffinity = "Binary Translation",
            lastRetrievedTimestamp = "19:30:15"
        ),
        SynapseFile(
            id = "f6",
            path = "/synapse/embeddings/",
            filename = "contextual_user.vec",
            sizeKb = 1240,
            category = FileCategory.NEURAL_WEIGHTS,
            cluster = "Semantic Intent Index",
            tags = listOf("synapse", "vector", "faiss", "semantic"),
            semanticDescription = "Dense semantic vector index mapping file contexts, temporal affinities, and user intent.",
            accessCount = 380,
            recentRetrievalFrequency = 0.91f,
            retrievalLatencyMs = 1.5f,
            cacheHitRate = 96.8f,
            tokenCount = 4800,
            prefetchAffinity = "Continuous background cosine similarity pipeline",
            contextAffinity = "Filesystem Search",
            lastRetrievedTimestamp = "19:44:58"
        ),
        SynapseFile(
            id = "f7",
            path = "/sec/ebpf/",
            filename = "ebpf_syscall_filter.o",
            sizeKb = 96,
            category = FileCategory.SECURITY_POLICY,
            cluster = "Kernel eBPF Probes",
            tags = listOf("ebpf", "kernel", "sandbox", "audit"),
            semanticDescription = "Bytecode filter attached to syscall dispatch table for runtime taint tracking and IPC isolation.",
            accessCount = 220,
            recentRetrievalFrequency = 0.82f,
            retrievalLatencyMs = 1.0f,
            cacheHitRate = 98.6f,
            tokenCount = 920,
            prefetchAffinity = "Loaded on microkernel boot and capsule spawn",
            contextAffinity = "Syscall Interception",
            lastRetrievedTimestamp = "19:42:00"
        ),
        SynapseFile(
            id = "f8",
            path = "/ai/models/",
            filename = "gemini_kv_cache.bin",
            sizeKb = 2048,
            category = FileCategory.NEURAL_WEIGHTS,
            cluster = "LLM Attention KV-Cache",
            tags = listOf("gemini", "npu", "kv_cache", "attention"),
            semanticDescription = "On-device quantized key-value attention cache for real-time prompt completion and intent prediction.",
            accessCount = 530,
            recentRetrievalFrequency = 0.98f,
            retrievalLatencyMs = 0.8f,
            cacheHitRate = 99.8f,
            tokenCount = 8192,
            prefetchAffinity = "Prefetched prior to transformer token decode cycle",
            contextAffinity = "Neural Engine NPU Core 1",
            lastRetrievedTimestamp = "19:45:03"
        ),
        SynapseFile(
            id = "f9",
            path = "/dam/telemetry/",
            filename = "dam_eviction_telemetry.log",
            sizeKb = 380,
            category = FileCategory.TELEMETRY_LOGS,
            cluster = "DAM Memory Logs",
            tags = listOf("dam", "memory", "wss", "eviction"),
            semanticDescription = "Cyclic ring buffer tracking working set size shifts, zRAM compaction ratios, and cold page reclaims.",
            accessCount = 110,
            recentRetrievalFrequency = 0.55f,
            retrievalLatencyMs = 2.1f,
            cacheHitRate = 93.4f,
            tokenCount = 1420,
            prefetchAffinity = "Streaming append by DAM kernel thread",
            contextAffinity = "Memory Telemetry",
            lastRetrievedTimestamp = "19:38:40"
        ),
        SynapseFile(
            id = "f10",
            path = "/capsules/shaders/",
            filename = "liquid_impeller_shaders.spv",
            sizeKb = 430,
            category = FileCategory.WASI_BYTECODE,
            cluster = "Vulkan Shader Cache",
            tags = listOf("vulkan", "impeller", "gpu", "spirv"),
            semanticDescription = "Precompiled SPIR-V rasterization and blur kernels for hardware-accelerated UI rendering.",
            accessCount = 165,
            recentRetrievalFrequency = 0.68f,
            retrievalLatencyMs = 1.6f,
            cacheHitRate = 96.0f,
            tokenCount = 1800,
            prefetchAffinity = "Prefetched during Liquid UI surface initialization",
            contextAffinity = "GPU Pipeline",
            lastRetrievedTimestamp = "19:41:10"
        ),
        SynapseFile(
            id = "f11",
            path = "/net/entangle/",
            filename = "entangle_mesh_state.cbor",
            sizeKb = 175,
            category = FileCategory.USER_DATA,
            cluster = "P2P Device Graph",
            tags = listOf("p2p", "cbor", "mesh", "nodes"),
            semanticDescription = "Cryptographic state database of nearby distributed compute nodes and remote capsule replicas.",
            accessCount = 92,
            recentRetrievalFrequency = 0.48f,
            retrievalLatencyMs = 2.5f,
            cacheHitRate = 92.1f,
            tokenCount = 890,
            prefetchAffinity = "Synced over mDNS / BLE discovery",
            contextAffinity = "Entanglement Mesh",
            lastRetrievedTimestamp = "19:35:50"
        ),
        SynapseFile(
            id = "f12",
            path = "/kernel/caps/",
            filename = "sel4_capability_manifest.cap",
            sizeKb = 48,
            category = FileCategory.KERNEL_CORE,
            cluster = "IPC Capability Tokens",
            tags = listOf("sel4", "capability", "tokens", "security"),
            semanticDescription = "Hierarchical cryptographic capability token tree governing access to all hardware and VFS resources.",
            accessCount = 340,
            recentRetrievalFrequency = 0.89f,
            retrievalLatencyMs = 0.9f,
            cacheHitRate = 99.1f,
            tokenCount = 420,
            prefetchAffinity = "Prefetched with every process creation and IPC call",
            contextAffinity = "Access Control List",
            lastRetrievedTimestamp = "19:44:59"
        ),
        SynapseFile(
            id = "f13",
            path = "/var/log/audit/",
            filename = "audit_trail_archive.zst",
            sizeKb = 910,
            category = FileCategory.TELEMETRY_LOGS,
            cluster = "Zero-Trust Event Stream",
            tags = listOf("audit", "zstd", "archive", "cold"),
            semanticDescription = "Zstandard compressed historical telemetry of intercepted syscall anomalies and quarantine actions.",
            accessCount = 45,
            recentRetrievalFrequency = 0.22f,
            retrievalLatencyMs = 3.8f,
            cacheHitRate = 88.5f,
            tokenCount = 3900,
            prefetchAffinity = "Cold storage page; accessed only on compliance audit",
            contextAffinity = "Cold Archive",
            lastRetrievedTimestamp = "18:12:00"
        ),
        SynapseFile(
            id = "f14",
            path = "/home/semantic/",
            filename = "user_semantic_workspace.db",
            sizeKb = 680,
            category = FileCategory.USER_DATA,
            cluster = "Local Intent Embedding Store",
            tags = listOf("user", "workspace", "sqlite", "vector"),
            semanticDescription = "Local encrypted relational metadata and vector embeddings linking personal notes, code, and prompts.",
            accessCount = 190,
            recentRetrievalFrequency = 0.74f,
            retrievalLatencyMs = 1.7f,
            cacheHitRate = 95.8f,
            tokenCount = 2750,
            prefetchAffinity = "Prefetched upon user workspace interaction",
            contextAffinity = "User Space",
            lastRetrievedTimestamp = "19:43:55"
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

    private fun initialSecurityProtocols(): List<SecurityProtocol> = listOf(
        SecurityProtocol(
            id = "proto_zt_ipc",
            code = "ZT-IPC",
            name = "Zero-Trust Capability IPC",
            description = "Cryptographic capability token verification on every inter-process and inter-capsule IPC endpoint. Eliminates ambient authority.",
            category = ProtocolCategory.ZERO_TRUST,
            status = ProtocolStatus.HARDENED,
            isEnabled = true,
            inspectedEventsCount = 284190,
            violationsBlockedCount = 18,
            latencyImpactUs = 3.8f,
            enforcedRulesCount = 42
        ),
        SecurityProtocol(
            id = "proto_ebpf_fw",
            code = "eBPF-FW",
            name = "eBPF Syscall Filter & Ring Buffer",
            description = "In-kernel bytecode filter scoring syscall frequencies and parameters against learned heuristic baselines in microsecond timescales.",
            category = ProtocolCategory.KERNEL_INTEGRITY,
            status = ProtocolStatus.ENFORCING,
            isEnabled = true,
            inspectedEventsCount = 612840,
            violationsBlockedCount = 27,
            latencyImpactUs = 4.2f,
            enforcedRulesCount = 64
        ),
        SecurityProtocol(
            id = "proto_wx_mem",
            code = "W^X-MEM",
            name = "W^X Memory Invariant Enforcement",
            description = "Strict hardware NX bit enforcement preventing writable pages from being executed in WASI guest sandboxes and kernel space.",
            category = ProtocolCategory.MEMORY_HARDENING,
            status = ProtocolStatus.HARDENED,
            isEnabled = true,
            inspectedEventsCount = 98120,
            violationsBlockedCount = 9,
            latencyImpactUs = 1.1f,
            enforcedRulesCount = 16
        ),
        SecurityProtocol(
            id = "proto_dp_mask",
            code = "DP-MASK",
            name = "Differential Privacy & Semantic Masking",
            description = "Scrubbing of PII and injection of calibrated Laplace/Gaussian noise into SynapseFS vector search queries and telemetry streams.",
            category = ProtocolCategory.DATA_PRIVACY,
            status = ProtocolStatus.ENFORCING,
            isEnabled = true,
            inspectedEventsCount = 15200,
            violationsBlockedCount = 12,
            latencyImpactUs = 6.4f,
            enforcedRulesCount = 24
        ),
        SecurityProtocol(
            id = "proto_entropy_k",
            code = "ENTROPY-K",
            name = "Dynamic Hardware Entropy & Key Rotation",
            description = "Automatic cryptographic re-keying and ephemeral seed rotation using physical TRNG entropy pool every 300 seconds.",
            category = ProtocolCategory.ZERO_TRUST,
            status = ProtocolStatus.ENFORCING,
            isEnabled = true,
            inspectedEventsCount = 4800,
            violationsBlockedCount = 0,
            latencyImpactUs = 0.5f,
            enforcedRulesCount = 8
        ),
        SecurityProtocol(
            id = "proto_net_isol",
            code = "NET-ISOL",
            name = "Subnet Isolation & Socket Policy",
            description = "Zero-privilege socket sandboxing preventing raw TCP/UDP outbound binds without mediated cryptographic proxies.",
            category = ProtocolCategory.NETWORK_ISOLATION,
            status = ProtocolStatus.ENFORCING,
            isEnabled = true,
            inspectedEventsCount = 74300,
            violationsBlockedCount = 5,
            latencyImpactUs = 2.9f,
            enforcedRulesCount = 30
        )
    )

    private fun initialPrivacyAlerts(): List<PrivacyAlert> = listOf(
        PrivacyAlert(
            id = "pa_1",
            timestamp = "19:18:22",
            title = "Hardware Audio Sensor Intercepted",
            sourceEntity = "untrusted_audio_capsule",
            category = PrivacyCategory.SENSOR_ACCESS,
            severity = ThreatSeverity.HIGH,
            actionTaken = "PROBE BLOCKED",
            details = "Attempted low-level microphone stream ingestion without capability token 'capsule.sensor:audio'.",
            tokenFingerprint = "SHA256:d8a2...3f11 (UNVERIFIED)",
            remediationHint = "Host rejected ambient audio request. Capsule confined to synthetic loopback device."
        ),
        PrivacyAlert(
            id = "pa_2",
            timestamp = "19:12:05",
            title = "PII Sanitized in SynapseFS Query",
            sourceEntity = "synapse_vector_indexer",
            category = PrivacyCategory.DATA_LEAK_PREVENTION,
            severity = ThreatSeverity.INFO,
            actionTaken = "2 TOKENS MASKED",
            details = "Semantic search query contained personal identity tokens. Scrubbed prior to dense vector embedding.",
            tokenFingerprint = "DP-EPSILON: 0.5 (LAPLACE)",
            remediationHint = "Differential privacy protocol prevented index leakage."
        ),
        PrivacyAlert(
            id = "pa_3",
            timestamp = "19:04:41",
            title = "Cross-Capsule Memory Canary Triggered",
            sourceEntity = "rogue_wasi_probe",
            category = PrivacyCategory.DATA_LEAK_PREVENTION,
            severity = ThreatSeverity.HIGH,
            actionTaken = "ISOLATED",
            details = "Attempted out-of-bounds read into adjacent capsule's memory region.",
            tokenFingerprint = "SFI_CANARY_TRIP#4096",
            remediationHint = "Software Fault Isolation boundary caught and prevented memory leak."
        ),
        PrivacyAlert(
            id = "pa_4",
            timestamp = "18:58:19",
            title = "Telemetry Jitter Randomized",
            sourceEntity = "dam_memory_telemetry",
            category = PrivacyCategory.DIFFERENTIAL_PRIVACY,
            severity = ThreatSeverity.INFO,
            actionTaken = "GAUSSIAN NOISE INJECTED",
            details = "Memory working set delta anonymized to thwart side-channel timing analysis.",
            tokenFingerprint = "DP-GAUSSIAN: σ=1.2",
            remediationHint = "Anti-side-channel telemetry obfuscation operational."
        )
    )
}
