"""
文件职责：
    文件存储层，负责将零食数据和操作记录持久化到本地 JSON 文件。
    服务启动时从 JSON 文件读取数据恢复内存库存，
    每次内存数据变更后将最新状态写回 JSON 文件。

数据流：
    读取：snacks.json（磁盘） → 解析为字典列表 → 转换为 Snack 对象列表 → 交给 snack_service
    写入：snack_service 传入 Snack 对象列表 → 转换为字典列表 → 写入 snacks.json（磁盘）
    操作记录同理，保存在 operation_logs.json
"""

import json
import os
from typing import List

from src.model.snack import Snack, OperationLog


class SnackFileStore:
    """零食文件存储器，基于本地 JSON 文件实现持久化"""

    def __init__(self, snack_file_path: str = None, log_file_path: str = None,
                 settings_file_path: str = None):
        """
        初始化文件存储器
        :param snack_file_path: 零食数据 JSON 文件路径
        :param log_file_path: 操作记录 JSON 文件路径
        :param settings_file_path: 用户设置 JSON 文件路径
        """
        project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        data_dir = os.path.join(project_root, "data")

        if snack_file_path is None:
            snack_file_path = os.path.join(data_dir, "snacks.json")
        if log_file_path is None:
            log_file_path = os.path.join(data_dir, "operation_logs.json")
        if settings_file_path is None:
            settings_file_path = os.path.join(data_dir, "settings.json")

        self.snack_file_path = snack_file_path
        self.log_file_path = log_file_path
        self.settings_file_path = settings_file_path
        self._ensure_file_exists(self.snack_file_path)
        self._ensure_file_exists(self.log_file_path)

    @staticmethod
    def _ensure_file_exists(file_path: str) -> None:
        """确保存储文件和所在目录存在，不存在则创建空文件"""
        directory = os.path.dirname(file_path)
        if directory and not os.path.exists(directory):
            os.makedirs(directory, exist_ok=True)
        if not os.path.exists(file_path):
            with open(file_path, "w", encoding="utf-8") as f:
                json.dump([], f, ensure_ascii=False, indent=2)

    # ===== 零食数据 =====

    def load_snacks(self) -> List[Snack]:
        """
        从 JSON 文件读取所有零食数据
        :return: Snack 对象列表
        """
        try:
            with open(self.snack_file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            return [Snack.from_dict(item) for item in data]
        except (json.JSONDecodeError, FileNotFoundError, IOError):
            return []

    def save_snacks(self, snacks: List[Snack]) -> None:
        """
        将所有零食数据写入 JSON 文件
        :param snacks: Snack 对象列表
        """
        data = [snack.to_dict() for snack in snacks]
        with open(self.snack_file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    # ===== 操作记录 =====

    def load_logs(self) -> List[OperationLog]:
        """
        从 JSON 文件读取所有操作记录
        :return: OperationLog 对象列表
        """
        try:
            with open(self.log_file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            return [OperationLog.from_dict(item) for item in data]
        except (json.JSONDecodeError, FileNotFoundError, IOError):
            return []

    def save_logs(self, logs: List[OperationLog]) -> None:
        """
        将所有操作记录写入 JSON 文件
        :param logs: OperationLog 对象列表
        """
        data = [log.to_dict() for log in logs]
        with open(self.log_file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    # ===== 用户设置 =====

    def load_settings(self) -> dict:
        """
        从 JSON 文件读取用户设置
        :return: 设置字典
        """
        try:
            with open(self.settings_file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            if isinstance(data, dict):
                return data
            return {}
        except (json.JSONDecodeError, FileNotFoundError, IOError):
            return {}

    def save_settings(self, settings: dict) -> None:
        """
        将用户设置写入 JSON 文件
        :param settings: 设置字典
        """
        directory = os.path.dirname(self.settings_file_path)
        if directory and not os.path.exists(directory):
            os.makedirs(directory, exist_ok=True)
        with open(self.settings_file_path, "w", encoding="utf-8") as f:
            json.dump(settings, f, ensure_ascii=False, indent=2)

    # ===== 兼容旧方法名 =====

    def load_all(self) -> List[Snack]:
        """兼容旧接口：加载所有零食"""
        return self.load_snacks()

    def save_all(self, snacks: List[Snack]) -> None:
        """兼容旧接口：保存所有零食"""
        self.save_snacks(snacks)
