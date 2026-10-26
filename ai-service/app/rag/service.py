import asyncio
import logging
from datetime import datetime, timezone
from typing import Dict, List, Optional

from app.core.config import Settings
from app.rag.embeddings import EmbeddingClient
from app.rag.loader import load_knowledge_chunks
from app.rag.store import KnowledgeHit, KnowledgeStore


logger = logging.getLogger(__name__)


class KnowledgeService:
    """Owns the knowledge index lifecycle for the AI service."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._directory = settings.knowledge_path
        self._store: Optional[KnowledgeStore] = None
        self._loaded = False
        self._last_built: Optional[datetime] = None
        self._last_error: Optional[str] = None
        self._locks: Dict[int, asyncio.Lock] = {}

    @property
    def enabled(self) -> bool:
        return self._settings.rag_enabled

    async def ensure_loaded(self) -> None:
        if self._loaded:
            return
        async with self._get_lock():
            if self._loaded:
                return
            await self._rebuild_locked()

    async def refresh(self) -> dict:
        async with self._get_lock():
            await self._rebuild_locked()
        return self.status()

    async def search(self, query: str, top_k: Optional[int] = None) -> List[KnowledgeHit]:
        if not self.enabled:
            return []
        await self.ensure_loaded()
        if self._store is None:
            return []
        limit = top_k if top_k is not None else self._settings.rag_top_k
        return await self._store.search(query, limit)

    def status(self) -> dict:
        store = self._store
        chunks = store.chunks if store else []
        return {
            "enabled": self.enabled,
            "loaded": self._loaded,
            "documents": len({chunk.source for chunk in chunks}),
            "chunks": len(chunks),
            "mode": store.mode if store else "none",
            "embedding_model": (
                self._settings.rag_embedding_model
                if self._settings.embeddings_configured
                else None
            ),
            "last_built": self._last_built.isoformat() if self._last_built else None,
            "last_error": self._last_error,
        }

    async def _rebuild_locked(self) -> None:
        self._last_error = None
        try:
            chunks = load_knowledge_chunks(
                self._directory,
                chunk_size=self._settings.rag_chunk_size,
                chunk_overlap=self._settings.rag_chunk_overlap,
            )
        except Exception as exc:  # noqa: BLE001 - keep the service alive
            logger.exception("Failed to read knowledge documents")
            chunks = []
            self._last_error = f"读取知识文档失败: {exc}"

        if not self._directory.exists():
            self._last_error = f"知识目录不存在: {self._directory}"

        embedder = (
            EmbeddingClient(self._settings)
            if self._settings.embeddings_configured
            else None
        )
        store = KnowledgeStore(chunks, embedder)
        await store.build_index()

        self._store = store
        self._loaded = True
        self._last_built = datetime.now(timezone.utc)
        logger.info(
            "Knowledge base loaded: documents=%d chunks=%d mode=%s",
            len({chunk.source for chunk in chunks}),
            len(chunks),
            store.mode,
        )

    def _get_lock(self) -> asyncio.Lock:
        loop = asyncio.get_running_loop()
        lock = self._locks.get(id(loop))
        if lock is None:
            lock = asyncio.Lock()
            self._locks[id(loop)] = lock
        return lock

