# Sankofa Mini PC (Android)

**Turn an Android phone into a self-hosted local AI computer.**

Sankofa Mini PC is a repo-first, headless system that runs inside Termux and exposes a browser dashboard, local API, persistent memory, agent services, device monitoring, and a pluggable inference runtime. It is not primarily an Android app.

> **Your phone. Your computer. Your AI.**

[**Check My Phone**](https://sankofa-mini-pc-android.vercel.app/) · [Installation](#quick-start) · [Roadmap](docs/ROADMAP.md)

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

- Full Kimi K3 runtime
- Local browser automation
- Agent tool execution
- Secure external access
- Automatic boot setup
- Model shard downloader

The repository intentionally builds the complete system around the model first. Kimi K3 will be inserted through the stable model-backend interface after its checkpoint format is available and studied.

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

**Important:** exact Kimi K3 requirements cannot be finalized until the public checkpoint is inspected. The current checker verifies whether the phone can run the Sankofa foundation and labels unusually powerful devices as K3 research candidates.

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
       ├── Agent runtime           [planned]
       ├── Browser worker          [planned]
       ├── Model manager           [planned]
       └── Model backend interface
                  │
                  ├── Mock backend [working]
                  ├── Small local backend [next]
                  └── Kimi K3 backend [research]
```

Read [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for design boundaries and [`docs/ROADMAP.md`](docs/ROADMAP.md) for milestones.

## Data location

Runtime data is stored under:

```text
~/.sankofa/
├── data/sankofa.db
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

The uninstaller preserves `~/.sankofa` to avoid deleting conversations or downloaded models accidentally.

## Kimi K3 mission

The long-term research target is strict:

> Run the complete Kimi K3 architecture and all required weights locally on one Android phone, without an inference API, remote GPU, or second computer.

The foundation does not claim that this milestone has already been achieved. Research will focus on expert-aware quantization, out-of-core weight streaming, cache-aware MoE execution, and Android CPU/GPU/NPU scheduling.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Technical claims must be reproducible and clearly labeled as measured, projected, or unverified.

## License

Sankofa Mini PC source code is licensed under the MIT License. Model weights retain their own licences and are not included in this repository.
