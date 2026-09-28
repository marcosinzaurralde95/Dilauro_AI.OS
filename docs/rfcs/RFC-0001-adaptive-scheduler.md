# RFC-0001: Adaptive Scheduler Design

**Status:** Draft  
**Author:** Marcos Sinzaurralde  
**Created:** 2024  

---

## Summary

Design the ML-driven process scheduler (core of the D-Kernel) that learns CPU usage patterns and dynamically adjusts task priorities without human intervention.

## Motivation

Traditional schedulers (CFS, round-robin) treat all processes equally and rely on static priority hints from the user. Dilauro's scheduler should:
- Learn which processes are latency-sensitive vs. throughput-bound
- Predict upcoming CPU bursts from historical patterns
- Reduce scheduling overhead by batching compatible tasks

## Proposed Design

### Phase 1 — Round-Robin Baseline
Implement a standard preemptive round-robin scheduler with fixed time slices. This gives us a working foundation and telemetry collection point.

### Phase 2 — Telemetry Collection
Attach a ring buffer to each process recording:
- CPU burst lengths
- Wait times
- Context switch frequency
- Memory access patterns

### Phase 3 — ML Augmentation
Train a small decision-tree or linear model (no_std compatible) on collected telemetry to:
- Classify processes as interactive / batch / daemon
- Adjust time slice lengths per classification
- Pre-empt batch jobs when an interactive process wakes

## Open Questions
- What ML library can run in `no_std` with minimal binary size?
- Should model weights be static (compiled-in) or updatable at runtime?
- How do we evaluate scheduler quality without a full benchmark suite?

## References
- [ghOSt: Multi-Agent Scheduling for the Linux Kernel](https://research.google/pubs/pub50745/)
- [BPF-based Linux scheduler patches](https://lwn.net/Articles/922405/)
