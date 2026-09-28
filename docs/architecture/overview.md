# Dilauro AI-OS — Architecture Overview

## Component Map

```
┌─────────────────────────────────────────────────────────────┐
│                         Liquid UI                           │
│          (adaptive, context-aware interface layer)          │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                        Capsules                             │
│          (WASI micro-container runtime for apps)            │
└──────┬────────────────────────────────────────┬─────────────┘
       │                                        │
┌──────▼──────────┐                  ┌──────────▼──────────┐
│   SynapseFS     │                  │   Neural Shield     │
│ (semantic FS)   │                  │  (ML security)      │
└──────┬──────────┘                  └──────────┬──────────┘
       │                                        │
┌──────▼────────────────────────────────────────▼──────────┐
│                       D-Kernel                            │
│   ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│   │ Adaptive    │  │  DAM-Engine  │  │      HUT       │  │
│   │ Scheduler   │  │  (memory)    │  │  (binary xlat) │  │
│   └─────────────┘  └──────────────┘  └────────────────┘  │
└──────────────────────────┬────────────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────────────┐
│              Entanglement Protocol                        │
│       (distributed computing across devices)              │
└───────────────────────────────────────────────────────────┘
```

## Design Principles

1. **AI-first, not AI-bolted-on** — ML is a first-class citizen of the kernel, not a userspace add-on
2. **Microkernel isolation** — each component runs in its own protection domain
3. **Privacy by default** — all inference runs on-device, no cloud required
4. **Capsule portability** — apps are WASM modules, runnable on any Dilauro device

## Key Interfaces

| Interface | From | To | Protocol |
|-----------|------|----|---------|
| Syscall ABI | Capsules | D-Kernel | Capability tokens |
| VFS API | Capsules | SynapseFS | POSIX-compatible |
| Threat events | Neural Shield | D-Kernel | Async event queue |
| Memory pressure | DAM-Engine | Capsules | Signal + advisory |
| Device hand-off | Entanglement | D-Kernel | libp2p message |
