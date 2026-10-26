import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.chat import router
from app.api.confirm import router as confirm_router
from app.api.deps import get_knowledge_service
from app.api.knowledge import router as knowledge_router
from app.core.config import get_settings
from app.core.logging import configure_logging
from app.schemas.common import ApiResult


settings = get_settings()
configure_logging(settings.log_level)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_: FastAPI):
    knowledge = get_knowledge_service()
    try:
        await knowledge.ensure_loaded()
    except Exception:  # noqa: BLE001 - the service must still start
        logger.exception("Failed to initialise the knowledge base")
    yield

app = FastAPI(
    title="Hospital AI Service",
    version="0.1.0",
    description="FastAPI service for the hospital appointment assistant.",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["GET", "POST", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

app.include_router(router)
app.include_router(confirm_router)
app.include_router(knowledge_router)


@app.exception_handler(HTTPException)
async def handle_http_exception(_: Request, exc: HTTPException) -> JSONResponse:
    payload = ApiResult(code=exc.status_code, message=str(exc.detail), data=None)
    return JSONResponse(status_code=exc.status_code, content=payload.model_dump())


@app.exception_handler(RequestValidationError)
async def handle_validation_exception(_: Request, exc: RequestValidationError) -> JSONResponse:
    errors = exc.errors()
    message = errors[0].get("msg", "请求参数错误") if errors else "请求参数错误"
    payload = ApiResult(code=400, message=message, data=None)
    return JSONResponse(status_code=400, content=payload.model_dump())


@app.exception_handler(Exception)
async def handle_unexpected_exception(_: Request, exc: Exception) -> JSONResponse:
    logger.exception("Unhandled AI service exception")
    payload = ApiResult(code=500, message="服务器内部错误", data=None)
    return JSONResponse(status_code=500, content=payload.model_dump())
