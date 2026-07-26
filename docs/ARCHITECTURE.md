# Architecture

## Product boundary

Sankofa Mini PC is a headless local computing environment for Android. Termux supplies the Linux-like userspace. Sankofa supplies the daemon, CLI, dashboard, storage, model abstraction, runtime management and eventually agent services.

The browser dashboard is a control surface, not the core product.

Sankofa is not tied to one inference engine. Small and conventional models may use a GGUF/llama.cpp-style backend. Very large Mixture-of-Experts models may require a storage-streaming runtime such as Colibri. Kimi K3 will require a model-specific implementation behind the same Sankofa contract.

## Core principles

1. **Local by default:** bind only to loopback until security is configured.
2. **Model-independent core:** the server never imports K3-specific code directly.
3. **Replaceable backends:** every model implements the same generation contract.
4. **Runtime isolation:** native engines run behind a supervised adapter boundary.
5. **Persistent state:** tasks and conversations survive browser closure and restarts.
6. **Measured claims:** performance statements require hardware and benchmark details.
7. **Graceful degradation:** the system remains manageable when no model is loaded.
8. **No silent quality changes:** placement and resource policies must not secretly swap models or alter precision.

## Current vertical slice

```text
Web dashboard
  → FastAPI local server
  → ModelBackend interface
  → MockBackend
  → SQLite conversation memory
```

## Target runtime architecture

```text
Browser / local client
        │
        ▼
Sankofa API daemon
        │
        ├── memory and tasks
        ├── permissions and approvals
        ├── device / thermal manager
        ├── model manager
        └── runtime supervisor
                  │
                  ├── MockBackend
                  ├── small-model backend
                  ├── ColibriAdapter
                  │       └── native Colibri process
                  └── KimiK3Adapter
                          └── future K3-native runtime
```

The API daemon must not contain model-specific tensor code. It starts, monitors and stops a runtime; converts requests into the runtime protocol; converts runtime events into Sankofa streaming responses; and enforces resource and permission policies.

## Planned subsystem boundaries

### Core daemon

Owns process lifecycle, HTTP API, configuration, health endpoints and event coordination.

### Runtime supervisor

Owns native-process startup, shutdown, health checks, stderr/stdout capture, cancellation, crash recovery and resource limits. A native runtime cannot directly mutate Sankofa memory or execute agent tools.

### Model runtime

Owns tokenization, loading, placement, generation, cancellation, cache state, memory budgets and model-specific telemetry.

Runtime candidates:

- **MockBackend:** deterministic end-to-end testing.
- **Small-model backend:** practical local inference on ordinary Android devices.
- **ColibriAdapter:** experiment for storage-streamed massive MoE inference.
- **KimiK3Adapter:** future model-specific architecture and package implementation.

### Model manager

Owns manifests, resumable shard downloads, signatures, checksums, storage reservation, activation, rollback and removal.

Large-model downloads must be explicit. Device checks and runtime probes must never automatically download model weights.

### Device and thermal manager

Owns RAM, storage, CPU architecture, frequency and thermal telemetry. It can request generation slowdown, pause downloads or stop a runtime when configured safety limits are reached.

### Agent runtime

Owns the reasoning loop, context construction, tool requests, approvals, retries and task persistence. The model proposes actions; the runtime authorizes and executes them.

### Memory

SQLite is the source of truth. FTS5 supports local keyword retrieval. Embedding indexes are optional and rebuildable.

### Browser worker

Runs separately from the core process and accepts a strict command schema. Financial, account, destructive and submission actions require explicit approval.

### Security

External access is disabled by default. Future network mode requires authentication, encryption, rate limiting, audit logs and tool permissions.

## Model backend contract

The initial Python contract is intentionally small:

- `model_id`
- `generate(request)`
- `stream(request)`
- `health()`

Native adapters will extend the implementation behind this contract with lifecycle and telemetry methods without changing the public API.

## Colibri integration rule

Colibri is currently an external research dependency, not vendored source. The first phase is a clean-room integration through its command/process interface. If source changes become necessary, Sankofa should contribute portable Android changes upstream where practical and preserve Apache-2.0 notices for any redistributed Colibri code.

See [`COLIBRI_INTEGRATION.md`](COLIBRI_INTEGRATION.md) for the experiment and decision gates.