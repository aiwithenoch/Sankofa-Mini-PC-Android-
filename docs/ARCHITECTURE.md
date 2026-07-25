# Architecture

## Product boundary

Sankofa Mini PC is a headless local computing environment for Android. Termux supplies the Linux-like userspace. Sankofa supplies the daemon, CLI, dashboard, storage, model abstraction, and eventually agent services.

The browser dashboard is a control surface, not the core product.

## Core principles

1. **Local by default:** bind only to loopback until security is configured.
2. **Model-independent core:** the server never imports K3-specific code directly.
3. **Replaceable backends:** every model implements the same generation contract.
4. **Persistent state:** tasks and conversations survive browser closure and restarts.
5. **Measured claims:** performance statements require hardware and benchmark details.
6. **Graceful degradation:** the system must remain manageable when no model is loaded.

## Current vertical slice

```text
Web dashboard
  → FastAPI local server
  → ModelBackend interface
  → MockBackend
  → SQLite conversation memory
```

## Planned subsystem boundaries

### Core daemon

Owns process lifecycle, HTTP API, configuration, health endpoints, and event coordination.

### Model runtime

Owns tokenization, loading, generation, cancellation, memory budgets, and model-specific telemetry.

### Model manager

Owns manifests, resumable shard downloads, signatures, checksums, storage reservation, activation, rollback, and removal.

### Agent runtime

Owns the reasoning loop, context construction, tool requests, approvals, retries, and task persistence. The model proposes actions; the runtime authorizes and executes them.

### Memory

SQLite is the source of truth. FTS5 supports local keyword retrieval. Embedding indexes will be optional and rebuildable.

### Browser worker

Runs separately from the core process and accepts a strict command schema. Financial, account, destructive, and submission actions require explicit approval.

### Security

External access is disabled by default. Future network mode requires authentication, encryption, rate limiting, audit logs, and tool permissions.

## Model backend contract

The initial Python contract is intentionally small:

- `model_id`
- `generate(request)`
- `stream(request)`
- `health()`

The K3 native runtime will eventually sit behind a Python/Rust FFI adapter without changing the API or dashboard.
