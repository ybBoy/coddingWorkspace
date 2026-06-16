"""
数据模型层：定义灵感卡片的数据结构
每个灵感包含：标题、内容、标签、来源、收藏状态、创建时间等
"""
import uuid
from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import List


@dataclass
class Idea:
    """灵感卡片数据模型"""
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    title: str = ""
    content: str = ""
    tags: List[str] = field(default_factory=list)
    source: str = ""
    is_favorite: bool = False
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())
    updated_at: str = field(default_factory=lambda: datetime.now().isoformat())

    def to_dict(self) -> dict:
        """转换为字典，方便序列化为 JSON"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "Idea":
        """从字典创建 Idea 对象"""
        return cls(**{k: v for k, v in data.items() if k in cls.__dataclass_fields__})
