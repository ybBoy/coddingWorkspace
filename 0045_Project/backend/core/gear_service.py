from typing import List, Optional
from backend.model.camping_gear import CampingGear
from backend.repository.gear_json_repository import GearJsonRepository


class GearService:
    def __init__(self, repository: GearJsonRepository):
        self._repo = repository

    def list_gears(self, category: Optional[str] = None) -> List[CampingGear]:
        if category and category != "全部":
            return self._repo.find_by_category(category)
        return self._repo.find_all()

    def get_gear(self, gear_id: str) -> Optional[CampingGear]:
        return self._repo.find_by_id(gear_id)

    def add_gear(self, name: str, category: str, weight: float,
                 essential: bool = False, notes: str = "") -> CampingGear:
        gear = CampingGear(
            name=name,
            category=category,
            weight=weight,
            essential=essential,
            notes=notes,
        )
        return self._repo.add(gear)

    def toggle_packed(self, gear_id: str) -> Optional[CampingGear]:
        gear = self._repo.find_by_id(gear_id)
        if gear is None:
            return None
        return self._repo.update(gear_id, packed=not gear.packed)

    def toggle_essential(self, gear_id: str) -> Optional[CampingGear]:
        gear = self._repo.find_by_id(gear_id)
        if gear is None:
            return None
        return self._repo.update(gear_id, essential=not gear.essential)

    def delete_gear(self, gear_id: str) -> bool:
        return self._repo.delete(gear_id)

    def get_categories(self) -> List[str]:
        return self._repo.get_all_categories()

    def get_stats(self) -> dict:
        gears = self._repo.find_all()
        total_weight = sum(g.weight for g in gears)
        packed_count = sum(1 for g in gears if g.packed)
        unpacked_essential_count = sum(
            1 for g in gears if g.essential and not g.packed
        )
        return {
            "total_weight": round(total_weight, 2),
            "total_count": len(gears),
            "packed_count": packed_count,
            "unpacked_essential_count": unpacked_essential_count,
        }
