from dataclasses import dataclass, field, asdict
from typing import Optional
import uuid


@dataclass
class CampingGear:
    name: str
    category: str
    weight: float
    packed: bool = False
    essential: bool = False
    notes: str = ""
    id: str = field(default_factory=lambda: str(uuid.uuid4()))

    def to_dict(self):
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "CampingGear":
        return cls(
            id=data.get("id", str(uuid.uuid4())),
            name=data.get("name", ""),
            category=data.get("category", ""),
            weight=data.get("weight", 0.0),
            packed=data.get("packed", False),
            essential=data.get("essential", False),
            notes=data.get("notes", ""),
        )
