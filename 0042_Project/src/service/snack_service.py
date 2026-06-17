"""
文件职责：
    业务服务层，包含零食库存的核心业务逻辑。
    维护内存中的零食列表和操作记录，负责新增、查询、修改、删除、操作记录、
    导入导出等操作，每次变更后通过 snack_file_store 持久化到磁盘。

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


class SnackService:
    """零食业务服务，管理内存中的零食库存和操作记录并负责持久化"""

    MAX_LOGS = 100

    def __init__(self, store: SnackFileStore = None):
        """
        初始化服务，从文件存储加载初始数据
        :param store: 文件存储器实例
        """
        self.store = store or SnackFileStore()
        self._snacks: List[Snack] = self.store.load_snacks()
        self._logs: List[OperationLog] = self.store.load_logs()

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
                only_attention: bool = False) -> List[Snack]:
        """
        获取零食列表，支持按位置、分类筛选，或只看需要处理的
        """
        result = self._snacks
        if location and location != "all":
            result = [s for s in result if s.location == location]
        if category and category != "all":
            result = [s for s in result if s.category == category]
        if only_attention:
            result = [s for s in result if s.needs_attention()]
        return list(result)

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
        return sum(1 for s in self._snacks if s.needs_attention())

    def get_statistics(self) -> Dict:
        """获取详细统计：总数、低库存、临期、已过期"""
        total = len(self._snacks)
        low_stock = sum(1 for s in self._snacks if s.is_low_stock())
        expiring_soon = sum(1 for s in self._snacks if s.is_expiring_soon())
        expired = sum(1 for s in self._snacks if s.is_expired())
        return {
            "total": total,
            "low_stock": low_stock,
            "expiring_soon": expiring_soon,
            "expired": expired,
            "attention": low_stock + expiring_soon + expired,
        }

    # ===== 操作记录 =====

    def get_logs(self, limit: int = 20) -> List[OperationLog]:
        """获取最近的操作记录"""
        return list(self._logs[:max(0, limit)])

    # ===== 新增 =====

    def add_snack(self, name: str, flavor: str, quantity: int,
                  location: str, expiry_date: str, category: str = "") -> Snack:
        """
        新增一个零食
        """
        snack = Snack.create(name, flavor, quantity, location, expiry_date, category)
        self._snacks.append(snack)
        self._persist_snacks()
        self._add_log("新增", snack, quantity_change=quantity,
                      note=f"初始库存 {quantity} 份")
        return snack

    # ===== 更新 =====

    def update_snack(self, snack_id: str, **kwargs) -> Optional[Snack]:
        """
        更新零食信息，传入要修改的字段
        支持字段：name, flavor, quantity, location, expiry_date, category, disabled
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
        writer.writerow(["ID", "名称", "口味", "分类", "数量", "存放位置", "保质期", "是否停用"])
        for s in self._snacks:
            writer.writerow([
                s.id, s.name, s.flavor, s.category,
                s.quantity, s.location, s.expiry_date,
                "是" if s.disabled else "否"
            ])
        return output.getvalue()

    def import_json(self, json_str: str) -> int:
        """
        从 JSON 字符串导入零食数据（合并，ID 已存在则更新）
        :return: 导入的零食数量
        """
        try:
            data = json.loads(json_str)
        except (json.JSONDecodeError, TypeError):
            return 0

        count = 0
        for item in data:
            existing = self.get_by_id(item.get("id", "")) if item.get("id") else None
            if existing:
                existing.update(
                    name=item.get("name"),
                    flavor=item.get("flavor"),
                    quantity=int(item.get("quantity", 0)),
                    location=item.get("location"),
                    expiry_date=item.get("expiry_date"),
                    category=item.get("category"),
                    disabled=bool(item.get("disabled", False)),
                )
                note = "导入更新"
            else:
                snack = Snack.create(
                    name=item.get("name", ""),
                    flavor=item.get("flavor", ""),
                    quantity=int(item.get("quantity", 0)),
                    location=item.get("location", ""),
                    expiry_date=item.get("expiry_date", ""),
                    category=item.get("category", ""),
                )
                if item.get("disabled"):
                    snack.disabled = True
                self._snacks.append(snack)
                note = "导入新增"
                existing = snack
            self._add_log("导入", existing, note=note)
            count += 1

        if count > 0:
            self._persist_snacks()

        return count
