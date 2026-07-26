# Sankofa Mini PC (Android)

**Turn an Android phone into a self-hosted local AI computer.**

Sankofa Mini PC is a repo-first, headless system that runs inside Termux and exposes a browser dashboard, local API, persistent memory, agent services, device monitoring, and pluggable inference runtimes. It is not primarily an Android app.

> **Your phone. Your computer. Your AI.**

[**Check My Phone**](https://sankofa-mini-pc-android.vercel.app/) · [Installation](#quick-start) · [Android runtime experiment](experiments/colibri-termux/README.md) · [Roadmap](docs/ROADMAP.md)

## Current status

**Foundation release — v0.1.0**

Working now:

- One-command Termux installation
- Quick browser compatibility checker
- Accurate terminal device checker
- Headless local server
- Browser dashboard at `http://127.0.0.1:8787`
- SQLite conversation memory
- OpenAI-compatible local API skeleton
- Pluggable model-backend interface
- Mock local model for end-to-end testing
- Start, stop, status, restart, and logs commands

Not implemented yet:

- Real local inference in the default installer
- Colibri runtime on Android
- Full Kimi K3 runtime
- Local browser automation
- Agent tool execution
- Secure external access
- Automatic boot setup
- Model shard downloader

## Current research direction

Sankofa is now evaluating [Colibri](https://github.com/JustVugg/colibri) as an optional runtime for models that are much larger than available RAM.

Colibri demonstrates a useful architecture for very large Mixture-of-Experts models: keep dense components and hot experts in fast memory, keep the larger expert set on storage, and stream selected experts when the router needs them.

This does **not** mean that Colibri, GLM-5.2, or Kimi K3 already runs on Android. The immediate engineering goal is narrower and measurable:

> Compile Colibri inside Termux on an ARM64 Android phone and pass its tiny architecture self-test.

Sankofa remains the Android product layer: installer, lifecycle management, device checks, downloads, dashboard, API, memory, permissions, agents, thermal controls, and runtime adapters. Colibri is being evaluated as one possible massive-MoE inference engine behind that layer.

No Colibri source code is currently vendored into this repository. Any future source integration must preserve its Apache-2.0 licence and attribution requirements. See [`docs/COLIBRI_INTEGRATION.md`](docs/COLIBRI_INTEGRATION.md).

## Immediate Android experiment

On an Android phone with the current Termux build installed, clone this repository and run:

```bash
git clone https://github.com/aiwithenoch/Sankofa-Mini-PC-Android-.git
cd Sankofa-Mini-PC-Android-
bash experiments/colibri-termux/probe.sh
```

The probe:

- records Android, CPU, RAM, storage and compiler details;
- installs only the build tools required for the experiment;
- clones a shallow copy of Colibri;
- checks whether Termux clang can build an OpenMP program;
- attempts a native ARM64 build;
- runs Colibri's bundled tiny self-test when the build succeeds;
- writes a complete log without downloading any large model weights.

The result log is saved under:

```text
~/.sankofa/experiments/colibri-termux/
```

A successful compile is only the first proof. It does not establish usable generation speed, phone thermal stability, or K3 compatibility.

## Quick start

### 1. Check your phone

Open the [browser checker](https://sankofa-mini-pc-android.vercel.app/). It performs a quick estimate before installation. The terminal checker remains the accurate source of truth.

### 2. Install Termux

Install the current Termux release from its official F-Droid or GitHub release page. The old Google Play build is not recommended.

- F-Droid: https://f-droid.org/packages/com.termux/
- GitHub: https://github.com/termux/termux-app/releases

### 3. Run one command

Open Termux and paste:

```bash
curl -fsSL https://raw.githubusercontent.com/aiwithenoch/Sankofa-Mini-PC-Android-/main/install.sh | bash
```

### 4. Open the dashboard

```text
http://127.0.0.1:8787
```

The dashboard is served by the phone itself. No hosted web application is required.

## Device checker

Run:

```bash
sankofa check
```

Machine-readable output:

```bash
sankofa check --json
```

The checker reports Android version, CPU architecture, SoC when available, RAM, storage, thermal sensors, and a compatibility tier.

**Important:** exact Kimi K3 requirements cannot be finalized until an official checkpoint is available and inspected. The current checker verifies whether the phone can run the Sankofa foundation and labels unusually powerful devices as K3 research candidates.

## Commands

```bash
sankofa check
sankofa start
sankofa stop
sankofa restart
sankofa status
sankofa logs
```

## Local API

Health:

```bash
curl http://127.0.0.1:8787/health
```

Models:

```bash
curl http://127.0.0.1:8787/v1/models
```

OpenAI-compatible chat endpoint:

```bash
curl http://127.0.0.1:8787/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "sankofa/mock-local",
    "messages": [{"role": "user", "content": "Hello from Android"}]
  }'
```

## Architecture

```text
Browser dashboard
       │
       ▼
Sankofa local API server
       │
       ├── SQLite memory
       ├── Device/resource monitor
       ├── Agent runtime                 [planned]
       ├── Browser worker                [planned]
       ├── Model manager                 [planned]
       └── Model backend interface
                  │
                  ├── Mock backend       [working]
                  ├── llama.cpp adapter  [planned]
                  ├── Colibri adapter    [Android experiment]
                  └── Kimi K3 backend    [research]
```

Read [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for design boundaries, [`docs/COLIBRI_INTEGRATION.md`](docs/COLIBRI_INTEGRATION.md) for the runtime evaluation, and [`docs/ROADMAP.md`](docs/ROADMAP.md) for milestones.

## Data location

Runtime data is stored under:

```text
~/.sankofa/
├── data/sankofa.db
├── experiments/
├── logs/server.log
├── models/
└── run/sankofa.pid
```

Set `SANKOFA_HOME` to change this directory.

## Network safety

Sankofa binds to `127.0.0.1` by default, so only the phone can access it. Do not bind it publicly until authentication, TLS, rate limits, and permission controls are enabled.

## Uninstall

```bash
curl -fsSL https://raw.githubusercontent.com/aiwithenoch/Sankofa-Mini-PC-Android-/main/uninstall.sh | bash
```

The uninstaller preserves `~/.sankofa` to avoid deleting conversations, experiment logs or downloaded models accidentally.

## Kimi K3 mission

The long-term research target is strict:

> Run the complete Kimi K3 architecture and all required weights locally on one Android phone, without an inference API, remote GPU, or second computer.

The project does not claim that this milestone has already been achieved. The work requires an official checkpoint, an exact architecture implementation, model-size analysis, expert-aware packaging, out-of-core execution, reference-output validation, and measured Android performance.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Technical claims must be reproducible and clearly labelled as measured, projected, or unverified.

## Licences

Sankofa Mini PC source code is licensed under the MIT License. Model weights and external runtimes retain their own licences and are not included unless explicitly documented.