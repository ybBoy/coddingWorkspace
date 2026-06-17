"""
文件职责：
    定义零食（Snack）和操作记录（OperationLog）的数据模型，
    用于在各层之间传递零食信息和操作历史。

数据流中的位置：
    snack_file_store（JSON ↔ 对象） ↔ snack_service（业务逻辑） ↔ snack_api（JSON响应）
"""

from dataclasses import dataclass, asdict, field
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
    disabled: bool = False

    @staticmethod
    def create(name: str, flavor: str, quantity: int, location: str,
               expiry_date: str, category: str = "") -> "Snack":
        """创建一个新的零食实例，自动生成唯一 ID"""
        return Snack(
            id=str(uuid.uuid4()),
            name=name,
            flavor=flavor,
            quantity=max(0, quantity),
            location=location,
            expiry_date=expiry_date,
            category=category,
            disabled=False,
        )

    def to_dict(self) -> dict:
        """将 Snack 对象转换为字典，用于 JSON 序列化"""
        return asdict(self)

    @staticmethod
    def from_dict(data: dict) -> "Snack":
        """从字典构造 Snack 对象"""
        return Snack(
            id=data.get("id", str(uuid.uuid4())),
            name=data.get("name", ""),
            flavor=data.get("flavor", ""),
            quantity=max(0, int(data.get("quantity", 0))),
            location=data.get("location", ""),
            expiry_date=data.get("expiry_date", ""),
            category=data.get("category", ""),
            disabled=bool(data.get("disabled", False)),
        )

    def update(self, name: str = None, flavor: str = None, quantity: int = None,
               location: str = None, expiry_date: str = None,
               category: str = None, disabled: bool = None) -> None:
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
        if disabled is not None:
            self.disabled = disabled

    def is_low_stock(self) -> bool:
        """判断库存是否不足（数量低于 2）"""
        return self.quantity < 2

    def is_expiring_soon(self) -> bool:
        """判断是否临近保质期（少于 7 天且未过期）"""
        days = self.days_to_expiry()
        return days is not None and 0 <= days < 7

    def is_expired(self) -> bool:
        """判断是否已过期"""
        days = self.days_to_expiry()
        return days is not None and days < 0

    def needs_attention(self) -> bool:
        """判断是否需要处理（库存不足 或 临近保质期 或 已过期）"""
        return self.is_low_stock() or self.is_expiring_soon() or self.is_expired()

    def days_to_expiry(self) -> Optional[int]:
        """计算距离保质期的天数，过期返回负数，格式错误返回 None"""
        if not self.expiry_date:
            return None
        try:
            expiry = datetime.strptime(self.expiry_date, "%Y-%m-%d").date()
            return (expiry - date.today()).days
        except (ValueError, TypeError):
            return None


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
        return OperationLog(
            id=data.get("id", ""),
            timestamp=data.get("timestamp", ""),
            action=data.get("action", ""),
            snack_id=data.get("snack_id", ""),
            snack_name=data.get("snack_name", ""),
            quantity_change=int(data.get("quantity_change", 0)),
            note=data.get("note", ""),
        )
