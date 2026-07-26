# Sankofa Android App

The Android app is the normal-user control surface for Sankofa Mini PC. It does not replace the local runtime; it manages and talks to it.

## Working in this alpha foundation

- Jetpack Compose control interface
- Device RAM, storage, CPU, ABI, and compatibility tier detection
- JNI/native C++ runtime boundary
- Local Sankofa health check at `127.0.0.1:8787`
- Foreground, resumable HTTPS model downloads
- `.part` files and HTTP range continuation
- Optional expected-size verification
- SHA-256 verification before activation
- Composio connection-link flow through the gateway
- Tool-risk classification
- One-time approval dialog for external writes
- Destructive tools blocked by default

## Not bundled yet

- Model weights
- `llama.cpp`
- Colibri source code
- A Kimi K3 backend
- Automatic Termux installation or process control
- Public multi-user authentication for the Composio gateway

Model weights cannot reasonably be stored inside the APK. The app downloads selected model files after installation and verifies them before use.

## Open in Android Studio

Open the `android/` directory as a Gradle project.

Required components:

- JDK 17
- Android SDK 35
- Android NDK
- CMake 3.22.1

Build from a machine with Gradle installed:

```bash
gradle -p android :app:assembleDebug
```

The debug APK will be produced under:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Runtime architecture

```text
Compose app
   │
   ├── DeviceProfiler
   ├── ModelDownloadWorker
   ├── AgentPolicy
   ├── GatewayClient ──────► optional Sankofa gateway ──────► Composio
   │
   ├── local HTTP health ──► Sankofa/Termux daemon
   │
   └── JNI ────────────────► libsankofa_runtime.so
                                  │
                                  ├── llama.cpp target [next]
                                  └── Colibri target   [experimental]
```

## Downloader guarantees

The downloader:

1. Requires HTTPS.
2. Writes to `<model>.part`.
3. Uses `Range` and `If-Range` when a partial file and ETag exist.
4. Survives app-process restarts through WorkManager.
5. Verifies the declared byte size when supplied.
6. Verifies SHA-256 when supplied.
7. Moves the model into its final filename only after verification.

A model catalog should always publish size and SHA-256. URLs without a checksum are supported for development but should not be used in production.

## Colibri integration boundary

The current native library reports architecture and NEON availability. Colibri is deliberately not copied into the APK yet. The next native milestone is:

1. Pin an audited Colibri commit.
2. Preserve its Apache-2.0 notices.
3. Build its architecture self-test for Android.
4. Pass that test on physical ARM64 phones.
5. Add cancellation, telemetry, memory budgets, and thermal throttling.
6. Only then expose massive-model downloads in the app.
