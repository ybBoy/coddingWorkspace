from typing import Optional

from server.models.clothing import Clothing
from server.models.outfit_log import OutfitLog
from server.storage.wardrobe_store import get_store


class WardrobeService:
    def __init__(self):
        self.store = get_store()

    def get_clothes(
        self,
        type: Optional[str] = None,
        color: Optional[str] = None,
        season: Optional[str] = None,
    ) -> list[Clothing]:
        clothes = self.store.list_clothes()
        if type and type.lower() != "all":
            clothes = [c for c in clothes if c.type.lower() == type.lower()]
        if color and color.lower() != "all":
            clothes = [c for c in clothes if c.color.lower() == color.lower()]
        if season and season.lower() != "all":
            clothes = [c for c in clothes if c.season.lower() == season.lower()]
        clothes.sort(key=lambda c: c.created_at, reverse=True)
        return clothes

    def get_clothing(self, clothing_id: str) -> Optional[Clothing]:
        return self.store.get_clothing(clothing_id)

    def add_clothing(
        self,
        name: str,
        type: str,
        color: str,
        season: str,
        remark: str = "",
    ) -> Clothing:
        if not name or not type or not color or not season:
            raise ValueError("name, type, color, season are required")
        clothing = Clothing.create(name, type, color, season, remark)
        return self.store.add_clothing(clothing)

    def update_clothing(
        self,
        clothing_id: str,
        name: Optional[str] = None,
        type: Optional[str] = None,
        color: Optional[str] = None,
        season: Optional[str] = None,
        remark: Optional[str] = None,
    ) -> Optional[Clothing]:
        fields = {}
        if name is not None:
            fields["name"] = name.strip() if name else ""
        if type is not None:
            fields["type"] = type.strip() if type else ""
        if color is not None:
            fields["color"] = color.strip() if color else ""
        if season is not None:
            fields["season"] = season.strip() if season else ""
        if remark is not None:
            fields["remark"] = remark.strip() if remark else ""
        if not fields:
            return self.store.get_clothing(clothing_id)
        return self.store.update_clothing(clothing_id, **fields)

    def delete_clothing(self, clothing_id: str) -> bool:
        return self.store.delete_clothing(clothing_id)

    def record_outfit(self, clothing_ids: list[str], note: str = "") -> dict:
        if not clothing_ids:
            raise ValueError("clothing_ids cannot be empty")
        unique_ids = list(set(clothing_ids))

        invalid_ids = []
        for cid in unique_ids:
            if not self.store.get_clothing(cid):
                invalid_ids.append(cid)
        if invalid_ids:
            raise ValueError(f"Invalid clothing IDs: {', '.join(invalid_ids)}")

        updated = self.store.record_wear(unique_ids)
        log = OutfitLog.create(
            clothing_ids=[c.id for c in updated],
            note=note,
        )
        self.store.add_outfit_log(log)
        return {
            "outfit_log": log.to_dict(),
            "updated_clothes": [c.to_dict() for c in updated],
        }

    def get_outfit_logs(self, limit: int = 100) -> list[OutfitLog]:
        return self.store.list_outfit_logs(limit)

    def get_stats(self) -> dict:
        clothes = self.store.list_clothes()
        total = len(clothes)

        sorted_by_wear = sorted(clothes, key=lambda c: c.wear_count, reverse=True)
        top3 = sorted_by_wear[:3]

        return {
            "total_count": total,
            "top_worn": [
                {
                    "id": c.id,
                    "name": c.name,
                    "wear_count": c.wear_count,
                    "type": c.type,
                }
                for c in top3
            ],
        }

    def get_filters(self) -> dict:
        clothes = self.store.list_clothes()
        return {
            "types": sorted({c.type for c in clothes}),
            "colors": sorted({c.color for c in clothes}),
            "seasons": sorted({c.season for c in clothes}),
        }


_service_instance: Optional[WardrobeService] = None


def get_service() -> WardrobeService:
    global _service_instance
    if _service_instance is None:
        _service_instance = WardrobeService()
    return _service_instance
