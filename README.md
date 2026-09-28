# Dilauro AI-OS for Android 🧠

> **An adaptive, AI-native operating system dashboard and microkernel orchestrator built with Jetpack Compose & Kotlin.**

---

## Overview

**Dilauro AI-OS** has been rewritten as a native Android application, bringing the full vision of an adaptive, machine-learning-driven operating system to modern mobile and embedded devices.

The application implements the core architectural pillars defined in the original Dilauro AI-OS specifications and RFCs:

1. **D-Kernel Adaptive Scheduler (RFC-0001)**:
   - ML-augmented process scheduling with historical CPU burst ring buffers.
   - Dynamic classification of interactive vs. batch vs. daemon processes.
   - Latency-sensitive preemption and priority scoring.
   - Interactive process management (spawning, priority boosting, and task termination).

2. **DAM-Engine (Dynamic Adaptive Memory)**:
   - Real-time RAM breakdown: Working Set Size (WSS), Cold Pages, and compressed memory.
   - Predictive pre-eviction of inactive pages before memory pressure triggers critical OOM events.
   - Interactive memory pressure injection (Normal, Moderate, Critical) and zRAM page compaction.

3. **SynapseFS (Semantic Vector Filesystem)**:
   - Context-aware filesystem with rich metadata, semantic tags, and contextual affinities.
   - Vector similarity search ranked by cosine similarity.
   - Real-time file creation, tag indexing, and file inspection.

4. **Capsules (WASI Micro-Container Runtime)**:
   - WebAssembly micro-container management with zero ambient authority.
   - Capability-based security token model (`capsule.fs:ro`, `capsule.net:libp2p`, `capsule.sys:telemetry`).
   - Live hot-swap updates and Entanglement cross-device migration.

5. **Neural Shield (Heuristic ML Security)**:
   - Real-time eBPF syscall anomaly detection and behavioral baseline scoring.
   - Anomaly probability gauge with zero-trust validation.
   - Threat simulation benchmark (Syscall flood, memory boundary breach, token spoofing) with immediate automated quarantine.

6. **HUT & Entanglement Protocol**:
   - Hardware Universal Translator pipeline (x86_64 to ARM64 JIT dynamic recompilation stats).
   - Distributed multi-device compute mesh status and device hand-off state.

---

## Tech Stack & Architecture

- **Language:** Kotlin 2.1
- **UI Framework:** Jetpack Compose + Material Design 3 (Liquid UI Cyberpunk / AI-Native theme)
- **Architecture:** Clean Architecture + MVVM with Kotlin Coroutines and StateFlow
- **Build System:** Gradle 9.3.1 + Android Gradle Plugin (AGP) 9.1.1
- **Target SDK:** Android 36 (Android 16 / Vanilla Ice Cream)
- **Min SDK:** Android 26 (Android 8.0 Oreo)

---

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/dilauro_aios/
│   │   ├── MainActivity.kt               # Navigation host & Liquid UI app shell
│   │   ├── model/                        # Domain models (Process, Memory, Synapse, Capsule, Security)
│   │   ├── viewmodel/                    # AiosViewModel (Kernel heartbeat, ML scheduler, DAM, SynapseFS)
│   │   └── ui/
│   │       ├── theme/                    # Color, Typography, and M3 Theme
│   │       ├── components/               # CyberCard, MetricBadge, BurstRingBufferGraph
│   │       └── screens/                  # Dashboard, Scheduler, Memory, SynapseFS, Capsules, NeuralShield, Entanglement
│   └── res/                              # Adaptive icons, hero banners, strings, and layouts
└── build.gradle.kts
```

---

## License

MIT License — see [LICENSE](LICENSE) for details.
