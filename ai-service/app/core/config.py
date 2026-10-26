from functools import lru_cache
from pathlib import Path
from typing import List, Optional

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


BASE_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=BASE_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    app_name: str = "hospital-ai-service"
    app_host: str = "127.0.0.1"
    app_port: int = 8000
    log_level: str = "INFO"
    cors_origins: str = "http://localhost:5173,http://127.0.0.1:5173"

    java_api_base_url: str = "http://127.0.0.1:8080"
    java_api_timeout_seconds: float = 5.0

    llm_base_url: Optional[str] = None
    llm_api_key: Optional[SecretStr] = None
    llm_model: str = "deepseek-chat"
    llm_temperature: float = 0.2
    llm_timeout_seconds: float = 30.0
    llm_max_retries: int = 1

    conversation_ttl_minutes: int = 30
    max_history_messages: int = 20
    confirmation_ttl_minutes: int = 5

    # Hospital knowledge base (RAG)
    knowledge_dir: str = str(BASE_DIR / "knowledge")
    rag_enabled: bool = True
    rag_top_k: int = 4
    rag_chunk_size: int = 480
    rag_chunk_overlap: int = 80
    rag_embedding_model: Optional[str] = None
    rag_embedding_base_url: Optional[str] = None
    rag_embedding_api_key: Optional[SecretStr] = None
    rag_embedding_timeout_seconds: float = 15.0

    @property
    def llm_configured(self) -> bool:
        return bool(
            self.llm_base_url
            and self.llm_model
            and self.llm_api_key
            and self.llm_api_key.get_secret_value().strip()
        )

    @property
    def normalized_llm_base_url(self) -> str:
        return (self.llm_base_url or "").rstrip("/")

    @property
    def knowledge_path(self) -> Path:
        raw = (self.knowledge_dir or '').strip()
        if not raw:
            return BASE_DIR / 'knowledge'
        return Path(raw).expanduser()

    @property
    def resolved_embedding_base_url(self) -> str:
        return (self.rag_embedding_base_url or self.llm_base_url or "").rstrip("/")

    @property
    def resolved_embedding_api_key(self) -> str:
        key = self.rag_embedding_api_key or self.llm_api_key
        return key.get_secret_value().strip() if key else ""

    @property
    def embeddings_configured(self) -> bool:
        return bool(
            self.rag_enabled
            and self.rag_embedding_model
            and self.resolved_embedding_base_url
            and self.resolved_embedding_api_key
        )

    @property
    def cors_origin_list(self) -> List[str]:
        return [item.strip() for item in self.cors_origins.split(",") if item.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
