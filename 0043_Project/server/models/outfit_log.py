from dataclasses import dataclass, asdict, field
from datetime import datetime, timezone
import uuid


@dataclass
class OutfitLog:
    id: str
    clothing_ids: list[str]
    worn_at: str
    note: str = ""

    @staticmethod
    def create(clothing_ids: list[str], note: str = "") -> "OutfitLog":
        return OutfitLog(
            id=str(uuid.uuid4()),
            clothing_ids=sorted(set(clothing_ids)),
            worn_at=datetime.now(timezone.utc).isoformat(),
            note=note.strip(),
        )

    def to_dict(self) -> dict:
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "OutfitLog":
        return OutfitLog(
            id=data["id"],
            clothing_ids=data["clothing_ids"],
            worn_at=data["worn_at"],
            note=data.get("note", ""),
        )
