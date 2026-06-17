from typing import List, Optional, Tuple
from datetime import datetime
import json

from api.models.medicine import Medicine
from api.storage.medicine_store import medicine_store
from api.storage.log_store import log_store


class MedicineService:
    def __init__(self):
        self.store = medicine_store
        self.logs = log_store

    def get_medicines(
        self,
        purpose: Optional[str] = None,
        location: Optional[str] = None,
        keyword: Optional[str] = None,
    ) -> Tuple[List[dict], dict]:
        medicines = self.store.get_all()

        if purpose:
            medicines = [m for m in medicines if m.purpose == purpose]
        if location:
            medicines = [m for m in medicines if m.location == location]
        if keyword:
            keyword_lower = keyword.lower()
            medicines = [
                m
                for m in medicines
                if keyword_lower in m.name.lower()
                or keyword_lower in (m.remark or "").lower()
            ]

        medicines_sorted = sorted(medicines, key=lambda m: m.created_at, reverse=True)

        result = [m.to_dict_with_check() for m in medicines_sorted]

        needs_check_count = 0
        low_stock_count = 0
        expiring_count = 0
        for m in medicines:
            needs_check, reason = m.needs_check()
            if needs_check:
                needs_check_count += 1
                if "库存不足" in reason:
                    low_stock_count += 1
                if "即将过期" in reason or "已过期" in reason:
                    expiring_count += 1

        stats = {
            "total": len(medicines),
            "needs_check": needs_check_count,
            "low_stock_count": low_stock_count,
            "expiring_count": expiring_count,
        }

        return result, stats

    def get_medicine_by_id(self, medicine_id: str) -> Optional[dict]:
        medicine = self.store.get_by_id(medicine_id)
        return medicine.to_dict_with_check() if medicine else None

    def add_medicine(self, data: dict) -> dict:
        required_fields = [
            "name",
            "purpose",
            "quantity",
            "unit",
            "expiry_date",
            "location",
        ]
        for field in required_fields:
            if field not in data or data[field] in (None, ""):
                raise ValueError(f"缺少必填字段: {field}")

        try:
            quantity = int(data["quantity"])
            if quantity < 0:
                raise ValueError("数量不能为负数")
        except (ValueError, TypeError):
            raise ValueError("数量必须是有效的正整数")

        try:
            datetime.strptime(data["expiry_date"], "%Y-%m-%d")
        except ValueError:
            raise ValueError("有效期格式不正确，应为 YYYY-MM-DD")

        medicine = Medicine(
            name=data["name"].strip(),
            purpose=data["purpose"].strip(),
            quantity=quantity,
            unit=data["unit"].strip(),
            expiry_date=data["expiry_date"],
            location=data["location"].strip(),
            remark=data.get("remark", "").strip(),
        )

        created = self.store.add(medicine)
        return created.to_dict_with_check()

    def update_medicine(self, medicine_id: str, data: dict) -> Optional[dict]:
        medicine = self.store.get_by_id(medicine_id)
        if not medicine:
            return None

        update_data = {}
        for field in ["name", "purpose", "unit", "expiry_date", "location", "remark"]:
            if field in data and data[field] is not None:
                update_data[field] = str(data[field]).strip()

        if "quantity" in data and data["quantity"] is not None:
            try:
                qty = int(data["quantity"])
                if qty < 0:
                    raise ValueError("数量不能为负数")
                update_data["quantity"] = qty
            except (ValueError, TypeError):
                raise ValueError("数量必须是有效的正整数")

        if "expiry_date" in update_data:
            try:
                datetime.strptime(update_data["expiry_date"], "%Y-%m-%d")
            except ValueError:
                raise ValueError("有效期格式不正确，应为 YYYY-MM-DD")

        updated = self.store.update(medicine_id, update_data)
        return updated.to_dict_with_check() if updated else None

    def delete_medicine(self, medicine_id: str) -> bool:
        return self.store.delete(medicine_id)

    def use_medicine(self, medicine_id: str, amount: int) -> Optional[dict]:
        try:
            amount = int(amount)
            if amount <= 0:
                raise ValueError("使用数量必须大于0")
        except (ValueError, TypeError):
            raise ValueError("使用数量必须是有效的正整数")

        try:
            updated = self.store.use_medicine(medicine_id, amount)
            if updated:
                self.logs.add_log("使用", updated.name, amount, updated.unit)
            return updated.to_dict_with_check() if updated else None
        except ValueError as e:
            raise ValueError(str(e))

    def replenish_medicine(self, medicine_id: str, amount: int) -> Optional[dict]:
        try:
            amount = int(amount)
            if amount <= 0:
                raise ValueError("补充数量必须大于0")
        except (ValueError, TypeError):
            raise ValueError("补充数量必须是有效的正整数")

        updated = self.store.replenish_medicine(medicine_id, amount)
        if updated:
            self.logs.add_log("补充", updated.name, amount, updated.unit)
        return updated.to_dict_with_check() if updated else None

    def get_filter_options(self) -> dict:
        medicines = self.store.get_all()
        purposes = sorted(list({m.purpose for m in medicines}))
        locations = sorted(list({m.location for m in medicines}))
        return {"purposes": purposes, "locations": locations}

    def get_operation_logs(self, limit: int = 50) -> list[dict]:
        return self.logs.get_logs(limit)

    def export_data(self) -> dict:
        medicines = [m.to_dict() for m in self.store.get_all()]
        logs = self.logs.get_logs(500)
        return {
            "version": "1.0",
            "exported_at": datetime.now().isoformat(),
            "medicines": medicines,
            "operation_logs": logs,
        }

    def import_data(self, data: dict) -> dict:
        if not isinstance(data, dict):
            raise ValueError("导入数据格式错误")

        medicines_data = data.get("medicines", [])
        if not isinstance(medicines_data, list):
            raise ValueError("medicines 必须是数组")

        imported_count = 0
        for item in medicines_data:
            try:
                medicine = Medicine.from_dict(item)
                self.store.add(medicine)
                imported_count += 1
            except Exception as e:
                print(f"跳过无效数据: {e}")

        logs_data = data.get("operation_logs", [])
        if isinstance(logs_data, list):
            pass

        return {"imported": imported_count, "skipped": len(medicines_data) - imported_count}


medicine_service = MedicineService()
