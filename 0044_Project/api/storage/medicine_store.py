import json
import os
from typing import List, Optional
from pathlib import Path

from api.models.medicine import Medicine


class MedicineStore:
    def __init__(self, data_file: str = "data/medicines.json"):
        self.data_file = Path(data_file)
        self._medicines: dict[str, Medicine] = {}
        self._ensure_data_file()
        self._load_from_file()

    def _ensure_data_file(self):
        self.data_file.parent.mkdir(parents=True, exist_ok=True)
        if not self.data_file.exists():
            self.data_file.write_text(
                json.dumps({"medicines": []}, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )

    def _load_from_file(self):
        try:
            with open(self.data_file, "r", encoding="utf-8") as f:
                data = json.load(f)
            for item in data.get("medicines", []):
                medicine = Medicine.from_dict(item)
                self._medicines[medicine.id] = medicine
        except (json.JSONDecodeError, FileNotFoundError):
            self._medicines = {}

    def _save_to_file(self):
        medicines_list = [m.to_dict() for m in self._medicines.values()]
        with open(self.data_file, "w", encoding="utf-8") as f:
            json.dump({"medicines": medicines_list}, f, ensure_ascii=False, indent=2)

    def get_all(self) -> List[Medicine]:
        return list(self._medicines.values())

    def get_by_id(self, medicine_id: str) -> Optional[Medicine]:
        return self._medicines.get(medicine_id)

    def add(self, medicine: Medicine) -> Medicine:
        self._medicines[medicine.id] = medicine
        self._save_to_file()
        return medicine

    def update(self, medicine_id: str, data: dict) -> Optional[Medicine]:
        medicine = self._medicines.get(medicine_id)
        if not medicine:
            return None
        for key, value in data.items():
            if hasattr(medicine, key) and value is not None:
                setattr(medicine, key, value)
        self._save_to_file()
        return medicine

    def delete(self, medicine_id: str) -> bool:
        if medicine_id in self._medicines:
            del self._medicines[medicine_id]
            self._save_to_file()
            return True
        return False

    def use_medicine(self, medicine_id: str, amount: int) -> Optional[Medicine]:
        medicine = self._medicines.get(medicine_id)
        if not medicine:
            return None
        if medicine.quantity < amount:
            raise ValueError("库存不足，无法使用")
        medicine.quantity -= amount
        self._save_to_file()
        return medicine

    def replenish_medicine(self, medicine_id: str, amount: int) -> Optional[Medicine]:
        medicine = self._medicines.get(medicine_id)
        if not medicine:
            return None
        medicine.quantity += amount
        self._save_to_file()
        return medicine


medicine_store = MedicineStore()
