import json
import os
from typing import List, Optional
from backend.model.camping_gear import CampingGear


class GearJsonRepository:
    def __init__(self, file_path: str):
        self._file_path = file_path
        self._gears: dict[str, CampingGear] = {}
        self._load()

    def _load(self):
        if os.path.exists(self._file_path):
            with open(self._file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                for item in data:
                    gear = CampingGear.from_dict(item)
                    self._gears[gear.id] = gear

    def _save(self):
        os.makedirs(os.path.dirname(self._file_path), exist_ok=True)
        data = [gear.to_dict() for gear in self._gears.values()]
        with open(self._file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def find_all(self) -> List[CampingGear]:
        return list(self._gears.values())

    def find_by_id(self, gear_id: str) -> Optional[CampingGear]:
        return self._gears.get(gear_id)

    def find_by_category(self, category: str) -> List[CampingGear]:
        return [g for g in self._gears.values() if g.category == category]

    def add(self, gear: CampingGear) -> CampingGear:
        self._gears[gear.id] = gear
        self._save()
        return gear

    def update(self, gear_id: str, **kwargs) -> Optional[CampingGear]:
        gear = self._gears.get(gear_id)
        if gear is None:
            return None
        for key, value in kwargs.items():
            if hasattr(gear, key):
                setattr(gear, key, value)
        self._save()
        return gear

    def delete(self, gear_id: str) -> bool:
        if gear_id in self._gears:
            del self._gears[gear_id]
            self._save()
            return True
        return False

    def get_all_categories(self) -> List[str]:
        categories = set(g.category for g in self._gears.values() if g.category)
        return sorted(categories)
