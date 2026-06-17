"""
文件职责：
    定义零食（Snack）和操作记录（OperationLog）的数据模型，
    用于在各层之间传递零食信息和操作历史。

数据流中的位置：
    snack_file_store（JSON ↔ 对象） ↔ snack_service（业务逻辑） ↔ snack_api（JSON响应）
"""

from dataclasses import dataclass, asdict
from datetime import datetime, date
from typing import Optional
import uuid


@dataclass
class Snack:
    """零食数据模型"""

    id: str
    name: str
    flavor: str
    quantity: int
    location: str
    expiry_date: str
    category: str = ""
    target_quantity: int = 0
    disabled: bool = False

    @staticmethod
    def create(name: str, flavor: str, quantity: int, location: str,
               expiry_date: str, category: str = "",
               target_quantity: int = 0) -> "Snack":
        """创建一个新的零食实例，自动生成唯一 ID"""
        return Snack(
            id=str(uuid.uuid4()),
            name=name,
            flavor=flavor,
            quantity=max(0, quantity),
            location=location,
            expiry_date=expiry_date,
            category=category,
            target_quantity=max(0, target_quantity),
            disabled=False,
        )

    def to_dict(self) -> dict:
        """将 Snack 对象转换为字典，用于 JSON 序列化"""
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "Snack":
        """从字典构造 Snack 对象，容错非法数字"""
        try:
            safe_qty = max(0, int(data.get("quantity", 0)))
        except (ValueError, TypeError):
            safe_qty = 0

        try:
            safe_target = max(0, int(data.get("target_quantity", 0)))
        except (ValueError, TypeError):
            safe_target = 0

        return Snack(
            id=data.get("id", str(uuid.uuid4())),
            name=data.get("name", ""),
            flavor=data.get("flavor", ""),
            quantity=safe_qty,
            location=data.get("location", ""),
            expiry_date=data.get("expiry_date", ""),
            category=data.get("category", ""),
            target_quantity=safe_target,
            disabled=bool(data.get("disabled", False)),
        )

    def update(self, name: str = None, flavor: str = None, quantity: int = None,
               location: str = None, expiry_date: str = None,
               category: str = None, target_quantity: int = None,
               disabled: bool = None) -> None:
        """更新零食的部分字段，传 None 表示不修改"""
        if name is not None:
            self.name = name
        if flavor is not None:
            self.flavor = flavor
        if quantity is not None:
            self.quantity = max(0, quantity)
        if location is not None:
            self.location = location
        if expiry_date is not None:
            self.expiry_date = expiry_date
        if category is not None:
            self.category = category
        if target_quantity is not None:
            self.target_quantity = max(0, target_quantity)
        if disabled is not None:
            self.disabled = disabled

    def is_low_stock(self) -> bool:
        """判断库存是否不足（数量低于 2）"""
        return self.quantity < 2

    def is_expiring_soon(self, days_threshold: int = 7) -> bool:
        """
        判断是否临近保质期
        :param days_threshold: 临期天数阈值，默认 7 天
        """
        days = self.days_to_expiry()
        return days is not None and 0 <= days < max(1, days_threshold)

    def is_expired(self) -> bool:
        """判断是否已过期"""
        days = self.days_to_expiry()
        return days is not None and days < 0

    def needs_attention(self, expiring_days: int = 7) -> bool:
        """判断是否需要处理（库存不足 或 临近保质期 或 已过期）"""
        return (self.is_low_stock()
                or self.is_expiring_soon(expiring_days)
                or self.is_expired())

    def is_below_target(self) -> bool:
        """判断是否低于目标库存（目标库存 > 0 且 当前数量 < 目标库存）"""
        return self.target_quantity > 0 and self.quantity < self.target_quantity

    def restock_suggestion(self) -> int:
        """
        计算补货建议数量
        :return: 建议补货数量（目标库存 - 当前数量，低于目标时返回差值，否则返回 0
        """
        if not self.is_below_target():
            return 0
        return self.target_quantity - self.quantity

    def days_to_expiry(self) -> Optional[int]:
        """计算距离保质期的天数，过期返回负数，格式错误返回 None"""
        if not self.expiry_date:
            return None
        try:
            expiry = datetime.strptime(self.expiry_date, "%Y-%m-%d").date()
            return (expiry - date.today()).days
        except (ValueError, TypeError):
            return None

    def matches_keyword(self, keyword: str) -> bool:
        """
        判断是否匹配搜索关键词（按名称、口味、分类模糊匹配，不区分大小写）
        """
        if not keyword:
            return True
        kw = keyword.lower()
        return (kw in self.name.lower()
                or kw in self.flavor.lower()
                or kw in self.category.lower())


@dataclass
class OperationLog:
    """操作记录数据模型"""

    id: str
    timestamp: str
    action: str
    snack_id: str
    snack_name: str
    quantity_change: int = 0
    note: str = ""

    @staticmethod
    def create(action: str, snack_id: str, snack_name: str,
               quantity_change: int = 0, note: str = "") -> "OperationLog":
        """创建一条新的操作记录"""
        return OperationLog(
            id=str(uuid.uuid4()),
            timestamp=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            action=action,
            snack_id=snack_id,
            snack_name=snack_name,
            quantity_change=quantity_change,
            note=note,
        )

    def to_dict(self) -> dict:
        """转换为字典"""
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "OperationLog":
        """从字典构造"""
        safe_qty = 0
        try:
            safe_qty = int(data.get("quantity_change", 0))
        except (ValueError, TypeError):
            safe_qty = 0
        return OperationLog(
            id=data.get("id", ""),
            timestamp=data.get("timestamp", ""),
            action=data.get("action", ""),
            snack_id=data.get("snack_id", ""),
            snack_name=data.get("snack_name", ""),
            quantity_change=safe_qty,
            note=data.get("note", ""),
        )
