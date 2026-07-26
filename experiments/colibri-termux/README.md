# Colibri on Termux — Phase 1 Probe

This experiment answers one question:

> Can Colibri compile and pass its bundled tiny correctness test directly inside Termux on an ARM64 Android phone?

It does **not** download GLM-5.2, Kimi K3 or any other large model.

## Requirements

- ARM64 Android phone
- Current Termux installation
- Internet access for package and source downloads
- At least 2 GB of free working storage
- Phone connected to power for the build

Close demanding apps before running the probe. Keep the phone uncovered and stop if Android displays an overheating warning.

## Run

From the Sankofa repository:

```bash
bash experiments/colibri-termux/probe.sh
```

The script installs standard Termux build tools, clones a shallow copy of Colibri, tests OpenMP support, attempts the native build and runs the tiny test when possible.

## Output

Logs are stored at:

```text
~/.sankofa/experiments/colibri-termux/
```

The final terminal output prints the exact log path.

## Result meanings

### `PASS`

The Colibri executable compiled and the bundled tiny test reported the expected `32/32 positions` result.

This proves basic Android compilation and tiny-model correctness only.

### `BUILD_ONLY`

The executable compiled, but the tiny reference fixture was unavailable or did not produce the expected marker.

### `BUILD_FAILED`

The current upstream source did not compile under the phone's Termux toolchain. The log should identify the first Android compatibility issue.

### `ENVIRONMENT_FAILED`

The required Termux packages or compiler probe failed before the Colibri build began.

## Sharing a result

Do not paste only the last line. Include:

- the complete generated log;
- phone model;
- Android version;
- whether the phone was charging;
- free RAM before the run;
- any heat or throttling observed.

Remove personally identifying paths or device names before posting publicly.

## What comes after a pass

1. Reproduce the result on another Android phone.
2. Pin the tested Colibri commit.
3. Add any required Android patches cleanly.
4. Benchmark phone storage with expert-like read patterns.
5. Add thermal and frequency logging.
6. Integrate the engine behind a Sankofa runtime adapter.

Large model downloads come only after these smaller proofs are repeatable.