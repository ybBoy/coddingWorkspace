"""
candle_service.py - 蜡烛业务服务层
文件职责：处理蜡烛相关的核心业务逻辑，管理内存中的蜡烛数据。
提供新增、查询、筛选、点燃记录、修改剩余比例、删除等业务方法。
每次数据变更后自动调用存储层持久化到 JSON 文件。

业务流程：
  API 路由 → candle_service 处理业务 → 更新内存数据 → candle_json_store 保存 JSON
"""

import uuid
from datetime import datetime
from typing import List, Optional
from src.models.candle import Candle
from src.store.candle_json_store import CandleJsonStore


class CandleService:
    """
    蜡烛业务服务类
    封装所有蜡烛相关的业务逻辑
    """

    LOW_THRESHOLD = 15  # 快用完的阈值（剩余比例低于此值）

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
        )
        self._candles.append(candle)
        self._persist()
        return candle

    def light_candle(self, candle_id: str) -> Optional[Candle]:
        """
        记录点燃一次，使用次数 +1
        
        Args:
            candle_id: 蜡烛 ID
            
        Returns:
            更新后的 Candle 对象，找不到则返回 None
        """
        candle = self.get_by_id(candle_id)
        if not candle:
            return None
        candle.use_count += 1
        candle.updated_at = self._now()
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
            统计字典：total_count（总数）、low_count（快用完的数量）、filtered_count（筛选结果数量）
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
