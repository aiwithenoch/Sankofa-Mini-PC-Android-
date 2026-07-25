from __future__ import annotations

import json
import os
import platform
import shutil
import subprocess
from pathlib import Path
from typing import Any

import psutil


def _android_property(name: str) -> str | None:
    getprop = shutil.which("getprop")
    if not getprop:
        return None
    try:
        value = subprocess.check_output([getprop, name], text=True, timeout=2).strip()
        return value or None
    except (subprocess.SubprocessError, OSError):
        return None


def _thermal_zones() -> list[dict[str, Any]]:
    zones: list[dict[str, Any]] = []
    root = Path("/sys/class/thermal")
    if not root.exists():
        return zones
    for zone in sorted(root.glob("thermal_zone*")):
        try:
            raw = (zone / "temp").read_text().strip()
            value = float(raw)
            celsius = value / 1000 if value > 1000 else value
            zone_type = (zone / "type").read_text().strip() if (zone / "type").exists() else zone.name
            if -20 <= celsius <= 150:
                zones.append({"name": zone_type, "celsius": round(celsius, 1)})
        except (OSError, ValueError):
            continue
    return zones[:20]


def collect_device_info() -> dict[str, Any]:
    memory = psutil.virtual_memory()
    disk = psutil.disk_usage(str(Path.home()))
    android_version = _android_property("ro.build.version.release")
    device_model = _android_property("ro.product.model")
    soc_model = _android_property("ro.soc.model") or _android_property("ro.board.platform")
    architecture = platform.machine()

    total_ram_gb = round(memory.total / (1024**3), 2)
    free_storage_gb = round(disk.free / (1024**3), 2)

    result = {
        "is_android": bool(os.getenv("ANDROID_ROOT") or android_version),
        "device_model": device_model or platform.node() or "Unknown",
        "android_version": android_version,
        "architecture": architecture,
        "soc": soc_model,
        "cpu_cores": psutil.cpu_count(logical=True),
        "ram_total_gb": total_ram_gb,
        "ram_available_gb": round(memory.available / (1024**3), 2),
        "storage_total_gb": round(disk.total / (1024**3), 2),
        "storage_free_gb": free_storage_gb,
        "thermal_zones": _thermal_zones(),
    }

    checks = {
        "android": result["is_android"],
        "arm64": architecture in {"aarch64", "arm64", "arm64-v8a"},
        "python_ready": platform.python_version_tuple() >= ("3", "11", "0"),
        "minimum_ram_4gb": total_ram_gb >= 4,
        "minimum_free_storage_8gb": free_storage_gb >= 8,
    }
    result["checks"] = checks
    result["foundation_ready"] = all(checks.values())

    if total_ram_gb >= 20 and free_storage_gb >= 700:
        tier = "K3 research candidate"
    elif total_ram_gb >= 8 and free_storage_gb >= 32:
        tier = "Local AI agent ready"
    elif result["foundation_ready"]:
        tier = "Foundation ready"
    else:
        tier = "Not ready"
    result["compatibility_tier"] = tier
    return result


def device_info_json() -> str:
    return json.dumps(collect_device_info(), indent=2)
