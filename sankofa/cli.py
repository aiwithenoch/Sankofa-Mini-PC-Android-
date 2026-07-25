from __future__ import annotations

import argparse
import json
import os
import signal
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

from .config import get_settings
from .device import collect_device_info


def _read_pid(path: Path) -> int | None:
    try:
        return int(path.read_text().strip())
    except (OSError, ValueError):
        return None


def _is_running(pid: int | None) -> bool:
    if not pid:
        return False
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def command_check(json_output: bool = False) -> int:
    info = collect_device_info()
    if json_output:
        print(json.dumps(info, indent=2))
        return 0

    print("Sankofa Mini PC — Device Check")
    print("=" * 36)
    print(f"Device:       {info['device_model']}")
    print(f"Android:      {info['android_version'] or 'Not detected'}")
    print(f"Architecture: {info['architecture']}")
    print(f"Processor:    {info['soc'] or 'Unknown'}")
    print(f"CPU cores:    {info['cpu_cores']}")
    print(f"RAM:          {info['ram_total_gb']} GB total")
    print(f"Storage:      {info['storage_free_gb']} GB free")
    print(f"Tier:         {info['compatibility_tier']}")
    print()
    for name, passed in info["checks"].items():
        print(f"[{'PASS' if passed else 'FAIL'}] {name}")
    print()
    print("Note: full Kimi K3 requirements will be finalized after checkpoint inspection.")
    return 0 if info["foundation_ready"] else 2


def command_start() -> int:
    settings = get_settings()
    pid = _read_pid(settings.pid_path)
    if _is_running(pid):
        print(f"Sankofa is already running (PID {pid}).")
        return 0

    log_handle = settings.log_path.open("ab")
    process = subprocess.Popen(
        [
            sys.executable,
            "-m",
            "uvicorn",
            "sankofa.app:app",
            "--host",
            settings.host,
            "--port",
            str(settings.port),
        ],
        stdin=subprocess.DEVNULL,
        stdout=log_handle,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )
    settings.pid_path.write_text(str(process.pid))
    time.sleep(1)
    if process.poll() is not None:
        print(f"Sankofa failed to start. Check {settings.log_path}", file=sys.stderr)
        return 1
    print(f"Sankofa started (PID {process.pid}).")
    print(f"Dashboard: http://{settings.host}:{settings.port}")
    return 0


def command_stop() -> int:
    settings = get_settings()
    pid = _read_pid(settings.pid_path)
    if not _is_running(pid):
        settings.pid_path.unlink(missing_ok=True)
        print("Sankofa is not running.")
        return 0
    assert pid is not None
    os.kill(pid, signal.SIGTERM)
    for _ in range(30):
        if not _is_running(pid):
            settings.pid_path.unlink(missing_ok=True)
            print("Sankofa stopped.")
            return 0
        time.sleep(0.1)
    os.kill(pid, signal.SIGKILL)
    settings.pid_path.unlink(missing_ok=True)
    print("Sankofa was force-stopped.")
    return 0


def command_status() -> int:
    settings = get_settings()
    pid = _read_pid(settings.pid_path)
    running = _is_running(pid)
    print(f"Process: {'running' if running else 'stopped'}" + (f" (PID {pid})" if running else ""))
    url = f"http://{settings.host}:{settings.port}/health"
    if running:
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                payload = json.loads(response.read().decode("utf-8"))
            print(f"Health:  {payload.get('status', 'unknown')}")
            print(f"Model:   {payload.get('model', {}).get('model', 'unknown')}")
            print(f"UI:      http://{settings.host}:{settings.port}")
            return 0
        except (urllib.error.URLError, json.JSONDecodeError):
            print("Health:  process exists but API is unavailable")
            return 1
    return 3


def command_logs(lines: int) -> int:
    settings = get_settings()
    if not settings.log_path.exists():
        print("No logs yet.")
        return 0
    content = settings.log_path.read_text(errors="replace").splitlines()
    print("\n".join(content[-max(1, lines):]))
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="sankofa",
        description="Control the Sankofa Mini PC local AI server.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    check = subparsers.add_parser("check", help="Inspect phone compatibility")
    check.add_argument("--json", action="store_true", help="Print machine-readable output")
    subparsers.add_parser("start", help="Start the local server")
    subparsers.add_parser("stop", help="Stop the local server")
    subparsers.add_parser("restart", help="Restart the local server")
    subparsers.add_parser("status", help="Show server status")
    logs = subparsers.add_parser("logs", help="Show recent logs")
    logs.add_argument("-n", "--lines", type=int, default=80)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.command == "check":
        code = command_check(args.json)
    elif args.command == "start":
        code = command_start()
    elif args.command == "stop":
        code = command_stop()
    elif args.command == "restart":
        command_stop()
        code = command_start()
    elif args.command == "status":
        code = command_status()
    elif args.command == "logs":
        code = command_logs(args.lines)
    else:
        code = 1
    raise SystemExit(code)


if __name__ == "__main__":
    main()
