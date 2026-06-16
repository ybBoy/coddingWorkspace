"""
文件存储层：负责将灵感数据持久化到本地 JSON 文件
服务启动时从 JSON 加载到内存，数据变更时写回 JSON 文件
"""
import json
import os
from typing import List

from backend.models.idea import Idea


class JsonStore:
    """JSON 文件存储管理器"""

    def __init__(self, file_path: str = "data/ideas.json"):
        self.file_path = file_path
        self._ensure_dir()

    def _ensure_dir(self) -> None:
        """确保数据文件所在目录存在"""
        dir_path = os.path.dirname(self.file_path)
        if dir_path and not os.path.exists(dir_path):
            os.makedirs(dir_path, exist_ok=True)

    def load_all(self) -> List[Idea]:
        """从 JSON 文件加载所有灵感"""
        if not os.path.exists(self.file_path):
            return []
        try:
            with open(self.file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            return [Idea.from_dict(item) for item in data]
        except (json.JSONDecodeError, KeyError, TypeError):
            return []

    def save_all(self, ideas: List[Idea]) -> None:
        """将所有灵感保存到 JSON 文件"""
        data = [idea.to_dict() for idea in ideas]
        with open(self.file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
