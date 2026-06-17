import json
import os
import threading
from typing import Optional
from pathlib import Path

from server.models.clothing import Clothing
from server.models.outfit_log import OutfitLog
from server.models.outfit_template import OutfitTemplate


BASE_DIR = Path(__file__).resolve().parent.parent.parent
DATA_FILE = BASE_DIR / "data" / "wardrobe.json"


class WardrobeStore:
    def __init__(self, data_file: Path = DATA_FILE):
        self.data_file = data_file
        self._lock = threading.RLock()
        self._clothes: dict[str, Clothing] = {}
        self._outfit_logs: dict[str, OutfitLog] = {}
        self._templates: dict[str, OutfitTemplate] = {}
        self._load()

    def _load(self) -> None:
        with self._lock:
            if not self.data_file.exists():
                self._save_to_disk()
                return
            try:
                with open(self.data_file, "r", encoding="utf-8") as f:
                    raw = json.load(f)
                self._clothes = {
                    item["id"]: Clothing.from_dict(item)
                    for item in raw.get("clothes", [])
                }
                self._outfit_logs = {
                    item["id"]: OutfitLog.from_dict(item)
                    for item in raw.get("outfit_logs", [])
                }
                self._templates = {
                    item["id"]: OutfitTemplate.from_dict(item)
                    for item in raw.get("templates", [])
                }
            except (json.JSONDecodeError, KeyError, TypeError):
                self._clothes = {}
                self._outfit_logs = {}
                self._templates = {}
                self._save_to_disk()

    def _save_to_disk(self) -> None:
        self.data_file.parent.mkdir(parents=True, exist_ok=True)
        data = {
            "clothes": [c.to_dict() for c in self._clothes.values()],
            "outfit_logs": [l.to_dict() for l in self._outfit_logs.values()],
            "templates": [t.to_dict() for t in self._templates.values()],
        }
        with open(self.data_file, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def _persist(self) -> None:
        try:
            self._save_to_disk()
        except OSError as e:
            raise RuntimeError(f"Failed to save data to disk: {e}") from e

    def list_clothes(self) -> list[Clothing]:
        with self._lock:
            return list(self._clothes.values())

    def get_clothing(self, clothing_id: str) -> Optional[Clothing]:
        with self._lock:
            return self._clothes.get(clothing_id)

    def add_clothing(self, clothing: Clothing) -> Clothing:
        with self._lock:
            self._clothes[clothing.id] = clothing
            self._persist()
            return clothing

    def update_clothing(self, clothing_id: str, **fields) -> Optional[Clothing]:
        with self._lock:
            clothing = self._clothes.get(clothing_id)
            if not clothing:
                return None
            for key, value in fields.items():
                if hasattr(clothing, key):
                    setattr(clothing, key, value)
            self._persist()
            return clothing

    def delete_clothing(self, clothing_id: str) -> bool:
        with self._lock:
            if clothing_id not in self._clothes:
                return False
            del self._clothes[clothing_id]
            for template in self._templates.values():
                if clothing_id in template.clothing_ids:
                    template.clothing_ids = [
                        cid for cid in template.clothing_ids if cid != clothing_id
                    ]
            self._persist()
            return True

    def list_outfit_logs(self, limit: int = 100) -> list[OutfitLog]:
        with self._lock:
            logs = list(self._outfit_logs.values())
            logs.sort(key=lambda x: x.worn_at, reverse=True)
            return logs[:limit]

    def add_outfit_log(self, log: OutfitLog) -> OutfitLog:
        with self._lock:
            self._outfit_logs[log.id] = log
            self._persist()
            return log

    def record_wear(self, clothing_ids: list[str]) -> list[Clothing]:
        with self._lock:
            updated = []
            for cid in clothing_ids:
                c = self._clothes.get(cid)
                if c:
                    c.record_wear()
                    updated.append(c)
            self._persist()
            return updated

    def list_templates(self) -> list[OutfitTemplate]:
        with self._lock:
            templates = list(self._templates.values())
            templates.sort(key=lambda x: x.created_at, reverse=True)
            return templates

    def get_template(self, template_id: str) -> Optional[OutfitTemplate]:
        with self._lock:
            return self._templates.get(template_id)

    def add_template(self, template: OutfitTemplate) -> OutfitTemplate:
        with self._lock:
            self._templates[template.id] = template
            self._persist()
            return template

    def update_template(self, template_id: str, **fields) -> Optional[OutfitTemplate]:
        with self._lock:
            template = self._templates.get(template_id)
            if not template:
                return None
            for key, value in fields.items():
                if hasattr(template, key):
                    setattr(template, key, value)
            self._persist()
            return template

    def delete_template(self, template_id: str) -> bool:
        with self._lock:
            if template_id not in self._templates:
                return False
            del self._templates[template_id]
            self._persist()
            return True

    def clear_all(self) -> None:
        with self._lock:
            self._clothes.clear()
            self._outfit_logs.clear()
            self._templates.clear()
            self._persist()

    def export_all(self) -> dict:
        with self._lock:
            return {
                "clothes": [c.to_dict() for c in self._clothes.values()],
                "outfit_logs": [l.to_dict() for l in self._outfit_logs.values()],
                "templates": [t.to_dict() for t in self._templates.values()],
            }

    def import_templates(self, templates_data: list[dict], merge: bool = True) -> int:
        with self._lock:
            count = 0
            if not merge:
                self._templates.clear()
            for item in templates_data:
                try:
                    tpl = OutfitTemplate.from_dict(item)
                    self._templates[tpl.id] = tpl
                    count += 1
                except (KeyError, TypeError):
                    continue
            self._persist()
            return count


_store_instance: Optional[WardrobeStore] = None


def get_store() -> WardrobeStore:
    global _store_instance
    if _store_instance is None:
        _store_instance = WardrobeStore()
    return _store_instance
