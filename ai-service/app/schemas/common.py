from typing import Any, Optional

from pydantic import BaseModel


class ApiResult(BaseModel):
    code: int
    message: str
    data: Optional[Any] = None


def success(data: Any = None) -> dict:
    return ApiResult(code=200, message="操作成功", data=data).model_dump()
