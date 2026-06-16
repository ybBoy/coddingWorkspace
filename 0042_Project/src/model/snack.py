"""
文件职责：
    定义零食（Snack）的数据模型，用于在各层之间传递零食信息。
    包含零食的基本属性：名称、口味、数量、存放位置、保质期等，
    以及判断是否需要补货/临近保质期的辅助方法。

数据流中的位置：
    snack_file_store（JSON ↔ Snack对象） ↔ snack_service（业务逻辑） ↔ snack_api（JSON响应）
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
    disabled: bool = False

    @staticmethod
    def create(name: str, flavor: str, quantity: int, location: str, expiry_date: str) -> "Snack":
        """创建一个新的零食实例，自动生成唯一 ID"""
        return Snack(
            id=str(uuid.uuid4()),
            name=name,
            flavor=flavor,
            quantity=max(0, quantity),
            location=location,
            expiry_date=expiry_date,
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
            disabled=bool(data.get("disabled", False)),
        )

    def is_low_stock(self) -> bool:
        """判断库存是否不足（数量低于 2）"""
        return self.quantity < 2

    def is_expiring_soon(self) -> bool:
        """判断是否临近保质期（少于 7 天）"""
        if not self.expiry_date:
            return False
        try:
            expiry = datetime.strptime(self.expiry_date, "%Y-%m-%d").date()
            days_left = (expiry - date.today()).days
            return days_left < 7
        except (ValueError, TypeError):
            return False

    def needs_attention(self) -> bool:
        """判断是否需要处理（库存不足 或 临近保质期 或 已标记不再购买）"""
        return self.is_low_stock() or self.is_expiring_soon() or self.disabled

    def days_to_expiry(self) -> Optional[int]:
        """计算距离保质期的天数，过期返回负数，格式错误返回 None"""
        if not self.expiry_date:
            return None
        try:
            expiry = datetime.strptime(self.expiry_date, "%Y-%m-%d").date()
            return (expiry - date.today()).days
        except (ValueError, TypeError):
            return None
