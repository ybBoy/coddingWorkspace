from dataclasses import dataclass, asdict
from typing import Optional
from datetime import datetime


SUCCESS_LEVELS = ["失败", "一般", "良好", "优秀", "完美"]
DESSERT_TYPES = ["面包", "饼干", "蛋糕", "其他"]


@dataclass
class BakingTrial:
    id: Optional[int]
    dessert_name: str
    dessert_type: str
    recipe_version: str
    temperature: int
    duration_minutes: int
    success_level: str
    notes: str
    created_at: str

    def to_dict(self):
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "BakingTrial":
        return BakingTrial(
            id=data.get("id"),
            dessert_name=data.get("dessert_name", ""),
            dessert_type=data.get("dessert_type", "其他"),
            recipe_version=data.get("recipe_version", "v1"),
            temperature=data.get("temperature", 180),
            duration_minutes=data.get("duration_minutes", 30),
            success_level=data.get("success_level", "一般"),
            notes=data.get("notes", ""),
            created_at=data.get("created_at", datetime.now().isoformat())
        )
