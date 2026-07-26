# Roadmap

Sankofa uses proof-driven milestones. A task is complete only when the repository contains reproducible commands, logs, hardware details and expected results.

## Milestone 1 — Foundation

- [x] One-command Termux installer
- [x] Device checker
- [x] Local daemon
- [x] Browser dashboard
- [x] SQLite conversation memory
- [x] Model-backend interface
- [x] Mock local backend
- [x] OpenAI-compatible endpoint skeleton
- [x] Public phone checker on Vercel

## Milestone 2 — Android massive-model runtime proof

This is the immediate priority.

- [x] Document Colibri as a runtime candidate
- [x] Add a non-model Termux probe script
- [ ] Run the probe on a real ARM64 Android phone
- [ ] Compile Colibri with Termux clang
- [ ] Pass the bundled tiny self-test at the expected result
- [ ] Record exact phone, Android, Termux and compiler versions
- [ ] Measure internal-storage sequential and random-read performance
- [ ] Record sustained CPU temperature and frequency behaviour
- [ ] Identify required Android compatibility patches
- [ ] Publish the first reproducible Android experiment report

Success at this milestone proves only that the engine can execute correctly on Android. It does not prove that a frontier model is fast, thermally sustainable or small enough for the phone.

## Milestone 3 — Sankofa runtime adapter

- [ ] Define a native subprocess/FFI adapter contract
- [ ] Add Colibri process lifecycle management
- [ ] Convert runtime output into Sankofa streaming events
- [ ] Expose runtime health and placement telemetry
- [ ] Add generation cancellation and timeout handling
- [ ] Add RAM, storage and thermal budgets
- [ ] Keep MockBackend available for testing
- [ ] Add a smaller real-model backend for everyday phones

## Milestone 4 — Reliable server

- [ ] Streaming token responses
- [ ] API authentication
- [ ] Structured configuration file
- [ ] Automatic startup with Termux:Boot
- [ ] Watchdog and crash recovery
- [ ] Database backups and migrations
- [ ] Release checksums and signed installer

## Milestone 5 — Model manager

- [ ] Model manifests
- [ ] Storage reservation before download
- [ ] Resumable shard downloads
- [ ] Per-shard checksums
- [ ] Atomic activation and rollback
- [ ] Runtime/model compatibility checks
- [ ] Safe model removal
- [ ] Download and thermal pause/resume policies

## Milestone 6 — Agent computer

- [ ] Persistent task queue
- [ ] Tool registry
- [ ] Permission and approval engine
- [ ] Filesystem tools
- [ ] Local document search
- [ ] Browser worker
- [ ] Procedural and episodic memory

## Milestone 7 — Mini cloud

- [ ] Authenticated LAN access
- [ ] Optional encrypted remote access
- [ ] Multi-user isolation
- [ ] Rate limits and audit trail
- [ ] Webhook receiver

## Milestone 8 — Kimi K3 research

- [ ] Inspect an official checkpoint and licence
- [ ] Publish an exact tensor and storage report
- [ ] Identify dense, shared-expert and routed-expert components
- [ ] Implement tokenizer and architecture reference path
- [ ] Validate reference outputs before quantization
- [ ] Design an expert-addressable Android package format
- [ ] Run mixed-precision quantization experiments
- [ ] Implement out-of-core Android weight streaming
- [ ] Execute the first official K3 expert on Android
- [ ] Execute the first complete K3 layer on Android
- [ ] Generate the first verified offline K3 token on one phone

No K3 milestone will be marked complete without reproducible commands, hashes, hardware details, logs and reference-output comparison.