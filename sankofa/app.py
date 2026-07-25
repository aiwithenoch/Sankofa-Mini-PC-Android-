from __future__ import annotations

from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from . import __version__
from .config import get_settings
from .device import collect_device_info
from .memory import MemoryStore
from .models import ChatMessage, GenerationRequest, MockBackend

settings = get_settings()
memory = MemoryStore(settings.database_path)
model = MockBackend()
static_dir = Path(__file__).parent / "static"

app = FastAPI(
    title="Sankofa Mini PC",
    version=__version__,
    description="A local-first AI mini-PC environment for Android.",
)
app.mount("/static", StaticFiles(directory=static_dir), name="static")


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=50_000)
    conversation_id: int | None = None
    max_tokens: int = Field(default=256, ge=1, le=4096)
    temperature: float = Field(default=0.2, ge=0, le=2)


class OpenAIMessage(BaseModel):
    role: str
    content: str


class OpenAIChatRequest(BaseModel):
    model: str | None = None
    messages: list[OpenAIMessage]
    max_tokens: int = Field(default=256, ge=1, le=4096)
    temperature: float = Field(default=0.2, ge=0, le=2)
    stream: bool = False


@app.get("/", include_in_schema=False)
def dashboard() -> FileResponse:
    return FileResponse(static_dir / "index.html")


@app.get("/health")
def health() -> dict[str, object]:
    return {
        "status": "ok",
        "version": __version__,
        "model": model.health(),
    }


@app.get("/api/system")
def system_info() -> dict[str, object]:
    return collect_device_info()


@app.get("/v1/models")
def list_models() -> dict[str, object]:
    return {
        "object": "list",
        "data": [
            {
                "id": model.model_id,
                "object": "model",
                "owned_by": "sankofa-mini-pc",
            }
        ],
    }


@app.post("/api/chat")
def chat(request: ChatRequest) -> dict[str, object]:
    conversation_id = request.conversation_id
    if conversation_id is None:
        conversation_id = memory.create_conversation(request.message[:80])

    try:
        memory.add_message(conversation_id, "user", request.message)
        history = memory.get_messages(conversation_id)
        response = model.generate(
            GenerationRequest(
                messages=[ChatMessage(role=str(item["role"]), content=str(item["content"])) for item in history],
                max_tokens=request.max_tokens,
                temperature=request.temperature,
            )
        )
        memory.add_message(conversation_id, "assistant", response)
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Local generation failed") from exc

    return {
        "conversation_id": conversation_id,
        "model": model.model_id,
        "response": response,
    }


@app.post("/v1/chat/completions")
def openai_chat(request: OpenAIChatRequest) -> dict[str, object]:
    if request.stream:
        raise HTTPException(status_code=501, detail="Streaming arrives in milestone 2")
    response = model.generate(
        GenerationRequest(
            messages=[ChatMessage(role=item.role, content=item.content) for item in request.messages],
            max_tokens=request.max_tokens,
            temperature=request.temperature,
        )
    )
    return {
        "id": "chatcmpl-sankofa-local",
        "object": "chat.completion",
        "model": model.model_id,
        "choices": [
            {
                "index": 0,
                "message": {"role": "assistant", "content": response},
                "finish_reason": "stop",
            }
        ],
        "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0},
    }
