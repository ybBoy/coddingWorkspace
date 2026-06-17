import random
import json
from typing import Optional, List

from server.models.clothing import Clothing
from server.models.outfit_log import OutfitLog
from server.models.outfit_template import OutfitTemplate
from server.storage.wardrobe_store import get_store


RECOMMEND_TYPES = ["上衣", "裤子", "鞋子"]


class WardrobeService:
    def __init__(self):
        self.store = get_store()

    def get_clothes(
        self,
        type: Optional[str] = None,
        color: Optional[str] = None,
        season: Optional[str] = None,
        tag: Optional[str] = None,
    ) -> list[Clothing]:
        clothes = self.store.list_clothes()
        if type and type.lower() != "all":
            clothes = [c for c in clothes if c.type.lower() == type.lower()]
        if color and color.lower() != "all":
            clothes = [c for c in clothes if c.color.lower() == color.lower()]
        if season and season.lower() != "all":
            clothes = [c for c in clothes if c.season.lower() == season.lower()]
        if tag and tag.lower() != "all":
            clothes = [c for c in clothes if tag.lower() in [t.lower() for t in c.tags]]
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
        image_url: str = "",
        tags: Optional[List[str]] = None,
    ) -> Clothing:
        if not name or not type or not color or not season:
            raise ValueError("name, type, color, season are required")
        clothing = Clothing.create(name, type, color, season, remark, image_url, tags)
        return self.store.add_clothing(clothing)

    def update_clothing(
        self,
        clothing_id: str,
        name: Optional[str] = None,
        type: Optional[str] = None,
        color: Optional[str] = None,
        season: Optional[str] = None,
        remark: Optional[str] = None,
        image_url: Optional[str] = None,
        tags: Optional[List[str]] = None,
        wear_count: Optional[int] = None,
        last_worn_at: Optional[str] = None,
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
        if image_url is not None:
            fields["image_url"] = image_url.strip() if image_url else ""
        if tags is not None:
            if not isinstance(tags, list):
                raise ValueError("tags must be a list")
            fields["tags"] = list(set(t.strip() for t in tags if isinstance(t, str) and t.strip()))
        if wear_count is not None:
            try:
                fields["wear_count"] = max(0, int(wear_count))
            except (ValueError, TypeError):
                raise ValueError("wear_count must be a non-negative integer")
        if last_worn_at is not None:
            if last_worn_at == "" or last_worn_at is None:
                fields["last_worn_at"] = None
            else:
                from datetime import datetime, timezone
                try:
                    dt = datetime.fromisoformat(last_worn_at.replace("Z", "+00:00"))
                    if dt.tzinfo is None:
                        dt = dt.replace(tzinfo=timezone.utc)
                    fields["last_worn_at"] = dt.isoformat()
                except (ValueError, TypeError):
                    raise ValueError("last_worn_at must be a valid ISO date string")
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

    def get_outfit_logs_with_details(self, limit: int = 100) -> list[dict]:
        logs = self.store.list_outfit_logs(limit)
        result = []
        for log in logs:
            log_dict = log.to_dict()
            items = []
            for cid in log.clothing_ids:
                c = self.store.get_clothing(cid)
                if c:
                    items.append({
                        "id": c.id,
                        "name": c.name,
                        "type": c.type,
                        "color": c.color,
                        "image_url": c.image_url,
                    })
            log_dict["clothes"] = items
            result.append(log_dict)
        return result

    def get_outfit_logs(self, limit: int = 100) -> list[OutfitLog]:
        return self.store.list_outfit_logs(limit)

    def get_calendar(self, year: int, month: int) -> dict:
        from datetime import date, timedelta

        if month < 1 or month > 12:
            raise ValueError("month must be between 1 and 12")

        first_day = date(year, month, 1)
        if month == 12:
            next_month = date(year + 1, 1, 1)
        else:
            next_month = date(year, month + 1, 1)
        last_day = next_month - timedelta(days=1)

        all_logs = self.store.list_outfit_logs(500)

        days = {}
        for log in all_logs:
            try:
                log_date_str = log.worn_at
                log_date = date.fromisoformat(log_date_str[:10])
            except (ValueError, TypeError):
                continue
            if first_day <= log_date <= last_day:
                day_key = log_date.isoformat()
                if day_key not in days:
                    days[day_key] = []
                clothes = []
                for cid in log.clothing_ids:
                    c = self.store.get_clothing(cid)
                    if c:
                        clothes.append({
                            "id": c.id,
                            "name": c.name,
                            "type": c.type,
                            "color": c.color,
                            "image_url": c.image_url,
                        })
                days[day_key].append({
                    "id": log.id,
                    "note": log.note,
                    "clothes": clothes,
                    "worn_at": log.worn_at,
                })

        total_days = last_day.day
        first_weekday = first_day.weekday()

        return {
            "year": year,
            "month": month,
            "first_weekday": first_weekday,
            "total_days": total_days,
            "logs_by_day": days,
        }

    def recommend_outfit(self, season: Optional[str] = None) -> dict:
        clothes = self.store.list_clothes()
        if season and season.lower() != "all":
            clothes = [c for c in clothes if c.season.lower() == season.lower()]

        recommendation = {}
        for rtype in RECOMMEND_TYPES:
            candidates = [c for c in clothes if c.type == rtype]
            if not candidates:
                recommendation[rtype] = None
                continue

            long_time = [c for c in candidates if c.is_long_time_no_wear()]
            pool = long_time if long_time else candidates
            chosen = random.choice(pool)
            recommendation[rtype] = {
                "id": chosen.id,
                "name": chosen.name,
                "type": chosen.type,
                "color": chosen.color,
                "image_url": chosen.image_url,
                "is_long_time_no_wear": chosen.is_long_time_no_wear(),
            }

        return {
            "season": season or "all",
            "items": recommendation,
            "tip": "优先推荐很久没穿的衣物，让每件衣服都有出场机会～",
        }

    def get_stats(self) -> dict:
        from datetime import datetime, timezone, timedelta

        clothes = self.store.list_clothes()
        total = len(clothes)

        sorted_by_wear = sorted(clothes, key=lambda c: c.wear_count, reverse=True)
        top3 = sorted_by_wear[:3]

        type_counts = {}
        for c in clothes:
            type_counts[c.type] = type_counts.get(c.type, 0) + 1

        color_counts = {}
        for c in clothes:
            color_counts[c.color] = color_counts.get(c.color, 0) + 1

        long_time_count = sum(1 for c in clothes if c.is_long_time_no_wear())

        logs = self.store.list_outfit_logs(200)
        recent_7_days = []
        today = datetime.now(timezone.utc).date()
        for i in range(6, -1, -1):
            day = today - timedelta(days=i)
            count = 0
            for log in logs:
                log_date = datetime.fromisoformat(
                    log.worn_at.replace("Z", "+00:00")
                    if log.worn_at.endswith("Z")
                    else log.worn_at
                ).date()
                if log_date == day:
                    count += 1
            recent_7_days.append({
                "date": day.isoformat(),
                "count": count,
            })

        return {
            "total": total,
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
            "type_counts": type_counts,
            "color_counts": color_counts,
            "long_time_no_wear_count": long_time_count,
            "recent_7_days": recent_7_days,
        }

    def get_filters(self) -> dict:
        clothes = self.store.list_clothes()
        all_tags = set()
        for c in clothes:
            all_tags.update(c.tags)
        return {
            "types": sorted({c.type for c in clothes}),
            "colors": sorted({c.color for c in clothes}),
            "seasons": sorted({c.season for c in clothes}),
            "tags": sorted(all_tags),
        }

    def list_templates(self) -> list[dict]:
        templates = self.store.list_templates()
        result = []
        for t in templates:
            t_dict = t.to_dict()
            clothes = []
            for cid in t.clothing_ids:
                c = self.store.get_clothing(cid)
                if c:
                    clothes.append({
                        "id": c.id,
                        "name": c.name,
                        "type": c.type,
                        "color": c.color,
                        "image_url": c.image_url,
                    })
            t_dict["clothes"] = clothes
            result.append(t_dict)
        return result

    def get_template(self, template_id: str) -> Optional[dict]:
        t = self.store.get_template(template_id)
        if not t:
            return None
        t_dict = t.to_dict()
        clothes = []
        for cid in t.clothing_ids:
            c = self.store.get_clothing(cid)
            if c:
                clothes.append({
                    "id": c.id,
                    "name": c.name,
                    "type": c.type,
                    "color": c.color,
                    "image_url": c.image_url,
                })
        t_dict["clothes"] = clothes
        return t_dict

    def add_template(self, name: str, clothing_ids: list[str], note: str = "") -> dict:
        template = OutfitTemplate.create(name, clothing_ids, note)

        invalid_ids = []
        for cid in template.clothing_ids:
            if not self.store.get_clothing(cid):
                invalid_ids.append(cid)
        if invalid_ids:
            raise ValueError(f"Invalid clothing IDs: {', '.join(invalid_ids)}")

        self.store.add_template(template)
        return self.get_template(template.id)

    def update_template(
        self,
        template_id: str,
        name: Optional[str] = None,
        clothing_ids: Optional[list[str]] = None,
        note: Optional[str] = None,
    ) -> Optional[dict]:
        t = self.store.get_template(template_id)
        if not t:
            return None

        fields = {}
        if name is not None:
            if not name.strip():
                raise ValueError("name cannot be empty")
            fields["name"] = name.strip()
        if clothing_ids is not None:
            if not clothing_ids:
                raise ValueError("clothing_ids cannot be empty")
            invalid_ids = [cid for cid in clothing_ids if not self.store.get_clothing(cid)]
            if invalid_ids:
                raise ValueError(f"Invalid clothing IDs: {', '.join(invalid_ids)}")
            fields["clothing_ids"] = list(clothing_ids)
        if note is not None:
            fields["note"] = note.strip() if note else ""

        if not fields:
            return self.get_template(template_id)

        self.store.update_template(template_id, **fields)
        return self.get_template(template_id)

    def delete_template(self, template_id: str) -> bool:
        return self.store.delete_template(template_id)

    def apply_template(self, template_id: str, note: str = "") -> dict:
        t = self.store.get_template(template_id)
        if not t:
            raise ValueError("Template not found")
        if not t.clothing_ids:
            raise ValueError("Template has no clothes")
        final_note = note if note else f"使用模板：{t.name}"
        return self.record_outfit(t.clothing_ids, final_note)

    def export_data(self) -> dict:
        data = self.store.export_all()
        return {
            "version": 2,
            "exported_at": __import__("datetime").datetime.now(__import__("datetime").timezone.utc).isoformat(),
            "clothes": data["clothes"],
            "outfit_logs": data["outfit_logs"],
            "templates": data["templates"],
        }

    def import_data(self, data: dict, merge: bool = True) -> dict:
        if not isinstance(data, dict):
            raise ValueError("Invalid data format")
        if "clothes" not in data or "outfit_logs" not in data:
            raise ValueError("Missing required fields: clothes, outfit_logs")

        imported_clothes = data.get("clothes", [])
        imported_logs = data.get("outfit_logs", [])
        imported_templates = data.get("templates", [])

        if not isinstance(imported_clothes, list) or not isinstance(imported_logs, list):
            raise ValueError("clothes and outfit_logs must be arrays")

        if not merge:
            self.store.clear_all()

        clothes_added = 0
        logs_added = 0
        templates_added = 0
        skipped_clothes = 0

        for item in imported_clothes:
            try:
                clothing = Clothing.from_dict(item)
                if merge and self.store.get_clothing(clothing.id):
                    skipped_clothes += 1
                    continue
                self.store.add_clothing(clothing)
                clothes_added += 1
            except (KeyError, ValueError, TypeError):
                skipped_clothes += 1

        for item in imported_logs:
            try:
                log = OutfitLog.from_dict(item)
                self.store.add_outfit_log(log)
                logs_added += 1
            except (KeyError, ValueError, TypeError):
                pass

        for item in imported_templates:
            try:
                tpl = OutfitTemplate.from_dict(item)
                if merge and self.store.get_template(tpl.id):
                    continue
                self.store.add_template(tpl)
                templates_added += 1
            except (KeyError, ValueError, TypeError):
                pass

        return {
            "clothes_added": clothes_added,
            "clothes_skipped": skipped_clothes,
            "logs_added": logs_added,
            "templates_added": templates_added,
            "total_clothes": len(self.store.list_clothes()),
            "total_logs": len(self.store.list_outfit_logs()),
            "total_templates": len(self.store.list_templates()),
        }


_service_instance: Optional[WardrobeService] = None


def get_service() -> WardrobeService:
    global _service_instance
    if _service_instance is None:
        _service_instance = WardrobeService()
    return _service_instance
