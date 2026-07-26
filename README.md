# Sankofa Mini PC (Android)

**Turn an Android phone into a self-hosted local AI computer.**

Sankofa has two layers:

1. an Android app for normal users; and
2. a local runtime for models, memory, agents, downloads and device control.

> **Your phone. Your computer. Your AI.**

[**Download Android alpha**](https://github.com/aiwithenoch/Sankofa-Mini-PC-Android-/releases) · [Check My Phone](https://sankofa-mini-pc-android.vercel.app/) · [Termux installation](#termux-foundation) · [Roadmap](docs/ROADMAP.md)

## Current status

**Android foundation — v0.2.0 alpha**

Working now:

- Installable Jetpack Compose Android APK
- Phone RAM, storage, ABI and CPU profiling
- Lite, standard, performance and research capability tiers
- Native C++/JNI runtime boundary
- Local Sankofa daemon health check
- Resumable HTTPS model downloads
- HTTP Range and ETag continuation after interruption
- SHA-256 verification before model activation
- Official Qwen3 0.6B Q8 starter-model catalog entry
- Optional Composio gateway client
- One-time approval prompts for external writes
- Destructive cloud tools blocked by default
- CI-built APK and gateway type checks
- Existing Termux server, dashboard, SQLite memory and mock model

Not completed yet:

- Local token generation inside the APK
- Embedded `llama.cpp` runtime
- Embedded Colibri runtime
- Physical-device validation across phone tiers
- Production signing and Play Store/F-Droid distribution
- Public multi-user authentication for the agent gateway
- Full Kimi K3 runtime

The alpha can download and verify the starter GGUF, but it cannot generate tokens from that file until the `llama.cpp` integration is completed.

## Download the Android app

Open the repository's [Releases page](https://github.com/aiwithenoch/Sankofa-Mini-PC-Android-/releases) and download:

```text
Sankofa-Mini-PC-0.2.0-alpha-debug.apk
```

This first APK is debug-signed for testing. Android may ask you to allow installation from your browser or file manager. Do not treat it as a production release yet.

The release also includes `SHA256SUMS.txt` so the APK can be verified before installation.

## What the Android alpha does

```text
Android app
├── Phone profiler
├── Verified model downloader
├── Local runtime health controls
├── Native JNI bridge
├── Model catalog
└── Agent approvals
        │
        └── Optional secure gateway ──► Composio tools
```

The app remains useful without Composio. Cloud-account tools are optional and must go through the separate gateway so the Composio project key is never placed inside the APK.

Read [`android/README.md`](android/README.md) and [`gateway/README.md`](gateway/README.md) for build and deployment details.

## Starter model

The alpha catalog currently contains one deliberately small proof model:

```text
Qwen3 0.6B Q8 GGUF
```

The downloader provides interruption recovery and checksum verification. The model is not bundled into the APK because model files are far larger than application packages and different phones require different models.

Future catalog tiers will be enabled only after physical-device measurements:

- Lite phones: tiny models
- Standard phones: small local models
- Performance phones: larger quantized models
- Research phones: experimental out-of-core and MoE runtimes

## Agentic architecture

Sankofa does not give a model unrestricted control over accounts. The model proposes a tool call; Sankofa applies policy before anything runs.

```text
User request
    ↓
Local model or planner
    ↓
Tool request
    ↓
Risk classification
    ├── Read-only: may run
    ├── External write: approval required
    └── Destructive: blocked by default
    ↓
Gateway execution
    ↓
Result returned to the local agent
```

The current gateway supports Composio connection links and tool execution behind:

- a server-side API key;
- toolkit-prefix allowlists;
- one-time approval checks; and
- destructive-action blocking.

The shared alpha gateway token is suitable only for private testing. A public release requires per-user authentication, short-lived tokens, rate limits and app attestation.

## Termux foundation

The existing headless Sankofa runtime still works through Termux.

### 1. Check your phone

Open the [browser checker](https://sankofa-mini-pc-android.vercel.app/). The terminal checker remains the more accurate source of truth.

### 2. Install Termux

Use the current Termux release from F-Droid or its official GitHub releases. Avoid obsolete builds.

### 3. Install Sankofa

```bash
curl -fsSL https://raw.githubusercontent.com/aiwithenoch/Sankofa-Mini-PC-Android-/main/install.sh | bash
```

### 4. Start and open the dashboard

```bash
sankofa start
```

```text
http://127.0.0.1:8787
```

### Commands

```bash
sankofa check
sankofa check --json
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

OpenAI-compatible chat skeleton:

```bash
curl http://127.0.0.1:8787/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "sankofa/mock-local",
    "messages": [{"role": "user", "content": "Hello from Android"}]
  }'
```

## Runtime architecture

```text
Android Compose app
        │
        ├── Verified model manager
        ├── Agent policy and approvals
        ├── Local daemon client
        └── JNI runtime contract
                  │
                  ├── Native status stub       [working]
                  ├── llama.cpp backend        [next]
                  ├── Colibri backend          [experiment]
                  └── Kimi K3 backend          [research]

Termux daemon
        ├── FastAPI local API
        ├── SQLite conversation memory
        ├── Mock model backend
        └── Browser dashboard
```

## Colibri research

Sankofa is evaluating [Colibri](https://github.com/JustVugg/colibri) as an optional engine for models larger than available RAM. Colibri keeps dense components and hot experts in memory while streaming selected experts from storage.

This does **not** mean Colibri or Kimi K3 already runs inside the Android app. The next proof is still:

> Compile Colibri on a real ARM64 Android phone and pass its bundled tiny correctness test.

Run the Termux experiment with:

```bash
git clone https://github.com/aiwithenoch/Sankofa-Mini-PC-Android-.git
cd Sankofa-Mini-PC-Android-
bash experiments/colibri-termux/probe.sh
```

The probe does not download giant model weights. Read [`docs/COLIBRI_INTEGRATION.md`](docs/COLIBRI_INTEGRATION.md) for the integration boundary.

## Data locations

Termux runtime data:

```text
~/.sankofa/
├── data/sankofa.db
├── experiments/
├── logs/server.log
├── models/
└── run/sankofa.pid
```

Android app model files are stored inside the app's private storage and activated only after verification.

## Network safety

The local Sankofa server binds to `127.0.0.1` by default. Do not expose it publicly without authentication, encryption, rate limits and permission controls.

Never place a Composio API key, OAuth client secret or production gateway secret inside Android resources, `BuildConfig` or the APK.

## Kimi K3 mission

The long-term research target remains strict:

> Run the complete Kimi K3 architecture and all required weights locally on one Android phone, without an inference API, remote GPU or second computer.

The project does not claim this has already been achieved. It requires an official checkpoint, exact architecture implementation, model-size analysis, expert-aware packaging, out-of-core execution, reference-output validation and measured Android performance.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Technical claims must be reproducible and labelled as measured, projected or unverified.

## Licences

Sankofa Mini PC source code is licensed under the MIT License. Model weights and external runtimes retain their own licences and are not included unless explicitly documented.
