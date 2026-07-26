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

## Milestone 2 — Android application foundation

- [x] Jetpack Compose Android project
- [x] Device RAM, storage, ABI and CPU profiling
- [x] Capability tiers for lite, standard, performance and research devices
- [x] JNI/C++ native-runtime boundary
- [x] Local Sankofa daemon health check
- [x] Foreground model-download worker
- [x] HTTP Range and ETag continuation for interrupted downloads
- [x] SHA-256 and optional byte-size verification
- [x] Atomic model activation after verification
- [x] Official verified starter-model catalog entry
- [x] Android APK build workflow
- [ ] Install and smoke-test the APK on physical ARM64 phones
- [ ] Publish a signed alpha APK through GitHub Releases

## Milestone 3 — Real everyday local inference

This is the next product milestone.

- [ ] Pin and audit a compatible `llama.cpp` revision
- [ ] Compile `llama.cpp` for Android ARM64 through the NDK
- [ ] Load the verified Qwen3 0.6B starter GGUF
- [ ] Stream tokens into the Compose chat interface
- [ ] Add generation cancellation and timeout handling
- [ ] Add context-length, thread-count and RAM budgets
- [ ] Add thermal throttling and background resource release
- [ ] Benchmark 4 GB, 6 GB, 8 GB, 12 GB and 16 GB phones
- [ ] Add larger catalog models only after measured device testing

## Milestone 4 — Agent computer

- [x] Optional Composio gateway boundary
- [x] Composio project key kept outside the APK
- [x] Toolkit-prefix allowlist
- [x] Read/write/destructive risk classification
- [x] One-time user approval for external writes
- [x] Destructive cloud tools blocked by default
- [x] Gateway TypeScript CI
- [ ] Replace the private-alpha shared token with per-user authentication
- [ ] Persistent local task queue
- [ ] Tool registry with exact-slug policies
- [ ] Planner-to-tool-call protocol
- [ ] Result verification and retry rules
- [ ] Filesystem tools
- [ ] Local document search
- [ ] Browser worker
- [ ] Calendar and Gmail read/draft workflows
- [ ] Procedural and episodic memory
- [ ] Privacy-preserving audit history

## Milestone 5 — Android massive-model runtime proof

- [x] Document Colibri as a runtime candidate
- [x] Add a non-model Termux probe script
- [ ] Run the probe on a real ARM64 Android phone
- [ ] Compile Colibri with Termux clang
- [ ] Pass the bundled tiny self-test at the expected result
- [ ] Pin and audit an Android-compatible Colibri revision
- [ ] Preserve Apache-2.0 notices and attribution
- [ ] Link Colibri behind the existing JNI contract
- [ ] Record exact phone, Android, Termux and compiler versions
- [ ] Measure internal-storage sequential and random-read performance
- [ ] Record sustained CPU temperature and frequency behaviour
- [ ] Identify required Android compatibility patches
- [ ] Publish the first reproducible Android experiment report

Success at this milestone proves only that the engine can execute correctly on Android. It does not prove that a frontier model is fast, thermally sustainable or small enough for the phone.

## Milestone 6 — Adaptive runtime governor

- [ ] Monitor Android thermal status and available memory
- [ ] Reduce thread count and cache size under pressure
- [ ] Pause generation at severe thermal levels
- [ ] Unload models before the operating system kills the app
- [ ] Choose model profiles from measured device capability
- [ ] Preserve tasks and conversations across native-runtime crashes
- [ ] Keep the user interface responsive in a separate process

## Milestone 7 — Reliable local server

- [ ] Streaming token responses
- [ ] API authentication
- [ ] Structured configuration file
- [ ] Automatic startup with Termux:Boot where supported
- [ ] Watchdog and crash recovery
- [ ] Database backups and migrations
- [ ] Release checksums and signed installer

## Milestone 8 — Production model manager

- [x] Resumable single-file downloads
- [x] SHA-256 verification
- [x] Atomic activation
- [ ] Signed model manifests
- [ ] Storage reservation before download
- [ ] Resumable multi-shard downloads
- [ ] Per-shard checksums
- [ ] Mirror failover
- [ ] Activation rollback
- [ ] Runtime/model compatibility checks
- [ ] Safe model removal
- [ ] Wi-Fi-only and charging-only policies
- [ ] Android reboot recovery tests

## Milestone 9 — Mini cloud

- [ ] Authenticated LAN access
- [ ] Optional encrypted remote access
- [ ] Multi-user isolation
- [ ] Per-user rate limits and audit trail
- [ ] Webhook receiver

## Milestone 10 — Kimi K3 research

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
