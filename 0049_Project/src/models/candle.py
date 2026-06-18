"""
candle.py - 蜡烛数据模型
文件职责：定义香薰蜡烛的数据结构，包含所有蜡烛相关的属性字段。
使用 dataclass 简化对象创建和序列化，支持与 JSON 格式互相转换。
"""

from dataclasses import dataclass, asdict
from datetime import date, datetime
from typing import Optional


@dataclass
class Candle:
    """
    香薰蜡烛数据模型
    
    属性说明：
    - id: 唯一标识符，自动生成
    - name: 蜡烛名称
    - scent: 香型，如"玫瑰"、"薰衣草"等
    - capacity: 容量（克或毫升）
    - remaining_ratio: 剩余比例，0-100 的整数
    - purchase_date: 购买日期，格式 YYYY-MM-DD
    - use_count: 使用次数，即点燃次数
    - note: 备注信息，可选
    - created_at: 创建时间
    - updated_at: 更新时间
    """
    id: Optional[str] = None
    name: str = ""
    scent: str = ""
    capacity: float = 0.0
    remaining_ratio: int = 100
    purchase_date: str = ""
    use_count: int = 0
    note: str = ""
    created_at: Optional[str] = None
    updated_at: Optional[str] = None

    def to_dict(self) -> dict:
        """将 Candle 对象转换为字典，便于 JSON 序列化"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "Candle":
        """从字典创建 Candle 对象"""
        return cls(**{k: v for k, v in data.items() if k in cls.__dataclass_fields__})

    def is_low(self, threshold: int = 15) -> bool:
        """判断蜡烛是否快用完了（剩余比例低于阈值）"""
        return self.remaining_ratio < threshold
