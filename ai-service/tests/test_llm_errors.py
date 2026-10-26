import asyncio

import httpx
import pytest
from langchain_core.messages import AIMessage
from openai import APITimeoutError

from app.core.config import Settings
from app.services.llm_service import (
    LlmNotConfiguredError,
    LlmProviderError,
    LlmService,
    LlmTimeoutError,
)


class TimeoutModel:
    async def ainvoke(self, messages):
        raise APITimeoutError(request=httpx.Request("POST", "http://test-llm"))


class EmptyModel:
    async def ainvoke(self, messages):
        return AIMessage(content="")


def test_llm_not_configured_error() -> None:
    async def run() -> None:
        service = LlmService(Settings(llm_base_url=None, llm_api_key=None))
        with pytest.raises(LlmNotConfiguredError):
            await service.chat([], "你好")

    asyncio.run(run())


def test_llm_timeout_maps_to_domain_error() -> None:
    async def run() -> None:
        service = LlmService(Settings(llm_base_url=None, llm_api_key=None))
        service._client = TimeoutModel()
        with pytest.raises(LlmTimeoutError):
            await service.chat([], "你好")

    asyncio.run(run())


def test_llm_empty_response_maps_to_provider_error() -> None:
    async def run() -> None:
        service = LlmService(Settings(llm_base_url=None, llm_api_key=None))
        service._client = EmptyModel()
        with pytest.raises(LlmProviderError):
            await service.chat([], "你好")

    asyncio.run(run())
