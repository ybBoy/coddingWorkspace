import json
import os
from typing import List, Optional
from server.models.exhibit_record import ExhibitRecord


class ExhibitRepository:
    def __init__(self, json_file_path: str):
        self._json_file_path = json_file_path
        self._records: List[ExhibitRecord] = []
        self._load_from_file()

    def _load_from_file(self) -> None:
        if os.path.exists(self._json_file_path):
            try:
                with open(self._json_file_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self._records = [ExhibitRecord.from_dict(item) for item in data]
            except (json.JSONDecodeError, KeyError, TypeError):
                self._records = []
        else:
            self._records = []

    def _save_to_file(self) -> None:
        os.makedirs(os.path.dirname(self._json_file_path), exist_ok=True)
        with open(self._json_file_path, "w", encoding="utf-8") as f:
            json.dump([r.to_dict() for r in self._records], f, ensure_ascii=False, indent=2)

    def get_all(self) -> List[ExhibitRecord]:
        return list(self._records)

    def get_by_id(self, record_id: str) -> Optional[ExhibitRecord]:
        for record in self._records:
            if record.id == record_id:
                return record
        return None

    def add(self, record: ExhibitRecord) -> ExhibitRecord:
        self._records.append(record)
        self._save_to_file()
        return record

    def update_rating(self, record_id: str, rating: int) -> Optional[ExhibitRecord]:
        record = self.get_by_id(record_id)
        if record:
            record.rating = rating
            self._save_to_file()
            return record
        return None

    def delete(self, record_id: str) -> bool:
        for i, record in enumerate(self._records):
            if record.id == record_id:
                self._records.pop(i)
                self._save_to_file()
                return True
        return False
