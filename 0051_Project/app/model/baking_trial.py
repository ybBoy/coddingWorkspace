from dataclasses import dataclass, asdict, field
from typing import Optional
from datetime import datetime


SUCCESS_LEVELS = ["失败", "一般", "良好", "优秀", "完美"]
DESSERT_TYPES = ["面包", "饼干", "蛋糕", "其他"]
SORT_FIELDS = ["created_at", "temperature", "duration_minutes", "success_level", "recipe_version"]
SORT_ORDERS = ["desc", "asc"]

SUCCESS_LEVEL_RANK = {
    "失败": 1,
    "一般": 2,
    "良好": 3,
    "优秀": 4,
    "完美": 5,
}

TEMP_MIN_COMMON = 100
TEMP_MAX_COMMON = 250
DURATION_MIN_COMMON = 5
DURATION_MAX_COMMON = 180


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
    taste_score: Optional[int] = None
    look_score: Optional[int] = None
    texture_score: Optional[int] = None
    image_data: Optional[str] = None

    def to_dict(self):
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "BakingTrial":
        return BakingTrial(
            id=data.get("id"),
            dessert_name=data.get("dessert_name", ""),
            dessert_type=data.get("dessert_type", "其他"),
            recipe_version=data.get("recipe_version", "v1"),
            temperature=int(data.get("temperature", 180)),
            duration_minutes=int(data.get("duration_minutes", 30)),
            success_level=data.get("success_level", "一般"),
            notes=data.get("notes", ""),
            created_at=data.get("created_at", datetime.now().isoformat()),
            taste_score=data.get("taste_score"),
            look_score=data.get("look_score"),
            texture_score=data.get("texture_score"),
            image_data=data.get("image_data"),
        )
