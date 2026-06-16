# place_manager.py — 地点管理核心逻辑
# 负责在内存中维护地点列表，提供增删改查、筛选、排序和统计功能
# 数据流：place_api 收到请求 → place_manager 操作内存数据 → place_store 持久化到 JSON

from typing import List, Optional, Dict
from app.entities.place import Place
from app.files.place_store import load_places, save_places


class PlaceManager:
    def __init__(self):
        self._places: List[Place] = load_places()

    def _persist(self):
        save_places(self._places)

    def add_place(self, data: dict) -> Place:
        place = Place.from_dict(data)
        self._places.append(place)
        self._persist()
        return place

    def list_places(
        self,
        place_type: Optional[str] = None,
        sort_by_cost: bool = False,
        keyword: Optional[str] = None,
        max_cost: Optional[float] = None,
    ) -> List[Place]:
        result = list(self._places)
        if place_type:
            result = [p for p in result if p.place_type == place_type]
        if keyword:
            kw = keyword.lower()
            result = [
                p for p in result
                if kw in p.name.lower()
                or kw in p.notes.lower()
                or kw in p.place_type.lower()
            ]
        if max_cost is not None:
            result = [p for p in result if p.estimated_cost <= max_cost]
        if sort_by_cost:
            result.sort(key=lambda p: p.estimated_cost)
        return result

    def update_place(self, place_id: str, data: dict) -> Optional[Place]:
        for place in self._places:
            if place.id == place_id:
                if "name" in data:
                    place.name = data["name"]
                if "place_type" in data:
                    place.place_type = data["place_type"]
                if "estimated_cost" in data:
                    place.estimated_cost = float(data["estimated_cost"])
                if "transport" in data:
                    place.transport = data["transport"]
                if "want_level" in data:
                    place.want_level = int(data["want_level"])
                if "notes" in data:
                    place.notes = data["notes"]
                if "plan_date" in data:
                    place.plan_date = data["plan_date"]
                if "visited" in data:
                    place.visited = bool(data["visited"])
                self._persist()
                return place
        return None

    def toggle_visited(self, place_id: str) -> Optional[Place]:
        for place in self._places:
            if place.id == place_id:
                place.visited = not place.visited
                self._persist()
                return place
        return None

    def delete_place(self, place_id: str) -> bool:
        before = len(self._places)
        self._places = [p for p in self._places if p.id != place_id]
        if len(self._places) < before:
            self._persist()
            return True
        return False

    def get_stats(
        self,
        place_type: Optional[str] = None,
        keyword: Optional[str] = None,
        max_cost: Optional[float] = None,
    ) -> Dict:
        places = self.list_places(
            place_type=place_type, keyword=keyword, max_cost=max_cost
        )
        all_places = self._places

        if all_places:
            avg_cost = round(sum(p.estimated_cost for p in all_places) / len(all_places), 2)
        else:
            avg_cost = 0.0

        sorted_by_want = sorted(all_places, key=lambda p: p.want_level, reverse=True)
        top3_count = len(sorted_by_want[:3])

        return {
            "avg_cost": avg_cost,
            "top3_count": top3_count,
            "filtered_count": len(places),
            "total_count": len(all_places),
        }

    def get_weekend_recommend(self) -> List[Place]:
        candidates = [p for p in self._places if not p.visited]
        candidates.sort(key=lambda p: (-p.want_level, p.estimated_cost))
        return candidates[:3]
