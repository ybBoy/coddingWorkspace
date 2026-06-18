"""
candle_json_store.py - JSON 文件存储层
文件职责：负责蜡烛数据的持久化存储和读取。
将内存中的蜡烛列表保存到本地 JSON 文件，服务重启时从文件加载恢复数据。
使用线程锁保证文件操作的线程安全。

存储流程：
  candle_service 更新内存数据 → 调用 save_all() → 写入 JSON 文件
加载流程：
  服务启动 → 调用 load_all() → 从 JSON 文件读取 → 返回 Candle 对象列表
"""

import json
import os
import threading
from typing import List
from src.models.candle import Candle


class CandleJsonStore:
    """
    蜡烛 JSON 存储类
    提供基于本地 JSON 文件的数据持久化能力
    """

    def __init__(self, file_path: str):
        """
        初始化存储
        
        Args:
            file_path: JSON 文件的存储路径
        """
        self.file_path = file_path
        self._lock = threading.Lock()
        self._ensure_file_exists()

    def _ensure_file_exists(self) -> None:
        """确保 JSON 文件存在，如果不存在则创建空文件"""
        if not os.path.exists(self.file_path):
            os.makedirs(os.path.dirname(self.file_path), exist_ok=True)
            with open(self.file_path, "w", encoding="utf-8") as f:
                json.dump([], f, ensure_ascii=False, indent=2)

    def load_all(self) -> List[Candle]:
        """
        从 JSON 文件加载所有蜡烛数据
        
        Returns:
            Candle 对象列表
        """
        with self._lock:
            try:
                with open(self.file_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                return [Candle.from_dict(item) for item in data]
            except (json.JSONDecodeError, FileNotFoundError):
                return []

    def save_all(self, candles: List[Candle]) -> None:
        """
        将所有蜡烛数据保存到 JSON 文件
        
        Args:
            candles: Candle 对象列表
        """
        with self._lock:
            data = [candle.to_dict() for candle in candles]
            # 先写入临时文件，再替换，防止写入过程中断导致文件损坏
            temp_path = self.file_path + ".tmp"
            with open(temp_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            os.replace(temp_path, self.file_path)
