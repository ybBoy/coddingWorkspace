# place_store.py — JSON 文件持久化层
# 负责将地点数据写入本地 JSON 文件，以及从 JSON 文件读取恢复数据
# 数据流：place_manager 更新内存数据后调用 place_store 保存 → JSON 文件
#        服务启动时 place_manager 调用 place_store 加载 → 内存数据恢复

import json
import os
from typing import List
from app.entities.place import Place

_DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "files")
_JSON_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "places.json")


def load_places() -> List[Place]:
    if not os.path.exists(_JSON_PATH):
        return []
    try:
        with open(_JSON_PATH, "r", encoding="utf-8") as f:
            raw = json.load(f)
        return [Place.from_dict(item) for item in raw]
    except (json.JSONDecodeError, OSError):
        return []


def save_places(places: List[Place]) -> None:
    os.makedirs(os.path.dirname(_JSON_PATH), exist_ok=True)
    data = [p.to_dict() for p in places]
    with open(_JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
