from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class UserProfile(BaseModel):
    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: int = Field(gt=0)
    username: str
    real_name: str = Field(alias="realName")
    role: int
    phone: Optional[str] = None
