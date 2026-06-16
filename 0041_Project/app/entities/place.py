# place.py — 地点实体定义
# 负责定义 Place 数据模型，包含地点的所有字段
# 数据流：Place 对象在整个后端各层之间传递，最终序列化为 JSON 返回前端

from dataclasses import dataclass, field, asdict
from typing import Optional
import uuid


@dataclass
class Place:
    id: str = field(default_factory=lambda: uuid.uuid4().hex[:8])
    name: str = ""
    place_type: str = ""
    estimated_cost: float = 0.0
    transport: str = ""
    want_level: int = 3
    notes: str = ""

    def to_dict(self) -> dict:
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "Place":
        return Place(
            id=data.get("id", uuid.uuid4().hex[:8]),
            name=data.get("name", ""),
            place_type=data.get("place_type", ""),
            estimated_cost=float(data.get("estimated_cost", 0)),
            transport=data.get("transport", ""),
            want_level=int(data.get("want_level", 3)),
            notes=data.get("notes", ""),
        )
