from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import Optional
import uuid


@dataclass
class Medicine:
    name: str
    purpose: str
    quantity: int
    unit: str
    expiry_date: str
    location: str
    remark: str = ""
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())

    def to_dict(self) -> dict:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "Medicine":
        return cls(
            id=data.get("id", str(uuid.uuid4())),
            name=data["name"],
            purpose=data["purpose"],
            quantity=data["quantity"],
            unit=data["unit"],
            expiry_date=data["expiry_date"],
            location=data["location"],
            remark=data.get("remark", ""),
            created_at=data.get("created_at", datetime.now().isoformat()),
        )

    def needs_check(self) -> tuple[bool, str]:
        reasons = []
        if self.quantity < 3:
            reasons.append("库存不足")
        try:
            expiry = datetime.strptime(self.expiry_date, "%Y-%m-%d")
            days_left = (expiry - datetime.now()).days
            if days_left < 30:
                reasons.append("即将过期")
        except (ValueError, TypeError):
            pass
        return (len(reasons) > 0, "、".join(reasons))

    def to_dict_with_check(self) -> dict:
        result = self.to_dict()
        needs_check, check_reason = self.needs_check()
        result["needs_check"] = needs_check
        result["check_reason"] = check_reason
        return result
