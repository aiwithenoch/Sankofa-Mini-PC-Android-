from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Settings:
    home: Path
    host: str
    port: int
    model_backend: str

    @property
    def data_dir(self) -> Path:
        return self.home / "data"

    @property
    def log_dir(self) -> Path:
        return self.home / "logs"

    @property
    def run_dir(self) -> Path:
        return self.home / "run"

    @property
    def model_dir(self) -> Path:
        return self.home / "models"

    @property
    def database_path(self) -> Path:
        return self.data_dir / "sankofa.db"

    @property
    def pid_path(self) -> Path:
        return self.run_dir / "sankofa.pid"

    @property
    def log_path(self) -> Path:
        return self.log_dir / "server.log"

    def ensure_directories(self) -> None:
        for path in (self.home, self.data_dir, self.log_dir, self.run_dir, self.model_dir):
            path.mkdir(parents=True, exist_ok=True)


def get_settings() -> Settings:
    default_home = Path.home() / ".sankofa"
    settings = Settings(
        home=Path(os.getenv("SANKOFA_HOME", str(default_home))).expanduser(),
        host=os.getenv("SANKOFA_HOST", "127.0.0.1"),
        port=int(os.getenv("SANKOFA_PORT", "8787")),
        model_backend=os.getenv("SANKOFA_MODEL_BACKEND", "mock"),
    )
    settings.ensure_directories()
    return settings
