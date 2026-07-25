from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Iterable


@dataclass(frozen=True)
class ChatMessage:
    role: str
    content: str


@dataclass(frozen=True)
class GenerationRequest:
    messages: list[ChatMessage]
    max_tokens: int = 256
    temperature: float = 0.2


class ModelBackend(ABC):
    """Stable contract between Sankofa Core and any local model runtime."""

    @property
    @abstractmethod
    def model_id(self) -> str:
        raise NotImplementedError

    @abstractmethod
    def generate(self, request: GenerationRequest) -> str:
        raise NotImplementedError

    def stream(self, request: GenerationRequest) -> Iterable[str]:
        yield self.generate(request)

    def health(self) -> dict[str, object]:
        return {"model": self.model_id, "status": "ready"}
