from __future__ import annotations

from .base import GenerationRequest, ModelBackend


class MockBackend(ModelBackend):
    """A deterministic local backend used while the K3 runtime is developed."""

    @property
    def model_id(self) -> str:
        return "sankofa/mock-local"

    def generate(self, request: GenerationRequest) -> str:
        user_text = next(
            (message.content for message in reversed(request.messages) if message.role == "user"),
            "",
        )
        if not user_text:
            return "Sankofa Mini PC is running locally. Send a message to test the system."
        return (
            "Sankofa Mini PC received your message locally: "
            f'“{user_text[:280]}”\n\n'
            "The server, dashboard, model interface, and SQLite memory are working. "
            "The full Kimi K3 runtime will connect through the same model interface."
        )
