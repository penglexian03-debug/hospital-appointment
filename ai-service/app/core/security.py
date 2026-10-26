from dataclasses import dataclass
from typing import Optional

from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.clients.java_api import JavaApiClient, JavaApiError
from app.core.config import Settings, get_settings
from app.schemas.user import UserProfile


@dataclass(frozen=True)
class AuthenticatedUser:
    token: str
    profile: UserProfile


bearer_scheme = HTTPBearer(auto_error=False)


async def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(bearer_scheme),
    settings: Settings = Depends(get_settings),
) -> AuthenticatedUser:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=401,
            detail="请先登录",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = credentials.credentials.strip()
    if not token:
        raise HTTPException(
            status_code=401,
            detail="请先登录",
            headers={"WWW-Authenticate": "Bearer"},
        )

    client = JavaApiClient(settings)
    try:
        profile = await client.get_current_user(token)
    except JavaApiError as exc:
        if exc.status_code in (401, 403):
            raise HTTPException(
                status_code=exc.status_code,
                detail=exc.message,
                headers={"WWW-Authenticate": "Bearer"},
            ) from exc
        raise HTTPException(
            status_code=503,
            detail="认证服务暂时不可用",
        ) from exc

    return AuthenticatedUser(token=token, profile=profile)


async def require_patient(
    current_user: AuthenticatedUser = Depends(get_current_user),
) -> AuthenticatedUser:
    if current_user.profile.role != 1:
        raise HTTPException(status_code=403, detail="AI 助手仅对患者开放")
    return current_user


async def require_admin(
    current_user: AuthenticatedUser = Depends(get_current_user),
) -> AuthenticatedUser:
    if current_user.profile.role != 2:
        raise HTTPException(status_code=403, detail="仅管理员可执行该操作")
    return current_user
