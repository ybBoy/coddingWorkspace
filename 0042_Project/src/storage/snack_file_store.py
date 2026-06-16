"""
文件职责：
    文件存储层，负责将零食数据持久化到本地 JSON 文件。
    服务启动时从 JSON 文件读取数据恢复内存库存，
    每次内存数据变更后将最新状态写回 JSON 文件。

数据流：
    读取：snacks.json（磁盘） → 解析为字典列表 → 转换为 Snack 对象列表 → 交给 snack_service
    写入：snack_service 传入 Snack 对象列表 → 转换为字典列表 → 写入 snacks.json（磁盘）
"""

import json
import os
from typing import List

from src.model.snack import Snack


class SnackFileStore:
    """零食文件存储器，基于本地 JSON 文件实现持久化"""

    def __init__(self, file_path: str = None):
        """
        初始化文件存储器
        :param file_path: JSON 文件路径，默认使用项目根目录下的 data/snacks.json
        """
        if file_path is None:
            project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
            file_path = os.path.join(project_root, "data", "snacks.json")
        self.file_path = file_path
        self._ensure_file_exists()

    def _ensure_file_exists(self) -> None:
        """确保存储文件和所在目录存在，不存在则创建空文件"""
        directory = os.path.dirname(self.file_path)
        if directory and not os.path.exists(directory):
            os.makedirs(directory, exist_ok=True)
        if not os.path.exists(self.file_path):
            with open(self.file_path, "w", encoding="utf-8") as f:
                json.dump([], f, ensure_ascii=False, indent=2)

    def load_all(self) -> List[Snack]:
        """
        从 JSON 文件读取所有零食数据
        :return: Snack 对象列表
        """
        try:
            with open(self.file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            return [Snack.from_dict(item) for item in data]
        except (json.JSONDecodeError, FileNotFoundError, IOError):
            return []

    def save_all(self, snacks: List[Snack]) -> None:
        """
        将所有零食数据写入 JSON 文件
        :param snacks: Snack 对象列表
        """
        data = [snack.to_dict() for snack in snacks]
        with open(self.file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
