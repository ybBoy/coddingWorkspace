"""
文件职责：
    业务服务层，包含零食库存的核心业务逻辑。
    维护内存中的零食列表、操作记录和用户设置，负责新增、查询、修改、删除、
    操作记录、导入导出、搜索排序、批量操作等，每次变更后通过 snack_file_store 持久化到磁盘。

数据流：
    snack_api（HTTP请求） → snack_service（操作内存列表） → snack_file_store（保存到JSON）
                                                      ↓
    snack_api（HTTP响应） ←  返回结果/列表
"""

import csv
import io
import json
from typing import List, Optional, Dict

from src.model.snack import Snack, OperationLog
from src.storage.snack_file_store import SnackFileStore


def _safe_int(value, default: int = 0) -> int:
    """安全解析整数，解析失败返回默认值"""
    try:
        return int(value)
    except (ValueError, TypeError):
        return default


class SnackService:
    """零食业务服务，管理内存中的零食库存、操作记录和用户设置并负责持久化"""

    MAX_LOGS = 100
    DEFAULT_SETTINGS = {
        "expiring_days": 7,
    }
    SORT_FIELDS = {"name", "quantity", "expiry_date"}

    def __init__(self, store: SnackFileStore = None):
        """
        初始化服务，从文件存储加载初始数据
        :param store: 文件存储器实例
        """
        self.store = store or SnackFileStore()
        self._snacks: List[Snack] = self.store.load_snacks()
        self._logs: List[OperationLog] = self.store.load_logs()
        self._settings: Dict = self._load_settings()

    # ===== 设置管理 =====

    def _load_settings(self) -> Dict:
        """加载用户设置，与默认设置合并"""
        raw = self.store.load_settings()
        merged = dict(self.DEFAULT_SETTINGS)
        if isinstance(raw, dict):
            merged.update(raw)
        merged["expiring_days"] = max(1, _safe_int(merged.get("expiring_days"), 7))
        return merged

    def get_settings(self) -> Dict:
        """获取当前设置"""
        return dict(self._settings)

    def update_settings(self, **kwargs) -> Dict:
        """
        更新用户设置
        :param kwargs: 要更新的设置字段，如 expiring_days=3
        :return: 更新后的设置
        """
        if "expiring_days" in kwargs:
            days = max(1, _safe_int(kwargs["expiring_days"], 7))
            self._settings["expiring_days"] = days

        self._persist_settings()
        return self.get_settings()

    def _persist_settings(self) -> None:
        """持久化设置到文件"""
        self.store.save_settings(self._settings)

    # ===== 持久化辅助 =====

    def _persist_snacks(self) -> None:
        """将当前零食数据持久化到文件"""
        self.store.save_snacks(self._snacks)

    def _persist_logs(self) -> None:
        """将当前操作记录持久化到文件"""
        self.store.save_logs(self._logs)

    def _add_log(self, action: str, snack: Snack,
                 quantity_change: int = 0, note: str = "") -> None:
        """添加一条操作记录"""
        log = OperationLog.create(
            action=action,
            snack_id=snack.id,
            snack_name=snack.name,
            quantity_change=quantity_change,
            note=note,
        )
        self._logs.insert(0, log)
        if len(self._logs) > self.MAX_LOGS:
            self._logs = self._logs[:self.MAX_LOGS]
        self._persist_logs()

    # ===== 查询 =====

    def get_all(self, location: Optional[str] = None,
                category: Optional[str] = None,
                only_attention: bool = False,
                keyword: Optional[str] = None,
                sort_by: Optional[str] = None,
                sort_order: str = "asc") -> List[Snack]:
        """
        获取零食列表，支持筛选、搜索、排序
        :param location: 按位置筛选
        :param category: 按分类筛选
        :param only_attention: 只看需要处理的
        :param keyword: 搜索关键词（名称/口味/分类模糊匹配）
        :param sort_by: 排序字段：name/quantity/expiry_date
        :param sort_order: 排序方向：asc/desc
        """
        result = list(self._snacks)

        if location and location != "all":
            result = [s for s in result if s.location == location]
        if category and category != "all":
            result = [s for s in result if s.category == category]
        if only_attention:
            exp_days = self._settings.get("expiring_days", 7)
            result = [s for s in result if s.needs_attention(exp_days)]
        if keyword:
            result = [s for s in result if s.matches_keyword(keyword)]

        if sort_by and sort_by in self.SORT_FIELDS:
            reverse = sort_order.lower() == "desc"
            if sort_by == "name":
                result.sort(key=lambda s: s.name.lower(), reverse=reverse)
            elif sort_by == "quantity":
                result.sort(key=lambda s: s.quantity, reverse=reverse)
            elif sort_by == "expiry_date":
                result.sort(key=lambda s: s.expiry_date or "9999-12-31", reverse=reverse)

        return result

    def get_by_id(self, snack_id: str) -> Optional[Snack]:
        """根据 ID 查找零食"""
        for snack in self._snacks:
            if snack.id == snack_id:
                return snack
        return None

    def get_locations(self) -> List[str]:
        """获取所有不重复的存放位置"""
        return sorted({s.location for s in self._snacks if s.location})

    def get_categories(self) -> List[str]:
        """获取所有不重复的分类"""
        return sorted({s.category for s in self._snacks if s.category})

    # ===== 统计 =====

    def count_needs_attention(self) -> int:
        """统计需要处理的零食数量"""
        exp_days = self._settings.get("expiring_days", 7)
        return sum(1 for s in self._snacks if s.needs_attention(exp_days))

    def get_statistics(self) -> Dict:
        """获取详细统计：总数、低库存、临期、已过期、需要处理（去重统计）、低于目标"""
        exp_days = self._settings.get("expiring_days", 7)
        total = len(self._snacks)
        low_stock = sum(1 for s in self._snacks if s.is_low_stock())
        expiring_soon = sum(1 for s in self._snacks if s.is_expiring_soon(exp_days))
        expired = sum(1 for s in self._snacks if s.is_expired())
        attention = sum(1 for s in self._snacks if s.needs_attention(exp_days))
        below_target = sum(1 for s in self._snacks if s.is_below_target())
        return {
            "total": total,
            "low_stock": low_stock,
            "expiring_soon": expiring_soon,
            "expired": expired,
            "attention": attention,
            "below_target": below_target,
            "expiring_days": exp_days,
        }

    # ===== 操作记录 =====

    def get_logs(self, limit: int = 20) -> List[OperationLog]:
        """获取最近的操作记录"""
        return list(self._logs[:max(0, limit)])

    # ===== 新增 =====

    def add_snack(self, name: str, flavor: str, quantity: int,
                  location: str, expiry_date: str, category: str = "",
                  target_quantity: int = 0) -> Snack:
        """
        新增一个零食
        """
        snack = Snack.create(name, flavor, quantity, location, expiry_date,
                             category, target_quantity)
        self._snacks.append(snack)
        self._persist_snacks()
        note = f"初始库存 {quantity} 份"
        if target_quantity > 0:
            note += f"，目标库存 {target_quantity} 份"
        self._add_log("新增", snack, quantity_change=quantity, note=note)
        return snack

    # ===== 更新 =====

    def update_snack(self, snack_id: str, **kwargs) -> Optional[Snack]:
        """
        更新零食信息，传入要修改的字段
        支持字段：name, flavor, quantity, location, expiry_date,
                 category, target_quantity, disabled
        """
        snack = self.get_by_id(snack_id)
        if snack is None:
            return None

        old_qty = snack.quantity
        snack.update(
            name=kwargs.get("name"),
            flavor=kwargs.get("flavor"),
            quantity=kwargs.get("quantity"),
            location=kwargs.get("location"),
            expiry_date=kwargs.get("expiry_date"),
            category=kwargs.get("category"),
            target_quantity=kwargs.get("target_quantity"),
            disabled=kwargs.get("disabled"),
        )
        self._persist_snacks()

        qty_change = snack.quantity - old_qty
        if qty_change != 0:
            note = f"调整数量：{old_qty} → {snack.quantity}"
            self._add_log("编辑", snack, quantity_change=qty_change, note=note)
        else:
            self._add_log("编辑", snack, note="修改信息")

        return snack

    def consume(self, snack_id: str) -> Optional[Snack]:
        """
        吃掉一份，数量减 1（最低为 0）"""
        snack = self.get_by_id(snack_id)
        if snack is None:
            return None
        old_qty = snack.quantity
        snack.quantity = max(0, snack.quantity - 1)
        self._persist_snacks()
        if old_qty != snack.quantity:
            self._add_log("吃掉", snack, quantity_change=-1, note="吃掉一份")
        return snack

    def restock(self, snack_id: str, amount: int = 1) -> Optional[Snack]:
        """
        补货，数量增加
        """
        snack = self.get_by_id(snack_id)
        if snack is None:
            return None
        add = max(0, amount)
        snack.quantity = snack.quantity + add
        self._persist_snacks()
        if add > 0:
            self._add_log("补货", snack, quantity_change=add, note=f"补货 {add} 份")
        return snack

    def toggle_disabled(self, snack_id: str) -> Optional[Snack]:
        """
        切换零食的"不再购买"标记"""
        snack = self.get_by_id(snack_id)
        if snack is None:
            return None
        snack.disabled = not snack.disabled
        self._persist_snacks()
        note = "标记不再购买" if snack.disabled else "恢复购买"
        self._add_log("状态变更", snack, note=note)
        return snack

    # ===== 批量操作 =====

    def batch_delete(self, snack_ids: List[str]) -> int:
        """
        批量删除零食
        :param snack_ids: 要删除的零食 ID 列表
        :return: 成功删除的数量
        """
        if not isinstance(snack_ids, list):
            return 0
        id_set = set(snack_ids)
        to_delete = [s for s in self._snacks if s.id in id_set]
        count = 0
        for snack in to_delete:
            self._snacks = [s for s in self._snacks if s.id != snack.id]
            self._add_log("删除", snack, note="批量删除")
            count += 1
        if count > 0:
            self._persist_snacks()
        return count

    def batch_disable(self, snack_ids: List[str], disabled: bool = True) -> int:
        """
        批量停用/启用零食
        :param snack_ids: 要操作的零食 ID 列表
        :param disabled: True 停用，False 启用
        :return: 成功操作的数量
        """
        if not isinstance(snack_ids, list):
            return 0
        id_set = set(snack_ids)
        count = 0
        for snack in self._snacks:
            if snack.id in id_set and snack.disabled != disabled:
                snack.disabled = disabled
                note = "批量停用" if disabled else "批量恢复"
                self._add_log("状态变更", snack, note=note)
                count += 1
        if count > 0:
            self._persist_snacks()
        return count

    def batch_restock(self, snack_ids: List[str], amount: int = 1) -> int:
        """
        批量补货
        :param snack_ids: 要补货的零食 ID 列表
        :param amount: 每个补多少份
        :return: 成功补货的数量
        """
        if not isinstance(snack_ids, list):
            return 0
        add = max(0, _safe_int(amount, 0))
        if add <= 0:
            return 0
        id_set = set(snack_ids)
        count = 0
        for snack in self._snacks:
            if snack.id in id_set:
                snack.quantity += add
                self._add_log("补货", snack, quantity_change=add,
                              note=f"批量补货 {add} 份")
                count += 1
        if count > 0:
            self._persist_snacks()
        return count

    # ===== 删除 =====

    def delete_snack(self, snack_id: str) -> bool:
        """
        删除零食
        :return: 是否成功删除
        """
        for i, snack in enumerate(self._snacks):
            if snack.id == snack_id:
                del self._snacks[i]
                self._persist_snacks()
                self._add_log("删除", snack, note="已从库存中移除")
                return True
        return False

    # ===== 导入导出 =====

    def export_json(self) -> str:
        """导出所有零食数据为 JSON 字符串"""
        data = [snack.to_dict() for snack in self._snacks]
        return json.dumps(data, ensure_ascii=False, indent=2)

    def export_csv(self) -> str:
        """导出所有零食数据为 CSV 字符串"""
        output = io.StringIO()
        writer = csv.writer(output)
        writer.writerow(["ID", "名称", "口味", "分类", "数量", "目标库存",
                         "存放位置", "保质期", "是否停用"])
        for s in self._snacks:
            writer.writerow([
                s.id, s.name, s.flavor, s.category,
                s.quantity, s.target_quantity,
                s.location, s.expiry_date,
                "是" if s.disabled else "否"
            ])
        return output.getvalue()

    def import_json(self, json_str: str) -> int:
        """
        从 JSON 字符串导入零食数据（合并，ID 已存在则更新）
        非数组格式、格式异常的数据会被安全跳过，不会抛 500
        :return: 导入的零食数量
        """
        try:
            data = json.loads(json_str)
        except (json.JSONDecodeError, TypeError):
            return 0

        if not isinstance(data, list):
            return 0

        count = 0
        for item in data:
            if not isinstance(item, dict):
                continue

            item_id = item.get("id", "") if isinstance(item.get("id", ""), str) else ""
            existing = self.get_by_id(item_id) if item_id else None

            qty = _safe_int(item.get("quantity"), 0)
            target_qty = _safe_int(item.get("target_quantity"), 0)
            name = str(item.get("name", "")) if item.get("name") is not None else ""
            flavor = str(item.get("flavor", "")) if item.get("flavor") is not None else ""
            location = str(item.get("location", "")) if item.get("location") is not None else ""
            expiry_date = str(item.get("expiry_date", "")) if item.get("expiry_date") is not None else ""
            category = str(item.get("category", "")) if item.get("category") is not None else ""
            disabled = bool(item.get("disabled", False))

            if existing:
                existing.update(
                    name=name,
                    flavor=flavor,
                    quantity=qty,
                    location=location,
                    expiry_date=expiry_date,
                    category=category,
                    target_quantity=target_qty,
                    disabled=disabled,
                )
                note = "导入更新"
            else:
                snack = Snack.create(
                    name=name,
                    flavor=flavor,
                    quantity=qty,
                    location=location,
                    expiry_date=expiry_date,
                    category=category,
                    target_quantity=target_qty,
                )
                if disabled:
                    snack.disabled = True
                self._snacks.append(snack)
                note = "导入新增"
                existing = snack
            self._add_log("导入", existing, note=note)
            count += 1

        if count > 0:
            self._persist_snacks()

        return count
