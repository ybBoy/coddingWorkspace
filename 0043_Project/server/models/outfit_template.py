from dataclasses import dataclass, asdict, field
from datetime import datetime, timezone
from typing import List, Optional
import uuid


@dataclass
class OutfitTemplate:
    id: str
    name: str
    clothing_ids: List[str]
    note: str = ""
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    @staticmethod
    def create(name: str, clothing_ids: List[str], note: str = "") -> "OutfitTemplate":
        if not name or not name.strip():
            raise ValueError("模板名称不能为空")
        if not clothing_ids or len(clothing_ids) == 0:
            raise ValueError("请至少选择一件衣物")
        return OutfitTemplate(
            id=str(uuid.uuid4()),
            name=name.strip(),
            clothing_ids=[cid for cid in clothing_ids if cid],
            note=note.strip(),
        )

    def to_dict(self) -> dict:
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "OutfitTemplate":
        return OutfitTemplate(
            id=data["id"],
            name=data["name"],
            clothing_ids=list(data.get("clothing_ids", [])),
            note=data.get("note", ""),
            created_at=data.get("created_at", datetime.now(timezone.utc).isoformat()),
        )
