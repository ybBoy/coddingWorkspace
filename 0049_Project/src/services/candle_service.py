"""
candle_service.py - 蜡烛业务服务层
文件职责：处理蜡烛相关的核心业务逻辑，管理内存中的蜡烛数据。
提供新增、查询、筛选、点燃记录（含使用历史）、自动估算剩余、删除等业务方法。
每次数据变更后自动调用存储层持久化到 JSON 文件。

业务流程：
  API 路由 → candle_service 处理业务 → 更新内存数据 → candle_json_store 保存 JSON

新增业务（v2）：
  - 点燃时记录使用历史（usage_logs），可选备注和燃烧时长
  - 按燃烧时长自动估算剩余比例（total_burn_hours + burned_hours）
  - 获取所有不重复香型列表
  - 获取快用完的蜡烛列表
  - 导入导出数据
"""

import uuid
from datetime import datetime
from typing import List, Optional, Dict, Any
from src.models.candle import Candle
from src.store.candle_json_store import CandleJsonStore


class CandleService:
    """
    蜡烛业务服务类
    封装所有蜡烛相关的业务逻辑
    """

    LOW_THRESHOLD = 15

    def __init__(self, store: CandleJsonStore):
        """
        初始化服务，从存储层加载数据到内存

        Args:
            store: JSON 存储实例
        """
        self.store = store
        self._candles: List[Candle] = self.store.load_all()

    def _now(self) -> str:
        """获取当前时间字符串"""
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    def _persist(self) -> None:
        """将内存数据持久化到 JSON 文件"""
        self.store.save_all(self._candles)

    def get_all(self, scent: Optional[str] = None) -> List[Candle]:
        """
        获取所有蜡烛，可按香型筛选

        Args:
            scent: 香型关键词，为空则返回全部

        Returns:
            蜡烛列表
        """
        if not scent:
            return list(self._candles)
        return [c for c in self._candles if scent.lower() in c.scent.lower()]

    def get_by_id(self, candle_id: str) -> Optional[Candle]:
        """
        根据 ID 获取单个蜡烛

        Args:
            candle_id: 蜡烛 ID

        Returns:
            Candle 对象或 None
        """
        for candle in self._candles:
            if candle.id == candle_id:
                return candle
        return None

    def add_candle(
        self,
        name: str,
        scent: str,
        capacity: float,
        remaining_ratio: int = 100,
        purchase_date: str = "",
        note: str = "",
        total_burn_hours: float = 0.0,
    ) -> Candle:
        """
        新增一支蜡烛

        Args:
            name: 蜡烛名称
            scent: 香型
            capacity: 容量
            remaining_ratio: 剩余比例（默认 100%）
            purchase_date: 购买日期
            note: 备注
            total_burn_hours: 预设总燃烧小时数（0 表示未设置）

        Returns:
            新创建的 Candle 对象
        """
        now = self._now()
        candle = Candle(
            id=str(uuid.uuid4()),
            name=name,
            scent=scent,
            capacity=float(capacity),
            remaining_ratio=max(0, min(100, int(remaining_ratio))),
            purchase_date=purchase_date or datetime.now().strftime("%Y-%m-%d"),
            use_count=0,
            note=note,
            created_at=now,
            updated_at=now,
            usage_logs=[],
            total_burn_hours=float(total_burn_hours),
            burned_hours=0.0,
        )
        self._candles.append(candle)
        self._persist()
        return candle

    def light_candle(
        self,
        candle_id: str,
        note: str = "",
        burn_hours: float = 0.0,
    ) -> Optional[Candle]:
        """
        记录点燃一次，使用次数 +1，追加使用历史记录。
        如果提供了 burn_hours 且蜡烛设置了 total_burn_hours，自动估算剩余比例。

        流程：
          1. use_count += 1
          2. 如果 burn_hours > 0 且 total_burn_hours > 0：
             - burned_hours += burn_hours
             - remaining_ratio = calc_remaining_from_burn(burn_hours)
          3. 追加 usage_logs 记录
          4. 持久化

        Args:
            candle_id: 蜡烛 ID
            note: 本次点燃备注
            burn_hours: 本次燃烧时长（小时）

        Returns:
            更新后的 Candle 对象，找不到则返回 None
        """
        candle = self.get_by_id(candle_id)
        if not candle:
            return None

        now = self._now()
        candle.use_count += 1

        old_remaining = candle.remaining_ratio

        if burn_hours > 0 and candle.total_burn_hours > 0:
            candle.burned_hours += burn_hours
            estimated = candle.calc_remaining_from_burn(0)
            candle.remaining_ratio = max(0, min(100, estimated))

        log_entry = {
            "time": now,
            "remaining_ratio": candle.remaining_ratio,
            "note": note,
            "burn_hours": burn_hours,
        }
        candle.usage_logs.append(log_entry)
        candle.updated_at = now
        self._persist()
        return candle

    def update_remaining(self, candle_id: str, remaining_ratio: int) -> Optional[Candle]:
        """
        修改剩余比例

        Args:
            candle_id: 蜡烛 ID
            remaining_ratio: 新的剩余比例（0-100）

        Returns:
            更新后的 Candle 对象，找不到则返回 None
        """
        candle = self.get_by_id(candle_id)
        if not candle:
            return None
        candle.remaining_ratio = max(0, min(100, int(remaining_ratio)))
        candle.updated_at = self._now()
        self._persist()
        return candle

    def delete_candle(self, candle_id: str) -> bool:
        """
        删除一支蜡烛

        Args:
            candle_id: 蜡烛 ID

        Returns:
            是否删除成功
        """
        for i, candle in enumerate(self._candles):
            if candle.id == candle_id:
                self._candles.pop(i)
                self._persist()
                return True
        return False

    def get_stats(self, scent: Optional[str] = None) -> dict:
        """
        获取统计数据

        Args:
            scent: 筛选的香型（用于计算筛选结果数量）

        Returns:
            统计字典：total_count、low_count、filtered_count
        """
        total = len(self._candles)
        low = sum(1 for c in self._candles if c.is_low(self.LOW_THRESHOLD))
        if scent:
            filtered = len([c for c in self._candles if scent.lower() in c.scent.lower()])
        else:
            filtered = total
        return {
            "total_count": total,
            "low_count": low,
            "filtered_count": filtered,
        }

    def get_all_scents(self) -> List[str]:
        """
        获取所有不重复的香型列表

        Returns:
            去重后的香型字符串列表
        """
        scents = list(dict.fromkeys(c.scent for c in self._candles if c.scent))
        return scents

    def get_low_candles(self) -> List[Candle]:
        """
        获取快用完的蜡烛列表（剩余比例低于阈值）

        Returns:
            剩余比例低于 15% 的蜡烛列表
        """
        return [c for c in self._candles if c.is_low(self.LOW_THRESHOLD)]

    def export_data(self) -> List[dict]:
        """
        导出所有蜡烛数据

        Returns:
            蜡烛字典列表
        """
        return [c.to_dict() for c in self._candles]

    def import_data(self, data: List[dict], merge: bool = False) -> dict:
        """
        导入蜡烛数据

        Args:
            data: 蜡烛字典列表
            merge: True 为合并模式（追加不覆盖），False 为覆盖模式

        Returns:
            导入结果统计
        """
        if not isinstance(data, list):
            raise ValueError("导入数据必须是列表")

        valid_items = []
        for item in data:
            if not isinstance(item, dict):
                continue
            try:
                candle = Candle.from_dict(item)
                if candle.name and candle.scent:
                    valid_items.append(candle)
            except Exception:
                continue

        if not valid_items:
            raise ValueError("没有有效的蜡烛数据可导入")

        if merge:
            existing_ids = {c.id for c in self._candles}
            added = 0
            skipped = 0
            for candle in valid_items:
                if candle.id and candle.id in existing_ids:
                    skipped += 1
                else:
                    if not candle.id:
                        candle.id = str(uuid.uuid4())
                    self._candles.append(candle)
                    added += 1
            self._persist()
            return {"mode": "merge", "added": added, "skipped": skipped}
        else:
            self._candles = valid_items
            self._persist()
            return {"mode": "replace", "count": len(valid_items)}
