import os
from typing import List, Optional, Tuple
from datetime import datetime
import uuid
from werkzeug.utils import secure_filename

from api.models.medicine import Medicine
from api.models.settings import Settings
from api.storage.medicine_store import medicine_store
from api.storage.log_store import log_store
from api.storage.settings_store import settings_store


UPLOAD_FOLDER = "uploads"
ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "bmp", "webp"}
MAX_IMAGE_SIZE = 5 * 1024 * 1024


def _allowed_file(filename: str) -> bool:
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS


class MedicineService:
    def __init__(self):
        self.store = medicine_store
        self.logs = log_store
        self.settings_store = settings_store

    def _get_settings(self) -> Settings:
        return self.settings_store.get_settings()

    def get_medicines(
        self,
        purpose: Optional[str] = None,
        location: Optional[str] = None,
        keyword: Optional[str] = None,
        member: Optional[str] = None,
        sort: Optional[str] = None,
        only_expired: bool = False,
    ) -> Tuple[List[dict], dict]:
        settings = self._get_settings()
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
                or keyword_lower in (m.purpose or "").lower()
            ]
        if member:
            medicines = [m for m in medicines if member in (m.members or [])]

        if only_expired:
            medicines = [m for m in medicines if m.is_expired()]

        medicines_sorted = self._sort_medicines(medicines, sort)

        result = [
            m.to_dict_with_check(settings.low_stock_threshold, settings.expiring_days)
            for m in medicines_sorted
        ]

        needs_check_count = 0
        low_stock_count = 0
        expiring_count = 0
        expired_count = 0
        for m in medicines:
            needs_check, reason = m.needs_check(
                settings.low_stock_threshold, settings.expiring_days
            )
            if needs_check:
                needs_check_count += 1
                if "库存不足" in reason:
                    low_stock_count += 1
                if "即将过期" in reason:
                    expiring_count += 1
                if "已过期" in reason:
                    expired_count += 1

        stats = {
            "total": len(medicines),
            "needs_check": needs_check_count,
            "low_stock_count": low_stock_count,
            "expiring_count": expiring_count,
            "expired_count": expired_count,
        }

        return result, stats

    def _sort_medicines(self, medicines: List[Medicine], sort: Optional[str]) -> List[Medicine]:
        if not sort:
            sort = "created_desc"

        def expiry_key(m: Medicine):
            try:
                return datetime.strptime(m.expiry_date, "%Y-%m-%d")
            except (ValueError, TypeError):
                return datetime.max

        if sort == "expiry_asc":
            return sorted(medicines, key=expiry_key)
        elif sort == "expiry_desc":
            return sorted(medicines, key=expiry_key, reverse=True)
        elif sort == "quantity_asc":
            return sorted(medicines, key=lambda m: m.quantity)
        elif sort == "quantity_desc":
            return sorted(medicines, key=lambda m: m.quantity, reverse=True)
        elif sort == "name_asc":
            return sorted(medicines, key=lambda m: m.name)
        elif sort == "created_desc":
            return sorted(medicines, key=lambda m: m.created_at, reverse=True)
        elif sort == "created_asc":
            return sorted(medicines, key=lambda m: m.created_at)
        else:
            return sorted(medicines, key=lambda m: m.created_at, reverse=True)

    def get_medicine_by_id(self, medicine_id: str) -> Optional[dict]:
        settings = self._get_settings()
        medicine = self.store.get_by_id(medicine_id)
        if not medicine:
            return None
        return medicine.to_dict_with_check(
            settings.low_stock_threshold, settings.expiring_days
        )

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

        members = data.get("members", [])
        if not isinstance(members, list):
            members = []

        medicine = Medicine(
            name=data["name"].strip(),
            purpose=data["purpose"].strip(),
            quantity=quantity,
            unit=data["unit"].strip(),
            expiry_date=data["expiry_date"],
            location=data["location"].strip(),
            remark=data.get("remark", "").strip(),
            dosage=data.get("dosage", "").strip(),
            frequency=data.get("frequency", "").strip(),
            suitable_for=data.get("suitable_for", "").strip(),
            contraindication=data.get("contraindication", "").strip(),
            members=members,
            image_url=data.get("image_url", "").strip(),
        )

        created = self.store.add(medicine)
        settings = self._get_settings()
        return created.to_dict_with_check(
            settings.low_stock_threshold, settings.expiring_days
        )

    def update_medicine(self, medicine_id: str, data: dict) -> Optional[dict]:
        medicine = self.store.get_by_id(medicine_id)
        if not medicine:
            return None

        update_data = {}
        for field in ["name", "purpose", "unit", "expiry_date", "location", "remark",
                      "dosage", "frequency", "suitable_for", "contraindication", "image_url"]:
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

        if "members" in data:
            members = data["members"]
            if isinstance(members, list):
                update_data["members"] = [str(m).strip() for m in members if str(m).strip()]

        if "expiry_date" in update_data:
            try:
                datetime.strptime(update_data["expiry_date"], "%Y-%m-%d")
            except ValueError:
                raise ValueError("有效期格式不正确，应为 YYYY-MM-DD")

        updated = self.store.update(medicine_id, update_data)
        if not updated:
            return None

        settings = self._get_settings()
        return updated.to_dict_with_check(
            settings.low_stock_threshold, settings.expiring_days
        )

    def delete_medicine(self, medicine_id: str) -> bool:
        return self.store.delete(medicine_id)

    def delete_expired_medicines(self) -> dict:
        medicines = self.store.get_all()
        expired = [m for m in medicines if m.is_expired()]
        deleted = 0
        for m in expired:
            if self.store.delete(m.id):
                deleted += 1
        return {"deleted": deleted, "total_expired": len(expired)}

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
            settings = self._get_settings()
            return (
                updated.to_dict_with_check(
                    settings.low_stock_threshold, settings.expiring_days
                )
                if updated
                else None
            )
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
        settings = self._get_settings()
        return (
            updated.to_dict_with_check(
                settings.low_stock_threshold, settings.expiring_days
            )
            if updated
            else None
        )

    def get_filter_options(self) -> dict:
        settings = self._get_settings()
        medicines = self.store.get_all()
        purposes = sorted(list({m.purpose for m in medicines}))
        locations = sorted(list({m.location for m in medicines}))
        all_members = set()
        for m in medicines:
            if m.members:
                all_members.update(m.members)
        members = sorted(list(all_members))
        return {
            "purposes": purposes,
            "locations": locations,
            "members": members,
            "default_members": settings.default_members,
        }

    def get_operation_logs(self, limit: int = 50) -> list[dict]:
        return self.logs.get_logs(limit)

    def get_settings(self) -> dict:
        return self._get_settings().to_dict()

    def update_settings(self, data: dict) -> dict:
        updated = self.settings_store.update_settings(data)
        return updated.to_dict()

    def upload_image(self, medicine_id: str, file_storage) -> Optional[str]:
        medicine = self.store.get_by_id(medicine_id)
        if not medicine:
            raise ValueError("药品不存在")

        if not file_storage or not file_storage.filename:
            raise ValueError("未选择文件")

        filename = secure_filename(file_storage.filename)
        if not _allowed_file(filename):
            raise ValueError("不支持的图片格式")

        os.makedirs(UPLOAD_FOLDER, exist_ok=True)

        ext = filename.rsplit(".", 1)[1].lower()
        new_filename = f"{medicine_id}_{uuid.uuid4().hex[:8]}.{ext}"
        file_path = os.path.join(UPLOAD_FOLDER, new_filename)

        file_storage.seek(0, os.SEEK_END)
        size = file_storage.tell()
        if size > MAX_IMAGE_SIZE:
            raise ValueError("图片大小不能超过 5MB")
        file_storage.seek(0)

        file_storage.save(file_path)

        image_url = f"/uploads/{new_filename}"
        self.store.update(medicine_id, {"image_url": image_url})
        return image_url

    def export_data(self) -> dict:
        settings = self._get_settings()
        medicines = [m.to_dict() for m in self.store.get_all()]
        logs = self.logs.get_logs(500)
        return {
            "version": "2.0",
            "exported_at": datetime.now().isoformat(),
            "medicines": medicines,
            "operation_logs": logs,
            "settings": settings.to_dict(),
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
            self.logs.import_logs(logs_data)

        settings_data = data.get("settings")
        if isinstance(settings_data, dict):
            try:
                self.settings_store.update_settings(settings_data)
            except Exception as e:
                print(f"导入设置失败: {e}")

        return {"imported": imported_count, "skipped": len(medicines_data) - imported_count}


medicine_service = MedicineService()
