from dataclasses import dataclass, asdict, field
from datetime import datetime, timezone
from typing import Optional
import uuid


@dataclass
class Clothing:
    id: str
    name: str
    type: str
    color: str
    season: str
    remark: str = ""
    wear_count: int = 0
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    last_worn_at: Optional[str] = None

    @staticmethod
    def create(name: str, type: str, color: str, season: str, remark: str = "") -> "Clothing":
        return Clothing(
            id=str(uuid.uuid4()),
            name=name.strip(),
            type=type.strip(),
            color=color.strip(),
            season=season.strip(),
            remark=remark.strip(),
        )

    def to_dict(self) -> dict:
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "Clothing":
        return Clothing(
            id=data["id"],
            name=data["name"],
            type=data["type"],
            color=data["color"],
            season=data["season"],
            remark=data.get("remark", ""),
            wear_count=data.get("wear_count", 0),
            created_at=data.get("created_at", datetime.now(timezone.utc).isoformat()),
            last_worn_at=data.get("last_worn_at"),
        )

    def record_wear(self) -> None:
        self.wear_count += 1
        self.last_worn_at = datetime.now(timezone.utc).isoformat()

    def days_since_last_worn(self) -> Optional[int]:
        if not self.last_worn_at:
            return None
        try:
            last_worn = datetime.fromisoformat(self.last_worn_at)
            delta = datetime.now(timezone.utc) - last_worn
            return delta.days
        except (ValueError, TypeError):
            return None

    def is_long_time_no_wear(self, threshold_days: int = 60) -> bool:
        days = self.days_since_last_worn()
        return days is not None and days >= threshold_days
