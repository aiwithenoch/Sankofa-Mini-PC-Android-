# Contributing

Thank you for helping build Sankofa Mini PC.

## Development setup

```bash
git clone https://github.com/aiwithenoch/Sankofa-Mini-PC-Android-.git
cd Sankofa-Mini-PC-Android-
python -m venv .venv
source .venv/bin/activate
pip install -e .
sankofa start
```

Open `http://127.0.0.1:8787`.

## Pull-request expectations

- Keep Android/Termux compatibility in mind.
- Add tests for new core behavior.
- Do not expose the server publicly by default.
- Do not add telemetry that leaves the device without explicit opt-in.
- Include hardware, model, precision, context, and exact commands for benchmarks.
- Label unmeasured performance ideas as projections, not results.

## Commit style

Use focused messages such as:

```text
feat: add resumable model shard downloads
fix: recover stale daemon pid file
docs: document Snapdragon benchmark process
```
