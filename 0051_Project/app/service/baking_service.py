import re
from typing import List, Optional, Dict, Any
from datetime import datetime
from app.model.baking_trial import (
    BakingTrial, SUCCESS_LEVELS, SUCCESS_LEVEL_RANK,
    SORT_FIELDS, SORT_ORDERS,
    TEMP_MIN_COMMON, TEMP_MAX_COMMON,
    DURATION_MIN_COMMON, DURATION_MAX_COMMON,
)
from app.storage.baking_store import BakingStore


class BakingService:
    def __init__(self, store: BakingStore):
        self.store = store

    def _parse_int_optional(self, val):
        if val is None or val == "":
            return None
        try:
            return int(val)
        except (ValueError, TypeError):
            return None

    def _sort_key(self, trial: BakingTrial, sort_by: str):
        if sort_by == "success_level":
            return SUCCESS_LEVEL_RANK.get(trial.success_level, 0)
        if sort_by == "recipe_version":
            m = re.search(r"(\d+)", trial.recipe_version or "")
            return (int(m.group(1)) if m else 0, trial.recipe_version or "")
        if sort_by == "temperature":
            return trial.temperature
        if sort_by == "duration_minutes":
            return trial.duration_minutes
        return trial.created_at

    def get_trials(
        self,
        dessert_type: Optional[str] = None,
        success_level: Optional[str] = None,
        search: Optional[str] = None,
        sort_by: str = "created_at",
        sort_order: str = "desc",
    ) -> List[BakingTrial]:
        trials = self.store.get_all()

        if dessert_type and dessert_type != "全部":
            trials = [t for t in trials if t.dessert_type == dessert_type]

        if success_level and success_level != "全部":
            trials = [t for t in trials if t.success_level == success_level]

        if search:
            search_lower = search.lower()
            trials = [
                t for t in trials
                if search_lower in t.dessert_name.lower()
            ]

        if sort_by not in SORT_FIELDS:
            sort_by = "created_at"
        if sort_order not in SORT_ORDERS:
            sort_order = "desc"
        reverse = sort_order == "desc"
        trials.sort(key=lambda t: self._sort_key(t, sort_by), reverse=reverse)
        return trials

    def get_trial_by_id(self, trial_id: int) -> Optional[BakingTrial]:
        return self.store.get_by_id(trial_id)

    def _clean_payload(self, data: dict) -> dict:
        cleaned = dict(data)
        for k in ["temperature", "duration_minutes"]:
            if k in cleaned:
                try:
                    cleaned[k] = int(cleaned[k])
                except (ValueError, TypeError):
                    pass
        for k in ["taste_score", "look_score", "texture_score"]:
            cleaned[k] = self._parse_int_optional(cleaned.get(k))
        return cleaned

    def create_trial(self, data: dict) -> BakingTrial:
        cleaned = self._clean_payload(data)
        trial = BakingTrial.from_dict(cleaned)
        trial.created_at = datetime.now().isoformat()
        if trial.success_level not in SUCCESS_LEVELS:
            trial.success_level = "一般"
        return self.store.add(trial)

    def update_trial(self, trial_id: int, data: dict) -> Optional[BakingTrial]:
        trial = self.store.get_by_id(trial_id)
        if trial is None:
            return None
        cleaned = self._clean_payload(data)
        allowed = [
            "dessert_name", "dessert_type", "recipe_version",
            "temperature", "duration_minutes", "success_level",
            "notes", "taste_score", "look_score", "texture_score", "image_data",
        ]
        updates = {k: v for k, v in cleaned.items() if k in allowed}
        if "success_level" in updates and updates["success_level"] not in SUCCESS_LEVELS:
            updates["success_level"] = "一般"
        return self.store.update(trial_id, updates)

    def update_success_level(self, trial_id: int, success_level: str) -> Optional[BakingTrial]:
        if success_level not in SUCCESS_LEVELS:
            success_level = "一般"
        return self.store.update(trial_id, {"success_level": success_level})

    def delete_trial(self, trial_id: int) -> bool:
        return self.store.delete(trial_id)

    def get_statistics(self, filtered_count: Optional[int] = None) -> dict:
        all_trials = self.store.get_all()
        total = len(all_trials)
        good_levels = {"良好", "优秀", "完美"}
        success_count = sum(1 for t in all_trials if t.success_level in good_levels)
        success_rate = round((success_count / total * 100), 1) if total > 0 else 0.0
        return {
            "total": total,
            "success_rate": success_rate,
            "filtered_count": filtered_count if filtered_count is not None else total,
        }

    def get_version_comparison(self, dessert_name: Optional[str] = None) -> List[Dict[str, Any]]:
        trials = self.store.get_all()
        grouped: Dict[str, List[BakingTrial]] = {}
        for t in trials:
            key = t.dessert_name.strip()
            if not key:
                continue
            grouped.setdefault(key, []).append(t)

        results = []
        for name, items in grouped.items():
            if dessert_name and dessert_name.lower() not in name.lower():
                continue
            items.sort(key=lambda t: self._sort_key(t, "recipe_version"), reverse=False)
            versions = []
            for t in items:
                avg_score = None
                scores = [t.taste_score, t.look_score, t.texture_score]
                valid = [s for s in scores if isinstance(s, int)]
                if valid:
                    avg_score = round(sum(valid) / len(valid), 1)
                versions.append({
                    "id": t.id,
                    "version": t.recipe_version,
                    "temperature": t.temperature,
                    "duration_minutes": t.duration_minutes,
                    "success_level": t.success_level,
                    "success_rank": SUCCESS_LEVEL_RANK.get(t.success_level, 0),
                    "taste_score": t.taste_score,
                    "look_score": t.look_score,
                    "texture_score": t.texture_score,
                    "avg_score": avg_score,
                    "notes": t.notes,
                    "created_at": t.created_at,
                })
            if len(versions) >= 2:
                results.append({
                    "dessert_name": name,
                    "dessert_type": items[0].dessert_type,
                    "version_count": len(versions),
                    "versions": versions,
                })
        results.sort(key=lambda r: (r["version_count"], r["dessert_name"]), reverse=True)
        return results

    def validate_trial(self, data: dict, exclude_id: Optional[int] = None) -> List[Dict[str, str]]:
        warnings = []
        name = (data.get("dessert_name") or "").strip()
        version = (data.get("recipe_version") or "").strip() or "v1"
        temp = self._parse_int_optional(data.get("temperature"))
        dur = self._parse_int_optional(data.get("duration_minutes"))

        if name:
            existing = [
                t for t in self.store.get_all()
                if t.dessert_name.strip() == name
                and t.recipe_version.strip() == version
                and (exclude_id is None or t.id != exclude_id)
            ]
            if existing:
                warnings.append({
                    "level": "warn",
                    "code": "duplicate",
                    "message": f"「{name}」的「{version}」版本已存在，是否重复录入？",
                })

        if temp is not None:
            if temp < TEMP_MIN_COMMON:
                warnings.append({
                    "level": "info",
                    "code": "temp_low",
                    "message": f"温度 {temp}°C 低于常见范围 ({TEMP_MIN_COMMON}-{TEMP_MAX_COMMON}°C)，请确认",
                })
            elif temp > TEMP_MAX_COMMON:
                warnings.append({
                    "level": "warn",
                    "code": "temp_high",
                    "message": f"温度 {temp}°C 高于常见范围 ({TEMP_MIN_COMMON}-{TEMP_MAX_COMMON}°C)，请注意防焦",
                })

        if dur is not None:
            if dur < DURATION_MIN_COMMON:
                warnings.append({
                    "level": "info",
                    "code": "dur_short",
                    "message": f"时间 {dur} 分钟短于常见范围 ({DURATION_MIN_COMMON}-{DURATION_MAX_COMMON} 分钟)，请确认",
                })
            elif dur > DURATION_MAX_COMMON:
                warnings.append({
                    "level": "warn",
                    "code": "dur_long",
                    "message": f"时间 {dur} 分钟长于常见范围 ({DURATION_MIN_COMMON}-{DURATION_MAX_COMMON} 分钟)，可能偏干",
                })

        for k in ["taste_score", "look_score", "texture_score"]:
            v = self._parse_int_optional(data.get(k))
            if v is not None and (v < 1 or v > 10):
                warnings.append({
                    "level": "warn",
                    "code": f"{k}_range",
                    "message": f"{k} 建议填写 1-10 分",
                })

        return warnings

    def import_trials(self, items: List[dict]) -> Dict[str, Any]:
        imported = 0
        errors = []
        for i, raw in enumerate(items):
            try:
                cleaned = self._clean_payload(raw)
                required = ["dessert_name", "dessert_type", "temperature", "duration_minutes"]
                for f in required:
                    if f not in cleaned or cleaned[f] in (None, ""):
                        raise ValueError(f"缺少字段: {f}")
                trial = BakingTrial.from_dict(cleaned)
                trial.created_at = cleaned.get("created_at") or datetime.now().isoformat()
                if trial.success_level not in SUCCESS_LEVELS:
                    trial.success_level = "一般"
                self.store.add(trial)
                imported += 1
            except Exception as e:
                errors.append({"row": i + 1, "error": str(e)})
        return {"imported": imported, "errors": errors}
