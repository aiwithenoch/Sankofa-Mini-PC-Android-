from sankofa.models import ChatMessage, GenerationRequest, MockBackend


def test_mock_backend_is_deterministic() -> None:
    backend = MockBackend()
    request = GenerationRequest(messages=[ChatMessage(role="user", content="Hello")])
    first = backend.generate(request)
    second = backend.generate(request)
    assert first == second
    assert "Hello" in first
    assert backend.model_id == "sankofa/mock-local"
