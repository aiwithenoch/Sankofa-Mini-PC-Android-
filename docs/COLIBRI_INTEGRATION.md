# Colibri Integration Research Plan

## Status

**Decision: evaluate before adopting.**

Sankofa is testing whether [Colibri](https://github.com/JustVugg/colibri) can become an optional native runtime for very large Mixture-of-Experts models on Android.

Colibri is not currently bundled, forked or declared production-ready for Android. The first goal is to establish basic compilation and correctness in Termux.

## Why it matters

The main problem with very large MoE models is not only total weight size. The runtime must decide what stays resident, what remains on storage, which experts are loaded for each token, how reads overlap computation, and how hot experts are cached.

Colibri already implements this class of storage-streamed execution for supported model architectures. Sankofa should reuse or collaborate on proven runtime work rather than rebuild the same mechanisms without evidence.

## Product boundary

Colibri and Sankofa solve different layers:

| Layer | Responsibility |
|---|---|
| Colibri candidate runtime | Native model execution, tensor loading, expert placement, cache and generation |
| Sankofa runtime adapter | Start, stop, health, cancellation, logs and event conversion |
| Sankofa model manager | Downloads, manifests, checksums, storage reservation and activation |
| Sankofa device manager | RAM, storage, CPU and thermal budgets |
| Sankofa API and dashboard | Local user experience and OpenAI-compatible access |
| Sankofa agent runtime | Memory, tasks, tools, permissions and approvals |

## What can potentially be reused

- ARM64 and NEON compute paths
- Storage-addressable expert layout
- Expert cache and placement policies
- Router-driven expert loading
- Prefetch and asynchronous read design
- Runtime telemetry concepts
- OpenAI-compatible serving patterns
- Tiny correctness fixtures and reference comparisons

Reuse is conditional on Android testing and model compatibility.

## What cannot be assumed

- That the current build system works in Termux
- That Android supports every Linux I/O path used by the runtime
- That phone UFS storage behaves like desktop NVMe
- That sustained inference is thermally safe or fast
- That GLM-specific code can execute Kimi K3
- That K3 fits within phone storage after acceptable quantization
- That a successful tiny self-test predicts frontier-model performance

## Phase 1 — compile and correctness

Run [`experiments/colibri-termux/probe.sh`](../experiments/colibri-termux/probe.sh) on a real phone.

Required evidence:

1. Phone model and Android version
2. CPU architecture and available instruction features
3. Termux and clang versions
4. OpenMP probe result
5. Complete build output
6. Tiny self-test output
7. Peak RAM during the test
8. Temperature before and after the test

Success condition:

> The native engine builds inside Termux and its bundled tiny reference test produces the expected result.

## Phase 2 — Android compatibility patch set

Only after Phase 1 evidence exists:

- isolate build-system assumptions;
- replace unsupported Linux APIs or flags;
- add Android target detection;
- add Termux-specific paths without hard-coding one device;
- preserve portable fallbacks;
- upstream generally useful changes where practical;
- add an Android build to repeatable tests.

## Phase 3 — storage and thermal measurements

Measure the phone rather than guessing:

- sequential read throughput;
- parallel random reads at expert-like block sizes;
- buffered versus direct-I/O behaviour where supported;
- filesystem and internal-versus-external storage differences;
- temperature, clock reduction and battery drain;
- cache hit-rate sensitivity to available RAM.

No public speed claim should be made without the exact command, model/container, phone, storage location, temperature and runtime configuration.

## Phase 4 — Sankofa adapter

The preferred first integration is process-based:

```text
Sankofa daemon
    ↕ structured local protocol
Colibri native process
```

This boundary provides:

- crash isolation;
- independent runtime upgrades;
- clean logging;
- cancellation and watchdog control;
- simpler licence separation;
- no model-specific C code inside the Python API daemon.

FFI can be considered later if process overhead becomes measurable and important.

## Phase 5 — Kimi K3 backend research

Colibri's streaming and placement ideas do not automatically provide K3 support. A K3 backend requires verified implementation of the official architecture, tokenizer, tensor naming, attention path, routing, expert format, cache behaviour and reference outputs.

The required order is:

1. inspect official checkpoint and licence;
2. generate exact tensor and storage reports;
3. build a high-precision reference implementation;
4. match official outputs;
5. design expert-addressable packaging;
6. test mixed precision;
7. execute one expert, one layer and then one verified token on Android.

## Licence policy

Colibri is licensed under Apache-2.0. Sankofa is licensed under MIT.

Calling an external Colibri executable does not require relicensing Sankofa, but redistributed Colibri binaries or modified source must include the applicable Apache-2.0 licence, notices and modification statements. Model licences are separate and must be reviewed independently.

No Colibri code is copied into Sankofa at this stage.

## Decision gates

Sankofa should adopt Colibri as a supported runtime only after:

- Android compilation is repeatable;
- correctness tests pass;
- required patches are maintainable;
- logs and cancellation work through the adapter;
- resource limits can prevent runaway RAM or thermal behaviour;
- licence and attribution handling is complete.

Until then, the correct label is **experimental runtime candidate**.