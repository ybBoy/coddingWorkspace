"""
candle.py - 蜡烛数据模型
文件职责：定义香薰蜡烛的数据结构，包含所有蜡烛相关的属性字段。
使用 dataclass 简化对象创建和序列化，支持与 JSON 格式互相转换。

新增字段说明（v2）：
- usage_logs: 使用历史列表，每次点燃记录 {time, remaining_ratio, note, burn_hours}
- total_burn_hours: 预设总燃烧小时数，用于自动估算剩余比例
- burned_hours: 已燃烧小时数，每次点燃累加
"""

from dataclasses import dataclass, asdict, field
from typing import Optional, List, Dict


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
    - usage_logs: 使用历史列表，每条记录含 time/remaining_ratio/note/burn_hours
    - total_burn_hours: 预设总燃烧小时数（0 表示未设置，不自动估算）
    - burned_hours: 已燃烧小时数
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
    usage_logs: List[Dict] = field(default_factory=list)
    total_burn_hours: float = 0.0
    burned_hours: float = 0.0

    def to_dict(self) -> dict:
        """将 Candle 对象转换为字典，便于 JSON 序列化"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "Candle":
        """从字典创建 Candle 对象，忽略未知字段以兼容旧数据"""
        known = {k: v for k, v in data.items() if k in cls.__dataclass_fields__}
        if "usage_logs" not in known:
            known["usage_logs"] = []
        if "total_burn_hours" not in known:
            known["total_burn_hours"] = 0.0
        if "burned_hours" not in known:
            known["burned_hours"] = 0.0
        return cls(**known)

    def is_low(self, threshold: int = 15) -> bool:
        """判断蜡烛是否快用完了（剩余比例低于阈值）"""
        return self.remaining_ratio < threshold

    def sanitize(self) -> None:
        """
        规范化字段类型和范围，防止脏数据导致后续逻辑异常。
        主要用于导入数据后的清洗，保证：
        - capacity >= 0 的 float
        - remaining_ratio 在 0-100 的 int
        - use_count >= 0 的 int
        - usage_logs 必须是 list，每条记录规范字段
        - total_burn_hours >= 0 的 float
        - burned_hours >= 0 的 float
        """
        try:
            self.capacity = max(0.0, float(self.capacity))
        except (TypeError, ValueError):
            self.capacity = 0.0

        try:
            self.remaining_ratio = max(0, min(100, int(self.remaining_ratio)))
        except (TypeError, ValueError):
            self.remaining_ratio = 100

        try:
            self.use_count = max(0, int(self.use_count))
        except (TypeError, ValueError):
            self.use_count = 0

        if not isinstance(self.usage_logs, list):
            self.usage_logs = []
        else:
            sanitized_logs = []
            for log in self.usage_logs:
                if not isinstance(log, dict):
                    continue
                sanitized_logs.append({
                    "time": str(log.get("time", "")),
                    "remaining_ratio": max(0, min(100, int(log.get("remaining_ratio", 100)))),
                    "note": str(log.get("note", "")),
                    "burn_hours": max(0.0, float(log.get("burn_hours", 0))),
                })
            self.usage_logs = sanitized_logs

        try:
            self.total_burn_hours = max(0.0, float(self.total_burn_hours))
        except (TypeError, ValueError):
            self.total_burn_hours = 0.0

        try:
            self.burned_hours = max(0.0, float(self.burned_hours))
        except (TypeError, ValueError):
            self.burned_hours = 0.0

    def calc_remaining_from_burn(self, additional_hours: float) -> int:
        """
        根据燃烧时长自动估算剩余比例

        计算公式：
          new_burned = burned_hours + additional_hours
          remaining_ratio = round((1 - new_burned / total_burn_hours) * 100)
          限制在 0-100 范围内

        Args:
            additional_hours: 本次燃烧时长（小时）

        Returns:
            估算的剩余比例（0-100），如果 total_burn_hours 为 0 则返回 -1 表示无法估算
        """
        if self.total_burn_hours <= 0:
            return -1
        new_burned = self.burned_hours + additional_hours
        ratio = round((1 - new_burned / self.total_burn_hours) * 100)
        return max(0, min(100, ratio))
