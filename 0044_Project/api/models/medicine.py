from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import Optional, List
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
    dosage: str = ""
    frequency: str = ""
    suitable_for: str = ""
    contraindication: str = ""
    members: List[str] = field(default_factory=list)
    image_url: str = ""
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())

    def to_dict(self) -> dict:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict) -> "Medicine":
        members = data.get("members", [])
        if not isinstance(members, list):
            members = []
        return cls(
            id=data.get("id", str(uuid.uuid4())),
            name=data["name"],
            purpose=data["purpose"],
            quantity=data["quantity"],
            unit=data["unit"],
            expiry_date=data["expiry_date"],
            location=data["location"],
            remark=data.get("remark", ""),
            dosage=data.get("dosage", ""),
            frequency=data.get("frequency", ""),
            suitable_for=data.get("suitable_for", ""),
            contraindication=data.get("contraindication", ""),
            members=members,
            image_url=data.get("image_url", ""),
            created_at=data.get("created_at", datetime.now().isoformat()),
        )

    def needs_check(
        self, low_stock_threshold: int = 3, expiring_days: int = 30
    ) -> tuple[bool, str]:
        reasons = []
        if self.quantity < low_stock_threshold:
            reasons.append("库存不足")
        try:
            expiry = datetime.strptime(self.expiry_date, "%Y-%m-%d")
            days_left = (expiry - datetime.now()).days
            if days_left < 0:
                reasons.append("已过期")
            elif days_left < expiring_days:
                reasons.append("即将过期")
        except (ValueError, TypeError):
            pass
        return (len(reasons) > 0, "、".join(reasons))

    def is_expired(self) -> bool:
        try:
            expiry = datetime.strptime(self.expiry_date, "%Y-%m-%d")
            return (expiry - datetime.now()).days < 0
        except (ValueError, TypeError):
            return False

    def to_dict_with_check(
        self, low_stock_threshold: int = 3, expiring_days: int = 30
    ) -> dict:
        result = self.to_dict()
        needs_check, check_reason = self.needs_check(low_stock_threshold, expiring_days)
        result["needs_check"] = needs_check
        result["check_reason"] = check_reason
        result["is_expired"] = self.is_expired()
        return result
